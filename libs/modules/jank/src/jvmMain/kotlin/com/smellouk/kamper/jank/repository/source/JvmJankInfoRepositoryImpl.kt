package com.smellouk.kamper.jank.repository.source

import com.smellouk.kamper.jank.JankInfo
import com.smellouk.kamper.jank.repository.JankInfoRepository

internal class JvmJankInfoRepositoryImpl : JankInfoRepository {
    override fun getInfo(): JankInfo = JankInfo.UNSUPPORTED
}
