package com.smellouk.konitor.api

interface InfoRepository<I : Info> {
    fun getInfo(): I
}
