package com.smellouk.konitor.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.smellouk.konitor.Konitor
import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.android.views.MetricRowView
import java.net.URL
import java.util.concurrent.Executors

class NetworkFragment : Fragment() {

    private var rxRow: MetricRowView? = null
    private var rxDetail: TextView? = null
    private var txRow: MetricRowView? = null
    private var txDetail: TextView? = null
    private var appRxRow: MetricRowView? = null
    private var appRxDetail: TextView? = null
    private var appTxRow: MetricRowView? = null
    private var appTxDetail: TextView? = null
    private var statusText: TextView? = null
    private var downloadButton: View? = null

    private var peakRx = 0f
    private var peakTx = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_network, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rxRow = view.findViewById<MetricRowView>(R.id.rxRow).apply {
            label = "Download"; barColor = 0xFF94E2D5.toInt()
        }
        txRow = view.findViewById<MetricRowView>(R.id.txRow).apply {
            label = "Upload"; barColor = 0xFFCBA6F7.toInt()
        }
        appRxRow = view.findViewById<MetricRowView>(R.id.appRxRow).apply {
            label = "Rx App"; barColor = 0xFF94E2D5.toInt()
        }
        appTxRow = view.findViewById<MetricRowView>(R.id.appTxRow).apply {
            label = "Tx App"; barColor = 0xFFCBA6F7.toInt()
        }
        rxDetail = view.findViewById(R.id.rxDetail)
        txDetail = view.findViewById(R.id.txDetail)
        appRxDetail = view.findViewById(R.id.appRxDetail)
        appTxDetail = view.findViewById(R.id.appTxDetail)
        statusText = view.findViewById(R.id.networkStatus)
        downloadButton = view.findViewById(R.id.downloadButton)
        downloadButton?.setOnClickListener { triggerDownload() }
    }

    override fun onDestroyView() {
        rxRow = null; rxDetail = null; txRow = null; txDetail = null
        appRxRow = null; appRxDetail = null; appTxRow = null; appTxDetail = null
        statusText = null; downloadButton = null
        super.onDestroyView()
    }

    fun update(info: NetworkInfo) {
        val act = activity ?: return
        act.runOnUiThread {
            if (info == NetworkInfo.NOT_SUPPORTED) {
                rxDetail?.text = "Not supported on this device"
                txDetail?.text = "Not supported on this device"
                return@runOnUiThread
            }
            peakRx = maxOf(peakRx, info.rxSystemTotalInMb)
            peakTx = maxOf(peakTx, info.txSystemTotalInMb)

            val scale = maxOf(peakRx, peakTx, 1f)
            rxRow?.fraction = (info.rxSystemTotalInMb / scale).coerceIn(0f, 1f)
            rxRow?.valueText = formatSpeed(info.rxSystemTotalInMb)
            txRow?.fraction = (info.txSystemTotalInMb / scale).coerceIn(0f, 1f)
            txRow?.valueText = formatSpeed(info.txSystemTotalInMb)
            rxDetail?.text = "%.3f MB/interval   peak %.2f MB".format(info.rxSystemTotalInMb, peakRx)
            txDetail?.text = "%.3f MB/interval   peak %.2f MB".format(info.txSystemTotalInMb, peakTx)

            val appScale = maxOf(info.rxAppInMb, info.txAppInMb, 0.1f)
            appRxRow?.fraction = (info.rxAppInMb / appScale).coerceIn(0f, 1f)
            appRxRow?.valueText = formatSpeed(info.rxAppInMb)
            appTxRow?.fraction = (info.txAppInMb / appScale).coerceIn(0f, 1f)
            appTxRow?.valueText = formatSpeed(info.txAppInMb)
            appRxDetail?.text = "%.3f MB/interval".format(info.rxAppInMb)
            appTxDetail?.text = "%.3f MB/interval".format(info.txAppInMb)
        }
    }

    private fun triggerDownload() {
        Konitor.logEvent("network_download_test")
        val act = activity ?: return
        statusText?.text = "Fetching 20 MB…"
        downloadButton?.isEnabled = false
        Executors.newSingleThreadExecutor().submit {
            runCatching {
                URL("https://speed.cloudflare.com/__down?bytes=20000000")
                    .openStream().use { it.readBytes() }
            }
            act.runOnUiThread {
                if (isAdded) {
                    statusText?.text = "Done"
                    downloadButton?.isEnabled = true
                }
            }
        }
    }

    private fun formatSpeed(mb: Float): String = when {
        mb >= 1f -> "%.2f MB".format(mb)
        mb >= 0.01f -> "%.0f KB".format(mb * 1024)
        else -> "< 10 KB"
    }
}
