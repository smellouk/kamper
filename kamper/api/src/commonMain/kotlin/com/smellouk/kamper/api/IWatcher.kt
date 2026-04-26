package com.smellouk.kamper.api

interface IWatcher<I : Info> {
    fun startWatching(
        intervalInMs: Long,
        listeners: List<InfoListener<I>>,
        onSampleDelivered: (() -> Unit)? = null
    )
    fun stopWatching()
}
