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

    // Set to store sniffed video URLs
    val sniffedVideoUrls = remember { mutableSetOf<String>() }
    // Set to store video URLs captured by the injected XHR/fetch hook (web_inject.js)
    val capturedUrls = remember { mutableSetOf<String>() }
    // Guard to ensure we only finish once
    val finished = remember { mutableStateOf(false) }

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
        }
    }

    // 平台品牌色（快手橙 / 抖音青 / 小红书红）与标识，用于来源区分
    val sourceColor = when (source) {
        "kuaishou" -> Color(0xFFFE5000)
        "douyin" -> Color(0xFF25F4EE)
        else -> Color(0xFFFE2C55)
    }
    val sourceLabel = when (source) {
        "kuaishou" -> "快手"
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
                            if (!finished.value) {
                                extractImages(context, webView, sniffedVideoUrls, capturedUrls, source, finished, onResult, 0)
                            }
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
                    // 抖音/快手：提供"直链解析(备用)"入口，WebView 取不到时强制走后台直链解析
                    if (source == "douyin" || source == "kuaishou") {
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
                // 注入 XHR/fetch 钩子（幂等），捕获 SPA 异步拉取的播放地址
                injectHook(webView, context)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loading = false
                // 抖音/快手：SPA 异步水合，视频数据由页面 JS 通过 XHR/fetch 拉取
                //（_ROUTER_DATA 现在只是 SSR 壳，videoInfoRes 不再内联）。单次延时不够，
                // 改为轮询：每 ~800ms 提取一次，直到拿到地址或超时，期间钩子/嗅探/视频元素扫描持续补充。
                if ((source == "douyin" || source == "kuaishou") && !finished.value) {
                    view?.postDelayed({
                        if (!finished.value) {
                            extractImages(context, webView, sniffedVideoUrls, capturedUrls, source, finished, onResult, 0)
                        }
                    }, 1500)
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
                        it.contains("tiktokcdn") ||
                        it.contains("ib.douyin.com") ||
                        it.contains(".mp4")
                    )
                    // 快手视频嗅探（PC/H5 详情页 CDN 特征）
                    val isKuaishouVideo = source == "kuaishou" && (
                        it.contains("kwaicdn.com") ||
                        it.contains("chenzhongtech.com") ||
                        it.contains("gifshow.com") ||
                        it.contains("kwai-player") ||
                        it.contains("kwai") ||
                        it.contains("kuaishou") ||
                        it.contains(".mp4")
                    )

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
    attempt: Int
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
                    scheduleNextOrFallback(context, webView, sniffedUrls, capturedUrls, source, finished, onResult, attempt)
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
                    onResult(allUrls, contentText, taskId, false)
                } else {
                    scheduleNextOrFallback(context, webView, sniffedUrls, capturedUrls, source, finished, onResult, attempt)
                }
            } catch (e: Exception) {
                scheduleNextOrFallback(context, webView, sniffedUrls, capturedUrls, source, finished, onResult, attempt)
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
    attempt: Int
) {
    // 轮询 ~2 分钟（150 × 800ms ≈ 120s）：GitHub 双引擎方案的浏览器引擎实践——
    // 抖音/快手桌面页在 Android WebView 上 SPA 水合很慢（模拟器实测抖音 ~85s 才出数据），
    // 20s 窗口必然提前回退到已被风控的 HTTP 直解。延长窗口让页面有足够时间加载并触发
    // web_inject.js 的 API 重拉/响应捕获。
    val MAX_ATTEMPTS = 150
    if (finished.value) return
    if (attempt + 1 < MAX_ATTEMPTS) {
        // 轮询下一次（~800ms），等待 SPA 异步数据 / 视频元素就绪
        webView.postDelayed({
            if (!finished.value) {
                extractImages(context, webView, sniffedUrls, capturedUrls, source, finished, onResult, attempt + 1)
            }
        }, 800)
    } else {
        // 兜底：WebView 始终抓不到 → 先把页面诊断写入失败日志（含版本号，便于判断是否新包），
        // 再回退直链解析（DownloadService 再回退 HTTP，最终可能失败）。
        if (source == "douyin" || source == "kuaishou") {
            val diagUrl = webView.url ?: ""
            webView.evaluateJavascript(
                "(function(){try{var vs=document.querySelectorAll('video').length;var im=document.querySelectorAll('img').length;var t=(document.title||'').slice(0,80);var b=(document.body?(document.body.innerText||''):'').slice(0,120).replace(/\\n/g,' ');return JSON.stringify({videos:vs,imgs:im,title:t,body:b});}catch(e){return JSON.stringify({err:String(e)});}})()"
            ) { diag ->
                com.neoruaa.xhsdn.utils.DownloadLogger.logFailure(
                    context, source, diagUrl,
                    "WebView 提取超时 diag=$diag captured=${capturedUrls.size} sniffed=${sniffedUrls.size}"
                )
                if (!finished.value) {
                    finished.value = true
                    onResult(emptyList(), "", null, false)
                }
            }
            Toast.makeText(context, context.getString(R.string.no_accessible_urls_found), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(R.string.no_accessible_urls_found), Toast.LENGTH_SHORT).show()
        }
    }
}

private fun injectHook(webView: WebView, context: android.content.Context) {
    val js = readAssetFile(context, "web_inject.js")
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

