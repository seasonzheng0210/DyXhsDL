package com.neoruaa.xhsdn.web

import androidx.annotation.RequiresApi
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.util.Collections
import kotlin.coroutines.resume

/**
 * 后台不可见 WebView 解析器（Service 内静默解析，不弹任何 Activity / 无黑窗）。
 *
 * 背景（v1.10.12）：抖音把纯 OkHttp HTTP 直解全站风控打挂（aweme/feed 等接口返回
 * HTTP 200 + 空 body「End of input」、分享页返回 argus-csp-token 安全页无 _ROUTER_DATA）；
 * 快手 GraphQL 同样对匿名 did 风控。v1.10.11 把 WebView 兜底整个移除后，视频/主页批量
 * 全部解析失败。而 v1.10.10 的「透明 Activity + INVISIBLE WebView」又引入黑窗闪动。
 *
 * 本类把解析下沉到前台服务内的后台 WebView：
 *  - WebView 在 Service 进程主线程创建（复用单实例），无窗口、无布局，用户全程无感；
 *  - 与 WebViewActivity 共用全局 CookieManager（app_webview 持久化登录态）与 assets 里的
 *    web_inject.js / douyin_extractor.js / kuaishou_extractor.js / abogus.js；
 *  - 桌面 UA 加载 www.douyin.com/video|note/{id} / 快手桌面作品页，由页面自带签名 API +
 *    XHR/fetch 钩子捕获真实播放地址（水印源 playwm→play 已由 JS 侧去除）；
 *  - 串行（Mutex）执行：同一时刻只有一个解析任务在用 WebView，调用方各自挂起等待；
 *  - 每个任务带总超时兜底，超时返回 null，不阻塞调用方。
 *
 * 结果契约与 WebViewActivity 的 extractImages 相同：
 *  videoUrls=视频直链（抖音/快手）；imageUrls=帖子图集图片（仅抖音图文/图集帖非空）。
 */
@SuppressLint("SetJavaScriptEnabled")
class BgWebViewParser(private val appContext: Context) {

    /** 解析结果。videoUrls/imageUrls 任一非空即视为成功。 */
    data class Result(
        val videoUrls: List<String> = emptyList(),
        val imageUrls: List<String> = emptyList(),
        val contentText: String = "",
        val title: String = "",
        val pageUrl: String = ""
    ) {
        val isNotEmpty: Boolean get() = videoUrls.isNotEmpty() || imageUrls.isNotEmpty()
    }

    /** 一次解析任务的会话状态。bridge 回传可能来自 WebView 内部线程，集合用同步包装。 */
    private class Session(val source: String) {
        val videoUrls: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
        val imageUrls: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
        @Volatile
        var done: Boolean = false
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutex = Mutex()

    /** 串行复用的单 WebView；只在主线程访问。 */
    private var webView: WebView? = null
    /** bridge 回调路由到的当前会话（主线程赋值/清空）。 */
    private var activeSession: Session? = null

    private val cookieManager: CookieManager by lazy {
        CookieManager.getInstance().apply { setAcceptCookie(true) }
    }

    private val androidVideoBridge = object {
        @JavascriptInterface
        fun onVideoUrl(url: String) {
            if (url.isNotBlank()) activeSession?.videoUrls?.add(url)
        }

        @JavascriptInterface
        fun onImageUrl(url: String) {
            if (url.isNotBlank()) activeSession?.imageUrls?.add(url)
        }
    }

    /**
     * 后台解析单个抖音/快手作品页。串行执行（同一 WebView 同时只跑一个任务），
     * 成功返回非空 [Result]，超时/页面无媒体返回 null。
     *
     * @param url    分享链接或作品页 URL（v.douyin.com 短链 / www.douyin.com/video|note/{id} /
     *               www.kuaishou.com 作品链接均可，内部按桌面页改写规则处理）
     * @param source "douyin" | "kuaishou"
     */
    suspend fun parse(url: String, source: String): Result? {
        if (source != "douyin" && source != "kuaishou") return null
        return mutex.withLock {
            suspendCancellableCoroutine { cont ->
                mainHandler.post {
                    val session = Session(source)
                    activeSession = session
                    try {
                        val wv = getOrCreateWebView()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            cookieManager.setAcceptThirdPartyCookies(wv, true)
                        }
                        // 若上一任务页面仍在加载/轮询，先停掉，避免旧页面回调污染新会话
                        wv.stopLoading()

                        val pageUrl = normalizeUrl(url, source)
                        attachClient(wv, session, pageUrl, source) { r ->
                            if (!session.done) {
                                session.done = true
                                activeSession = null
                                if (cont.isActive) cont.resume(r)
                            }
                        }
                        wv.loadUrl(pageUrl)

                        // 总超时兜底：页面水合慢 / 无媒体时在预算内结束，绝不无限等待
                        mainHandler.postDelayed({
                            if (!session.done) {
                                session.done = true
                                activeSession = null
                                Log.w(TAG, "后台解析超时(${TOTAL_TIMEOUT_MS}ms): url=$url")
                                if (cont.isActive) cont.resume(null)
                            }
                        }, TOTAL_TIMEOUT_MS)
                    } catch (e: Exception) {
                        Log.w(TAG, "后台解析异常: url=$url", e)
                        if (!session.done) {
                            session.done = true
                            activeSession = null
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
            }
        }
    }

    /** 销毁 WebView（Service onDestroy 时调用；必须在主线程执行）。 */
    fun destroy() {
        mainHandler.post {
            activeSession = null
            runCatching { webView?.stopLoading() }
            runCatching { webView?.destroy() }
            webView = null
        }
    }

    // region 主线程实现

    private fun getOrCreateWebView(): WebView {
        webView?.let { return it }
        return WebView(appContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            // 桌面 UA：快手/抖音分享短链在桌面 UA 下才 302 到带数据的桌面播放页
            // （移动 UA 会被甩到「请在 App 内观看」拒绝页/中转遮罩页，拿不到任何媒体数据）
            settings.userAgentString = DESKTOP_UA
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            settings.allowUniversalAccessFromFileURLs = true
            settings.allowFileAccessFromFileURLs = true
            settings.mediaPlaybackRequiresUserGesture = true
            // 视频地址/图集图片回传桥：web_inject.js 捕获到后经此回调
            addJavascriptInterface(androidVideoBridge, "AndroidVideoBridge")
        }.also { webView = it }
    }

    /** 改写为可提取的桌面页（与 WebViewActivity.shouldOverrideUrlLoading 同规则）。 */
    private fun normalizeUrl(url: String, source: String): String {
        var u = url.trim()
        // 抖音分享短链（v.douyin.com）让 WebView 原生跟随重定向；服务端 302 落地若为
        // iesdouyin 拒绝页，由 shouldOverrideUrlLoading 改写。无协议时补全 https。
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
        return u
    }

    private fun attachClient(
        wv: WebView,
        session: Session,
        pageUrl: String,
        source: String,
        onDone: (Result?) -> Unit
    ) {
        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                // 新文档就绪前先注入 XHR/fetch 钩子与 a_bogus（幂等守卫，重复注入无副作用）
                injectJs(view, "web_inject.js")
                if (source == "douyin") injectJs(view, "abogus.js")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // 文档就绪后重新注入：SPA 页面跳转会清掉早先注入的钩子；
                // 抖音/快手视频数据由页面 JS 在 onPageFinished 后才 XHR/fetch 拉取
                injectJs(view, "web_inject.js")
                if (source == "douyin") injectJs(view, "abogus.js")
                // 稍候片刻等 SPA 水合发出首个数据请求，再开始轮询提取
                view?.postDelayed({
                    if (!session.done) {
                        pollExtract(view, session, pageUrl, source, onDone, PAGE_READY_DELAY_MS)
                    }
                }, PAGE_READY_DELAY_MS)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val u = request?.url?.toString() ?: return false
                // 抖音分享页 302 到 iesdouyin.com/share/note|video/{id} 是「请在 App 内观看」拒绝页，
                // 改写为真·桌面图文/播放页（与 WebViewActivity 一致；快手短链由 WebView 原生处理）
                Regex("""iesdouyin\.com/share/note/(\d+)""").find(u)?.let { m ->
                    view?.loadUrl("https://www.douyin.com/note/${m.groupValues[1]}")
                    return true
                }
                Regex("""iesdouyin\.com/share/video/(\d+)""").find(u)?.let { m ->
                    view?.loadUrl("https://www.douyin.com/video/${m.groupValues[1]}")
                    return true
                }
                return false
            }

            /**
             * WebView 渲染进程崩溃/被系统 OOM 回收时接管，避免 aw_browser_terminator 连带杀掉宿主 App
             * （实测：模拟器 2.5GB 内存加载抖音重型 note 页时 renderer 被 OOM 杀，无本回调则整个
             *  DownloadService 进程 signal 9 死亡）。
             * 返回 true = 宿主自行处理：结束本次解析（返回 null 优雅失败）并销毁重建 WebView。
             */
            @RequiresApi(android.os.Build.VERSION_CODES.O)
            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                Log.w(TAG, "WebView 渲染进程 gone(crash=${detail?.didCrash()})，接管：结束解析并重建，不杀宿主")
                if (!session.done) {
                    session.done = true
                    activeSession = null
                    onDone(null)
                }
                runCatching { view?.stopLoading() }
                runCatching { view?.destroy() }
                if (webView === view) webView = null
                return true
            }
        }
    }

    /**
     * 轮询提取：每次 evaluate 对应平台的 extractor JS，解析 {urls, image_urls, content}，
     * 与 bridge 捕获（web_inject.js 的 onVideoUrl/onImageUrl）合并；任一非空即结束。
     * 无结果则每 [POLL_INTERVAL_MS] 重试，直到总超时（由 parse 的 postDelayed 兜底）。
     */
    private fun pollExtract(
        wv: WebView,
        session: Session,
        pageUrl: String,
        source: String,
        onDone: (Result?) -> Unit,
        delayMs: Long
    ) {
        if (session.done) return
        mainHandler.postDelayed({
            if (session.done) return@postDelayed
            val jsName = if (source == "douyin") "douyin_extractor.js" else "kuaishou_extractor.js"
            val js = readAssetFile(jsName)
            if (js == null) {
                Log.w(TAG, "extractor asset 缺失: $jsName")
                onDone(null)
                return@postDelayed
            }
            runCatching {
                wv.evaluateJavascript(js) { raw ->
                    if (session.done) return@evaluateJavascript
                    val (jsVideos, jsImages, contentText, title) = parseExtractorJson(raw)
                    val videos = (jsVideos + session.videoUrls)
                        .filter { it.startsWith("http") && !it.endsWith(".m3u8") }
                        .distinct()
                    val images = (jsImages + session.imageUrls)
                        .filter { it.startsWith("http") && !it.endsWith(".m3u8") }
                        .distinct()
                    if (videos.isNotEmpty() || images.isNotEmpty()) {
                        Log.i(TAG, "提取成功 source=$source videos=${videos.size} images=${images.size} title=${title.take(40)}")
                        onDone(Result(videos, images, contentText, title, pageUrl))
                    } else {
                        // 未命中：等 SPA 更多数据/页面水合完成后重试
                        pollExtract(wv, session, pageUrl, source, onDone, POLL_INTERVAL_MS)
                    }
                }
            }.onFailure { e ->
                // evaluateJavascript 偶发在页面导航窗口抛异常：稍后重试即可
                Log.d(TAG, "evaluate extractor 失败，稍后重试: ${e.message}")
                pollExtract(wv, session, pageUrl, source, onDone, POLL_INTERVAL_MS)
            }
        }, delayMs)
    }

    private fun injectJs(view: WebView?, assetName: String) {
        if (view == null) return
        val js = readAssetFile(assetName) ?: return
        runCatching { view.evaluateJavascript(js, null) }
    }

    /**
     * 解析 evaluateJavascript 返回的 extractor 结果字符串。
     * JS 返回 { urls:[], image_urls:[], content:{content,title,desc} }；
     * evaluateJavascript 会把对象序列化成 JSON 字符串并做一层转义（首尾带引号）。
     */
    private fun parseExtractorJson(raw: String?): Extracted {
        var s = raw?.trim().orEmpty()
        if (s.isEmpty() || s == "null") return Extracted()
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
        }
        return try {
            val json = JSONObject(s)
            val urls = json.optJSONArray("urls")?.let { arr ->
                (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.startsWith("http") } }
            } ?: emptyList()
            val images = json.optJSONArray("image_urls")?.let { arr ->
                (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.startsWith("http") } }
            } ?: emptyList()
            val content = json.optJSONObject("content")
            val title = content?.optString("title", "")?.takeIf { it.isNotBlank() } ?: ""
            val contentText = content?.optString("content", "")?.takeIf { it.isNotBlank() } ?: ""
            Extracted(urls, images, contentText, title)
        } catch (e: Exception) {
            Log.d(TAG, "extractor JSON 解析失败: ${e.message}")
            Extracted()
        }
    }

    private data class Extracted(
        val videos: List<String> = emptyList(),
        val images: List<String> = emptyList(),
        val contentText: String = "",
        val title: String = ""
    )

    private fun readAssetFile(fileName: String): String? {
        return try {
            appContext.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "read asset 失败: $fileName", e)
            null
        }
    }

    // endregion

    companion object {
        private const val TAG = "BgWebViewParser"
        // 与 WebViewActivity 的桌面 UA 保持一致（桌面播放/图文页才有媒体数据）
        const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        /** onPageFinished 后开始轮询提取的等待时长（等 SPA 水合发出首个数据请求）。 */
        private const val PAGE_READY_DELAY_MS = 1200L
        /** 无结果时的轮询间隔。 */
        private const val POLL_INTERVAL_MS = 800L
        /** 单次解析总超时（含页面加载 + 轮询窗口）。 */
        private const val TOTAL_TIMEOUT_MS = 60_000L
    }
}
