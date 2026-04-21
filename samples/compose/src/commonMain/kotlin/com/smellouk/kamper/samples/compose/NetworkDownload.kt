package com.smellouk.kamper.samples.compose

expect suspend fun performNetworkTest(onStatus: (String) -> Unit)
