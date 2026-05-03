@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.konitor.ios.ui

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.network.NetworkInfo
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

class NetworkViewController : UIViewController(nibName = null, bundle = null) {
    private val rxRow = metricRow("Download", Theme.TEAL,  Theme.SURFACE0)
    private val txRow = metricRow("Upload",   Theme.MAUVE, Theme.SURFACE0)

    private lateinit var rxDetail:    UILabel
    private lateinit var txDetail:    UILabel
    private lateinit var statusLabel: UILabel
    private lateinit var testButton:  UIButton
    private lateinit var testTarget:  ActionTarget

    private var peakRx = 0.0
    private var peakTx = 0.0

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.BASE

        rxDetail    = detailLbl()
        txDetail    = detailLbl()
        statusLabel = hintLabel("")
        testTarget  = ActionTarget { triggerTest() }
        testButton  = makeButton("Test Network", testTarget)

        val title   = sectionLabel("System Traffic  (per interval)")
        val appHint = hintLabel("Per-app traffic is Android-only.")
        val sep     = makeSeparator()

        listOf(title, rxRow, rxDetail, txRow, txDetail, appHint, sep, statusLabel, testButton)
            .forEach { view.addSubview(it) }

        val pad = 20.0; val rh = METRIC_ROW_HEIGHT
        val c = mutableListOf<NSLayoutConstraint>()

        c += title.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor, constant = pad)
        c += title.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        c += rxRow.topAnchor.constraintEqualToAnchor(title.bottomAnchor, constant = 8.0)
        c += rxRow.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += rxRow.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)
        c += rxRow.heightAnchor.constraintEqualToConstant(rh)

        c += rxDetail.topAnchor.constraintEqualToAnchor(rxRow.bottomAnchor, constant = 2.0)
        c += rxDetail.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        c += txRow.topAnchor.constraintEqualToAnchor(rxDetail.bottomAnchor, constant = 10.0)
        c += txRow.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += txRow.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)
        c += txRow.heightAnchor.constraintEqualToConstant(rh)

        c += txDetail.topAnchor.constraintEqualToAnchor(txRow.bottomAnchor, constant = 2.0)
        c += txDetail.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        c += appHint.topAnchor.constraintEqualToAnchor(txDetail.bottomAnchor, constant = 10.0)
        c += appHint.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        c += sep.topAnchor.constraintEqualToAnchor(appHint.bottomAnchor, constant = 12.0)
        c += sep.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)

        c += testButton.topAnchor.constraintEqualToAnchor(sep.bottomAnchor, constant = 10.0)
        c += testButton.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += statusLabel.centerYAnchor.constraintEqualToAnchor(testButton.centerYAnchor)
        c += statusLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: NetworkInfo) {
        if (info == NetworkInfo.INVALID) return
        if (info == NetworkInfo.NOT_SUPPORTED) {
            rxDetail.text = "Not supported on this OS"
            txDetail.text = "Not supported on this OS"
            return
        }
        peakRx = maxOf(peakRx, info.rxSystemTotalInMb.toDouble())
        peakTx = maxOf(peakTx, info.txSystemTotalInMb.toDouble())
        val scale = maxOf(peakRx, peakTx, MIN_SCALE)

        rxRow.fraction  = (info.rxSystemTotalInMb / scale).coerceIn(0.0, 1.0)
        rxRow.valueText = fmtSpeed(info.rxSystemTotalInMb.toDouble())
        txRow.fraction  = (info.txSystemTotalInMb / scale).coerceIn(0.0, 1.0)
        txRow.valueText = fmtSpeed(info.txSystemTotalInMb.toDouble())

        rxDetail.text = "${fmtMb3(info.rxSystemTotalInMb.toDouble())} MB/interval   peak ${fmtMb2(peakRx)} MB"
        txDetail.text = "${fmtMb3(info.txSystemTotalInMb.toDouble())} MB/interval   peak ${fmtMb2(peakTx)} MB"
    }

    private fun triggerTest() {
        Konitor.logEvent("network_download_test")
        statusLabel.text = "Fetching…"
        val url = NSURL.URLWithString("https://connectivitycheck.gstatic.com/generate_204") ?: return
        NSURLSession.sharedSession.dataTaskWithURL(url) { _, response, error ->
            val text = when {
                error != null -> "Error: ${error.localizedDescription}"
                else -> "HTTP ${(response as? NSHTTPURLResponse)?.statusCode ?: "?"}"
            }
            dispatch_async(dispatch_get_main_queue()!!) { statusLabel.text = text }
        }.resume()
    }
}

private const val MIN_SCALE = 1.0

private fun detailLbl() = UILabel().apply {
    text      = "—"
    font      = Theme.MONO_SMALL
    textColor = Theme.MUTED
    translatesAutoresizingMaskIntoConstraints = false
}

private fun fmtSpeed(mb: Double): String = when {
    mb >= 1.0  -> "${fmtMb2(mb)} MB"
    mb >= 0.01 -> "${(mb * 1024).toInt()} KB"
    else       -> "< 10 KB"
}

internal fun fmtMb2(v: Double): String { val i = v.toInt(); val d = ((v - i) * 100).toInt(); return "$i.${d.toString().padStart(2, '0')}" }
internal fun fmtMb3(v: Double): String { val i = v.toInt(); val d = ((v - i) * 1000).toInt(); return "$i.${d.toString().padStart(3, '0')}" }
