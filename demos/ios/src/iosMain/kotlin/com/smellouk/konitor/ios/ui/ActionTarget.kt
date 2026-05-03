@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.konitor.ios.ui

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

class ActionTarget(val handler: () -> Unit) : NSObject() {
    @ObjCAction
    fun invoke(sender: NSObject?) {
        handler()
    }
}

fun makeButton(title: String, target: ActionTarget): UIButton {
    val btn = UIButton.buttonWithType(UIButtonTypeSystem)
    btn.setTitle(title, forState = UIControlStateNormal)
    btn.setTitleColor(Theme.BLUE, forState = UIControlStateNormal)
    btn.backgroundColor = Theme.SURFACE0
    btn.layer.cornerRadius = 8.0
    btn.contentEdgeInsets = UIEdgeInsetsMake(6.0, 12.0, 6.0, 12.0)
    btn.translatesAutoresizingMaskIntoConstraints = false
    btn.addTarget(target, NSSelectorFromString("invoke:"), forControlEvents = UIControlEventTouchUpInside)
    return btn
}
