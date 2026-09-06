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
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
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
import top.yukonga.miuix.kmp.basic.Switch
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.animate
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
import com.neoruaa.xhsdn.ui.glass.GlassTokens
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
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.window.WindowBottomSheet

// 缩略图内存缓存（最多缓存 50 张缩略图）
private val thumbnailCache = object : LruCache<String, ImageBitmap>(50) {}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val _autoDownloadIntentUrl = mutableStateOf<String?>(null)
    private var context: Context =  this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _autoDownloadIntentUrl.value = intent.getStringExtra("auto_download_url")
        intent.removeExtra("auto_download_url")

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
            // 底部主标签栏：0=视频下载 / 1=主页下载
            var mainTab by remember { mutableStateOf(0) }
            // 已处理过的剪贴板链接（去重用），相同链接不再重复读取/下载
            var lastHandledUrl by remember { mutableStateOf<String?>(null) }
            var manualInputLinks by remember { mutableStateOf(prefs.getBoolean("manual_input_links", false)) }
            var selectiveDownload by remember { mutableStateOf(prefs.getBoolean("selective_download", false)) }

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
                            launchDouyinHomepageDownload(clipText)
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

                MainScreen(
                    uiState = uiState,
                    manualInputLinks = manualInputLinks,
                    showInputDialog = showInputDialog,
                    onShowInputDialogChange = { showInputDialog = it },
                    scrollBehavior = scrollBehavior,
                    mainTab = mainTab,
                    onMainTabSelected = {
                        trackEvent("main_tab_switch", mapOf("tab" to if (it == 0) "video" else "homepage"))
                        mainTab = it
                    },
                    onHomepageDownload = { link -> fetchHomepagePreview(link) },
                    homePreview = homePreview,
                    onHomepageConfirm = { range, n, includeImages, skipDownloaded ->
                        (homePreview as? HomePreviewState.Ready)?.let {
                            confirmHomepageDownload(it, range, n, includeImages, skipDownloaded)
                        }
                    },
                    onDownload = {
                        if (!manualInputLinks) {
                            ensureStoragePermission {
                                // 先读取剪贴板
                                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                // 主页链接：走主页下载，反查作者并批量下载
                                if (UrlUtils.isDouyinHomepageLink(clipText)) {
                                    launchDouyinHomepageDownload(clipText)
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
                                    launchDouyinHomepageDownload(clipText)
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
                                launchDouyinHomepageDownload(task.noteUrl)
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
                            launchDouyinHomepageDownload(inputLink)
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
                                    launchDouyinHomepageDownload(link)
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
                    WindowDialog(
                        title = stringResource(R.string.retry_inconsistent_dialog_title),
                        summary = stringResource(
                            R.string.retry_inconsistent_dialog_message,
                            inconsistentRetry.previousCount,
                            inconsistentRetry.currentCount
                        ),
                        show = true,
                        onDismissRequest = { viewModel.dismissInconsistentRetryDialog() }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            TextButton(
                                text = stringResource(R.string.cancel),
                                onClick = { viewModel.dismissInconsistentRetryDialog() },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(12.dp))
                            TextButton(
                                text = stringResource(R.string.retry_inconsistent_save_button),
                                onClick = {
                                    viewModel.saveInconsistentRetryLogs(
                                        onResult = { showToast(it) },
                                        onError = { showToast(it) }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }
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
        // 无黑窗闪动。主页批量走 launchDouyinHomepageDownload，不受影响。
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
                startDouyinHomepageCrawl(homeUrl)
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
     * 主页下载 v2 入口：输入主页链接/视频链接 → post API 拉作品列表 → 预览卡（昵称/总数/新增）。
     * L1 直取 403 → BgWebViewParser 预热一次重试；仍风控 → 提示先登录。
     */
    private fun fetchHomepagePreview(input: String) {
        trackEvent("homepage_preview")
        val cleanUrl = UrlUtils.extractFirstUrl(input)
        if (cleanUrl == null) {
            showToast(getString(R.string.invalid_link_please_reenter))
            return
        }
        ensureStoragePermission {
            homePreview = HomePreviewState.Loading
            lifecycleScope.launch {
                try {
                    val secUid = withContext(Dispatchers.IO) {
                        // 先归一化短链：主页分享短链（v.douyin.com/x → share/user/{sec_uid}）须识别为主页
                        val finalUrl = runCatching { DouyinParser.resolveFinalUrl(cleanUrl) }.getOrDefault(cleanUrl)
                        val userFromFinal = Regex("""(?:iesdouyin|douyin)\.com/(?:share/)?user/([0-9A-Za-z_-]+)""")
                            .find(finalUrl)?.groupValues?.getOrNull(1)
                        userFromFinal ?: if (UrlUtils.isDouyinHomepageLink(input)) {
                            null
                        } else {
                            // 视频链接：反查作者主页（resolveAuthorHomepageUrl 内部会再跟跳一次，幂等）
                            DouyinParser.resolveAuthorHomepageUrl(cleanUrl)
                                ?.substringAfterLast('/')?.substringBefore('?')
                        }
                    }
                    if (secUid.isNullOrBlank()) {
                        homePreview = HomePreviewState.Error(getString(R.string.home_preview_bad_link), false)
                        return@launch
                    }
                    val homepageUrl = "https://www.douyin.com/user/$secUid"
                    val items = mutableListOf<DouyinPostItem>()
                    var nickname: String? = HomeRepo.nickname(this@MainActivity, secUid)
                    var cursor = 0L
                    var hasMore = true
                    var page = 0
                    while (hasMore && page < HOMEPAGE_MAX_PAGES) {
                        val pg = try {
                            withContext(Dispatchers.IO) { DouyinParser.fetchPostList(secUid, cursor) }
                        } catch (e: WebDetailBlockedException) {
                            // 风控：离屏 WebView 预热种 cookie 后重试一次。
                            // 预热页用轻量首页而非 /user/{secUid}——cookie 按域共享，任意
                            // www.douyin.com 页都能种下 UIFID 指纹；user 页过重，慢机（模拟器
                            // 软渲染）20s 内 onPageFinished 不触发即超时，预热等于空转（E2E 实证）。
                            BgWebViewParser(applicationContext).warmupAndSnapshot(HOMEPAGE_WARMUP_URL)
                            withContext(Dispatchers.IO) { DouyinParser.fetchPostList(secUid, cursor) }
                        }
                        items += pg.items
                        nickname = nickname ?: pg.nickname
                        cursor = pg.maxCursor
                        hasMore = pg.hasMore
                        page++
                    }
                    if (items.isEmpty()) {
                        homePreview = HomePreviewState.Error(getString(R.string.home_preview_empty), false)
                        return@launch
                    }
                    val lastSync = HomeRepo.lastSync(this@MainActivity, secUid)
                    val newItems = HomeRepo.filterNew(this@MainActivity, secUid, items)
                    // 记录最近作者，供「上次解析」快捷卡（nickname 为空时仍记录 secUid 级）
                    HomeRepo.touch(this@MainActivity, secUid, nickname.orEmpty())
                    homePreview = HomePreviewState.Ready(
                        secUid, nickname ?: getString(R.string.home_preview_default_author),
                        homepageUrl, items, newItems, lastSync
                    )
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

    /** 预览确认：按范围+开关选条 → HomepageBatchStore 暂存 → Service 批量下载。 */
    private fun confirmHomepageDownload(
        state: HomePreviewState.Ready,
        range: HomepageBatchStore.Range,
        latestN: Int,
        includeImages: Boolean,
        skipDownloaded: Boolean
    ) {
        val chosen = scopeHomepageItems(state, range, latestN, includeImages, skipDownloaded)
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
    }

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
        /** 预览拉取翻页上限（防异常账号无限翻页拖垮 Service）。 */
        private const val HOMEPAGE_MAX_PAGES = 20
        /** 主页批量 cookie 预热的轻量页：同域即种 UIFID 指纹（user 页过重，慢机 20s 预热会超时空转）。 */
        private const val HOMEPAGE_WARMUP_URL = "https://www.douyin.com/"
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
    homePreview: HomePreviewState = HomePreviewState.Idle,
    onHomepageConfirm: (HomepageBatchStore.Range, Int, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
    onClipboardBubbleActivate: () -> Unit = {},
    onDismissPrompt: () -> Unit,
    onCancelSelectiveDownload: () -> Unit,
    onSaveSelectedMedia: () -> Unit,
    onToggleSelectiveItem: (String) -> Unit
) {
    val topBarState = rememberTopAppBarState()
    val miuixScrollBehavior = MiuixScrollBehavior(state = topBarState)
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
                val title = stringResource(R.string.app_full_name)
                TopAppBar(
                    title = title,
                    largeTitle = title,
                    scrollBehavior = miuixScrollBehavior,
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 20.dp)
//                                .size(48.dp)
                                .clickable { menuExpanded = !menuExpanded },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = MiuixIcons.MoreCircle,
                                contentDescription = "更多",
//                                modifier = Modifier.size(24.dp)
                            )

                            val menuItems = listOf(stringResource(R.string.copy_description), stringResource(R.string.clear_history), stringResource(R.string.menu_failure_log), stringResource(R.string.menu_normal_log), stringResource(R.string.menu_package_log), stringResource(R.string.menu_events_log))

                            WindowListPopup(
                                show = menuExpanded && !uiState.isDownloading,
                                popupPositionProvider = rememberOffsetPopupPositionProvider(x = (-60).dp),
                                alignment = PopupPositionProvider.Align.TopEnd,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                ListPopupColumn {
                                    menuItems.forEachIndexed { index, item ->
                                        DropdownImpl(
                                            text = item,
                                            optionSize = menuItems.size,
                                            isSelected = false,
                                            onSelectedIndexChange = {
                                                menuExpanded = false
                                                when (index) {
                                                    0 -> {
                                                        onCopyText()
                                                    }
                                                    1 -> {
                                                        showClearHistoryDialog = true
                                                    }
                                                    2 -> {
                                                        EventTracker.track(ctx, "logs_view", mapOf("type" to "failure"))
                                                        showFailureLogDialog = true
                                                    }
                                                    3 -> {
                                                        EventTracker.track(ctx, "logs_view", mapOf("type" to "normal"))
                                                        showNormalLogDialog = true
                                                    }
                                                    4 -> {
                                                        EventTracker.track(ctx, "logs_view", mapOf("type" to "package"))
                                                        // 打包日志：压缩 正常日志 + 失败日志，并通过分享面板导出
                                                        val zip = DownloadLogger.packageLogs(ctx)
                                                        if (zip == null) {
                                                            android.widget.Toast.makeText(
                                                                ctx,
                                                                ctx.getString(R.string.package_log_failed),
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        } else {
                                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                                ctx,
                                                                "${ctx.packageName}.fileprovider",
                                                                zip
                                                            )
                                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                type = "application/zip"
                                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            ctx.startActivity(
                                                                android.content.Intent.createChooser(
                                                                    shareIntent,
                                                                    ctx.getString(R.string.package_log_share)
                                                                )
                                                            )
                                                            android.widget.Toast.makeText(
                                                                ctx,
                                                                ctx.getString(R.string.package_log_success, zip.absolutePath),
                                                                android.widget.Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                    5 -> {
                                                        EventTracker.track(ctx, "logs_view", mapOf("type" to "events"))
                                                        showEventsLogDialog = true
                                                    }
                                                }
                                            },
                                            index = index,
//                                            enabled = !uiState.isDownloading
                                        )
                                    }
                                }
                            }
                        }
                        // 清除历史记录确认对话框
                        if (showClearHistoryDialog) {
                            WindowDialog(
                                title = stringResource(R.string.clear_history_dialog_title),
                                summary = stringResource(R.string.clear_history_dialog_message),
                                show = true,
                                onDismissRequest = { showClearHistoryDialog = false }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    TextButton(
                                        text = stringResource(R.string.cancel),
                                        onClick = { showClearHistoryDialog = false },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    TextButton(
                                        text = stringResource(R.string.apply),
                                        onClick = {
                                            onClearHistory()
                                            showClearHistoryDialog = false
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.textButtonColorsPrimary()
                                    )
                                }
                            }
                        }

                        // 下载失败日志查看对话框（无需文件管理器即可查看）
                        if (showFailureLogDialog) {
                            val logCtx = LocalContext.current
                            var failureLogVersion by remember { mutableStateOf(0) }
                            val logContent = remember(failureLogVersion) { DownloadLogger.getLogContent(logCtx) }
                            WindowDialog(
                                title = stringResource(R.string.failure_log_title),
                                show = true,
                                onDismissRequest = { showFailureLogDialog = false }
                            ) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    if (logContent.isBlank()) {
                                        Text(
                                            text = stringResource(R.string.failure_log_empty),
                                            fontSize = 14.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    } else {
                                        Text(
                                            text = logContent,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 320.dp)
                                                .verticalScroll(rememberScrollState())
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(
                                            text = stringResource(R.string.clear_failure_log),
                                            onClick = {
                                                DownloadLogger.clearFailureLog(logCtx)
                                                failureLogVersion++
                                                android.widget.Toast.makeText(logCtx, logCtx.getString(R.string.failure_log_cleared), android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        TextButton(
                                            text = stringResource(R.string.cancel),
                                            onClick = { showFailureLogDialog = false }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        TextButton(
                                            text = stringResource(R.string.copy_log),
                                            onClick = {
                                                val cm = logCtx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                cm.setPrimaryClip(android.content.ClipData.newPlainText("failure_log", logContent))
                                                android.widget.Toast.makeText(logCtx, logCtx.getString(R.string.log_copied), android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 正常日志查看对话框（与失败日志对称，便于对照排查）
                        if (showNormalLogDialog) {
                            val logCtx = LocalContext.current
                            var normalLogVersion by remember { mutableStateOf(0) }
                            val logContent = remember(normalLogVersion) { DownloadLogger.getNormalLogContent(logCtx) }
                            WindowDialog(
                                title = stringResource(R.string.normal_log_title),
                                show = true,
                                onDismissRequest = { showNormalLogDialog = false }
                            ) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    if (logContent.isBlank()) {
                                        Text(
                                            text = stringResource(R.string.normal_log_empty),
                                            fontSize = 14.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    } else {
                                        Text(
                                            text = logContent,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 320.dp)
                                                .verticalScroll(rememberScrollState())
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(
                                            text = stringResource(R.string.clear_normal_log),
                                            onClick = {
                                                DownloadLogger.clearNormalLog(logCtx)
                                                normalLogVersion++
                                                android.widget.Toast.makeText(logCtx, logCtx.getString(R.string.normal_log_cleared), android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        TextButton(
                                            text = stringResource(R.string.cancel),
                                            onClick = { showNormalLogDialog = false }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        TextButton(
                                            text = stringResource(R.string.copy_log),
                                            onClick = {
                                                val cm = logCtx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                cm.setPrimaryClip(android.content.ClipData.newPlainText("normal_log", logContent))
                                                android.widget.Toast.makeText(logCtx, logCtx.getString(R.string.log_copied), android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 埋码（功能使用）日志查看对话框：复制给开发者分析哪些功能用得少
                        if (showEventsLogDialog) {
                            val evCtx = LocalContext.current
                            var eventsLogVersion by remember { mutableStateOf(0) }
                            val eventsContent = remember(eventsLogVersion) { EventTracker.getEventsLogContent(evCtx) }
                            WindowDialog(
                                title = stringResource(R.string.events_log_title),
                                show = true,
                                onDismissRequest = { showEventsLogDialog = false }
                            ) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Text(
                                        text = stringResource(R.string.events_log_desc),
                                        fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    if (eventsContent.isBlank()) {
                                        Text(
                                            text = stringResource(R.string.events_log_empty),
                                            fontSize = 14.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    } else {
                                        Text(
                                            text = eventsContent,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 320.dp)
                                                .verticalScroll(rememberScrollState())
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(
                                            text = stringResource(R.string.clear_events_log),
                                            onClick = {
                                                EventTracker.clear(evCtx)
                                                eventsLogVersion++
                                                android.widget.Toast.makeText(evCtx, evCtx.getString(R.string.events_log_cleared), android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        TextButton(
                                            text = stringResource(R.string.cancel),
                                            onClick = { showEventsLogDialog = false }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        TextButton(
                                            text = stringResource(R.string.copy_log),
                                            onClick = {
                                                val cm = evCtx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                cm.setPrimaryClip(android.content.ClipData.newPlainText("events_log", eventsContent))
                                                android.widget.Toast.makeText(evCtx, evCtx.getString(R.string.log_copied), android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
//                                .size(48.dp)
                                .clickable { onOpenSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Settings,
                                contentDescription = "设置",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (mainTab == 0) {
                        HistoryPage(
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
                            onDismissPrompt = onDismissPrompt,
                            modifier = Modifier.fillMaxSize(),
                            nestedScrollConnection = miuixScrollBehavior.nestedScrollConnection
                        )
                    } else {
                        HomepagePage(
                            preview = homePreview,
                            onPreview = onHomepageDownload,
                            onConfirm = onHomepageConfirm,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                // 底部主标签栏：视频下载 / 主页下载
                MainTabBar(mainTab, onMainTabSelected)
            }
        }

        SelectiveDownloadSheet(
            uiState = uiState,
            onCancel = onCancelSelectiveDownload,
            onSave = onSaveSelectedMedia,
            onToggleItem = onToggleSelectiveItem
        )
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
                    tint = if (canSave) MiuixTheme.colorScheme.primary else Color.Gray
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
    homePreview: HomePreviewState = HomePreviewState.Idle,
    onHomepageConfirm: (HomepageBatchStore.Range, Int, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
    onClipboardBubbleActivate: () -> Unit = {},
    onDismissPrompt: () -> Unit,
    modifier: Modifier = Modifier,
    nestedScrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection? = null
) {
    val tasks by com.neoruaa.xhsdn.data.TaskManager.getAllTasks().collectAsStateWithLifecycle(initialValue = emptyList())
    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val activeTask = tasks.firstOrNull {
        it.status == com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING || it.status == com.neoruaa.xhsdn.data.TaskStatus.QUEUED
    }

    var taskToDelete by remember { mutableStateOf<com.neoruaa.xhsdn.data.DownloadTask?>(null) }

    if (taskToDelete != null) {
        WindowDialog(
            title = stringResource(R.string.delete_task_dialog_title),
            summary = stringResource(R.string.delete_task_dialog_message),
            show = taskToDelete != null,
            onDismissRequest = { taskToDelete = null }
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = { taskToDelete = null },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    text = stringResource(R.string.apply),
                    onClick = {
                        taskToDelete?.let { onDeleteTask(it) }
                        taskToDelete = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }

    val dark = isSystemInDarkTheme()
    // 主按钮前景字：暗色启用态按令牌用浅橙字（#FFC9A3），亮色/禁用态保持白
    val primaryButtonFg = if (dark) GlassTokens.OrangeOnDark else Color.White
    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 18.dp,
            colors = CardDefaults.defaultColors(
                // 玻璃态：页面级 Chrome 面板（亮 70% 白 / 暗 12% 白，壁纸透出）
                color = if (dark) GlassTokens.ChromeDark else GlassTokens.ChromeLight
            )
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
                val configuration = LocalConfiguration.current
                TabRowWithContour(
                    tabs = filterLabels,
                    selectedTabIndex = selectedTab,
                    fontSize = 14.sp,
                    height = 40.dp,
                    colors = TabRowDefaults.tabRowColors(
                       selectedBackgroundColor = if (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                           Color(0xFF434343)
                       } else {
                           Color(0xFFFFFFFF)
                       }
                    ),
                    itemSpacing = 2.dp,
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
                    WindowDialog(
                        title = "清除已完成任务",
                        summary = "确定清除当前页签的 $completedInTab 条已成功下载的任务？\n（失败 / 下载中的任务会保留，可继续重试）",
                        show = true,
                        onDismissRequest = { showClearCompletedDialog = false }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            TextButton(
                                text = stringResource(R.string.cancel),
                                onClick = { showClearCompletedDialog = false },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(12.dp))
                            TextButton(
                                text = "清除",
                                onClick = {
                                    EventTracker.track(clearCtx, "clear_completed", mapOf("tab" to clearTabLabel, "n" to completedInTab.toString()))
                                    com.neoruaa.xhsdn.data.TaskManager.clearCompletedTasks(tabScope)
                                    showClearCompletedDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary()
                            )
                        }
                    }
                }
                // 「清除已完成」入口：当前页签有已完成任务时展示（页签栏下沿右对齐小胶囊）
                if (completedInTab > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier.clickable { showClearCompletedDialog = true },
                            cornerRadius = 14.dp,
                            colors = CardDefaults.defaultColors(
                                color = MiuixTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "清除已完成 $completedInTab",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    // 与下方任务列表之间留出呼吸间距，避免按钮紧贴卡片
                    Spacer(Modifier.height(6.dp))
                }
                if (filteredTasks.isEmpty()) {
                    // 空状态
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(ContinuousRoundedRectangle(18.dp))
                            .background(MiuixTheme.colorScheme.surfaceVariant)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Info,
                            contentDescription = stringResource(R.string.no_downloaded_files),
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_downloaded_files),
                            fontWeight = FontWeight.Medium,
                            fontSize = MiuixTheme.textStyles.headline1.fontSize,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.ready_to_download),
                            fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = Color.Gray
                        )
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
                            // 底部右下 FAB 高度 + navPadding + 余量，避免最后一项被 FAB 遮挡
                            bottom = navPadding + 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = if (nestedScrollConnection != null) {
                            Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)
                        } else {
                            Modifier.fillMaxSize()
                        }
                    ) {
                        itemsIndexed(filteredTasks, key = { _, task -> task.id }) { _, task ->
                            val context = LocalContext.current
                            TaskCell(
                                task = task,
                                // 只有正在下载的任务才使用 uiState.mediaItems
                                mediaItems = if (task.filePaths.isNotEmpty()) {
                                    task.filePaths.map { MediaItem(it, detectMediaType(it)) }
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
                                    val detailIntent = DetailActivity.newIntent(
                                        context,
                                        task.id.toString(),
                                        task.noteTitle ?: task.noteUrl,
                                        task.filePaths,
                                        task.noteContent,
                                        task.noteUrl  // Pass the note URL
                                    )
                                    context.startActivity(detailIntent)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 悬停在页面底部的下载按钮
        // 右下角紧凑 FAB：避免遮挡任务卡片列表（替代原先的底部宽栏）
        Card(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = navPadding + 12.dp)
                .clickable(enabled = !uiState.isDownloading) {
                    if (manualInputLinks) {
                        onShowInputDialogChange(true)
                    } else {
                        onDownload()
                    }
                },
            cornerRadius = 22.dp,
            colors = CardDefaults.defaultColors(
                color = if (uiState.isDownloading) MiuixTheme.colorScheme.disabledPrimaryButton else MiuixTheme.colorScheme.primary
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
//                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (manualInputLinks) MiuixIcons.Link else MiuixIcons.File,
                        contentDescription = stringResource(R.string.github_link),
                        modifier = Modifier.padding(end = 8.dp),
                        tint = primaryButtonFg
                    )
                    Text(
                        text = if (uiState.isDownloading) stringResource(R.string.downloading_files) else if (manualInputLinks) stringResource(R.string.manual_input_links) else stringResource(R.string.start_download_from_clipboard),
                        color = primaryButtonFg,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (uiState.isDownloading) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeTask?.noteTitle ?: activeTask?.noteUrl ?: " ",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 剪贴板检测提示气泡（叠加层，靠近底部按钮）
        if (detectedXhsLink != null && !uiState.isDownloading && !manualInputLinks) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = navPadding + 76.dp + 56.dp),
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
                    cornerRadius = 18.dp,
                    colors = CardDefaults.defaultColors(
                        color = Color(0xFFDDECDE)
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
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bubbleLabel,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = detectedXhsLink,
                                fontSize = 12.sp,
                                color = Color(0xB04CAF50),
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
                                color = Color(0xFF4CAF50)
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
                        color = Color(0xFFDDECDE)
                    )
                }
            }
        }

        // 输入分享链接对话框（放在 HistoryPage 内，避免键盘偏移异常）
        if (showInputDialog) {
            val context = LocalContext.current
            val manualInputTitle = stringResource(R.string.manual_input_links)
            val enterXhsUrl = stringResource(R.string.enter_xhs_url)
            val cancelText = stringResource(R.string.cancel)
            val downloadButtonText = stringResource(R.string.download_button)
            val pleaseEnterUrl = stringResource(R.string.please_enter_url)

            var inputLink by remember { mutableStateOf("") }

            WindowDialog(
                title = manualInputTitle,
                show = showInputDialog,
                summary = enterXhsUrl,
                onDismissRequest = {
                    onShowInputDialogChange(false)
                    inputLink = ""
                }
            ) {
                Column {
                    TextField(
                        value = inputLink,
                        onValueChange = { inputLink = it },
                        label = "http://xhslink.com/o/...",
                        useLabelAsPlaceholder = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ContinuousRoundedRectangle(16.dp)),
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        TextButton(
                            text = cancelText,
                            onClick = {
                                onShowInputDialogChange(false)
                                inputLink = ""
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            text = downloadButtonText,
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
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        }

    }
}

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
    val statusColor = when (task.status) {
        com.neoruaa.xhsdn.data.TaskStatus.QUEUED -> Color(0xFF9E9E9E)       // 灰色
        com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING -> Color(0xFF2196F3)  // 蓝色
        com.neoruaa.xhsdn.data.TaskStatus.COMPLETED -> Color(0xFF4CAF50)    // 绿色
        com.neoruaa.xhsdn.data.TaskStatus.FAILED -> Color(0xFFF44336)       // 红色
        com.neoruaa.xhsdn.data.TaskStatus.WAITING_FOR_USER -> Color(0xFFFF9800) // 橙色
    }
    
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
    val offsetX = remember { mutableStateOf(0f) }
    val swipeScope = rememberCoroutineScope()
    val REVEAL = 76f
    var showTaskMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(18.dp))
    ) {
        // 删除按钮（满铺红层，α=左滑进度；静止 α=0 → 不透过玻璃卡底色显形，根治珊瑚/粉穿帮）
        val deleteProgress = (-offsetX.value / REVEAL).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFFF44336).copy(alpha = deleteProgress))
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
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clip(ContinuousRoundedRectangle(18.dp))
                .background(MiuixTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
                .combinedClickable(
                    onClick = { onClick?.invoke() },
                    onLongClick = { showTaskMenu = true }
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX.value = (offsetX.value + dragAmount).coerceIn(-REVEAL, 0f)
                        },
                        onDragEnd = {
                            swipeScope.launch {
                                animate(
                                    initialValue = offsetX.value,
                                    targetValue = if (offsetX.value < -REVEAL / 2f) -REVEAL else 0f,
                                    animationSpec = tween(200)
                                ) { value, _ -> offsetX.value = value }
                            }
                        },
                        onDragCancel = {
                            swipeScope.launch {
                                animate(initialValue = offsetX.value, targetValue = 0f, animationSpec = tween(200)) { value, _ -> offsetX.value = value }
                            }
                        }
                    )
                }
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
        ) {
            // 顶部：时间 + 状态标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 创建时间
                Text(
                    text = formatTime(task.createdAt),
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 2.dp)
                )

                // 右侧：来源 + 状态标签
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 来源标识（快手/抖音/小红书）
                    val sourceLabel = when (task.source) {
                        "kuaishou" -> "快手"
                        "douyin" -> "抖音"
                        else -> "小红书"
                    }
                    val sourceColor = when (task.source) {
                        "kuaishou" -> Color(0xFFFE5000)
                        "douyin" -> Color(0xFF25F4EE)
                        else -> Color(0xFFFE2C55)
                    }
                    Box(
                        modifier = Modifier
                            .clip(ContinuousRoundedRectangle(999.dp))
                            .background(sourceColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sourceLabel,
                            fontSize = 11.sp,
                            color = sourceColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 状态标签
                    Box(
                        modifier = Modifier
                            .clip(ContinuousRoundedRectangle(999.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 标题（最多两行）
            Text(
                text = task.noteTitle ?: task.noteUrl,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 类型 + 文件数量
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.task_info_format, typeText, task.totalFiles),
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = Color.Gray
                )

                if (task.failedFiles > 0) {
                    Text(
                        text = stringResource(R.string.failed_files_format, task.failedFiles),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 进度条（仅下载中显示）
            if (task.totalFiles > 0 && task.status == com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING) {
                Column {
                    // 进度文本
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.files_completed_format, task.completedFiles, task.totalFiles),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = stringResource(R.string.progress_format, (task.progress * 100).toInt()),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // 进度条
                    LinearProgressIndicator(
                        progress = task.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(ContinuousRoundedRectangle(3.dp))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 媒体预览网格（最后一个任务显示）
            if (mediaItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    mediaItems.forEach { item ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(MiuixTheme.colorScheme.surface, shape = ContinuousRoundedRectangle(8.dp))
                                .clickable { onMediaClick(item) }
                        ) {
                            val bitmap = rememberThumbnail(item)
                            bitmap?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                        .clip(ContinuousRoundedRectangle(8.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        if (task.status != com.neoruaa.xhsdn.data.TaskStatus.COMPLETED) {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 操作按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isDownloading = task.status == com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING ||
                                task.status == com.neoruaa.xhsdn.data.TaskStatus.QUEUED

            if (isDownloading) {
                 Button(
                     onClick = onStop,
                     modifier = Modifier.weight(1f),
                     colors = ButtonDefaults.buttonColorsPrimary()
                 ) {
                     Text("停止", color = MiuixTheme.colorScheme.onPrimary)
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
                            Button(
                                onClick = onContinue,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColorsPrimary()
                            ) {
                                Text("坚持下载", color = MiuixTheme.colorScheme.onPrimary)
                            }
                            Button(
                                onClick = onWebCrawl,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    MiuixTheme.colorScheme.surface,
                                    MiuixTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(stringResource(R.string.web_crawl_option), color = MiuixTheme.colorScheme.onSurface)
                            }
                        }
                    }
                } else {
                    // 重试按钮（仅失败任务显示）
                    if (task.status == com.neoruaa.xhsdn.data.TaskStatus.FAILED) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Text(
                                text = stringResource(R.string.retry),
                                color = MiuixTheme.colorScheme.onPrimary
                            )
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
        WindowListPopup(
            show = showTaskMenu,
            popupPositionProvider = rememberOffsetPopupPositionProvider(x = 0.dp),
            alignment = PopupPositionProvider.Align.TopEnd,
            onDismissRequest = { showTaskMenu = false }
        ) {
            ListPopupColumn {
                menuActions.forEachIndexed { index, action ->
                    val text = when (action) {
                        "homepage" -> stringResource(R.string.homepage_download)
                        "copy" -> stringResource(R.string.copy_link)
                        else -> stringResource(R.string.delete_task_menu)
                    }
                    DropdownImpl(
                        text = text,
                        optionSize = menuActions.size,
                        isSelected = false,
                        onSelectedIndexChange = {
                            showTaskMenu = false
                            when (action) {
                                "homepage" -> onHomepageDownload()
                                "copy" -> onCopyUrl()
                                else -> onDelete()
                            }
                        },
                        index = index
                    )
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
    onSelected: (Int) -> Unit
) {
    val items = listOf(stringResource(R.string.main_tab_video), stringResource(R.string.homepage_download))
    val dark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 玻璃态：底栏 Chrome 玻璃（亮 70% 白 / 暗 12% 白）
            .background(if (dark) GlassTokens.ChromeDark else GlassTokens.ChromeLight)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, label ->
                val selectedNow = index == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelected(index) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selectedNow) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary,
                        fontWeight = if (selectedNow) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/** 主页批量：指定数量档的默认值与步进。 */
private const val HOMEPAGE_DEFAULT_LIMIT = 100
private const val HOMEPAGE_LIMIT_STEP = 10

/**
 * 主页批量：按范围档 + 两开关计算实际待下载作品集（UI 统计与确认共用，保证所见即所得）。
 *  - ONLY_NEW：恒为未下载集合（"仅新增"定义即未下载，不受 skipDownloaded 影响）；
 *  - ALL：skipDownloaded=true 时等同仅新增，false 时含已下载（全量重下）；
 *  - LATEST_N：从全量/新增里取最新 N 条；
 *  - includeImages=false 时剔除图文帖（只下视频）。
 */
private fun scopeHomepageItems(
    state: HomePreviewState.Ready,
    range: HomepageBatchStore.Range,
    latestN: Int,
    includeImages: Boolean,
    skipDownloaded: Boolean
): List<DouyinPostItem> {
    val base = when (range) {
        HomepageBatchStore.Range.ONLY_NEW -> state.newItems
        HomepageBatchStore.Range.ALL -> if (skipDownloaded) state.newItems else state.allItems
        HomepageBatchStore.Range.LATEST_N ->
            if (skipDownloaded) state.newItems.take(latestN) else state.allItems.take(latestN)
    }
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
    onConfirm: (HomepageBatchStore.Range, Int, Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    var link by remember { mutableStateOf("") }
    var selectedRange by remember { mutableStateOf(HomepageBatchStore.Range.ONLY_NEW) }
    var limitN by remember { mutableStateOf(HOMEPAGE_DEFAULT_LIMIT) }
    var includeImages by remember { mutableStateOf(true) }
    var skipDownloaded by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 56.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
    ) {
        Text(
            text = stringResource(R.string.homepage_download),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.homepage_desc),
            fontSize = 13.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = link,
            onValueChange = { link = it },
            label = stringResource(R.string.homepage_input_hint),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(ContinuousRoundedRectangle(16.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.homepage_paste_clipboard))
            }
            Button(
                onClick = {
                    if (link.isNotBlank()) {
                        onPreview(link)
                    } else {
                        Toast.makeText(ctx, ctx.getString(R.string.please_enter_url), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColorsPrimary()
            ) {
                Text(stringResource(R.string.home_preview_parse), color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        when (val p = preview) {
            is HomePreviewState.Loading -> {
                Text(
                    text = stringResource(R.string.home_preview_loading),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            is HomePreviewState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp,
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = p.message,
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            is HomePreviewState.Ready -> {
                val p = preview as HomePreviewState.Ready
                val downloadedCount = (p.allItems.size - p.newItems.size).coerceAtLeast(0)
                val selCount = scopeHomepageItems(p, selectedRange, limitN, includeImages, skipDownloaded).size
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
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                HomeStatCell(
                                    stringResource(R.string.home_stat_new), p.newItems.size,
                                    valueColor = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                HomeStatCell(
                                    stringResource(R.string.home_stat_downloaded), downloadedCount,
                                    modifier = Modifier.weight(1f)
                                )
                                HomeStatCell(
                                    stringResource(R.string.home_stat_total), p.allItems.size,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // 作品预览网格：最新 9 条，视频/图文以底色+角标区分
                    p.allItems.take(9).chunked(3).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowItems.forEach { item -> HomeGridCell(item, Modifier.weight(1f)) }
                            repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // 范围选择（列表单选）
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            HomeRangeRow(
                                label = stringResource(R.string.home_preview_range_all),
                                countText = p.allItems.size.toString(),
                                selected = selectedRange == HomepageBatchStore.Range.ALL
                            ) { selectedRange = HomepageBatchStore.Range.ALL }
                            HomeRangeRow(
                                label = stringResource(R.string.home_preview_range_only_new),
                                countText = p.newItems.size.toString(),
                                selected = selectedRange == HomepageBatchStore.Range.ONLY_NEW,
                                pill = stringResource(R.string.home_preview_recommend)
                            ) { selectedRange = HomepageBatchStore.Range.ONLY_NEW }
                            // 指定数量行：尾部 −/+ 步进（点击即选中该档）
                            val cappedN = limitN.coerceAtMost(p.allItems.size.coerceAtLeast(1))
                            HomeRangeRow(
                                label = stringResource(R.string.home_preview_range_latest),
                                selected = selectedRange == HomepageBatchStore.Range.LATEST_N,
                                countText = cappedN.toString()
                            ) { selectedRange = HomepageBatchStore.Range.LATEST_N }
                            // 步进控件（独立于选中态，点击先切档再步进）
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 6.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                HomeStepButton("−") {
                                    selectedRange = HomepageBatchStore.Range.LATEST_N
                                    limitN = (limitN - HOMEPAGE_LIMIT_STEP).coerceAtLeast(HOMEPAGE_LIMIT_STEP)
                                }
                                Text(
                                    text = cappedN.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                HomeStepButton("+") {
                                    selectedRange = HomepageBatchStore.Range.LATEST_N
                                    limitN = (limitN + HOMEPAGE_LIMIT_STEP)
                                        .coerceAtMost(p.allItems.size.coerceAtLeast(HOMEPAGE_LIMIT_STEP))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    // 开关组
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                            HomeSwitchRow(stringResource(R.string.home_opt_include_images), includeImages) {
                                includeImages = it
                            }
                            HomeSwitchRow(stringResource(R.string.home_opt_skip_downloaded), skipDownloaded) {
                                skipDownloaded = it
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.home_preview_estimate_fmt, selCount),
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onConfirm(selectedRange, limitN, includeImages, skipDownloaded) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(stringResource(R.string.home_preview_confirm), color = Color.White)
                    }
                }
            }
            HomePreviewState.Idle -> {
                // 「上次解析」快捷卡：最近一位作者，点击重新解析
                val recent = remember { HomeRepo.recentAuthor(ctx) }
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
                                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text(
                                    text = (rec.nickname.ifBlank { ctx.getString(R.string.home_preview_default_author) })
                                        .take(1),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MiuixTheme.colorScheme.primary
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
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.homepage_supported),
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

/** 主页预览统计格：label + 数值，valueColor 非空时数值用该色（高亮）。 */
@Composable
private fun HomeStatCell(label: String, value: Int, valueColor: Color? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.6f))
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

/** 主页预览网格单元：视频=灰绿底+播放三角，图文=焦糖底+2x2 缩略。 */
@Composable
private fun HomeGridCell(item: com.neoruaa.xhsdn.douyin.DouyinPostItem, modifier: Modifier = Modifier) {
    val isVideo = item.type == com.neoruaa.xhsdn.douyin.DouyinMediaType.VIDEO
    Box(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isVideo) Color(0xFFBFD0C6) else Color(0xFFE0A06B)),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = this.size.width
            val h = this.size.height
            if (isVideo) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.28f, h * 0.16f)
                    lineTo(w * 0.28f, h * 0.84f)
                    lineTo(w * 0.84f, h * 0.5f)
                    close()
                }
                drawPath(path, Color.White.copy(alpha = 0.95f))
            } else {
                val gap = w * 0.08f
                val side = (w - gap * 3) / 2
                for (r in 0..1) {
                    for (c in 0..1) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.9f),
                            topLeft = androidx.compose.ui.geometry.Offset(gap + c * (side + gap), gap + r * (side + gap)),
                            size = androidx.compose.ui.geometry.Size(side, side)
                        )
                    }
                }
            }
        }
    }
}

/** 主页预览范围行：整行可点，选中高亮为主色底白字；可选尾部 pill 与计数。 */
@Composable
private fun HomeRangeRow(
    label: String,
    countText: String? = null,
    pill: String? = null,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MiuixTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) Color.White else MiuixTheme.colorScheme.onSurface
        )
        if (pill != null) {
            Text(
                text = pill,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) Color.White else MiuixTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) Color.White.copy(alpha = 0.25f)
                        else MiuixTheme.colorScheme.primary.copy(alpha = 0.14f)
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (countText != null) {
            Text(
                text = countText,
                fontSize = 12.sp,
                color = if (selected) Color.White.copy(alpha = 0.9f)
                else MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}

/** 主页预览步进小按钮（指定数量 − / +）。 */
@Composable
private fun HomeStepButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(RoundedCornerShape(50))
            .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface
        )
    }
}

/** 主页预览开关行：Miuix Switch。 */
@Composable
private fun HomeSwitchRow(title: String, checked: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
