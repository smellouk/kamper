package com.smellouk.konitor.android

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smellouk.konitor.Konitor
import com.smellouk.konitor.fps.FpsInfo
import com.smellouk.konitor.fps.FpsModule

class FpsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fps)
        lifecycle.addObserver(Konitor)
        val infoTxt = findViewById<TextView>(R.id.infoTxt)

        Konitor.apply {
            install(FpsModule)

            addInfoListener<FpsInfo> { fpsInfo ->
                if (fpsInfo == FpsInfo.INVALID) return@addInfoListener

                with(fpsInfo) {
                    infoTxt.text = "Fps: $fps"
                }
            }
        }
    }
}