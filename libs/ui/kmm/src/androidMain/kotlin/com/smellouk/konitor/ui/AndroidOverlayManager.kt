package com.smellouk.konitor.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Density
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
import com.smellouk.konitor.ui.KonitorUiSettings
import com.smellouk.konitor.ui.compose.KonitorChip
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

internal class AndroidOverlayManager(
    private val app: Application,
    private val state: StateFlow<KonitorUiState>,
    private val settings: StateFlow<KonitorUiSettings>,
    private val config: KonitorUiConfig,
    private val onClearIssues: () -> Unit
) {
    private val density = app.resources.displayMetrics.density
    // D-07: runtime detection — no separate Gradle source set
    private val isLeanback: Boolean by lazy {
        app.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
    // D-13: pixel-space overscan margin (Compose LocalDensity does NOT shift FrameLayout coordinates — Pitfall 4)
    private val tvOverscanPx: Int get() = if (isLeanback) (TV_OVERSCAN_DP * density).toInt() else 0
    // On TV the chip is rendered at TV_SCALE_FACTOR× layout density, so the peek window must
    // also scale to show the same visual content width (label fits without clipping).
    private val peekWidthPx get() = (PEEK_WIDTH_DP * density * if (isLeanback) TV_SCALE_FACTOR * 0.75f else 1f).toInt()
    private val screenW get() = app.resources.displayMetrics.widthPixels
    private val screenH get() = app.resources.displayMetrics.heightPixels

    private var chipState by mutableStateOf(ChipState.PEEK)
    private var mirrorLayout by mutableStateOf(false)
    private var isTvFocused by mutableStateOf(false)
    // chipX/chipY = "home" = the fully-expanded left-edge of the chip.
    // translationX offsets from home for peek/expand (avoids FrameLayout width squeezing).
    private var chipX = 0
    private var chipY = 0
    private var chipW = 0
    private var chipH = 0
    private var onRightSide = true
    private var initialized = false

    private val prefs: SharedPreferences =
        app.getSharedPreferences("konitor_ui_prefs", Context.MODE_PRIVATE)

    private var currentAnimator: ValueAnimator? = null
    private var chipView: View? = null
    private var shakeDetector: ShakeDetector? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoCollapseRunnable = Runnable { collapseChip() }
    private var panelOpened = false
    private val overlayViews = mutableSetOf<View>()

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
        // D-03 parity: shake detection disabled on Android TV (no accelerometer-based shake on TV).
        if (!isLeanback) {
            shakeDetector = ShakeDetector(app) { expandChip() }
            shakeDetector?.start()
        }
    }

    fun hide() {
        mainHandler.removeCallbacks(autoCollapseRunnable)
        shakeDetector?.stop()
        shakeDetector = null
        app.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    private fun attachToActivity(activity: Activity) {
        if (activity is KonitorPanelActivity) return
        val root = activity.window.decorView as? ViewGroup ?: return
        if (overlayViews.any { it.parent == root }) return
        // Compose requires ViewTreeLifecycleOwner, which only ComponentActivity provides.
        // Non-ComponentActivity hosts (e.g. FlutterActivity) crash on attach — skip them.
        if (activity !is ComponentActivity) return

        val leanbackView: LeanbackComposeView? = if (isLeanback) LeanbackComposeView(activity) else null
        val view: View = leanbackView ?: ComposeView(activity)
        val composeTarget: ComposeView = leanbackView?.composeView ?: (view as ComposeView)
        composeTarget.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val s by state.collectAsState()
                val cfg by settings.collectAsState()
                val chipContent = @androidx.compose.runtime.Composable {
                    KonitorChip(
                        state = s,
                        settings = cfg,
                        chipState = chipState,
                        mirrorLayout = mirrorLayout,
                        onClick = {
                            mainHandler.post {
                                when (chipState) {
                                    ChipState.PEEK -> expandChip()
                                    ChipState.EXPANDED -> {
                                        mainHandler.removeCallbacks(autoCollapseRunnable)
                                        launchPanel(activity)
                                    }
                                }
                            }
                        },
                        onDrag    = if (!isLeanback) { dx, dy -> chipView?.let { v -> onDrag(v, dx, dy) } } else null,
                        onDragEnd = if (!isLeanback) { { chipView?.let { v -> onDragEnd(v) } } } else null,
                        isTv      = isLeanback,
                        hasTvFocus = isTvFocused
                    )
                }
                if (isLeanback) {
                    // D-12: Scale layout dp for 10-foot readability; keep fontScale at system value
                    // so text matches tab/system text size (not double-scaled).
                    val scaledDensity = Density(
                        density = LocalDensity.current.density * TV_SCALE_FACTOR,
                        fontScale = LocalDensity.current.fontScale
                    )
                    CompositionLocalProvider(LocalDensity provides scaledDensity) { chipContent() }
                } else {
                    chipContent()
                }
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
        // Re-check at the point of mutation: a rapid pause/resume can pass the early guard twice
        if (overlayViews.any { it.parent == root }) return
        root.addView(view, lp)
        overlayViews.add(view)
        chipView = view
        // Pitfall 3: ComposeView in DecorView FrameLayout requires explicit focus request to receive
        // key events on Android TV. Request focus after window gains focus so DecorView's own
        // focus restoration (which fires at onWindowFocusChanged) doesn't steal it back.
        if (isLeanback) {
            view.setOnFocusChangeListener { _, hasFocus -> isTvFocused = hasFocus }
            root.viewTreeObserver.addOnWindowFocusChangeListener(
                object : ViewTreeObserver.OnWindowFocusChangeListener {
                    override fun onWindowFocusChanged(hasFocus: Boolean) {
                        if (hasFocus) {
                            chipView?.requestFocus()
                            try { root.viewTreeObserver.removeOnWindowFocusChangeListener(this) }
                            catch (_: Exception) {}
                        }
                    }
                }
            )
        }
        if (isLeanback && view is LeanbackComposeView) {
            view.onSelect = {
                mainHandler.post {
                    when (chipState) {
                        ChipState.PEEK -> expandChip()
                        ChipState.EXPANDED -> {
                            mainHandler.removeCallbacks(autoCollapseRunnable)
                            launchPanel(activity)
                        }
                    }
                }
            }
            view.onLongPressMenu = { mainHandler.post { launchPanel(activity) } }
        }

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
                v.doOnNextLayout { v.alpha = 1f }
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

    private fun launchPanel(activity: Activity) {
        panelOpened = true
        activity.startActivity(
            Intent(activity, KonitorPanelActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    private fun detachFromActivity(activity: Activity) {
        val root = activity.window.decorView as? ViewGroup ?: return
        val toRemove = overlayViews.filter { it.parent == root }
        toRemove.forEach { v ->
            try {
                root.removeView(v)
            } catch (_: Exception) { }
            overlayViews.remove(v)
            if (chipView == v) chipView = null
        }
    }

    private fun initPosition() {
        val savedY = prefs.getInt(PREF_CHIP_Y, -1)
        val savedRight = prefs.getBoolean(PREF_ON_RIGHT, true)

        // D-10 / D-13: on TV, position is fixed from config; chipX is anchored from edge with overscan margin.
        if (isLeanback) {
            onRightSide = isRightCorner(config.position)
        } else {
            onRightSide = savedRight
        }
        mirrorLayout = !onRightSide
        chipX = if (onRightSide) screenW - chipW else 0
        chipY = if (savedY >= 0 && !isLeanback) {
            savedY.coerceIn(0, screenH - chipH)
        } else {
            when (config.position) {
                ChipPosition.TOP_START, ChipPosition.TOP_END ->
                    (STATUS_BAR_DP * density + MARGIN_DP * density).toInt() + tvOverscanPx
                ChipPosition.CENTER_START, ChipPosition.CENTER_END ->
                    (screenH - chipH) / 2
                ChipPosition.BOTTOM_START, ChipPosition.BOTTOM_END ->
                    screenH - chipH - (BOTTOM_MARGIN_DP * density).toInt() - tvOverscanPx
            }
        }
        chipState = ChipState.PEEK
    }

    private fun isRightCorner(p: ChipPosition): Boolean = when (p) {
        ChipPosition.TOP_END, ChipPosition.CENTER_END, ChipPosition.BOTTOM_END -> true
        ChipPosition.TOP_START, ChipPosition.CENTER_START, ChipPosition.BOTTOM_START -> false
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
        const val PREF_CHIP_Y = "chip_y"
        const val PREF_ON_RIGHT = "on_right"
        const val PEEK_WIDTH_DP = 56f
        const val STATUS_BAR_DP = 24f
        const val MARGIN_DP = 8f
        const val BOTTOM_MARGIN_DP = 34f
        const val ANIM_DURATION_MS = 280L
        const val AUTO_COLLAPSE_MS = 3_000L
        const val TV_SCALE_FACTOR = 1.5f   // D-12: 1.5x chosen over 2x to avoid corner overflow
        const val TV_OVERSCAN_DP  = 48f    // D-13: title-safe area margin
    }
}

/**
 * FrameLayout wrapper that hosts a [ComposeView] for the Konitor TV overlay and intercepts
 * D-pad and KEYCODE_MENU/KEYCODE_BACK events. Used only when [PackageManager.FEATURE_LEANBACK]
 * is present. [ComposeView] is final and cannot be subclassed, so a FrameLayout wrapper is used.
 *
 * D-09: D-pad Select cycles PEEK -> EXPANDED -> launchPanel.
 * D-11: KEYCODE_MENU long press (primary) or KEYCODE_BACK long press (Fire TV fallback)
 * launches KonitorPanelActivity directly.
 *
 * Long press is detected via ACTION_DOWN + repeatCount > 0 (RESEARCH.md Pitfall 5).
 *
 * The handler reads chipState/expandChip/launchPanel via a callback bag to avoid a back-reference
 * to AndroidOverlayManager (would create a cycle). The bag is set by the manager when the view
 * is created (see attachToActivity).
 */
internal class LeanbackComposeView(activity: Activity) : FrameLayout(activity) {
    val composeView: ComposeView = ComposeView(activity)
    var onSelect: (() -> Unit)? = null         // bound to AndroidOverlayManager.handleSelect()
    var onLongPressMenu: (() -> Unit)? = null  // bound to AndroidOverlayManager.launchPanel()

    init {
        addView(composeView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // D-09: confirm key (Select / DPAD_CENTER / Enter on numpad) on UP -> select handler
        // isConfirmKey added in API 22; min is 21, so use explicit key code check as fallback
        val isConfirm = event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER
            || event.keyCode == KeyEvent.KEYCODE_ENTER
            || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        if (isConfirm && event.action == KeyEvent.ACTION_UP) {
            onSelect?.invoke()
            return true
        }
        // D-11: KEYCODE_MENU or KEYCODE_BACK long press -> launch panel directly
        if ((event.keyCode == KeyEvent.KEYCODE_MENU || event.keyCode == KeyEvent.KEYCODE_BACK)
            && event.action == KeyEvent.ACTION_DOWN
            && event.repeatCount > 0) {
            onLongPressMenu?.invoke()
            return true
        }
        return super.dispatchKeyEvent(event)
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
