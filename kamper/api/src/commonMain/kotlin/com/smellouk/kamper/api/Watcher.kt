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
) {
    private var job: Job? = null

    fun startWatching(intervalInMs: Long, listeners: List<InfoListener<I>>) {
        if (job?.isActive == true) {
            return
        }
        job = CoroutineScope(defaultDispatcher).launch {
            while (isActive) {
                val info = try {
                    infoRepository.getInfo()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                if (info != null) {
                    println(info)
                    withContext(mainDispatcher) {
                        listeners.forEach { listener ->
                            listener.invoke(info)
                        }
                    }
                }
                delay(intervalInMs)
            }
        }
    }

    fun stopWatching() {
        job?.cancel()
        job = null
    }
}
