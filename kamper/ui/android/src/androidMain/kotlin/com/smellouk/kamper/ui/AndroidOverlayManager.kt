package com.smellouk.kamper.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.smellouk.kamper.ui.KamperUiSettings
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.doOnLayout
import com.smellouk.kamper.ui.compose.KamperChip
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

internal class AndroidOverlayManager(
    private val app: Application,
    private val state: StateFlow<KamperUiState>,
    private val settings: StateFlow<KamperUiSettings>,
    private val config: KamperUiConfig
) {
    private val density = app.resources.displayMetrics.density
    private val peekWidthPx get() = (PEEK_WIDTH_DP * density).toInt()
    private val screenW get() = app.resources.displayMetrics.widthPixels
    private val screenH get() = app.resources.displayMetrics.heightPixels

    private var chipState by mutableStateOf(ChipState.PEEK)
    private var mirrorLayout by mutableStateOf(false)
    // chipX/chipY = "home" = the fully-expanded left-edge of the chip.
    // translationX offsets from home for peek/expand (avoids FrameLayout width squeezing).
    private var chipX = 0
    private var chipY = 0
    private var chipW = 0
    private var chipH = 0
    private var onRightSide = true
    private var initialized = false

    private val prefs: SharedPreferences =
        app.getSharedPreferences("kamper_ui_prefs", Context.MODE_PRIVATE)

    private var currentAnimator: ValueAnimator? = null
    private var chipView: View? = null
    private var shakeDetector: ShakeDetector? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoCollapseRunnable = Runnable { collapseChip() }
    private var panelOpened = false

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) = attachToActivity(activity)
        override fun onActivityPaused(activity: Activity) = detachFromActivity(activity)
        override fun onActivityCreated(activity: Activity, b: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, b: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    fun show() {
        app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        shakeDetector = ShakeDetector(app) { expandChip() }
        shakeDetector?.start()
    }

    fun hide() {
        mainHandler.removeCallbacks(autoCollapseRunnable)
        shakeDetector?.stop()
        shakeDetector = null
        app.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    private fun attachToActivity(activity: Activity) {
        if (activity is KamperPanelActivity) return
        val root = activity.window.decorView as? ViewGroup ?: return
        if (root.findViewWithTag<View>(OVERLAY_TAG) != null) return

        val view = ComposeView(activity).apply {
            tag = OVERLAY_TAG
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val s by state.collectAsState()
                val cfg by settings.collectAsState()
                KamperChip(
                    state = s,
                    settings = cfg,
                    mirrorLayout = mirrorLayout,
                    onClick = {
                        when (chipState) {
                            ChipState.PEEK -> expandChip()
                            ChipState.EXPANDED -> {
                                mainHandler.removeCallbacks(autoCollapseRunnable)
                                panelOpened = true
                                activity.startActivity(
                                    Intent(activity, KamperPanelActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                )
                            }
                        }
                    },
                    onDrag = { dx, dy -> chipView?.let { v -> onDrag(v, dx, dy) } },
                    onDragEnd = { chipView?.let { v -> onDragEnd(v) } }
                )
            }
        }

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START
        ).apply {
            leftMargin = if (initialized) chipX else 0
            topMargin = if (initialized) chipY else 0
        }
        if (!initialized) view.alpha = 0f
        root.addView(view, lp)
        chipView = view

        view.doOnLayout { v ->
            chipW = v.width
            chipH = v.height
            if (!initialized) {
                initPosition()
                initialized = true
                (v.layoutParams as? FrameLayout.LayoutParams)?.let {
                    it.leftMargin = chipX
                    it.topMargin = chipY
                }
                v.requestLayout()
                v.post { v.alpha = 1f }
            }
            // Restore visual position via translationX (no layout squeezing)
            v.translationX = peekTranslationX()
            // Restart auto-collapse if returning from panel while chip is still expanded
            if (panelOpened && chipState == ChipState.EXPANDED) {
                panelOpened = false
                mainHandler.removeCallbacks(autoCollapseRunnable)
                mainHandler.postDelayed(autoCollapseRunnable, AUTO_COLLAPSE_MS)
            }
        }
    }

    private fun detachFromActivity(activity: Activity) {
        val root = activity.window.decorView as? ViewGroup ?: return
        root.findViewWithTag<View>(OVERLAY_TAG)?.let { v ->
            if (chipView == v) chipView = null
            root.removeView(v)
        }
    }

    private fun initPosition() {
        val savedY = prefs.getInt(PREF_CHIP_Y, -1)
        val savedRight = prefs.getBoolean(PREF_ON_RIGHT, true)

        onRightSide = savedRight
        mirrorLayout = !onRightSide
        chipX = if (onRightSide) screenW - chipW else 0
        chipY = if (savedY >= 0) {
            savedY.coerceIn(0, screenH - chipH)
        } else {
            when (config.position) {
                ChipPosition.TOP_START, ChipPosition.TOP_END ->
                    (STATUS_BAR_DP * density + MARGIN_DP * density).toInt()
                ChipPosition.CENTER_START, ChipPosition.CENTER_END ->
                    (screenH - chipH) / 2
                ChipPosition.BOTTOM_START, ChipPosition.BOTTOM_END ->
                    screenH - chipH - (BOTTOM_MARGIN_DP * density).toInt()
            }
        }
        chipState = ChipState.PEEK
    }

    // Peek offset from home: positive = shift right (right-side peek), negative = shift left (left-side peek)
    private fun peekTranslationX() = when {
        chipState == ChipState.EXPANDED -> 0f
        onRightSide -> (chipW - peekWidthPx).toFloat()
        else -> -(chipW - peekWidthPx).toFloat()
    }

    private fun expandChip() {
        chipState = ChipState.EXPANDED
        animateTranslationX(chipView ?: return, 0f)
        // Auto-collapse after delay if user doesn't open panel
        mainHandler.removeCallbacks(autoCollapseRunnable)
        mainHandler.postDelayed(autoCollapseRunnable, AUTO_COLLAPSE_MS)
    }

    private fun collapseChip() {
        chipState = ChipState.PEEK
        animateTranslationX(chipView ?: return, peekTranslationX())
    }

    private fun onDrag(view: View, dx: Float, dy: Float) {
        currentAnimator?.cancel()
        mainHandler.removeCallbacks(autoCollapseRunnable)
        // translationX/Y for drag — no layout changes, no squeezing
        view.translationX += dx
        view.translationY = (view.translationY + dy)
            .coerceIn(-chipY.toFloat(), (screenH - chipH - chipY).toFloat())
    }

    private fun onDragEnd(view: View) {
        val absX = (chipX + view.translationX).toInt()
        val absY = (chipY + view.translationY).toInt().coerceIn(0, screenH - chipH)

        onRightSide = absX + chipW / 2 > screenW / 2
        mirrorLayout = !onRightSide

        // New home = expanded position on chosen edge
        chipX = if (onRightSide) screenW - chipW else 0
        chipY = absY

        // Bake Y into layout, clear translationY
        view.translationY = 0f
        (view.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.leftMargin = chipX
            it.topMargin = chipY
        }
        view.requestLayout()

        // translationX relative to new home, then snap to peek
        view.translationX = (absX - chipX).toFloat()
        prefs.edit().putInt(PREF_CHIP_Y, chipY).putBoolean(PREF_ON_RIGHT, onRightSide).apply()
        chipState = ChipState.PEEK
        animateTranslationX(view, peekTranslationX())
    }

    private fun animateTranslationX(view: View, to: Float) {
        currentAnimator?.cancel()
        val from = view.translationX
        currentAnimator = ValueAnimator.ofFloat(from, to).apply {
            duration = ANIM_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { view.translationX = it.animatedValue as Float }
            start()
        }
    }

    private companion object {
        const val OVERLAY_TAG = "kamper_chip_overlay"
        const val PREF_CHIP_Y = "chip_y"
        const val PREF_ON_RIGHT = "on_right"
        const val PEEK_WIDTH_DP = 56f
        const val STATUS_BAR_DP = 24f
        const val MARGIN_DP = 8f
        const val BOTTOM_MARGIN_DP = 34f
        const val ANIM_DURATION_MS = 280L
        const val AUTO_COLLAPSE_MS = 3_000L
    }
}

private class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    fun start() {
        sm?.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() = sm?.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        if (sqrt((x * x + y * y + z * z).toDouble()).toFloat() > SHAKE_THRESHOLD) onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    private companion object {
        const val SHAKE_THRESHOLD = 20f
    }
}
