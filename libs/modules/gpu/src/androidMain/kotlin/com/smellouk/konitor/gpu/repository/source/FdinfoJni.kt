package com.smellouk.konitor.gpu.repository.source

internal object FdinfoJni {
    init {
        runCatching { System.loadLibrary("konitor_gpu_fdinfo") }
    }

    @JvmStatic
    external fun readEngineNs(): DoubleArray?
}
