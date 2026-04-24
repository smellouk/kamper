@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.smellouk.kamper.ui

import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreMotion.CMMotionManager
import kotlin.math.sqrt

private val motionManager = CMMotionManager()
private var shakeJob: Job? = null
private val shakeExceptionHandler = CoroutineExceptionHandler { _, _ -> }

actual fun startShakeDetection() {
    if (!motionManager.isAccelerometerAvailable()) return
    try {
        motionManager.accelerometerUpdateInterval = 0.1
        motionManager.startAccelerometerUpdates()
    } catch (_: Throwable) {
        return
    }
    shakeJob = CoroutineScope(Dispatchers.Default + SupervisorJob() + shakeExceptionHandler).launch {
        while (true) {
            val data = try { motionManager.accelerometerData } catch (_: Throwable) { null }
            if (data != null) {
                val mag = data.acceleration.useContents { sqrt(x * x + y * y + z * z) }
                if (mag > SHAKE_THRESHOLD_G) {
                    CoroutineScope(Dispatchers.Main).launch { KamperUi.expandChip() }
                }
            }
            delay(100)
        }
    }
}

actual fun stopShakeDetection() {
    shakeJob?.cancel()
    shakeJob = null
    motionManager.stopAccelerometerUpdates()
}

private const val SHAKE_THRESHOLD_G = 2.5
