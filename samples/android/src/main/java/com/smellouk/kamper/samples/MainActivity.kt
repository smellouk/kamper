package com.smellouk.kamper.samples

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule

class MainActivity : AppCompatActivity() {

    private val cpuFragment = CpuFragment()
    private val fpsFragment = FpsFragment()
    private val memoryFragment = MemoryFragment()
    private val networkFragment = NetworkFragment()
    private val fragments = listOf(cpuFragment, fpsFragment, memoryFragment, networkFragment)
    private val tabTitles = listOf("CPU", "FPS", "Memory", "Network")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupTabs()
        setupKamper()
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

    private fun setupKamper() {
        lifecycle.addObserver(Kamper)
        Kamper.apply {
            install(CpuModule)
            install(FpsModule)
            install(MemoryModule(applicationContext))
            install(NetworkModule)

            addInfoListener<CpuInfo>     { if (it != CpuInfo.INVALID)     cpuFragment.update(it) }
            addInfoListener<FpsInfo>     { if (it != FpsInfo.INVALID)     fpsFragment.update(it) }
            addInfoListener<MemoryInfo>  { if (it != MemoryInfo.INVALID)  memoryFragment.update(it) }
            addInfoListener<NetworkInfo> { if (it != NetworkInfo.INVALID) networkFragment.update(it) }
        }
    }
}
