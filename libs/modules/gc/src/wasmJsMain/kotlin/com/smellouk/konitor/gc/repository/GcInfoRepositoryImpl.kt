package com.smellouk.konitor.gc.repository

import com.smellouk.konitor.gc.GcInfo

internal class GcInfoRepositoryImpl : GcInfoRepository {
    override fun getInfo(): GcInfo = GcInfo.UNSUPPORTED
}
