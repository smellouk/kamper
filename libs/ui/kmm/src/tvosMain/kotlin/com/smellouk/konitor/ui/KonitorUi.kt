@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.smellouk.konitor.ui

// tvOS overlay actual.
// Differences vs appleMain/KonitorUi.kt:
//   - No drag (D-01): no ChipTouchView, no NSUserDefaults position persistence,
//     no snapToEdge, no dragStartChipX. Position fixed from config.position + 48dp overscan (D-13).
//   - D-pad input via UIKit pressesBegan/pressesEnded on a TvosChipViewController subclass
//     (Compose Modifier focusable() does NOT participate in the tvOS UIKit focus engine —
//      RESEARCH.md Pitfall 2).
//   - D-06 secondary trigger uses UIPressTypePlayPause long press (>= 500ms).
//     Menu long press is system-reserved by tvOS (RESEARCH.md Pitfall 1) and CANNOT be intercepted.
//   - Shake detection is a no-op (TvosSupport.kt provides the actuals — D-03).

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.smellouk.konitor.ui.compose.KonitorChip
import com.smellouk.konitor.ui.compose.KonitorPanel
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDate
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIModalPresentationOverCurrentContext
import platform.UIKit.UIModalTransitionStyleCrossDissolve
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIPressTypePlayPause
import platform.UIKit.UIPressTypeSelect
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowLevelNormal
import platform.UIKit.UIWindowScene

actual object KonitorUi {
    private const val CHIP_WIDTH = 200.0
    private const val CHIP_HEIGHT = 200.0
    private const val TV_OVERSCAN_DP = 48.0          // D-13: title-safe area margin
    private const val LONG_PRESS_THRESHOLD_MS = 500L // D-06

    internal var config: KonitorUiConfig = KonitorUiConfig()
    internal var repository: KonitorUiRepository? = null
    private var chipWindow: UIWindow? = null
    private var chipVC: UIViewController? = null
    internal var chipState by mutableStateOf(ChipState.PEEK)
    private var mirrorLayout by mutableStateOf(false)
    private var screenW = 0.0
    private var screenH = 0.0

    actual fun configure(block: KonitorUiConfig.() -> Unit) {
        config = KonitorUiConfig().apply(block)
    }

    actual fun attach() {
        if (!config.isEnabled) return
        val repo = KonitorUiRepository(maxRecordingSamples = config.maxRecordingSamples.coerceAtLeast(100))
            .also { repository = it }

        // tvOS 13+: scene-based window creation required. UIWindowScene is the modern API (RESEARCH.md Pattern 1).
        val scene = UIApplication.sharedApplication.connectedScenes
            .firstOrNull { it is UIWindowScene } as? UIWindowScene ?: return

        UIScreen.mainScreen.bounds.useContents {
            screenW = size.width; screenH = size.height
        }

        mirrorLayout = isLeftCorner(config.position)
        val chipX = cornerX(config.position, screenW)
        val chipY = cornerY(config.position, screenH)

        val composeVC = ComposeUIViewController(configure = {
            enforceStrictPlistSanityCheck = false; opaque = false
        }) {
            val s by repo.state.collectAsState()
            val cfg by repo.settings.collectAsState()
            // No drag (D-01); no focusable modifier on tvOS (Pitfall 2). All input via UIKit pressesBegan.
            KonitorChip(
                state = s,
                settings = cfg,
                chipState = chipState,
                mirrorLayout = mirrorLayout,
                onClick = {},
                onDrag = null,
                onDragEnd = null
            )
        }

        // Embed composeVC inside TvosChipViewController so we can override pressesBegan.
        val rootVC = TvosChipViewController(composeVC, repo)

        val window = UIWindow(frame = CGRectMake(chipX, chipY, CHIP_WIDTH, CHIP_HEIGHT))
        window.windowLevel = UIWindowLevelNormal + 1.0
        window.backgroundColor = UIColor.clearColor
        window.windowScene = scene
        window.rootViewController = rootVC
        window.setHidden(false)

        chipWindow = window
        chipVC = rootVC

        // D-03: shake disabled — TvosSupport.kt provides empty stubs already.
        startShakeDetection()  // no-op on tvOS
    }

    actual fun detach() {
        stopShakeDetection()  // no-op on tvOS
        chipWindow?.setHidden(true)
        chipWindow?.rootViewController = null
        chipWindow = null
        chipVC = null
        repository?.clear()
        repository = null
    }

    // Public facade — used by react-native-konitor iOS/tvOS TurboModule (Phase 14 output).
    fun show() = attach()
    actual fun hide() = detach()

    internal fun expandChip() {
        chipState = ChipState.EXPANDED
        // No frame change — tvOS chip is fixed corner; expanding is a Compose state change only.
    }

    internal fun openPanel(parent: UIViewController, repo: KonitorUiRepository) {
        chipWindow?.setHidden(true)
        val panelVC = ComposeUIViewController(configure = {
            enforceStrictPlistSanityCheck = false; opaque = false
        }) {
            KonitorPanel(
                state                = repo.state,
                settings             = repo.settings,
                isRecording          = repo.isRecording,
                recordingSampleCount = repo.recordingSampleCount,
                maxRecordingSamples  = repo.maxRecordingSamples,
                onSettingsChange     = { repo.updateSettings(it) },
                onClearIssues        = { repo.clearIssues() },
                onStartRecording     = { repo.startRecording() },
                onStopRecording      = { repo.stopRecording() },
                onExportTrace        = {},
                onStartEngine        = { repo.startEngine() },
                onStopEngine         = { repo.stopEngine() },
                onRestartEngine      = { repo.restartEngine() },
                onDismiss            = {
                    // D-04: tvOS Back/Menu dismiss is the standard modal-dismissal path.
                    // D-05 / D-14: chip auto-collapses to PEEK at corner.
                    parent.dismissViewControllerAnimated(true, completion = {
                        chipWindow?.setHidden(false)
                        chipState = ChipState.PEEK
                    })
                }
            )
        }
        panelVC.modalPresentationStyle = UIModalPresentationOverCurrentContext
        panelVC.modalTransitionStyle = UIModalTransitionStyleCrossDissolve
        parent.presentViewController(panelVC, animated = true, completion = null)
    }

    // D-13: 48dp overscan margin for title-safe area, applied to the chip's window frame.
    private fun isLeftCorner(p: ChipPosition): Boolean = when (p) {
        ChipPosition.TOP_START, ChipPosition.CENTER_START, ChipPosition.BOTTOM_START -> true
        ChipPosition.TOP_END, ChipPosition.CENTER_END, ChipPosition.BOTTOM_END -> false
    }

    private fun cornerX(p: ChipPosition, screenW: Double): Double = when (p) {
        ChipPosition.TOP_START, ChipPosition.CENTER_START, ChipPosition.BOTTOM_START ->
            TV_OVERSCAN_DP
        ChipPosition.TOP_END, ChipPosition.CENTER_END, ChipPosition.BOTTOM_END ->
            screenW - CHIP_WIDTH - TV_OVERSCAN_DP
    }

    private fun cornerY(p: ChipPosition, screenH: Double): Double = when (p) {
        ChipPosition.TOP_START, ChipPosition.TOP_END -> TV_OVERSCAN_DP
        ChipPosition.CENTER_START, ChipPosition.CENTER_END -> (screenH - CHIP_HEIGHT) / 2
        ChipPosition.BOTTOM_START, ChipPosition.BOTTOM_END ->
            screenH - CHIP_HEIGHT - TV_OVERSCAN_DP
    }
}

/**
 * Custom UIViewController hosting the Compose chip VC. Overrides pressesBegan/pressesEnded
 * to handle Siri Remote D-pad Select (D-02) and Play/Pause long press (D-06).
 *
 * RESEARCH.md Pitfall 1: Menu long press is system-reserved on tvOS — exits to home —
 * cannot be intercepted. Play/Pause is the only universally-available non-reserved button
 * for D-06 secondary trigger (Apple Forums thread/46438).
 *
 * RESEARCH.md Pitfall 2: Compose Modifier focusable() does NOT participate in tvOS UIKit
 * focus engine — focus and presses MUST be handled at the UIKit layer.
 */
private class TvosChipViewController(
    private val composeVC: UIViewController,
    private val repo: KonitorUiRepository
) : UIViewController(nibName = null, bundle = null) {

    private var playPausePressStartMs: Long = 0L

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor.clearColor
        // Embed the Compose chip VC as a child so it owns its own view tree.
        addChildViewController(composeVC)
        view.addSubview(composeVC.view)
        composeVC.view.setFrame(view.bounds)
        composeVC.didMoveToParentViewController(this)
    }

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        for (press in presses.filterIsInstance<UIPress>()) {
            when (press.type) {
                UIPressTypeSelect -> {
                    when (KonitorUi.chipState) {
                        ChipState.PEEK -> KonitorUi.expandChip()                       // D-02 first Select
                        ChipState.EXPANDED -> KonitorUi.openPanel(this, repo)          // D-02 second Select
                    }
                    return
                }
                UIPressTypePlayPause -> {
                    playPausePressStartMs = nowMs()                                   // D-06 start tracking
                    return
                }
                else -> { /* fall through to super */ }
            }
        }
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        for (press in presses.filterIsInstance<UIPress>()) {
            if (press.type == UIPressTypePlayPause) {
                val held = nowMs() - playPausePressStartMs
                playPausePressStartMs = 0L
                if (held >= 500L) {                                                    // D-06 long press threshold
                    KonitorUi.openPanel(this, repo)
                    return
                }
            }
        }
        super.pressesEnded(presses, withEvent)
    }

    private fun nowMs(): Long =
        (NSDate.date().timeIntervalSince1970 * 1000.0).toLong()
}
