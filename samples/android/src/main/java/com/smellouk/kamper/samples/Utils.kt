package com.smellouk.kamper.samples

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import java.text.DecimalFormat
import java.util.Random


class Utils {
    companion object {
        val videoUrl =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

        fun startHeavyWorkOnBackgroundThread(): List<CpuWork> = mutableListOf<CpuWork>().apply {
            for (i in 1..10) {
                add(CpuWork().apply { execute() })
            }
        }

        fun downloadBigBuckBunnyVideo(context: Context) {
            //https://gist.github.com/jsturgis/3b19447b304616f18657
            val request = DownloadManager.Request(Uri.parse(videoUrl))
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "BigBuckBunny.mp4"
            );

            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager?
            downloadManager?.enqueue(request);
        }
    }
}

private val percentFormatter = DecimalFormat("##.##%")
fun Double.toPercent(): String = percentFormatter.format(this)
fun Float.toDecimal(): String = String.format("%.2f", this)

class CpuWork : AsyncTask<Void, Void, Unit>() {
    override fun doInBackground(vararg params: Void?) {
        val rd = Random()
        val arr = IntArray(1_000_000)
        for (i in arr.indices) {
            arr[i] = rd.nextInt()
        }
        RandomSort(arr)
    }
}

class RandomSort(i: IntArray) {
    init {
        var counter = 0
        while (!isSorted(i)) {
            shuffle(i)
            counter++
        }
    }

    private fun shuffle(i: IntArray) {
        for (x in i.indices) {
            val index1 = (Math.random() * i.size).toInt()
            val index2 = (Math.random() * i.size).toInt()
            val a = i[index1]
            i[index1] = i[index2]
            i[index2] = a
        }
    }

    private fun isSorted(i: IntArray): Boolean {
        for (x in 0 until i.size - 1) {
            if (i[x] > i[x + 1]) {
                return false
            }
        }
        return true
    }
}