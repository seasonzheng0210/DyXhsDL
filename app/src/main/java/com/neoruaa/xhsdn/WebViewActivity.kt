package com.neoruaa.xhsdn

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.MutableState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import androidx.compose.ui.res.stringResource
import com.kyant.capsule.ContinuousRoundedRectangle

class WebViewActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        val direct = intent?.getBooleanExtra("direct", false) ?: false
        if (direct) {
            // 直达下载模式：透明主题，不向用户展示抖音/快手/小红书网页本身，
            // 仅显示一个「解析中」浮层；WebView 在后台（INVISIBLE）跑 a_bogus 签名与抓 token，成功后直接下载。
            setTheme(R.style.Theme_WebViewDirect)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(lightScrim = android.graphics.Color.TRANSPARENT, darkScrim = android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(lightScrim = android.graphics.Color.TRANSPARENT, darkScrim = android.graphics.Color.TRANSPARENT)
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (direct) {
            // 透明窗口：让后面的 MainActivity 透出来，用户看不到抖音/快手/小红书网页
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = !isNightMode

        val initialUrl = intent?.getStringExtra("url")
        val taskId = intent?.getLongExtra("task_id", -1L) ?: -1L
        val source = com.neoruaa.xhsdn.utils.Router.resolveWebViewSource(initialUrl, intent?.getStringExtra("source"))
        // mode：extract=普通提取（默认）；homepage=主页爬取（自动滚动收集全部 /video/{id}）
        val mode = intent?.getStringExtra("mode") ?: "extract"

        setContent {
            val controller = ThemeController(ColorSchemeMode.System)
            val localInitialUrl = initialUrl // Capture the variable in the composition scope
            val localSource = source
            val localMode = mode
            val taskCreatedId = remember { mutableStateOf<Long?>(null) }
            val activity = this@WebViewActivity
            // 直达下载模式下，提取失败才降级为可见页（让用户登录/手动重试），否则全程不展示平台网页
            val fallback = remember { mutableStateOf(false) }
            MiuixTheme(controller = controller) {
                WebViewScreen(
                    initialUrl = localInitialUrl,
                    source = localSource,
                    mode = localMode,
                    direct = direct,
                    fallback = fallback,
                    onBack = { flushWebViewCookies(); finish() },
                    onResult = { urls, content, taskId, forceDirect ->
                        val resultIntent = Intent().apply {
                            putStringArrayListExtra("image_urls", ArrayList(urls))
                            if (content.isNotEmpty()) {
                                putExtra("content_text", content)
                            }
                            // Pass the URL that was crawled
                            putExtra("url", localInitialUrl ?: "")
                            // Pass the task ID if it was created
                            taskId?.let { id ->
                                putExtra("task_id", id)
                            }
                            putExtra("source", if (mode == "homepage") "douyin_home" else localSource)
                            putExtra("force_direct", forceDirect)
                        }
                        // Make sure setResult is called before finish
                        activity?.setResult(RESULT_OK, resultIntent)
                        activity?.finish()
                    }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewScreen(
    initialUrl: String?,
    source: String,
    mode: String = "extract",
    direct: Boolean = false,
    fallback: MutableState<Boolean> = remember { mutableStateOf(false) },
    onBack: () -> Unit,
    onResult: (List<String>, String, Long?, Boolean) -> Unit
) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf(TextFieldValue(initialUrl ?: "")) }
    var loading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    val statusMsg = remember { mutableStateOf("") }

    val topBarState = rememberTopAppBarState()
    val scrollBehavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior(state = topBarState)

    // Set to store sniffed video URLs
    val sniffedVideoUrls = remember { mutableSetOf<String>() }
    // Set to store video URLs captured by the injected XHR/fetch hook (web_inject.js)
    val capturedUrls = remember { mutableSetOf<String>() }
    // 主页爬取模式：收集到的全部视频页 URL（/video/{id}）
    val collectedVideoUrls = remember { mutableSetOf<String>() }
    // Guard to ensure we only finish once
    val finished = remember { mutableStateOf(false) }
    // 抖音/快手：提取失败时置位，用于展示「去登录 / 重试提取」引导（不再静默结束 Activity）
    val extractionFailed = remember { mutableStateOf(false) }
    // 失败时的页面 URL，重试时重新加载该视频页（复用已登录 Cookie）
    val retryUrl = remember { mutableStateOf<String?>(null) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            // 快手/抖音分享链接：强制桌面 UA。
            // - 快手：移动 UA 会被甩到 m.gifshow.com「在 App 打开」中转遮罩页（无任何视频数据）；
            //   桌面页 www.kuaishou.com/short-video/{id} 才含真实播放数据（window.__APOLLO_STATE__）。
            // - 抖音：移动 UA 打开 v.douyin.com 会被服务端拒绝——移动分享页渲染「请在抖音 App 内观看」
            //   错误页（不拉取任何视频数据，SSR 的 _ROUTER_DATA 也仅剩壳）；桌面 UA 下短链直接 302 到
            //   www.douyin.com/video/{id} 桌面播放页，视频正常加载，由 web_inject.js 的 XHR 钩子捕获播放地址。
            // 小红书保留移动 UA（其提取器依赖移动端 DOM 选择器 .note-image-box img 等）。
            val ua = if (source == "kuaishou" || source == "douyin") {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            } else {
                "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36"
            }
            settings.userAgentString = ua
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            // 允许混合内容（HTTP和HTTPS）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            // 允许明文内容
            settings.allowUniversalAccessFromFileURLs = true
            settings.allowFileAccessFromFileURLs = true
            setInitialScale(80)
            // 注册视频地址桥：web_inject.js 捕获到播放地址后回传，绕过 SPA 异步数据缺失
            addJavascriptInterface(object {
                @JavascriptInterface
                fun onVideoUrl(url: String) {
                    if (url.isNotBlank()) capturedUrls.add(url)
                }
            }, "AndroidVideoBridge")

            // 允许并持久化 Cookie（登录态）：抖音/快手在 WebView 内登录后，后续 XHR 会自动携带
            // 会话 Cookie，从而解除「未登录」门控拿到真实播放地址。Cookie 默认持久化到应用私有
            // 目录（app_webview/Cookies），跨启动保留，无需每次都登录。
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.setAcceptThirdPartyCookies(this, true)
            }
        }
    }

    // 直达下载模式且尚未降级：WebView 不绘制给用户看（INVISIBLE 仍参与布局、JS 正常执行，
    // 因此 a_bogus 读取的 window/document 尺寸依旧有效），仅由透明浮层提示「解析中」。
    val invisible = direct && !fallback.value
    LaunchedEffect(direct, fallback.value) {
        webView.visibility = if (direct && !fallback.value) View.INVISIBLE else View.VISIBLE
    }

    // 主页爬取模式标识（加载作者主页、自动滚动收集全部 /video/{id}）
    val isHomepage = mode == "homepage"

    // 平台品牌色（快手橙 / 抖音青 / 小红书红）与标识，用于来源区分
    val sourceColor = when (source) {
        "kuaishou" -> Color(0xFFFE5000)
        "douyin" -> Color(0xFF25F4EE)
        else -> Color(0xFFFE2C55)
    }
    val sourceLabel = when {
        isHomepage -> "抖音主页"
        source == "kuaishou" -> "快手"
        source == "douyin" -> "抖音"
        else -> "小红书"
    }

    // 抖音/快手：在 App 内 WebView 打开平台登录页（登录态由 CookieManager 自动持久化）
    val goLogin = {
        flushWebViewCookies()
        val loginUrl = if (source == "douyin") "https://www.douyin.com/" else "https://www.kuaishou.com/"
        statusMsg.value = "请在打开的${sourceLabel}页面完成登录，登录态会自动保存"
        webView.loadUrl(loginUrl)
    }
    // 提取失败后重试：清空状态并重新加载视频页，复用已登录 Cookie 重新提取
    val retryExtract = {
        if (extractionFailed.value) {
            extractionFailed.value = false
            finished.value = false
            capturedUrls.clear()
            sniffedVideoUrls.clear()
            statusMsg.value = "已重新加载，正在提取视频地址…"
            val target = retryUrl.value ?: urlText.text
            if (target.isNotBlank()) loadUrl(webView, target) else webView.reload()
        }
    }

    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }

    Scaffold(
        contentWindowInsets = if (invisible) WindowInsets(0, 0, 0, 0) else WindowInsets.statusBars.union(WindowInsets.displayCutout),
        topBar = if (invisible) ({
        }) else ({
            TopAppBar(
                title = stringResource(R.string.webview_title),
                navigationIcon = {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = "返回",
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable { onBack() }
                    )
                },
                scrollBehavior = scrollBehavior
            )
        })
    ) { padding ->
        if (invisible) {
            // 直达下载浮层：透明背景，仅展示「解析中」；WebView 在后台（INVISIBLE）跑 a_bogus 签名，
            // 成功后由 onResult 直接下载，用户全程不看到抖音/快手/小红书网页。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { webView.apply { layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT) } },
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier
                            .background(MiuixTheme.colorScheme.surface, ContinuousRoundedRectangle(16.dp))
                            .padding(horizontal = 22.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "正在解析视频，请稍候…",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        if (loading) {
                            LinearProgressIndicator(progress = progress / 100f, color = MiuixTheme.colorScheme.primary)
                        } else {
                            LinearProgressIndicator(color = MiuixTheme.colorScheme.primary)
                        }
                        Text(
                            text = if (loading) "页面加载中" else "正在提取视频地址…",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .background(MiuixTheme.colorScheme.surface)
                .padding(padding)
                .padding(bottom = max(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(), 16.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 平台标识：用品牌色字体区分快手/抖音/小红书
                Box(
                    modifier = Modifier
                        .background(sourceColor.copy(alpha = 0.12f), ContinuousRoundedRectangle(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = sourceLabel,
                        color = sourceColor
                    )
                }
                TextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    modifier = Modifier.fillMaxWidth().clip(ContinuousRoundedRectangle(16.dp)),
                    label = stringResource(R.string.webview_enter_url),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(imeAction = ImeAction.Go),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { loadUrl(webView, urlText.text) })
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { loadUrl(webView, urlText.text) },
                        modifier = Modifier.weight(1f),
                        enabled = urlText.text.isNotBlank(),
                    ) {
                        Text(
                            text = stringResource(R.string.webview_go)
                        )
                    }
                    Button(
                        onClick = {
                            if (isHomepage) {
                                if (!finished.value) {
                                    finishHomepageCrawl(context, webView, collectedVideoUrls, onResult, finished, statusMsg)
                                }
                            } else if (!finished.value) {
                                extractImages(context, webView, sniffedVideoUrls, capturedUrls, source, finished, onResult, 0, statusMsg, extractionFailed, retryUrl, direct, fallback)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !loading,
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(
                            text = if (isHomepage) "完成爬取" else stringResource(R.string.webview_crawl),
                            color = Color.White
                        )
                    }
                    // 抖音/快手：提供"直链解析(备用)"入口，WebView 取不到时强制走后台直链解析（主页爬取模式无此按钮）
                    if ((source == "douyin" || source == "kuaishou") && !isHomepage) {
                        Button(
                            onClick = {
                                if (!finished.value) {
                                    finished.value = true
                                    onResult(emptyList(), "", null, true)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !loading
                        ) {
                        Text(
                            text = "直链解析",
                            color = Color.White
                        )
                        }
                    }
                }

                // 抖音/快手：登录引导（非阻塞，提取期间也可提前登录；失败后出现「重试提取」）
                if (source == "douyin" || source == "kuaishou") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { goLogin() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "去登录${sourceLabel}", color = Color.White)
                        }
                        if (extractionFailed.value) {
                            Button(
                                onClick = { retryExtract() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColorsPrimary()
                            ) {
                                Text(text = "重试提取", color = Color.White)
                            }
                        }
                    }
                    if (extractionFailed.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(sourceColor.copy(alpha = 0.1f), ContinuousRoundedRectangle(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "提取失败：${sourceLabel}可能需要登录后才能获取播放地址。请点「去登录${sourceLabel}」在本窗口完成登录（登录态已自动保存），再点「重试提取」重新获取。",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (statusMsg.value.isNotEmpty()) {
                Text(
                    text = statusMsg.value,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .background(MiuixTheme.colorScheme.background, ContinuousRoundedRectangle(18.dp))
            ) {


                if (loading) {
                    LinearProgressIndicator(
                        progress = progress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = MiuixTheme.colorScheme.primary
                    )
                } else {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { webView.apply { layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT) } },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(ContinuousRoundedRectangle(18.dp)),

                        update = { }
                    )
                }
            }
        }
        }
    }

    DisposableEffect(Unit) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                loading = true
                url?.let { urlText = TextFieldValue(it) }
                // Clear sniffed URLs on new page load
                sniffedVideoUrls.clear()
                // 新页面加载即视为一次新尝试，先收起「提取失败」引导卡（失败会再次置位）
                extractionFailed.value = false
                // 注入 XHR/fetch 钩子（幂等），捕获 SPA 异步拉取的播放地址
                injectHook(webView, context)
                // 抖音：加载 a_bogus 签名算法，供直连 API 兜底（GitHub 双引擎的 API 引擎）
                if (source == "douyin") {
                    loadAbogus(webView, context)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loading = false
                // 关键修复：onPageStarted 里注入钩子并不可靠（文档可能未就绪，且 SPA 内部跳转会清掉注入）。
                // 这里在文档就绪后【重新注入】钩子（幂等 __dyInject 守卫），确保能捕获页面加载后才发出的
                // 异步 API 请求（抖音/快手视频数据由页面 JS 在 onPageFinished 之后才 XHR/fetch 拉取）。
                injectHook(webView, context)
                if (source == "douyin") loadAbogus(webView, context)
                // 抖音/快手：SPA 异步水合，视频数据由页面 JS 通过 XHR/fetch 拉取
                //（_ROUTER_DATA 现在只是 SSR 壳，videoInfoRes 不再内联）。单次延时不够，
                // 改为轮询：每 ~800ms 提取一次，直到拿到地址或超时，期间钩子/嗅探/视频元素扫描持续补充。
                // 抖音/快手：SPA 异步水合，视频数据由页面 JS 在 onPageFinished 后才 XHR/fetch 拉取。
                // 直达下载模式（direct）下，小红书也走自动提取，无需用户点「爬取」。
                val autoExtract = direct || source == "douyin" || source == "kuaishou"
                if (isHomepage && !finished.value) {
                    // 主页爬取模式：页面加载后自动滚动收集全部 /video/{id}
                    statusMsg.value = "页面已加载，正在爬取主页视频…"
                    view?.postDelayed({
                        if (!finished.value) {
                            crawlHomepage(context, webView, collectedVideoUrls, statusMsg, onResult, 0, finished)
                        }
                    }, 1500)
                } else if (autoExtract && !finished.value) {
                    statusMsg.value = "页面已加载，正在提取视频地址…"
                    view?.postDelayed({
                        if (!finished.value) {
                            extractImages(context, webView, sniffedVideoUrls, capturedUrls, source, finished, onResult, 0, statusMsg, extractionFailed, retryUrl, direct, fallback)
                        }
                    }, 1500)
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val u = request?.url?.toString() ?: return false
                // 抖音分享短链 302 到 iesdouyin.com/share/video/{id} 是「请在抖音极速版内观看」拒绝页，
                // 浏览器（含真机 WebView）拿不到视频数据。改写为真·桌面播放页 www.douyin.com/video/{id}，
                // 该页在真机/模拟器 WebView（真实浏览器指纹）下可正常加载并由 XHR 钩子捕获播放地址。
                val m = Regex("""iesdouyin\.com/share/video/(\d+)""").find(u)
                if (m != null) {
                    val id = m.groupValues[1]
                    view?.loadUrl("https://www.douyin.com/video/$id")
                    return true
                }
                // 其余导航（含快手短链 → www.kuaishou.com/short-video/{id}）让 WebView 原生处理。
                return false
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                url?.let {
                    // 小红书视频嗅探
                    val isXhsVideo = (it.contains("sns-video") && it.contains("xhscdn.com")) ||
                        it.endsWith(".mp4") ||
                        it.contains("masterUrl")
                    // 抖音视频嗅探（CDN 特征）
                    val isDouyinVideo = source == "douyin" && (
                        it.contains("douyinvod") ||
                        it.contains("aweme.snssdk.com") ||
                        it.contains("bytecdn") ||
                        it.contains("tiktokcdn") ||
                        it.contains("ib.douyin.com") ||
                        it.contains(".mp4")
                    )
                    // 快手视频嗅探（PC/H5 详情页 CDN 特征）；先排除封面/图集等图片资源，只认视频
                    val isKuaishouVideo = source == "kuaishou" && run {
                        val ku = it.lowercase()
                        val isKuaishouImage = ku.endsWith(".jpg") || ku.endsWith(".jpeg") || ku.endsWith(".png") ||
                            ku.endsWith(".webp") || ku.endsWith(".gif") ||
                            ku.contains(".jpg?") || ku.contains(".png?") || ku.contains(".webp?") ||
                            ku.contains("/img/") || ku.contains("cover") || ku.contains("water")
                        !isKuaishouImage && (
                            ku.contains("kwaicdn.com") || ku.contains("chenzhongtech.com") ||
                            ku.contains("gifshow.com") || ku.contains("kwai-player") ||
                            ku.contains("kwai") || ku.contains("kuaishou") ||
                            ku.contains(".mp4") || ku.contains(".m3u8")
                        )
                    }

                    if (isXhsVideo || isDouyinVideo || isKuaishouVideo) {
                        if (!sniffedVideoUrls.contains(it)) {
                            sniffedVideoUrls.add(it)
                            android.util.Log.d("WebViewActivity", "Sniffed video URL: $it")
                        }
                    }
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress = newProgress
            }
        }

        if (!initialUrl.isNullOrBlank()) {
            loadUrl(webView, initialUrl)
        } else {
            webView.loadUrl("about:blank")
        }
        onDispose { }
    }
}

private fun flushWebViewCookies() {
    // 立即将 WebView 内存中的 Cookie 落盘（登录态持久化，跨启动保留）
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().flush()
    }
}

private fun loadUrl(webView: WebView, raw: String) {
    var url = raw.trim()
    if (url.isEmpty()) return
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "https://$url"
    }
    webView.loadUrl(url)
}

private fun applyDefaultZoom(webView: WebView) {
    val targetScale = 0.8f
    webView.post {
        runCatching { webView.setInitialScale((targetScale * 100).toInt()) }
        // 通过放大 viewport 宽度来实现 50% 视觉缩放，同时保持内容铺满
        val js = """
            (function() {
                try {
                    var scale = $targetScale;
                    var width = Math.floor(window.innerWidth / scale);
                    var meta = document.querySelector('meta[name="viewport"]');
                    var content = 'width=' + width + ', initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no';
                    if (meta) {
                        meta.setAttribute('content', content);
                    } else {
                        meta = document.createElement('meta');
                        meta.name = 'viewport';
                        meta.content = content;
                        document.head.appendChild(meta);
                    }
                    // 清理之前的 transform/zoom 以防冲突
                    var reset = function(el) {
                        el.style.transform = '';
                        el.style.transformOrigin = '';
                        el.style.width = '';
                        el.style.height = '';
                        el.style.zoom = '';
                        el.style.margin = '';
                        el.style.padding = '';
                    };
                    reset(document.documentElement);
                    reset(document.body);
                } catch (e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}

private fun extractImages(
    context: android.content.Context,
    webView: WebView,
    sniffedUrls: Set<String>,
    capturedUrls: Set<String>,
    source: String,
    finished: MutableState<Boolean>,
    onResult: (List<String>, String, Long?, Boolean) -> Unit,
    attempt: Int,
    statusMsg: MutableState<String>,
    extractionFailed: MutableState<Boolean>,
    retryUrl: MutableState<String?>,
    direct: Boolean = false,
    fallback: MutableState<Boolean> = mutableStateOf(false)
) {
    if (finished.value) return
    webView.postDelayed({
        val jsFileName = when (source) {
            "douyin" -> "douyin_extractor.js"
            "kuaishou" -> "kuaishou_extractor.js"
            else -> "xhs_extractor.js"
        }
        val jsCode = readAssetFile(context, jsFileName) ?: run {
            Toast.makeText(context, context.getString(R.string.no_urls_found_javascript_null), Toast.LENGTH_SHORT).show()
            return@postDelayed
        }
        webView.evaluateJavascript(jsCode) { result ->
            try {
                if (result == null || result == "null" || result.isEmpty()) {
                    scheduleNextOrFallback(context, webView, sniffedUrls, capturedUrls, source, finished, onResult, attempt, statusMsg, extractionFailed, retryUrl, direct, fallback)
                    return@evaluateJavascript
                }
                var cleanResult = result
                if (cleanResult.startsWith("\"") && cleanResult.endsWith("\"")) {
                    cleanResult = cleanResult.substring(1, cleanResult.length - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t")
                }
                val json = org.json.JSONObject(cleanResult)
                val urlsArray = json.getJSONArray("urls")
                val contentObj = json.optJSONObject("content")
                val contentText = contentObj?.optString("content", "") ?: ""

                val allUrls = mutableListOf<String>()
                for (i in 0 until urlsArray.length()) {
                    val url = urlsArray.getString(i)
                    if (url.isNullOrEmpty()) continue
                    if (url.startsWith("http") && !url.startsWith("blob:") && !url.startsWith("data:") && !url.endsWith(".m3u8")) {
                        allUrls.add(url)
                    }
                }
                // 合并嗅探到的地址与 XHR/fetch 钩子捕获到的地址（跳过无法直下的 HLS）
                for (u in sniffedUrls) if (!u.endsWith(".m3u8")) allUrls.add(u)
                for (u in capturedUrls) if (!u.endsWith(".m3u8")) allUrls.add(u)

                if (allUrls.isNotEmpty()) {
                    // Create a task for the web crawl
                    val taskId = com.neoruaa.xhsdn.data.TaskManager.createTask(
                        noteUrl = webView.url ?: "",
                        noteTitle = webView.title ?: "",
                        noteType = com.neoruaa.xhsdn.data.NoteType.UNKNOWN,
                        totalFiles = allUrls.size,
                        noteContent = contentText
                    )

                    // Update the task status to DOWNLOADING immediately
                    com.neoruaa.xhsdn.data.TaskManager.updateTaskStatus(taskId, com.neoruaa.xhsdn.data.TaskStatus.DOWNLOADING)

                    finished.value = true
                    statusMsg.value = ""
                    onResult(allUrls, contentText, taskId, false)
                } else {
                    scheduleNextOrFallback(context, webView, sniffedUrls, capturedUrls, source, finished, onResult, attempt, statusMsg, extractionFailed, retryUrl, direct, fallback)
                }
            } catch (e: Exception) {
                scheduleNextOrFallback(context, webView, sniffedUrls, capturedUrls, source, finished, onResult, attempt, statusMsg, extractionFailed, retryUrl)
            }
        }
    }, if (attempt == 0) 10 else 0)
}

private fun scheduleNextOrFallback(
    context: android.content.Context,
    webView: WebView,
    sniffedUrls: Set<String>,
    capturedUrls: Set<String>,
    source: String,
    finished: MutableState<Boolean>,
    onResult: (List<String>, String, Long?, Boolean) -> Unit,
    attempt: Int,
    statusMsg: MutableState<String>,
    extractionFailed: MutableState<Boolean>,
    retryUrl: MutableState<String?>,
    direct: Boolean = false,
    fallback: MutableState<Boolean> = mutableStateOf(false)
) {
    // 轮询 ~2 分钟（150 × 800ms ≈ 120s）：GitHub 双引擎方案的浏览器引擎实践——
    // 抖音/快手桌面页在 Android WebView 上 SPA 水合很慢（模拟器实测抖音 ~85s 才出数据），
    // 20s 窗口必然提前回退到已被风控的 HTTP 直解。延长窗口让页面有足够时间加载并触发
    // web_inject.js 的 API 重拉/响应捕获。
    val MAX_ATTEMPTS = 150
    if (finished.value) return
    if (attempt + 1 < MAX_ATTEMPTS) {
        // 轮询下一次（~800ms），等待 SPA 异步数据 / 视频元素就绪
        statusMsg.value = "正在提取视频地址…（第 ${attempt + 1} 次轮询）"
        webView.postDelayed({
            if (!finished.value) {
                extractImages(context, webView, sniffedUrls, capturedUrls, source, finished, onResult, attempt + 1, statusMsg, extractionFailed, retryUrl, direct, fallback)
            }
        }, 800)
    } else {
        // 兜底：WebView 始终抓不到 → 先把页面诊断写入失败日志（含版本号，便于判断是否新包）。
        // 直达下载模式下，自动降级为「可见页」，把平台网页与登录/重试入口展示给用户（不再静默失败）；
        // 非直达模式则维持原行为（展示登录引导卡）。
        if (direct && !fallback.value) {
            fallback.value = true
            Toast.makeText(context, "自动解析未完成，已为你打开页面手动提取/登录", Toast.LENGTH_LONG).show()
        }
        if (source == "douyin" || source == "kuaishou") {
            val diagUrl = webView.url ?: ""
            val label = if (source == "douyin") "抖音" else if (source == "kuaishou") "快手" else "该平台"
            retryUrl.value = diagUrl
            extractionFailed.value = true
            statusMsg.value = "提取超时（钩子${capturedUrls.size}/嗅探${sniffedUrls.size}）。${label}可能要求登录，可在本窗口登录后点「重试提取」。"
            Toast.makeText(context, "提取超时，可登录${label}后重试", Toast.LENGTH_LONG).show()
            webView.evaluateJavascript(
                "(function(){try{var vs=document.querySelectorAll('video').length;var im=document.querySelectorAll('img').length;var t=(document.title||'').slice(0,80);var b=(document.body?(document.body.innerText||''):'').slice(0,120).replace(/\\n/g,' ');return JSON.stringify({videos:vs,imgs:im,title:t,body:b});}catch(e){return JSON.stringify({err:String(e)});}})()"
            ) { diag ->
                com.neoruaa.xhsdn.utils.DownloadLogger.logFailure(
                    context, source, diagUrl,
                    "WebView 提取超时 diag=$diag captured=${capturedUrls.size} sniffed=${sniffedUrls.size}（建议登录后重试）"
                )
            }
        } else {
            Toast.makeText(context, context.getString(R.string.no_accessible_urls_found), Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * 主页爬取模式：自动滚动作者主页，收集全部视频页链接（/video/{id}）。
 * 每次轮询：收集 a[href*="/video/"] → 滚动到底触发懒加载 → 直到轮询上限或连续多次无新增（视为到底）。
 * 收集到的链接写入 collectedUrls，最终由 finishHomepageCrawl 回传（source=douyin_home）。
 */
private fun crawlHomepage(
    context: android.content.Context,
    webView: WebView,
    collectedUrls: MutableSet<String>,
    statusMsg: MutableState<String>,
    onResult: (List<String>, String, Long?, Boolean) -> Unit,
    attempt: Int,
    finished: MutableState<Boolean>
) {
    if (finished.value) return
    webView.postDelayed({
        val js = """(function(){
          try {
            var nodes = document.querySelectorAll('a[href*="/video/"]');
            var arr = [];
            for (var i = 0; i < nodes.length; i++) {
              var h = nodes[i].getAttribute('href') || '';
              if (h && h.indexOf('/video/') >= 0) {
                if (h.indexOf('http') !== 0) { h = 'https://www.douyin.com' + h; }
                arr.push(h);
              }
            }
            // 触发无限滚动加载更多
            window.scrollTo(0, document.body.scrollHeight);
            return JSON.stringify(arr);
          } catch (e) { return JSON.stringify([]); }
        })();"""
        webView.evaluateJavascript(js) { result ->
            try {
                val raw = result?.trim() ?: "[]"
                val clean = if (raw.startsWith("\"") && raw.endsWith("\"")) {
                    raw.substring(1, raw.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
                } else raw
                val jsonArr = org.json.JSONArray(clean)
                val prevSize = collectedUrls.size
                for (i in 0 until jsonArr.length()) {
                    val u = jsonArr.optString(i)
                    if (u.contains("/video/")) collectedUrls.add(u)
                }
                val newCount = collectedUrls.size - prevSize
                statusMsg.value = "已收集 ${collectedUrls.size} 个视频${if (newCount > 0) "（本次 +$newCount）" else ""}，继续滚动加载…"
                val MAX_ATTEMPTS = 40
                if (attempt + 1 < MAX_ATTEMPTS && !(newCount == 0 && attempt > 4)) {
                    crawlHomepage(context, webView, collectedUrls, statusMsg, onResult, attempt + 1, finished)
                } else {
                    finishHomepageCrawl(context, webView, collectedUrls, onResult, finished, statusMsg)
                }
            } catch (e: Exception) {
                finishHomepageCrawl(context, webView, collectedUrls, onResult, finished, statusMsg)
            }
        }
    }, if (attempt == 0) 1500 else 1200)
}

/**
 * 结束主页爬取：回传已收集的全部视频页链接（source=douyin_home），或在无结果时提示。
 */
private fun finishHomepageCrawl(
    context: android.content.Context,
    webView: WebView,
    collectedUrls: MutableSet<String>,
    onResult: (List<String>, String, Long?, Boolean) -> Unit,
    finished: MutableState<Boolean>,
    statusMsg: MutableState<String>
) {
    if (finished.value) return
    finished.value = true
    if (collectedUrls.isNotEmpty()) {
        onResult(collectedUrls.toList(), "", null, false)
    } else {
        Toast.makeText(context, "未收集到主页视频，请确认已登录抖音或重试", Toast.LENGTH_LONG).show()
        statusMsg.value = "未收集到视频，请在登录后重试"
    }
}

private fun injectHook(webView: WebView, context: android.content.Context) {
    val js = readAssetFile(context, "web_inject.js")
    if (js != null) {
        webView.evaluateJavascript(js, null)
    }
}

private fun loadAbogus(webView: WebView, context: android.content.Context) {
    val js = readAssetFile(context, "abogus.js")
    if (js != null) {
        webView.evaluateJavascript(js, null)
    }
}

private fun readAssetFile(context: android.content.Context, fileName: String): String? {
    return try {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        null
    }
}

