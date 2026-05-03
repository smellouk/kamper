package com.smellouk.konitor.compose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

actual suspend fun performNetworkTest(onStatus: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            onStatus("Downloading 20 MB…")
            val conn = URL("https://speed.cloudflare.com/__down?bytes=20000000")
                .openConnection() as HttpURLConnection
            val bytes = conn.inputStream.use { it.readBytes() }
            onStatus("Done! ${bytes.size / 1024 / 1024} MB received")
        } catch (e: Exception) {
            onStatus("Error: ${e.message?.take(50) ?: "unknown"}")
        }
    }
}
