package com.smellouk.kamper.samples

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule

class FpsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fps)
        lifecycle.addObserver(Kamper)
        val infoTxt = findViewById<TextView>(R.id.infoTxt)

        Kamper.apply {
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