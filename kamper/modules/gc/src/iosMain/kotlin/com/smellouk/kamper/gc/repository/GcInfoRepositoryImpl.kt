package com.smellouk.kamper.gc.repository

import com.smellouk.kamper.gc.GcInfo

internal class GcInfoRepositoryImpl : GcInfoRepository {
    override fun getInfo(): GcInfo = GcInfo.INVALID
}
