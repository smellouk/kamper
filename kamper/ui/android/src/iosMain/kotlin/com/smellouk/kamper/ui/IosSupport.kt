@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.smellouk.kamper.ui

import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreMotion.CMMotionManager
import kotlin.math.sqrt

private val motionManager = CMMotionManager()
private var shakeJob: Job? = null

actual fun startShakeDetection() {
    if (!motionManager.isAccelerometerAvailable()) return
    motionManager.accelerometerUpdateInterval = 0.1
    motionManager.startAccelerometerUpdates()
    shakeJob = CoroutineScope(Dispatchers.Default).launch {
        while (true) {
            val data = motionManager.accelerometerData
            if (data != null) {
                val mag = data.acceleration.useContents { sqrt(x * x + y * y + z * z) }
                if (mag > SHAKE_THRESHOLD_G) {
                    KamperUi.expandChip()
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
