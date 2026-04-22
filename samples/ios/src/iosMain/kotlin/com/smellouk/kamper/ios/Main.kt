@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.ios

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.gc.GcInfo
import com.smellouk.kamper.gc.GcModule
import com.smellouk.kamper.issues.AnrConfig
import com.smellouk.kamper.issues.IssueInfo
import com.smellouk.kamper.issues.IssuesModule
import com.smellouk.kamper.jank.JankInfo
import com.smellouk.kamper.jank.JankModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule
import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalModule
import com.smellouk.kamper.ios.ui.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.Foundation.NSStringFromClass
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

fun main() {
    val delegateClass = NSStringFromClass(AppDelegate().`class`()!!)
    UIApplicationMain(0, null, null, delegateClass)
}

@OptIn(kotlin.experimental.ExperimentalObjCName::class)
@ObjCName("AppDelegate")
class AppDelegate : NSObject, UIApplicationDelegateProtocol {
    @OverrideInit constructor() : super()

    private var appWindow: UIWindow? = null

    override fun applicationDidFinishLaunching(application: UIApplication) {
        appWindow = UIWindow(frame = UIScreen.mainScreen.bounds)
        appWindow!!.rootViewController = RootViewController()
        appWindow!!.makeKeyAndVisible()
    }
}

class RootViewController : UITabBarController(nibName = null, bundle = null) {
    private lateinit var cpuVC:     CpuViewController
    private lateinit var fpsVC:     FpsViewController
    private lateinit var memoryVC:  MemoryViewController
    private lateinit var networkVC: NetworkViewController
    private lateinit var issuesVC:  IssuesViewController
    private lateinit var jankVC:    JankViewController
    private lateinit var gcVC:      GcViewController
    private lateinit var thermalVC: ThermalViewController

    override fun viewDidLoad() {
        super.viewDidLoad()

        cpuVC     = CpuViewController()
        fpsVC     = FpsViewController()
        memoryVC  = MemoryViewController()
        networkVC = NetworkViewController()
        issuesVC  = IssuesViewController()
        jankVC    = JankViewController()
        gcVC      = GcViewController()
        thermalVC = ThermalViewController()

        cpuVC.tabBarItem     = UITabBarItem(title = "CPU",     image = null, tag = 0)
        fpsVC.tabBarItem     = UITabBarItem(title = "FPS",     image = null, tag = 1)
        memoryVC.tabBarItem  = UITabBarItem(title = "Memory",  image = null, tag = 2)
        networkVC.tabBarItem = UITabBarItem(title = "Network", image = null, tag = 3)
        issuesVC.tabBarItem  = UITabBarItem(title = "Issues",  image = null, tag = 4)
        jankVC.tabBarItem    = UITabBarItem(title = "Jank",    image = null, tag = 5)
        gcVC.tabBarItem      = UITabBarItem(title = "GC",      image = null, tag = 6)
        thermalVC.tabBarItem = UITabBarItem(title = "Thermal", image = null, tag = 7)

        setViewControllers(listOf(cpuVC, fpsVC, memoryVC, networkVC, issuesVC, jankVC, gcVC, thermalVC), animated = false)

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
            install(IssuesModule(anr = AnrConfig()) { crash { chainToPreviousHandler = false } })
            install(JankModule)
            install(GcModule)
            install(ThermalModule)

            addInfoListener<CpuInfo>     { info -> dispatch_async(dispatch_get_main_queue()) { if (cpuVC.isViewLoaded())     cpuVC.update(info) } }
            addInfoListener<FpsInfo>     { info -> dispatch_async(dispatch_get_main_queue()) { if (fpsVC.isViewLoaded())     fpsVC.update(info) } }
            addInfoListener<MemoryInfo>  { info -> dispatch_async(dispatch_get_main_queue()) { if (memoryVC.isViewLoaded()) memoryVC.update(info) } }
            addInfoListener<NetworkInfo> { info -> dispatch_async(dispatch_get_main_queue()) { if (networkVC.isViewLoaded()) networkVC.update(info) } }
            addInfoListener<IssueInfo>   { info -> if (issuesVC.isViewLoaded()) issuesVC.addIssue(info.issue) }
            addInfoListener<JankInfo>    { info -> dispatch_async(dispatch_get_main_queue()) { if (jankVC.isViewLoaded())    jankVC.update(info) } }
            addInfoListener<GcInfo>      { info -> dispatch_async(dispatch_get_main_queue()) { if (gcVC.isViewLoaded())      gcVC.update(info) } }
            addInfoListener<ThermalInfo> { info -> dispatch_async(dispatch_get_main_queue()) { if (thermalVC.isViewLoaded()) thermalVC.update(info) } }
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
