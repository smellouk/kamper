package com.smellouk.kamper.compose

expect suspend fun performNetworkTest(onStatus: (String) -> Unit)
