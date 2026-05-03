package com.smellouk.konitor.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.smellouk.konitor.Konitor
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.DEFAULT
import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.CpuModule
import com.smellouk.konitor.fps.FpsInfo
import com.smellouk.konitor.fps.FpsModule
import com.smellouk.konitor.gc.GcInfo
import com.smellouk.konitor.gc.GcModule
import com.smellouk.konitor.gpu.GpuInfo
import com.smellouk.konitor.gpu.GpuModule
import com.smellouk.konitor.issues.AnrConfig
import com.smellouk.konitor.issues.IssueInfo
import com.smellouk.konitor.issues.IssuesModule
import com.smellouk.konitor.issues.SlowStartConfig
import com.smellouk.konitor.issues.StrictModeConfig
import com.smellouk.konitor.jank.JankInfo
import com.smellouk.konitor.jank.JankModule
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.memory.MemoryModule
import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.network.NetworkModule
import com.smellouk.konitor.thermal.ThermalInfo
import com.smellouk.konitor.thermal.ThermalModule

class MainActivity : AppCompatActivity() {

    private val cpuFragment     = CpuFragment()
    private val gpuFragment     = GpuFragment()
    private val fpsFragment     = FpsFragment()
    private val memoryFragment  = MemoryFragment()
    private val networkFragment = NetworkFragment()
    private val issuesFragment  = IssuesFragment()
    private val jankFragment    = JankFragment()
    private val gcFragment      = GcFragment()
    private val thermalFragment = ThermalFragment()
    private val eventsFragment = EventsFragment()

    private val fragments  = listOf(
        cpuFragment, gpuFragment, fpsFragment, memoryFragment, eventsFragment,
        networkFragment, issuesFragment, jankFragment, gcFragment, thermalFragment
    )
    private val tabTitles = listOf("CPU", "GPU", "FPS", "Memory", "Events", "Network", "Issues", "Jank", "GC", "Thermal")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupTabs()
        setupKonitor()
    }

    private fun setupTabs() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabTitles.forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }

        val fm = supportFragmentManager
        fm.beginTransaction().apply {
            fragments.forEachIndexed { i, frag ->
                add(R.id.fragmentContainer, frag, "tab_$i")
                if (i != 0) hide(frag)
            }
        }.commit()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                supportFragmentManager.beginTransaction().apply {
                    fragments.forEachIndexed { i, frag ->
                        if (i == tab.position) show(frag) else hide(frag)
                    }
                }.commit()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupKonitor() {
        lifecycle.addObserver(Konitor)
        Konitor.apply {
            install(CpuModule)
            install(GpuModule { logger = Logger.DEFAULT })
            install(FpsModule)
            install(MemoryModule(applicationContext))
            install(NetworkModule)
            install(JankModule(application))
            install(GcModule)
            install(ThermalModule(applicationContext))
            install(
                IssuesModule(
                    context = applicationContext,
                    anr = AnrConfig(thresholdMs = 5_000L),
                    slowStart = SlowStartConfig(coldStartThresholdMs = 1_500L),
                    strictMode = StrictModeConfig()
                ) {
                    crash { chainToPreviousHandler = false }
                }
            )

            addInfoListener<CpuInfo>     { if (it != CpuInfo.INVALID)     cpuFragment.update(it) }
            addInfoListener<GpuInfo>     { if (it != GpuInfo.INVALID)     gpuFragment.update(it) }
            addInfoListener<FpsInfo>     { if (it != FpsInfo.INVALID)     fpsFragment.update(it) }
            addInfoListener<MemoryInfo>  { if (it != MemoryInfo.INVALID)  memoryFragment.update(it) }
            addInfoListener<NetworkInfo> { if (it != NetworkInfo.INVALID) networkFragment.update(it) }
            addInfoListener<IssueInfo>   { issuesFragment.addIssue(it.issue) }
            addInfoListener<JankInfo>    { if (it != JankInfo.INVALID)    jankFragment.update(it) }
            addInfoListener<GcInfo>      { if (it != GcInfo.INVALID)      gcFragment.update(it) }
            addInfoListener<ThermalInfo> { if (it != ThermalInfo.INVALID) thermalFragment.update(it) }
        }
    }
}
