package com.smellouk.kamper.api

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class Watcher<I : Info>(
    private val defaultDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
    private val infoRepository: InfoRepository<I>,
    private val logger: Logger
) : IWatcher<I> {
    // Visible only for testing
    internal var job: Job? = null

    override fun startWatching(
        intervalInMs: Long,
        listeners: List<InfoListener<I>>,
        onSampleDelivered: (() -> Unit)?
    ) {
        if (job?.isActive == true) {
            return
        }
        job = CoroutineScope(defaultDispatcher).launch {
            delay(intervalInMs)
            while (isActive) {
                val info = try {
                    infoRepository.getInfo()
                } catch (e: Exception) {
                    logger.log(e.stackTraceToString())
                    null
                }
                if (info != null) {
                    logger.log(info.toString())
                    withContext(mainDispatcher) {
                        listeners.forEach { listener ->
                            listener.invoke(info)
                        }
                        onSampleDelivered?.invoke()
                    }
                }
                delay(intervalInMs)
            }
        }
    }

    override fun stopWatching() {
        job?.cancel()
        job = null
    }
}
