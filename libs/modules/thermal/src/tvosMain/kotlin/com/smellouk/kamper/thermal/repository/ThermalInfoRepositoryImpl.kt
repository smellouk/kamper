package com.smellouk.kamper.thermal.repository

import com.smellouk.kamper.thermal.ThermalInfo

internal class ThermalInfoRepositoryImpl : ThermalInfoRepository {
    override fun getInfo(): ThermalInfo = ThermalInfo.UNSUPPORTED
}
