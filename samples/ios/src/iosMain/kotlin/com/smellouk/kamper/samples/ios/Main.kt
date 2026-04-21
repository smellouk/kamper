@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.samples.ios

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule
import com.smellouk.kamper.samples.ios.ui.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

fun main() {
    UIApplicationMain(0, null, null, "AppDelegate")
}

class AppDelegate : NSObject(), UIApplicationDelegateProtocol {
    private var appWindow: UIWindow? = null

    override fun applicationDidFinishLaunching(application: UIApplication) {
        appWindow = UIWindow(frame = UIScreen.mainScreen.bounds)
        appWindow!!.rootViewController = RootViewController()
        appWindow!!.makeKeyAndVisible()
    }
}

class RootViewController : UITabBarController(nibName = null, bundle = null) {
    private val cpuVC     = CpuViewController()
    private val fpsVC     = FpsViewController()
    private val memoryVC  = MemoryViewController()
    private val networkVC = NetworkViewController()

    override fun viewDidLoad() {
        super.viewDidLoad()

        cpuVC.tabBarItem     = UITabBarItem(title = "CPU",     image = null, tag = 0)
        fpsVC.tabBarItem     = UITabBarItem(title = "FPS",     image = null, tag = 1)
        memoryVC.tabBarItem  = UITabBarItem(title = "Memory",  image = null, tag = 2)
        networkVC.tabBarItem = UITabBarItem(title = "Network", image = null, tag = 3)

        setViewControllers(listOf(cpuVC, fpsVC, memoryVC, networkVC), animated = false)

        tabBar.barTintColor           = Theme.MANTLE
        tabBar.tintColor              = Theme.BLUE
        tabBar.unselectedItemTintColor = Theme.MUTED

        setupKamper()
    }

    private fun setupKamper() {
        Kamper.apply {
            install(CpuModule)
            install(FpsModule)
            install(MemoryModule())
            install(NetworkModule)

            addInfoListener<CpuInfo>    { info -> dispatch_async(dispatch_get_main_queue()) { cpuVC.update(info) } }
            addInfoListener<FpsInfo>    { info -> dispatch_async(dispatch_get_main_queue()) { fpsVC.update(info) } }
            addInfoListener<MemoryInfo> { info -> dispatch_async(dispatch_get_main_queue()) { memoryVC.update(info) } }
            addInfoListener<NetworkInfo>{ info -> dispatch_async(dispatch_get_main_queue()) { networkVC.update(info) } }
        }
        Kamper.start()
    }
}

class HeaderView : UIView {
    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        backgroundColor = Theme.MANTLE
    }

    override fun drawRect(rect: CValue<CGRect>) {
        super.drawRect(rect)
        val w = bounds.useContents { size.width }
        val h = bounds.useContents { size.height }

        Theme.MANTLE.setFill()
        UIBezierPath.bezierPathWithRect(bounds).fill()

        // Bottom separator
        Theme.SURFACE0.setFill()
        UIBezierPath.bezierPathWithRect(CGRectMake(0.0, h - 1.0, w, 1.0)).fill()

        // Title
        val title = "K|iOS"
        val attrs = mapOf<Any?, Any?>(
            NSFontAttributeName            to Theme.HEADER_FONT,
            NSForegroundColorAttributeName to Theme.BLUE
        )
        val sz = (title as NSString).sizeWithAttributes(attrs)
        val tx = (w - sz.useContents { width  }) / 2
        val ty = (h - sz.useContents { height }) / 2
        (title as NSString).drawAtPoint(CGPointMake(tx, ty), withAttributes = attrs)

        // Green dot
        val strW = sz.useContents { width }
        val dotX = (w + strW) / 2 + 8.0
        val dotY = (h - 8.0) / 2.0
        Theme.GREEN.setFill()
        UIBezierPath.bezierPathWithOvalInRect(CGRectMake(dotX, dotY, 8.0, 8.0)).fill()
    }
}
