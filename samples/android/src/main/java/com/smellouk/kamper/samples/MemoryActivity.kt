package com.smellouk.kamper.samples

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule

class MemoryActivity : AppCompatActivity() {
    private val intItemSize = 1310720
    private val dataList = mutableListOf<IntArray>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory)
        lifecycle.addObserver(Kamper)
        val infoTxt = findViewById<TextView>(R.id.infoTxt)

        Kamper.apply {
            install(MemoryModule(applicationContext))

            addInfoListener<MemoryInfo> { memoryInfo ->
                if (memoryInfo == MemoryInfo.INVALID) return@addInfoListener

                with(memoryInfo) {
                    val total = (ramInfo.totalRamInMb / 1024).toDecimal()
                    val ramUsed =
                        ((ramInfo.totalRamInMb - ramInfo.availableRamInMb) / 1024).toDecimal()

                    val maxHeap = heapMemoryInfo.maxMemoryInMb.toDecimal()
                    val usedHeap = heapMemoryInfo.allocatedInMb.toDecimal()
                    infoTxt.text = "Ram>\n" +
                            "Total: ${total}Gb, Used: ${ramUsed}Gb\n" +
                            "\nHeap>\n" +
                            "Max: ${maxHeap}Mb, Used: ${usedHeap}Mb"
                }
            }
        }
    }

    fun onBtnClicked(view: View) {
        when (view.id) {
            R.id.addHeapBtn -> addDataToMemoryHeap()
        }
    }

    private fun addDataToMemoryHeap() {
        try {
            // Int size = 4 bytes / 4 * 1310720 == 5Mb
            dataList.add(IntArray(intItemSize))
        } catch (e: OutOfMemoryError) {
            Toast.makeText(
                this,
                e.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}