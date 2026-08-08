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
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.neoruaa.xhsdn.taobao.TaobaoParser
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
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = !isNightMode

        val initialUrl = intent?.getStringExtra("url")
        val taskId = intent?.getLongExtra("task_id", -1L) ?: -1L
        val source = com.neoruaa.xhsdn.utils.Router.resolveWebViewSource(initialUrl, intent?.getStringExtra("source"))

        setContent {
            val controller = ThemeController(ColorSchemeMode.System)
            val localInitialUrl = initialUrl // Capture the variable in the composition scope
            val localSource = source
            val taskCreatedId = remember { mutableStateOf<Long?>(null) }
            val activity = this@WebViewActivity
            MiuixTheme(controller = controller) {
                WebViewScreen(
                    initialUrl = localInitialUrl,
                    source = localSource,
                    onBack = { finish() },
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
                            putExtra("source", localSource)
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
    onBack: () -> Unit,
    onResult: (List<String>, String, Long?, Boolean) -> Unit
) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf(TextFieldValue(initialUrl ?: "")) }
    var loading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }

    val topBarState = rememberTopAppBarState()
    val scrollBehavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior(state = topBarState)

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36"
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
        }
    }

    // Set to store sniffed video URLs
    val sniffedVideoUrls = remember { mutableSetOf<String>() }
    // Guard to ensure we only finish once
    val finished = remember { mutableStateOf(false) }

    // 淘宝登录提示状态：首次进入淘宝链接且未配置 cookie → 提示登录；提取失败（cookie 失效）→ 也提示
    val needLogin = remember { mutableStateOf(source == "taobao" && TaobaoParser.cookie.isBlank()) }
    val loginHint = remember {
        mutableStateOf(
            if (source == "taobao" && TaobaoParser.cookie.isBlank())
                "淘宝需要登录才能下载主图：请先在此页面登录淘宝账号，再点「保存 Cookie」并点「爬取」"
            else ""
        )
    }
    // 淘宝登录态是否已保存（绿色提示用）
    val savedOk = remember { mutableStateOf(false) }
    // 淘宝自检诊断信息（抠到 0 图时显示真实 DOM 统计，便于在真机验证选择器）
    val taobaoDiag = remember { mutableStateOf<String?>(null) }
    // 平台品牌色（淘宝橙 / 抖音青 / 小红书红）与标识，用于来源区分
    val sourceColor = when (source) {
        "taobao" -> Color(0xFFFF5000)
        "douyin" -> Color(0xFF25F4EE)
        else -> Color(0xFFFE2C55)
    }
    val sourceLabel = when (source) {
        "taobao" -> "淘宝"
        "douyin" -> "抖音"
        else -> "小红书"
    }

    DisposableEffect(webView) {
        onDispose { webView.destroy() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
        topBar = {
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
        }
    ) { padding ->
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
                // 平台标识：用品牌色字体区分淘宝/抖音/小红书
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
                            // 先回抓 WebView 当前登录态 cookie（若已登录则自动保存，实现登录一次后续全自动）
                            if (source == "taobao" && trySaveTaobaoCookie(context)) {
                                needLogin.value = false
                                loginHint.value = ""
                                savedOk.value = true
                            }
                            extractImages(context, webView, sniffedVideoUrls, source, finished, needLogin, loginHint, taobaoDiag, onResult)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !loading,
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(
                            text = stringResource(R.string.webview_crawl),
                            color = Color.White
                        )
                    }
                    // 抖音/淘宝：提供"直链解析(备用)"入口，WebView 取不到时强制走后台直链解析
                    if (source == "douyin" || source == "taobao") {
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
            }

            // 淘宝登录提示（橙色）：首次进入未配置 cookie 或 cookie 失效时提示登录；抖音/小红书不提示
            if (source == "taobao" && needLogin.value) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(sourceColor.copy(alpha = 0.12f), ContinuousRoundedRectangle(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = loginHint.value,
                        color = sourceColor
                    )
                }
            }

            // 淘宝：登录后手动保存 Cookie 按钮（显式控制，避免误存匿名态）
            if (source == "taobao") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val (ok, msg) = saveTaobaoCookieManually(context)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (ok) {
                                savedOk.value = true
                                needLogin.value = false
                                loginHint.value = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(text = "保存 Cookie", color = Color.White)
                    }
                }
            }

            // 淘宝：已保存登录态提示（绿色）
            if (source == "taobao" && savedOk.value) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color(0xFF1B8A3A).copy(alpha = 0.12f), ContinuousRoundedRectangle(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "已保存淘宝登录态，可点「爬取」下载主图",
                        color = Color(0xFF1B8A3A)
                    )
                }
            }

            // 淘宝：自检诊断（抠到 0 图时显示真实 DOM 统计，便于真机验证选择器是否命中）
            if (source == "taobao" && taobaoDiag.value != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color(0xFF8A6D1B).copy(alpha = 0.12f), ContinuousRoundedRectangle(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "自检诊断：当前页面抠到 0 张图。把下方信息发给开发者即可精修选择器（无需你懂代码）。",
                        color = Color(0xFF8A6D1B)
                    )
                    Text(
                        text = taobaoDiag.value ?: "",
                        color = Color(0xFF8A6D1B)
                    )
                    Button(
                        onClick = {
                            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("taobao_diag", taobaoDiag.value ?: ""))
                            Toast.makeText(context, "诊断已复制，发给开发者即可", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(text = "复制诊断信息", color = Color.White)
                    }
                }
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

    DisposableEffect(Unit) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                loading = true
                url?.let { urlText = TextFieldValue(it) }
                // Clear sniffed URLs on new page load
                sniffedVideoUrls.clear()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loading = false
                // 淘宝：页面加载完成且已跳离短链落地页后，尝试回抓登录态 cookie 自动保存
                // （登录完成后页面跳转/刷新会触发本回调，此时已带 cookie2/unb/_m_h5_tk）
                if (source == "taobao" && url != null && !url.contains("tb.cn/h")) {
                    if (trySaveTaobaoCookie(context)) {
                        needLogin.value = false
                        loginHint.value = ""
                        savedOk.value = true
                    }
                }
                // 抖音/淘宝：页面加载完成后自动尝试提取（优先 WebView），失败再走直链兜底
                if ((source == "douyin" || source == "taobao") && !finished.value) {
                    // 淘宝短链落地页（tb.cn/h）还没跳到真实商品页时不抠，等 onPageFinished 再次触发再抠
                    val isTaobaoLanding = source == "taobao" && url != null && url.contains("tb.cn/h")
                    if (!isTaobaoLanding) {
                        // SPA 二次水合/懒加载图片需更久，延时拉长到 3500ms；
                        // 若页面发生二次加载，onPageFinished 会再触发一次，自然形成一次重试。
                        view?.postDelayed({
                            if (!finished.value) {
                                extractImages(context, webView, sniffedVideoUrls, source, finished, needLogin, loginHint, taobaoDiag, onResult)
                            }
                        }, 3500)
                    }
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request?.url?.scheme == "http" || request?.url?.scheme == "https") {
                    view?.let { loadUrl(it, request.url.toString()) }
                } else {
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
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
                        it.contains(".mp4") && it.contains("douyin")
                    )
                    // 淘宝视频嗅探（PC/H5 详情页 CDN 特征）
                    val isTaobaoVideo = source == "taobao" && (
                        it.contains("cloud.video.taobao.com") ||
                        (it.contains("alicdn.com") && it.contains(".mp4")) ||
                        (it.contains("tbcache.com") && it.contains(".mp4")) ||
                        (it.contains("taobao.com") && it.contains(".mp4"))
                    )

                    if (isXhsVideo || isDouyinVideo || isTaobaoVideo) {
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
            // 淘宝：若用户在设置中填入了登录态 cookie，注入到 WebView，
            // 否则匿名 WebView 会撞登录墙（实测匿名 item.taobao.com 仅返回 5KB 登录页）。
            if (source == "taobao" && TaobaoParser.cookie.isNotBlank()) {
                try {
                    val cm = CookieManager.getInstance()
                    cm.setAcceptCookie(true)
                    cm.setCookie("https://item.taobao.com", TaobaoParser.cookie)
                    cm.setCookie("https://detail.tmall.com", TaobaoParser.cookie)
                    cm.setCookie(".taobao.com", TaobaoParser.cookie)
                    cm.flush()
                } catch (e: Exception) {
                    Log.e("WebViewActivity", "inject taobao cookie failed: ${e.message}")
                }
            }
            loadUrl(webView, initialUrl)
        } else {
            webView.loadUrl("about:blank")
        }
        onDispose { }
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
    source: String,
    finished: MutableState<Boolean>,
    needLogin: MutableState<Boolean>,
    loginHint: MutableState<String>,
    taobaoDiag: MutableState<String?>,
    onResult: (List<String>, String, Long?, Boolean) -> Unit
) {
    if (finished.value) return
    webView.postDelayed({
        val jsFileName = when (source) {
            "douyin" -> "douyin_extractor.js"
            "taobao" -> "taobao_extractor.js"
            else -> "xhs_extractor.js"
        }
        val jsCode = readAssetFile(context, jsFileName) ?: run {
            Toast.makeText(context, context.getString(R.string.no_urls_found_javascript_null), Toast.LENGTH_SHORT).show()
            return@postDelayed
        }
        webView.evaluateJavascript(jsCode) { result ->
            try {
                if (result == null || result == "null" || result.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.no_urls_found_javascript_null), Toast.LENGTH_SHORT).show()
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
                    if (url.startsWith("http") && !url.startsWith("blob:") && !url.startsWith("data:")) {
                        allUrls.add(url)
                    }
                }

                // Merge extracted URLs with sniffed URLs
                allUrls.addAll(sniffedUrls)

                if (allUrls.isNotEmpty()) {
                    // 淘宝提取成功 → 关闭登录提示与诊断（说明登录态有效且选择器命中）
                    if (source == "taobao") { needLogin.value = false; taobaoDiag.value = null }
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
                    onResult(allUrls, contentText, taskId, false)
                } else {
                    if (source == "douyin") {
                        // 抖音 WebView 取不到资源 → 交给 ViewModel 走直链兜底
                        finished.value = true
                        onResult(emptyList(), contentText, null, false)
                    } else {
                        // 淘宝提取为空（cookie 失效/未登录/选择器未命中）→ 提示重新登录 + 跑自检
                        if (source == "taobao") {
                            needLogin.value = true
                            loginHint.value = "登录态可能已失效，或页面 DOM 未被当前选择器命中。请重新登录淘宝后点「保存 Cookie」再点「爬取」；若仍为空，点下方「复制诊断」发开发者精修选择器。"
                            diagnoseTaobaoDom(webView) { taobaoDiag.value = it }
                        }
                        Toast.makeText(context, context.getString(R.string.no_accessible_urls_found), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (source == "douyin") {
                    finished.value = true
                    onResult(emptyList(), "", null, false)
                } else {
                    Toast.makeText(context, context.getString(R.string.error_parsing_urls, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }, 10)
}

/**
 * 判定 WebView 当前 cookie 是否代表“已登录淘宝”。
 * 关键：cookie2 才是淘宝账号登录态主 cookie；匿名/未登录会话虽可能带 _m_h5_tk / unb，
 * 但没有 cookie2，不能作为“已登录”标志（v1.0.21 修复：此前误把匿名 _m_h5_tk 当作登录态，
 * 导致“还没登录就说已保存”，且 HTTP 直解用匿名 cookie 撞登录墙）。
 */
internal fun isTaobaoLoggedIn(rawCookie: String): Boolean {
    // cookie2 后的取值必须非空（[^;]+）：淘宝在未登录态会把 cookie2 置空(cookie2=)，
    // 仅 contains("cookie2=") 会误判空值 cookie 为已登录。
    return Regex("cookie2=[^;]+").containsMatchIn(rawCookie)
}

/**
 * 淘宝页面加载完成后自动回抓登录态 cookie（仅当检测到 cookie2 才算登录）。
 * 匿名登录墙页面不会误写，避免 HTTP 直解撞墙。
 * @return 是否成功保存了有效登录态（含已是最新无需变更）
 */
private fun trySaveTaobaoCookie(context: android.content.Context): Boolean {
    return try {
        val cm = CookieManager.getInstance()
        val raw = cm.getCookie("https://item.taobao.com") ?: cm.getCookie(".taobao.com") ?: ""
        if (raw.isNullOrBlank()) return false
        // 仅当检测到真实登录态(cookie2)才视为已登录并保存
        if (!isTaobaoLoggedIn(raw)) return false
        val prefs = context.getSharedPreferences("XHSDownloaderPrefs", android.content.Context.MODE_PRIVATE)
        val existing = prefs.getString("taobao_cookie", "") ?: ""
        if (existing != raw) {
            prefs.edit().putString("taobao_cookie", raw).apply()
            TaobaoParser.cookie = raw
            Log.d("WebViewActivity", "saved taobao login cookie from webview (len=${raw.length})")
        }
        true
    } catch (e: Exception) {
        Log.e("WebViewActivity", "save taobao cookie failed: ${e.message}")
        false
    }
}

/**
 * 手动保存淘宝登录态 cookie（用户在 WebView 内登录后点「保存 Cookie」按钮触发）。
 * 显式反馈：未登录 / 已是最新 / 保存成功 / 失败。
 * @return (是否成功, 反馈文案)
 */
private fun saveTaobaoCookieManually(context: android.content.Context): Pair<Boolean, String> {
    return try {
        val cm = CookieManager.getInstance()
        val raw = cm.getCookie("https://item.taobao.com") ?: cm.getCookie(".taobao.com") ?: ""
        if (raw.isNullOrBlank()) return false to "未检测到任何淘宝 cookie"
        if (!isTaobaoLoggedIn(raw)) return false to "尚未登录：请先在页面内登录淘宝账号，再点「保存 Cookie」"
        val prefs = context.getSharedPreferences("XHSDownloaderPrefs", android.content.Context.MODE_PRIVATE)
        val existing = prefs.getString("taobao_cookie", "") ?: ""
        if (existing == raw) return true to "登录态已是最新，无需重复保存"
        prefs.edit().putString("taobao_cookie", raw).apply()
        TaobaoParser.cookie = raw
        Log.d("WebViewActivity", "manual saved taobao login cookie (len=${raw.length})")
        true to "已保存淘宝登录态，下次自动使用"
    } catch (e: Exception) {
        false to "保存失败：${e.message}"
    }
}

private fun readAssetFile(context: android.content.Context, fileName: String): String? {
    return try {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        null
    }
}

/**
 * 淘宝真机自检：在 WebView 渲染完成后统计真实 DOM，输出诊断 JSON。
 * 用于解决“选择器是否命中真实页面”这一无法在开发机验证的问题——
 * 用户登录后若仍抠不到图，把诊断发回即可精修选择器。
 */
private fun diagnoseTaobaoDom(webView: WebView, cb: (String) -> Unit) {
    val js = """
        (function() {
            try {
                var imgs = document.querySelectorAll('img');
                var alicdn = 0, firstSrc = '';
                for (var i = 0; i < imgs.length; i++) {
                    var s = imgs[i].src || imgs[i].getAttribute('src') ||
                            imgs[i].getAttribute('data-src') || imgs[i].getAttribute('data-original') ||
                            imgs[i].getAttribute('data-ks-lazyload') || '';
                    if (/alicdn\.com|taobao\.com|tmall\.com/.test(s)) { alicdn++; if (!firstSrc) firstSrc = s; }
                }
                var vids = document.querySelectorAll('video').length;
                var scripts = document.querySelectorAll('script');
                var stateKeys = [];
                for (var j = 0; j < scripts.length; j++) {
                    var t = scripts[j].textContent || '';
                    if (t.indexOf('auctionImages') >= 0 && stateKeys.indexOf('auctionImages') < 0) stateKeys.push('auctionImages');
                    if (t.indexOf('__INITIAL_STATE__') >= 0 && stateKeys.indexOf('__INITIAL_STATE__') < 0) stateKeys.push('__INITIAL_STATE__');
                    if (t.indexOf('detailData') >= 0 && stateKeys.indexOf('detailData') < 0) stateKeys.push('detailData');
                    if (t.indexOf('picList') >= 0 && stateKeys.indexOf('picList') < 0) stateKeys.push('picList');
                }
                var diag = {
                    imgTotal: imgs.length,
                    alicdnImg: alicdn,
                    firstAlicdnSrc: firstSrc,
                    video: vids,
                    hasEmbeddedState: stateKeys.length > 0,
                    stateKeys: stateKeys,
                    title: (document.title || '').substring(0, 80),
                    url: location.href
                };
                return JSON.stringify(diag);
            } catch (e) {
                return JSON.stringify({ error: String(e) });
            }
        })();
    """.trimIndent()
    webView.evaluateJavascript(js) { raw ->
        var s = raw ?: "{}"
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
        }
        cb(s)
    }
}
