package com.smellouk.kamper.samples

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onBtnClicked(view: View) {
        when (view.id) {
            R.id.cpuBtn -> {
                startActivity(Intent(this, CpuActivity::class.java))
            }
            R.id.fpsBtn -> {
                startActivity(Intent(this, FpsActivity::class.java))
            }
            R.id.memoryBtn -> {
                startActivity(Intent(this, MemoryActivity::class.java))
            }
            R.id.networkBtn -> {
                startActivity(Intent(this, NetworkActivity::class.java))
            }
        }
    }
}