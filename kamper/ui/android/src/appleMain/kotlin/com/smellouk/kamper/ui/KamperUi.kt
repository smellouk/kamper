@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.smellouk.kamper.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.smellouk.kamper.ui.compose.KamperChip
import com.smellouk.kamper.ui.compose.KamperPanel
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSUserDefaults
import platform.QuartzCore.CATransaction
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIEvent
import platform.UIKit.UIModalPresentationOverCurrentContext
import platform.UIKit.UIModalTransitionStyleCrossDissolve
import platform.UIKit.UIScreen
import platform.UIKit.UITouch
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowLevelNormal
import platform.UIKit.UIWindowScene

actual object KamperUi {
    // UIWindow is wider/taller than content so peek can hide the overflow off-screen.
    private const val CHIP_WIDTH = 200.0
    // Tall enough for all 8 metric rows; transparent extra space is invisible (opaque=false).
    private const val CHIP_HEIGHT = 200.0
    private const val PEEK_WIDTH = 56.0
    // Actual Compose content width: ROW_WIDTH_DP(128) + padding start(6) + padding end(6)
    private const val CONTENT_WIDTH = 140.0
    private const val PREF_CHIP_Y = "kamper_chip_y"
    private const val PREF_ON_RIGHT = "kamper_on_right"
    private const val AUTO_COLLAPSE_MS = 3_000L

    internal var config: KamperUiConfig = KamperUiConfig()
    internal var repository: KamperUiRepository? = null
    private var chipWindow: UIWindow? = null
    private var chipVC: UIViewController? = null
    private var chipState by mutableStateOf(ChipState.PEEK)
    private var mirrorLayout by mutableStateOf(false)
    private var chipX = 0.0
    private var chipY = 0.0
    private var onRightSide = true
    private var dragStartChipX = 0.0
    private var screenW = 0.0
    private var screenH = 0.0

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var autoCollapseJob: Job? = null

    actual fun configure(block: KamperUiConfig.() -> Unit) {
        config = KamperUiConfig().apply(block)
    }

    actual fun attach() {
        if (!config.isEnabled) return
        val repo = KamperUiRepository().also { repository = it }
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return

        UIScreen.mainScreen.bounds.useContents {
            screenW = size.width
            screenH = size.height
        }

        val defaults = NSUserDefaults.standardUserDefaults
        val savedY = defaults.doubleForKey(PREF_CHIP_Y)
        val savedRight = if (defaults.objectForKey(PREF_ON_RIGHT) != null)
            defaults.boolForKey(PREF_ON_RIGHT) else true

        onRightSide = savedRight
        mirrorLayout = !onRightSide
        chipY = if (savedY != 0.0) {
            savedY.coerceIn(0.0, screenH - CHIP_HEIGHT)
        } else {
            when (config.position) {
                ChipPosition.TOP_START, ChipPosition.TOP_END -> 60.0
                ChipPosition.CENTER_START, ChipPosition.CENTER_END -> screenH / 2 - CHIP_HEIGHT / 2
                ChipPosition.BOTTOM_START, ChipPosition.BOTTOM_END -> screenH - CHIP_HEIGHT - 34.0
            }
        }
        chipX = peekX()

        val vc = ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false; opaque = false }) {
            val s by repo.state.collectAsState()
            val cfg by repo.settings.collectAsState()
            // Interaction (tap + drag) is handled by ChipTouchView overlay below.
            KamperChip(
                state = s,
                settings = cfg,
                chipState = chipState,
                mirrorLayout = mirrorLayout,
                onClick = {},
                onDrag = null,
                onDragEnd = null
            )
        }

        val window = UIWindow(frame = CGRectMake(chipX, chipY, CHIP_WIDTH, CHIP_HEIGHT))
        window.windowLevel = UIWindowLevelNormal + 1.0
        window.backgroundColor = UIColor.clearColor

        val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene }
        if (scene is UIWindowScene) window.windowScene = scene

        window.rootViewController = vc
        window.setHidden(false)
        vc.view.backgroundColor = UIColor.clearColor

        // Transparent UIView overlay that intercepts all touches using UIKit's native touch
        // pipeline. Distinguishes tap from drag by movement threshold; calls callbacks directly.
        val touchView = ChipTouchView(frame = CGRectMake(0.0, 0.0, CHIP_WIDTH, CHIP_HEIGHT))
        touchView.onTap = {
            when (chipState) {
                ChipState.PEEK -> expandChip()
                ChipState.EXPANDED -> openPanel(rootVC, repo)
            }
        }
        touchView.onDrag = { dx, dy ->
            chipX += dx
            chipY = (chipY + dy).coerceIn(0.0, screenH - CHIP_HEIGHT)
            CATransaction.begin()
            CATransaction.setDisableActions(true)
            window.setFrame(CGRectMake(chipX, chipY, CHIP_WIDTH, CHIP_HEIGHT))
            CATransaction.commit()
        }
        touchView.onDragStart = { autoCollapseJob?.cancel(); dragStartChipX = chipX }
        touchView.onDragEnd = { snapToEdge() }
        touchView.chipWindow = window
        vc.view.addSubview(touchView)

        chipWindow = window
        chipVC = vc

        startShakeDetection()
    }

    actual fun detach() {
        autoCollapseJob?.cancel()
        stopShakeDetection()
        chipWindow?.setHidden(true)
        chipWindow?.rootViewController = null
        chipWindow = null
        chipVC = null
        repository?.clear()
        repository = null
    }

    private fun snapToEdge() {
        autoCollapseJob?.cancel()
        chipState = ChipState.PEEK
        val displacement = chipX - dragStartChipX
        onRightSide = when {
            displacement < -20.0 -> false  // dragged left → snap left
            displacement >  20.0 -> true   // dragged right → snap right
            else -> chipX + PEEK_WIDTH / 2 > screenW / 2  // tiny/no drag → nearest edge
        }
        mirrorLayout = !onRightSide
        chipX = peekX()
        chipWindow?.setFrame(CGRectMake(chipX, chipY, CHIP_WIDTH, CHIP_HEIGHT))
        val d = NSUserDefaults.standardUserDefaults
        d.setDouble(chipY, PREF_CHIP_Y)
        d.setBool(onRightSide, PREF_ON_RIGHT)
    }

    internal fun expandChip() {
        chipState = ChipState.EXPANDED
        chipX = if (onRightSide) screenW - CONTENT_WIDTH else 0.0
        chipWindow?.setFrame(CGRectMake(chipX, chipY, CHIP_WIDTH, CHIP_HEIGHT))
        autoCollapseJob?.cancel()
        autoCollapseJob = scope.launch {
            delay(AUTO_COLLAPSE_MS)
            snapToEdge()
        }
    }

    internal fun openPanel(parent: UIViewController, repo: KamperUiRepository) {
        autoCollapseJob?.cancel()
        chipWindow?.setHidden(true)
        val panelVC = ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false; opaque = false }) {
            KamperPanel(
                state                = repo.state,
                settings             = repo.settings,
                isRecording          = repo.isRecording,
                recordingSampleCount = repo.recordingSampleCount,
                onSettingsChange     = { repo.updateSettings(it) },
                onClearIssues        = { repo.clearIssues() },
                onStartRecording     = { repo.startRecording() },
                onStopRecording      = { repo.stopRecording() },
                onExportTrace        = {},
                onStartEngine        = { repo.startEngine() },
                onStopEngine         = { repo.stopEngine() },
                onRestartEngine      = { repo.restartEngine() },
                onDismiss            = {
                    parent.dismissViewControllerAnimated(true, completion = {
                        chipWindow?.setHidden(false)
                        snapToEdge()
                    })
                }
            )
        }
        panelVC.modalPresentationStyle = UIModalPresentationOverCurrentContext
        panelVC.modalTransitionStyle = UIModalTransitionStyleCrossDissolve
        parent.presentViewController(panelVC, animated = true, completion = null)
    }

    // Right-side peek: first PEEK_WIDTH of content visible, overflow off-screen right.
    // Left-side peek:  last PEEK_WIDTH of content visible, overflow off-screen left.
    private fun peekX() = if (onRightSide) screenW - PEEK_WIDTH else -(CONTENT_WIDTH - PEEK_WIDTH)
}

private const val DRAG_THRESHOLD = 8.0

// Transparent UIView overlay that handles tap and drag using UIKit's native touch system.
// Located at (0,0,CHIP_WIDTH,CHIP_HEIGHT) on top of the ComposeUIViewController's view.
// Screen position = window.frame.origin + locationInView(null), so deltas are always
// true screen-space movement even when setFrame repositions the window mid-drag.
private class ChipTouchView : UIView {
    @OverrideInit
    constructor(frame: CValue<CGRect>) : super(frame) {
        backgroundColor = UIColor.clearColor
    }

    var onTap: (() -> Unit)? = null
    var onDrag: ((dx: Double, dy: Double) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null
    var onDragStart: (() -> Unit)? = null
    var chipWindow: UIWindow? = null

    private var isDragging = false
    private var lastScreenX = 0.0
    private var lastScreenY = 0.0
    private var cumulativeDx = 0.0
    private var cumulativeDy = 0.0

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        isDragging = false; cumulativeDx = 0.0; cumulativeDy = 0.0
        val touch = touches.firstOrNull() as? UITouch ?: return
        val win = chipWindow ?: return
        val loc = touch.locationInView(null)
        lastScreenX = win.frame.useContents { origin.x } + loc.useContents { x }
        lastScreenY = win.frame.useContents { origin.y } + loc.useContents { y }
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        val touch = touches.firstOrNull() as? UITouch ?: return
        val win = chipWindow ?: return
        val loc = touch.locationInView(null)
        val sx = win.frame.useContents { origin.x } + loc.useContents { x }
        val sy = win.frame.useContents { origin.y } + loc.useContents { y }
        val dx = sx - lastScreenX
        val dy = sy - lastScreenY
        lastScreenX = sx; lastScreenY = sy
        cumulativeDx += dx; cumulativeDy += dy
        if (!isDragging && cumulativeDx * cumulativeDx + cumulativeDy * cumulativeDy > DRAG_THRESHOLD * DRAG_THRESHOLD) {
            isDragging = true
            onDragStart?.invoke()
        }
        if (isDragging) onDrag?.invoke(dx, dy)
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        if (isDragging) onDragEnd?.invoke() else onTap?.invoke()
        isDragging = false
    }

    override fun touchesCancelled(touches: Set<*>, withEvent: UIEvent?) {
        if (isDragging) onDragEnd?.invoke()
        isDragging = false
    }
}

expect fun startShakeDetection()
expect fun stopShakeDetection()
