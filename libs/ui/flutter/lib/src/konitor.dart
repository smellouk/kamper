import 'dart:async';
import 'package:flutter/services.dart';
import 'konitor_config.dart';
import 'models/cpu_info.dart';
import 'models/fps_info.dart';
import 'models/memory_info.dart';
import 'models/network_info.dart';
import 'models/issue_info.dart';
import 'models/jank_info.dart';
import 'models/gc_info.dart';
import 'models/thermal_info.dart';
import 'models/gpu_info.dart';
import 'models/user_event_info.dart';

class Konitor {
  static const _control = MethodChannel('com.smellouk.konitor/control');

  static Stream<CpuInfo> get cpuStream =>
      const EventChannel('com.smellouk.konitor/cpu')
          .receiveBroadcastStream()
          .map((event) =>
              CpuInfo.fromMap(Map<dynamic, dynamic>.from(event as Map)));

  static Stream<FpsInfo> get fpsStream =>
      const EventChannel('com.smellouk.konitor/fps')
          .receiveBroadcastStream()
          .map((event) =>
              FpsInfo.fromMap(Map<dynamic, dynamic>.from(event as Map)));

  static Stream<MemoryInfo> get memoryStream =>
      const EventChannel('com.smellouk.konitor/memory')
          .receiveBroadcastStream()
          .map((event) =>
              MemoryInfo.fromMap(Map<dynamic, dynamic>.from(event as Map)));

  static Stream<NetworkInfo> get networkStream =>
      const EventChannel('com.smellouk.konitor/network')
          .receiveBroadcastStream()
          .map((event) =>
              NetworkInfo.fromMap(Map<dynamic, dynamic>.from(event as Map)));

  static Stream<IssueInfo> get issuesStream =>
      const EventChannel('com.smellouk.konitor/issues')
          .receiveBroadcastStream()
          .map((event) =>
              IssueInfo.fromMap(Map<dynamic, dynamic>.from(event as Map)));

  static Stream<JankInfo> get jankStream =>
      const EventChannel('com.smellouk.konitor/jank')
          .receiveBroadcastStream()
          .map((event) =>
              JankInfo.fromMap(Map<dynamic, dynamic>.from(event as Map)));

  static Stream<GcInfo> get gcStream =>
      const EventChannel('com.smellouk.konitor/gc')
          .receiveBroadcastStream()
          .map((event) =>
              GcInfo.fromMap(Map<dynamic, dynamic>.from(event as Map)));

  static Stream<ThermalInfo> get thermalStream =>
      const EventChannel('com.smellouk.konitor/thermal')
          .receiveBroadcastStream()
          .map((event) =>
              ThermalInfo.fromMap(Map<dynamic, dynamic>.from(event as Map)));

  static Stream<GpuInfo> get gpuStream =>
      const EventChannel('com.smellouk.konitor/gpu')
          .receiveBroadcastStream()
          .map((event) =>
              GpuInfo.fromMap(Map<dynamic, dynamic>.from(event as Map)));

  static Stream<UserEventInfo> get userEventStream =>
      const EventChannel('com.smellouk.konitor/user_event')
          .receiveBroadcastStream()
          .map((event) =>
              UserEventInfo.fromMap(Map<dynamic, dynamic>.from(event as Map)));

  static Future<void> start([KonitorConfig? config]) =>
      _control.invokeMethod('start', config?.toMap() ?? {});

  static Future<void> stop() => _control.invokeMethod('stop');

  static Future<void> showOverlay() => _control.invokeMethod('showOverlay');

  static Future<void> hideOverlay() => _control.invokeMethod('hideOverlay');

  static Future<void> logEvent(String name) =>
      _control.invokeMethod('logEvent', {'name': name});

  static Future<int> startEvent(String name) async =>
      (await _control.invokeMethod<int>('startEvent', {'name': name}))!;

  static Future<void> endEvent(int tokenId) =>
      _control.invokeMethod('endEvent', {'tokenId': tokenId});

  static Future<void> simulateCrash() =>
      _control.invokeMethod('simulateCrash');

  static Future<void> simulateSlowSpan() =>
      _control.invokeMethod('simulateSlowSpan');

  static Future<void> simulateJank() =>
      _control.invokeMethod('simulateJank');
}
