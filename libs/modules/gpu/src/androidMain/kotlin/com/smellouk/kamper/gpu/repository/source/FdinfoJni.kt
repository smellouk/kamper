package com.smellouk.kamper.gpu.repository.source

internal object FdinfoJni {
    init {
        runCatching { System.loadLibrary("kamper_gpu_fdinfo") }
    }

    @JvmStatic
    external fun readEngineNs(): DoubleArray?
}
