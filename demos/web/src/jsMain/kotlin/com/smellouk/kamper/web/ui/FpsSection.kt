package com.smellouk.kamper.web.ui

import com.smellouk.kamper.fps.FpsInfo
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object FpsSection {
    private lateinit var fpsNumberEl: HTMLElement
    private lateinit var canvas: HTMLCanvasElement
    private var ctx: CanvasRenderingContext2D? = null
    private var angle = 0.0
    private var animHandle = 0

    fun build(container: HTMLElement) {
        val card = container.div("card") {
            p("card-title") { textContent = "Frame Rate" }
            div("fps-hero") {
                fpsNumberEl = span("fps-number") { textContent = "—" }
                p("fps-unit") { textContent = "frames / second" }
            }
            div("fps-canvas-wrap") {
                canvas = (document.createElement("canvas") as HTMLCanvasElement).also { c ->
                    c.width = 200
                    c.height = 200
                    c.style.cssText = "border-radius:8px"
                    appendChild(c)
                    ctx = c.getContext("2d") as CanvasRenderingContext2D
                }
            }
        }
        startAnimation()
    }

    fun update(info: FpsInfo) {
        if (info == FpsInfo.INVALID) return
        fpsNumberEl.textContent = info.fps.toString()
    }

    private fun startAnimation() {
        fun draw(ts: Double) {
            val c = ctx ?: return
            val w = canvas.width.toDouble()
            val h = canvas.height.toDouble()
            val cx = w / 2
            val cy = h / 2
            val r = cx - 10

            c.clearRect(0.0, 0.0, w, h)

            c.fillStyle = "#313244"
            c.beginPath()
            c.arc(cx, cy, r, 0.0, 2 * PI)
            c.fill()

            val dotColors = arrayOf("#89b4fa", "#cba6f7", "#a6e3a1", "#f9e2af", "#fab387")
            val dotCount = 5
            for (i in 0 until dotCount) {
                val a = angle + (2 * PI * i / dotCount)
                val x = cx + (r - 18) * cos(a)
                val y = cy + (r - 18) * sin(a)
                val dotR = 8.0 - i * 0.8
                c.fillStyle = dotColors[i % dotColors.size]
                c.globalAlpha = 1.0 - i * 0.15
                c.beginPath()
                c.arc(x, y, dotR, 0.0, 2 * PI)
                c.fill()
            }
            c.globalAlpha = 1.0
            angle += 0.04
            animHandle = window.requestAnimationFrame(::draw)
        }
        animHandle = window.requestAnimationFrame(::draw)
    }

    fun stopAnimation() {
        window.cancelAnimationFrame(animHandle)
    }
}
