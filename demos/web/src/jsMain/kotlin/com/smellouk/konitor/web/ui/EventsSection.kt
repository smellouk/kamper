package com.smellouk.konitor.web.ui

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.api.UserEventInfo
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement

internal object EventsSection {
    private lateinit var listEl: HTMLElement
    private lateinit var emptyEl: HTMLElement
    private lateinit var videoBtn: HTMLButtonElement
    private val events = mutableListOf<Pair<UserEventInfo, Double>>()

    fun build(parent: HTMLElement) {
        parent.div("card") {
            p("card-title") { textContent = "Events" }

            div("card-footer") {
                videoBtn = button("btn btn-action") {
                    textContent = "user_login"
                    style.color = "#89b4fa"
                    onclick = { Konitor.logEvent("user_login"); null }
                }
                button("btn btn-action") {
                    textContent = "purchase"
                    style.color = "#89b4fa"
                    onclick = { Konitor.logEvent("purchase"); null }
                }
                button("btn btn-action") {
                    textContent = "screen_view"
                    style.color = "#89b4fa"
                    onclick = { Konitor.logEvent("screen_view"); null }
                }
                videoBtn = button("btn btn-action") {
                    textContent = "video_playback"
                    style.color = "#89b4fa"
                    onclick = { startVideoPlayback(); null }
                }
                button("btn btn-action") {
                    textContent = "Clear"
                    onclick = { clearEvents(); null }
                }
            }

            div("event-input-row") {
                val input = (document.createElement("input") as HTMLInputElement).also {
                    it.type = "text"
                    it.placeholder = "custom event name…"
                    it.className = "event-input"
                    appendChild(it)
                }
                button("btn btn-action event-log-btn") {
                    textContent = "LOG"
                    style.color = "#89b4fa"
                    onclick = {
                        val name = input.value.trim()
                        if (name.isNotEmpty()) { Konitor.logEvent(name); input.value = "" }
                        null
                    }
                }
                input.onkeydown = { e ->
                    if ((e.asDynamic().key as String) == "Enter") {
                        val name = input.value.trim()
                        if (name.isNotEmpty()) { Konitor.logEvent(name); input.value = "" }
                    }
                    null
                }
            }

            emptyEl = (document.createElement("p") as HTMLElement).also {
                it.className = "issue-empty"
                it.textContent = "No events logged"
                appendChild(it)
            }

            listEl = (document.createElement("div") as HTMLElement).also {
                it.className = "event-list"
                appendChild(it)
            }
        }
    }

    fun addEvent(info: UserEventInfo) {
        events.add(0, Pair(info, js("Date.now()") as Double))
        if (events.size > 200) events.removeAt(events.size - 1)
        render()
    }

    private fun startVideoPlayback() {
        videoBtn.textContent = "Recording…"
        videoBtn.disabled = true
        val token = Konitor.startEvent("video_playback")
        window.setTimeout({
            Konitor.endEvent(token)
            videoBtn.textContent = "video_playback"
            videoBtn.disabled = false
        }, 2000)
    }

    private fun clearEvents() {
        events.clear()
        render()
    }

    private fun render() {
        listEl.innerHTML = ""
        events.forEach { (event, wallMs) ->
            val row = (document.createElement("div") as HTMLElement).also {
                it.className = "event-row"
                listEl.appendChild(it)
            }
            val barColor = if (event.durationMs != null) "#89b4fa" else "#a6e3a1"
            (document.createElement("div") as HTMLElement).also {
                it.className = "event-bar"
                it.style.background = barColor
                row.appendChild(it)
            }
            val content = (document.createElement("div") as HTMLElement).also {
                it.className = "event-content"
                row.appendChild(it)
            }
            (document.createElement("span") as HTMLElement).also {
                it.className = "event-name"
                it.textContent = event.name
                content.appendChild(it)
            }
            event.durationMs?.let { dur ->
                (document.createElement("span") as HTMLElement).also {
                    it.className = "event-duration"
                    it.textContent = "${dur}ms"
                    content.appendChild(it)
                }
            }
            (document.createElement("span") as HTMLElement).also {
                it.className = "event-time"
                it.textContent = fmtTime(wallMs.toLong())
                row.appendChild(it)
            }
        }
        updateVisibility()
    }

    private fun updateVisibility() {
        if (events.isEmpty()) {
            emptyEl.style.display = "block"
            listEl.style.display = "none"
        } else {
            emptyEl.style.display = "none"
            listEl.style.display = "flex"
        }
    }

    private fun fmtTime(ms: Long): String {
        val sec = (ms / 1000) % 86400
        val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}
