package com.smellouk.konitor.jank.repository.source

import com.smellouk.konitor.jank.JankInfo
import com.smellouk.konitor.jank.repository.JankInfoRepository

internal class JvmJankInfoRepositoryImpl : JankInfoRepository {
    override fun getInfo(): JankInfo = JankInfo.UNSUPPORTED
}
