@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.tvos

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.gc.GcInfo
import com.smellouk.kamper.gc.GcModule
import com.smellouk.kamper.gpu.GpuInfo
import com.smellouk.kamper.gpu.GpuModule
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
import com.smellouk.kamper.tvos.ui.*
import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSStringFromClass
import platform.Foundation.NSSelectorFromString
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
class AppDelegate : NSObject {
    @OverrideInit constructor() : super()

    private var appWindow: UIWindow? = null

    @ObjCAction
    fun applicationDidFinishLaunching(application: UIApplication) {
        appWindow = UIWindow(frame = CGRectMake(0.0, 0.0, 1920.0, 1080.0))
        appWindow!!.rootViewController = RootViewController()
        appWindow!!.makeKeyAndVisible()
    }
}

private val TAB_TITLES = listOf("CPU", "GPU", "FPS", "Memory", "Network", "Issues", "Jank", "GC", "Thermal")

class RootViewController : UIViewController(nibName = null, bundle = null) {
    private lateinit var cpuVC:     CpuViewController
    private lateinit var gpuVC:     GpuViewController
    private lateinit var fpsVC:     FpsViewController
    private lateinit var memoryVC:  MemoryViewController
    private lateinit var networkVC: NetworkViewController
    private lateinit var issuesVC:  IssuesViewController
    private lateinit var jankVC:    JankViewController
    private lateinit var gcVC:      GcViewController
    private lateinit var thermalVC: ThermalViewController

    private lateinit var segControl: UISegmentedControl
    private lateinit var segTarget: ActionTarget
    private var children: List<UIViewController> = emptyList()
    private var currentIndex = 0

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.BASE

        cpuVC     = CpuViewController()
        gpuVC     = GpuViewController()
        fpsVC     = FpsViewController()
        memoryVC  = MemoryViewController()
        networkVC = NetworkViewController()
        issuesVC  = IssuesViewController()
        jankVC    = JankViewController()
        gcVC      = GcViewController()
        thermalVC = ThermalViewController()
        children  = listOf(cpuVC, gpuVC, fpsVC, memoryVC, networkVC, issuesVC, jankVC, gcVC, thermalVC)

        val seg = UISegmentedControl(items = TAB_TITLES)
        seg.selectedSegmentIndex = 0
        seg.translatesAutoresizingMaskIntoConstraints = false
        seg.backgroundColor = Theme.MANTLE
        seg.selectedSegmentTintColor = Theme.SURFACE0
        seg.setTitleTextAttributes(mapOf<Any?, Any?>(NSForegroundColorAttributeName to Theme.TEXT), forState = UIControlStateNormal)
        seg.setTitleTextAttributes(mapOf<Any?, Any?>(NSForegroundColorAttributeName to Theme.BLUE), forState = UIControlStateSelected)
        segTarget = ActionTarget { switchTab(seg.selectedSegmentIndex.toInt()) }
        seg.addTarget(segTarget, NSSelectorFromString("invoke:"), forControlEvents = UIControlEventValueChanged)
        segControl = seg
        view.addSubview(seg)

        val sep = makeSeparator()
        view.addSubview(sep)

        val c = mutableListOf<NSLayoutConstraint>()
        c += seg.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor, constant = 20.0)
        c += seg.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = 80.0)
        c += seg.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -80.0)
        c += sep.topAnchor.constraintEqualToAnchor(seg.bottomAnchor, constant = 16.0)
        c += sep.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(2.0)
        NSLayoutConstraint.activateConstraints(c)

        switchTab(0)
        setupKamper()
    }

    private fun switchTab(index: Int) {
        val old = children.getOrNull(currentIndex)
        old?.willMoveToParentViewController(null)
        old?.view?.removeFromSuperview()
        old?.removeFromParentViewController()

        currentIndex = index
        val vc = children[index]
        addChildViewController(vc)
        vc.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(vc.view)

        val sep = view.subviews.filterIsInstance<UIView>().firstOrNull { it.backgroundColor == Theme.SURFACE0 }
        val topAnchor = sep?.bottomAnchor ?: view.safeAreaLayoutGuide.topAnchor

        val c = mutableListOf<NSLayoutConstraint>()
        c += vc.view.topAnchor.constraintEqualToAnchor(topAnchor)
        c += vc.view.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += vc.view.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += vc.view.bottomAnchor.constraintEqualToAnchor(view.bottomAnchor)
        NSLayoutConstraint.activateConstraints(c)
        vc.didMoveToParentViewController(this)
    }

    private fun setupKamper() {
        Kamper.apply {
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
            addInfoListener<MemoryInfo>  { info -> dispatch_async(dispatch_get_main_queue()) { if (memoryVC.isViewLoaded()) memoryVC.update(info) } }
            addInfoListener<NetworkInfo> { info -> dispatch_async(dispatch_get_main_queue()) { if (networkVC.isViewLoaded()) networkVC.update(info) } }
            addInfoListener<IssueInfo>   { info -> dispatch_async(dispatch_get_main_queue()) { if (issuesVC.isViewLoaded()) issuesVC.addIssue(info.issue) } }
            addInfoListener<JankInfo>    { info -> dispatch_async(dispatch_get_main_queue()) { if (jankVC.isViewLoaded())    jankVC.update(info) } }
            addInfoListener<GcInfo>      { info -> dispatch_async(dispatch_get_main_queue()) { if (gcVC.isViewLoaded())      gcVC.update(info) } }
            addInfoListener<ThermalInfo> { info -> dispatch_async(dispatch_get_main_queue()) { if (thermalVC.isViewLoaded()) thermalVC.update(info) } }
        }
        Kamper.start()
    }
}
