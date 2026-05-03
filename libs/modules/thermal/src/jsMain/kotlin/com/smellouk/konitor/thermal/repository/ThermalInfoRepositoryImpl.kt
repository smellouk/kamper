package com.smellouk.konitor.thermal.repository

import com.smellouk.konitor.thermal.ThermalInfo

internal class ThermalInfoRepositoryImpl : ThermalInfoRepository {
    override fun getInfo(): ThermalInfo = ThermalInfo.UNSUPPORTED
}
