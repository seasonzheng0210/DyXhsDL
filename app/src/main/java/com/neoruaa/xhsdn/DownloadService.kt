package com.neoruaa.xhsdn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.neoruaa.xhsdn.data.NoteType
import com.neoruaa.xhsdn.data.TaskManager
import com.neoruaa.xhsdn.data.TaskStatus
import com.neoruaa.xhsdn.douyin.DouyinMediaInfo
import com.neoruaa.xhsdn.douyin.DouyinMediaType
import com.neoruaa.xhsdn.douyin.DouyinParser
import com.neoruaa.xhsdn.kuaishou.KuaishouMediaInfo
import com.neoruaa.xhsdn.kuaishou.KuaishouMediaType
import com.neoruaa.xhsdn.kuaishou.KuaishouParser
import com.neoruaa.xhsdn.utils.DownloadLogger
import com.neoruaa.xhsdn.utils.EventTracker
import com.neoruaa.xhsdn.utils.UrlUtils
import com.neoruaa.xhsdn.web.BgWebViewParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * 前台下载服务：把下载放到 Service 进程优先级里执行，
 * 这样切到后台 / 锁屏时系统不会像对待后台缓存进程那样收紧网络或杀掉下载。
 *
 * MainActivity 的所有下载入口（剪贴板自动读、点按、手动输入、WebView 返回）
 * 都改为向本服务发 Intent，由本服务复用 DouyinParser / XHSDownloader / FileDownloader
 * 完成解析与下载，并通过 TaskManager 持久化任务状态（回到 App 进度照常显示）。
 */
class DownloadService : Service() {

    /** HTTP 快解结果：value=媒体数据（成功），error=失败末因（供失败文案精确定位，非 null 表示失败）。 */
    private data class HttpOutcome<T>(val value: T?, val error: String?)

    // 视频直链下载用的移动 UA（iPhone），对抖音系/第三方直链最稳
    private val DIRECT_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"
    // 快手 CDN（kwaicdn.com）需带快手 Referer，否则返回 403
    private val KUAISHOU_REFERER = "https://www.kuaishou.com/"

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val activeUrls = ConcurrentHashMap.newKeySet<String>()

    /**
     * 后台不可见 WebView 解析器（懒加载，首个解析任务发起时才创建）：
     * HTTP 直解被风控打挂后的兜底引擎，在 Service 内静默解析抖音/快手作品页，
     * 复用 app_webview 持久化登录态 Cookie，不弹任何 Activity（无黑窗）。
     */
    private val bgParser by lazy { BgWebViewParser(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        TaskManager.init(applicationContext)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 立即提升为前台服务，避免切后台被打断
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.fg_service_running), ""))

        // 取消任务的请求
        if (intent?.action == ACTION_STOP) {
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
            if (taskId > 0) {
                activeJobs.remove(taskId)?.cancel()
                TaskManager.completeTask(taskId, false, getString(R.string.download_cancelled_by_user))
                updateNotification(getString(R.string.download_cancelled_notification_title),
                    getString(R.string.user_manually_stopped), false)
                maybeStop()
            }
            return START_NOT_STICKY
        }

        val url = intent?.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            // 没有任务可跑，安全地停掉自己
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val source = intent.getStringExtra(EXTRA_SOURCE) ?: UrlUtils.detectPlatform(url) ?: "xhs"
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_NORMAL
        val taskIdExtra = intent.getLongExtra(EXTRA_TASK_ID, -1L).takeIf { it > 0 }

        // 中立的「视频直链」短路：任何视频直链（.mp4、aweme.snssdk.com 播放接口、
        // douyinvod.com 等）都不走平台解析器，直接下载跟随 302 的本体。
        // 这样用户粘贴的直链、WebView 提取出的第三方视频直链都能下，且不会误归属到抖音/其他平台。
        if (isDirectVideoUrl(url)) {
            downloadDirectVideo(url, taskIdExtra)
            return START_NOT_STICKY
        }

        when (mode) {
            MODE_WEBCRAWL -> {
                val urls = intent.getStringArrayListExtra(EXTRA_URLS) ?: emptyList()
                val content = intent.getStringExtra(EXTRA_WEB_CONTENT)
                startWebCrawl(urls, content, taskIdExtra)
            }
            MODE_DOUYIN_IMAGES -> {
                // 抖音图文/图集帖：WebView 兜底路径已提取出帖子图集图片，逐张下载
                val urls = intent.getStringArrayListExtra(EXTRA_URLS) ?: emptyList()
                val pageUrl = intent.getStringExtra(EXTRA_URL)
                startDownloadDouyinImagesInternal(urls, pageUrl, taskIdExtra)
            }
            MODE_DOUYIN_HOME -> {
                // 抖音主页批量：WebView 主页爬取收集到全部 /video/{id} 页链接，
                // 逐条后台解析（HTTP 快解 → 后台 WebView 兜底）后同任务卡批量下载
                val urls = intent.getStringArrayListExtra(EXTRA_URLS) ?: emptyList()
                val pageUrl = intent.getStringExtra(EXTRA_URL)
                startDouyinHomeBatch(urls, pageUrl, taskIdExtra)
            }
            else -> when (source) {
                "douyin" -> {
                    EventTracker.track(this, "download_requested", mapOf("source" to "douyin", "mode" to mode))
                    startDouyin(url, mode, taskIdExtra)
                }
                "kuaishou" -> {
                    EventTracker.track(this, "download_requested", mapOf("source" to "kuaishou", "mode" to mode))
                    startKuaishou(url, mode, taskIdExtra)
                }
                else -> {
                    EventTracker.track(this, "download_requested", mapOf("source" to "xhs", "mode" to mode))
                    startXhs(url, taskIdExtra)
                }
            }
        }

        return START_NOT_STICKY
    }

    /** 视频直链判定：命中即走中立的 downloadDirectVideo，不进任何平台解析器。 */
    private fun isDirectVideoUrl(url: String): Boolean {
        val u = url.lowercase()
        if (u.contains(".mp4") || u.contains(".mov") || u.contains(".m4v") || u.contains(".webm")) return true
        if (u.contains("aweme.snssdk.com/aweme/v1/play/")) return true
        if (u.contains("douyinvod.com")) return true
        // 抖音/快手系常见视频 CDN：WebView 提取出的播放直链都应直接下载，而非二次喂给已失效的 HTTP 解析器
        if (u.contains("douyin") && (u.contains("bytecdn") || u.contains("ib.douyin.com") || u.contains("tiktokcdn") || u.contains("douyinvod"))) return true
        if (u.contains("kuaishou") && (u.contains("kwaicdn") || u.contains("chenzhongtech") || u.contains("gifshow") || u.contains("kwai"))) return true
        return false
    }

        /**
         * 中立的「视频直链」下载：不归属任何平台解析器。
         * 用于用户粘贴的视频直链，或某平台 WebView 提取出的第三方视频直链
         * （如抖音系 aweme.snssdk.com 播放直链）。
         * 这类 URL 的 video_id 已是真实文件 id，直接下载（跟随 302）即得 mp4。
         */
    private fun downloadDirectVideo(rawUrl: String, taskIdExtra: Long?) {
        val targetUrl = UrlUtils.extractFirstUrl(rawUrl) ?: rawUrl
        if (!targetUrl.startsWith("http://", true) && !targetUrl.startsWith("https://", true)) {
            DownloadLogger.logFailure(this, "direct", rawUrl, "非有效视频直链: $rawUrl")
            TaskManager.createTask(targetUrl, null, NoteType.VIDEO, 1, source = "direct").also {
                TaskManager.startTask(it)
                TaskManager.completeTask(it, false, "非有效链接")
            }
            updateNotification(getString(R.string.download_failed_notification_title),
                getString(R.string.no_valid_link_found), false)
            return
        }

        if (!activeUrls.add(targetUrl)) {
            updateNotification(getString(R.string.downloading_files), "该链接正在下载中，已忽略重复", false)
            return
        }

        scope.launch {
            var myTaskId: Long = taskIdExtra ?: -1L
            try {
                if (myTaskId < 0) {
                    val plan = TaskManager.planDownloadTask(targetUrl)
                    if (plan.ignore) return@launch
                    myTaskId = plan.taskId?.also { TaskManager.resetTask(it) }
                        ?: TaskManager.createTask(targetUrl, null, NoteType.VIDEO, 1, source = "direct").also {
                            TaskManager.startTask(it)
                        }
                }
                activeJobs[myTaskId] = coroutineContext[Job]!!

                updateNotification(getString(R.string.downloading_files), "视频直链下载中…", true)

                // 视频直链：按 host 选择 Referer/UA。抖音/快手 CDN 需要带对应站点 Referer，
                // 否则常返回 403；抖音还要 Chrome 移动 UA（与 DouyinDL 一致）。
                val lowUrl = targetUrl.lowercase()
                val (referer, ua) = when {
                    lowUrl.contains("douyin") -> DouyinParser.REFERER to DouyinParser.MOBILE_UA
                    lowUrl.contains("kuaishou") -> "https://www.kuaishou.com/" to KuaishouParser.MOBILE_UA
                    else -> null to DIRECT_UA
                }

                // 解析最终直链（跟随 snssdk/douyinvod 等的 302），避免 OkHttp 透明跨 host 重定向
                // 在部分安卓网络栈下失效。解析失败则回退原 URL。
                val resolved = resolveFinalUrl(targetUrl, ua, referer) ?: targetUrl

                // 包装回调：同时驱动 TaskManager 进度，并记录真实失败原因用于诊断
                val baseCb = createCallback(myTaskId)
                var lastErr = ""
                val diagCb = object : DownloadCallback {
                    override fun onFileDownloaded(path: String) = baseCb.onFileDownloaded(path)
                    override fun onDownloadError(status: String, originalUrl: String) {
                        lastErr = status
                        baseCb.onDownloadError(status, originalUrl)
                    }
                    override fun onDownloadProgress(status: String) = baseCb.onDownloadProgress(status)
                    override fun onDownloadProgressUpdate(downloaded: Long, total: Long) =
                        baseCb.onDownloadProgressUpdate(downloaded, total)
                    override fun onVideoDetected() = baseCb.onVideoDetected()
                }
                val downloader = FileDownloader(this@DownloadService, diagCb)

                val ok = runCatching {
                    // 先用解析出的 CDN 直链下；失败重试一次（回退原 snssdk URL）
                    downloader.downloadFile(resolved, "direct_video_${System.currentTimeMillis()}.mp4", referer, ua)
                        || downloader.downloadFile(targetUrl, "direct_video_${System.currentTimeMillis()}.mp4", referer, ua)
                }.getOrElse { e ->
                    lastErr = "下载异常: ${e.message}"
                    false
                }
                if (ok) {
                    DownloadLogger.logInfo(this@DownloadService, "direct", targetUrl, "视频直链下载完成 (resolved=$resolved)")
                } else {
                    DownloadLogger.logFailure(this@DownloadService, "direct", targetUrl, "视频直链下载失败: ${lastErr.ifBlank { "返回false" }}")
                }
                TaskManager.completeTask(myTaskId, ok, if (ok) null else lastErr.ifBlank { "下载失败" })
                updateNotification(
                    if (ok) getString(R.string.download_completed_notification_title)
                    else getString(R.string.download_failed_notification_title),
                    if (ok) getString(R.string.download_completed_files_count, 1)
                    else getString(R.string.download_failed_check_network),
                    false
                )
            } catch (e: CancellationException) {
                // 用户手动停止：不写失败日志（ACTION_STOP 分支已置 FAILED），这里仅兜底保证终态
                if (myTaskId > 0) TaskManager.failIfActive(myTaskId, getString(R.string.download_cancelled_by_user))
            } catch (e: Exception) {
                Log.e(TAG, "direct video download error", e)
                if (myTaskId > 0) {
                    DownloadLogger.logFailure(this@DownloadService, "direct", targetUrl, "下载异常终止: ${e.message}")
                    TaskManager.failIfActive(myTaskId, "下载异常: ${e.message}")
                }
            } finally {
                activeUrls.remove(targetUrl)
                if (myTaskId > 0) activeJobs.remove(myTaskId)
                maybeStop()
            }
        }
    }

    /** 跟随重定向拿到最终直链（不读取响应体，仅取最终 URL）。失败返回 null。 */
    private fun resolveFinalUrl(url: String, ua: String, referer: String? = null): String? {
        return try {
            val builder = okhttp3.Request.Builder().url(url).header("User-Agent", ua)
            if (!referer.isNullOrBlank()) builder.header("Referer", referer)
            FileDownloader.getSharedHttpClient().newCall(builder.build()).execute().use { resp ->
                resp.request.url.toString()
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveFinalUrl failed for $url: ${e.message}")
            null
        }
    }

    override fun onDestroy() {
        scope.cancel()
        // 销毁后台 WebView（懒加载字段：仅当初始化过才真正销毁）
        runCatching { bgParser.destroy() }
        super.onDestroy()
    }

    // region 抖音下载
    private fun startDouyin(rawUrl: String, mode: String, taskIdExtra: Long?) {
        val targetUrl = UrlUtils.extractFirstUrl(rawUrl) ?: rawUrl
        if (!targetUrl.startsWith("http://", true) && !targetUrl.startsWith("https://", true)) {
            DownloadLogger.logFailure(this, "douyin", rawUrl, "无法从剪贴板提取有效链接: $rawUrl")
            TaskManager.createTask(targetUrl, null, NoteType.VIDEO, 1, source = "douyin").also {
                TaskManager.startTask(it)
                TaskManager.completeTask(it, false, "无法提取有效链接")
            }
            updateNotification(getString(R.string.download_failed_notification_title),
                getString(R.string.no_valid_link_found), false)
            return
        }

        // WebView 提取出的 CDN 直链（aweme.snssdk.com/douyinvod/bytecdn 等）：直接下载，
        // 绝不再喂给已被风控打死的 HTTP 直解解析器（否则秒挂 End of input）
        if (isDirectVideoUrl(targetUrl)) {
            downloadDirectVideo(targetUrl, taskIdExtra)
            return
        }

        if (!activeUrls.add(targetUrl)) {
            updateNotification(getString(R.string.downloading_files), "该链接正在下载中，已忽略重复", false)
            return
        }

        scope.launch {
            var myTaskId: Long = taskIdExtra ?: -1L
            try {
                if (myTaskId < 0) {
                    val plan = TaskManager.planDownloadTask(targetUrl)
                    // 查重（复用同一条任务）：同链接正在下载 → 忽略本次触发
                    if (plan.ignore) {
                        updateNotification(getString(R.string.downloading_files), "该链接正在下载中，已忽略重复", false)
                        return@launch
                    }
                    // 同链接已有完成/失败记录 → 原地重置复用（保持单条记录）；无记录 → 新建
                    myTaskId = plan.taskId?.also { TaskManager.resetTask(it) }
                        ?: TaskManager.createTask(
                            targetUrl, null, NoteType.VIDEO, 1, source = "douyin"
                        ).also { TaskManager.startTask(it) }
                }
                activeJobs[myTaskId] = coroutineContext[Job]!!

                updateNotification(getString(R.string.downloading_files), "抖音解析中…", true)

                // 解析：HTTP 直解（10s 快解）失败 → 后台不可见 WebView 真浏览器兜底
                //（复用登录态 Cookie，不弹 Activity / 无黑窗）。两条路都拿不到媒体才算失败。
                val (info, failReason) = resolveDouyinMediaInfo(targetUrl)
                if (info == null) {
                    // 失败文案携带各环节末因（HTTP 快解/预热/WebView），用户复制日志即可精确定位
                    val fullReason = failReason.ifBlank { "未知原因" }
                    EventTracker.track(this@DownloadService, "parse_fail", mapOf("source" to "douyin", "reason" to fullReason))
                    DownloadLogger.logFailure(this@DownloadService, "douyin", targetUrl, "解析失败: $fullReason")
                    TaskManager.completeTask(myTaskId, false, "解析失败: ${fullReason.take(60)}")
                    updateNotification(getString(R.string.download_failed_notification_title),
                        "抖音解析失败", false)
                    return@launch
                }
                EventTracker.track(this@DownloadService, "parse_success", mapOf(
                    "source" to "douyin",
                    "type" to (if (info.type == DouyinMediaType.IMAGE) "image" else "video"),
                    "n" to (if (info.type == DouyinMediaType.IMAGE) info.imageUrls.size.toString() else "1")
                ))

                // 图集/图文：更新任务类型为图片，文件总数为图片数量
                if (info.type == DouyinMediaType.IMAGE) {
                    TaskManager.updateTask(myTaskId) { t ->
                        t.copy(noteType = NoteType.IMAGE, totalFiles = info.imageUrls.size)
                    }
                }

                updateNotification(getString(R.string.downloading_files), info.title, true)
                val mediaDesc = if (info.type == DouyinMediaType.IMAGE) "${info.imageUrls.size} 张图" else "视频"
                DownloadLogger.logInfo(this@DownloadService, "douyin", targetUrl, "解析成功: ${info.title} ($mediaDesc)")

                val downloader = FileDownloader(this@DownloadService, createCallback(myTaskId))
                val success = if (info.type == DouyinMediaType.IMAGE) {
                    downloadImages(downloader, info, myTaskId)
                } else {
                    val fileName = "${info.title}.mp4"
                    runCatching {
                        // 抖音 play_addr 直链（365yg / api-play 等 CDN）需带抖音 Referer，否则常 403
                        downloader.downloadFile(info.videoUrl, fileName, DouyinParser.REFERER, info.userAgent)
                    }.getOrElse { e ->
                        DownloadLogger.logFailure(this@DownloadService, "douyin", info.videoUrl ?: targetUrl, "下载异常: ${e.message}")
                        false
                    }.also { ok ->
                        if (!ok) {
                            DownloadLogger.logFailure(this@DownloadService, "douyin", info.videoUrl ?: targetUrl, "下载失败(返回false)")
                        }
                    }
                }

                EventTracker.track(this@DownloadService, "download_done", mapOf(
                    "source" to "douyin",
                    "ok" to success.toString(),
                    "type" to (if (info.type == DouyinMediaType.IMAGE) "image" else "video")
                ))
                if (success) {
                    DownloadLogger.logInfo(this@DownloadService, "douyin", targetUrl, "下载完成(成功): ${info.title}")
                }

                TaskManager.completeTask(myTaskId, success,
                    if (success) null else "下载失败")
                updateNotification(
                    if (success) getString(R.string.download_completed_notification_title)
                    else getString(R.string.download_failed_notification_title),
                    if (success) info.title else getString(R.string.download_failed_check_network),
                    false
                )
            } catch (e: CancellationException) {
                // 用户手动停止：不写失败日志，兜底保证终态
                if (myTaskId > 0) TaskManager.failIfActive(myTaskId, getString(R.string.download_cancelled_by_user))
            } catch (e: Exception) {
                Log.e(TAG, "douyin download error", e)
                // 关键修复：任何未预期异常都必须把任务置为 FAILED + 落失败日志，
                // 否则任务会永远停在 DOWNLOADING（UI 表现为「卡在停止」且无失败记录）。
                if (myTaskId > 0) {
                    DownloadLogger.logFailure(this@DownloadService, "douyin", targetUrl, "下载异常终止: ${e.message}")
                    TaskManager.failIfActive(myTaskId, "下载异常: ${e.message}")
                }
            } finally {
                activeUrls.remove(targetUrl)
                if (myTaskId > 0) activeJobs.remove(myTaskId)
                maybeStop()
            }
        }
    }
    // endregion

    // region 抖音图集下载
    /**
     * 逐张下载图集图片，复用 createCallback 统计进度。
     * 全部成功返回 true，任一张失败返回 false（失败项已落日志）。
     */
    private fun downloadImages(downloader: FileDownloader, info: DouyinMediaInfo, taskId: Long): Boolean {
        var okAll = true
        info.imageUrls.forEachIndexed { index, url ->
            val ext = DouyinParser.mediaExtension(url)
            val fileName = "${info.title}_${index + 1}.$ext"
            val ok = runCatching {
                // 抖音图床（douyinpic.com）必须带抖音 Referer，否则返回 403 导致图片下载失败
                downloader.downloadFile(url, fileName, DouyinParser.REFERER, info.userAgent)
            }.getOrElse { e ->
                DownloadLogger.logFailure(this@DownloadService, "douyin", url, "下载图片异常: ${e.message}")
                false
            }
            if (!ok) {
                okAll = false
                DownloadLogger.logFailure(this@DownloadService, "douyin", url, "图片下载失败(返回false)")
            }
        }
        return okAll
    }
    // endregion

    // region 抖音/快手解析工具（HTTP 快解 + 后台 WebView 兜底）

    /**
     * 抖音解析主入口：先 HTTP 快解（10s 上限；抖音已全站风控，多数场景秒失败返回空），
     * 失败先尝试 **Cookie 懒预热**（note 图文 HTTP 全失败多为 Argus 403 缺 UIFID 指纹 cookie；
     * 预热让后台 WebView 种 cookie 进全局 CookieManager → HTTP 重试时 detail API ② 携新 cookie
     * 直解图文，绕开页面渲染/登录墙），仍失败再转后台不可见 WebView 真浏览器兜底
     * （复用登录态 Cookie、不弹 Activity）。三条路都拿不到媒体返回 null。
     */
    /**
     * 抖音解析主入口。返回 (媒体信息, 失败原因)：成功时 info 非 null、reason 空串；
     * 失败时 info 为 null、reason 携带各失败环节末因（HTTP 快解 → 预热 → 后台 WebView），
     * 供上层失败文案精确定位，用户复制日志即可诊断。
     */
    private suspend fun resolveDouyinMediaInfo(targetUrl: String): Pair<DouyinMediaInfo?, String> {
        val reasons = mutableListOf<String>()

        val http = fastHttpResolve { DouyinParser.parse(targetUrl) }
        if (http.value != null) return http.value to ""
        http.error?.let { reasons += it }

        // Cookie 懒预热 + HTTP 重试（v1.12.0 note 免登录方案 P2）
        val warmed = warmupDouyinCookie(targetUrl)
        if (warmed) {
            reasons += "预热成功仍失败"
            val retry = fastHttpResolve { DouyinParser.parse(targetUrl) }
            if (retry.value != null) return retry.value to ""
            retry.error?.let { reasons += "预热后: $it" }
        } else {
            reasons += "Cookie 预热未触发/失败(跳过重试)"
        }

        reasons += "转后台 WebView 兜底"
        DownloadLogger.logInfo(this@DownloadService, "douyin", targetUrl, "HTTP 直解未取到媒体，转后台 WebView 解析…")
        val bg = bgParser.parse(targetUrl, "douyin")
        if (bg == null) {
            reasons += "WebView 未取到媒体"
            return null to reasons.joinToString("; ")
        }
        val id = extractDouyinId(targetUrl)
        val safeTitle = safeFileName(bg.title.ifBlank { "douyin_$id" })
        val info = when {
            bg.imageUrls.isNotEmpty() -> DouyinMediaInfo(
                DouyinMediaType.IMAGE, safeTitle, null, bg.imageUrls, null, id, DouyinParser.MOBILE_UA
            )
            else -> bg.videoUrls.firstOrNull { it.startsWith("http") }?.let { v ->
                DouyinMediaInfo(DouyinMediaType.VIDEO, safeTitle, v, emptyList(), null, id, DouyinParser.MOBILE_UA)
            }
        }
        if (info == null) reasons += "WebView 有数据但无可用媒体"
        return info to if (info == null) reasons.joinToString("; ") else ""
    }

    /**
     * Cookie 懒预热：用后台 WebView 加载目标作品页（video/note 与链接类型一致），
     * 让抖音风控壳 JS 执行种下 UIFID_TEMP 等指纹 cookie。成功后返回 true——cookie 已进全局
     * CookieManager，随后 DouyinParser.fetchWebDetail 重新快照即得；预热失败（WebView 崩/超时）
     * 返回 false 不阻塞，直接降级后台 WebView 兜底。
     */
    private suspend fun warmupDouyinCookie(targetUrl: String): Boolean {
        return try {
            val id = extractDouyinId(targetUrl)
            if (id.isBlank() || id == "dy") return false
            val warmUrl = if (targetUrl.contains("note", ignoreCase = true)) {
                "https://www.douyin.com/note/$id"
            } else {
                "https://www.douyin.com/video/$id"
            }
            DownloadLogger.logInfo(this@DownloadService, "douyin", targetUrl, "Cookie 预热(懒): $warmUrl …")
            val cookie = bgParser.warmupAndSnapshot(warmUrl)
            if (cookie.isNullOrBlank()) {
                DownloadLogger.logInfo(this@DownloadService, "douyin", targetUrl, "Cookie 预热未取到 cookie，跳过重试")
                false
            } else {
                DownloadLogger.logInfo(this@DownloadService, "douyin", targetUrl, "Cookie 预热成功(${cookie.length}字符)")
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cookie 预热异常: ${e.message}")
            false
        }
    }

    /**
     * 快手解析主入口：先 HTTP GraphQL 快解（10s 上限），失败转后台 WebView 兜底。
     */
    /**
     * 快手解析主入口：先 HTTP GraphQL 快解（10s 上限），失败转后台 WebView 兜底。
     * 返回 (媒体信息, 失败原因)——失败时 reason 携带各环节末因供文案精确定位。
     */
    private suspend fun resolveKuaishouMediaInfo(targetUrl: String): Pair<KuaishouMediaInfo?, String> {
        val reasons = mutableListOf<String>()
        val http = fastHttpResolve { KuaishouParser.parse(targetUrl) }
        if (http.value != null) return http.value to ""
        http.error?.let { reasons += it }

        reasons += "转后台 WebView 兜底"
        DownloadLogger.logInfo(this@DownloadService, "kuaishou", targetUrl, "HTTP 直解未取到媒体，转后台 WebView 解析…")
        val bg = bgParser.parse(targetUrl, "kuaishou")
        if (bg == null) {
            reasons += "WebView 未取到媒体"
            return null to reasons.joinToString("; ")
        }
        val id = targetUrl.substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() } ?: "ks"
        val safeTitle = safeFileName(bg.title.ifBlank { "kuaishou_$id" })
        val info = when {
            bg.imageUrls.isNotEmpty() -> KuaishouMediaInfo(
                KuaishouMediaType.IMAGE, safeTitle, null, bg.imageUrls, null, id, KuaishouParser.MOBILE_UA
            )
            else -> bg.videoUrls.firstOrNull { it.startsWith("http") }?.let { v ->
                KuaishouMediaInfo(KuaishouMediaType.VIDEO, safeTitle, v, emptyList(), null, id, KuaishouParser.MOBILE_UA)
            }
        }
        if (info == null) reasons += "WebView 有数据但无可用媒体"
        return info to if (info == null) reasons.joinToString("; ") else ""
    }

    /**
     * HTTP 快解：给平台 Parser 一个短预算，避免被风控的空响应/慢连接拖住几十秒才轮到
     * 后台 WebView 兜底。返回 [HttpOutcome]——成功 value 非 null；失败 error 携带末因。
     * 协程取消(CancellationException)与主页分享链接异常(DouyinHomepageLinkException)原样上抛。
     */
    private suspend fun <T> fastHttpResolve(block: suspend () -> T): HttpOutcome<T> {
        return try {
            HttpOutcome(withTimeout(HTTP_FAST_TIMEOUT_MS) { block() }, null)
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "HTTP 直解超时(${HTTP_FAST_TIMEOUT_MS}ms)，转后台 WebView")
            HttpOutcome(null, "HTTP 直解超时(${HTTP_FAST_TIMEOUT_MS / 1000}s)")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 主页分享短链（302 → share/user）不是单作品：不做降级，直接上抛给用户明确指引
            if (e is com.neoruaa.xhsdn.douyin.DouyinHomepageLinkException) throw e
            Log.w(TAG, "HTTP 直解失败: ${e.message}")
            HttpOutcome(null, e.message ?: "HTTP 直解未知异常")
        }
    }

    private fun extractDouyinId(url: String): String {
        Regex("""(?:video|note)/(\d+)""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("""modal_id=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        return url.substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() } ?: "dy"
    }

    /** 清洗成安全文件名（文件名非法字符替换、限长 80）。 */
    private fun safeFileName(raw: String): String = raw
        .replace(Regex("""[\\/:*?"<>|#\n\r]"""), "_")
        .trim(' ', '.')
        .take(80)
        .ifBlank { "download" }

    // endregion

    // region 抖音主页批量下载（后台逐条解析 + 同任务卡下载）
    /**
     * 抖音作者主页批量下载：MainActivity 的 WebView 主页爬取收集到全部 /video/{id} 页链接后，
     * 逐条在后台解析（HTTP 快解 → 后台 WebView 兜底），每条解析成功即下载进同一批任务卡。
     * 修复 v1.10.11「主页视频解析失败」——此前把 /video/{id} 列表拿回 MainActivity 再走已被
     * 风控打死的 HTTP DouyinParser.parse，必然全空。
     */
    private fun startDouyinHomeBatch(videoPageUrls: List<String>, homepageUrl: String?, taskIdExtra: Long?) {
        val pageUrls = videoPageUrls.distinct().filter { it.contains("/video/") || it.contains("/note/") }
        if (pageUrls.isEmpty()) {
            updateNotification(getString(R.string.download_failed_notification_title),
                getString(R.string.no_valid_link_found), false)
            return
        }
        val batchKey = homepageUrl?.takeIf { it.isNotBlank() } ?: pageUrls.first()
        if (!activeUrls.add(batchKey)) return

        scope.launch {
            var myTaskId: Long = taskIdExtra ?: -1L
            try {
                if (myTaskId < 0) {
                    myTaskId = TaskManager.createTask(
                        batchKey, "主页视频(${pageUrls.size})", NoteType.VIDEO, pageUrls.size, source = "douyin"
                    ).also { TaskManager.startTask(it) }
                }
                activeJobs[myTaskId] = coroutineContext[Job]!!

                updateNotification(getString(R.string.downloading_files), "主页视频 解析中(0/${pageUrls.size})…", true)

                val completed = AtomicInteger(0)
                val failed = AtomicInteger(0)
                val myJob = coroutineContext[Job]
                // 进度回调只负责登记文件路径；条数进度由循环内显式 updateProgress 驱动
                //（条数=URL 数；图集帖多张图片全部成功也只算 1 条，避免 completed 超 totalFiles）
                val cb = object : DownloadCallback {
                    override fun onFileDownloaded(path: String) {
                        TaskManager.addFilePath(myTaskId, path)
                    }
                    override fun onDownloadError(status: String, originalUrl: String) {}
                    override fun onDownloadProgress(status: String) {}
                    override fun onDownloadProgressUpdate(downloaded: Long, total: Long) {}
                    override fun onVideoDetected() {}
                }
                val downloader = FileDownloader(this@DownloadService, cb)

                pageUrls.forEachIndexed { index, pageUrl ->
                    // 任务被用户取消时优雅跳过后续条目（协程挂起点也会自动抛取消异常兜底）
                    if (myJob?.isActive == false) return@forEachIndexed
                    updateNotification(getString(R.string.downloading_files),
                        "主页视频 解析+下载中(${index + 1}/${pageUrls.size})…", true)
                    val (info, hbReason) = resolveDouyinMediaInfo(pageUrl)
                    val ok = if (info == null) {
                        DownloadLogger.logFailure(this@DownloadService, "douyin", pageUrl, "主页批量：单条解析失败: ${hbReason.ifBlank { "未知" }}")
                        false
                    } else if (info.type == DouyinMediaType.IMAGE) {
                        // 图集帖：单条下载全部图片（文件名带序号前缀避免与其它帖撞名覆盖）
                        val prefixed = info.copy(title = "${index + 1}_${info.title}")
                        downloadImages(downloader, prefixed, myTaskId)
                    } else {
                        val fileName = "${index + 1}_${info.title}.mp4"
                        runCatching {
                            downloader.downloadFile(info.videoUrl, fileName, DouyinParser.REFERER, info.userAgent)
                        }.getOrElse { e ->
                            DownloadLogger.logFailure(this@DownloadService, "douyin",
                                info.videoUrl ?: pageUrl, "主页批量：单条下载异常: ${e.message}")
                            false
                        }
                    }
                    if (ok) {
                        completed.incrementAndGet()
                        DownloadLogger.logInfo(this@DownloadService, "douyin", pageUrl, "主页批量：单条完成")
                    } else {
                        failed.incrementAndGet()
                    }
                    TaskManager.updateProgress(myTaskId, completed.get(), failed.get(), 0f)
                }

                val success = completed.get() > 0 && failed.get() == 0
                TaskManager.completeTask(myTaskId, success,
                    if (success) null
                    else if (completed.get() > 0) "部分主页视频下载失败"
                    else "主页视频全部解析失败（可能需登录抖音）")
                updateNotification(
                    if (success) getString(R.string.download_completed_notification_title)
                    else getString(R.string.download_failed_notification_title),
                    if (success) getString(R.string.download_completed_files_count, completed.get())
                    else "主页视频：成功 ${completed.get()}，失败 ${failed.get()}",
                    false
                )
            } catch (e: CancellationException) {
                // 用户手动停止：不写失败日志，兜底保证终态
                if (myTaskId > 0) TaskManager.failIfActive(myTaskId, getString(R.string.download_cancelled_by_user))
            } catch (e: Exception) {
                Log.e(TAG, "douyin home batch error", e)
                if (myTaskId > 0) {
                    DownloadLogger.logFailure(this@DownloadService, "douyin", batchKey, "主页批量异常终止: ${e.message}")
                    TaskManager.failIfActive(myTaskId, "主页批量异常: ${e.message}")
                }
            } finally {
                activeUrls.remove(batchKey)
                if (myTaskId > 0) activeJobs.remove(myTaskId)
                maybeStop()
            }
        }
    }
    // endregion

    // region 抖音图文/图集下载（WebView 兜底路径）
    /**
     * 抖音图文/图集帖下载（WebView 兜底路径）：复用/预建 IMAGE 任务，逐张下载帖子图集图片。
     * 与 [startDouyin] 的 IMAGE 分支同构，区别在于这里直接拿到图片直链列表、无需再 HTTP 解析。
     */
    private fun startDownloadDouyinImagesInternal(imageUrls: List<String>, pageUrl: String?, taskIdExtra: Long?) {
        if (imageUrls.isEmpty()) return
        val targetTaskId = taskIdExtra ?: TaskManager.createTask(
            pageUrl ?: imageUrls.first(), null, NoteType.IMAGE, imageUrls.size, source = "douyin"
        ).also { TaskManager.startTask(it) }
        // 复用 WebView 预建任务时，把类型/总数/来源纠正为抖音图片（预建可能是 UNKNOWN + 视频图片合并计数，且来源默认 xhs）
        TaskManager.updateTask(targetTaskId) { t -> t.copy(noteType = NoteType.IMAGE, totalFiles = imageUrls.size, source = "douyin") }
        scope.launch {
            try {
                val rawTitle = pageUrl?.split("/")?.last()?.takeIf { it.isNotBlank() } ?: "douyin_image"
                val title = rawTitle.replace(Regex("""[\\/:*?"<>|#\n\r]"""), "_").take(80)
                val info = DouyinMediaInfo(DouyinMediaType.IMAGE, title, null, imageUrls, null, "", DouyinParser.MOBILE_UA)
                val downloader = FileDownloader(this@DownloadService, createCallback(targetTaskId))
                val ok = downloadImages(downloader, info, targetTaskId)
                TaskManager.completeTask(targetTaskId, ok, if (ok) null else "部分图片下载失败")
                updateNotification(
                    if (ok) getString(R.string.download_completed_notification_title)
                    else getString(R.string.download_failed_notification_title),
                    if (ok) getString(R.string.download_completed_files_count, imageUrls.size)
                    else getString(R.string.download_failed_check_network),
                    false
                )
            } catch (e: CancellationException) {
                // 用户手动停止：不写失败日志，兜底保证终态
                TaskManager.failIfActive(targetTaskId, getString(R.string.download_cancelled_by_user))
            } catch (e: Exception) {
                Log.e(TAG, "douyin images download error", e)
                // 关键修复：未预期异常也必须置 FAILED + 落失败日志，避免「卡在停止」且无失败记录
                DownloadLogger.logFailure(this@DownloadService, "douyin", pageUrl ?: imageUrls.firstOrNull().orEmpty(), "图集下载异常终止: ${e.message}")
                TaskManager.failIfActive(targetTaskId, "图集下载异常: ${e.message}")
            } finally {
                taskIdExtra?.let { activeJobs.remove(it) }
                maybeStop()
            }
        }
    }
    // endregion

    // region 快手下载
    /**
     * 快手分享链接下载：复用 KuaishouParser 通过 GraphQL visionVideoDetail 取无水印播放源/图集。
     * 快手公开作品无需登录，流程与 startDouyin 同构。
     */
    private fun startKuaishou(rawUrl: String, mode: String, taskIdExtra: Long?) {
        val targetUrl = UrlUtils.extractFirstUrl(rawUrl) ?: rawUrl
        if (!targetUrl.startsWith("http://", true) && !targetUrl.startsWith("https://", true)) {
            DownloadLogger.logFailure(this, "kuaishou", rawUrl, "无法从剪贴板提取有效链接: $rawUrl")
            TaskManager.createTask(targetUrl, null, NoteType.VIDEO, 1, source = "kuaishou").also {
                TaskManager.startTask(it)
                TaskManager.completeTask(it, false, "无法提取有效链接")
            }
            updateNotification(getString(R.string.download_failed_notification_title),
                getString(R.string.no_valid_link_found), false)
            return
        }

        // WebView 提取出的 CDN 直链（kwaicdn/chenzhongtech/gifshow 等）：直接下载，
        // 不再走已被风控打死的 HTTP 直解解析器
        if (isDirectVideoUrl(targetUrl)) {
            downloadDirectVideo(targetUrl, taskIdExtra)
            return
        }

        if (!activeUrls.add(targetUrl)) {
            updateNotification(getString(R.string.downloading_files), "该链接正在下载中，已忽略重复", false)
            return
        }

        scope.launch {
            var myTaskId: Long = taskIdExtra ?: -1L
            try {
                if (myTaskId < 0) {
                    val plan = TaskManager.planDownloadTask(targetUrl)
                    // 查重（复用同一条任务）：同链接正在下载 → 忽略本次触发
                    if (plan.ignore) {
                        updateNotification(getString(R.string.downloading_files), "该链接正在下载中，已忽略重复", false)
                        return@launch
                    }
                    // 同链接已有完成/失败记录 → 原地重置复用（保持单条记录）；无记录 → 新建
                    myTaskId = plan.taskId?.also { TaskManager.resetTask(it) }
                        ?: TaskManager.createTask(
                            targetUrl, null, NoteType.VIDEO, 1, source = "kuaishou"
                        ).also { TaskManager.startTask(it) }
                }
                activeJobs[myTaskId] = coroutineContext[Job]!!

                updateNotification(getString(R.string.downloading_files), "快手解析中…", true)

                // 解析：HTTP GraphQL 直解（10s 快解）失败 → 后台不可见 WebView 真浏览器兜底
                val (info, kFailReason) = resolveKuaishouMediaInfo(targetUrl)
                if (info == null) {
                    val fullReason = kFailReason.ifBlank { "未知原因" }
                    EventTracker.track(this@DownloadService, "parse_fail", mapOf("source" to "kuaishou", "reason" to fullReason))
                    DownloadLogger.logFailure(this@DownloadService, "kuaishou", targetUrl, "解析失败: $fullReason")
                    TaskManager.completeTask(myTaskId, false, "解析失败: ${fullReason.take(60)}")
                    updateNotification(getString(R.string.download_failed_notification_title),
                        "快手解析失败", false)
                    return@launch
                }
                EventTracker.track(this@DownloadService, "parse_success", mapOf(
                    "source" to "kuaishou",
                    "type" to (if (info.type == KuaishouMediaType.IMAGE) "image" else "video"),
                    "n" to (if (info.type == KuaishouMediaType.IMAGE) info.imageUrls.size.toString() else "1")
                ))

                // 图集：更新任务类型为图片，文件总数为图片数量
                if (info.type == KuaishouMediaType.IMAGE) {
                    TaskManager.updateTask(myTaskId) { t ->
                        t.copy(noteType = NoteType.IMAGE, totalFiles = info.imageUrls.size)
                    }
                }

                updateNotification(getString(R.string.downloading_files), info.title, true)
                val mediaDesc = if (info.type == KuaishouMediaType.IMAGE) "${info.imageUrls.size} 张图" else "视频"
                DownloadLogger.logInfo(this@DownloadService, "kuaishou", targetUrl, "解析成功: ${info.title} ($mediaDesc)")

                val downloader = FileDownloader(this@DownloadService, createCallback(myTaskId))
                val success = if (info.type == KuaishouMediaType.IMAGE) {
                    downloadKuaishouMedia(downloader, info, myTaskId)
                } else {
                    val fileName = "${info.title}.mp4"
                    runCatching {
                        downloader.downloadFile(info.videoUrl, fileName, KUAISHOU_REFERER, info.userAgent)
                    }.getOrElse { e ->
                        DownloadLogger.logFailure(this@DownloadService, "kuaishou", info.videoUrl ?: targetUrl, "下载异常: ${e.message}")
                        false
                    }.also { ok ->
                        if (!ok) {
                            DownloadLogger.logFailure(this@DownloadService, "kuaishou", info.videoUrl ?: targetUrl, "下载失败(返回false)")
                        }
                    }
                }

                EventTracker.track(this@DownloadService, "download_done", mapOf(
                    "source" to "kuaishou",
                    "ok" to success.toString(),
                    "type" to (if (info.type == KuaishouMediaType.IMAGE) "image" else "video")
                ))
                if (success) {
                    DownloadLogger.logInfo(this@DownloadService, "kuaishou", targetUrl, "下载完成(成功): ${info.title}")
                }

                TaskManager.completeTask(myTaskId, success,
                    if (success) null else "下载失败")
                updateNotification(
                    if (success) getString(R.string.download_completed_notification_title)
                    else getString(R.string.download_failed_notification_title),
                    if (success) info.title else getString(R.string.download_failed_check_network),
                    false
                )
            } catch (e: CancellationException) {
                // 用户手动停止：不写失败日志，兜底保证终态
                if (myTaskId > 0) TaskManager.failIfActive(myTaskId, getString(R.string.download_cancelled_by_user))
            } catch (e: Exception) {
                Log.e(TAG, "kuaishou download error", e)
                // 关键修复：未预期异常也必须置 FAILED + 落失败日志，避免「卡在停止」且无失败记录
                if (myTaskId > 0) {
                    DownloadLogger.logFailure(this@DownloadService, "kuaishou", targetUrl, "下载异常终止: ${e.message}")
                    TaskManager.failIfActive(myTaskId, "下载异常: ${e.message}")
                }
            } finally {
                activeUrls.remove(targetUrl)
                if (myTaskId > 0) activeJobs.remove(myTaskId)
                maybeStop()
            }
        }
    }

    /**
     * 下载快手图集（逐张），若解析到视频直链则一并下载。
     * 全部成功返回 true，任一项失败返回 false（失败项已落日志）。
     */
    private fun downloadKuaishouMedia(downloader: FileDownloader, info: KuaishouMediaInfo, taskId: Long): Boolean {
        var okAll = true
        info.imageUrls.forEachIndexed { index, url ->
            val ext = KuaishouParser.mediaExtension(url)
            val fileName = "${info.title}_${index + 1}.$ext"
            val ok = runCatching {
                downloader.downloadFile(url, fileName, KUAISHOU_REFERER, info.userAgent)
            }.getOrElse { e ->
                DownloadLogger.logFailure(this@DownloadService, "kuaishou", url, "下载图片异常: ${e.message}")
                false
            }
            if (!ok) {
                okAll = false
                DownloadLogger.logFailure(this@DownloadService, "kuaishou", url, "图片下载失败(返回false)")
            }
        }
        // 图集之外若拿到视频直链，一并下载
        if (!info.videoUrl.isNullOrBlank()) {
            val vOk = runCatching {
                downloader.downloadFile(info.videoUrl, "${info.title}.mp4", KUAISHOU_REFERER, info.userAgent)
            }.getOrElse { e ->
                DownloadLogger.logFailure(this@DownloadService, "kuaishou", info.videoUrl, "下载视频异常: ${e.message}")
                false
            }
            if (!vOk) {
                okAll = false
                DownloadLogger.logFailure(this@DownloadService, "kuaishou", info.videoUrl, "视频下载失败(返回false)")
            }
        }
        return okAll
    }

    // endregion

    // region 小红书下载（普通模式，复用 XHSDownloader）
    private fun startXhs(rawUrl: String, taskIdExtra: Long?) {
        val targetUrl = UrlUtils.extractFirstUrl(rawUrl) ?: rawUrl
        if (!targetUrl.startsWith("http://", true) && !targetUrl.startsWith("https://", true)) {
            DownloadLogger.logFailure(this, "xhs", rawUrl, "无法从剪贴板提取有效链接")
            TaskManager.createTask(targetUrl, null, NoteType.IMAGE, 1).also {
                TaskManager.startTask(it)
                TaskManager.completeTask(it, false, "无法提取有效链接")
            }
            updateNotification(getString(R.string.download_failed_notification_title),
                getString(R.string.no_valid_link_found), false)
            return
        }

        if (!activeUrls.add(targetUrl)) {
            updateNotification(getString(R.string.downloading_files), "该链接正在下载中，已忽略重复", false)
            return
        }

        scope.launch {
            var myTaskId: Long = taskIdExtra ?: -1L
            try {
                val mediaCount = runCatching { XHSDownloader(this@DownloadService).getMediaCount(targetUrl) }.getOrElse { e ->
                    DownloadLogger.logFailure(this@DownloadService, "xhs", targetUrl, "获取媒体数量失败: ${e.message}")
                    Log.e(TAG, "xhs getMediaCount failed", e)
                    0
                }
                if (myTaskId < 0) {
                    val plan = TaskManager.planDownloadTask(targetUrl)
                    // 查重（复用同一条任务）：同链接正在下载 → 忽略本次触发
                    if (plan.ignore) {
                        updateNotification(getString(R.string.downloading_files), "该链接正在下载中，已忽略重复", false)
                        return@launch
                    }
                    // 同链接已有完成/失败记录 → 原地重置复用（保持单条记录）；无记录 → 新建
                    myTaskId = plan.taskId?.also { TaskManager.resetTask(it) }
                        ?: TaskManager.createTask(
                            targetUrl, null, NoteType.IMAGE, if (mediaCount > 0) mediaCount else 1
                        ).also { TaskManager.startTask(it) }
                }
                activeJobs[myTaskId] = coroutineContext[Job]!!

                updateNotification(getString(R.string.downloading_files),
                    getString(R.string.downloading_files_count, mediaCount), true)

                val completed = AtomicInteger(0)
                val failed = AtomicInteger(0)
                val downloader = XHSDownloader(this@DownloadService, object : DownloadCallback {
                    override fun onFileDownloaded(filePath: String) {
                        completed.incrementAndGet()
                        TaskManager.updateProgress(myTaskId, completed.get(), failed.get(), 0f)
                        TaskManager.addFilePath(myTaskId, filePath)
                    }
                    override fun onDownloadError(status: String, originalUrl: String) {
                        if (isTerminalError(status)) {
                            failed.incrementAndGet()
                            TaskManager.updateProgress(myTaskId, completed.get(), failed.get(), 0f)
                            // 小红书失败也要落日志，方便排查（之前这里没有日志）
                            DownloadLogger.logFailure(this@DownloadService, "xhs", originalUrl, "下载单个文件失败: $status")
                        }
                    }
                    override fun onDownloadProgress(status: String) {}
                    override fun onDownloadProgressUpdate(downloaded: Long, total: Long) {
                        val p = if (total > 0) downloaded.toFloat() / total else 0f
                        TaskManager.updateProgress(myTaskId, completed.get(), failed.get(), p)
                    }
                    override fun onVideoDetected() {
                        TaskManager.updateTaskType(myTaskId, NoteType.VIDEO)
                    }
                })
                downloader.setShouldStopOnVideo(false)

                val success = runCatching { downloader.downloadContent(targetUrl) }.getOrElse { e ->
                    DownloadLogger.logFailure(this@DownloadService, "xhs", targetUrl, "下载过程异常: ${e.message}")
                    Log.e(TAG, "xhs downloadContent error", e)
                    false
                }

                val c = completed.get()
                val f = failed.get()
                EventTracker.track(this@DownloadService, "download_done", mapOf(
                    "source" to "xhs",
                    "ok" to (success && f == 0 && c > 0).toString(),
                    "files" to c.toString(),
                    "failed" to f.toString()
                ))
                if (success && f == 0 && c > 0) {
                    DownloadLogger.logInfo(this@DownloadService, "xhs", targetUrl, "下载完成(成功): $c 个文件")
                }
                when {
                    success && f == 0 && c > 0 -> {
                        TaskManager.completeTask(myTaskId, true)
                        updateNotification(getString(R.string.download_completed_notification_title),
                            getString(R.string.download_completed_files_count, c), false)
                    }
                    c > 0 -> {
                        TaskManager.completeTask(myTaskId, false, "部分文件下载失败")
                        updateNotification(getString(R.string.download_failed_notification_title),
                            getString(R.string.download_completed_files_count, c) + " " +
                                getString(R.string.failed_files_format, f), false)
                    }
                    else -> {
                        // 0 个文件：解析或下载全部失败，记一条日志便于定位（小红书接口被风控时高频出现）
                        DownloadLogger.logFailure(this@DownloadService, "xhs", targetUrl,
                            "未获取到任何可下载文件（解析或下载失败，无媒体可保存）。多为小红书接口风控/返回空内容导致")
                        TaskManager.completeTask(myTaskId, false, getString(R.string.download_failed_no_files))
                        updateNotification(getString(R.string.download_failed_notification_title),
                            getString(R.string.download_failed_no_files), false)
                    }
                }
            } catch (e: CancellationException) {
                // 用户手动停止：不写失败日志，兜底保证终态
                if (myTaskId > 0) TaskManager.failIfActive(myTaskId, getString(R.string.download_cancelled_by_user))
            } catch (e: Exception) {
                Log.e(TAG, "xhs download error", e)
                // 关键修复：未预期异常也必须置 FAILED + 落失败日志，避免「卡在停止」且无失败记录
                if (myTaskId > 0) {
                    DownloadLogger.logFailure(this@DownloadService, "xhs", targetUrl, "下载异常终止: ${e.message}")
                    TaskManager.failIfActive(myTaskId, "下载异常: ${e.message}")
                }
            } finally {
                activeUrls.remove(targetUrl)
                if (myTaskId > 0) activeJobs.remove(myTaskId)
                maybeStop()
            }
        }
    }
    // endregion

    // region 网页爬取（多 URL，复用 MainViewModel.onWebCrawlResult 的去重/过滤逻辑）
    private fun startWebCrawl(urls: List<String>, content: String?, taskIdExtra: Long?) {
        if (urls.isEmpty()) {
            updateNotification(getString(R.string.download_failed_notification_title),
                getString(R.string.no_images_found_via_web_crawl), false)
            return
        }

        // 去重：视频优先 HD，图片+视频合并
        fun isVideo(u: String) = u.contains(".mp4") || u.contains("sns-video") || u.contains("blob:")
        val (videoUrls, imageUrls) = urls.partition { isVideo(it) }
        val finalVideoUrls = if (videoUrls.size > 1) {
            val hd = videoUrls.filter { it.contains("sns-video") }
            if (hd.isNotEmpty()) listOf(hd.distinct().first()) else listOf(videoUrls.distinct().first())
        } else videoUrls
        val finalUrls = (imageUrls + finalVideoUrls).distinct()
        if (finalUrls.isEmpty()) {
            updateNotification(getString(R.string.download_failed_notification_title),
                getString(R.string.no_images_found_via_web_crawl), false)
            return
        }

        val myTaskId = taskIdExtra ?: TaskManager.createTask(
            finalUrls.first(), null, NoteType.UNKNOWN, finalUrls.size
        ).also { TaskManager.startTask(it) }
        activeJobs[myTaskId] = scope.coroutineContext[Job]!!

        updateNotification(getString(R.string.downloading_files),
            getString(R.string.downloading_files_count, finalUrls.size), true)

        scope.launch {
            try {
                val completed = AtomicInteger(0)
                val failed = AtomicInteger(0)
                val downloader = FileDownloader(this@DownloadService, object : DownloadCallback {
                    override fun onFileDownloaded(filePath: String) {
                        completed.incrementAndGet()
                        TaskManager.updateProgress(myTaskId, completed.get(), failed.get(), 0f)
                        TaskManager.addFilePath(myTaskId, filePath)
                    }
                    override fun onDownloadError(status: String, originalUrl: String) {
                        if (isTerminalError(status)) {
                            failed.incrementAndGet()
                            TaskManager.updateProgress(myTaskId, completed.get(), failed.get(), 0f)
                        }
                    }
                    override fun onDownloadProgress(status: String) {}
                    override fun onDownloadProgressUpdate(downloaded: Long, total: Long) {}
                    override fun onVideoDetected() {
                        TaskManager.updateTaskType(myTaskId, NoteType.VIDEO)
                    }
                })

                finalUrls.forEach { rawUrl ->
                    val transformed = rawUrl
                    val low = transformed.lowercase()
                    // 视频直链判定：命中抖音/快手系 CDN（含无扩展名的播放直链）按视频处理，带对应站点 Referer/UA
                    val isVideoUrl = low.contains(".mp4") || low.contains("sns-video")
                        || low.contains("aweme.snssdk.com") || low.contains("douyinvod.com")
                        || low.contains("kwaicdn") || low.contains("chenzhongtech")
                        || low.contains("gifshow") || low.contains("kwai") || low.contains("tiktokcdn")
                    // 按 host 选择 Referer/UA：抖音/快手 CDN 不带 Referer 常被 403，与 downloadDirectVideo 保持一致
                    val (referer, ua) = when {
                        low.contains("douyin") -> DouyinParser.REFERER to DouyinParser.MOBILE_UA
                        low.contains("kuaishou") -> "https://www.kuaishou.com/" to KuaishouParser.MOBILE_UA
                        else -> null to DIRECT_UA
                    }
                    val extension = when {
                        transformed.contains(".mp4") -> "mp4"
                        transformed.contains(".png") -> "png"
                        transformed.contains(".gif") -> "gif"
                        transformed.contains(".webp") -> "webp"
                        else -> "jpg"
                    }
                    val fileName = "webview_${System.currentTimeMillis()}_${completed.get() + 1}.$extension"
                    val ok = if (isVideoUrl) {
                        // 视频类：解析最终直链（跟随 302）后用对应站点 Referer/UA 直连，失败回退原 URL 重试一次
                        val resolved = resolveFinalUrl(transformed, ua, referer) ?: transformed
                        runCatching { downloader.downloadFile(resolved, fileName, referer, ua) }.getOrElse { false }
                            || runCatching { downloader.downloadFile(transformed, fileName, referer, ua) }.getOrElse { false }
                    } else {
                        runCatching { downloader.downloadFile(transformed, fileName) }.getOrElse { false }
                    }
                    if (!ok) {
                        failed.incrementAndGet()
                        TaskManager.updateProgress(myTaskId, completed.get(), failed.get(), 0f)
                    }
                }

                val c = completed.get()
                val f = failed.get()
                val success = c > 0 && f == 0
                if (success) {
                    DownloadLogger.logInfo(this@DownloadService, "web", finalUrls.first(), "网页爬取完成(成功): $c 个文件")
                }
                TaskManager.completeTask(myTaskId, success,
                    if (success) null else if (c > 0) "部分文件下载失败" else "下载失败")
                updateNotification(
                    if (success) getString(R.string.download_completed_notification_title)
                    else getString(R.string.download_failed_notification_title),
                    if (success) getString(R.string.download_completed_files_count, c)
                    else getString(R.string.download_failed_check_network),
                    false
                )
            } catch (e: CancellationException) {
                // 用户手动停止：不写失败日志，兜底保证终态
                TaskManager.failIfActive(myTaskId, getString(R.string.download_cancelled_by_user))
            } catch (e: Exception) {
                Log.e(TAG, "web crawl error", e)
                // 关键修复：未预期异常也必须置 FAILED + 落失败日志，避免「卡在停止」且无失败记录
                DownloadLogger.logFailure(this@DownloadService, "web", finalUrls.firstOrNull().orEmpty(), "网页爬取异常终止: ${e.message}")
                TaskManager.failIfActive(myTaskId, "下载异常: ${e.message}")
            } finally {
                taskIdExtra?.let { activeJobs.remove(it) }
                maybeStop()
            }
        }
    }
    // endregion

    private fun createCallback(taskId: Long): DownloadCallback {
        val completed = AtomicInteger(0)
        val failed = AtomicInteger(0)
        return object : DownloadCallback {
            override fun onFileDownloaded(filePath: String) {
                completed.incrementAndGet()
                TaskManager.updateProgress(taskId, completed.get(), failed.get(), 0f)
                TaskManager.addFilePath(taskId, filePath)
            }
            override fun onDownloadError(status: String, originalUrl: String) {
                if (isTerminalError(status)) {
                    failed.incrementAndGet()
                    TaskManager.updateProgress(taskId, completed.get(), failed.get(), 0f)
                }
            }
            override fun onDownloadProgress(status: String) {}
            override fun onDownloadProgressUpdate(downloaded: Long, total: Long) {}
            override fun onVideoDetected() {}
        }
    }

    private fun isTerminalError(status: String): Boolean {
        val n = status.lowercase()
        return n.contains("failed to download after") ||
            n.contains("exception downloading") ||
            n.contains("download failed") ||
            n.contains("io error downloading file") ||
            n.contains("security exception while downloading file") ||
            n.contains("non-media response received") ||
            n.contains("both image and video failed to download separately")
    }

    /** 所有任务都结束后，如果没有正在进行的任务，停止前台服务。 */
    private fun maybeStop() {
        if (activeJobs.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // region 通知
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID, getString(R.string.fg_channel_name), NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            mgr.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(title: String, content: String, ongoing: Boolean) {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = buildNotification(title, content)
        // 复用同一个 NOTIFICATION_ID；结束时把 ongoing 标志改为非持续
        mgr.notify(NOTIFICATION_ID, n)
        if (!ongoing) {
            // 任务结束：移除常驻标记。简单做法：用新的非 ongoing 通知替换
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val end = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            mgr.notify(NOTIFICATION_ID, end)
            // 结束通知展示后，稍等片刻再取消前台状态并停止
            android.os.Handler(mainLooper).postDelayed({
                stopForeground(STOP_FOREGROUND_REMOVE)
                if (activeJobs.isEmpty()) stopSelf()
            }, 1500)
        }
    }
    // endregion

    companion object {
        private const val TAG = "DownloadService"
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "xhs_fg_download_channel"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_SOURCE = "extra_source"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_URLS = "extra_urls"
        const val EXTRA_WEB_CONTENT = "extra_web_content"
        const val MODE_NORMAL = "normal"
        const val MODE_SELECTIVE = "selective"
        const val MODE_WEBCRAWL = "webcrawl"
        const val MODE_DOUYIN_IMAGES = "douyin_images"
        const val MODE_DOUYIN_HOME = "douyin_home_batch"

        /** HTTP 直解快解预算：超过即转后台 WebView 兜底（避免被风控空响应拖死）。 */
        private const val HTTP_FAST_TIMEOUT_MS = 10_000L
        private const val ACTION_STOP = "com.neoruaa.xhsdn.action.STOP"

        /** 网页爬取（多 URL）下载入口。 */
        fun startWebCrawl(context: Context, urls: List<String>, content: String? = null, taskId: Long? = null) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_MODE, MODE_WEBCRAWL)
                putStringArrayListExtra(EXTRA_URLS, ArrayList(urls))
                content?.let { putExtra(EXTRA_WEB_CONTENT, it) }
                taskId?.let { putExtra(EXTRA_TASK_ID, it) }
            }
            context.startForegroundService(intent)
        }

        /** 取消指定任务（供 MainActivity 的停止按钮调用）。 */
        fun stopTask(context: Context, taskId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }

        fun startDownload(context: Context, url: String, source: String? = null, taskId: Long? = null) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_SOURCE, source ?: UrlUtils.detectPlatform(url) ?: "xhs")
                taskId?.let { putExtra(EXTRA_TASK_ID, it) }
            }
            context.startForegroundService(intent)
        }

        /** 抖音图文/图集帖：传入帖子图集图片直链列表，逐张下载（不进视频解析器）。 */
        fun startDownloadDouyinImages(context: Context, imageUrls: List<String>, pageUrl: String? = null, taskId: Long? = null) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_MODE, MODE_DOUYIN_IMAGES)
                putStringArrayListExtra(EXTRA_URLS, ArrayList(imageUrls))
                pageUrl?.let { putExtra(EXTRA_URL, it) }
                taskId?.let { putExtra(EXTRA_TASK_ID, it) }
            }
            context.startForegroundService(intent)
        }

        /**
         * 抖音主页批量下载入口：videoPageUrls 为 WebView 主页爬取收集到的视频页链接列表
         * （/video/{id}），由服务后台逐条解析（HTTP 快解 → 后台 WebView 兜底）并同卡下载。
         */
        fun startDouyinHomeBatch(context: Context, videoPageUrls: List<String>, homepageUrl: String? = null, taskId: Long? = null) {
            if (videoPageUrls.isEmpty()) return
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_MODE, MODE_DOUYIN_HOME)
                putStringArrayListExtra(EXTRA_URLS, ArrayList(videoPageUrls))
                homepageUrl?.let { putExtra(EXTRA_URL, it) }
                taskId?.let { putExtra(EXTRA_TASK_ID, it) }
            }
            context.startForegroundService(intent)
        }
    }
}
