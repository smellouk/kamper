@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.konitor.ios

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.CpuModule
import com.smellouk.konitor.fps.FpsInfo
import com.smellouk.konitor.fps.FpsModule
import com.smellouk.konitor.gc.GcInfo
import com.smellouk.konitor.gc.GcModule
import com.smellouk.konitor.gpu.GpuInfo
import com.smellouk.konitor.gpu.GpuModule
import com.smellouk.konitor.issues.AnrConfig
import com.smellouk.konitor.issues.IssueInfo
import com.smellouk.konitor.issues.IssuesModule
import com.smellouk.konitor.jank.JankInfo
import com.smellouk.konitor.jank.JankModule
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.memory.MemoryModule
import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.network.NetworkModule
import com.smellouk.konitor.thermal.ThermalInfo
import com.smellouk.konitor.thermal.ThermalModule
import com.smellouk.konitor.api.UserEventInfo
import com.smellouk.konitor.ios.ui.*
import com.smellouk.konitor.ui.KonitorUi
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
        KonitorUi.attach()
    }
}

class RootViewController : UIViewController(nibName = null, bundle = null) {
    private lateinit var cpuVC:     CpuViewController
    private lateinit var gpuVC:     GpuViewController
    private lateinit var fpsVC:     FpsViewController
    private lateinit var memoryVC:  MemoryViewController
    private lateinit var eventsVC:  EventsViewController
    private lateinit var networkVC: NetworkViewController
    private lateinit var issuesVC:  IssuesViewController
    private lateinit var jankVC:    JankViewController
    private lateinit var gcVC:      GcViewController
    private lateinit var thermalVC: ThermalViewController
    private lateinit var tabVC:     UITabBarController

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.BASE

        cpuVC     = CpuViewController()
        gpuVC     = GpuViewController()
        fpsVC     = FpsViewController()
        memoryVC  = MemoryViewController()
        eventsVC  = EventsViewController()
        networkVC = NetworkViewController()
        issuesVC  = IssuesViewController()
        jankVC    = JankViewController()
        gcVC      = GcViewController()
        thermalVC = ThermalViewController()

        cpuVC.tabBarItem     = UITabBarItem(title = "CPU",     image = UIImage.systemImageNamed("cpu"),                            tag = 0)
        gpuVC.tabBarItem     = UITabBarItem(title = "GPU",     image = UIImage.systemImageNamed("display"),                        tag = 1)
        fpsVC.tabBarItem     = UITabBarItem(title = "FPS",     image = UIImage.systemImageNamed("play.circle"),                    tag = 2)
        memoryVC.tabBarItem  = UITabBarItem(title = "Memory",  image = UIImage.systemImageNamed("memorychip"),                     tag = 3)
        eventsVC.tabBarItem  = UITabBarItem(title = "Events",  image = UIImage.systemImageNamed("star.circle"),                    tag = 4)
        networkVC.tabBarItem = UITabBarItem(title = "Network", image = UIImage.systemImageNamed("network"),                        tag = 5)
        issuesVC.tabBarItem  = UITabBarItem(title = "Issues",  image = UIImage.systemImageNamed("exclamationmark.triangle"),       tag = 6)
        jankVC.tabBarItem    = UITabBarItem(title = "Jank",    image = UIImage.systemImageNamed("chart.line.uptrend.xyaxis"),      tag = 7)
        gcVC.tabBarItem      = UITabBarItem(title = "GC",      image = UIImage.systemImageNamed("arrow.triangle.2.circlepath"),    tag = 8)
        thermalVC.tabBarItem = UITabBarItem(title = "Thermal", image = UIImage.systemImageNamed("thermometer"),                    tag = 9)

        tabVC = UITabBarController()
        tabVC.setViewControllers(listOf(cpuVC, gpuVC, fpsVC, memoryVC, eventsVC, networkVC, issuesVC, jankVC, gcVC, thermalVC), animated = false)

        val appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = Theme.MANTLE
        tabVC.tabBar.standardAppearance   = appearance
        tabVC.tabBar.scrollEdgeAppearance = appearance
        tabVC.tabBar.tintColor              = Theme.BLUE
        tabVC.tabBar.unselectedItemTintColor = Theme.MUTED

        // Header view pinned to safe area top
        val headerH = 56.0
        val header = HeaderView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0))
        header.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(header)

        // Tab bar controller embedded as child below header
        addChildViewController(tabVC)
        tabVC.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tabVC.view)
        tabVC.didMoveToParentViewController(this)

        NSLayoutConstraint.activateConstraints(listOf(
            header.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor),
            header.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor),
            header.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor),
            header.heightAnchor.constraintEqualToConstant(headerH),

            tabVC.view.topAnchor.constraintEqualToAnchor(header.bottomAnchor),
            tabVC.view.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor),
            tabVC.view.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor),
            tabVC.view.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor)
        ))

        setupKonitor()
    }

    private fun setupKonitor() {
        Konitor.apply {
            install(CpuModule)
            install(GpuModule)
            install(FpsModule)
            install(MemoryModule())
            install(NetworkModule)
            install(IssuesModule(anr = AnrConfig()) { crash { chainToPreviousHandler = false } })
            install(JankModule)
            install(GcModule)
            install(ThermalModule)

            addInfoListener<CpuInfo>     { info -> dispatch_async(dispatch_get_main_queue()) { if (cpuVC.isViewLoaded())     cpuVC.update(info) } }
            addInfoListener<GpuInfo>     { info -> dispatch_async(dispatch_get_main_queue()) { if (gpuVC.isViewLoaded())     gpuVC.update(info) } }
            addInfoListener<FpsInfo>     { info -> dispatch_async(dispatch_get_main_queue()) { if (fpsVC.isViewLoaded())     fpsVC.update(info) } }
            addInfoListener<MemoryInfo>     { info -> dispatch_async(dispatch_get_main_queue()) { if (memoryVC.isViewLoaded())  memoryVC.update(info) } }
            addInfoListener<UserEventInfo>  { info -> dispatch_async(dispatch_get_main_queue()) { if (eventsVC.isViewLoaded())  eventsVC.addEvent(info) } }
            addInfoListener<NetworkInfo>    { info -> dispatch_async(dispatch_get_main_queue()) { if (networkVC.isViewLoaded()) networkVC.update(info) } }
            addInfoListener<IssueInfo>   { info -> dispatch_async(dispatch_get_main_queue()) { if (issuesVC.isViewLoaded()) issuesVC.addIssue(info.issue) } }
            addInfoListener<JankInfo>    { info -> dispatch_async(dispatch_get_main_queue()) { if (jankVC.isViewLoaded())    jankVC.update(info) } }
            addInfoListener<GcInfo>      { info -> dispatch_async(dispatch_get_main_queue()) { if (gcVC.isViewLoaded())      gcVC.update(info) } }
            addInfoListener<ThermalInfo> { info -> dispatch_async(dispatch_get_main_queue()) { if (thermalVC.isViewLoaded()) thermalVC.update(info) } }
        }
        Konitor.start()
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

        val pad = 16.0

        // Title — left-aligned
        val title = "K|iOS"
        val attrs = mapOf<Any?, Any?>(
            NSFontAttributeName            to Theme.HEADER_FONT,
            NSForegroundColorAttributeName to Theme.BLUE
        )
        val sz = (title as NSString).sizeWithAttributes(attrs)
        val ty = (h - sz.useContents { height }) / 2
        (title as NSString).drawAtPoint(CGPointMake(pad, ty), withAttributes = attrs)

        // Green dot — right-aligned
        val dotSize = 10.0
        val dotX = w - pad - dotSize
        val dotY = (h - dotSize) / 2.0
        Theme.GREEN.setFill()
        UIBezierPath.bezierPathWithOvalInRect(CGRectMake(dotX, dotY, dotSize, dotSize)).fill()
    }
}
