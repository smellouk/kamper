package com.smellouk.kamper.samples.compose

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURLSession
import platform.Foundation.NSURL
import kotlin.coroutines.resume

actual suspend fun performNetworkTest(onStatus: (String) -> Unit): Unit =
    suspendCancellableCoroutine { cont ->
        onStatus("Downloading 5 MB…")
        val url = NSURL.URLWithString("https://speed.cloudflare.com/__down?bytes=5000000")
        if (url == null) {
            onStatus("Error: invalid URL")
            cont.resume(Unit)
            return@suspendCancellableCoroutine
        }

        val task = NSURLSession.sharedSession.dataTaskWithURL(url) { data, _, error ->
            if (error != null) {
                onStatus("Error: ${error.localizedDescription.take(50)}")
            } else {
                val bytes = data?.length?.toLong() ?: 0L
                onStatus("Done! ${bytes / 1024 / 1024} MB received")
            }
            cont.resume(Unit)
        }

        cont.invokeOnCancellation { task.cancel() }
        task.resume()
    }
