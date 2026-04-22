package com.smellouk.kamper.compose

actual suspend fun performNetworkTest(onStatus: (String) -> Unit) {
    // Network stress test is not implemented on iOS; the NetworkModule already monitors
    // live bandwidth via getifaddrs(). Trigger network activity manually to see it update.
    onStatus("Browse or stream to see network activity in the monitor above")
}
