package com.smellouk.konitor.jank.repository

import com.smellouk.konitor.jank.JankInfo

internal class JankInfoRepositoryImpl : JankInfoRepository {
    override fun getInfo(): JankInfo = JankInfo.UNSUPPORTED
}
