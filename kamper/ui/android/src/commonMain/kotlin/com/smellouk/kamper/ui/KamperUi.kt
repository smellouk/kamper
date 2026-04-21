package com.smellouk.kamper.ui

expect object KamperUi {
    fun configure(block: KamperUiConfig.() -> Unit)
    fun attach()
    fun detach()
}
