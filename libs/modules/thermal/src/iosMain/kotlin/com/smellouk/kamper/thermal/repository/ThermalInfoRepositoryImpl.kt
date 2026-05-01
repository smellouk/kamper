package com.smellouk.kamper.thermal.repository

import com.smellouk.kamper.thermal.ThermalInfo

// NSProcessInfo.thermalState is not exposed in the Kotlin/Native iOS platform bindings.
// A future implementation can bridge it via a custom .def cinterop file (same pattern as
// the macOS SMC implementation). For now iOS reports UNSUPPORTED on all targets.
internal class ThermalInfoRepositoryImpl : ThermalInfoRepository {
    override fun getInfo(): ThermalInfo = ThermalInfo.UNSUPPORTED
}
