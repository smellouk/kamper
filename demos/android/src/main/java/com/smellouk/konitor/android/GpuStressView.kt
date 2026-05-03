package com.smellouk.konitor.android

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Square container that hosts a [GLSurfaceView] while stress is active.
 * A new [GLSurfaceView] is created each time [start] is called so that
 * [GLSurfaceView.setRenderer] can be invoked on a fresh instance — the
 * GLSurfaceView API forbids calling setRenderer more than once per instance.
 */
internal class GpuStressView : FrameLayout {
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = -1,
        defStyleRes: Int = -1
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    private var glView: GLSurfaceView? = null

    fun start() {
        val sv = GLSurfaceView(context).also {
            it.setEGLContextClientVersion(GL_ES_VERSION)
            it.setRenderer(GpuStressRenderer())
            it.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        glView = sv
        addView(sv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        visibility = VISIBLE
        sv.onResume()
    }

    fun stop() {
        glView?.onPause()
        removeAllViews()
        glView = null
        visibility = GONE
    }

    fun resumeRenderer() { glView?.onResume() }
    fun pauseRenderer()  { glView?.onPause() }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredWidth)
    }

    private companion object {
        const val GL_ES_VERSION = 2
    }
}
