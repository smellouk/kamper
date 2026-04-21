package com.smellouk.kamper.ui

import com.smellouk.kamper.Engine
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.issues.AnrConfig
import com.smellouk.kamper.issues.IssueInfo
import com.smellouk.kamper.issues.IssuesModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.Foundation.NSUserDefaults

private const val HISTORY_SIZE = 60
private const val MAX_ISSUES = 100
private const val PREF_ISSUES = "kamper_issues_list"

internal actual class KamperUiRepository {
    private val defaults = NSUserDefaults.standardUserDefaults

    private fun loadSettings(): KamperUiSettings {
        fun bool(key: String) = if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else true
        return KamperUiSettings(
            showCpu     = bool("show_cpu"),
            showFps     = bool("show_fps"),
            showMemory  = bool("show_memory"),
            showNetwork = bool("show_network"),
            showIssues  = bool("show_issues")
        )
    }

    private val _settings = MutableStateFlow(loadSettings())
    actual val settings: StateFlow<KamperUiSettings> = _settings.asStateFlow()

    actual fun updateSettings(s: KamperUiSettings) {
        _settings.value = s
        defaults.apply {
            setBool(s.showCpu, "show_cpu")
            setBool(s.showFps, "show_fps")
            setBool(s.showMemory, "show_memory")
            setBool(s.showNetwork, "show_network")
            setBool(s.showIssues, "show_issues")
        }
    }

    private val engine = Engine()
    private val _state = MutableStateFlow(KamperUiState.EMPTY)
    actual val state: StateFlow<KamperUiState> = _state.asStateFlow()

    private val cpuHist = ArrayDeque<Float>()
    private val fpsHist = ArrayDeque<Float>()
    private val memHist = ArrayDeque<Float>()
    private val netHist = ArrayDeque<Float>()

    private fun ArrayDeque<Float>.push(v: Float) {
        if (size >= HISTORY_SIZE) removeFirst()
        addLast(v)
    }

    private val issuesList = loadPersistedIssues()

    private fun loadPersistedIssues(): MutableList<com.smellouk.kamper.issues.Issue> {
        val raw = defaults.stringForKey(PREF_ISSUES) ?: return mutableListOf()
        return raw.lines().mapNotNull { it.deserializeIssue() }.toMutableList()
    }

    private fun saveIssues() {
        defaults.setObject(issuesList.joinToString("\n") { it.serialize() }, PREF_ISSUES)
    }

    actual fun clearIssues() {
        issuesList.clear()
        saveIssues()
        _state.update { it.copy(issues = emptyList(), unreadIssueCount = 0) }
    }

    init {
        if (issuesList.isNotEmpty()) {
            _state.update { it.copy(issues = issuesList.toList()) }
        }

        with(engine) {
            install(CpuModule)
            install(FpsModule)
            install(MemoryModule())
            install(NetworkModule)
            install(IssuesModule(anr = AnrConfig()) { crash { chainToPreviousHandler = false } })

            addInfoListener<CpuInfo> { info ->
                if (info == CpuInfo.INVALID) return@addInfoListener
                val v = (info.totalUseRatio * 100).toFloat()
                cpuHist.push(v)
                _state.update { s -> s.copy(cpuPercent = v, cpuHistory = cpuHist.toList()) }
            }

            addInfoListener<FpsInfo> { info ->
                if (info == FpsInfo.INVALID) return@addInfoListener
                val v = info.fps
                fpsHist.push(v.toFloat())
                _state.update { s ->
                    s.copy(
                        fps        = v,
                        fpsPeak    = maxOf(s.fpsPeak, v),
                        fpsLow     = if (s.fpsLow == Int.MAX_VALUE) v else minOf(s.fpsLow, v),
                        fpsHistory = fpsHist.toList()
                    )
                }
            }

            addInfoListener<MemoryInfo> { info ->
                if (info == MemoryInfo.INVALID) return@addInfoListener
                val v = info.heapMemoryInfo.allocatedInMb
                memHist.push(v)
                _state.update { s -> s.copy(memoryUsedMb = v, memoryHistory = memHist.toList()) }
            }

            addInfoListener<NetworkInfo> { info ->
                if (info == NetworkInfo.INVALID || info == NetworkInfo.NOT_SUPPORTED) return@addInfoListener
                val v = info.rxSystemTotalInMb
                netHist.push(v)
                _state.update { s -> s.copy(downloadMbps = v, downloadHistory = netHist.toList()) }
            }

            addInfoListener<IssueInfo> { info ->
                if (info == IssueInfo.INVALID) return@addInfoListener
                issuesList.add(0, info.issue)
                if (issuesList.size > MAX_ISSUES) issuesList.removeAt(issuesList.size - 1)
                saveIssues()
                _state.update { s ->
                    s.copy(
                        issues           = issuesList.toList(),
                        unreadIssueCount = s.unreadIssueCount + 1
                    )
                }
            }

            start()
        }
    }

    actual fun clear() = engine.clear()
}
