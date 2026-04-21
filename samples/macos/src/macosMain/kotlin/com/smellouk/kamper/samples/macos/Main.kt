@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.samples.macos

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule
import com.smellouk.kamper.samples.macos.ui.ActionTarget
import com.smellouk.kamper.samples.macos.ui.CpuView
import com.smellouk.kamper.samples.macos.ui.FpsView
import com.smellouk.kamper.samples.macos.ui.MemoryView
import com.smellouk.kamper.samples.macos.ui.NetworkView
import com.smellouk.kamper.samples.macos.ui.Theme
import kotlinx.cinterop.*
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*
import platform.darwin.NSObject

fun main() {
    val app = NSApplication.sharedApplication()
    app.setActivationPolicy(NSApplicationActivationPolicy.NSApplicationActivationPolicyRegular)
    val delegate = AppDelegate()
    app.delegate = delegate
    app.run()
}

class AppDelegate : NSObject(), NSApplicationDelegateProtocol {
    private var window: KamperDemoWindow? = null

    override fun applicationDidFinishLaunching(notification: NSNotification) {
        window = KamperDemoWindow()
        window?.makeKeyAndOrderFront(null)
        NSApplication.sharedApplication().activateIgnoringOtherApps(true)
    }

    override fun applicationShouldTerminateAfterLastWindowClosed(sender: NSApplication): Boolean = true
}

class KamperDemoWindow : NSWindow(
    contentRect = NSMakeRect(0.0, 0.0, 680.0, 520.0),
    styleMask = NSWindowStyleMaskTitled or NSWindowStyleMaskClosable or
            NSWindowStyleMaskMiniaturizable or NSWindowStyleMaskResizable,
    backing = NSBackingStoreBuffered,
    defer = false
) {
    private val cpuView     = CpuView(NSMakeRect(0.0, 0.0, 0.0, 0.0))
    private val fpsView     = FpsView(NSMakeRect(0.0, 0.0, 0.0, 0.0))
    private val memoryView  = MemoryView(NSMakeRect(0.0, 0.0, 0.0, 0.0))
    private val networkView = NetworkView(NSMakeRect(0.0, 0.0, 0.0, 0.0))
    private var tabSwitchTarget: ActionTarget? = null

    init {
        title = "K|macOS"
        minSize = NSSizeFromString("{560, 440}")
        backgroundColor = Theme.BASE
        contentView?.let { setupContent(it) }
        center()
        setupKamper()
    }

    private fun setupContent(content: NSView) {
        content.wantsLayer = true

        val header  = HeaderView(NSMakeRect(0.0, 0.0, 0.0, 52.0))
        val (seg, tabs) = buildTabView()

        header.translatesAutoresizingMaskIntoConstraints = false
        seg.translatesAutoresizingMaskIntoConstraints    = false
        tabs.translatesAutoresizingMaskIntoConstraints   = false

        content.addSubview(header)
        content.addSubview(seg)
        content.addSubview(tabs)

        NSLayoutConstraint.activateConstraints(listOf(
            header.topAnchor.constraintEqualToAnchor(content.topAnchor),
            header.leadingAnchor.constraintEqualToAnchor(content.leadingAnchor),
            header.trailingAnchor.constraintEqualToAnchor(content.trailingAnchor),
            header.heightAnchor.constraintEqualToConstant(52.0),

            seg.topAnchor.constraintEqualToAnchor(header.bottomAnchor, constant = 12.0),
            seg.centerXAnchor.constraintEqualToAnchor(content.centerXAnchor),
            seg.heightAnchor.constraintEqualToConstant(28.0),

            tabs.topAnchor.constraintEqualToAnchor(seg.bottomAnchor, constant = 12.0),
            tabs.leadingAnchor.constraintEqualToAnchor(content.leadingAnchor),
            tabs.trailingAnchor.constraintEqualToAnchor(content.trailingAnchor),
            tabs.bottomAnchor.constraintEqualToAnchor(content.bottomAnchor)
        ))
    }

    private fun buildTabView(): Pair<NSSegmentedControl, NSTabView> {
        val tabs = NSTabView(NSMakeRect(0.0, 0.0, 0.0, 0.0))
        tabs.tabViewType = NSNoTabsNoBorder

        fun addTab(label: String, view: NSView) {
            val tabItem = NSTabViewItem()
            tabItem.label = label
            val itemView = tabItem.view ?: return
            view.translatesAutoresizingMaskIntoConstraints = false
            itemView.addSubview(view)
            NSLayoutConstraint.activateConstraints(listOf(
                view.topAnchor.constraintEqualToAnchor(itemView.topAnchor),
                view.leadingAnchor.constraintEqualToAnchor(itemView.leadingAnchor),
                view.trailingAnchor.constraintEqualToAnchor(itemView.trailingAnchor),
                view.bottomAnchor.constraintEqualToAnchor(itemView.bottomAnchor)
            ))
            tabs.addTabViewItem(tabItem)
        }

        addTab("CPU",     cpuView)
        addTab("FPS",     fpsView)
        addTab("Memory",  memoryView)
        addTab("Network", networkView)

        val seg = NSSegmentedControl()
        seg.segmentCount = 4
        seg.setLabel("CPU",     forSegment = 0)
        seg.setLabel("FPS",     forSegment = 1)
        seg.setLabel("Memory",  forSegment = 2)
        seg.setLabel("Network", forSegment = 3)
        seg.selectedSegment = 0
        seg.segmentStyle = NSSegmentStyleRounded
        seg.trackingMode = NSSegmentSwitchTrackingSelectOne

        tabSwitchTarget = ActionTarget {
            tabs.selectTabViewItemAtIndex(seg.selectedSegment)
        }
        seg.target = tabSwitchTarget
        seg.action = NSSelectorFromString("invoke:")

        return seg to tabs
    }

    private fun setupKamper() {
        Kamper.apply {
            install(CpuModule)
            install(FpsModule)
            install(MemoryModule())
            install(NetworkModule)

            addInfoListener<CpuInfo>     { cpuView.update(it) }
            addInfoListener<FpsInfo>     { fpsView.update(it) }
            addInfoListener<MemoryInfo>  { memoryView.update(it) }
            addInfoListener<NetworkInfo> { networkView.update(it) }
        }
        Kamper.start()
    }
}

class HeaderView : NSView {

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        wantsLayer = true
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        val w = bounds.useContents { size.width }
        val h = bounds.useContents { size.height }

        val gradient = NSGradient(startingColor = Theme.MANTLE, endingColor = Theme.BASE)
        gradient?.drawInRect(bounds, angle = 0.0)

        Theme.SURFACE0.setFill()
        NSBezierPath.bezierPathWithRect(NSMakeRect(0.0, 0.0, w, 1.0)).fill()

        val title = "Kamper Performance Monitor"
        val attrs: Map<Any?, Any?> = mapOf(NSFontAttributeName to Theme.HEADER_FONT, NSForegroundColorAttributeName to Theme.BLUE)
        val strSize = (title as NSString).sizeWithAttributes(attrs)
        val strW = strSize.useContents { width }
        val strH = strSize.useContents { height }
        val tx = (w - strW) / 2
        val ty = (h - strH) / 2
        (title as NSString).drawAtPoint(NSMakePoint(tx, ty), withAttributes = attrs)

        val dotX = (w + strW) / 2 + 8
        val dotY = (h - 8.0) / 2.0
        Theme.GREEN.setFill()
        NSBezierPath.bezierPathWithOvalInRect(NSMakeRect(dotX, dotY, 8.0, 8.0)).fill()
    }
}
