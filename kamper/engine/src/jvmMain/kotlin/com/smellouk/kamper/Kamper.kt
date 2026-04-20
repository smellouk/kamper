package com.smellouk.kamper

object Kamper : Engine() {
    fun setup(builder: KamperConfig.Builder.() -> Unit): Kamper {
        config = KamperConfig.Builder.apply(builder).build()
        return this
    }
}
