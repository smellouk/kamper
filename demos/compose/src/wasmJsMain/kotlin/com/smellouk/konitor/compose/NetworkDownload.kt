package com.smellouk.konitor.compose

import kotlinx.coroutines.delay

@JsFun("""() => {
    window.__konitorDl = null;
    fetch('https://speed.cloudflare.com/__down?bytes=20000000')
        .then(function(r) { return r.arrayBuffer(); })
        .then(function(b) { window.__konitorDl = b.byteLength; })
        .catch(function() { window.__konitorDl = -1; });
}""")
private external fun jsStartNetworkTest()

@JsFun("() => { var r = window.__konitorDl; return (r === null || r === undefined) ? -2.0 : +r; }")
private external fun jsGetNetworkTestResult(): Double

actual suspend fun performNetworkTest(onStatus: (String) -> Unit) {
    jsStartNetworkTest()
    onStatus("Downloading 20 MB…")
    repeat(600) {
        delay(100)
        val result = jsGetNetworkTestResult()
        when {
            result == -2.0 -> return@repeat
            result < 0 -> { onStatus("Download failed"); return }
            else -> { onStatus("Done! ${(result / 1024.0 / 1024.0).toInt()} MB received"); return }
        }
    }
    onStatus("Download timed out")
}
