package com.smellouk.konitor.web.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement

internal inline fun HTMLElement.div(cls: String, block: HTMLElement.() -> Unit = {}): HTMLElement =
    (document.createElement("div") as HTMLElement).also {
        it.className = cls
        it.block()
        appendChild(it)
    }

internal inline fun HTMLElement.p(cls: String, block: HTMLElement.() -> Unit = {}): HTMLElement =
    (document.createElement("p") as HTMLElement).also {
        it.className = cls
        it.block()
        appendChild(it)
    }

internal inline fun HTMLElement.span(cls: String, block: HTMLElement.() -> Unit = {}): HTMLElement =
    (document.createElement("span") as HTMLElement).also {
        it.className = cls
        it.block()
        appendChild(it)
    }

internal inline fun HTMLElement.button(
    cls: String,
    block: HTMLButtonElement.() -> Unit = {}
): HTMLButtonElement =
    (document.createElement("button") as HTMLButtonElement).also {
        it.className = cls
        it.block()
        appendChild(it)
    }
