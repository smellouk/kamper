package com.smellouk.kamper.jank.repository

import com.smellouk.kamper.jank.JankInfo

internal class JankInfoRepositoryImpl : JankInfoRepository {
    override fun getInfo(): JankInfo = JankInfo.INVALID
}
