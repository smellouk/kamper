@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.macos.ui

import kotlinx.cinterop.*
import platform.AppKit.*
import platform.Foundation.*
import platform.darwin.NSObject

class ActionTarget(val handler: () -> Unit) : NSObject() {
    @ObjCAction
    fun invoke(sender: NSObject?) {
        handler()
    }
}

fun makeButton(title: String, target: ActionTarget): NSButton {
    val btn = NSButton()
    btn.title = title
    btn.bezelStyle = NSBezelStyleRounded
    btn.setButtonType(NSButtonTypeMomentaryLight)
    btn.translatesAutoresizingMaskIntoConstraints = false
    btn.target = target
    btn.action = NSSelectorFromString("invoke:")
    return btn
}
