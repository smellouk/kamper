import Flutter
import UIKit
import Konitor

// MARK: - MetricStreamHandler
// One instance per functional channel. Stores the FlutterEventSink for emission.
class MetricStreamHandler: NSObject, FlutterStreamHandler {
    var eventSink: FlutterEventSink?

    func onListen(withArguments arguments: Any?,
                  eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        self.eventSink = events
        return nil
    }

    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        self.eventSink = nil
        return nil
    }
}

// MARK: - NoOpStreamHandler
// Registers Android-only channels so Dart does not error. Never emits.
class NoOpStreamHandler: NSObject, FlutterStreamHandler {
    func onListen(withArguments arguments: Any?,
                  eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        return nil
    }
    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        return nil
    }
}

// MARK: - KonitorFlutterPlugin
public class KonitorFlutterPlugin: NSObject, FlutterPlugin {

    private var bridge: KonitorBridge?
    private var tokenMap: [Int: (token: EventToken, name: String, startMs: Double)] = [:]
    private var nextTokenId: Int = 0

    private let cpuHandler       = MetricStreamHandler()
    private let fpsHandler       = MetricStreamHandler()
    private let memoryHandler    = MetricStreamHandler()
    private let networkHandler   = MetricStreamHandler()
    private let issuesHandler    = MetricStreamHandler()
    private let userEventHandler = MetricStreamHandler()

    public static func register(with registrar: FlutterPluginRegistrar) {
        let instance = KonitorFlutterPlugin()

        // Control MethodChannel
        let methodChannel = FlutterMethodChannel(
            name: "com.smellouk.konitor/control",
            binaryMessenger: registrar.messenger())
        registrar.addMethodCallDelegate(instance, channel: methodChannel)

        // 4 functional iOS channels
        FlutterEventChannel(name: "com.smellouk.konitor/cpu",
                            binaryMessenger: registrar.messenger())
            .setStreamHandler(instance.cpuHandler)
        FlutterEventChannel(name: "com.smellouk.konitor/fps",
                            binaryMessenger: registrar.messenger())
            .setStreamHandler(instance.fpsHandler)
        FlutterEventChannel(name: "com.smellouk.konitor/memory",
                            binaryMessenger: registrar.messenger())
            .setStreamHandler(instance.memoryHandler)
        FlutterEventChannel(name: "com.smellouk.konitor/network",
                            binaryMessenger: registrar.messenger())
            .setStreamHandler(instance.networkHandler)

        FlutterEventChannel(name: "com.smellouk.konitor/issues",
                            binaryMessenger: registrar.messenger())
            .setStreamHandler(instance.issuesHandler)

        // Android-only channels — registered as no-ops to prevent Dart-side errors (D-08)
        FlutterEventChannel(name: "com.smellouk.konitor/jank",
                            binaryMessenger: registrar.messenger())
            .setStreamHandler(NoOpStreamHandler())
        FlutterEventChannel(name: "com.smellouk.konitor/gc",
                            binaryMessenger: registrar.messenger())
            .setStreamHandler(NoOpStreamHandler())
        FlutterEventChannel(name: "com.smellouk.konitor/thermal",
                            binaryMessenger: registrar.messenger())
            .setStreamHandler(NoOpStreamHandler())
        FlutterEventChannel(name: "com.smellouk.konitor/gpu",
                            binaryMessenger: registrar.messenger())
            .setStreamHandler(NoOpStreamHandler())
        FlutterEventChannel(name: "com.smellouk.konitor/user_event",
                            binaryMessenger: registrar.messenger())
            .setStreamHandler(instance.userEventHandler)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "start":
            startMonitoring(config: call.arguments as? [String: Any])
            result(nil)
        case "stop":
            stopMonitoring()
            result(nil)
        case "showOverlay":
            KonitorUi.shared.attach()
            result(nil)
        case "hideOverlay":
            KonitorUi.shared.hide()
            result(nil)
        case "logEvent":
            if let args = call.arguments as? [String: Any],
               let name = args["name"] as? String {
                Konitor.shared.logEvent(name: name)
                DispatchQueue.main.async { [weak self] in
                    self?.userEventHandler.eventSink?(["name": name, "durationMs": NSNull()])
                }
            }
            result(nil)
        case "startEvent":
            if let args = call.arguments as? [String: Any],
               let name = args["name"] as? String {
                let token = Konitor.shared.startEvent(name: name)
                nextTokenId += 1
                let id = nextTokenId
                tokenMap[id] = (token: token, name: name, startMs: Date().timeIntervalSince1970 * 1000)
                result(id)
            } else {
                result(FlutterError(code: "INVALID_ARG", message: "name required", details: nil))
            }
        case "endEvent":
            if let args = call.arguments as? [String: Any],
               let tokenId = args["tokenId"] as? Int,
               let entry = tokenMap.removeValue(forKey: tokenId) {
                Konitor.shared.endEvent(token: entry.token)
                let durationMs = Date().timeIntervalSince1970 * 1000 - entry.startMs
                DispatchQueue.main.async { [weak self] in
                    self?.userEventHandler.eventSink?(["name": entry.name, "durationMs": durationMs])
                }
            }
            result(nil)
        case "simulateCrash":
            result(nil)
            bridge?.simulateCrash()
        case "simulateSlowSpan":
            result(nil)
            bridge?.simulateSlowSpan()
        case "simulateJank":
            result(nil)
        default:
            result(FlutterMethodNotImplemented)
        }
    }

    private func startMonitoring(config: [String: Any]?) {
        if bridge != nil { stopMonitoring() }

        let cpuEnabled     = boolFlag(config, "cpu")
        let fpsEnabled     = boolFlag(config, "fps")
        let memoryEnabled  = boolFlag(config, "memory")
        let networkEnabled = boolFlag(config, "network")
        let issuesEnabled  = boolFlag(config, "issues")

        let b = KonitorBridge()
        bridge = b

        b.setup(
            onCpu: { [weak self] info in
                guard cpuEnabled else { return }
                guard info != CpuInfo.Companion.shared.INVALID else { return }
                DispatchQueue.main.async { [weak self] in
                    self?.cpuHandler.eventSink?([
                        "totalUseRatio": info.totalUseRatio,
                        "appRatio":      info.appRatio,
                        "userRatio":     info.userRatio,
                        "systemRatio":   info.systemRatio,
                        "ioWaitRatio":   info.ioWaitRatio
                    ])
                }
            },
            onFps: { [weak self] info in
                guard fpsEnabled else { return }
                guard info != FpsInfo.Companion.shared.INVALID else { return }
                DispatchQueue.main.async { [weak self] in
                    self?.fpsHandler.eventSink?(["fps": info.fps])
                }
            },
            onMemory: { [weak self] info in
                guard memoryEnabled else { return }
                guard info != MemoryInfo.Companion.shared.INVALID else { return }
                let heap = info.heapMemoryInfo
                let ram = info.ramInfo
                DispatchQueue.main.async { [weak self] in
                    self?.memoryHandler.eventSink?([
                        "heapAllocatedMb": heap.allocatedInMb,
                        "heapMaxMb":       heap.maxMemoryInMb,
                        "ramUsedMb":       ram.totalRamInMb - ram.availableRamInMb,
                        "ramTotalMb":      ram.totalRamInMb,
                        "isLowMemory":     ram.isLowMemory
                    ])
                }
            },
            onNetwork: { [weak self] info in
                guard networkEnabled else { return }
                guard info != NetworkInfo.Companion.shared.INVALID,
                      info != NetworkInfo.Companion.shared.NOT_SUPPORTED else { return }
                DispatchQueue.main.async { [weak self] in
                    self?.networkHandler.eventSink?([
                        "rxMb": info.rxSystemTotalInMb,
                        "txMb": info.txSystemTotalInMb
                    ])
                }
            }
        )
        b.start()

        if issuesEnabled {
            b.setupIssues { [weak self] info in
                guard info != IssueInfo.Companion.shared.INVALID else { return }
                let iss = info.issue
                var map: [String: Any] = [
                    "id"       : iss.id,
                    "type"     : iss.type.name,
                    "severity" : iss.severity.name,
                    "message"  : iss.message,
                    "timestamp": Double(iss.timestampMs)
                ]
                if let dur = iss.durationMs { map["durationMs"] = Double(truncating: dur) }
                if let thr = iss.threadName { map["threadName"] = thr }
                DispatchQueue.main.async { [weak self] in
                    self?.issuesHandler.eventSink?(map)
                }
            }
        }
    }

    private func stopMonitoring() {
        bridge?.stop()
        bridge?.clear()
        bridge = nil
        for entry in tokenMap.values { Konitor.shared.endEvent(token: entry.token) }
        tokenMap.removeAll()
    }
}

// MARK: - Config flag helper
private func boolFlag(_ config: [String: Any]?, _ key: String) -> Bool {
    guard let config = config, let val = config[key] else { return true }
    return (val as? Bool) ?? true
}
