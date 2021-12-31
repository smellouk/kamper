package com.smellouk.kamper.samples

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule

class NetworkActivity : AppCompatActivity() {
    private val infoText: AppCompatTextView by lazy {
        findViewById(R.id.infoText)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network)
        lifecycle.addObserver(Kamper)
        Kamper.apply {
            install(NetworkModule)

            addInfoListener<NetworkInfo> { networkInfo ->
                if (networkInfo == NetworkInfo.INVALID) return@addInfoListener
                if (networkInfo == NetworkInfo.NOT_SUPPORTED) {
                    infoText.text = "Device not supported!"
                    return@addInfoListener
                }

                with(networkInfo) {
                    infoText.text = "Downloading Video \n(Check notification center)\n" +
                            "\nApp >\n" +
                            "▲ ${txAppInMb.toDecimal()}Mb / ▼ ${rxAppInMb.toDecimal()}Mb\n" +
                            "\nSystem >\n" +
                            "▲ ${txSystemTotalInMb.toDecimal()}Mb / ▼ ${rxSystemTotalInMb.toDecimal()}Mb"
                }
            }
        }

        requestStoragePermission()
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE
                )
            }
        } else {
            Utils.downloadBigBuckBunnyVideo(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        Utils.downloadBigBuckBunnyVideo(this)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Writing Permission Denied",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                return
            }
        }
    }

    fun onLinkOpen(view: View) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(Utils.videoUrl))
        startActivity(browserIntent)
    }
}

private const val PERMISSION_REQUEST_CODE = 1234