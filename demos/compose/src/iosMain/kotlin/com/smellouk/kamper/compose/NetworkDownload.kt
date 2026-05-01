@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.smellouk.kamper.compose

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.*
import kotlin.coroutines.resume

actual suspend fun performNetworkTest(onStatus: (String) -> Unit) {
    onStatus("Downloading 20 MB…")
    withContext(Dispatchers.Default) {
        try {
            val url = NSURL.URLWithString("https://speed.cloudflare.com/__down?bytes=20000000")!!
            val bytesReceived = suspendCancellableCoroutine<Long> { cont ->
                val task = NSURLSession.sharedSession.dataTaskWithURL(url) { data, _, _ ->
                    val bytes = (data as? NSData)?.length?.toLong() ?: 0L
                    cont.resume(bytes)
                }
                task?.resume()
                cont.invokeOnCancellation { task?.cancel() }
            }
            val mb = bytesReceived / 1024 / 1024
            onStatus("Done! $mb MB received")
        } catch (e: Exception) {
            onStatus("Error: ${e.message?.take(60) ?: "unknown"}")
        }
    }
}
