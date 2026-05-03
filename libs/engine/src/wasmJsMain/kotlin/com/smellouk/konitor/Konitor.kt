package com.smellouk.konitor

object Konitor : Engine() {
    fun setup(builder: KonitorConfig.Builder.() -> Unit): Konitor {
        config = KonitorConfig.Builder().apply(builder).build()
        return this
    }
}
