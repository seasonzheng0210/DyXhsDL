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
import com.neoruaa.xhsdn.douyin.DouyinParser
import com.neoruaa.xhsdn.utils.DownloadLogger
import com.neoruaa.xhsdn.utils.UrlUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val activeUrls = ConcurrentHashMap.newKeySet<String>()

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

        when (mode) {
            MODE_WEBCRAWL -> {
                val urls = intent.getStringArrayListExtra(EXTRA_URLS) ?: emptyList()
                val content = intent.getStringExtra(EXTRA_WEB_CONTENT)
                startWebCrawl(urls, content, taskIdExtra)
            }
            else -> when (source) {
                "douyin" -> startDouyin(url, mode, taskIdExtra)
                else -> startXhs(url, taskIdExtra)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
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

        if (!activeUrls.add(targetUrl)) {
            // 正在下载，忽略重复
            return
        }

        scope.launch {
            try {
                val myTaskId = taskIdExtra
                    ?: TaskManager.findActiveTaskIdByUrl(targetUrl)
                    ?: TaskManager.createTask(
                        targetUrl, null, NoteType.VIDEO, 1, source = "douyin"
                    ).also { TaskManager.startTask(it) }
                activeJobs[myTaskId] = coroutineContext[Job]!!

                updateNotification(getString(R.string.downloading_files), "抖音解析中…", true)

                val info = runCatching { DouyinParser.parse(targetUrl) }.getOrElse { e ->
                    DownloadLogger.logFailure(this@DownloadService, "douyin", targetUrl, "解析失败: ${e.message}")
                    TaskManager.completeTask(myTaskId, false, "解析失败: ${e.message}")
                    updateNotification(getString(R.string.download_failed_notification_title),
                        "抖音解析失败: ${e.message}", false)
                    return@launch
                }

                updateNotification(getString(R.string.downloading_files), info.title, true)

                val fileName = "${info.title}.mp4"
                val downloader = FileDownloader(this@DownloadService, createCallback(myTaskId))
                val success = runCatching {
                    downloader.downloadFile(info.videoUrl, fileName, "", info.userAgent)
                }.getOrElse { e ->
                    DownloadLogger.logFailure(this@DownloadService, "douyin", info.videoUrl, "下载异常: ${e.message}")
                    false
                }

                if (!success) {
                    DownloadLogger.logFailure(this@DownloadService, "douyin", info.videoUrl, "下载失败(返回false)")
                }
                TaskManager.completeTask(myTaskId, success,
                    if (success) null else "下载失败")
                updateNotification(
                    if (success) getString(R.string.download_completed_notification_title)
                    else getString(R.string.download_failed_notification_title),
                    if (success) info.title else getString(R.string.download_failed_check_network),
                    false
                )
            } catch (e: Exception) {
                Log.e(TAG, "douyin download error", e)
            } finally {
                activeUrls.remove(targetUrl)
                taskIdExtra?.let { activeJobs.remove(it) }
                maybeStop()
            }
        }
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

        if (!activeUrls.add(targetUrl)) return

        scope.launch {
            try {
                val mediaCount = runCatching { XHSDownloader(this@DownloadService).getMediaCount(targetUrl) }.getOrElse { e ->
                    DownloadLogger.logFailure(this@DownloadService, "xhs", targetUrl, "获取媒体数量失败: ${e.message}")
                    Log.e(TAG, "xhs getMediaCount failed", e)
                    0
                }
                val myTaskId = taskIdExtra
                    ?: TaskManager.findActiveTaskIdByUrl(targetUrl)
                    ?: TaskManager.createTask(
                        targetUrl, null, NoteType.IMAGE, if (mediaCount > 0) mediaCount else 1
                    ).also { TaskManager.startTask(it) }
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
            } catch (e: Exception) {
                Log.e(TAG, "xhs download error", e)
            } finally {
                activeUrls.remove(targetUrl)
                taskIdExtra?.let { activeJobs.remove(it) }
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
                    val transformed = downloader.run {
                        // transformXhsCdnUrl 为 XHSDownloader 的扩展，这里用原始 URL 直连
                        rawUrl
                    }
                    val extension = when {
                        transformed.contains(".mp4") -> "mp4"
                        transformed.contains(".png") -> "png"
                        transformed.contains(".gif") -> "gif"
                        transformed.contains(".webp") -> "webp"
                        else -> "jpg"
                    }
                    val fileName = "webview_${System.currentTimeMillis()}_${completed.get() + 1}.$extension"
                    val ok = runCatching { downloader.downloadFile(transformed, fileName) }.getOrElse { false }
                    if (!ok) {
                        failed.incrementAndGet()
                        TaskManager.updateProgress(myTaskId, completed.get(), failed.get(), 0f)
                    }
                }

                val c = completed.get()
                val f = failed.get()
                val success = c > 0 && f == 0
                TaskManager.completeTask(myTaskId, success,
                    if (success) null else if (c > 0) "部分文件下载失败" else "下载失败")
                updateNotification(
                    if (success) getString(R.string.download_completed_notification_title)
                    else getString(R.string.download_failed_notification_title),
                    if (success) getString(R.string.download_completed_files_count, c)
                    else getString(R.string.download_failed_check_network),
                    false
                )
            } catch (e: Exception) {
                Log.e(TAG, "web crawl error", e)
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
    }
}
