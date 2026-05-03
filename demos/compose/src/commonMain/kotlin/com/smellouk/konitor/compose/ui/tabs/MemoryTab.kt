package com.smellouk.konitor.compose.ui.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smellouk.konitor.Konitor
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.compose.ui.KonitorColors
import com.smellouk.konitor.compose.ui.MetricRow

@Composable
fun MemoryTab(info: MemoryInfo, modifier: Modifier = Modifier) {
    val allocations = remember { mutableStateOf(mutableListOf<ByteArray>()) }
    var allocationCount = remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        val heap = info.heapMemoryInfo
        val ram = info.ramInfo
        val pss = info.pssInfo
        val heapValid = heap != MemoryInfo.HeapMemoryInfo.INVALID && heap.maxMemoryInMb > 0
        val ramValid = ram != MemoryInfo.RamInfo.INVALID && ram.totalRamInMb > 0
        val pssValid = pss != MemoryInfo.PssInfo.INVALID && pss.totalPssInMb >= 0

        SectionTitle("Heap Memory")

        if (heapValid) {
            val frac = (heap.allocatedInMb / heap.maxMemoryInMb).coerceIn(0f, 1f)
            MetricRow("Heap", frac, "${(frac * 100).toInt()}%", KonitorColors.green)
            Text(
                text = "${heap.allocatedInMb.fmt1()} MB  /  ${heap.maxMemoryInMb.fmt1()} MB max",
                fontSize = 11.sp,
                color = KonitorColors.overlay1,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        } else {
            Text("—", color = KonitorColors.overlay1, fontSize = 13.sp)
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("System RAM")

        if (ramValid) {
            val used = ram.totalRamInMb - ram.availableRamInMb
            val frac = (used / ram.totalRamInMb).coerceIn(0f, 1f)
            MetricRow("RAM", frac, "${(frac * 100).toInt()}%", KonitorColors.blue)
            Text(
                text = "${used.toInt()} MB  /  ${ram.totalRamInMb.toInt()} MB total",
                fontSize = 11.sp,
                color = KonitorColors.overlay1,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            if (ram.isLowMemory) {
                Spacer(Modifier.height(4.dp))
                Text("⚠ Low Memory", color = KonitorColors.red, fontSize = 13.sp)
            }
        } else {
            Text("—", color = KonitorColors.overlay1, fontSize = 13.sp)
        }

        if (pssValid) {
            Spacer(Modifier.height(16.dp))
            SectionTitle("PSS Breakdown  (Android only)")
            Text(
                text = "Total: ${pss.totalPssInMb.fmt1()} MB   Dalvik: ${pss.dalvikPssInMb.fmt1()}" +
                        "   Native: ${pss.nativePssInMb.fmt1()}   Other: ${pss.otherPssInMb.fmt1()}",
                fontSize = 11.sp,
                color = KonitorColors.overlay1,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    Konitor.logEvent("memory_alloc_32mb")
                    allocations.value.add(ByteArray(32 * 1024 * 1024))
                    allocationCount.value++
                },
                colors = ButtonDefaults.buttonColors(containerColor = KonitorColors.surface1),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text("ALLOC 32 MB", color = KonitorColors.text, fontSize = 13.sp)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    Konitor.logEvent("memory_gc")
                    allocations.value.clear()
                    allocationCount.value = 0
                    gc()
                },
                colors = ButtonDefaults.buttonColors(containerColor = KonitorColors.surface1),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text("FORCE GC", color = KonitorColors.text, fontSize = 13.sp)
            }
        }
    }
}

private fun Float.fmt1(): String {
    val v = (this * 10f).toInt()
    return "${v / 10}.${v % 10}"
}

expect fun gc()
