package com.smellouk.kamper.api

interface InfoRepository<I : Info> {
    fun getInfo(): I
}
