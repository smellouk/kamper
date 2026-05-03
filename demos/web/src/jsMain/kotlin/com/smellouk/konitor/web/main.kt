package com.smellouk.konitor.web

import com.smellouk.konitor.web.ui.App
import kotlinx.browser.document

fun main() {
    document.addEventListener("DOMContentLoaded", { App.init() })
}
