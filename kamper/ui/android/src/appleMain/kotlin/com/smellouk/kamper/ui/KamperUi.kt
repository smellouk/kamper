@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.smellouk.kamper.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.smellouk.kamper.ui.compose.KamperChip
import com.smellouk.kamper.ui.compose.KamperPanel
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIModalPresentationOverCurrentContext
import platform.UIKit.UIModalTransitionStyleCrossDissolve
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController

actual object KamperUi {
    // VC frame covers the full expanded width; peek is achieved by setting x so most is off-screen
    private const val CHIP_WIDTH = 200.0
    private const val CHIP_HEIGHT = 120.0
    private const val PEEK_WIDTH = 56.0  // visible strip in peek state
    private const val PREF_CHIP_Y = "kamper_chip_y"
    private const val PREF_ON_RIGHT = "kamper_on_right"

    internal var config: KamperUiConfig = KamperUiConfig()
    internal var repository: KamperUiRepository? = null
    private var chipVC: UIViewController? = null
    private var chipState by mutableStateOf(ChipState.PEEK)
    private var chipX = 0.0   // VC frame left edge (screen coords)
    private var chipY = 0.0
    private var onRightSide = true
    private var screenW = 0.0
    private var screenH = 0.0

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
        chipY = if (savedY != 0.0) {
            savedY.coerceIn(0.0, screenH - CHIP_HEIGHT)
        } else {
            when (config.position) {
                ChipPosition.TOP_START, ChipPosition.TOP_END -> 60.0
                ChipPosition.CENTER_START, ChipPosition.CENTER_END -> screenH / 2 - CHIP_HEIGHT / 2
                ChipPosition.BOTTOM_START, ChipPosition.BOTTOM_END -> screenH - CHIP_HEIGHT - 34.0
            }
        }
        chipX = if (onRightSide) screenW - PEEK_WIDTH else -(CHIP_WIDTH - PEEK_WIDTH)

        val vc = ComposeUIViewController {
            val s by repo.state.collectAsState()
            val cfg by repo.settings.collectAsState()
            KamperChip(
                state = s,
                settings = cfg,
                chipState = chipState,
                onClick = {
                    when (chipState) {
                        ChipState.PEEK -> expandChip()
                        ChipState.EXPANDED -> openPanel(rootVC, repo)
                    }
                },
                onDrag = { dx, dy ->
                    chipX += dx
                    chipY = (chipY + dy).coerceIn(0.0, screenH - CHIP_HEIGHT)
                    chipVC?.view?.setFrame(CGRectMake(chipX, chipY, CHIP_WIDTH, CHIP_HEIGHT))
                },
                onDragEnd = { snapToEdge() }
            )
        }

        rootVC.view.addSubview(vc.view)
        vc.view.setFrame(CGRectMake(chipX, chipY, CHIP_WIDTH, CHIP_HEIGHT))
        chipVC = vc

        startShakeDetection()
    }

    actual fun detach() {
        stopShakeDetection()
        chipVC?.view?.removeFromSuperview()
        chipVC = null
        repository?.clear()
        repository = null
    }

    private fun snapToEdge() {
        chipState = ChipState.PEEK
        val chipCenter = chipX + CHIP_WIDTH / 2
        onRightSide = chipCenter > screenW / 2
        chipX = if (onRightSide) screenW - PEEK_WIDTH else -(CHIP_WIDTH - PEEK_WIDTH)
        chipVC?.view?.setFrame(CGRectMake(chipX, chipY, CHIP_WIDTH, CHIP_HEIGHT))
        NSUserDefaults.standardUserDefaults.apply {
            setDouble(chipY, PREF_CHIP_Y)
            setBool(onRightSide, PREF_ON_RIGHT)
        }
    }

    internal fun expandChip() {
        chipState = ChipState.EXPANDED
        chipX = if (onRightSide) screenW - CHIP_WIDTH else 0.0
        chipVC?.view?.setFrame(CGRectMake(chipX, chipY, CHIP_WIDTH, CHIP_HEIGHT))
    }

    internal fun openPanel(parent: UIViewController, repo: KamperUiRepository) {
        val panelVC = ComposeUIViewController {
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
                onDismiss            = { parent.dismissViewControllerAnimated(true, null) }
            )
        }
        panelVC.modalPresentationStyle = UIModalPresentationOverCurrentContext
        panelVC.modalTransitionStyle = UIModalTransitionStyleCrossDissolve
        parent.presentViewController(panelVC, animated = true, completion = null)
    }
}

expect fun startShakeDetection()
expect fun stopShakeDetection()
