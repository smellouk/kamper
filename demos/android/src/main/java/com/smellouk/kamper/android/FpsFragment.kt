package com.smellouk.kamper.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.smellouk.kamper.fps.FpsInfo

class FpsFragment : Fragment() {

    private var fpsLabel: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_fps, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fpsLabel = view.findViewById(R.id.fpsLabel)
    }

    override fun onDestroyView() {
        fpsLabel = null
        super.onDestroyView()
    }

    fun update(info: FpsInfo) {
        val act = activity ?: return
        act.runOnUiThread { fpsLabel?.text = info.fps.toString() }
    }
}
