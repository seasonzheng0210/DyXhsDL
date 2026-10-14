package com.neoruaa.xhsdn

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import com.neoruaa.xhsdn.utils.UrlUtils
import com.neoruaa.xhsdn.utils.DownloadLogger
import com.neoruaa.xhsdn.utils.EventTracker
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.Composable
import com.neoruaa.xhsdn.utils.detectMediaType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neoruaa.xhsdn.douyin.DouyinParser
import com.neoruaa.xhsdn.douyin.DouyinMediaType
import com.neoruaa.xhsdn.douyin.DouyinPostItem
import com.neoruaa.xhsdn.douyin.WebDetailBlockedException
import com.neoruaa.xhsdn.data.HomeRepo
import com.neoruaa.xhsdn.data.HomepageBatchStore
import com.neoruaa.xhsdn.web.BgWebViewParser
import com.neoruaa.xhsdn.kuaishou.KuaishouParser
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import android.util.Size
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import java.io.File
import android.util.LruCache
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalConfiguration
import com.kyant.capsule.ContinuousRoundedRectangle
import com.neoruaa.xhsdn.ui.TabRowDefaults
import com.neoruaa.xhsdn.ui.TabRowWithContour
import com.neoruaa.xhsdn.ui.SelectableMediaWaterfall
import com.neoruaa.xhsdn.ui.glass.GlassColorSchemes
import com.neoruaa.xhsdn.ui.glass.GlassDialog
import com.neoruaa.xhsdn.ui.glass.GlassLogBody
import com.neoruaa.xhsdn.ui.glass.GlassPrimaryWrap
import com.neoruaa.xhsdn.ui.glass.GlassTokens
import com.neoruaa.xhsdn.ui.glass.GlassSpec
import com.neoruaa.xhsdn.ui.glass.accentBg
import com.neoruaa.xhsdn.ui.glass.accentFg
import com.neoruaa.xhsdn.ui.glass.glassSolid
import com.neoruaa.xhsdn.ui.glass.platformBrandColor
import com.neoruaa.xhsdn.ui.glass.platformLabel
import com.neoruaa.xhsdn.ui.OnboardingScreen
import com.neoruaa.xhsdn.ui.glass.WallpaperLayer
import androidx.compose.foundation.isSystemInDarkTheme
import com.neoruaa.xhsdn.viewmodels.MainUiState
import com.neoruaa.xhsdn.viewmodels.MainViewModel
import com.neoruaa.xhsdn.viewmodels.MediaItem
import com.neoruaa.xhsdn.viewmodels.MediaType
import com.neoruaa.xhsdn.viewmodels.SelectiveDownloadPhase
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.window.WindowListPopup
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import androidx.compose.ui.res.stringResource
import android.util.Log
import androidx.compose.ui.text.font.FontWeight
import com.neoruaa.xhsdn.ui.rememberOffsetPopupPositionProvider
import kotlinx.coroutines.awaitCancellation
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.TopDownloads
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.window.WindowBottomSheet

// 缩略图内存缓存（最多缓存 50 张缩略图）
private val thumbnailCache = object : LruCache<String, ImageBitmap>(50) {}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val _autoDownloadIntentUrl = mutableStateOf<String?>(null)
    /**
     * 外部（TaskDetailActivity 的「下载该作者的主页」）请求打开主页下载。
     * Pair(link, from)。在 setContent 内由 LaunchedEffect 消费 —— 避免在 onCreate
     * 阶段（composition 未就绪）直接发起解析。
     */
    private val _pendingHomepage = mutableStateOf<Pair<String, String>?>(null)
    private var context: Context =  this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _autoDownloadIntentUrl.value = intent.getStringExtra("auto_download_url")
        intent.removeExtra("auto_download_url")
        consumeHomepageIntent(intent)

        if (Build.VERSION.SDK_INT >= 33) { // Android 13
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 200)
            }
        }

        enableEdgeToEdge()
        com.neoruaa.xhsdn.data.TaskManager.init(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 切后台下载不被打断：所有下载改由前台服务执行
        // taskId 透传：重试时复用同一任务，避免旧失败记录变成无人更新的僵尸记录
        fun dispatchDownload(rawLink: String, source: String?, taskId: Long? = null) {
            com.neoruaa.xhsdn.DownloadService.startDownload(this, rawLink, source, taskId)
        }

        // 防止自动锁屏
        val prefs = getSharedPreferences("XHSDownloaderPrefs", MODE_PRIVATE)
        if (prefs.getBoolean("keep_screen_on", false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        setContent {
            // 玻璃态：注入固定玻璃色板（亮/暗两套随系统明暗），不跟随 Monet 动态色（锁品牌徕卡橙）
            val controller = ThemeController(
                colorSchemeMode = ColorSchemeMode.System,
                lightColors = GlassColorSchemes.light(),
                darkColors = GlassColorSchemes.dark()
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val topBarState = rememberTopAppBarState()
            val scrollBehavior = MiuixScrollBehavior(state = topBarState)
            
            // 处理自动下载
            val autoUrl by _autoDownloadIntentUrl
            LaunchedEffect(autoUrl) {
                autoUrl?.let { url ->
                     if (url.isNotEmpty()) {
                        trackEvent("entry_share", mapOf("platform" to (UrlUtils.detectPlatform(url) ?: "unknown")))
                        // 快手链接：HTTP 直解不稳定（匿名 GraphQL 偶被风控返回空），改用 WebView 真浏览器解析（与抖音一致）
                        if (UrlUtils.detectPlatform(url) == "kuaishou") {
                            launchKuaishouWebView(url)
                            _autoDownloadIntentUrl.value = null
                            return@LaunchedEffect
                        }
                        // 抖音链接：HTTP 直解已被风控拦截，改用 WebView 真浏览器解析
                        if (UrlUtils.detectPlatform(url) == "douyin") {
                            launchDouyinWebView(url)
                            _autoDownloadIntentUrl.value = null
                            return@LaunchedEffect
                        }
                        viewModel.updateUrl(url)
                        ensureStoragePermission { 
                            val selectiveDownload = getSharedPreferences("XHSDownloaderPrefs", MODE_PRIVATE)
                                .getBoolean("selective_download", false)
                            if (selectiveDownload) {
                                viewModel.startSelectiveDownload { showToast(it) }
                            } else {
                                dispatchDownload(url, null)
                            }
                        }
                     }
                     _autoDownloadIntentUrl.value = null // 消费完毕
                }
            }
            
            // 剪贴板检测相关状态
            context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("XHSDownloaderPrefs", MODE_PRIVATE) }
            var detectedXhsLink by remember { mutableStateOf<String?>(null) }
            var detectedPlatform by remember { mutableStateOf<String?>(null) }
            // 任务栏当前平台页签：0=抖音 / 1=小红书 / 2=快手；识别到链接自动跳转
            var taskTab by remember { mutableStateOf(0) }
            // 底部主标签栏：mainTab 已提升为 Activity 级状态（见类字段），此处不再 remember
            // 已处理过的剪贴板链接（去重用），相同链接不再重复读取/下载
            var lastHandledUrl by remember { mutableStateOf<String?>(null) }
            var manualInputLinks by remember { mutableStateOf(prefs.getBoolean("manual_input_links", false)) }
            var selectiveDownload by remember { mutableStateOf(prefs.getBoolean("selective_download", false)) }

            // 外部导航消费：TaskDetailActivity「下载该作者的主页」→ EXTRA_OPEN_HOMEPAGE。
            // 放在 composition 内（而非 onCreate）以确保 setContent 已完成、状态可安全写入。
            val pendingHome = _pendingHomepage.value
            LaunchedEffect(pendingHome) {
                if (pendingHome != null) {
                    _pendingHomepage.value = null
                    openHomepage(pendingHome.first, pendingHome.second)
                }
            }

            // 监听SharedPreferences变化，确保UI能够实时响应设置更改
            LaunchedEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "manual_input_links") {
                        manualInputLinks = prefs.getBoolean("manual_input_links", false)
                    } else if (key == "selective_download") {
                        selectiveDownload = prefs.getBoolean("selective_download", false)
                    } else if (key == "keep_screen_on") {
                        if (prefs.getBoolean("keep_screen_on", false)) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)

                // 清理监听器
                try {
                    awaitCancellation()
                } finally {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }
            
            // 监听生命周期 ON_RESUME 和 ON_PAUSE 进行剪贴板监听器管理
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

            // 提取核心检测逻辑为可复用函数
            fun checkClipboard() {
                // 1. Refresh Preferences
                val currentAutoRead = prefs.getBoolean("auto_read_clipboard", false)
                val currentShowBubble = prefs.getBoolean("show_clipboard_bubble", true)

                Log.d("XHS_Debug", "checkClipboard: AutoRead=$currentAutoRead, ShowBubble=$currentShowBubble, ManualInput=$manualInputLinks")

                // 2. Access Clipboard
                val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                if (clipboard.hasPrimaryClip()) {
                    val clipData = clipboard.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val clipText = clipData.getItemAt(0).text?.toString() ?: ""
                        Log.d("XHS_Debug", "ClipText: $clipText")
                        
                        val url = UrlUtils.extractFirstUrl(clipText)
                        Log.d("XHS_Debug", "Extracted URL: $url")

                        // 去重：同一链接已经处理过则跳过，避免自动读取反复触发
                        val dedupeKey = url ?: clipText
                        if (dedupeKey == lastHandledUrl) {
                            Log.d("XHS_Debug", "Same as last handled ($dedupeKey), skip")
                            return@checkClipboard
                        }
                        
                        if (UrlUtils.detectPlatform(clipText) != null) {
                        // 3. Logic Branching

                        // 0. 主页链接优先：抖音主页分享文案 / /user/{sec_uid} → 直接下载主页全部视频
                        if (UrlUtils.isDouyinHomepageLink(clipText)) {
                            lastHandledUrl = dedupeKey
                            // ★入口层修复：先切到首页下载 tab 再解析，否则解析白跑且用户看不到
                            openHomepage(clipText, HOMEPAGE_FROM_CLIPBOARD)
                            // 读取后清空剪贴板，避免同链接被反复读取
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
                            detectedXhsLink = null
                            return@checkClipboard
                        }

                        if (currentAutoRead && UrlUtils.detectPlatform(clipText) == "douyin") {
                            trackEvent("auto_download", mapOf("platform" to "douyin"))
                            // 抖音：HTTP 直解已被风控拦截，改用 WebView 真浏览器解析
                            lastHandledUrl = dedupeKey
                            // 识别到平台后自动跳到对应任务页签（无论当前停留在哪个页签）
                            taskTab = platformTabIndex("douyin")
                            launchDouyinWebView(url ?: clipText)
                            // 读取后清空剪贴板，避免同链接被反复读取
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
                            detectedXhsLink = null
                        } else if (currentAutoRead) {
                                // A. Auto Download Priority
                                lastHandledUrl = dedupeKey
                                val autoPlat = UrlUtils.detectPlatform(clipText)
                                trackEvent("auto_download", mapOf("platform" to (autoPlat ?: "unknown")))
                                // 识别到平台后自动跳到对应任务页签（无论当前停留在哪个页签）
                                taskTab = platformTabIndex(autoPlat)
                                if (autoPlat == "kuaishou") {
                                    // 快手：HTTP 直解不稳，改用 WebView 真浏览器解析
                                    launchKuaishouWebView(clipText)
                                } else if (autoPlat == "douyin") {
                                    launchDouyinWebView(clipText)
                                } else {
                                viewModel.updateUrl(clipText)
                                Log.d("XHS_Debug", "Triggering Auto Download")

                                if (selectiveDownload) {
                                    trackEvent("selective_start")
                                    viewModel.startSelectiveDownload { showToast(it) }
                                } else {
                                    dispatchDownload(clipText, null)
                                }
                                }
                                
                                // Show Notification with Full Content
                                com.neoruaa.xhsdn.utils.NotificationHelper.showDownloadNotification(
                                    context,
                                    System.currentTimeMillis().toInt(),
                                    "开始下载",
                                    clipText,
                                    false
                                )
                                
                                // Clear Clipboard
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
                                
                                // Ensure bubble is dismissed
                                detectedXhsLink = null
                                
                            } else if (currentShowBubble) {
                                // B. Show Bubble
                                Log.d("XHS_Debug", "Showing Bubble")
                                lastHandledUrl = dedupeKey
                                detectedXhsLink = clipText
                                detectedPlatform = UrlUtils.detectPlatform(clipText)
                            } else {
                                Log.d("XHS_Debug", "Bubble disabled in settings")
                            }
                        } else {
                            // Link invalid or not detected -> Disappear
                            Log.d("XHS_Debug", "Not XHS link or null -> Hide Bubble")
                            detectedXhsLink = null
                            detectedPlatform = null
                        }
                    } else {
                        // Clipboard empty -> Disappear
                        Log.d("XHS_Debug", "Clipboard empty/null data -> Hide Bubble")
                        detectedXhsLink = null
                        detectedPlatform = null
                    }
                } else {
                    // No clipboard -> Disappear
                    Log.d("XHS_Debug", "No Primary Clip -> Hide Bubble")
                    detectedXhsLink = null
                    detectedPlatform = null
                }
            }
            
            val scope = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycleScope
            
            val clipboardListener = remember {
                android.content.ClipboardManager.OnPrimaryClipChangedListener {
                     // 延迟检测，解决 listener 触发时 ClipData 可能尚未准备好的问题
                     scope.launch {
                         kotlinx.coroutines.delay(300)
                         checkClipboard()
                     }
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        // 注册监听器
                        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.addPrimaryClipChangedListener(clipboardListener)
                        // 延迟检测：Android 10+ 需要等待窗口焦点才能访问剪贴板
                        scope.launch {
                            kotlinx.coroutines.delay(500)
                            checkClipboard()
                        }
                    } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                        // 移除监听器
                        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.removePrimaryClipChangedListener(clipboardListener)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.removePrimaryClipChangedListener(clipboardListener)
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MiuixTheme(controller = controller) {
                // 手动输入链接对话框状态
                var showInputDialog by remember { mutableStateOf(false) }
                // UI v2 S5：首次引导（仅全新安装首启展示，SharedPreferences 标记）
                var showOnboarding by remember {
                    mutableStateOf(!prefs.getBoolean(ONBOARDING_DONE_KEY, false))
                }

                MainScreen(
                    uiState = uiState,
                    manualInputLinks = manualInputLinks,
                    showInputDialog = showInputDialog,
                    onShowInputDialogChange = { showInputDialog = it },
                    scrollBehavior = scrollBehavior,
                    mainTab = mainTab,
                    onMainTabSelected = {
                        val tabLabel = when (it) { 0 -> "home"; 1 -> "tasks"; else -> "mine" }
                        trackEvent("main_tab_switch", mapOf("tab" to tabLabel))
                        mainTab = it
                    },
                    onHomepageDownload = { link -> openHomepage(link, HOMEPAGE_FROM_TASK_CARD) },
                    onHomepageManualPreview = { link -> fetchHomepagePreview(link) },
                    homePreview = homePreview,
                    homepageEntry = homepageEntry,
                    homepageReturnVisible = homepageReturnVisible,
                    onHomepageReturn = { homeBackToOrigin() },
                    parseStage = homeParseStage,
                    onCancelPreview = { cancelHomepagePreview() },
                    activeLink = homeActiveLink,
                    onHomepageConfirm = { range, n, includeImages, skipDownloaded, selectedIds ->
                        (homePreview as? HomePreviewState.Ready)?.let {
                            confirmHomepageDownload(it, range, n, includeImages, skipDownloaded, selectedIds)
                        }
                    },
                    onFallbackCrawl = { link ->
                        trackEvent("homepage_fallback_webcrawl")
                        launchDouyinHomepageDownload(link)
                    },
                    onDownload = {
                        if (!manualInputLinks) {
                            ensureStoragePermission {
                                // 先读取剪贴板
                                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                // 主页链接：走主页下载，反查作者并批量下载
                                if (UrlUtils.isDouyinHomepageLink(clipText)) {
                                    openHomepage(clipText, HOMEPAGE_FROM_CLIPBOARD)
                                    return@ensureStoragePermission
                                }
                                // 提取有效链接
                                val platform = UrlUtils.detectPlatform(clipText)
                                if (platform == "douyin") {
                                    // 抖音：HTTP 直解已被风控拦截（空响应/安全页），改用 WebView 真浏览器解析
                                    trackEvent("entry_button", mapOf("platform" to "douyin"))
                                    taskTab = platformTabIndex(platform)
                                    launchDouyinWebView(clipText)
                                } else if (platform == "kuaishou") {
                                    // 快手：HTTP 直解不稳定（匿名 GraphQL 偶被风控），改用 WebView 真浏览器解析（与抖音一致）
                                    trackEvent("entry_button", mapOf("platform" to "kuaishou"))
                                    taskTab = platformTabIndex(platform)
                                    launchKuaishouWebView(clipText)
                                } else if (platform == "xhs") {
                                    trackEvent("entry_button", mapOf("platform" to "xhs"))
                                    taskTab = platformTabIndex(platform)
                                    viewModel.updateUrl(clipText)

                                    if (selectiveDownload) {
                                        trackEvent("selective_start")
                                        viewModel.startSelectiveDownload { showToast(it) }
                                    } else {
                                        // 派发到前台服务下载（普通模式）
                                        dispatchDownload(clipText, "xhs")
                                    }
                                } else {
                                    showToast(getString(R.string.link_not_recognized))
                                }
                            }
                        }
                    },
                    onCopyText = { 
                        trackEvent("copy_description")
                        // 先读取剪贴板
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (clipText.isNotEmpty()) {
                            viewModel.updateUrl(clipText)
                        }
                        ensureStoragePermission { viewModel.copyDescription({ showToast(getString(R.string.copied_description)) }, { showToast(it) }) } 
                    },
                    onOpenSettings = { trackEvent("settings_open"); startActivity(Intent(this, SettingsActivity::class.java)) },
                    onWebCrawlFromClipboard = homepageGuard@{
                        // 先读取剪贴板
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (clipText.isNotEmpty()) {
                            // Clean the URL using the same method as other places
                            val cleanUrl = UrlUtils.extractFirstUrl(clipText)
                            if (cleanUrl != null) {
                                // 主页链接：走主页下载，反查作者并批量下载
                                if (UrlUtils.isDouyinHomepageLink(clipText)) {
                                    openHomepage(clipText, HOMEPAGE_FROM_BUBBLE)
                                    detectedXhsLink = null
                                    return@homepageGuard
                                }
                                val platform = UrlUtils.detectPlatform(cleanUrl)
                                if (platform == "kuaishou") {
                                    // 快手链接走专用 WebView 入口（kuaishou_extractor 兜底）
                                    trackEvent("entry_webcrawl", mapOf("platform" to "kuaishou"))
                                    taskTab = platformTabIndex(platform)
                                    launchKuaishouWebView(cleanUrl)
                                } else if (platform == "douyin") {
                                    trackEvent("entry_webcrawl", mapOf("platform" to "douyin"))
                                    taskTab = platformTabIndex(platform)
                                    launchDouyinWebView(cleanUrl)
                                } else if (platform == "xhs") {
                                    trackEvent("entry_webcrawl", mapOf("platform" to "xhs"))
                                    taskTab = platformTabIndex(platform)
                                    val webViewIntent = Intent(this, WebViewActivity::class.java).apply {
                                        putExtra("url", cleanUrl)
                                        putExtra("source", "xhs")
                                        putExtra("direct", true)
                                        // Don't pass task_id here - let WebViewActivity create the task when user clicks "爬取"
                                    }
                                    startActivityForResult(webViewIntent, WEBVIEW_REQUEST_CODE)
                                } else {
                                    showToast(getString(R.string.link_not_recognized))
                                }
                                detectedXhsLink = null
                            } else {
                                showToast(getString(R.string.invalid_link_please_reenter))
                            }
                        }
                    },
                    onMediaClick = {
                        trackEvent("media_open")
                        openFile(it)
                    },
                    onCopyUrl = { url ->
                        trackEvent("copy_url")
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("xhs_url", url))
                        showToast(getString(R.string.link_copied))
                    },
                    onBrowseUrl = { url ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                try {
                                    // 使用通用URL提取
                                    val cleanUrl = UrlUtils.extractFirstUrl(url)
                                    if (cleanUrl != null) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl))
                                        startActivity(intent)
                                    } else {
                                        showToast("未找到有效链接")
                                    }
                                } catch (e: Exception) {
                                    showToast(getString(R.string.unable_to_open_browser, e.message))
                                }
                            }
                        }
                    },
                    onRetryTask = { task ->
                        trackEvent("retry_task", mapOf("source" to (task.source ?: "")))
                        when (task.source) {
                            "douyin_home" -> {
                                // 主页批次重试：重新反查作者并爬取（旧失败记录不再复用，直接删除避免僵尸）
                                com.neoruaa.xhsdn.data.TaskManager.deleteTask(task.id)
                                openHomepage(task.noteUrl, HOMEPAGE_FROM_TASK_CARD)
                            }
                            else -> ensureStoragePermission {
                                when (task.source) {
                                    "douyin" -> {
                                        // 复用同一任务记录（重置进度），把原失败任务 id 透传下去，重试成功即原地变 COMPLETED
                                        com.neoruaa.xhsdn.data.TaskManager.resetTask(task.id)
                                        launchDouyinWebView(task.noteUrl, task.id)
                                    }
                                    "kuaishou" -> {
                                        com.neoruaa.xhsdn.data.TaskManager.resetTask(task.id)
                                        launchKuaishouWebView(task.noteUrl, task.id)
                                    }
                                    else -> {
                                        // 复用同一任务（重置进度），重新派发到前台服务下载
                                        com.neoruaa.xhsdn.data.TaskManager.resetTask(task.id)
                                        com.neoruaa.xhsdn.data.TaskManager.startTask(task.id)
                                        dispatchDownload(task.noteUrl, "xhs", task.id)
                                    }
                                }
                            }
                        }
                    },
                    onDeleteTask = { task ->
                        trackEvent("task_delete", mapOf("source" to (task.source ?: "")))
                        com.neoruaa.xhsdn.data.TaskManager.deleteTask(task.id)
                    },
                    onContinueTask = { task -> 
                        trackEvent("task_continue")
                        viewModel.continueTask(task)
                    },
                    onWebCrawlTask = { task ->
                        trackEvent("task_webcrawl", mapOf("source" to (task.source ?: "")))
                        viewModel.updateUrl(task.noteUrl)
                        when (task.source) {
                            "kuaishou" -> launchKuaishouWebView(task.noteUrl, task.id)
                            "douyin" -> launchDouyinWebView(task.noteUrl, task.id)
                            else -> launchWebView(task.noteUrl, task.id)
                        }
                    },
                    onStopTask = { task ->
                         trackEvent("task_stop")
                         // 停止前台服务中正在进行的任务
                         com.neoruaa.xhsdn.DownloadService.stopTask(this, task.id)
                    },
                    onClearHistory = { trackEvent("clear_history"); viewModel.clearHistory() },
                    onManualInputDownload = homepageGuard@{ inputLink ->
                        // 抖音主页链接（分享文案 / /user/{sec_uid}）：走主页下载，反查作者并批量下载
                        if (UrlUtils.isDouyinHomepageLink(inputLink)) {
                            openHomepage(inputLink, HOMEPAGE_FROM_MANUAL)
                            return@homepageGuard
                        }
                        ensureStoragePermission {
                            val plat = UrlUtils.detectPlatform(inputLink)
                            if (plat == "douyin") {
                                // 抖音：HTTP 直解已被风控拦截，改用 WebView 真浏览器解析
                                trackEvent("entry_manual", mapOf("platform" to "douyin"))
                                taskTab = platformTabIndex(plat)
                                launchDouyinWebView(inputLink)
                            } else if (plat == "kuaishou") {
                                trackEvent("entry_manual", mapOf("platform" to "kuaishou"))
                                taskTab = platformTabIndex(plat)
                                launchKuaishouWebView(inputLink)
                            } else if (plat == "xhs") {
                                trackEvent("entry_manual", mapOf("platform" to "xhs"))
                                taskTab = platformTabIndex(plat)
                                viewModel.updateUrl(inputLink)
                                if (selectiveDownload) {
                                    trackEvent("selective_start")
                                    viewModel.startSelectiveDownload { showToast(it) }
                                } else {
                                    dispatchDownload(inputLink, "xhs")
                                }
                            } else {
                                // 既非抖音/快手/小红书任一：提示链接暂不可识别
                                showToast(getString(R.string.link_not_recognized))
                            }
                        }
                    },
                    detectedXhsLink = detectedXhsLink,
                    detectedPlatform = detectedPlatform,
                    taskTab = taskTab,
                    onTabSelected = {
                        trackEvent("tab_switch", mapOf("tab" to tabNameOf(it)))
                        taskTab = it
                    },
                    onClipboardBubbleActivate = homepageGuard@{
                        val link = detectedXhsLink
                        if (!link.isNullOrBlank()) {
                                val bubblePlatform = when {
                                    UrlUtils.isDouyinHomepageLink(link) -> "douyin_home"
                                    else -> UrlUtils.detectPlatform(link) ?: "unknown"
                                }
                                trackEvent("entry_bubble", mapOf("platform" to bubblePlatform))
                                // 主页链接：走主页下载，反查作者并批量下载
                                if (UrlUtils.isDouyinHomepageLink(link)) {
                                    openHomepage(link, HOMEPAGE_FROM_BUBBLE)
                                    detectedXhsLink = null
                                    detectedPlatform = null
                                    return@homepageGuard
                                }
                                // 平台判定：以当前链接实时识别为准（域名/关键词双保险）；
                                // detectedPlatform 仅作兜底——否则残留的小红书平台会把快手/抖音链接派错通道
                                val platform = UrlUtils.detectPlatform(link) ?: detectedPlatform
                            ensureStoragePermission {
                                // 识别到平台后自动跳到对应任务页签
                                taskTab = platformTabIndex(platform)
                                if (platform == "douyin") {
                                    // 抖音：HTTP 直解已被风控拦截，改用 WebView 真浏览器解析
                                    launchDouyinWebView(link)
                                } else if (platform == "kuaishou") {
                                    launchKuaishouWebView(link)
                                } else {
                                    viewModel.updateUrl(link)
                                    if (selectiveDownload) {
                                        trackEvent("selective_start")
                                        viewModel.startSelectiveDownload { showToast(it) }
                                    } else {
                                        dispatchDownload(link, "xhs")
                                    }
                                }
                                detectedXhsLink = null
                                detectedPlatform = null
                            }
                        }
                    },
                    onDismissPrompt = {
                        trackEvent("bubble_dismiss")
                        detectedXhsLink = null
                        detectedPlatform = null
                        lastHandledUrl = null
                    },
                    onCancelSelectiveDownload = viewModel::cancelSelectiveDownload,
                    onSaveSelectedMedia = { viewModel.saveSelectedMedia { showToast(it) } },
                    onToggleSelectiveItem = viewModel::toggleSelectiveItem
                )

                // 检测到"重试同一链接但解析数量不一致"时，提示是否导出诊断日志
                val inconsistentRetry = uiState.inconsistentRetry
                if (inconsistentRetry.show) {
                    // v3.0.8：WindowDialog → GlassDialog（自绘玻璃卡，真机不再透明）
                    GlassDialog(
                        title = stringResource(R.string.retry_inconsistent_dialog_title),
                        summary = stringResource(
                            R.string.retry_inconsistent_dialog_message,
                            inconsistentRetry.previousCount,
                            inconsistentRetry.currentCount
                        ),
                        confirmText = stringResource(R.string.retry_inconsistent_save_button),
                        cancelText = stringResource(R.string.cancel),
                        onConfirm = {
                            viewModel.saveInconsistentRetryLogs(
                                onResult = { showToast(it) },
                                onError = { showToast(it) }
                            )
                        },
                        onDismiss = { viewModel.dismissInconsistentRetryDialog() }
                    )
                }

                // UI v2 S5：引导页浮于主界面之上（首启仅一次；完成后进入主界面）
                if (showOnboarding) {
                    OnboardingScreen(
                        onDone = {
                            prefs.edit().putBoolean(ONBOARDING_DONE_KEY, true).apply()
                            trackEvent("onboarding_finish")
                            showOnboarding = false
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }


    /**
     * 埋码事件（events.log）：Activity 侧功能点操作的统一入口。与 DownloadService 内的
     * download_requested / parse_* / download_done 互补——本方法记录「用户点了哪个功能」。
     */
    private fun trackEvent(event: String, attrs: Map<String, String>? = null) {
        EventTracker.track(this, event, attrs)
    }

    /** 平台页签名（与 taskTab 索引对应），供埋码 tab_switch 使用。 */
    private fun tabNameOf(taskTabIndex: Int): String = when (taskTabIndex) {
        0 -> "douyin"; 1 -> "xhs"; 2 -> "kuaishou"; else -> "failed"
    }

    private fun launchWebView(input: String, taskId: Long? = null) {
        val cleanUrl = UrlUtils.extractFirstUrl(input)
        if (cleanUrl == null) {
            showToast(getString(R.string.invalid_link_please_reenter))
            return
        }
        viewModel.resetWebCrawlFlag()
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra("url", cleanUrl)
        intent.putExtra("direct", true)
        if (taskId != null && taskId > 0) {
            intent.putExtra("task_id", taskId)
        }
        startActivityForResult(intent, WEBVIEW_REQUEST_CODE)
    }

    /**
     * 抖音下载入口：优先 HTTP 直解（移动端 aweme/v1/feed 接口，不触发 Argus、无水印），
     * 成功则直接交给下载服务下载——「不跳提取页、直接下」。仅当直解失败时回退到
     * WebView 真浏览器解析（旧行为）。
     */
    private fun launchDouyinWebView(input: String, taskId: Long? = null) {
        val cleanUrl = UrlUtils.extractFirstUrl(input)
        if (cleanUrl == null) {
            showToast(getString(R.string.invalid_link_please_reenter))
            return
        }
        // 抖音图文/视频统一派发前台服务：服务内先 HTTP 快解（DouyinParser.parse 含 note 页
        // 整页 HTML 解析），失败转后台不可见 WebView 真浏览器兜底（复用登录态 Cookie）——
        // 成功即 createTask 跳任务卡片，与视频下载体验完全一致；全程不启动可见 WebViewActivity，
        // 无黑窗闪动。主页批量走 fetchHomepagePreview（v2 预览流），本函数只处理单作品。
        lifecycleScope.launch {
            // 主页短链自动转（v1.14.0，用户 2026-09-05 裁定）：v.douyin.com/xxx 302 落地
            // share/user/{sec_uid} 时是作者主页不是单作品——不再失败报错，直接转主页批量下载。
            // 短链须先跟跳才知道是不是主页；已含 /video/|/note/ 数字 id 的直链跳过跟跳零开销。
            val looksLikeWork = Regex("""/(?:video|note)/\d+|modal_id=\d+""").containsMatchIn(cleanUrl)
            val finalUrl = if (looksLikeWork) cleanUrl else DouyinParser.resolveFinalUrl(cleanUrl)
            val userMatch = Regex("""(?:iesdouyin|douyin)\.com/(?:share/)?user/([0-9A-Za-z_-]+)""").find(finalUrl)
            if (userMatch != null) {
                trackEvent("homepage_download", mapOf("trigger" to "shortlink_auto"))
                DownloadLogger.logInfo(this@MainActivity, "douyin", cleanUrl, "主页短链自动转主页批量下载: $finalUrl")
                // 重试场景（v1.13.0 遗留的 douyin 失败卡）：删旧卡避免僵尸，主页批次新建自己的卡
                if (taskId != null && taskId > 0) {
                    com.neoruaa.xhsdn.data.TaskManager.deleteTask(taskId)
                }
                // share/user/{sec_uid} → www.douyin.com/user/{sec_uid}（主页爬取标准形态）
                val homeUrl = "https://www.douyin.com/user/${userMatch.groupValues[1]}"
                openHomepage(homeUrl, HOMEPAGE_FROM_SHORTLINK)
            } else {
                DownloadService.startDownload(this@MainActivity, cleanUrl, "douyin", taskId)
            }
        }
    }

    private fun startDouyinWebViewFallback(cleanUrl: String, taskId: Long? = null) {
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra("url", cleanUrl)
        intent.putExtra("source", "douyin")
        intent.putExtra("direct", true)
        if (taskId != null && taskId > 0) {
            intent.putExtra("task_id", taskId)
        }
        startActivityForResult(intent, WEBVIEW_REQUEST_CODE)
    }

    /**
     * 快手下载入口：直接派发前台服务。v1.10.12 起不在 MainActivity 预解析、也不再启动
     * 可见 WebViewActivity——服务内先 HTTP GraphQL 快解（10s），失败转后台不可见 WebView
     * 真浏览器兜底（复用登录态 Cookie），解析全程无 Activity/黑窗。
     */
    private fun launchKuaishouWebView(input: String, taskId: Long? = null) {
        val cleanUrl = UrlUtils.extractFirstUrl(input)
        if (cleanUrl == null) {
            showToast(getString(R.string.invalid_link_please_reenter))
            return
        }
        DownloadService.startDownload(this@MainActivity, cleanUrl, "kuaishou", taskId)
    }

    private fun startKuaishouWebViewFallback(cleanUrl: String, taskId: Long? = null) {
        // 快手匿名即可访问，无需登录态 cookie；短链跳转由 WebView 自行处理
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra("url", cleanUrl)
        intent.putExtra("source", "kuaishou")
        intent.putExtra("direct", true)
        if (taskId != null && taskId > 0) {
            intent.putExtra("task_id", taskId)
        }
        startActivityForResult(intent, WEBVIEW_REQUEST_CODE)
    }

    /**
     * 主页下载入口：给定视频链接或主页链接，反查/直连作者主页，启动 WebView 主页爬取。
     *  - 主页链接（分享文案 / /user/{sec_uid}）：直接交给 WebView 加载并爬取；
     *  - 视频链接：用移动端 aweme/v1/feed 接口反查 author.sec_uid → /user/{sec_uid} 再爬取。
     */
    /**
     * 主页批量 v2 预览状态（P1）。状态类型见文件顶层 [HomePreviewState]。
     */
    private var homePreview by mutableStateOf<HomePreviewState>(HomePreviewState.Idle)

    /**
     * 底部主标签栏当前页：0=首页下载 / 1=任务 / 2=我的。
     * 提升为 Activity 级状态（原为 composable 内 remember）：入口层需要在
     * [openHomepage] 里"先导航再解析"，Activity 方法必须能直接改它。
     */
    private var mainTab by mutableStateOf(1)

    /** 进入首页下载前的页签，供来源条「返回」还原（0/1/2）。 */
    private var homepageReturnTab by mutableStateOf(1)

    /** 当前主页下载会话的入口来源（clipboard/bubble/taskCard/taskDetail/shortlink/manual/recent）。 */
    private var homepageEntry by mutableStateOf("manual")

    /** 是否显示来源条 + 返回按钮（manual/recent 两类不显示）。 */
    private var homepageReturnVisible by mutableStateOf(false)

    /** 解析阶段（0 定位作者主页 / 1 解析作者主页 / 2 读取作品列表），驱动三阶段进度条。 */
    private var homeParseStage by mutableStateOf(0)

    /**
     * 当前正在解析的链接（回填到主页页输入框）。
     * 美工/可用性：外部入口（剪贴板 / 任务详情…）触发解析时，输入框原来是空的 ——
     * 来源条只说了"从哪来"，没说"在解析谁"，用户无法核对。现回填实际链接。
     */
    private var homeActiveLink by mutableStateOf("")

    /** 当前解析协程，供「取消」中断（取消后保留已填链接）。 */
    private var homeParseJob: kotlinx.coroutines.Job? = null

    /**
     * 主页下载**统一入口通道**。所有入口（剪贴板识别 / 悬浮球 / 任务卡 / 任务详情 /
     * 单作品短链 302 / 主动粘贴 / 最近作者）都必须走这里，不得直接调 [fetchHomepagePreview]。
     *
     * 核心语义 —— **先导航，再解析**：
     * 1) 若不在首页下载 tab → 记 `homepageReturnTab` 并切到 tab 0（用户必然看到解析进度）；
     * 2) 置来源（驱动来源条文案 + 返回去向）；
     * 3) 解析（三阶段、可取消）→ Ready。
     *
     * 这一条修复了此前最严重的隐性缺陷：`fetchHomepagePreview` 只写数据不导航，
     * 导致"识别到了但屏幕上什么都没发生"（解析白跑 11~14s）。
     *
     * @param from 入口来源，见 [HOMEPAGE_FROM_*]
     */
    private fun openHomepage(link: String, from: String) {
        if (mainTab != 0) {
            homepageReturnTab = mainTab
            mainTab = 0
        }
        homepageEntry = from
        homepageReturnVisible = from != HOMEPAGE_FROM_MANUAL && from != HOMEPAGE_FROM_RECENT
        // 入口维度埋码：定位"哪个入口最常用 / 哪个入口转化差"
        trackEvent("homepage_entry", mapOf("from" to from))
        fetchHomepagePreview(link)
    }

    /** 来源条「返回」：还原到进入前的页签（成本 1 次点击；替代"先弹确认条"方案）。 */
    private fun homeBackToOrigin() {
        mainTab = homepageReturnTab
        homepageReturnVisible = false
    }

    /**
     * 主页下载 v2 入口：输入主页链接/视频链接 → post API 拉作品列表 → 预览卡（昵称/总数/新增）。
     * L1 直取 403 → BgWebViewParser 预热一次重试；仍风控 → 提示先登录。
     */
    private fun fetchHomepagePreview(input: String) {
        trackEvent("homepage_preview")
        // 回填输入框：让用户看到"正在解析的是哪条链接"
        homeActiveLink = input
        val cleanUrl = UrlUtils.extractFirstUrl(input)
        if (cleanUrl == null) {
            showToast(getString(R.string.invalid_link_please_reenter))
            return
        }
        ensureStoragePermission {
            homeParseJob?.cancel()
            homeParseStage = 0
            homePreview = HomePreviewState.Loading
            homeParseJob = lifecycleScope.launch {
                try {
                    val secUid = withContext(Dispatchers.IO) {
                        // 先归一化短链：主页分享短链（v.douyin.com/x → share/user/{sec_uid}）须识别为主页
                        val finalUrl = runCatching { DouyinParser.resolveFinalUrl(cleanUrl) }.getOrDefault(cleanUrl)
                        val userFromFinal = Regex("""(?:iesdouyin|douyin)\.com/(?:share/)?user/([0-9A-Za-z_-]+)""")
                            .find(finalUrl)?.groupValues?.getOrNull(1)
                        userFromFinal ?: if (UrlUtils.isDouyinHomepageLink(input)) {
                            null
                        } else {
                            // 视频链接：反查作者主页（resolveAuthorHomepageUrl 内部会再跟跳一次，幂等）。
                            // detail 反查遇 Argus 风控 → 预热种 cookie 后重试一次（与下方 post 列表同款）。
                            try {
                                DouyinParser.resolveAuthorHomepageUrl(cleanUrl)
                                    ?.substringAfterLast('/')?.substringBefore('?')
                            } catch (e: WebDetailBlockedException) {
                                // 反查作者前预热：作品页触发 secsdk 种下完整安全 cookie 链
                                // （轻量首页只给 UIFID_TEMP，detail API 照样空响应）
                                BgWebViewParser(applicationContext).warmupAndSnapshot(
                                    cleanUrl,
                                    requireCookies = BgWebViewParser.POST_API_REQUIRED_COOKIES,
                                    budgetMs = BgWebViewParser.POST_WARMUP_BUDGET_MS
                                )
                                DouyinParser.resolveAuthorHomepageUrl(cleanUrl)
                                    ?.substringAfterLast('/')?.substringBefore('?')
                            }
                        }
                    }
                    if (secUid.isNullOrBlank()) {
                        homePreview = HomePreviewState.Error(getString(R.string.home_preview_bad_link), false)
                        return@launch
                    }
                    // 已定位到作者 → 进入「解析作者主页」阶段
                    homeParseStage = 1
                    val homepageUrl = "https://www.douyin.com/user/$secUid"
                    val items = mutableListOf<DouyinPostItem>()
                    var nickname: String? = HomeRepo.nickname(this@MainActivity, secUid)
                    var totalCount = 0
                    var hasMore = false
                    var provider = "l1"

                    // ---- L1 快路径：自算 a_bogus + CookieManager 指纹（约 2s）----
                    // 抖音现要求 uifid / x-secsdk-web-signature / verifyFp 等**只能由页面 secsdk
                    // JS 产出**的参数，纯自建请求大概率 403；仍保留一次尝试（成本低，且若抖音
                    // 回退策略即可白捡秒级路径）。**不再为 L1 做 28s 预热**——实测预热不能救 L1。
                    try {
                        val pg = withContext(Dispatchers.IO) { DouyinParser.fetchPostList(secUid, 0L) }
                        if (pg.items.isNotEmpty()) {
                            items += pg.items
                            nickname = nickname ?: pg.nickname
                            totalCount = pg.totalCount
                            hasMore = pg.hasMore
                            var cursor = pg.maxCursor
                            var page = 1
                            while (hasMore && page < HOMEPAGE_MAX_PAGES) {
                                val next = withContext(Dispatchers.IO) { DouyinParser.fetchPostList(secUid, cursor) }
                                items += next.items
                                totalCount = maxOf(totalCount, next.totalCount)
                                cursor = next.maxCursor
                                hasMore = next.hasMore
                                page++
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "L1 post 列表不可用（预期内，转 L2a 嗅探）: ${e.message}")
                    }

                    // ---- L2a 主路径：蹭页面自己发出的 post 请求（不重算任何签名）----
                    if (items.isEmpty()) {
                        provider = "l2a"
                        val sniff = withContext(Dispatchers.IO) {
                            BgWebViewParser(applicationContext).sniffHomepage(secUid)
                        }
                        if (sniff != null && sniff.isNotEmpty) {
                            items += sniff.items
                            nickname = nickname ?: sniff.nickname
                            totalCount = maxOf(totalCount, sniff.totalCount)
                            hasMore = sniff.hasMore
                        }
                    }

                    if (items.isEmpty()) {
                        trackEvent("homepage_provider_result", mapOf("provider" to provider, "items" to "0"))
                        homePreview = HomePreviewState.Error(getString(R.string.home_preview_need_login), true)
                        return@launch
                    }
                    trackEvent(
                        "homepage_provider_result",
                        mapOf("provider" to provider, "items" to items.size.toString(),
                            "total" to totalCount.toString(), "hasMore" to hasMore.toString())
                    )
                    // 匿名上限提示：实测服务端对未登录只返回首屏 21 条（页面显示「登录后查看更多作品」）
                    if (totalCount > 0 && items.size < totalCount) {
                        showToast(getString(R.string.home_preview_partial, items.size, totalCount))
                    } else if (hasMore) {
                        // 档案接口没取到总数时降级文案，避免出现「共 0 条」
                        showToast(getString(R.string.home_preview_partial_unknown, items.size))
                    }
                    val lastSync = HomeRepo.lastSync(this@MainActivity, secUid)
                    val newItems = HomeRepo.filterNew(this@MainActivity, secUid, items)
                    // 记录最近作者，供「上次解析」快捷卡（nickname 为空时仍记录 secUid 级）
                    HomeRepo.touch(this@MainActivity, secUid, nickname.orEmpty())
                    homeParseStage = 2
                    homePreview = HomePreviewState.Ready(
                        secUid, nickname ?: getString(R.string.home_preview_default_author),
                        homepageUrl, items, newItems, totalCount, lastSync
                    )
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // 用户主动取消：不得落到下面的 Exception 分支被误报为「解析失败」
                    throw e
                } catch (e: WebDetailBlockedException) {
                    homePreview = HomePreviewState.Error(getString(R.string.home_preview_need_login), true)
                } catch (e: Exception) {
                    homePreview = HomePreviewState.Error(
                        getString(R.string.home_preview_failed, e.message ?: "unknown"), false
                    )
                }
            }
        }
    }

    /**
     * 取消解析：立即回 Idle 并中断协程。**保留已填链接**（用户可微调后重试），
     * 这是「不弹确认条」方案成立的关键 —— 误触的成本从"白等 14 秒"降到"点一下取消"。
     */
    private fun cancelHomepagePreview() {
        homeParseJob?.cancel()
        homeParseJob = null
        homeParseStage = 0
        homePreview = HomePreviewState.Idle
    }

    /** 预览确认：按范围+开关选条 → HomepageBatchStore 暂存 → Service 批量下载。 */
    private fun confirmHomepageDownload(
        state: HomePreviewState.Ready,
        range: HomepageBatchStore.Range,
        latestN: Int,
        includeImages: Boolean,
        skipDownloaded: Boolean,
        selectedIds: Set<String> = emptySet()
    ) {
        val chosen = scopeHomepageItems(state, range, latestN, includeImages, skipDownloaded, selectedIds)
        if (chosen.isEmpty()) {
            showToast(getString(R.string.home_preview_none_new))
            return
        }
        trackEvent("homepage_confirm", mapOf("range" to range.name, "count" to chosen.size.toString()))
        val token = HomepageBatchStore.put(
            HomepageBatchStore.Batch(
                state.secUid, state.nickname, state.homepageUrl, range, latestN,
                includeImages, skipDownloaded, chosen
            )
        )
        com.neoruaa.xhsdn.DownloadService.startHomepageBatch(this, token)
        showToast(getString(R.string.home_preview_started, chosen.size))
        homePreview = HomePreviewState.Idle
        // 批次已派出，来源条使命结束（避免返回按钮把用户带回一个已无关的页签）
        homepageReturnVisible = false
    }

    /**
     * 主页下载 v1 WebView 爬取降级链：仅当 v2 post API 被风控（需登录）时，
     * 由 HomepagePage Error 分支的「改用网页爬取方式」按钮调用。日常主页入口已全走
     * fetchHomepagePreview（v2 预览流），本函数是兜底，勿在别处新引用。
     */
    private fun launchDouyinHomepageDownload(input: String) {
        trackEvent("homepage_download")
        val cleanUrl = UrlUtils.extractFirstUrl(input)
        if (cleanUrl == null) {
            showToast(getString(R.string.invalid_link_please_reenter))
            return
        }
        ensureStoragePermission {
            lifecycleScope.launch {
                val homepageUrl = withContext(Dispatchers.IO) {
                    if (UrlUtils.isDouyinHomepageLink(input)) {
                        // 主页链接（短链或 /user/{sec_uid} 直链）：交给 WebView 重定向/爬取
                        cleanUrl
                    } else {
                        // 视频链接：用移动端 feed 接口反查 sec_uid → /user/{sec_uid}
                        runCatching { DouyinParser.resolveAuthorHomepageUrl(cleanUrl) }.getOrNull() ?: cleanUrl
                    }
                }
                startDouyinHomepageCrawl(homepageUrl)
            }
        }
    }

    private fun startDouyinHomepageCrawl(homepageUrl: String) {
        // 主页爬取需用户可见（滚动收集 + 登录态），故 direct=false
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra("url", homepageUrl)
            putExtra("source", "douyin")
            putExtra("mode", "homepage")
            putExtra("direct", false)
        }
        startActivityForResult(intent, WEBVIEW_REQUEST_CODE)
    }



    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("auto_download_url")?.let {
            _autoDownloadIntentUrl.value = it
            intent.removeExtra("auto_download_url")
        }
        consumeHomepageIntent(intent)
    }

    /** 解析外部导航 extra → 落到 [_pendingHomepage]，由 setContent 内 LaunchedEffect 消费。 */
    private fun consumeHomepageIntent(intent: Intent?) {
        val link = intent?.getStringExtra(EXTRA_OPEN_HOMEPAGE) ?: return
        if (link.isBlank()) return
        val from = intent.getStringExtra(EXTRA_HOMEPAGE_FROM) ?: HOMEPAGE_FROM_TASK_DETAIL
        intent.removeExtra(EXTRA_OPEN_HOMEPAGE)
        _pendingHomepage.value = link to from
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // 供旧 Java 下载逻辑回调调用，提示用户切换到网页模式
    fun showWebCrawlOption() {
        runOnUiThread {
            viewModel.notifyWebCrawlSuggestion()
        }
    }

    private fun ensureStoragePermission(onReady: () -> Unit) {
        if (hasStoragePermission()) {
            onReady()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
            showToast(getString(R.string.grant_all_files_access_retry))
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                showToast(getString(R.string.storage_permission_granted_continue))
            } else {
                showToast(getString(R.string.storage_permission_missing_unable_save))
            }
        }
    }

    private fun openFile(item: MediaItem) {
        val file = File(item.path)
        if (!file.exists()) {
            showToast(getString(R.string.file_does_not_exist, item.path))
            return
        }
        val mimeType = when (item.type) {
            MediaType.VIDEO -> "video/*"
            MediaType.IMAGE -> "image/*"
            MediaType.OTHER -> "*/*"
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        kotlin.runCatching { startActivity(intent) }.onFailure {
            showToast(getString(R.string.unable_to_open_file_error, it.message))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WEBVIEW_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val imageUrls = data.getStringArrayListExtra("image_urls") ?: emptyList()
            val videoUrls = data.getStringArrayListExtra("urls") ?: emptyList()
            val content = data.getStringExtra("content_text")
            val taskId = data.getLongExtra("task_id", -1L).takeIf { it > 0 }
            val source = data.getStringExtra("source") ?: "xhs"
            val forceDirect = data.getBooleanExtra("force_direct", false)

            if (source == "douyin_home") {
                // 主页下载：videoUrls 为 WebView 主页爬取收集到的视频页链接（/video/{id}）。
                // v1.10.12：逐条解析下放给 DownloadService 后台完成（HTTP 快解 → 后台 WebView
                // 兜底，复用登录态 Cookie，不弹 Activity），不再拿回 MainActivity 逐条走已被
                // 风控打死的 HTTP DouyinParser.parse（此前必现「主页视频解析失败」）。
                val videoPageUrls = videoUrls.filter { it.contains("/video/") || it.contains("/note/") }
                if (videoPageUrls.isEmpty()) {
                    showToast("未收集到主页视频链接，请重试或登录后重试")
                    return
                }
                val homepageUrl = data.getStringExtra("url") ?: ""
                showToast(getString(R.string.homepage_parsing, videoPageUrls.size))
                com.neoruaa.xhsdn.DownloadService.startDouyinHomeBatch(
                    this@MainActivity, videoPageUrls.distinct(), homepageUrl
                )
                return
            }

            if (source == "douyin") {
                val webViewUrl = data.getStringExtra("url") ?: ""
                if (forceDirect) {
                    // 抖音 HTTP 直解已被风控打死，走此路必败；引导用户登录后重试提取
                    showToast("抖音 HTTP 直解已失效（平台风控），请点「去登录抖音」登录后点「重试提取」")
                } else if (imageUrls.isNotEmpty()) {
                    // 图文/图集帖：imageUrls 非空 ⟺ 是抖音图文帖，下载全部图片（不再误当视频下）
                    com.neoruaa.xhsdn.DownloadService.startDownloadDouyinImages(
                        this, imageUrls, webViewUrl, taskId
                    )
                } else if (videoUrls.isNotEmpty()) {
                    // 视频帖：直链走中立下载，不进 HTTP 解析器
                    com.neoruaa.xhsdn.DownloadService.startDownload(
                        this,
                        videoUrls.firstOrNull { it.startsWith("http") } ?: webViewUrl,
                        "douyin",
                        taskId
                    )
                } else {
                    // 两条列表都为空：退回 HTTP 直解重新解析（拿到结构化结果，正确分流图文/视频）
                    com.neoruaa.xhsdn.DownloadService.startDownload(this, webViewUrl, "douyin")
                }
                return
            }

            if (source == "kuaishou") {
                val webViewUrl = data.getStringExtra("url") ?: ""
                if (forceDirect) {
                    // 快手 HTTP 直解已被风控打死，引导用户登录后重试提取
                    showToast("快手 HTTP 直解已失效（平台风控），请点「去登录快手」登录后点「重试提取」")
                    return
                }
                if (videoUrls.isEmpty()) {
                    showToast("WebView 未能提取快手作品，可重试或点「直链解析」")
                    return
                }
                // 快手只要视频：从 WebView 嗅探结果中过滤出视频直链，排除图集图片
                val videoUrlsFiltered = videoUrls.filter { isKuaishouVideoUrl(it) }
                if (videoUrlsFiltered.isEmpty()) {
                    showToast("WebView 提取结果中未找到视频直链，可重试或登录后重试提取")
                    return
                }
                val taskToUse = taskId ?: com.neoruaa.xhsdn.data.TaskManager.createTask(
                    noteUrl = webViewUrl,
                    noteTitle = null,
                    noteType = com.neoruaa.xhsdn.data.NoteType.VIDEO,
                    totalFiles = videoUrlsFiltered.size
                ).also {
                    com.neoruaa.xhsdn.data.TaskManager.updateTaskStatus(it, com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING)
                }
                // 只走服务的 startWebCrawl（FileDownloader，已按 host 带 Referer/UA 下快手 CDN）。
                // 不要再用 onWebCrawlResult：那条是 XHSDownloader（小红书专用），处理不了快手 CDN，
                // 会与 startWebCrawl 抢同一条 taskId，导致任务状态卡在 DOWNLOADING（显示「停止」）。
                com.neoruaa.xhsdn.DownloadService.startWebCrawl(this, videoUrlsFiltered, content, taskToUse)
                return
            }

            if (imageUrls.isNotEmpty()) {
                // Check if a task ID was passed from WebViewActivity (meaning task was already created)
                val taskToUse = if (taskId != null) {
                    // Task was already created in WebViewActivity
                    taskId
                } else {
                    // Create a new task when URLs are returned from WebViewActivity
                    val webViewUrl = data.getStringExtra("url") ?: "Unknown URL"
                    val newTaskId = com.neoruaa.xhsdn.data.TaskManager.createTask(
                        noteUrl = webViewUrl,
                        noteTitle = null,
                        noteType = com.neoruaa.xhsdn.data.NoteType.UNKNOWN,
                        totalFiles = imageUrls.size
                    )

                    // Update the task status to DOWNLOADING immediately since we have the URLs
                    com.neoruaa.xhsdn.data.TaskManager.updateTaskStatus(newTaskId, com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING)
                    newTaskId
                }

                viewModel.onWebCrawlResult(imageUrls, content, taskToUse)
                // 网页爬取同样需要在后台持续，转派前台服务执行
                com.neoruaa.xhsdn.DownloadService.startWebCrawl(this, imageUrls, content, taskToUse)
            } else {
                showToast("未发现可下载的资源")
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 3001
        const val WEBVIEW_REQUEST_CODE = 3002
        /** UI v2 S5 首次引导完成标记（仅全新安装首启展示）。 */
        private const val ONBOARDING_DONE_KEY = "v2_onboarding_done"
        /** logcat 标签（主页下载降级链诊断用）。 */
        private const val TAG = "MainActivity"
        /** 预览拉取翻页上限（防异常账号无限翻页拖垮 Service）。 */
        private const val HOMEPAGE_MAX_PAGES = 20

        // ── 主页下载跨 Activity 导航（TaskDetailActivity → 本页主页下载承载面）──
        /** 外部请求打开主页下载：值为主页链接或作品链接。 */
        const val EXTRA_OPEN_HOMEPAGE = "extra_open_homepage"
        /** 配合 [EXTRA_OPEN_HOMEPAGE] 的入口来源标识。 */
        const val EXTRA_HOMEPAGE_FROM = "extra_homepage_from"

        // ── 主页下载入口来源（驱动来源条文案 + 返回去向）──
        const val HOMEPAGE_FROM_CLIPBOARD = "clipboard"
        const val HOMEPAGE_FROM_BUBBLE = "bubble"
        const val HOMEPAGE_FROM_TASK_CARD = "taskCard"
        const val HOMEPAGE_FROM_TASK_DETAIL = "taskDetail"
        const val HOMEPAGE_FROM_SHORTLINK = "shortlink"
        const val HOMEPAGE_FROM_MANUAL = "manual"
        const val HOMEPAGE_FROM_RECENT = "recent"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainScreen(
    uiState: MainUiState,
    manualInputLinks: Boolean = false,
    showInputDialog: Boolean = false,
    onShowInputDialogChange: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onCopyText: () -> Unit,
    onOpenSettings: () -> Unit,
    onWebCrawlFromClipboard: () -> Unit,
    onClearHistory: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onCopyUrl: (String) -> Unit,
    onBrowseUrl: (String) -> Unit,
    onRetryTask: (com.neoruaa.xhsdn.data.DownloadTask) -> Unit,
    onStopTask: (com.neoruaa.xhsdn.data.DownloadTask) -> Unit,
    onDeleteTask: (com.neoruaa.xhsdn.data.DownloadTask) -> Unit,
    onContinueTask: (com.neoruaa.xhsdn.data.DownloadTask) -> Unit,
    onWebCrawlTask: (com.neoruaa.xhsdn.data.DownloadTask) -> Unit,
    onManualInputDownload: (String) -> Unit,
    scrollBehavior: ScrollBehavior,
    detectedXhsLink: String?,
    detectedPlatform: String? = null,
    taskTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    mainTab: Int = 0,
    onMainTabSelected: (Int) -> Unit = {},
    onHomepageDownload: (String) -> Unit = {},
    /** 主页下载 tab 内「主动粘贴」解析：不导航（用户已在承载面），语义区别于 [onHomepageDownload]。 */
    onHomepageManualPreview: (String) -> Unit = {},
    homePreview: HomePreviewState = HomePreviewState.Idle,
    /** 当前主页下载会话入口来源（clipboard/taskCard/manual/…），驱动来源条文案。 */
    homepageEntry: String = "manual",
    /** 是否显示来源条 + 返回按钮（manual/recent 不显示）。 */
    homepageReturnVisible: Boolean = false,
    /** 来源条「返回」：还原到进入前的页签。 */
    onHomepageReturn: () -> Unit = {},
    /** 解析阶段（0 定位作者 / 1 解析主页 / 2 读取作品）。 */
    parseStage: Int = 0,
    /** 取消解析（保留已填链接）。 */
    onCancelPreview: () -> Unit = {},
    /** 当前正在解析的链接（回填输入框，让用户核对"在解析谁"）。 */
    activeLink: String = "",
    onHomepageConfirm: (HomepageBatchStore.Range, Int, Boolean, Boolean, Set<String>) -> Unit = { _, _, _, _, _ -> },
    onFallbackCrawl: (String) -> Unit = {},
    onClipboardBubbleActivate: () -> Unit = {},
    onDismissPrompt: () -> Unit,
    onCancelSelectiveDownload: () -> Unit,
    onSaveSelectedMedia: () -> Unit,
    onToggleSelectiveItem: (String) -> Unit
) {
    val statusListState = rememberLazyListState()
    var menuExpanded by remember { mutableStateOf(false) }
    var overflowButtonBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val density = LocalDensity.current
    val scrimInteraction = remember { MutableInteractionSource() }
    val menuWidth = 180.dp
    val menuWidthPx = with(density) { menuWidth.roundToPx() }

    // 清除历史记录确认对话框状态
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    // 下载失败日志查看对话框状态
    var showFailureLogDialog by remember { mutableStateOf(false) }
    // 失败日志内容版本号：清除后自增触发重读
    var failureLogVersion by remember { mutableStateOf(0) }
    // 正常日志查看对话框状态
    var showNormalLogDialog by remember { mutableStateOf(false) }
    // 埋码（功能使用）日志查看对话框状态
    var showEventsLogDialog by remember { mutableStateOf(false) }
    // 在 Composable 作用域取一次 Context，供菜单 onClick（非 Composable）复用
    val ctx = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // 玻璃态壁纸层（最底，透出 Chrome/内容层玻璃）
        WallpaperLayer(modifier = Modifier.fillMaxSize())
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
            // 玻璃态：容器底色透明，让最底 WallpaperLayer 透出
            containerColor = Color.Transparent,
            topBar = {
                // v3.0.2 mockup .appnm：顶部居中标题「抖快红下载」（16.5/700 近黑，玻璃面板上居中）
                val darkT = isSystemInDarkTheme()
                val appTitle = "抖快红下载"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                        .padding(top = 10.dp, bottom = 6.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = appTitle,
                        color = if (darkT) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        ) { padding ->
            // v3.0.2 mockup .nav：底部 dock 绝对定位浮在整个背景（壁纸/内容）上，
            // 内容区延伸到屏幕底部，可滚到 dock 下方（被 dock 遮住是设计态，非截断）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (mainTab) {
                    // 1=任务（默认入口）：我的下载任务列表
                    1 -> HistoryPage(
                            uiState = uiState,
                            manualInputLinks = manualInputLinks,
                            showInputDialog = showInputDialog,
                            onShowInputDialogChange = onShowInputDialogChange,
                            statusListState = statusListState,
                            onDownload = onDownload,
                            onManualInputDownload = onManualInputDownload,
                            onMediaClick = onMediaClick,
                            onCopyUrl = onCopyUrl,
                            onBrowseUrl = onBrowseUrl,
                            onRetryTask = onRetryTask,
                            onContinueTask = onContinueTask,
                            onWebCrawlTask = onWebCrawlTask,
                            onStopTask = onStopTask,
                            onDeleteTask = onDeleteTask,
                            detectedXhsLink = detectedXhsLink,
                            detectedPlatform = detectedPlatform,
                            selectedTab = taskTab,
                            onTabSelected = onTabSelected,
                            onHomepageDownload = onHomepageDownload,
                            onClipboardBubbleActivate = onClipboardBubbleActivate,
                            onGoHomepage = { onMainTabSelected(0) },
                            onOpenFailureLog = { showFailureLogDialog = true },
                            onDismissPrompt = onDismissPrompt,
                            modifier = Modifier.fillMaxSize(),
                            // v3.0.8：不再传 miuixScrollBehavior.nestedScrollConnection —— 顶栏 v3.0.2 已改自绘、
                            // 不再消费滚动偏移，此 connection 挂在 LazyColumn 上只会吞掉滚动手势（任务页滑不动）。
                            nestedScrollConnection = null
                        )
                        // 0=首页：主页批量下载三步流程
                        0 -> HomepagePage(
                            preview = homePreview,
                            // 主动粘贴：不导航（用户已在承载面），故走 manual 通道而非 onHomepageDownload
                            onPreview = onHomepageManualPreview,
                            onConfirm = onHomepageConfirm,
                            onFallbackCrawl = onFallbackCrawl,
                            entryFrom = homepageEntry,
                            showSourceBar = homepageReturnVisible,
                            onReturn = onHomepageReturn,
                            parseStage = parseStage,
                            onCancelPreview = onCancelPreview,
                            activeLink = activeLink,
                            modifier = Modifier.fillMaxSize()
                        )
                        // 2=我的：内嵌设置页（UI v2 S3 分组；原 SettingsActivity 保留独立入口）
                    else -> {
                            // 设置数据全量落在 SharedPreferences：remember 持 VM 实例即可，无需 lifecycle-viewmodel-compose 依赖
                            val settingsContext = LocalContext.current
                            val settingsPrefs = remember { settingsContext.getSharedPreferences("XHSDownloaderPrefs", Context.MODE_PRIVATE) }
                            val settingsVm = remember { com.neoruaa.xhsdn.SettingsViewModel(settingsPrefs) }
                            val settingsState by settingsVm.state.collectAsStateWithLifecycle()
                            com.neoruaa.xhsdn.SettingsScreen(
                                uiState = settingsState,
                                embedded = true,
                                modifier = Modifier.fillMaxSize(),
                                onCreateLivePhotosChange = settingsVm::onCreateLivePhotosChange,
                                onUseCustomNamingChange = settingsVm::onUseCustomNamingChange,
                                onTemplateChange = settingsVm::onTemplateChange,
                                onResetTemplate = settingsVm::onResetTemplate,
                                onDebugNotificationChange = settingsVm::onDebugNotificationChange,
                                onDownloadCompleteNotificationChange = settingsVm::onDownloadCompleteNotificationChange,
                                onSelectiveDownloadChange = settingsVm::onSelectiveDownloadChange,
                                onKeepScreenOnChange = settingsVm::onKeepScreenOnChange,
                                onShowClipboardBubbleChange = settingsVm::onShowClipboardBubbleChange,
                                onAutoReadClipboardChange = settingsVm::onAutoReadClipboardChange,
                                onManualInputLinksChange = settingsVm::onManualInputLinksChange,
                                topBarState = rememberTopAppBarState()
                            )
                        }
                }
                // 底部主标签 dock（浮层，叠在内容上）
                MainTabBar(
                    selected = mainTab,
                    onSelected = onMainTabSelected,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        SelectiveDownloadSheet(
            uiState = uiState,
            onCancel = onCancelSelectiveDownload,
            onSave = onSaveSelectedMedia,
            onToggleItem = onToggleSelectiveItem
        )

        // v3.0.8：失败日志弹窗此前只置位 showFailureLogDialog 却无处渲染（点了没反应）→ 补上玻璃弹窗
        if (showFailureLogDialog) {
            val logContent = remember(failureLogVersion) { DownloadLogger.getLogContent(ctx) }
            GlassDialog(
                title = stringResource(R.string.failure_log_title),
                onDismiss = { showFailureLogDialog = false }
            ) {
                GlassLogBody(
                    text = logContent,
                    emptyText = stringResource(R.string.failure_log_empty)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        text = stringResource(R.string.clear_failure_log),
                        onClick = {
                            DownloadLogger.clearFailureLog(ctx)
                            failureLogVersion++
                            Toast.makeText(ctx, ctx.getString(R.string.failure_log_cleared), Toast.LENGTH_SHORT).show()
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        text = stringResource(R.string.copy_log),
                        onClick = {
                            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("failure_log", logContent))
                            Toast.makeText(ctx, ctx.getString(R.string.log_copied), Toast.LENGTH_SHORT).show()
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { showFailureLogDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectiveDownloadSheet(
    uiState: MainUiState,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onToggleItem: (String) -> Unit
) {
    val selectiveState = uiState.selectiveDownload
    val canSave = selectiveState.phase == SelectiveDownloadPhase.Ready &&
        selectiveState.selectedPaths.isNotEmpty()

    WindowBottomSheet(
        show = selectiveState.show,
        title = stringResource(R.string.selective_download),
        allowDismiss = false,
        onDismissRequest = {},
        backgroundColor = MiuixTheme.colorScheme.surface,
        startAction = {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.cancel),
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        endAction = {
            IconButton(
                onClick = onSave,
                enabled = canSave
            ) {
                Icon(
                    imageVector = MiuixIcons.Download,
                    contentDescription = stringResource(R.string.download_button),
                    modifier = Modifier.size(26.dp),
                    // ★橙色图标不要用 primary（半透明，暗色下压成深棕 ~1.9:1）；
                    //   禁用态也别硬编码 Color.Gray，跟随主题次级文字色
                    tint = if (canSave) accentFg() else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 20.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = when (selectiveState.phase) {
                            SelectiveDownloadPhase.Caching -> stringResource(R.string.selective_download_caching)
                            SelectiveDownloadPhase.Ready -> stringResource(
                                R.string.selective_download_ready,
                                selectiveState.selectedPaths.size,
                                selectiveState.items.size
                            )
                            SelectiveDownloadPhase.Saving -> stringResource(R.string.selective_download_saving)
                            SelectiveDownloadPhase.Error -> selectiveState.errorMessage ?: stringResource(R.string.selective_download_error)
                            SelectiveDownloadPhase.Idle -> ""
                        },
                        fontWeight = FontWeight.Medium
                    )
                    if (selectiveState.phase == SelectiveDownloadPhase.Caching ||
                        selectiveState.phase == SelectiveDownloadPhase.Saving
                    ) {
                        LinearProgressIndicator(
                            progress = selectiveState.progress.coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(ContinuousRoundedRectangle(3.dp))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectiveState.progressLabel.ifBlank { "0/?" },
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = selectiveState.progressText,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            if (selectiveState.phase == SelectiveDownloadPhase.Ready && selectiveState.items.isNotEmpty()) {
                item {
                    SelectableMediaWaterfall(
                        items = selectiveState.items,
                        selectedPaths = selectiveState.selectedPaths,
                        onToggle = onToggleItem
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryPage(
    uiState: MainUiState,
    manualInputLinks: Boolean = false,
    showInputDialog: Boolean = false,
    onShowInputDialogChange: (Boolean) -> Unit,
    statusListState: androidx.compose.foundation.lazy.LazyListState,
    onDownload: () -> Unit,
    onManualInputDownload: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onCopyUrl: (String) -> Unit,
    onBrowseUrl: (String) -> Unit,

    onRetryTask: (com.neoruaa.xhsdn.data.DownloadTask) -> Unit,
    onContinueTask: (com.neoruaa.xhsdn.data.DownloadTask) -> Unit,
    onWebCrawlTask: (com.neoruaa.xhsdn.data.DownloadTask) -> Unit,
    onStopTask: (com.neoruaa.xhsdn.data.DownloadTask) -> Unit,
    onDeleteTask: (com.neoruaa.xhsdn.data.DownloadTask) -> Unit,
    detectedXhsLink: String?,
    detectedPlatform: String? = null,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onHomepageDownload: (String) -> Unit = {},
    onGoHomepage: () -> Unit = {},
    onOpenFailureLog: () -> Unit = {},
    homePreview: HomePreviewState = HomePreviewState.Idle,
    onHomepageConfirm: (HomepageBatchStore.Range, Int, Boolean, Boolean, Set<String>) -> Unit = { _, _, _, _, _ -> },
    onFallbackCrawl: (String) -> Unit = {},
    onClipboardBubbleActivate: () -> Unit = {},
    onDismissPrompt: () -> Unit,
    modifier: Modifier = Modifier,
    nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection? = null
) {
    val tasks by com.neoruaa.xhsdn.data.TaskManager.getAllTasks().collectAsStateWithLifecycle(initialValue = emptyList())
    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // v3.1.6 列表底部让位（根治「FAB 压住最后一张卡操作按钮」）：
    //   FAB 只挂在 HistoryPage（见下方 GlassPrimaryWrap），它占 navbar+FabBottomOffset .. +FabSize 这条竖带。
    //   把**列表视口**底边直接钳到 FAB 上缘 → 内容在滚动过程中也不可能画进 FAB 底下（旧值 56dp 是
    //   v3.1.2 之前 38dp 老 dock 时代的残值：既让内容钻到 dock 胶囊下面 18dp，又把最后一张卡送进 FAB）。
    val listBottomReserve = GlassSpec.FabBottomOffset + GlassSpec.FabSize   // v0.2：91 + 56 = 147dp（FAB 46→56）
    val activeTask = tasks.firstOrNull {
        it.status == com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING || it.status == com.neoruaa.xhsdn.data.TaskStatus.QUEUED
    }

    var taskToDelete by remember { mutableStateOf<com.neoruaa.xhsdn.data.DownloadTask?>(null) }

    if (taskToDelete != null) {
        // v3.0.8：WindowDialog → GlassDialog（自绘玻璃卡，真机不再透明）
        GlassDialog(
            title = stringResource(R.string.delete_task_dialog_title),
            summary = stringResource(R.string.delete_task_dialog_message),
            confirmText = stringResource(R.string.apply),
            cancelText = stringResource(R.string.cancel),
            onConfirm = {
                taskToDelete?.let { onDeleteTask(it) }
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }

    val dark = isSystemInDarkTheme()
    // 主按钮前景字：暗色启用态按令牌用浅橙字（#FFC9A3），亮色/禁用态保持白
    val primaryButtonFg = if (dark) GlassTokens.OrangeOnDark else Color.White
    Box(modifier = modifier) {
        // v3.0.2 mockup：任务卡/页签直接浮在壁纸上——去掉页面级 Chrome 底色，壁纸全透
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 平台页签：抖音 / 小红书 / 快手 / 失败（失败 = 全平台失败任务，按状态归类）
                val tabScope: (com.neoruaa.xhsdn.data.DownloadTask) -> Boolean = when (selectedTab) {
                    0 -> { t -> t.source == "douyin" || t.source == "douyin_home" }
                    1 -> { t -> t.source != "douyin" && t.source != "douyin_home" && t.source != "kuaishou" }
                    2 -> { t -> t.source == "kuaishou" }
                    else -> { t -> t.status == com.neoruaa.xhsdn.data.TaskStatus.FAILED }
                }
                val counts = listOf(
                    tasks.count { it.source == "douyin" || it.source == "douyin_home" },
                    tasks.count { it.source != "douyin" && it.source != "douyin_home" && it.source != "kuaishou" },
                    tasks.count { it.source == "kuaishou" },
                    tasks.count { it.status == com.neoruaa.xhsdn.data.TaskStatus.FAILED }
                )
                val filterLabels = listOf("抖音", "小红书", "快手", "失败").mapIndexed { i, label -> "$label ${counts[i]}" }
                TabRowWithContour(
                    tabs = filterLabels,
                    selectedTabIndex = selectedTab,
                    fontSize = 11.5.sp,
                    height = 36.dp,
                    // v3.0.2 mockup .tabs: 轨道 r22(pad3 gap3 白22%/暗6%)，内胶囊 r18
                    cornerRadius = 18.dp,
                    colors = TabRowDefaults.tabRowColors(
                        backgroundColor = if (dark) Color(0x10FFFFFF) else Color(0x38FFFFFF),
                        selectedBackgroundColor = Color.Transparent,
                        selectedBackgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(GlassTokens.GradientStart, GlassTokens.GradientEnd)
                        ),
                        contentColor = if (dark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight,
                        selectedContentColor = Color.White
                    ),
                    itemSpacing = 3.dp,
                    onTabSelected = onTabSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )

                // 根据页签过滤任务：0=抖音 / 1=小红书(其余) / 2=快手 / 3=失败(全平台)
                val filteredTasks = tasks.filter(tabScope)
                // 当前页签内「已成功(COMPLETED)」任务数：>0 时显示「清除已完成」一键按钮（失败/下载中保留可重试）
                val completedInTab = filteredTasks.count { it.status == com.neoruaa.xhsdn.data.TaskStatus.COMPLETED }
                var showClearCompletedDialog by remember { mutableStateOf(false) }
                if (showClearCompletedDialog) {
                    val clearTabLabel = when (selectedTab) {
                        0 -> "douyin"; 1 -> "xhs"; 2 -> "kuaishou"; else -> "failed"
                    }
                    val clearCtx = LocalContext.current
                    // v3.0.4：WindowDialog 底弹样式按钮贴 nav 且无 dim → 改居中自绘玻璃卡
                    // （与长按菜单同语言：圆角22 + 近实玻璃底 + 折射描边 + 渐变确认钮；原生 Dialog 自带 dim）
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showClearCompletedDialog = false },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 44.dp)
                                .fillMaxWidth()
                                .shadow(12.dp, RoundedCornerShape(22.dp))
                                .clip(ContinuousRoundedRectangle(22.dp))
                                .background(if (dark) Color(0xF21E1B28) else Color(0xF7FFFFFF))
                                .border(1.dp, if (dark) GlassTokens.BorderDark else GlassTokens.BorderLight, RoundedCornerShape(22.dp))
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "清除已完成任务",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "确定清除当前页签的 $completedInTab 条已成功下载的任务？\n（失败 / 下载中的任务会保留，可继续重试）",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = if (dark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight
                            )
                            Spacer(Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    text = stringResource(R.string.cancel),
                                    onClick = { showClearCompletedDialog = false },
                                    modifier = Modifier.weight(1f)
                                )
                                // v3.1.5：内层必须显式给高度，否则按 Miuix 默认量测后被托盘 Box 顶对齐 → 文字偏上
                                GlassPrimaryWrap(
                                    modifier = Modifier.weight(1f).height(BtnHeight),
                                    cornerRadius = BtnCornerRadius
                                ) {
                                    Button(
                                        onClick = {
                                            EventTracker.track(clearCtx, "clear_completed", mapOf("tab" to clearTabLabel, "n" to completedInTab.toString()))
                                            com.neoruaa.xhsdn.data.TaskManager.clearCompletedTasks(tabScope)
                                            showClearCompletedDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth().height(BtnHeight),
                                        colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White),
                                        insideMargin = PaddingValues(horizontal = BtnBarPadH, vertical = 0.dp)
                                    ) {
                                        Text("清除", color = Color.White, fontSize = BtnFontSize, fontWeight = BtnFontWeight, letterSpacing = BtnLetterSpacing)
                                    }
                                }
                            }
                        }
                    }
                }
                // v3.0.2 mockup .clear: 页签下方右对齐玻璃小胶囊（completedInTab>0 且有列表时）
                // v3.0.10：mockup 页面主体 .bdy 是 gap:8px 统一节奏——胶囊与下方列表间距对齐 8dp
                //（此前 vertical=2dp 且列表无顶部 padding，胶囊几乎贴死第一张卡，用户实测打回）
                if (completedInTab > 0 && filteredTasks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(ContinuousRoundedRectangle(999.dp))
                                .background(if (dark) Color(0x1AFFFFFF) else Color(0x66FFFFFF))
                                .border(1.dp, if (dark) Color(0x29FFFFFF) else Color(0x99FFFFFF), ContinuousRoundedRectangle(999.dp))
                                .clickable { showClearCompletedDialog = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "清除已完成 $completedInTab",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight
                            )
                        }
                    }
                }
                if (filteredTasks.isEmpty()) {
                    // ===== UI v2 S6 空态双场景 =====
                    if (selectedTab >= 3) {
                        // S6-2 失败聚合空态：统计条（今日 / 近 7 天 / 累计）+ 引导
                        val nowMs = System.currentTimeMillis()
                        val todayCal = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        val todayStart = todayCal.timeInMillis
                        val weekAgo = nowMs - 7L * 24 * 3600 * 1000
                        val failedTasks = tasks.filter { it.status == com.neoruaa.xhsdn.data.TaskStatus.FAILED }
                        val failedToday = failedTasks.count { (it.completedAt ?: it.createdAt) >= todayStart }
                        val failedWeek = failedTasks.count { (it.completedAt ?: it.createdAt) >= weekAgo }
                        val failedTotal = failedTasks.size
                        val tPrimary = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight
                        val tSecondary = if (dark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight
                        val statCardBg = if (dark) GlassTokens.CardDark else GlassTokens.CardLight
                        val emptyCtx = LocalContext.current
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                // v3.1.6：与任务列表同样让位给 FAB（空态也从页面根部铺到底，FAB 会压住末行）
                                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = listBottomReserve),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 统计条（跨平台聚合）
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 22.dp,
                                colors = CardDefaults.defaultColors(color = statCardBg)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp)
                                ) {
                                    listOf("今日失败" to failedToday, "近 7 天" to failedWeek, "累计" to failedTotal).forEach { (label, value) ->
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "$value",
                                                color = if (value > 0) GlassTokens.OrangeTextDeep else tPrimary,
                                                // v3.1.5：21sp 不在设计稿字号并集内，收口到 20sp
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            Text(text = label, color = tSecondary, fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(28.dp))
                            Text(
                                text = "干干净净，没有失败任务",
                                color = tPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "下载失败会自动收进这里，可一键重试或查看原因",
                                color = tSecondary,
                                fontSize = 12.5.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(24.dp))
                            val goPaste: () -> Unit = {
                                EventTracker.track(emptyCtx, "empty_go_download", mapOf("where" to "failed"))
                                if (manualInputLinks) onShowInputDialogChange(true) else onDownload()
                            }
                            GlassPrimaryWrap(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(BtnHeight),
                                cornerRadius = BtnCornerRadius
                            ) {
                                Button(
                                    onClick = goPaste,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(BtnHeight),
                                    colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White),
                                insideMargin = PaddingValues(horizontal = BtnBarPadH, vertical = 0.dp)
                                ) {
                                    Text("去下载一个试试", color = Color.White, fontSize = BtnFontSize, fontWeight = BtnFontWeight, letterSpacing = BtnLetterSpacing)
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "仍失败可查看失败日志定位原因",
                                color = if (dark) GlassTokens.DownloadingDark else GlassTokens.OrangeTextDeep,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        EventTracker.track(emptyCtx, "failure_log_open", mapOf("from" to "empty"))
                                        onOpenFailureLog()
                                    }
                                    .padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else if (tasks.isEmpty()) {
                        // S6-1 全局空态：双入口引导（粘贴链接 / 批量下载博主主页）
                        val tPrimary = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight
                        val tSecondary = if (dark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight
                        val ctaCardBg = if (dark) GlassTokens.CardDark else GlassTokens.CardLight
                        val emptyCtx = LocalContext.current
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                // v3.1.6：同上，让位给 FAB
                                .padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = listBottomReserve),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 玻璃装饰圆
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(ctaCardBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp),
                                    tint = if (dark) GlassTokens.DownloadingDark else GlassTokens.OrangeTextDeep
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = "还没有下载任务",
                                color = tPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "从抖音 / 快手 / 小红书分享或粘贴链接，无水印保存在本机",
                                color = tSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(28.dp))
                            val pasteAction: () -> Unit = {
                                EventTracker.track(emptyCtx, "empty_paste", mapOf("where" to "tasks"))
                                if (manualInputLinks) onShowInputDialogChange(true) else onDownload()
                            }
                            GlassPrimaryWrap(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(BtnHeight),
                                cornerRadius = BtnCornerRadius
                            ) {
                                Button(
                                    onClick = pasteAction,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(BtnHeight),
                                    colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White),
                                insideMargin = PaddingValues(horizontal = BtnBarPadH, vertical = 0.dp)
                                ) {
                                    Text("粘贴链接", color = Color.White, fontSize = BtnFontSize, fontWeight = BtnFontWeight, letterSpacing = BtnLetterSpacing)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            // 次级玻璃按钮：批量下载博主主页（同 .btn 尺寸，只有底不同 —— 规范里
                            // .btn.ghost 与 .btn.primary 高度/圆角/字号完全一致）
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(BtnHeight)
                                    .clip(RoundedCornerShape(BtnCornerRadius))
                                    .clickable {
                                        EventTracker.track(emptyCtx, "empty_homepage", mapOf("where" to "tasks"))
                                        onGoHomepage()
                                    },
                                cornerRadius = BtnCornerRadius,
                                colors = CardDefaults.defaultColors(color = ctaCardBg)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("批量下载博主主页", color = tPrimary, fontSize = BtnFontSize, fontWeight = BtnFontWeight, letterSpacing = BtnLetterSpacing)
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = "分享的视频 / 图文会自动出现在这里",
                                color = tSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    } else {
                        // 平台页签为空但其他平台有任务：轻量提示，避免误导"全局没有任务"
                        val tPrimary = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight
                        val tSecondary = if (dark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Info,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = tSecondary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "该平台还没有任务",
                                color = tPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "粘贴其他平台的链接试试，或切换上方页签查看",
                                color = tSecondary,
                                fontSize = 12.5.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LaunchedEffect(filteredTasks.size) {
                        if (filteredTasks.isNotEmpty()) {
                            statusListState.animateScrollToItem(0)
                        }
                    }

                    LazyColumn(
                        state = statusListState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            // v3.1.6：仅留呼吸余量。底部让位已由外层视口（navPadding + listBottomReserve）负责，
                            // 这里原先再叠一层 `navPadding + 96.dp` 属重复预留，滚到底时会白白空掉 63dp。
                            bottom = 12.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        // v3.0.9：列表在 dock 上缘截断（padding 同时限制布局与绘制）。
                        // 设计稿用 backdrop-blur 遮住滚进 dock 区的内容；本项目无 blur，
                        // 穿透文字会直接堆在 dock 下方手势区（用户实测打回）→ 内容到 dock 上缘为止。
                        // v3.1.6：上缘进一步收到 **FAB 上缘**（navPadding + 137dp），见 listBottomReserve 注释。
                        modifier = if (nestedScrollConnection != null) {
                            Modifier.fillMaxSize()
                                .padding(bottom = navPadding + listBottomReserve)
                                .nestedScroll(nestedScrollConnection)
                        } else {
                            Modifier.fillMaxSize().padding(bottom = navPadding + listBottomReserve)
                        }
                    ) {
                        itemsIndexed(filteredTasks, key = { _, task -> task.id }) { _, task ->
                            val context = LocalContext.current
                            TaskCell(
                                task = task,
                                // 只有正在下载的任务才使用 uiState.mediaItems
                                // v3.0.7（P2-7）：filePaths 先过滤掉磁盘上已不存在的文件（数据迁移/用户清理），
                                // 避免完成卡渲染空白缩略格；全删则下方不渲染缩略图行。
                                mediaItems = if (task.filePaths.isNotEmpty()) {
                                    task.filePaths
                                        .filter { java.io.File(it).exists() }
                                        .map { MediaItem(it, detectMediaType(it)) }
                                } else if (task.status == com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING && uiState.mediaItems.isNotEmpty()) {
                                    uiState.mediaItems
                                } else {
                                    emptyList()
                                },

                                onCopyUrl = { onCopyUrl(task.noteUrl) },
                                onBrowseUrl = { onBrowseUrl(task.noteUrl) },
                                onRetry = { onRetryTask(task) },
                                onContinue = { onContinueTask(task) },
                                onWebCrawl = { onWebCrawlTask(task) },
                                onStop = { onStopTask(task) },
                                onDelete = { taskToDelete = task },
                                onHomepageDownload = { onHomepageDownload(task.noteUrl) },
                                onMediaClick = onMediaClick,
                                onClick = {
                                    // UI v2 S2：点任务卡进「任务详情三态页」；文件浏览（DetailActivity）下沉为已完成态的「查看文件」
                                    EventTracker.track(context, "task_detail_open", mapOf("source" to (task.source ?: "")))
                                    context.startActivity(TaskDetailActivity.newIntent(context, task.id))
                                }
                            )
                        }
                    }
                }
            }
        }

        // v3.1.5 FAB：按 LGv3 `.fab{width:46px;height:46px;border-radius:999px;right:16px}`
        //   由 60dp 圆角方 收到 **46dp 正圆**（尺寸依据见 GlassSpec 的文件头：已实测确认 LGv3
        //   的 px 是设备值而非缩略图值）。渐变由外层 GlassPrimaryWrap 画，本层不要重复画 —— 否则
        //   wrap 在 enabled=false 时不画渐变、这一层却照画，**下载中 FAB 看起来还是可点的橙钮**。
        GlassPrimaryWrap(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = GlassSpec.FabEndPadding, bottom = navPadding + FabBottomOffset),
            cornerRadius = FabSize / 2,
            enabled = !uiState.isDownloading
        ) {
            Box(
                modifier = Modifier
                    .size(FabSize)
                    .clip(CircleShape)
                    // 禁用态底色（wrap 在 disabled 时不画渐变，由这里兜住灰化外观）
                    .background(
                        if (uiState.isDownloading)
                            (if (isSystemInDarkTheme()) GlassTokens.DisabledPrimaryDark else GlassTokens.DisabledPrimaryLight)
                        else Color.Transparent
                    )
                    .clickable(enabled = !uiState.isDownloading) {
                        if (manualInputLinks) onShowInputDialogChange(true) else onDownload()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (manualInputLinks) MiuixIcons.Link else MiuixIcons.File,
                    contentDescription = if (uiState.isDownloading) "下载中" else if (manualInputLinks) "手动输入链接" else "粘贴下载",
                    modifier = Modifier.size(GlassSpec.FabIconSize),
                    tint = Color.White
                )
            }
        }

        // 剪贴板检测提示气泡（叠加层，靠近底部按钮）
        if (detectedXhsLink != null && !uiState.isDownloading && !manualInputLinks) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = navPadding + ClipBubbleBottomOffset),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val bubbleLabel = if (detectedPlatform == "douyin")
                    stringResource(R.string.clipboard_douyin_link_detected)
                else
                    stringResource(R.string.clipboard_xhs_link_detected)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClipboardBubbleActivate() },
                    cornerRadius = 24.dp,
                    colors = CardDefaults.defaultColors(
                        // 液态：玻璃卡底 + 翠绿语义（v3）
                        color = if (dark) GlassTokens.CardDark else GlassTokens.CardLight
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Info,
                            contentDescription = null,
                            tint = if (dark) GlassTokens.SuccessGreenDark else GlassTokens.SuccessGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bubbleLabel,
                                fontWeight = FontWeight.Bold,
                                color = if (dark) GlassTokens.SuccessGreenDark else GlassTokens.SuccessGreen
                            )
                            Text(
                                text = detectedXhsLink,
                                fontSize = 12.sp,
                                color = (if (dark) GlassTokens.SuccessGreenDark else GlassTokens.SuccessGreen).copy(alpha = 0.7f),
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onDismissPrompt() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "×",
                                fontSize = 18.sp,
                                color = if (dark) GlassTokens.SuccessGreenDark else GlassTokens.SuccessGreen
                            )
                        }
                    }
                }
                // 三角形指针（紧贴卡片底部）
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(24.dp, 10.dp)
                ) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2, size.height)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = if (dark) GlassTokens.CardDark else GlassTokens.CardLight
                    )
                }
            }
        }

        // 输入分享链接对话框（放在 HistoryPage 内，避免键盘偏移异常）
        // v3.0.6（2026-09-10 真机 vs 设计稿 P0）：WindowDialog 底弹无玻璃面板、标题直浮列表 →
        // 改居中自绘玻璃卡（与清除确认弹窗同语言：r22 玻璃底 + 折射描边 + 渐变确认钮 + 原生 Dialog dim）
        if (showInputDialog) {
            val context = LocalContext.current
            val inputDark = isSystemInDarkTheme()
            val manualInputTitle = stringResource(R.string.manual_input_links)
            val enterXhsUrl = stringResource(R.string.enter_share_url)
            val cancelText = stringResource(R.string.cancel)
            val downloadButtonText = stringResource(R.string.download_button)
            val pleaseEnterUrl = stringResource(R.string.please_enter_url)

            var inputLink by remember { mutableStateOf("") }

            androidx.compose.ui.window.Dialog(
                onDismissRequest = {
                    onShowInputDialogChange(false)
                    inputLink = ""
                },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 44.dp)
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(22.dp))
                        .clip(ContinuousRoundedRectangle(22.dp))
                        .background(if (inputDark) Color(0xF21E1B28) else Color(0xF7FFFFFF))
                        .border(1.dp, if (inputDark) GlassTokens.BorderDark else GlassTokens.BorderLight, RoundedCornerShape(22.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = manualInputTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (inputDark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = enterXhsUrl,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = if (inputDark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight
                    )
                    Spacer(Modifier.height(14.dp))
                    TextField(
                        value = inputLink,
                        onValueChange = { inputLink = it },
                        label = stringResource(R.string.share_url_hint),
                        useLabelAsPlaceholder = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ContinuousRoundedRectangle(16.dp)),
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            text = cancelText,
                            onClick = {
                                onShowInputDialogChange(false)
                                inputLink = ""
                            },
                            modifier = Modifier.weight(1f)
                        )
                        GlassPrimaryWrap(
                            modifier = Modifier.weight(1f).height(BtnHeight),
                            cornerRadius = BtnCornerRadius
                        ) {
                            Button(
                                onClick = {
                                    if (inputLink.isNotEmpty()) {
                                        // 执行手动输入下载
                                        onManualInputDownload(inputLink)

                                        // 关闭对话框并清空输入
                                        onShowInputDialogChange(false)
                                        inputLink = ""
                                    } else {
                                        Toast.makeText(context, pleaseEnterUrl, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                // 与主页下载页主按钮同规格（.btn）：显式填满托盘高度 + 统一字号字重字距
                                modifier = Modifier.fillMaxWidth().height(BtnHeight),
                                colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White),
                                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = downloadButtonText,
                                    color = Color.White,
                                    fontSize = BtnFontSize,
                                    fontWeight = BtnFontWeight,
                                    letterSpacing = BtnLetterSpacing
                                )
                            }
                        }
                    }
                }
            }
        }

    }
}

/** 左滑删除锚点（v3.0.12 anchoredDraggable）：Closed=静止，Revealed=滑出删除层 */
private enum class SwipeAnchor { Closed, Revealed }

/**
 * 任务 Cell 组件
 */
@Composable
private fun TaskCell(
    task: com.neoruaa.xhsdn.data.DownloadTask,
    mediaItems: List<MediaItem> = emptyList(),
    onCopyUrl: () -> Unit,
    onBrowseUrl: () -> Unit,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onWebCrawl: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onHomepageDownload: () -> Unit = {},
    onMediaClick: (MediaItem) -> Unit = {},
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 玻璃语汇语义色（明/暗两态；暗色取高亮变体，保证半透玻璃卡上可读）
    val dark = isSystemInDarkTheme()
    val statusColor = when (task.status) {
        com.neoruaa.xhsdn.data.TaskStatus.QUEUED -> GlassTokens.QueueGray
        com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING -> if (dark) GlassTokens.DownloadingDark else GlassTokens.DownloadingLight
        com.neoruaa.xhsdn.data.TaskStatus.COMPLETED -> if (dark) GlassTokens.SuccessGreenDark else GlassTokens.SuccessGreen
        com.neoruaa.xhsdn.data.TaskStatus.FAILED -> if (dark) GlassTokens.FailRedDark else GlassTokens.FailRed
        com.neoruaa.xhsdn.data.TaskStatus.WAITING_FOR_USER -> if (dark) GlassTokens.WaitAmberDark else GlassTokens.WaitAmber
    }
    // 玻璃文本 token（标题/元信息）
    val tPrimary = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight
    val tSecondary = if (dark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight
    // 平台点标（来源标识）—— v3.1.6 起色值与平台名都走 ui/glass/PlatformBrand.kt（全 App 唯一入口）
    val isDy = task.source == "douyin" || task.source == "douyin_home"
    val isKs = task.source == "kuaishou"
    val sourceLabel = platformLabel(task.source)
    val sourceColor = platformBrandColor(task.source)
    
    val statusText = when (task.status) {
        com.neoruaa.xhsdn.data.TaskStatus.QUEUED -> stringResource(R.string.task_status_queued)
        com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING -> stringResource(R.string.task_status_downloading)
        com.neoruaa.xhsdn.data.TaskStatus.COMPLETED -> stringResource(R.string.task_status_completed)
        com.neoruaa.xhsdn.data.TaskStatus.FAILED -> stringResource(R.string.task_status_failed)
        com.neoruaa.xhsdn.data.TaskStatus.WAITING_FOR_USER -> stringResource(R.string.task_status_waiting_for_user)
    }
    
    val typeText = when (// Check if this is a web crawl task (created from WebViewActivity)
        task.noteType) {
        com.neoruaa.xhsdn.data.NoteType.UNKNOWN if (task.noteUrl?.contains("xhslink.com") == true ||
                task.noteUrl?.contains("xiaohongshu.com") == true ||
                task.noteUrl?.startsWith("http") == true && task.totalFiles > 0) -> stringResource(R.string.note_type_web_crawl)
        com.neoruaa.xhsdn.data.NoteType.IMAGE -> stringResource(R.string.note_type_image)
        com.neoruaa.xhsdn.data.NoteType.VIDEO -> stringResource(R.string.note_type_video)
        com.neoruaa.xhsdn.data.NoteType.UNKNOWN -> stringResource(R.string.note_type_unknown)
    }
    
    // 左滑露出删除按钮（卡片左移 REVEAL.dp）；长按弹出菜单（复制链接 / 删除）作为左滑删除的兜底
    // v3.0.12：detectHorizontalDragGestures 在真机（触控轨迹带垂直分量）上会被 LazyColumn
    // 垂直滚动仲裁抢走 → 左滑失效（用户小米14 实测）。换官方 anchoredDraggable：
    // 拖拽锚点机制自带正确的轴向竞争与 settle/fling 语义（foundation 1.11 API）
    val swipeScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val revealPx = with(density) { 76.dp.toPx() }
    val dragState = remember(revealPx) {
        androidx.compose.foundation.gestures.AnchoredDraggableState(
            initialValue = SwipeAnchor.Closed,
            anchors = androidx.compose.foundation.gestures.DraggableAnchors {
                SwipeAnchor.Closed at 0f
                SwipeAnchor.Revealed at -revealPx
            }
        )
    }
    fun collapseSwipe() = swipeScope.launch {
        dragState.anchoredDrag { anchors ->
            dragTo(anchors.positionOf(SwipeAnchor.Closed))
        }
    }
    var showTaskMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(24.dp))
    ) {
        // 删除按钮（满铺红层，α=左滑进度；静止 α=0 → 不透过玻璃卡底色显形，根治珊瑚/粉穿帮）
        val swipeOffset = if (dragState.offset.isNaN()) 0f else dragState.offset
        val deleteProgress = (-swipeOffset / revealPx).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GlassTokens.FailRed.copy(alpha = deleteProgress))
                .clickable { onDelete() }
        ) {
            Text(
                text = "删除",
                color = Color.White.copy(alpha = deleteProgress),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            )
        }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .offset { IntOffset(if (dragState.offset.isNaN()) 0 else dragState.offset.roundToInt(), 0) }
                .clip(ContinuousRoundedRectangle(24.dp))
                // v3.0.2 mockup .lg：卡底=玻璃白（亮 55% / 暗 15%），非实色 surfaceVariant
                .background(if (dark) GlassTokens.CardDark else GlassTokens.CardLight)
                // v3.0 液态折射描边：1px 半透白边缘光（明暗两态 token）
                .border(
                    width = 1.dp,
                    color = if (dark) GlassTokens.BorderDark else GlassTokens.BorderLight,
                    shape = ContinuousRoundedRectangle(24.dp)
                )
                // v3.0.12 手势区覆盖整卡（含 padding 边带）→ 手势放 padding 之前；
                // combinedClickable 在前、anchoredDraggable 在后（内层）的顺序不能动：
                // Main pass 内层先跑，拖拽检测必须最内层才能先认领 touch slop。
                .combinedClickable(
                    // 已滑出（reveal）时点击先收回，不进详情
                    onClick = {
                        if (deleteProgress > 0f) collapseSwipe() else onClick?.invoke()
                    },
                    onLongClick = { showTaskMenu = true }
                )
                .anchoredDraggable(
                    state = dragState,
                    orientation = androidx.compose.foundation.gestures.Orientation.Horizontal
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            // v3.0.2 mockup：卡片内子项间距统一 gap:7px（无额外 Spacer）
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // ── R1 平台行：平台点标（左）+ 状态胶囊（右） ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)  // mockup .plat gap:5px
                ) {
                    // 平台色点（8dp 圆点 + 1.5dp 白环折射光，mockup .dot box-shadow ring）
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(ContinuousRoundedRectangle(999.dp))
                            .background(sourceColor)
                            .border(1.5.dp, Color.White.copy(alpha = 0.85f), ContinuousRoundedRectangle(999.dp))
                    )
                    // mockup .plat span: 字号 11.5/650，颜色=正文近黑(非平台色，平台色只给点)
                    Text(
                        text = sourceLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tPrimary
                    )
                }

                // 状态胶囊（v3.0.2 mockup .stat: 浅底+同色系深字，非实色底白字）
                //   st-f 红字#E23145+底rgba(255,77,94,.14) / st-ok 绿#00895C+底.15 / st-dl 橙#E2560E+橙渐变淡底
                //   dark 用高亮变体：底 alpha .16-.2 + 亮字（如 #FF9AA6 / #7AE0B8 / #FFB37A）
                val statBgAlpha = if (dark) 0.18f else 0.14f
                Box(
                    modifier = Modifier
                        .clip(ContinuousRoundedRectangle(999.dp))
                        .background(statusColor.copy(alpha = statBgAlpha))
                        .padding(horizontal = 9.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.5.sp,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── R2 主标题（mockup .title: 13.5/650/lh1.4/clamp2，间距 gap:7） ──
            Text(
                text = task.noteTitle ?: task.noteUrl,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = tPrimary,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // ── R3 元信息行：类型·数量（左）+ 时间（右，次级灰） ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.task_info_format, typeText, task.totalFiles),
                        fontSize = 11.sp,
                        color = tSecondary
                    )

                    if (task.failedFiles > 0) {
                        Text(
                            text = stringResource(R.string.failed_files_format, task.failedFiles),
                            fontSize = 11.sp,
                            color = if (dark) GlassTokens.FailRedDark else GlassTokens.FailRed
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = formatTime(task.createdAt),
                        fontSize = 10.5.sp,
                        color = tSecondary
                    )
                    // v3.1 入口层重构（§6.1-7）：长按菜单里的「主页下载」**不可发现** →
                    // 抖音/快手任务卡在时间右侧补一个常驻小胶囊，点击直接切 tab 并解析该作者。
                    // 内联在元信息行（而非单起一行）—— 避免每张卡多出 26dp 垂直空白。
                    if (isDy || isKs) {
                        Box(
                            modifier = Modifier
                                .clip(ContinuousRoundedRectangle(999.dp))
                                .background(accentBg(lightAlpha = 0.13f))
                                .clickable { onHomepageDownload() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.task_card_homepage_cd),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentFg()
                            )
                        }
                    }
                }
            }

            // 进度条（仅下载中显示；mockup .prog: h5/r3/浅灰底+橙→珊瑚渐变，无文本行）
            if (task.totalFiles > 0 && task.status == com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(ContinuousRoundedRectangle(3.dp))
                        .background(tPrimary.copy(alpha = 0.10f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(task.progress.coerceIn(0f, 1f))
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(GlassTokens.GradientStart, GlassTokens.GradientEnd)
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 媒体预览网格（mockup .thumbs: gap6, 52dp/radius14/白描边折射）
            // v3.0.12：去掉 horizontalScroll——它是横向滚动容器，会把落在缩略图行上的
            // 左滑手势 slop 先消费掉，父级 anchoredDraggable 弃权 → 用户报「只有部分区域能左滑」。
            // 改为最多显示 6 张（卡内宽约容纳 6×58dp），更多文件进任务详情页查看。
            if (mediaItems.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    mediaItems.take(6).forEach { item ->
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(ContinuousRoundedRectangle(14.dp))
                                .background(MiuixTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    if (dark) Color(0x24FFFFFF) else Color(0x99FFFFFF),
                                    ContinuousRoundedRectangle(14.dp)
                                )
                                .clickable { onMediaClick(item) }
                        ) {
                            val bitmap = rememberThumbnail(item)
                            bitmap?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                        .clip(ContinuousRoundedRectangle(14.dp))
                                )
                            }
                        }
                    }
                    // v3.0.9：移除完成卡缩略图行末位的橙色下载钮（v3.0.7 P2-6 按设计稿补的「重新下载」入口）。
                    // 用户裁定：文件下载完已自动保存到系统相册，「下载」图标语义重复且误导 → 删除，列表更干净。
                }
            }
        }

        // 操作按钮行（mockup .btns gap:7；已完成无按钮=无此段）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            val isDownloading = task.status == com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING ||
                                task.status == com.neoruaa.xhsdn.data.TaskStatus.QUEUED

            if (isDownloading) {
                 // v3.1.5：几何/字号统一走 GlassSpec（`.btn` 52dp / r26 / 14.5sp / 650）。
                 //（v3.0.10 曾按 mockup `.btn-grad` 缩略图值 h38/r19 实现，属误读——缩略图不是设备值）
                 GlassPrimaryWrap(
                     modifier = Modifier.weight(1f).height(BtnHeight),
                     cornerRadius = BtnCornerRadius
                 ) {
                     Button(
                         onClick = onStop,
                         modifier = Modifier.fillMaxWidth().height(BtnHeight),
                         colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White),
                         insideMargin = PaddingValues(horizontal = BtnBarPadH, vertical = 0.dp)
                     ) {
                         Text("停止", color = Color.White, fontSize = BtnFontSize, fontWeight = BtnFontWeight, letterSpacing = BtnLetterSpacing)
                     }
                 }
            } else {

                // 等待用户选择状态 (显示 坚持下载/网页爬取)
                if (task.status == com.neoruaa.xhsdn.data.TaskStatus.WAITING_FOR_USER) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 提示语
                        Text(
                            text = stringResource(R.string.official_limitation_tip),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassPrimaryWrap(
                                modifier = Modifier.weight(1f).height(BtnHeight),
                                cornerRadius = BtnCornerRadius
                            ) {
                                Button(
                                    onClick = onContinue,
                                    modifier = Modifier.fillMaxWidth().height(BtnHeight),
                                    colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White),
                                insideMargin = PaddingValues(horizontal = BtnBarPadH, vertical = 0.dp)
                                ) {
                                    Text("坚持下载", color = Color.White, fontSize = BtnFontSize, fontWeight = BtnFontWeight, letterSpacing = BtnLetterSpacing)
                                }
                            }
                            Button(
                                onClick = onWebCrawl,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(BtnHeight)
                                    .clip(ContinuousRoundedRectangle(BtnCornerRadius))
                                    .border(
                                        1.dp,
                                        if (dark) GlassTokens.BorderDark else GlassTokens.BorderLight,
                                        ContinuousRoundedRectangle(BtnCornerRadius)
                                    ),
                                colors = ButtonDefaults.buttonColors(
                                    // v3.0.8：原来用 MiuixTheme.colorScheme.surface（半透）→ 浮在玻璃卡上近乎隐形；
                                    // 改玻璃次级底（亮 45% 白 / 暗 10% 白）+ 折射描边，与主按钮形成主次对比
                                    if (dark) Color(0x1AFFFFFF) else Color(0x73FFFFFF),
                                    if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight
                                ),
                                insideMargin = PaddingValues(horizontal = BtnBarPadH, vertical = 0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.web_crawl_option),
                                    color = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight,
                                    fontSize = BtnFontSize,
                                    fontWeight = BtnFontWeight,
                                    letterSpacing = BtnLetterSpacing
                                )
                            }
                        }
                    }
                } else {
                    // 重试按钮（仅失败任务显示）v3.1.5：统一 .btn 规格
                    if (task.status == com.neoruaa.xhsdn.data.TaskStatus.FAILED) {
                        GlassPrimaryWrap(
                            modifier = Modifier.weight(1f).height(BtnHeight),
                            cornerRadius = BtnCornerRadius
                        ) {
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.fillMaxWidth().height(BtnHeight),
                                colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White),
                                insideMargin = PaddingValues(horizontal = BtnBarPadH, vertical = 0.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.retry),
                                    color = Color.White,
                                    fontSize = BtnFontSize,
                                    fontWeight = BtnFontWeight,
                                    letterSpacing = BtnLetterSpacing
                                )
                            }
                        }
                    }
                }
            }
        }
        } // card Column body

        // 长按菜单：主页下载（抖音任务）/ 复制链接 / 删除（左滑删除在部分真机失效时的兜底交互）
        val isDouyinTask = task.source == "douyin" || task.source == "douyin_home" || UrlUtils.isDouyinLink(task.noteUrl)
        val menuActions = remember(task.source, task.noteUrl) {
            buildList {
                if (isDouyinTask) add("homepage")
                add("copy")
                add("delete")
            }
        }
        // v3.0.4 长按菜单玻璃化：自绘圆角浮层（近实玻璃底 + 折射描边 + 删除项 danger 红），
        // 替换 Miuix WindowListPopup 的白底直角样式；Popup focusable 提供点外关闭
        if (showTaskMenu) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = { showTaskMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 4.dp, end = 4.dp)
                        .widthIn(min = 176.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .clip(ContinuousRoundedRectangle(20.dp))
                        .background(if (dark) Color(0xF21E1B28) else Color(0xF2FFFFFF))
                        .border(1.dp, if (dark) GlassTokens.BorderDark else GlassTokens.BorderLight, RoundedCornerShape(20.dp))
                        .padding(vertical = 6.dp)
                ) {
                    menuActions.forEachIndexed { index, action ->
                        val text = when (action) {
                            "homepage" -> stringResource(R.string.homepage_download)
                            "copy" -> stringResource(R.string.copy_link)
                            else -> stringResource(R.string.delete_task_menu)
                        }
                        val isDelete = action == "delete"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showTaskMenu = false
                                    when (action) {
                                        "homepage" -> onHomepageDownload()
                                        "copy" -> onCopyUrl()
                                        else -> onDelete()
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    isDelete -> if (dark) GlassTokens.FailRedDark else GlassTokens.FailRed
                                    else -> if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight
                                }
                            )
                        }
                    }
                }
            }
        }
    } // Box (swipe reveal wrapper)
}

/**
 * 格式化时间戳为可读字符串
 */
private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
private fun rememberThumbnail(item: MediaItem): ImageBitmap? {
    // 先检查缓存
    val cachedBitmap = thumbnailCache.get(item.path)
    if (cachedBitmap != null) {
        return cachedBitmap
    }
    
    val state = produceState<ImageBitmap?>(initialValue = null, key1 = item.path) {
        value = withContext(Dispatchers.IO) {
            // 再次检查缓存（可能在等待期间被其他协程加载）
            thumbnailCache.get(item.path)?.let { return@withContext it }
            
            val file = File(item.path)
            if (!file.exists()) return@withContext null
            val bitmap = runCatching {
                when (item.type) {
                    MediaType.IMAGE -> decodeSampledBitmap(file.path, 200, 200)?.asImageBitmap()
                    MediaType.VIDEO -> createVideoThumbnail(file)?.asImageBitmap()
                    MediaType.OTHER -> null
                }
            }.getOrNull()
            
            // 存入缓存
            bitmap?.let { thumbnailCache.put(item.path, it) }
            bitmap
        }
    }
    return state.value
}


private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): android.graphics.Bitmap? {
    val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(path, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    options.inJustDecodeBounds = false
    return android.graphics.BitmapFactory.decodeFile(path, options)
}

private fun calculateInSampleSize(options: android.graphics.BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

private fun createVideoThumbnail(file: File): android.graphics.Bitmap? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        android.media.ThumbnailUtils.createVideoThumbnail(
            file,
            Size(640, 360),
            null
        )
    } else {
        @Suppress("DEPRECATION")
        android.media.ThumbnailUtils.createVideoThumbnail(
            file.path,
            android.provider.MediaStore.Video.Thumbnails.MINI_KIND
        )
    }
}

/**
 * 平台 → 任务栏页签索引：抖音=0 / 小红书=1 / 快手=2
 */
private fun platformTabIndex(platform: String?): Int = when (platform) {
    "douyin" -> 0
    "kuaishou" -> 2
    else -> 1
}

/**
 * 判断 URL 是否为快手视频直链（与 WebViewActivity 的 isKuaishouVideo 判定保持一致）。
 * 用于从 WebView 嗅探结果里过滤出视频，排除图集图片。
 */
private fun isKuaishouVideoUrl(url: String): Boolean {
    val u = url.lowercase()
    // 只下载视频：先排除封面/图集/头像等图片资源（按扩展名与常见图片路径特征）
    if (u.endsWith(".jpg") || u.endsWith(".jpeg") || u.endsWith(".png") ||
        u.endsWith(".webp") || u.endsWith(".gif") ||
        u.contains(".jpg?") || u.contains(".png?") || u.contains(".webp?") ||
        u.contains("/img/") || u.contains("cover") || u.contains("water")) return false
    return u.contains("kwaicdn.com") || u.contains("chenzhongtech.com") ||
        u.contains("gifshow.com") || u.contains("kwai-player") ||
        u.contains("kwai") || u.contains("kuaishou") ||
        u.contains(".mp4") || u.contains(".m3u8")
}

/**
 * 底部主标签栏：视频下载 / 主页下载。
 */
@Composable
private fun MainTabBar(
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // v3.0.2 mockup .nav：底部浮起玻璃胶囊 dock（radius26 / pad6 4），选中项 .nv.on 玻璃胶囊+橙字
    // v3.0.9：设计稿的玻璃=白50%+backdrop-blur(26px)（雾白磨砂）；Android 无 backdrop blur，
    // 纯半透白会出现「内容滚过 dock 直接穿透」的丑象（用户实测打回）。
    // 提实度到等效磨砂观感：亮≈白91% / 暗≈深灰紫90%，补顶内高光1.5dp+底部投影（对齐 .lg box-shadow）。
    val items = listOf("首页下载", "任务", "设置")
    // ★2026-10-11 对齐平台规范（用户反馈"底部按钮还是小"，此前只有 11sp 纯文字，
    // 低于 Material 底部导航标准的 12sp，也没有规范要求的 24dp 图标）：
    //   Material 1 bottom navigation（icon+label 固定式）：Icon 24×24dp，
    //     文字 Roboto Regular **12sp（未选）/ 14sp（选中）**，容器高 56dp；
    //   Material 3 navigation bar：Label Medium = 12sp，且**导航项必须配图标**。
    // 本项目 dock 是 60dp 级浮起玻璃胶囊（比 56dp 标准容器略大），故取两者中值
    // **13sp 统一**（不做选中放大，避免切页时字号跳动；强调交给字重 + 颜色）。
    // 之前只有文字、没有图标，是"看起来小/空"的主因 —— 11sp 在 60dp 胶囊里没有任何
    // 视觉锚点；补上 22dp 图标后文字恢复成"图标下的第二行"，11sp 才成立。
    val icons = listOf(MiuixIcons.TopDownloads, MiuixIcons.Tasks, MiuixIcons.Settings)
    val dark = isSystemInDarkTheme()
    val navBg = if (dark) GlassTokens.SurfaceControlDark else GlassTokens.SurfaceControlLight   // v0.2 收令牌（值不变）
    val navBorder = if (dark) GlassTokens.SurfaceControlLineDark else GlassTokens.SurfaceControlLineLight // 折射描边
    val navOnBg = if (dark) Color(0x40FFFFFF) else Color(0x99FFFFFF)        // .nv.on 选中胶囊（在实底上再亮一档）
    val navOnFg = if (dark) Color(0xFFFFB37A) else Color(0xFFE04E10)        // .nv.on 橙字
    val navFg = if (dark) Color(0xFFC6CBD8) else Color(0xFF241F18)          // 未选：近黑/灰白
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(
                start = 13.dp, end = 13.dp,
                top = 6.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 10.dp
            )
    ) {
        // Box 包裹：玻璃底 + 顶内高光浮层 + 内容行（高光不占 flex 槽）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = ContinuousRoundedRectangle(26.dp),
                    ambientColor = Color(0x1E50287A),
                    spotColor = Color(0x5950287A)   // mockup 0 10px 24px -10px rgba(80,40,120,.4)
                )
                .clip(ContinuousRoundedRectangle(26.dp))
                .background(navBg)
                .border(1.dp, navBorder, ContinuousRoundedRectangle(26.dp))
        ) {
            // 顶内高光 1.5dp（mockup inset 0 1.5px 0 rgba(255,255,255,.95)）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .align(Alignment.TopStart)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(if (dark) GlassTokens.SurfaceControlHiDark else GlassTokens.SurfaceControlHiLight, Color.Transparent)
                        )
                    )
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            items.forEachIndexed { index, label ->
                val selectedNow = index == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ContinuousRoundedRectangle(999.dp))
                        .background(if (selectedNow) navOnBg else Color.Transparent)
                        .clickable { onSelected(index) }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = null,
                            tint = if (selectedNow) navOnFg else navFg.copy(alpha = 0.85f),
                            modifier = Modifier.size(DockIconSize)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            color = if (selectedNow) navOnFg else navFg,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            }
        }
    }
}

/** 主页批量：指定数量档的默认值与步进。 */
/** 「指定数量」档的初始条数（LATEST_N 档用）；「按勾选下载」档不读它。 */
private const val HOMEPAGE_DEFAULT_LIMIT = 100
/**
 * 主页「作品预览」条数（v3.1.7：60 → 6）。
 * 依据 mockup S2 的明确裁定：「预览只给 **6 张** —— 它是**确认信号**，不是挑选工具」；
 * 挑选已下沉到 S4 二级选片页。改回大数值会让主页首屏重新被网格撑长。
 */
private const val HOMEPAGE_GRID_PREVIEW = 6
/** 「指定数量」档步进值。 */
private const val HOMEPAGE_LIMIT_STEP = 10

// ══════════════════════════════════════════════════════════════════════════════
// 设计规范：按钮排版（唯一真源 = docs/design/首页下载-交互重构-mockup.html 的 .btn）
//
//   .btn{height:52px;border-radius:26px;font-size:14.5px;font-weight:650;letter-spacing:.2px}
//   .actionbar .btn{margin-left:auto;min-width:132px;padding:0 20px}
//   .actionbar .lbl{font-size:13.5px;font-weight:650;line-height:1.35}
//
// 此前主按钮的字号/字重/字距全靠 Miuix Button 默认值（body1 ≈ 17sp / Medium），
// 比规范大 2.5sp 且没有字距 —— 「解析作品」因此显得又大又糊；同一页面底部的
// 「确认下载」却是显式 14sp/Medium，比规范小。两个同级主按钮大小不一致，
// 所以看到「上面的大、下面的小」。统一收到这里，改一处即全局生效。
// ══════════════════════════════════════════════════════════════════════════════

// ★ v3.1.5：真源已抽到 ui/glass/GlassSpec.kt（TaskDetailActivity / OnboardingScreen 也要用，
//   私有在本文件里它们就用不到 —— 与 GlassAccent 抽公共是同一个原因）。
//   这里保留同名别名，既有的 BtnHeight/... 调用点无需改动。
/** .btn 字号 14.5px。 */
private val BtnFontSize = GlassSpec.BtnFontSize
/** .btn 字距 .2px。 */
private val BtnLetterSpacing = GlassSpec.BtnLetterSpacing
/** .btn 字重 650（Compose 无 650，取 SemiBold=600，Miuix 侧同档）。 */
private val BtnFontWeight = GlassSpec.BtnFontWeight
/** .btn 高度 52px。 */
private val BtnHeight = GlassSpec.BtnHeight
/** .btn 圆角 26px（= 高度一半，正胶囊）。 */
private val BtnCornerRadius = GlassSpec.BtnCornerRadius
/** .actionbar .btn min-width:132px —— 固定宽度而非 minWidth，避免换文案时按钮宽度跳动。 */
private val BtnBarWidth = GlassSpec.BtnBarWidth
/** .actionbar .btn padding:0 20px。 */
private val BtnBarPadH = GlassSpec.BtnBarPadH

// ── 底部 dock 几何（唯一真源 = docs/design/LiquidGlass-液态玻璃定稿总览-v3.html）────
//
//   .nav{position:absolute;left:13px;right:13px;bottom:10px;border-radius:26px;padding:6px 4px}
//   .nv{flex:1;font-size:11px;font-weight:600;padding:5px 0}
//   .fab{position:absolute;right:16px;bottom:66px;width:46px;height:46px}
//   （icon+label 版来自 docs/design/首页下载-交互重构-mockup.html）
//   .dock{height:64px}  .dock .tab{font-size:11px}
//   .dock .tab .g{display:block;font-size:17px;margin-bottom:2px}
//
// ★ 规范里 .nav 是**纯文字**（高 ≈35px，顶边在 bottom+45.2px），FAB 底在 66px
//   → **FAB 浮在 dock 上方 20.8px、不重叠**。本 App 的 dock 补了图标后高 ~63dp，
//   顶边抬到 bottom+73dp，原样沿用 bottom+66dp 的 FAB 就会被 dock 圆角切掉一块
//   （实测重叠 14px = 5.1dp）。所以 FAB / 剪贴板气泡的偏移必须由这里统一推导，
//   不能再各写各的常量 —— 改 dock 高度时它们自动跟着走。
// ──────────────────────────────────────────────────────────────────────────────

/** mockup .dock{height:64px} —— dock 胶囊的目标视觉高度。 */
private val DockCapsuleH = GlassSpec.DockCapsuleH
/** mockup .nav{bottom:10px} —— 胶囊底边距导航栏（手势条）顶边的内边距。 */
private val DockBottomInset = GlassSpec.DockBottomInset
/** dock 胶囊顶边相对导航栏顶边的偏移（FAB / 气泡的最低下界）。 */
private val DockTopOffset = GlassSpec.DockTopOffset
/** 底部导航项图标尺寸：Material bottom nav 标准 24dp（实测 mockup `.dock .tab .g` 渲染高正好 24px）。 */
private val DockIconSize = GlassSpec.DockIconSize
/**
 * ★v3.1.5：FAB 由 60dp 收到规范的 **46dp 圆形**。
 * LGv3 `.fab{width:46px;height:46px;border-radius:999px}` 且已实测确认该文件的 px 是**设备值**
 * （其 `.nv` 在 232px 画幅下仍是 11px，与 mockup 390px 画幅的 `.dock .tab` 同为 11px）。
 */
private val FabSize = GlassSpec.FabSize
/** FAB 底边偏移 = 让开 dock 顶边 + LGv3 实测的 17px 间隙（原来是拍脑袋的 12dp）。 */
private val FabBottomOffset = GlassSpec.FabBottomOffset
/** 剪贴板气泡底边偏移 = FAB 顶边 + 8dp 间隙。 */
private val ClipBubbleBottomOffset = GlassSpec.ClipBubbleBottomOffset

// ── 「橙 chip」色对 ────────────────────────────────────────────────────────────
// accentFg() / accentBg() 已抽到 ui/glass/GlassAccent.kt 共享（TaskDetailActivity /
// WebViewActivity 也需要，私有在本文件里它们就用不到 —— 那正是之前漏改的原因）。
// ★铁律：橙色文字/图标/浅底一律走那两个 helper，不要直接用 MiuixTheme.colorScheme.primary。
//   它是**半透明**的打底色（亮 .88 / 暗 .45，暗色合成后是深棕橙），当前景用暗色下只有 ~1.9:1。

/**
 * 主页批量：按范围档 + 两开关计算实际待下载作品集（UI 统计与确认共用，保证所见即所得）。
 *  - ONLY_NEW：恒为未下载集合（"仅新增"定义即未下载，不受 skipDownloaded 影响）；
 *  - ALL：skipDownloaded=true 时等同仅新增，false 时含已下载（全量重下）；
 *  - LATEST_N：从全量/新增里取最新 N 条；
 *  - SELECTED：网格手选集合（忽略 includeImages/skipDownloaded——用户勾了就下，所见即所得）；
 *  - includeImages=false 时剔除图文帖（只下视频）。
 */
private fun scopeHomepageItems(
    state: HomePreviewState.Ready,
    range: HomepageBatchStore.Range,
    latestN: Int,
    includeImages: Boolean,
    skipDownloaded: Boolean,
    selectedIds: Set<String> = emptySet()
): List<DouyinPostItem> {
    val base = when (range) {
        HomepageBatchStore.Range.ONLY_NEW -> state.newItems
        HomepageBatchStore.Range.ALL -> if (skipDownloaded) state.newItems else state.allItems
        HomepageBatchStore.Range.LATEST_N ->
            if (skipDownloaded) state.newItems.take(latestN) else state.allItems.take(latestN)
        HomepageBatchStore.Range.SELECTED ->
            if (selectedIds.isEmpty()) emptyList()
            else state.allItems.filter { it.id in selectedIds }
    }
    if (range == HomepageBatchStore.Range.SELECTED) return base
    return if (includeImages) base
    else base.filter { it.type == com.neoruaa.xhsdn.douyin.DouyinMediaType.VIDEO }
}

/** 主页批量 v2 预览状态（文件内私有，MainActivity 与 HomepagePage 共用）。 */
private sealed interface HomePreviewState {
    data object Idle : HomePreviewState
    data object Loading : HomePreviewState
    data class Ready(
        val secUid: String,
        val nickname: String,
        val homepageUrl: String,
        /** 本次拉取的全量作品（含已下载，供"全部作品"档与网格预览）。 */
        val allItems: List<DouyinPostItem>,
        /** 未下载过的新作品（= allItems − 已下载，默认"仅新增"档与统计条用）。 */
        val newItems: List<DouyinPostItem>,
        /** 该账号作品总数（来自 user/profile/other 的 aweme_count）。0 = 未知。 */
        val totalCount: Int = 0,
        val lastSync: Long
    ) : HomePreviewState
    data class Error(val message: String, val needLogin: Boolean) : HomePreviewState
}

/**
 * 主页下载页（v2 两段式）：粘贴链接 → 解析预览（作者/总数/新增）→ 选范围 → 确认批量下载。
 */
@Composable
private fun HomepagePage(
    preview: HomePreviewState,
    onPreview: (String) -> Unit,
    onConfirm: (HomepageBatchStore.Range, Int, Boolean, Boolean, Set<String>) -> Unit,
    onFallbackCrawl: (String) -> Unit = {},
    /** 入口来源（clipboard/taskCard/…），驱动来源条文案。 */
    entryFrom: String = "manual",
    /** 是否显示来源条 + 返回按钮。 */
    showSourceBar: Boolean = false,
    /** 来源条「返回」：还原到进入前的页签。 */
    onReturn: () -> Unit = {},
    /** 解析阶段（0 连接作者主页 / 1 解析页面环境 / 2 读取作品列表）。 */
    parseStage: Int = 0,
    /** 取消解析：立即回 Idle，保留已填链接。 */
    onCancelPreview: () -> Unit = {},
    /** 当前正在解析的链接（回填输入框）。 */
    activeLink: String = "",
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var link by remember { mutableStateOf("") }
    // 外部入口（剪贴板/任务详情/任务卡）触发的解析：把实际链接回填进输入框，
    // 否则来源条说了"从哪来"却没说"在解析谁"，用户无从核对。
    LaunchedEffect(activeLink) {
        if (activeLink.isNotBlank()) link = activeLink
    }
    val parsing = preview is HomePreviewState.Loading
    var selectedRange by remember { mutableStateOf(HomepageBatchStore.Range.ONLY_NEW) }
    var limitN by remember { mutableStateOf(HOMEPAGE_DEFAULT_LIMIT) }
    var includeImages by remember { mutableStateOf(true) }
    var skipDownloaded by remember { mutableStateOf(true) }
    // v3.1.7（对齐 mockup S3/S4）：调整面板改 bottom sheet、网格下沉到二级选片页
    var sheetOpen by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }

    // ── 提到分支外：常驻操作条需要读取这些值（原实现把主按钮放在滚动流末尾，要滑 5 次才找到）──
    val ready = preview as? HomePreviewState.Ready
    // 勾选集：默认勾选=全部新增；随作者变化重置
    val newIdSet = remember(ready?.secUid) {
        ready?.newItems?.map { it.id }?.toSet() ?: emptySet()
    }
    val selectedIds = remember(ready?.secUid) {
        mutableStateOf(ready?.newItems?.map { it.id }?.toSet() ?: emptySet())
    }
    // 本次实际将下载条数（操作条左侧与确认共用同一计算，保证所见即所得）
    val selCount = ready?.let {
        scopeHomepageItems(it, selectedRange, limitN, includeImages, skipDownloaded, selectedIds.value).size
    } ?: 0

    // dock 浮层避让：96dp + 导航栏；操作条再垫在 dock 之上
    val dockPad = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 常驻操作条实测高度 = 外 padding 8×2 + 内 padding 10×2 + 按钮 52 = 88dp（此前按钮 46dp 时是 82，
    // 预留只给了 76，靠渐隐托盘兜住）。按钮对齐规范升到 52 后同步把预留补足，避免最后一行被压住。
    val actionBarH = 88.dp

    Box(modifier = modifier.fillMaxSize()) {
    // ── S4 选片页（二级）：网格下沉到这里，主页首屏不再被几十条封面撑长 ──
    if (pickerOpen && ready != null) {
        HomepagePickerPage(
            items = ready.allItems,
            newIdSet = newIdSet,
            selectedIds = selectedIds.value,
            dockPad = dockPad,
            onToggle = { id, checked ->
                selectedIds.value =
                    if (checked) selectedIds.value + id else selectedIds.value - id
                // 手选即切「自选」档：回写 S2 的操作条，保证所见即所得
                selectedRange = HomepageBatchStore.Range.SELECTED
            },
            onSetSelection = { ids ->
                selectedIds.value = ids
                if (ids.isNotEmpty()) selectedRange = HomepageBatchStore.Range.SELECTED
            },
            onBack = { pickerOpen = false },
            onConfirm = {
                selectedRange = HomepageBatchStore.Range.SELECTED
                onConfirm(
                    HomepageBatchStore.Range.SELECTED, limitN,
                    includeImages, skipDownloaded, selectedIds.value
                )
            }
        )
    } else {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // ★底部让位必须钳在**视口**上：`Modifier.padding` 要写在 `.verticalScroll` **之前**。
            //   写在之后 → padding 变成「滚动内容末尾的一段空白」，只在滚到底才生效，
            //   滚动途中/短内容照样画进操作条与 dock 底下（同「列表底部让位铁律」的老坑）。
            .padding(bottom = dockPad + actionBarH)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp)
    ) {
        // ── 来源条：仅"被打断"型入口显示（manual/recent 两个原地入口不显示）──
        if (showSourceBar) {
            Spacer(modifier = Modifier.height(8.dp))
            HomepageSourceBar(entryFrom = entryFrom, onReturn = onReturn)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = link,
            onValueChange = { link = it },
            label = stringResource(R.string.homepage_input_hint),
            singleLine = true,
            // 解析中不禁用输入框：禁用会让回填的链接变灰不可读（用户反而核对不了）。
            // 解析已捕获入参，此处编辑不影响进行中的任务；重复触发由下方按钮禁用拦住。
            modifier = Modifier
                .fillMaxWidth()
                .clip(ContinuousRoundedRectangle(16.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            // 垂直居中：两钮等高（h52）时消除 1px 级高度差带来的顶边错位（真机像素实测 7px 偏移）
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val clip = (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                        .primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                    if (clip.isNotEmpty()) {
                        link = clip
                    } else {
                        Toast.makeText(ctx, ctx.getString(R.string.no_valid_link_found), Toast.LENGTH_SHORT).show()
                    }
                },
                // 解析中禁用：避免与进行中的解析抢占（复制粘贴会改变 link 语义）
                enabled = !parsing,
                // 显式 h52：Miuix Button 默认 50dp，比右侧玻璃托盘矮 2dp，两个胶囊底边不齐
                modifier = Modifier.weight(1f).height(BtnHeight),
                // v3.1：与右侧「解析作品」统一 h52 胶囊（此前用 Miuix 默认圆角矩形，
                // 与 36dp 的解析钮一大一小且底边不齐，真机截图实证）
                cornerRadius = BtnCornerRadius
            ) {
                // 排版对齐 .btn：14.5sp / 650 / .2px（此前吃 Miuix 默认 ≈17sp，比规范大 2.5sp）
                Text(
                    text = stringResource(R.string.homepage_paste_clipboard),
                    fontSize = BtnFontSize,
                    fontWeight = BtnFontWeight,
                    letterSpacing = BtnLetterSpacing
                )
            }
            // v3.1：h36 → h52 胶囊，与左侧粘贴钮等高对齐（全 App 主/次按钮统一 h52 标准）
            // 美工：解析中不再用「渐变 + alpha」降级 —— alpha 会把白色按钮字一起打掉，
            // 视觉上只剩一团淡粉，看不出是按钮。改为换成真正的禁用态实心胶囊 + 「正在解析…」文案。
            if (parsing) {
                Button(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.weight(1f).height(BtnHeight),
                    cornerRadius = BtnCornerRadius
                ) {
                    Text(
                        text = stringResource(R.string.home_preview_parsing_short),
                        fontSize = BtnFontSize,
                        fontWeight = BtnFontWeight,
                        letterSpacing = BtnLetterSpacing
                    )
                }
            } else {
                GlassPrimaryWrap(
                    modifier = Modifier.weight(1f).height(BtnHeight),
                    cornerRadius = BtnCornerRadius
                ) {
                    Button(
                        onClick = {
                            if (link.isNotBlank()) {
                                onPreview(link)
                            } else {
                                Toast.makeText(ctx, ctx.getString(R.string.please_enter_url), Toast.LENGTH_SHORT).show()
                            }
                        },
                        // ★ 必须显式填满托盘高度：托盘是 52dp 的 Box，内层 Button 不给高度时
                        // 按 Miuix 默认（≈41dp）量测，Box 默认 TopStart 对齐 → 文字整体上移 5.3dp
                        // （真机逐行墨迹实测：胶囊中心 537.5px，文字墨迹中心 523px）。
                        // 同排左侧按钮是垂直居中的，两者因此肉眼可见地不齐。
                        modifier = Modifier.fillMaxWidth().height(BtnHeight),
                        colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White),
                        // vertical 给 0：高度已由外层锁定，上下 padding 只会把文字挤偏
                        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_preview_parse),
                            color = Color.White,
                            fontSize = BtnFontSize,
                            fontWeight = BtnFontWeight,
                            letterSpacing = BtnLetterSpacing
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        when (val p = preview) {
            is HomePreviewState.Loading -> {
                // 三阶段真实进度（替代原「正在解析…」无信息量单行）：长任务必须让用户看到在动
                HomepageParseStages(activeStage = parseStage)
            }
            is HomePreviewState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = p.message,
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        // 风控/需登录：post API 被拦时给网页爬取降级出路（v1 老路径，需登录态）
                        if (p.needLogin && link.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "改用网页爬取方式（需在浏览器登录抖音）",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = accentFg(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onPreview(link)
                                        onFallbackCrawl(link)
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            is HomePreviewState.Ready -> {
                val p = preview as HomePreviewState.Ready
                val downloadedCount = (p.allItems.size - p.newItems.size).coerceAtLeast(0)
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 作者卡 + 三格统计
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.home_preview_author_fmt, p.nickname),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            // 匿名截断常驻提示：toast 会消失，这里保证用户不会误以为
                            // 「作者只有这十几个作品」。总数来自 profile 接口，取不到则降级文案。
                            val truncated = p.allItems.isNotEmpty() &&
                                (p.totalCount > p.allItems.size || (p.totalCount == 0 && p.allItems.size <= 21))
                            if (truncated) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (p.totalCount > p.allItems.size)
                                        stringResource(R.string.home_preview_truncated_hint, p.totalCount, p.allItems.size)
                                    else
                                        stringResource(R.string.home_preview_truncated_hint_unknown, p.allItems.size),
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                HomeStatCell(
                                    stringResource(R.string.home_stat_new), p.newItems.size,
                                    valueColor = accentFg(),
                                    modifier = Modifier.weight(1f)
                                )
                                HomeStatCell(
                                    stringResource(R.string.home_stat_downloaded), downloadedCount,
                                    modifier = Modifier.weight(1f)
                                )
                                HomeStatCell(
                                    stringResource(R.string.home_stat_total),
                                    // 有总数就显示账号真实总量（而非本次所取条数），
                                    // 与下方截断提示一致，避免「全部作品=21」的误导
                                    if (p.totalCount > 0) p.totalCount else p.allItems.size,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // ── 下载计划（只读摘要卡）── mockup S2：`.card.pad` + `.hdr` + 三行 `.row`
                    // 设计要点：它是**摘要**不是表单 —— 四档单选 + 两个开关整体收进 S3 sheet，
                    // 改设置走右上「调整」，主页主路径不再被表单撑长（v3.1.7）。
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 22.dp,
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // `.hdr{padding:12px 2px 4px}`：`.h` 13.5px/650、`.a` 12.5px/primaryText/600
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 2.dp, end = 2.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "下载计划",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "调整",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentFg(),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { sheetOpen = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            HomePlanRow(
                                label = "新增作品",
                                valueText = p.newItems.size.toString()
                            )
                            HomePlanRow(
                                label = "已下载将跳过",
                                valueText = downloadedCount.toString(),
                                mute = true
                            )
                            // `.row.tap` + `.row .go` → 进 S4 选片页；已手选时把条数挂在右侧
                            HomePlanRow(
                                label = "手动挑选作品",
                                goText = if (selectedRange == HomepageBatchStore.Range.SELECTED)
                                    selectedIds.value.size.toString() else null,
                                onClick = { pickerOpen = true }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    // ── 作品预览：`.sec` 标题 + 只给 6 张 ──
                    // 「预览是**确认信号**，不是挑选工具」：挑选下沉到 S4 二级页（v3.1.7）
                    HomepageSectionHeader(
                        title = "作品预览",
                        actionText = "查看全部 ${p.allItems.size} 条 ›",
                        onAction = { pickerOpen = true }
                    )
                    p.allItems.take(HOMEPAGE_GRID_PREVIEW).chunked(3).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { item ->
                                HomeThumbCell(
                                    item = item,
                                    selected = item.id in selectedIds.value,
                                    downloaded = item.id !in newIdSet,
                                    showCheck = false,
                                    onClick = { pickerOpen = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    // 主按钮已移入常驻操作条（不再放滚动流末尾——原实现要上滑 5 次才够得着）
                }
            }
            HomePreviewState.Idle -> {
                // 「本次会话已解析过」→ 回访是主页下载的主场景，最近作者给横滑条常驻（而非只留一位）
                val recents = remember { HomeRepo.recentAuthors(ctx, limit = 8) }
                // 最近一位：完整卡（昵称 + 已下载数 + 重新解析）
                val recent = recents.firstOrNull()
                if (recent != null) {
                    val (recentSecUid, rec) = recent
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                link = "https://www.douyin.com/user/$recentSecUid"
                                onPreview(link)
                            },
                        cornerRadius = 14.dp,
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accentBg(lightAlpha = 0.18f)),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text(
                                    text = (rec.nickname.ifBlank { ctx.getString(R.string.home_preview_default_author) })
                                        .take(1),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = accentFg()
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(
                                        R.string.home_preview_recent_hint,
                                        rec.nickname.ifBlank { ctx.getString(R.string.home_preview_default_author) }
                                    ),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.home_preview_recent_sub, rec.downloadedIds.size),
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                // 其余最近作者：横滑小胶囊（回访场景一眼可选，避免重新粘贴链接）
                if (recents.size > 1) {
                    Text(
                        text = stringResource(R.string.home_recent_authors_title),
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recents.drop(1).forEach { (secUid, rec2) ->
                            val name = rec2.nickname.ifBlank { ctx.getString(R.string.home_preview_default_author) }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    // ★surface 暗色是「白@14%」覆盖层，copy(alpha=0.7) 会变成白 70% → 白板
                                    .background(glassSolid(0.70f))
                                    .clickable {
                                        link = "https://www.douyin.com/user/$secUid"
                                        onPreview(link)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(accentBg(lightAlpha = 0.18f)),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Text(
                                        text = name.take(1),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = accentFg()
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = name,
                                    fontSize = 12.5.sp,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // ── 空态引导（美工）──
        // 原 Idle 态只有 3 行灰字（对比度不足）+ 中下部一大片留白，既不像"完成态"也没告诉
        // 用户下一步做什么。补一张三步「怎么用」卡，把留白变成有效信息。
        if (preview is HomePreviewState.Idle) {
            HomepageHowToCard()
            Spacer(modifier = Modifier.height(12.dp))
        }
        // 支持范围：折叠为一行（完整说明收进「怎么用」卡），并把 Color.Gray 换成
        // 主题次级字色 —— 灰字压在暖色壁纸上对比度只有 ~2.5:1，暗光下看不清
        Text(
            text = stringResource(R.string.homepage_supported_short),
            fontSize = 11.5.sp,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.48f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
        // ══ 常驻操作条（sticky）：位置稳定 > 省 72px 空间 ══
        // Parsing：可取消；Ready：唯一路径推进流程（按钮位置永不因滚动/状态变化而消失）
        // 外层"渐隐托盘"：从操作条顶边渐隐到屏幕底 —— 否则操作条与底部 dock 之间的缝隙
        // 会露出滚动内容（网格缩略图），视觉上像"条断成两截"
        if (preview is HomePreviewState.Loading || ready != null) {
            val scrim = if (isSystemInDarkTheme()) Color(0xF2151518) else Color(0xF2F7F0E7)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.26f to scrim.copy(alpha = 0.90f),
                            1f to scrim
                        )
                    )
                    .padding(bottom = dockPad)
            ) {
                when {
                    preview is HomePreviewState.Loading -> HomepageActionBar(
                        label = stringResource(R.string.home_preview_parsing_short),
                        primaryText = stringResource(R.string.cancel),
                        enabled = true,
                        onPrimary = { onCancelPreview() }
                    )
                    ready != null -> {
                        val isSelectedMode = selectedRange == HomepageBatchStore.Range.SELECTED
                        HomepageActionBar(
                            label = if (isSelectedMode)
                                stringResource(R.string.home_bar_selected_fmt, selCount)
                            else
                                stringResource(R.string.home_bar_plan_fmt, selCount),
                            primaryText = if (isSelectedMode)
                                stringResource(R.string.home_bar_download_selected)
                            else
                                stringResource(R.string.home_preview_confirm),
                            // 无可下内容时禁用但**保留**（避免按钮忽隐忽现导致误触）
                            enabled = selCount > 0,
                            onPrimary = {
                                onConfirm(selectedRange, limitN, includeImages, skipDownloaded, selectedIds.value)
                            }
                        )
                    }
                }
            }
        }
        // ── S3 调整面板（bottom sheet）：四档单选 + 两个开关从页面整体收进 sheet ──
        // 「改设置去 sheet」—— 页面里只留一张只读的「下载计划」摘要卡，主路径不再被表单撑长。
        if (sheetOpen && ready != null) {
            HomePlanSheet(
                selectedRange = selectedRange,
                onSelectRange = { selectedRange = it },
                limitN = limitN,
                onLimitN = { limitN = it },
                maxN = ready.allItems.size,
                newCount = ready.newItems.size,
                includeImages = includeImages,
                onIncludeImages = { includeImages = it },
                skipDownloaded = skipDownloaded,
                onSkipDownloaded = { skipDownloaded = it },
                dockPad = dockPad,
                onDismiss = { sheetOpen = false }
            )
        }
    }
    }
}

/**
 * 空态三步「怎么用」卡（美工）。
 * 原来的空态只有三行灰字，页面中下部一大片留白，既不好看也没回答"我该做什么"。
 * 这里把留白换成三步指引 + 支持范围，一次性把「入口在哪 / 怎么触发 / 结果在哪」讲清。
 */
@Composable
private fun HomepageHowToCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        // 与页内其它卡（作者卡 / 范围卡 / 开关卡）统一 14dp 圆角
        cornerRadius = 14.dp,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.home_howto_title),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            listOf(
                stringResource(R.string.home_howto_1),
                stringResource(R.string.home_howto_2),
                stringResource(R.string.home_howto_3)
            ).forEachIndexed { i, step ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(accentBg(lightAlpha = 0.16f)),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = (i + 1).toString(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentFg()
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = step,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                    )
                }
            }
            // 完整支持范围不在此重复：页面底部已有一行短版说明（原实现在两处都放 →
            // 空态下一长一短两段"支持：…"叠着出现，看起来像出错）
        }
    }
}

/**
 * 主页下载来源条：告知「这次解析是怎么被触发的」，并提供**一键返回**。
 * 识别会打断用户当前在做的事 —— 没有返回，被弹到首页下载后想回去要多点几次；
 * 有返回，成本降到 1 次点击。这也是「不弹确认条」方案成立的前提（用返回兜住 5% 误触）。
 */
@Composable
private fun HomepageSourceBar(entryFrom: String, onReturn: () -> Unit) {
    val ctx = LocalContext.current
    val text = when (entryFrom) {
        "clipboard" -> ctx.getString(R.string.home_src_clipboard)
        "bubble" -> ctx.getString(R.string.home_src_bubble)
        "taskCard" -> ctx.getString(R.string.home_src_task_card)
        "taskDetail" -> ctx.getString(R.string.home_src_task_detail)
        "shortlink" -> ctx.getString(R.string.home_src_shortlink)
        else -> ctx.getString(R.string.home_src_clipboard)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accentBg(lightAlpha = 0.10f))
            .padding(start = 12.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(
            imageVector = MiuixIcons.Link,
            contentDescription = null,
            tint = accentFg(),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.5.sp,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.home_src_return),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = accentFg(),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable { onReturn() }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/**
 * 常驻操作条（sticky）：左侧状态文案 + 右侧主按钮。
 * 位置**永不随滚动或状态变化消失** —— 原实现把「确认下载」放在滚动流末尾，
 * 实测要上滑 5 次才够得着，属阻断性问题（位置稳定 > 省 72px 空间）。
 */
@Composable
private fun HomepageActionBar(
    label: String,
    primaryText: String,
    enabled: Boolean,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(ContinuousRoundedRectangle(20.dp))
            // ★这里原来写的是 surface.copy(alpha = 0.94f)：亮色=纯白 94% 没问题，
            //   但暗色 surface 是「白@14%」覆盖层，copy(alpha) 是替换 →
            //   暗色下变成**白 94% 的白板**，条上状态文字(onSurface=#F5F6F8)只有 1.04:1，看不见。
            .background(glassSolid(0.94f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                // .actionbar .lbl{font-size:13.5px;font-weight:650}
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        // 禁用态不用 alpha 降级（会把白色按钮字一起打掉）→ 换成真正的禁用态胶囊，
        // 尺寸完全一致，保证"位置稳定 > 省空间"这一原则在禁用态也成立。
        // 排版同样对齐 .btn：14.5sp / 650 / .2px —— 此前 14sp/Medium 比同页的「解析作品」
        // 小一档，同一屏里上下两个主按钮大小不一致（用户直接指出的问题）。
        if (enabled) {
            GlassPrimaryWrap(
                modifier = Modifier
                    .height(BtnHeight)
                    .width(BtnBarWidth),
                cornerRadius = BtnCornerRadius
            ) {
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.fillMaxWidth().height(BtnHeight),
                    colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White),
                    insideMargin = PaddingValues(horizontal = BtnBarPadH, vertical = 0.dp)
                ) {
                    Text(
                        text = primaryText,
                        color = Color.White,
                        fontSize = BtnFontSize,
                        fontWeight = BtnFontWeight,
                        letterSpacing = BtnLetterSpacing,
                        maxLines = 1
                    )
                }
            }
        } else {
            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier
                    .height(BtnHeight)
                    .width(BtnBarWidth),
                cornerRadius = BtnCornerRadius
            ) {
                Text(
                    text = primaryText,
                    fontSize = BtnFontSize,
                    fontWeight = BtnFontWeight,
                    letterSpacing = BtnLetterSpacing,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 解析三阶段进度（真实回调驱动，非假动画）。
 * 长任务必须让用户看到「在动 + 动到哪一步」；配合操作条的「取消」，
 * 把原来 11~14 秒的黑盒等待变成可控操作。
 */
@Composable
private fun HomepageParseStages(activeStage: Int) {
    val stages = listOf(
        stringResource(R.string.home_stage_1),
        stringResource(R.string.home_stage_2),
        stringResource(R.string.home_stage_3)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            stages.forEachIndexed { idx, name ->
                val done = idx < activeStage
                val active = idx == activeStage
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // ── 阶段圆点：按 mockup .stage .dot 三态（此前两处都与规范不符）────────
                    //   .stage .dot.done{background:rgba(0,137,92,.18);color:var(--ok)}
                    //                                                          完成 = **绿色系**
                    //   .stage .dot.now{border:2px solid var(--primaryText);
                    //     border-top-color:transparent;animation:spin .8s linear infinite}
                    //                                                          进行中 = 橙描边 spinner
                    //   .stage .dot.wait{background:var(--track)}               等待 = 中性灰
                    // 旧实现是「完成 = 橙实底 + 白勾 / 进行中 = 橙@0.28 实心点」：
                    // 前者完成态用错品牌色（该绿），后者暗色合成后仅 ~1.5:1 几乎看不见。
                    val dark = isSystemInDarkTheme()
                    val okColor = if (dark) GlassTokens.SuccessGreenDark else GlassTokens.SuccessGreen
                    val ringColor = accentFg()
                    Box(
                        // .stage .dot{width:20px;height:20px;border-radius:50%}
                        modifier = Modifier.size(20.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        when {
                            done -> Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(okColor.copy(alpha = if (dark) 0.22f else 0.18f), CircleShape),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Ok,
                                    contentDescription = null,
                                    tint = okColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            active -> {
                                // .8s/圈匀速，等价 CSS spin .8s linear infinite
                                val spin = rememberInfiniteTransition(label = "stageSpin").animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 800, easing = LinearEasing)
                                    ),
                                    label = "stageSpinAngle"
                                )
                                Canvas(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(spin.value)
                                ) {
                                    drawArc(
                                        color = ringColor,
                                        startAngle = -90f,
                                        sweepAngle = 300f,  // 缺口 60°，等价 border-top-color:transparent
                                        useCenter = false,
                                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                            else -> Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.12f), CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = name,
                        // .stage .tx{font-size:13.5px} / .stage.now .tx{font-weight:650}
                        fontSize = 13.5.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            done -> MiuixTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            active -> MiuixTheme.colorScheme.onSurface
                            else -> MiuixTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                        }
                    )
                    if (active) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "…", fontSize = 13.5.sp, color = accentFg())
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_parse_hint),
                // .hint{font-size:11px}
                fontSize = 11.sp,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.50f)
            )
        }
    }
}

/** 主页预览统计格：label + 数值，valueColor 非空时数值用该色（高亮）。 */
@Composable
private fun HomeStatCell(label: String, value: Int, valueColor: Color? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            // ★同 4027：surface.copy(alpha=0.6f) 在暗色下是白 60% → 白格
            .background(glassSolid(0.60f))
            .padding(vertical = 8.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

/** 封面位图内存缓存（主页预览网格用，decoded 缩略位图；LruCache 容量=张数）。 */
private val homeCoverCache = android.util.LruCache<String, android.graphics.Bitmap>(120)

/** 远程封面加载：进组合才拉取，decode 时 inSampleSize 压尺寸；失败静默（格子有底色角标兜底）。 */
@Composable
private fun rememberHomeCover(url: String?): android.graphics.Bitmap? {
    if (url.isNullOrBlank()) return null
    var bmp by remember(url) { mutableStateOf(homeCoverCache.get(url)) }
    LaunchedEffect(url) {
        if (homeCoverCache.get(url) != null) {
            bmp = homeCoverCache.get(url)
            return@LaunchedEffect
        }
        val decoded = withContext(Dispatchers.IO) {
            runCatching {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 10000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
                conn.inputStream.use { input ->
                    val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                    android.graphics.BitmapFactory.decodeStream(input, null, opts)
                }
            }.getOrNull()
        }
        if (decoded != null) {
            homeCoverCache.put(url, decoded)
            bmp = decoded
        }
    }
    return bmp
}

/**
 * 缩略单元 —— **严格对齐 mockup `.th`**（v3.1.7：v3.1 的「86dp 固定高 + 文字角标」改成 4:5 比例 + 胶囊角标）。
 *
 * 规范原文（`docs/design/首页下载-交互重构-mockup.html`）：
 * ```
 * .th{border-radius:12px;aspect-ratio:4/5;position:relative;overflow:hidden;border:1px solid rgba(255,255,255,.35)}
 * body.dark .th{border-color:rgba(255,255,255,.14)}
 * .th .bd{left:5px;bottom:5px;font-size:9.5px;padding:2px 6px;border-radius:999px;background:rgba(0,0,0,.42);color:#fff;font-weight:600}
 * .th .ck{right:5px;top:5px;width:19px;height:19px;border-radius:50%;background:var(--grad);color:#fff;font-size:11px;font-weight:700}
 * .th .ck.off{background:rgba(0,0,0,.28);border:1.5px solid rgba(255,255,255,.9);color:transparent}
 * ```
 *
 * @param showCheck 主页「作品预览」给 false（预览只是**确认信号**，勾选在 S4 二级页）；
 *                  选片页给 true，点击即切换勾选。
 */
@Composable
private fun HomeThumbCell(
    item: com.neoruaa.xhsdn.douyin.DouyinPostItem,
    selected: Boolean,
    downloaded: Boolean,
    showCheck: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onToggle: (Boolean) -> Unit = {}
) {
    val dark = isSystemInDarkTheme()
    val isVideo = item.type == com.neoruaa.xhsdn.douyin.DouyinMediaType.VIDEO
    val cover = rememberHomeCover(item.coverUrl ?: item.imageUrls.firstOrNull())
    Box(
        modifier = modifier
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isVideo) Color(0xFFBFD0C6) else Color(0xFFE0A06B))
            // `.th` 描边：亮 rgba(255,255,255,.35) / 暗 rgba(255,255,255,.14)
            .border(
                1.dp,
                if (dark) Color(0x24FFFFFF) else Color(0x59FFFFFF),
                RoundedCornerShape(12.dp)
            )
            .clickable { if (showCheck) onToggle(!selected) else onClick() }
    ) {
        if (cover != null) {
            Image(
                bitmap = cover.asImageBitmap(),
                contentDescription = item.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // 压暗层：保证角标/勾选环压在封面上仍可读
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (selected) 0.22f else 0.10f))
            )
        }
        // `.th .bd` 类型角标（左下）
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isVideo) "▶ 视频" else "▣ 图集 ${item.imageUrls.size.coerceAtLeast(1)}",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        // 已下载标记（左上，规格同 `.dbtag`，字色改用完成态绿以区分语义）
        if (downloaded) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.50f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "已下",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9FE8C4)
                )
            }
        }
        // `.th .ck` 勾选圆（右上）：19dp；ON = 橙渐变 + 白勾，OFF = 半透黑 + 1.5dp 白描边
        if (showCheck) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(19.dp)
                    .clip(CircleShape)
                    .then(
                        if (selected) {
                            Modifier.background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(GlassTokens.GradientStart, GlassTokens.GradientEnd)
                                )
                            )
                        } else {
                            Modifier
                                .background(Color.Black.copy(alpha = 0.28f))
                                .border(1.5.dp, Color(0xE6FFFFFF), CircleShape)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Text("✓", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 「下载计划」行 —— mockup `.row`：
 * ```
 * .row{display:flex;align-items:center;gap:10px;padding:11px 2px;font-size:13.5px}
 * .row+.row{border-top:1px solid var(--track)}
 * .row .rv{margin-left:auto;font-weight:650}   .row .rv.mute{color:var(--text2);font-weight:600}
 * .row .go{margin-left:auto;color:var(--primaryText);font-weight:600;font-size:13px}
 * ```
 *
 * @param showDivider `.row+.row` 的顶部分隔线；首行给 false。
 * @param goText      右侧计数（「手动挑选作品」已手选时挂条数），有它才渲染 `›`。
 */
@Composable
private fun HomePlanRow(
    label: String,
    valueText: String? = null,
    mute: Boolean = false,
    goText: String? = null,
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val dark = isSystemInDarkTheme()
    val divider = if (dark) GlassTokens.TrackDark else GlassTokens.TrackLight
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(divider)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClick)
                    } else Modifier
                )
                .padding(horizontal = 2.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.5.sp,
                color = MiuixTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            if (goText != null) {
                Text(
                    text = goText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            val showChevron = goText != null || onClick != null
            if (valueText != null && !showChevron) {
                // `.rv`：非 mute 用主字色 650，mute 用次级色 600
                Text(
                    text = valueText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (mute) MiuixTheme.colorScheme.onSurfaceVariantSummary
                            else MiuixTheme.colorScheme.onSurface
                )
            } else if (showChevron) {
                Text(
                    text = "›",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentFg()
                )
            }
        }
    }
}

/**
 * 区块标题 —— mockup `.sec`：
 * `.sec{display:flex;align-items:baseline;gap:8px;margin:20px 2px 10px}`
 * `.sec .st{font-size:13.5px;font-weight:650}` / `.sec .sr{margin-left:auto;font-size:12px;color:var(--primaryText);font-weight:600}`
 */
@Composable
private fun HomepageSectionHeader(title: String, actionText: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = actionText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentFg(),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAction)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * S3 调整面板（bottom sheet）—— 严格对齐 mockup：
 * ```
 * .scrim{position:absolute;inset:0;background:rgba(16,12,24,.42);z-index:5}
 * .sheet{left:0;right:0;bottom:0;background:var(--cardHi);border-radius:30px 30px 0 0;
 *        border-top:1px solid var(--border);box-shadow:inset 0 1px 0 var(--hi);padding:10px 18px 26px}
 * .sheet .grab{width:38px;height:4px;border-radius:99px;background:var(--text3);margin:0 auto 14px}
 * .sheet .h{font-size:12px;font-weight:650;color:var(--text2);letter-spacing:.3px;margin:14px 2px 4px}
 * ```
 *
 * 内容顺序严格照 mockup：`下载范围`（仅新增·推荐 / 全部作品 / 最近 N 条）→ `选项`（两个开关）
 * → `.note` 显式声明「手动挑选时本面板不生效」→ `完成`。
 * 设计动机：把「策略」（四档）与「枚举」（手选）拆开 —— 手选是独立二级页（S4），不再与此处平铺混排。
 *
 * ⚠️ **两处必须偏离 mockup 的地方**（都是渲染能力差异，不是设计变更）：
 * 1. **底色必须实心**：mockup 用 `--cardHi`（白 62%/18%）+ `backdrop-filter:blur(34px)` 才成立；
 *    Compose 没有 backdrop-filter，直接铺半透令牌会让**下层页面文字整片穿透**叠在 sheet 上
 *    （实测：「下载计划 / 新增作品 18」与 sheet 的「下载范围 / 仅新增」完全重叠）。
 *    故改用 [glassSolid] 实心表面 —— 与铁律 1 同源问题。
 * 2. **底边要让开 dock**：mockup 里 `.sheet{z-index:6}` 盖在 `.dock` 之上，但本 App 的 dock 由
 *    MainActivity 画在页面内容**之上**，页内浮层盖不住它 → 必须像常驻操作条一样
 *    预留 `dockPad`，否则「完成」按钮被 dock 压住（实测首版就是被压住的）。
 */
@Composable
private fun HomePlanSheet(
    selectedRange: HomepageBatchStore.Range,
    onSelectRange: (HomepageBatchStore.Range) -> Unit,
    limitN: Int,
    onLimitN: (Int) -> Unit,
    maxN: Int,
    newCount: Int,
    includeImages: Boolean,
    onIncludeImages: (Boolean) -> Unit,
    skipDownloaded: Boolean,
    onSkipDownloaded: (Boolean) -> Unit,
    dockPad: Dp,
    onDismiss: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    // 实心表面（亮=纯白 97% / 暗=近黑 #151518@96%）：必须压住下层，见上方 ⚠️ 1
    val sheetBg = glassSolid(lightAlpha = 0.97f, darkAlpha = 0.96f)
    val tertiary = if (dark) GlassTokens.TextTertiaryDark else GlassTokens.TextTertiaryLight
    val headColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
    // 物理返回键先关面板，而不是退出页面
    BackHandler { onDismiss() }
    Box(modifier = Modifier.fillMaxSize()) {
        // ── 遮罩（`.scrim`）：rgba(16,12,24,.42) + 点击关闭 ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x6B100C18))
                .clickable { onDismiss() }
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(sheetBg)
                .border(
                    1.dp,
                    if (dark) GlassTokens.BorderDark else GlassTokens.BorderLight,
                    RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                )
                // `.sheet{padding:10px 18px 26px}`；底部再让开 dock（见上方 ⚠️ 2）
                .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = dockPad + 14.dp)
        ) {
            // `.grab` 抓手条
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 14.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(tertiary)
            )
            HomeSheetHead(text = "下载范围", color = headColor)
            HomeRangeRow(
                label = stringResource(R.string.home_preview_range_only_new),
                countText = newCount.toString(),
                selected = selectedRange == HomepageBatchStore.Range.ONLY_NEW,
                pill = stringResource(R.string.home_preview_recommend)
            ) { onSelectRange(HomepageBatchStore.Range.ONLY_NEW) }
            HomeRangeRow(
                label = stringResource(R.string.home_preview_range_all),
                countText = maxN.toString(),
                selected = selectedRange == HomepageBatchStore.Range.ALL
            ) { onSelectRange(HomepageBatchStore.Range.ALL) }
            // `.step{margin-left:auto}` 内联在同一行（不再另起一行）
            val cappedN = limitN.coerceAtMost(maxN.coerceAtLeast(1))
            HomeRangeRow(
                label = stringResource(R.string.home_preview_range_latest),
                selected = selectedRange == HomepageBatchStore.Range.LATEST_N,
                trailing = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HomeStepButton("−") {
                            onSelectRange(HomepageBatchStore.Range.LATEST_N)
                            onLimitN((limitN - HOMEPAGE_LIMIT_STEP).coerceAtLeast(HOMEPAGE_LIMIT_STEP))
                        }
                        Text(
                            text = cappedN.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.width(34.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        HomeStepButton("+") {
                            onSelectRange(HomepageBatchStore.Range.LATEST_N)
                            onLimitN(
                                (limitN + HOMEPAGE_LIMIT_STEP)
                                    .coerceAtMost(maxN.coerceAtLeast(HOMEPAGE_LIMIT_STEP))
                            )
                        }
                    }
                }
            ) { onSelectRange(HomepageBatchStore.Range.LATEST_N) }
            HomeSheetHead(text = "选项", color = headColor)
            HomeSwitchRow(
                stringResource(R.string.home_opt_include_images), includeImages,
                onChange = onIncludeImages
            )
            HomeSwitchRow(
                stringResource(R.string.home_opt_skip_downloaded), skipDownloaded,
                onChange = onSkipDownloaded
            )
            // `.note{font-size:11px;color:var(--text3);line-height:1.6;margin:14px 2px 12px}`
            // 把原来「改了没反应」的失效组合改成显式声明（mockup S3 的第三条设计说明）
            Text(
                text = "ⓘ 手动挑选作品时，本面板的设置不生效",
                fontSize = 11.sp,
                color = tertiary,
                lineHeight = 17.6.sp,
                modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 14.dp, bottom = 12.dp)
            )
            // `.btn.primary` 完成 —— 52dp 胶囊，显式高度避免内层 Button 吃 Miuix 默认 ≈41dp
            GlassPrimaryWrap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BtnHeight),
                cornerRadius = BtnCornerRadius
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(BtnHeight),
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    insideMargin = PaddingValues(horizontal = BtnBarPadH, vertical = 0.dp)
                ) {
                    Text(
                        text = "完成",
                        color = if (dark) GlassTokens.OrangeOnDark else Color.White,
                        fontSize = BtnFontSize,
                        fontWeight = BtnFontWeight,
                        letterSpacing = BtnLetterSpacing
                    )
                }
            }
        }
    }
}

/** `.sheet .h` 小节标题：12sp / 650 / 次级字色 / 字距 .3px / margin 14px 2px 4px。 */
@Composable
private fun HomeSheetHead(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        letterSpacing = 0.3.sp,
        modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 14.dp, bottom = 4.dp)
    )
}

/**
 * S4 选片页（二级）—— 严格对齐 mockup：
 * ```
 * .topbar .back{font-size:17px;color:var(--text2)}  .topbar .r{margin-left:auto;font-size:13px;color:var(--primaryText);font-weight:600}
 * .seg{display:flex;gap:8px;margin:4px 0 12px}
 * .seg span{flex:1;text-align:center;padding:9px 0;border-radius:14px;font-size:12.5px;font-weight:600;background:var(--card);border:1px solid var(--border);color:var(--text2)}
 * .seg span.on{background:var(--fill);border-color:transparent;color:var(--primaryText)}
 * .filters{display:flex;gap:8px;margin-bottom:12px;align-items:center}
 * .filters .f{font-size:11.5px;padding:5px 11px;border-radius:999px;background:var(--card);border:1px solid var(--border);color:var(--text2)}
 * .filters .f.on{color:var(--primaryText);border-color:var(--primaryText)}
 * ```
 *
 * 设计动机（mockup S4 三条）：① 网格从主页**下沉**到二级页，主页首屏不再被几十条封面撑长；
 * ② 分段「全部 / 新增 / 已下载」+ 类型筛选 + 全选/反选；③ **返回不丢选择**，回写 S2 的操作条。
 * 选择态由父级 [selectedIds] 持有（`remember` 在 HomepagePage 里），所以进出页面天然保留。
 */
@Composable
private fun HomepagePickerPage(
    items: List<com.neoruaa.xhsdn.douyin.DouyinPostItem>,
    newIdSet: Set<String>,
    selectedIds: Set<String>,
    dockPad: Dp,
    onToggle: (String, Boolean) -> Unit,
    onSetSelection: (Set<String>) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    // 0=全部 / 1=新增 / 2=已下载（`.seg` 三段）
    var seg by remember { mutableStateOf(1) }
    // null=全部类型 / true=仅视频 / false=仅图集（`.filters`）
    var typeFilter by remember { mutableStateOf<Boolean?>(null) }
    val newItems = items.filter { it.id in newIdSet }
    val downloadedItems = items.filter { it.id !in newIdSet }
    val segItems = when (seg) {
        1 -> newItems
        2 -> downloadedItems
        else -> items
    }
    val shownItems = when (typeFilter) {
        true -> segItems.filter { it.type == com.neoruaa.xhsdn.douyin.DouyinMediaType.VIDEO }
        false -> segItems.filter { it.type != com.neoruaa.xhsdn.douyin.DouyinMediaType.VIDEO }
        else -> segItems
    }
    val videoCount = segItems.count { it.type == com.neoruaa.xhsdn.douyin.DouyinMediaType.VIDEO }
    val imageCount = segItems.size - videoCount
    val selectedCount = items.count { it.id in selectedIds }
    val unselectedCount = items.size - selectedCount
    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 顶栏：`.back` + 标题 + 右侧 `.r`（全选）──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    fontSize = 17.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "挑选作品",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "全选",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentFg(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSetSelection(shownItems.map { it.id }.toSet()) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            // ── `.seg` 分段：全部 / 新增 / 已下载 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PickerSeg("全部 ${items.size}", seg == 0, Modifier.weight(1f)) { seg = 0 }
                PickerSeg("新增 ${newItems.size}", seg == 1, Modifier.weight(1f)) { seg = 1 }
                PickerSeg("已下载 ${downloadedItems.size}", seg == 2, Modifier.weight(1f)) { seg = 2 }
            }
            // ── `.filters`：类型筛选 + 反选 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PickerFilter("视频 $videoCount", typeFilter == true) {
                    typeFilter = if (typeFilter == true) null else true
                }
                PickerFilter("图集 $imageCount", typeFilter == false) {
                    typeFilter = if (typeFilter == false) null else false
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "反选",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentFg(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            // 反选只作用于**当前筛选出的**这一批，符合「所见即所选」
                            val visibleIds = shownItems.map { it.id }.toSet()
                            val keep = selectedIds.filterNot { it in visibleIds }
                            onSetSelection(keep.toSet() + visibleIds.filterNot { it in selectedIds })
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            // ── 3 列网格（`.thumbs{grid-template-columns:repeat(3,1fr);gap:8px}`）──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // ★让位钳在视口（padding 在 `.verticalScroll` 之前）——
                    //   否则封面会画进操作条/ dock 底下（S2 同款坑，见那边的注释）
                    .padding(bottom = dockPad + 88.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                if (shownItems.isEmpty()) {
                    Text(
                        text = "这一档没有作品",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
                shownItems.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { item ->
                            HomeThumbCell(
                                item = item,
                                selected = item.id in selectedIds,
                                downloaded = item.id !in newIdSet,
                                showCheck = true,
                                onToggle = { checked -> onToggle(item.id, checked) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        // ── `.actionbar`：已选 N 个 / M 个未选 + 下载已选 ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = dockPad)
        ) {
            HomepageActionBar(
                label = "已选 $selectedCount 个" +
                    if (unselectedCount > 0) " · $unselectedCount 个未选" else "",
                primaryText = stringResource(R.string.home_bar_download_selected),
                enabled = selectedCount > 0,
                onPrimary = onConfirm
            )
        }
    }
}

/** `.seg span` 分段项：flex1 / 居中 / padding 9px 0 / 圆角 14 / 12.5sp / 600。 */
@Composable
private fun PickerSeg(text: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (on) Modifier.background(accentBg(lightAlpha = 0.16f))   // `.seg span.on{background:var(--fill)}`
                else Modifier.background(MiuixTheme.colorScheme.surfaceVariant)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (on) accentFg() else MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

/** `.filters .f` 类型筛选胶囊：11.5sp / padding 5px 11px / 圆角 999 / 选中转橙描边。 */
@Composable
private fun PickerFilter(text: String, on: Boolean, onClick: () -> Unit) {
    val accent = accentFg()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant)
            .border(1.dp, if (on) accent else Color.Transparent, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            color = if (on) accent else MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

/**
 * 主页预览范围行 —— **严格对齐 mockup `.opt`（S3 调整面板）**，v3.1.6 从「橙实底 + 白字」改成 radio 规范。
 *
 * 规范原文（`docs/design/首页下载-交互重构-mockup.html`）：
 * ```
 * .opt{display:flex;align-items:center;gap:11px;padding:12px 2px;font-size:14px}
 * .opt .rd{width:20px;height:20px;border-radius:50%;border:2px solid var(--text3);flex:none}
 * .opt.on .rd{border-color:var(--primaryText)}          .opt.on .rd::after{width:10px;height:10px;…primaryText}
 * .opt.on .lb{font-weight:650}
 * .opt .rv{margin-left:auto;font-size:13px;color:var(--text2)}
 * .opt .rec{font-size:10px;padding:2px 7px;border-radius:999px;background:var(--fill);color:var(--primaryText);font-weight:650;margin-left:7px}
 * ```
 * 注意两点易错处：① `.lb` **不是** flex:1 —— `推荐` pill 紧跟标签，只有 `.rv` / `.step` 走 `margin-left:auto`；
 * ② 选中态**不再有实底**，靠圆环描边 + 内点 + 标签加粗表达（旧实现用 `colorScheme.primary` 半透明打底色当实底，
 * 暗色下合成深棕，前景用白字对比度不合格）。
 *
 * @param trailing 尾部自定义控件（如「最近 N 条」的 `.step`）。给了就不渲染 [countText]。
 */
@Composable
private fun HomeRangeRow(
    label: String,
    countText: String? = null,
    pill: String? = null,
    selected: Boolean,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    // `.rd` 未选中描边 = --text3（亮 rgba(36,31,24,.38) / 暗 rgba(245,246,248,.36)）
    val ringIdle = if (dark) GlassTokens.TextTertiaryDark else GlassTokens.TextTertiaryLight
    val accent = accentFg()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // .rd：20dp 圆环 / 2dp 描边；选中 → 描边转橙 + 中心 10dp 实心点
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) accent else ringIdle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MiuixTheme.colorScheme.onSurface
        )
        if (pill != null) {
            Text(
                text = pill,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier
                    // `.rec{margin-left:7px}` —— 必须用 **外层 padding** 表达 margin：
                    // 若改用 `Spacer(Modifier.width(7.dp))`，`Arrangement.spacedBy(11.dp)` 会在它两侧各加一次
                    // 间隔 → 实际变成 11+7+11=29dp（v3.1.6 实测偏右 11.1dp 后改回）。
                    .padding(start = 7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accentBg(lightAlpha = 0.16f))   // --fill
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.weight(1f))   // .step{margin-left:auto}
            trailing()
        } else if (countText != null) {
            Spacer(modifier = Modifier.weight(1f))   // .rv{margin-left:auto}
            Text(
                text = countText,
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}

/** 主页预览步进小按钮（「最近 N 条」的 − / +）。mockup `.step b{width:32px;height:32px;border-radius:10px;font-size:16px;font-weight:500}` */
@Composable
private fun HomeStepButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val dark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            // `.step b{background:var(--track)}` —— 亮 rgba(36,31,24,.10) / 暗 rgba(255,255,255,.14)。
            // 原先写 onSurface.copy(alpha=.08f)，亮色下 0.08 比规范淡、暗色下 direction 也不对（onSurface 暗色是近白）
            .background(if (dark) GlassTokens.TrackDark else GlassTokens.TrackLight)
            .clickable(onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface
        )
    }
}

/**
 * 主页预览开关行 —— mockup 里开关行也是 `.opt`，但**没有 `.rd`**：
 * `<div class="opt"><span class="lb">包含图文作品</span><span class="sw"></span></div>`
 */
@Composable
private fun HomeSwitchRow(title: String, checked: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurface
        )
        HomeSpecSwitch(checked = checked, onChange = onChange)
    }
}

/**
 * mockup `.sw` 规格开关：`44×26` 胶囊 / 圆角 999 / ON = `--grad`（橙→珊瑚粉）/ OFF = `--track`；
 * 滑块 `20dp` 白圆，ON 靠右 3dp、OFF 靠左 3dp。
 *
 * ★不要直接用 Miuix `Switch` —— 它默认约 52×32，比规范宽 8dp、高 6dp，
 * 摆在 `.opt` 行尾会把 44dp 的行高顶起来（v3.1.7 换成本实现）。
 */
@Composable
private fun HomeSpecSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val dark = isSystemInDarkTheme()
    val track = if (dark) GlassTokens.TrackDark else GlassTokens.TrackLight
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(999.dp))
            .then(
                if (checked) {
                    Modifier.background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(GlassTokens.GradientStart, GlassTokens.GradientEnd)
                        )
                    )
                } else {
                    Modifier.background(track)
                }
            )
            .clickable { onChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
