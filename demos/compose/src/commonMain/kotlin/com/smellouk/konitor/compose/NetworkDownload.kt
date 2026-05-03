package com.smellouk.konitor.compose

expect suspend fun performNetworkTest(onStatus: (String) -> Unit)
