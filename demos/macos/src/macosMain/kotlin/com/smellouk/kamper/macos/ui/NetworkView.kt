@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.macos.ui

import com.smellouk.kamper.network.NetworkInfo
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*

class NetworkView : NSView {

    private val rxRow = metricRow("Download", Theme.TEAL,  Theme.SURFACE0)
    private val txRow = metricRow("Upload",   Theme.MAUVE, Theme.SURFACE0)

    private val rxDetail    = detailLbl()
    private val txDetail    = detailLbl()
    private val statusLabel = NSTextField.labelWithString("").apply { font = Theme.HINT_FONT; textColor = Theme.MUTED; translatesAutoresizingMaskIntoConstraints = false }

    private var peakRx = 0.0
    private var peakTx = 0.0

    private val downloadTarget = ActionTarget { triggerDownload() }
    private val downloadButton = makeButton("Test Download", downloadTarget)

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        wantsLayer = true

        val title   = sectionLbl("System Traffic  (per interval)")
        val appHint = NSTextField.labelWithString("Per-app traffic is Android-only.").apply { font = Theme.HINT_FONT; textColor = Theme.MUTED; translatesAutoresizingMaskIntoConstraints = false }
        val sep = NSBox(NSMakeRect(0.0, 0.0, 0.0, 0.0)).apply { boxType = NSBoxSeparator; translatesAutoresizingMaskIntoConstraints = false }

        listOf(title, rxRow, rxDetail, txRow, txDetail, appHint, sep, statusLabel, downloadButton).forEach { addSubview(it) }

        val pad = 20.0
        val rh = METRIC_ROW_HEIGHT
        val c = mutableListOf<NSLayoutConstraint>()

        c += title.topAnchor.constraintEqualToAnchor(topAnchor, constant = pad)
        c += title.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)

        c += rxRow.topAnchor.constraintEqualToAnchor(title.bottomAnchor, constant = 8.0)
        c += rxRow.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += rxRow.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += rxRow.heightAnchor.constraintEqualToConstant(rh)

        c += rxDetail.topAnchor.constraintEqualToAnchor(rxRow.bottomAnchor, constant = 2.0)
        c += rxDetail.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)

        c += txRow.topAnchor.constraintEqualToAnchor(rxDetail.bottomAnchor, constant = 10.0)
        c += txRow.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += txRow.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += txRow.heightAnchor.constraintEqualToConstant(rh)

        c += txDetail.topAnchor.constraintEqualToAnchor(txRow.bottomAnchor, constant = 2.0)
        c += txDetail.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)

        c += appHint.topAnchor.constraintEqualToAnchor(txDetail.bottomAnchor, constant = 10.0)
        c += appHint.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)

        c += sep.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += sep.bottomAnchor.constraintEqualToAnchor(downloadButton.topAnchor, constant = -10.0)

        c += statusLabel.centerYAnchor.constraintEqualToAnchor(downloadButton.centerYAnchor)
        c += statusLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)

        c += downloadButton.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += downloadButton.bottomAnchor.constraintEqualToAnchor(bottomAnchor, constant = -10.0)
        c += downloadButton.heightAnchor.constraintEqualToConstant(28.0)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: NetworkInfo) {
        if (info == NetworkInfo.INVALID) return
        if (info == NetworkInfo.NOT_SUPPORTED) {
            rxDetail.stringValue = "Not supported on this OS"
            txDetail.stringValue = "Not supported on this OS"
            return
        }
        peakRx = maxOf(peakRx, info.rxSystemTotalInMb.toDouble())
        peakTx = maxOf(peakTx, info.txSystemTotalInMb.toDouble())
        val scale = maxOf(peakRx, peakTx, MIN_SCALE)

        rxRow.fraction  = (info.rxSystemTotalInMb / scale).coerceIn(0.0, 1.0)
        rxRow.valueText = fmtSpeed(info.rxSystemTotalInMb.toDouble())
        txRow.fraction  = (info.txSystemTotalInMb / scale).coerceIn(0.0, 1.0)
        txRow.valueText = fmtSpeed(info.txSystemTotalInMb.toDouble())

        rxDetail.stringValue = "${fmtMb3(info.rxSystemTotalInMb.toDouble())} MB/interval   peak ${fmtMb2(peakRx)} MB"
        txDetail.stringValue = "${fmtMb3(info.txSystemTotalInMb.toDouble())} MB/interval   peak ${fmtMb2(peakTx)} MB"
    }

    private fun triggerDownload() {
        statusLabel.stringValue = "Fetching 20 MB…"
        downloadButton.setEnabled(false)
        val url = NSURL.URLWithString("https://speed.cloudflare.com/__down?bytes=20000000") ?: return
        NSURLSession.sharedSession.dataTaskWithURL(url) { _, _, _ ->
            CoroutineScope(Dispatchers.Main).launch {
                statusLabel.stringValue = "Done"
                downloadButton.setEnabled(true)
            }
        }.resume()
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        Theme.BASE.setFill()
        NSBezierPath.bezierPathWithRect(bounds).fill()
    }

}

private const val MIN_SCALE = 1.0

private fun detailLbl() = NSTextField.labelWithString("—").apply {
    font = Theme.MONO_SMALL; textColor = Theme.MUTED; translatesAutoresizingMaskIntoConstraints = false
}

private fun sectionLbl(text: String) = NSTextField.labelWithString(text).apply {
    font = Theme.LABEL_FONT; textColor = Theme.SUBTEXT; translatesAutoresizingMaskIntoConstraints = false
}

private fun fmtSpeed(mb: Double): String = when {
    mb >= 1.0  -> "${fmtMb2(mb)} MB"
    mb >= 0.01 -> "${(mb * 1024).toInt()} KB"
    else       -> "< 10 KB"
}

internal fun fmtMb2(v: Double): String { val i = v.toInt(); val d = ((v - i) * 100).toInt(); return "$i.${d.toString().padStart(2, '0')}" }
internal fun fmtMb3(v: Double): String { val i = v.toInt(); val d = ((v - i) * 1000).toInt(); return "$i.${d.toString().padStart(3, '0')}" }
