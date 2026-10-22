package com.neoruaa.xhsdn.web

import androidx.annotation.RequiresApi
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
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

        // region 失败归因字段（v3.2.5）
        /** onPageFinished 时间戳（0 = 未触发）。用于区分「页面没加载出来」与「页面加载了但没媒体」。 */
        @Volatile
        var pageFinishedAt: Long = 0L

        /** 轮询期间是否曾见到任何媒体候选（直链或图片）。 */
        @Volatile
        var sawAnyCandidate: Boolean = false

        /** 硬失败原因（主文档加载失败 / 渲染进程被回收），非空时优先作为失败归因。 */
        @Volatile
        var hardFail: String? = null
        // endregion
    }

    /** 主页作品列表嗅探结果（L2a）。 */
    data class HomepageSniff(
        val items: List<com.neoruaa.xhsdn.douyin.DouyinPostItem>,
        val nickname: String?,
        val totalCount: Int,
        val hasMore: Boolean,
        val logTail: String
    ) {
        val isNotEmpty: Boolean get() = items.isNotEmpty()
    }

    /** 主页嗅探会话：bridge 回传在 WebView 线程，集合用同步包装。 */
    private class SniffSession {
        val items: MutableList<com.neoruaa.xhsdn.douyin.DouyinPostItem> =
            Collections.synchronizedList(mutableListOf())
        val ids: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

        @Volatile var nickname: String? = null
        @Volatile var totalCount: Int = 0
        @Volatile var hasMore: Boolean = false
        @Volatile var done: Boolean = false
        /** 首条作品落地的时间戳（0 = 尚未命中）。用于给档案接口留一小段收口窗口。 */
        @Volatile var firstItemAt: Long = 0L
        /** JS 侧诊断日志流（保留全部，便于定位「页面是否发出数据请求」）。 */
        val logs: MutableList<String> = Collections.synchronizedList(mutableListOf())
    }

    /**
     * 小红书主页嗅探：单条作品（[XHS_SNIFF_ASSET] 裁剪版）。
     * user_posted 列表**不带媒体直链**，只有封面/标题/xsec_token——详情由原生侧
     * 逐条 `explore/{id}?xsec_token=` 页获取（v3.3.0 P0，见 DownloadService.xhs 批量）。
     */
    data class XhsPostItem(
        val id: String,
        val xsecToken: String,
        val title: String,
        val isVideo: Boolean,
        val coverUrl: String?,
        /** SPA feed API 截获的媒体直链（v3.3.2）；空=未取到。 */
        val videoUrls: List<String> = emptyList(),
        val imageUrls: List<String> = emptyList()
    )

    /** 小红书主页嗅探结果（L2x-1）。 */
    data class XhsHomepageSniff(
        val items: List<XhsPostItem>,
        val nickname: String?,
        val totalCount: Int,
        val hasMore: Boolean,
        val logTail: String
    ) {
        val isNotEmpty: Boolean get() = items.isNotEmpty()
    }

    /** 小红书主页嗅探会话（同 [SniffSession]，条目为 [XhsPostItem]）。 */
    private class XhsSniffSession {
        val items: MutableList<XhsPostItem> = Collections.synchronizedList(mutableListOf())

        @Volatile var nickname: String? = null
        @Volatile var totalCount: Int = 0
        @Volatile var hasMore: Boolean = false
        @Volatile var done: Boolean = false
        /** 最近一次新增条目的时间戳：连续 [XHS_PAGE_QUIET_MS] 无新增 = 滚动翻页已尽。 */
        @Volatile var lastItemAt: Long = 0L
        val logs: MutableList<String> = Collections.synchronizedList(mutableListOf())
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutex = Mutex()

    /**
     * 最近一次 [parse] 的失败原因（v3.2.5）。
     * 旧行为：超时 / 无媒体 / 页面加载失败一律返回 null，上层只能报「WebView 未取到媒体」，
     * 用户和开发者都无法区分「页面根本没打开」「页面是风控空页」「渲染进程被系统回收」。
     * [parse] 由 [mutex] 串行化，故单字段 + @Volatile 足够。
     */
    @Volatile
    private var lastFailReason: String = ""

    /** 最近一次 [parse] 的失败原因；成功或尚未解析时为空串。 */
    fun lastFailReason(): String = lastFailReason

    /** 把会话状态汇总成一句可诊断的失败原因（v3.2.5）。 */
    private fun buildFailReason(session: Session): String {
        session.hardFail?.let { return it }
        return when {
            session.pageFinishedAt == 0L -> "页面在超时前未加载完成（可能网络不可达/该域名被拦截）"
            !session.sawAnyCandidate -> "页面已加载但未产出任何媒体候选（疑似抖音风控页/登录墙）"
            else -> "页面已加载且出现过候选，但未凑齐可用媒体（疑似提取器选择器失配）"
        }
    }

    /** 串行复用的单 WebView；只在主线程访问。 */
    private var webView: WebView? = null
    /** bridge 回调路由到的当前会话（主线程赋值/清空）。 */
    private var activeSession: Session? = null
    /** 主页嗅探会话（主线程赋值/清空；bridge 在 WebView 线程读取）。 */
    @Volatile
    private var sniffSession: SniffSession? = null

    /** 小红书主页嗅探会话（主线程赋值/清空；bridge 在 WebView 线程读取）。 */
    @Volatile
    private var xhsSniffSession: XhsSniffSession? = null

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

        /** 主页嗅探：一页作品（douyin_home_sniff.js 裁剪后回传，结构与 post 接口同构）。 */
        @JavascriptInterface
        fun onHomepagePage(json: String) {
            val s = sniffSession ?: return
            runCatching {
                val page = com.neoruaa.xhsdn.douyin.DouyinParser.parsePostListJson(json)
                s.nickname = s.nickname ?: page.nickname
                s.totalCount = maxOf(s.totalCount, page.totalCount)
                s.hasMore = page.hasMore
                for (item in page.items) {
                    if (s.ids.add(item.id)) s.items.add(item)
                }
                if (s.items.isNotEmpty() && s.firstItemAt == 0L) s.firstItemAt = System.currentTimeMillis()
            }.onFailure { Log.w(TAG, "sniff 页解析失败: ${it.message}") }
        }

        /**
         * 主页嗅探：作者档案（`user/profile/other` 的 user 节点）。
         *
         * post 响应里的 author 节点**不含** aweme_count，作品总数只能从该接口取
         * （实测匿名上限 21 条时 aweme_count=253）→ 用于「已取 M 条 / 共 N 条」提示。
         */
        @JavascriptInterface
        fun onHomepageProfile(json: String) {
            val s = sniffSession ?: return
            runCatching {
                val o = JSONObject(json)
                val n = o.optString("nickname", "").takeIf { it.isNotBlank() }
                val c = o.optInt("aweme_count", 0)
                if (n != null) s.nickname = s.nickname ?: n
                if (c > 0) s.totalCount = maxOf(s.totalCount, c)
            }.onFailure { Log.w(TAG, "sniff 档案解析失败: ${it.message}") }
        }

        /** 主页嗅探：JS 侧诊断日志（hook 安装/异常/回传统计）。 */
        @JavascriptInterface
        fun onHomepageSniffLog(msg: String) {
            sniffSession?.logs?.add(msg)
            Log.d(TAG, "sniff js: $msg")
        }

        /**
         * 小红书主页嗅探：一页作品（xhs_home_sniff.js 裁剪后回传）。
         * payload = { notes:[{id,xsec_token,title,type,cover}], has_more, cursor, total, page }。
         * JS 侧已按 noteId 跨页去重，此处再防御一次（无 Set，线性查 ≤ 数百条可接受）。
         */
        @JavascriptInterface
        fun onXhsPosted(json: String) {
            val s = xhsSniffSession ?: return
            runCatching {
                val o = JSONObject(json)
                val arr = o.optJSONArray("notes") ?: return
                s.hasMore = o.optBoolean("has_more", false)
                val t = o.optInt("total", 0)
                if (t > 0) s.totalCount = maxOf(s.totalCount, t)
                for (i in 0 until arr.length()) {
                    val n = arr.optJSONObject(i) ?: continue
                    val id = n.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val tok = n.optString("xsec_token")
                    val existingIdx = s.items.indexOfFirst { it.id == id }
                    if (existingIdx >= 0) {
                        // 岛路径先到时 token 为空；后到的 user_posted 条目带 note 级 xsec_token → 补进
                        // （batch 下载要靠它拼 discovery/item?xsec_token=… 打开完整笔记页）
                        if (tok.isNotBlank() && s.items[existingIdx].xsecToken.isBlank()) {
                            s.items[existingIdx] = s.items[existingIdx].copy(xsecToken = tok)
                        }
                        continue
                    }
                    s.items.add(
                        XhsPostItem(
                            id = id,
                            xsecToken = tok,
                            title = n.optString("title"),
                            isVideo = n.optString("type") == "video",
                            coverUrl = n.optString("cover").takeIf { it.isNotBlank() }
                        )
                    )
                    s.lastItemAt = System.currentTimeMillis()
                }
            }.onFailure { Log.w(TAG, "xhs posted 解析失败: ${it.message}") }
        }

        /** 小红书主页嗅探：作者档案（user/other → data.nickname / data.notes=作品总数）。 */
        @JavascriptInterface
        fun onXhsProfile(json: String) {
            val s = xhsSniffSession ?: return
            runCatching {
                val o = JSONObject(json)
                val n = o.optString("nickname", "").takeIf { it.isNotBlank() }
                val c = o.optInt("total", 0)
                if (n != null) s.nickname = s.nickname ?: n
                if (c > 0) s.totalCount = maxOf(s.totalCount, c)
            }.onFailure { Log.w(TAG, "xhs profile 解析失败: ${it.message}") }
        }

        /** SPA feed API 截获的媒体直链（v3.3.2）：{media:[{id,videoUrls,imageUrls}]}。 */
        @JavascriptInterface
        fun onXhsMedia(json: String) {
            val s = xhsSniffSession ?: return
            runCatching {
                val o = JSONObject(json)
                val arr = o.optJSONArray("media") ?: return
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    val id = m.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val vUrls = mutableListOf<String>().apply {
                        val va = m.optJSONArray("videoUrls")
                        if (va != null) for (j in 0 until va.length()) add(va.optString(j))
                    }
                    val iUrls = mutableListOf<String>().apply {
                        val ia = m.optJSONArray("imageUrls")
                        if (ia != null) for (j in 0 until ia.length()) add(ia.optString(j))
                    }
                    // 更新已有 item（by id）
                    val idx = s.items.indexOfFirst { it.id == id }
                    if (idx >= 0) {
                        s.items[idx] = s.items[idx].copy(videoUrls = vUrls, imageUrls = iUrls)
                    }
                }
                Log.d(TAG, "onXhsMedia: updated ${arr.length()} items")
            }.onFailure { Log.w(TAG, "xhs media 解析失败: ${it.message}") }
        }
    }

    /**
     * 后台解析单个抖音/快手作品页。串行执行（同一 WebView 同时只跑一个任务），
     * 成功返回非空 [Result]，超时/页面无媒体返回 null。
     *
     * @param url    分享链接或作品页 URL（v.douyin.com 短链 / www.douyin.com/video|note/{id} /
     *               www.kuaishou.com 作品链接均可，内部按桌面页改写规则处理）
     * @param source "douyin" | "kuaishou" | "xhs"
     *               （xhs = 小红书 explore/{noteId}?xsec_token=… 详情页，提取脚本 [XHS_NOTE_EXTRACTOR_ASSET]）
     */
    suspend fun parse(url: String, source: String): Result? {
        if (source != "douyin" && source != "kuaishou" && source != "xhs") return null
        return mutex.withLock {
            suspendCancellableCoroutine { cont ->
                mainHandler.post {
                    lastFailReason = ""
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
                        // 防污染：xhs 嗅探可能切过移动 UA；xhs note 页用移动 UA（桌面 explore 游客态 300031 封禁）
                        wv.settings.userAgentString = if (source == "xhs") XHS_MOBILE_UA else DESKTOP_UA
                        wv.loadUrl(pageUrl)
                        Log.d(TAG, "loadUrl 已发起: $pageUrl")

                        // 总超时兜底：页面水合慢 / 无媒体时在预算内结束，绝不无限等待
                        mainHandler.postDelayed({
                            if (!session.done) {
                                session.done = true
                                activeSession = null
                                lastFailReason = buildFailReason(session)
                                Log.w(TAG, "后台解析超时(${TOTAL_TIMEOUT_MS}ms): url=$url reason=$lastFailReason")
                                if (cont.isActive) cont.resume(null)
                            }
                        }, TOTAL_TIMEOUT_MS)
                    } catch (e: Exception) {
                        Log.w(TAG, "后台解析异常: url=$url", e)
                        if (!session.done) {
                            session.done = true
                            activeSession = null
                            lastFailReason = "解析异常: ${e.message}"
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
            }
        }
    }

    /**
     * Cookie 预热 + 快照（note 免登录 detail API 方案 P2）。
     *
     * 背景：抖音 Web detail API 需要"完整浏览器指纹 cookie"（ttwid/UIFID_TEMP/
     * __ac_signature/s_v_web_id…），这些只能由真实浏览器 JS 执行后种下，纯 HTTP 拿不到
     * （匿名直调 detail → 403 ArgusSecurityPlugin Uifid Not Found，沙箱实证）。
     *
     * 做法：复用本类同一后台 WebView（Mutex 串行，不弹窗），加载目标页面让抖音风控壳
     * JS 执行种 cookie（CookieManager 全局自动写入，与 WebViewActivity 共享 app_webview 目录）。
     *
     * 完成判定（v3.0.3 改造）：
     * - 传入 [requireCookies] 时**按 cookie 内容轮询**，全中即刻返回（不再依赖 onPageFinished
     *   ——慢机软渲染下用户主页可能长时间不触发该回调，旧实现因此空转超时）；
     * - 不传 [requireCookies] 时保持旧行为：onPageFinished 后等 [settleMs] 再快照。
     * 预算 [budgetMs] 耗尽时返回**已有 cookie**（部分成功优于全失败）。
     *
     * ⚠️ 主页 post API 预热必须加载**用户主页** `www.douyin.com/user/{sec_uid}`，
     * 而非轻量首页 `www.douyin.com/`：`__ac_signature` / `bd_ticket_guard_client_data`
     * 只在用户主页由 secsdk 生成，轻量首页仅给 UIFID_TEMP → post 接口会返回空 body。
     * 详见 docs/design/主页下载-全局重构方案-v3.md §1.6 对照实验。
     *
     * 调用方拿到 cookie 串后无需手工透传——CookieManager 已全局更新，OkHttp 侧
     * DouyinParser.fetchWebDetail 重新快照即得新 cookie。
     *
     * @return douyin.com 域 cookie 串（name=value;…）；失败返回 null。
     */
    suspend fun warmupAndSnapshot(
        url: String,
        settleMs: Long = WARMUP_SETTLE_MS,
        requireCookies: List<String> = emptyList(),
        budgetMs: Long = WARMUP_TOTAL_TIMEOUT_MS
    ): String? {
        if (url.isBlank()) return null
        return mutex.withLock {
            suspendCancellableCoroutine { cont ->
                mainHandler.post {
                    var finished = false
                    var pageFinishedAt = 0L
                    val startedAt = System.currentTimeMillis()
                    val finish: (String?) -> Unit = { cookie ->
                        if (!finished) {
                            finished = true
                            if (cont.isActive) cont.resume(cookie)
                        }
                    }
                    /** 轮询：requireCookies 全中即提前结束；或 onPageFinished 后满 settleMs。 */
                    val poll: Runnable = object : Runnable {
                        override fun run() {
                            if (finished) return
                            val cookie = snapshotDouyinCookies()
                            val allPresent = requireCookies.isNotEmpty() &&
                                requireCookies.all { cookie?.contains(it) == true }
                            if (allPresent) {
                                Log.i(TAG, "warmup 就绪 cookie=${cookie?.length ?: 0}字符 含${requireCookies.joinToString("/")}")
                                finish(cookie)
                                return
                            }
                            val settled = requireCookies.isEmpty() && pageFinishedAt > 0 &&
                                System.currentTimeMillis() - pageFinishedAt >= settleMs
                            if (settled) {
                                val hasUifid = cookie?.contains("UIFID_TEMP") == true
                                Log.i(TAG, "warmup 完成 cookie=${cookie?.length ?: 0}字符${if (hasUifid) " 含UIFID_TEMP" else ""}")
                                finish(cookie)
                                return
                            }
                            if (System.currentTimeMillis() - startedAt >= budgetMs) {
                                Log.w(TAG, "warmup 预算耗尽(${budgetMs}ms) cookie=${cookie?.length ?: 0}字符 " +
                                    "缺=${requireCookies.filter { cookie?.contains(it) != true }.joinToString("/")}")
                                finish(cookie)
                                return
                            }
                            mainHandler.postDelayed(this, WARMUP_POLL_MS)
                        }
                    }
                    try {
                        val wv = getOrCreateWebView()
                        wv.stopLoading()
                        wv.webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, u: String?, favicon: android.graphics.Bitmap?) {
                                // 风控壳/secdsk 种 cookie 时会读环境信号；保持与 DESKTOP_UA 一致
                                injectJs(view, DESKTOP_ENV_ASSET)
                            }

                            override fun onPageFinished(view: WebView?, u: String?) {
                                Log.d(TAG, "warmup onPageFinished: ${u?.take(80)}")
                                if (pageFinishedAt == 0L) pageFinishedAt = System.currentTimeMillis()
                            }
                        }
                wv.settings.userAgentString = DESKTOP_UA
                wv.loadUrl(url)
                // 轮询起点：不等 onPageFinished（慢机用户主页可能长时间不触发，旧实现因此空转）
                        mainHandler.postDelayed(poll, WARMUP_POLL_MS)
                        // 硬预算兜底（poll 内也判一次，双保险）
                        mainHandler.postDelayed({ finish(snapshotDouyinCookies()) }, budgetMs + 1_000L)
                    } catch (e: Exception) {
                        Log.w(TAG, "warmup 异常: ${url.take(80)}", e)
                        finish(null)
                    }
                }
            }
        }
    }

    /** 快照 douyin.com 域 cookie（CookieManager 标准 "name=value;…" 串，detail API 直接可用）。 */
    private fun snapshotDouyinCookies(): String? = runCatching {
        cookieManager.getCookie("https://www.douyin.com/")
    }.getOrNull()?.takeIf { it.isNotBlank() }

    /**
     * L2a · 作者主页作品列表响应嗅探（主页下载的主路径）。
     *
     * 原理：加载 `https://www.douyin.com/user/{secUid}`，用 [SNIFF_ASSET] 旁路 hook 页面的
     * fetch/XHR，把**页面自己发出的** `/aweme/v1/web/aweme/post/` 响应裁裁剪后回传。
     * 不重算任何签名——抖音要求 `uifid` / `x-secsdk-web-signature` / `verifyFp` 等
     * 只能由页面 secsdk JS 产出的参数，纯 HTTP 自建请求必被 403（详见方案文档 §1.6）。
     *
     * 实测上限：**匿名状态服务端只返回首屏 21 条**（has_more=1 且页面显示
     * 「登录后查看更多作品」）；要拿更多需用户登录。故本方法拿到 ≥1 页即返回，
     * 由调用方按 [HomepageSniff.hasMore] 决定是否引导登录。
     * 作品总数（[HomepageSniff.totalCount]）**不在 post 响应里**，由同一页并行发出的
     * `user/profile/other` 接口提供（`user.aweme_count`，实测匿名上限时 253），
     * 见 [androidVideoBridge.onHomepageProfile]。
     *
     * ⚠️ **前置条件：必须注入 [DESKTOP_ENV_ASSET] 统一环境身份**（本方法已内置）。
     * 抖音 post 接口会交叉校验「UA 声明的平台」与 `navigator.platform`：本 App 的 WebView
     * 用 [DESKTOP_UA]（Windows）而 `navigator.platform` 为 Android 的 Linux 值，
     * 服务端因此**静默拒绝**——HTTP 200 + 空 body（不是 403，状态码看不出问题）。
     * CDP 单变量对照（2026-10-10）：同一 UA 下 `platform=Win32` → 21 条；
     * `platform=Linux x86_64` → 0 字节。WebGL 渲染器/核数/内存/屏幕均**非**触发条件。
     * 详见 docs/design/主页下载-全局重构方案-v3.md §1.8。
     *
     * @return 嗅探结果；超时未嗅到任何作品返回 null。
     */
    suspend fun sniffHomepage(secUid: String, budgetMs: Long = SNIFF_BUDGET_MS): HomepageSniff? {
        if (secUid.isBlank()) return null
        return mutex.withLock { sniffOnce(secUid, budgetMs) }
    }

    /**
     * 单轮嗅探（调用方需持有 [mutex]）。
     */
    private suspend fun sniffOnce(secUid: String, budgetMs: Long): HomepageSniff? {
        val url = "https://www.douyin.com/user/$secUid"
        return suspendCancellableCoroutine { cont ->
            mainHandler.post {
                    val session = SniffSession()
                    sniffSession = session
                    val startedAt = System.currentTimeMillis()
                    var finished = false
                    val finish: () -> Unit = {
                        if (!finished) {
                            finished = true
                            sniffSession = null
                            if (cont.isActive) {
                                val snapshot = HomepageSniff(
                                    items = session.items.toList(),
                                    nickname = session.nickname,
                                    totalCount = session.totalCount,
                                    hasMore = session.hasMore,
                                    logTail = session.logs.toList().joinToString(" ; ")
                                )
                                Log.i(TAG, "sniff 结束 items=${snapshot.items.size} total=${snapshot.totalCount} " +
                                    "hasMore=${snapshot.hasMore} log=${snapshot.logTail}")
                                cont.resume(snapshot)
                            }
                        }
                    }
                    // 轮询：拿到首屏即返回（匿名上限一页，再等无意义）；
                    // 已登录账号可能翻页，故给它到预算耗尽前的多次机会
                    val poll: Runnable = object : Runnable {
                        override fun run() {
                            if (finished) return
                            if (session.items.isNotEmpty()) {
                                // 等作者档案（totalCount）落地再收口：实测档案请求先于 post 返回，
                                // 故正常情况下零额外延迟；万一没来，最多多等 [PROFILE_GRACE_MS]。
                                val firstAt = session.firstItemAt
                                if (session.totalCount > 0 || firstAt == 0L ||
                                    System.currentTimeMillis() - firstAt >= PROFILE_GRACE_MS
                                ) {
                                    Log.i(TAG, "sniff 首屏命中 ${session.items.size} 条（共 ${session.totalCount}），提前返回")
                                    finish()
                                }
                                return
                            }
                            if (System.currentTimeMillis() - startedAt >= budgetMs) {
                                Log.w(TAG, "sniff 预算耗尽(${budgetMs}ms) 未嗅到作品 log=${session.logs.toList().joinToString(" ; ")}")
                                finish()
                                return
                            }
                            mainHandler.postDelayed(this, SNIFF_POLL_MS)
                        }
                    }
                    try {
                        val wv = getOrCreateWebView()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            cookieManager.setAcceptThirdPartyCookies(wv, true)
                        }
                        // 离屏 WebView 默认无尺寸（从未 addView 到 window）→ SPA 视口 0×0，
                        // 作品网格可能不渲染、数据请求可能不发。显式 measure/layout 给一个
                        // 桌面级视口，保证页面按正常桌面布局渲染（只影响测量，不涉及 attach）。
                        runCatching {
                            wv.layoutParams = ViewGroup.LayoutParams(SNIFF_VIEW_W, SNIFF_VIEW_H)
                            wv.measure(
                                View.MeasureSpec.makeMeasureSpec(SNIFF_VIEW_W, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(SNIFF_VIEW_H, View.MeasureSpec.EXACTLY)
                            )
                            wv.layout(0, 0, SNIFF_VIEW_W, SNIFF_VIEW_H)
                        }
                        wv.stopLoading()
                        wv.webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, u: String?, favicon: android.graphics.Bitmap?) {
                                // 顺序敏感：先统一环境身份，再装嗅探钩子（都在页面主 JS 之前）
                                injectJs(view, DESKTOP_ENV_ASSET)
                                injectJs(view, SNIFF_ASSET)
                            }

                            override fun onPageFinished(view: WebView?, u: String?) {
                                // 幂等守卫（__dyDesktopEnv / __dyHomeSniff）保证重复注入无副作用
                                injectJs(view, DESKTOP_ENV_ASSET)
                                injectJs(view, SNIFF_ASSET)
                            }

                            @RequiresApi(android.os.Build.VERSION_CODES.O)
                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                Log.w(TAG, "sniff 渲染进程 gone，结束并重建 WebView")
                                finish()
                                runCatching { view?.stopLoading() }
                                runCatching { view?.destroy() }
                                if (webView === view) webView = null
                                return true
                            }
                        }
                wv.settings.userAgentString = DESKTOP_UA
                wv.loadUrl(url)
                Log.d(TAG, "sniff loadUrl: $url")
                        mainHandler.postDelayed(poll, SNIFF_POLL_MS)
                        mainHandler.postDelayed({ finish() }, budgetMs + 1_000L)
                    } catch (e: Exception) {
                        Log.w(TAG, "sniff 异常: ${url.take(80)}", e)
                        finish()
                    }
                }
            }
    }

    /**
     * L2x-1 · 小红书作者主页作品列表响应嗅探（v3.3.0 P0）。
     *
     * 原理：加载 `https://www.xiaohongshu.com/user/profile/{userId}`，用 [XHS_SNIFF_ASSET]
     * 旁路 hook 页面 fetch/XHR，把**页面自己发出的** `/api/sns/web/v1/user_posted` 响应
     * 裁剪后回传（列表含 id/xsec_token/标题/封面，**不含媒体直链**）。
     * 不重算任何签名——`x-s` / `x-s-common` 只能由页面 xhs.js 产出，与抖音 secsdk 同理。
     *
     * 翻页：cursor 翻页由页面滚动触发，JS 侧 t+4s/7s 自动滚动兜底；原生侧以
     * 「连续 [XHS_PAGE_QUIET_MS] 无新增条目」为收口条件（与抖音「首屏即返回」不同——
     * 小红书匿名状态下滚动翻页**可能**放出多页，值得多等一个静默窗）。
     *
     * @return 嗅探结果；预算内未嗅到任何作品返回**空结果**（items 为空，调用方判 isNotEmpty）。
     */
    suspend fun sniffXhsHomepage(
        userId: String,
        budgetMs: Long = SNIFF_BUDGET_MS,
        /** 分享短链跟跳出的完整主页 URL（自带 xsec_token 时页面初始加载更稳）；空=规范长链。 */
        startUrl: String? = null
    ): XhsHomepageSniff {
        if (userId.isBlank()) return XhsHomepageSniff(emptyList(), null, 0, false, "")
        return mutex.withLock { sniffXhsOnce(userId, budgetMs, startUrl) }
    }

    /** 小红书单轮嗅探（调用方需持有 [mutex]）。 */
    private suspend fun sniffXhsOnce(userId: String, budgetMs: Long, startUrl: String?): XhsHomepageSniff {
        val url = startUrl?.takeIf { it.contains("xiaohongshu.com/user/profile/") }
            ?: "https://www.xiaohongshu.com/user/profile/$userId"
        return suspendCancellableCoroutine { cont ->
            mainHandler.post {
                val session = XhsSniffSession()
                xhsSniffSession = session
                val startedAt = System.currentTimeMillis()
                var finished = false
                val finish: () -> Unit = {
                    if (!finished) {
                        finished = true
                        xhsSniffSession = null
                        if (cont.isActive) {
                            val snapshot = XhsHomepageSniff(
                                items = session.items.toList(),
                                nickname = session.nickname,
                                totalCount = session.totalCount,
                                hasMore = session.hasMore,
                                logTail = session.logs.toList().joinToString(" ; ")
                            )
                            Log.i(TAG, "xhs sniff 结束 items=${snapshot.items.size} total=${snapshot.totalCount} " +
                                "hasMore=${snapshot.hasMore} log=${snapshot.logTail}")
                            cont.resume(snapshot)
                        }
                    }
                }
                val poll: Runnable = object : Runnable {
                    override fun run() {
                        if (finished) return
                        // 静默收口：已有条目且连续 QUIET_MS 无新增（滚动翻页已尽/被登录墙截断）
                        val quiet = session.items.isNotEmpty() &&
                            System.currentTimeMillis() - session.lastItemAt >= XHS_PAGE_QUIET_MS
                        if (quiet || System.currentTimeMillis() - startedAt >= budgetMs) {
                            Log.i(TAG, "xhs sniff 收口 items=${session.items.size} quiet=$quiet " +
                                "log=${session.logs.toList().takeLast(3).joinToString(" ; ")}")
                            finish()
                            return
                        }
                        mainHandler.postDelayed(this, SNIFF_POLL_MS)
                    }
                }
                try {
                    val wv = getOrCreateWebView()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        cookieManager.setAcceptThirdPartyCookies(wv, true)
                    }
                    // 离屏 WebView 桌面级视口（同 sniffOnce，保证 SPA 正常布局渲染与滚动翻页）
                    runCatching {
                        wv.layoutParams = ViewGroup.LayoutParams(SNIFF_VIEW_W, SNIFF_VIEW_H)
                        wv.measure(
                            View.MeasureSpec.makeMeasureSpec(SNIFF_VIEW_W, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(SNIFF_VIEW_H, View.MeasureSpec.EXACTLY)
                        )
                        wv.layout(0, 0, SNIFF_VIEW_W, SNIFF_VIEW_H)
                    }
                    wv.stopLoading()
                    wv.webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, u: String?, favicon: android.graphics.Bitmap?) {
                            // 隔离实验：页面脚本前只注入被动探针（不 hook），验证 hook 是否破坏水合
                            injectJs(view, "xhs_home_sniff_passive.js")
                        }

                        override fun onPageFinished(view: WebView?, u: String?) {
                            // 幂等守卫（__xhsHomeSniff）保证重复注入无副作用
                            injectJs(view, XHS_SNIFF_ASSET)
                        }

                        @RequiresApi(android.os.Build.VERSION_CODES.O)
                        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                            Log.w(TAG, "xhs sniff 渲染进程 gone，结束并重建 WebView")
                            finish()
                            runCatching { view?.stopLoading() }
                            runCatching { view?.destroy() }
                            if (webView === view) webView = null
                            return true
                        }
                    }
                    // 移动 H5 试探：用移动 Chrome UA 打开分享落地页（= 手机用户看到的版本），
                    // 看小红书是否对移动访客保留 noteId（桌面游客态已实测被去 id）
                    wv.settings.userAgentString = XHS_MOBILE_UA
                    wv.loadUrl(url)
                    Log.d(TAG, "xhs sniff loadUrl(mobile): $url")
                    mainHandler.postDelayed(poll, SNIFF_POLL_MS)
                    mainHandler.postDelayed({ finish() }, budgetMs + 1_000L)
                } catch (e: Exception) {
                    Log.w(TAG, "xhs sniff 异常: ${url.take(80)}", e)
                    finish()
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
            // debug 构建开 WebView 远程调试（chrome://inspect / adb forward CDP），
            // 供后台解析页面取证（落地 URL / XHR 网络 / window 全局）。release 不开。
            if (com.neoruaa.xhsdn.BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
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
                Log.d(TAG, "onPageStarted: ${url?.takeLast(60)}")
                // 新文档就绪前先注入 XHR/fetch 钩子与 a_bogus（幂等守卫，重复注入无副作用）
                injectJs(view, "web_inject.js")
                if (source == "douyin") injectJs(view, "abogus.js")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(TAG, "onPageFinished: ${url?.takeLast(60)}")
                // 记首次就绪时间：失败归因与「无候选早退」都依赖它（v3.2.5）
                if (session.pageFinishedAt == 0L) session.pageFinishedAt = System.currentTimeMillis()
                // 文档就绪后重新注入：SPA 页面跳转会清掉早先注入的钩子；
                // 抖音/快手视频数据由页面 JS 在 onPageFinished 后才 XHR/fetch 拉取
                injectJs(view, "web_inject.js")
                if (source == "douyin") injectJs(view, "abogus.js")
                // 稍候片刻等 SPA 水合发出首个数据请求，再开始轮询提取
                // ⚠️ 必须 mainHandler.postDelayed 而非 view.postDelayed：本 WebView 是离屏
                // 创建（从不 addView 到 window），未 attach 时 View.postDelayed 的消息进
                // HandlerActionQueue 永不执行——pollExtract 从未运行，后台解析只能干等 60s
                // 超时（真机/模拟器快手取证 2026-09-05 实锤）。
                mainHandler.postDelayed({
                    if (!session.done) {
                        pollExtract(wv, session, pageUrl, source, onDone, PAGE_READY_DELAY_MS)
                    }
                }, PAGE_READY_DELAY_MS)
            }

            /** 主文档加载失败（域名不可达等）：记硬失败原因，供失败归因（v3.2.5）。 */
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    session.hardFail = "主文档加载失败: ${error?.description ?: error?.errorCode ?: "未知"}"
                    Log.w(TAG, "onReceivedError(main): ${error?.description}")
                }
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
                // v3.2.5：归因到「被系统回收(OOM)」——2026-10-16 模拟器实测 lowmemorykiller 杀
                // webview_service 后旧代码不留任何痕迹，上层只看到一句「WebView 未取到媒体」
                session.hardFail = if (detail?.didCrash() == true) {
                    "WebView 渲染进程崩溃"
                } else {
                    "WebView 渲染进程被系统回收（设备内存不足）"
                }
                // 与 pollExtract 同理：不预置 session.done，由 parse() 完成 lambda 统一置位，
                // 否则 resume 永不执行 → parse() 永久挂起（完成态所有权问题，2026-09-05 实锤）
                onDone(null)
                runCatching { view?.stopLoading() }
                runCatching { view?.destroy() }
                if (webView === view) webView = null
                return true
            }
        }
    }

    /**
     * 轮询提取：每 [POLL_INTERVAL_MS] 主动 evaluate 对应平台的 extractor JS，
     * 解析 {urls, image_urls, content}，与 bridge 捕获（web_inject.js 的 onVideoUrl/onImageUrl）
     * 合并；任一非空即结束。
     *
     * **回调丢失免疫（真机 2026-09-05 快手取证修复）**：页面 SPA 跳转/水合竞态时
     * evaluateJavascript 的回调可能既不抛异常也永不返回——旧实现「未命中才在回调里递归」
     * 会就此断链干等总超时。现改为：回调只负责「命中即提前结束」，定时器无条件排下一次
     * evaluate（session.done 守卫跳过），断链不再致命。
     */
    private fun pollExtract(
        wv: WebView,
        session: Session,
        pageUrl: String,
        source: String,
        onDone: (Result?) -> Unit,
        delayMs: Long
    ) {
        Log.d(TAG, "pollExtract 进入, done=${session.done}")
        if (session.done) return
        mainHandler.postDelayed({
            if (session.done) return@postDelayed
            // 无候选早退（v3.2.5）：页面就绪后连续 EARLY_GIVEUP_AFTER_READY_MS 仍未见任何候选，
            // 基本可判定是风控空页/登录墙，没必要让用户干等到 TOTAL_TIMEOUT_MS(60s)。
            // ⚠️ 只要见到过候选就不早退 —— 那属于「提取慢」，继续等才有价值。
            val readyAt = session.pageFinishedAt
            if (readyAt > 0L && !session.sawAnyCandidate &&
                System.currentTimeMillis() - readyAt >= EARLY_GIVEUP_AFTER_READY_MS
            ) {
                lastFailReason = buildFailReason(session)
                Log.w(TAG, "无候选早退(${EARLY_GIVEUP_AFTER_READY_MS}ms): url=$pageUrl reason=$lastFailReason")
                onDone(null)
                return@postDelayed
            }
            val jsName = when (source) {
                "kuaishou" -> "kuaishou_extractor.js"
                "xhs" -> XHS_NOTE_EXTRACTOR_ASSET
                else -> "douyin_extractor.js"
            }
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
                    // 快手滑动流（subBiz=BROWSE_SLIDE_PHOTO）落地页会自动连播跳到其它作品：
                    // 直链 clientCacheKey={photoId}_b.mp4 含原作品 id，按 id 过滤锁定原作品，
                    // 避免「点了 A 下了 B」。弹性回退：若 WebView 仍停在目标作品页（未跳走），
                    // 说明直链池就是目标页产出的（部分 CDN 直链不含 clientCacheKey），
                    // 整池可信直接用；仅当页面已跳走且池里没有目标 id 时才继续等，宁缺毋滥。
                    val lockedVideos = if (source == "kuaishou") {
                        val targetId = Regex("""/(?:short-video|photo|fw/photo)/([A-Za-z0-9_-]+)""")
                            .find(pageUrl)?.groupValues?.getOrNull(1)
                        when {
                            targetId.isNullOrBlank() -> videos
                            else -> {
                                val hit = videos.filter { it.contains(targetId) }
                                val onPage = runCatching { wv.url?.contains(targetId) == true }.getOrDefault(false)
                                when {
                                    hit.isNotEmpty() -> hit
                                    onPage && videos.isNotEmpty() -> {
                                        Log.i(TAG, "photoId=$targetId 未在直链中出现，但页面未跳走，采用整池(${videos.size})")
                                        videos
                                    }
                                    else -> emptyList()
                                }
                            }
                        }
                    } else videos
                    // ⚠️ 这里只调用 onDone，不预置 session.done：完成态（done/activeSession/
                    // resume）统一由 parse() 传入的完成 lambda 置位。若在此先置 done=true，
                    // lambda 的 if (!session.done) 恒假 → cont.resume 永不执行 → parse() 永久
                    // 挂起且 Mutex 永久占用（真机/模拟器快手取证 2026-09-05 实锤：提取成功已
                    // 打日志，但下游既无「解析成功」也无失败记录，任务卡 DOWNLOADING）。
                    if (videos.isNotEmpty() || images.isNotEmpty()) session.sawAnyCandidate = true
                    if (lockedVideos.isNotEmpty() || images.isNotEmpty()) {
                        Log.i(TAG, "提取成功 source=$source videos=${lockedVideos.size} images=${images.size} title=${title.take(40)}")
                        onDone(Result(lockedVideos, images, contentText, title, pageUrl))
                    }
                    // 未命中：不在这里递归——定时器在下方无条件排下一次，回调丢失不再断链
                }
            }.onFailure { e ->
                // evaluateJavascript 偶发在页面导航窗口抛异常：下个 tick 重试即可
                Log.d(TAG, "evaluate extractor 失败，稍后重试: ${e.message}")
            }
            // 无条件排下一次轮询（命中时上方已置 session.done，下一次 tick 自动跳过）
            pollExtract(wv, session, pageUrl, source, onDone, POLL_INTERVAL_MS)
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
        /** 小红书移动 H5 试探用（模拟手机 Chrome 打开分享落地页）。 */
        const val XHS_MOBILE_UA = "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        /** onPageFinished 后开始轮询提取的等待时长（等 SPA 水合发出首个数据请求）。 */
        private const val PAGE_READY_DELAY_MS = 1200L
        /** 无结果时的轮询间隔。 */
        private const val POLL_INTERVAL_MS = 800L
        /** 单次解析总超时（含页面加载 + 轮询窗口）。 */
        private const val TOTAL_TIMEOUT_MS = 60_000L
        /**
         * 页面就绪后「始终零候选」的早退阈值（v3.2.5）：
         * 风控空页/登录墙常年零候选，继续等只白耗用户时间；一旦见到过候选则不再早退。
         */
        private const val EARLY_GIVEUP_AFTER_READY_MS = 25_000L
        /** warmup：onPageFinished 后等 JS 种 cookie 的窗口（React 水合 + 风控壳，实证 ~5s 够）。 */
        private const val WARMUP_SETTLE_MS = 6_000L
        /** warmup 总超时兜底（页面卡死/无 onPageFinished 时也返回已有 cookie）。 */
        private const val WARMUP_TOTAL_TIMEOUT_MS = 20_000L
        /** warmup 按 cookie 内容判定完成时的轮询间隔。 */
        private const val WARMUP_POLL_MS = 400L
        /**
         * 主页 post API 需要的最小 cookie 集。
         *
         * **实测结论（CDP cookie 子集对照实验，详见方案文档 §1.7）：唯一必需的是 `ttwid`。**
         *   全量 28 个 → 21 条；去 __ac_signature → 21 条；去 bd_ticket_guard_client_data → 21 条；
         *   **仅 ttwid 一个 → 21 条**；去掉 ttwid → 空 body。
         * 早期误把 __ac_signature / bd_ticket_guard_client_data 也当必需，导致预热在模拟器上
         * 空等满预算（那两者在部分机型/环境下永不出现在 CookieManager 中）。
         */
        val POST_API_REQUIRED_COOKIES = listOf("ttwid")
        /** 主页预热预算（慢机软渲染下用户主页水合 + secsdk 执行约需 8~18s）。 */
        const val POST_WARMUP_BUDGET_MS = 28_000L

        /** L2a 主页嗅探脚本资产名。 */
        const val SNIFF_ASSET = "douyin_home_sniff.js"

        /** L2x-1 小红书主页嗅探脚本资产名（user_posted / user/other 旁路 hook）。 */
        const val XHS_SNIFF_ASSET = "xhs_home_sniff.js"

        /** 小红书单条笔记详情提取脚本（explore 页 __INITIAL_STATE__，输出契约同 douyin_extractor）。 */
        const val XHS_NOTE_EXTRACTOR_ASSET = "xhs_note_extractor.js"

        /** 小红书嗅探：连续无新增条目的收口静默窗（等滚动 cursor 翻页落地）。 */
        private const val XHS_PAGE_QUIET_MS = 6_000L

        /**
         * 桌面环境自洽补丁资产（见 assets/douyin_desktop_env.js 头部说明）。
         *
         * 抖音 post 接口交叉校验「UA 声明的平台」与 `navigator.platform`；本 App 的
         * WebView 用 [DESKTOP_UA]（Windows）但 `navigator.platform` 仍是 Android 的
         * Linux 值 → 服务端返回 **HTTP 200 + 空 body**（静默拒绝，非 403）。
         * 必须在页面主 JS 之前注入，把 platform / 触摸 / UA-CH / 屏幕 统一成桌面身份。
         */
        const val DESKTOP_ENV_ASSET = "douyin_desktop_env.js"
        /** L2a 嗅探轮询间隔。 */
        private const val SNIFF_POLL_MS = 500L
        /** L2a 嗅探总预算（页面加载 + SPA 水合 + 首个 post 请求）。 */
        /** 嗅探总预算：小红书页面冷加载慢（未登录+无缓存实测 35s 内请求未发出），放宽到 90s。 */
        private const val SNIFF_BUDGET_MS = 90_000L
        /** L2a 离屏 WebView 视口尺寸（桌面级，保证 SPA 正常布局渲染）。 */
        private const val SNIFF_VIEW_W = 1440
        private const val SNIFF_VIEW_H = 900
        /** 首条作品落地后，额外等作者档案接口（aweme_count）的收口窗口。 */
        private const val PROFILE_GRACE_MS = 2_500L
    }
}
