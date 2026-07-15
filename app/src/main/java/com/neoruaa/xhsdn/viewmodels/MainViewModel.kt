package com.neoruaa.xhsdn.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.NonCancellable
import com.neoruaa.xhsdn.data.TaskManager
import com.neoruaa.xhsdn.data.TaskStatus
import com.neoruaa.xhsdn.data.NoteType
import kotlinx.coroutines.CancellationException
import android.util.Log
import com.neoruaa.xhsdn.DownloadCallback
import com.neoruaa.xhsdn.FileDownloader
import com.neoruaa.xhsdn.R
import com.neoruaa.xhsdn.XHSDownloader
import com.neoruaa.xhsdn.data.DownloadTask
import com.neoruaa.xhsdn.douyin.DouyinParser
import com.neoruaa.xhsdn.utils.NotificationHelper
import com.neoruaa.xhsdn.utils.DownloadLogger
import com.neoruaa.xhsdn.utils.UrlUtils
import java.io.File
import java.io.IOException


data class MediaItem(val path: String, val type: MediaType)

enum class MediaType {
    IMAGE, VIDEO, OTHER
}

enum class SelectiveDownloadPhase {
    Idle, Caching, Ready, Saving, Error
}

data class CachedMediaItem(
    val path: String,
    val displayName: String,
    val type: MediaType
)

data class SelectiveDownloadUiState(
    val show: Boolean = false,
    val phase: SelectiveDownloadPhase = SelectiveDownloadPhase.Idle,
    val progress: Float = 0f,
    val progressLabel: String = "",
    val progressText: String = "0.0%｜0KB/s",
    val status: String = "",
    val items: List<CachedMediaItem> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val noteUrl: String = "",
    val noteContent: String? = null,
    val cacheDir: String? = null,
    val errorMessage: String? = null
)

/**
 * Shown when the user retries the same URL but the app parses a different number of
 * media items than the previous attempt. Offers to export both attempts' details so
 * the inconsistency can be reported to the developer (see issue #37).
 */
data class InconsistentRetryDialogState(
    val show: Boolean = false,
    val url: String = "",
    val previousCount: Int = 0,
    val currentCount: Int = 0,
    val logContent: String = ""
)

data class MainUiState(
    val urlInput: String = "",
    val status: List<String> = emptyList(),
    val mediaItems: List<MediaItem> = emptyList(),
    val isDownloading: Boolean = false,
    val progressLabel: String = "",
    val progress: Float = 0f,
    val downloadProgressText: String = "0%｜0kb/s", // Format: "XX%｜XXXkb/s"
    val showWebCrawl: Boolean = false,
    val showVideoWarning: Boolean = false,
    val selectiveDownload: SelectiveDownloadUiState = SelectiveDownloadUiState(),
    val inconsistentRetry: InconsistentRetryDialogState = InconsistentRetryDialogState()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    private var totalMediaCount = 0
    private var downloadedCount = 0
    private val displayedFiles = mutableSetOf<String>()
    private var currentUrl: String? = null
    private var hasUserContinuedAfterVideoWarning = false
    private var downloadJob: Job? = null
    private var currentDownloader: XHSDownloader? = null

    // Records the previous download attempt so we can detect when the user retries the
    // same URL but the app parses a different number of media items (see issue #37).
    private data class ParseAttempt(
        val url: String,
        val mediaCount: Int,
        val timestampMillis: Long,
        val mode: String
    )
    private var lastParseAttempt: ParseAttempt? = null

    // Track individual file progress for more accurate overall progress
    private val fileProgressMap = mutableMapOf<String, Float>() // Maps file path to progress (0.0 to 1.0)
    private var currentFileProgress = 0f // Progress of the currently downloading file (0.0 to 1.0)
    private var lastOverallProgress = 0f // Track the last overall progress to prevent regression

    // Fields to track download progress and speed for the first callback
    private var currentDownloadStartTime: Long = 0
    private var currentDownloadStartBytes: Long = 0
    private var currentDownloadTotalBytes: Long = 0
    private var currentDownloadedBytes: Long = 0
    private var lastSpeedCalculationTime: Long = 0
    private var lastCalculatedSpeed = "0kb/s"

    // Task tracking for history
    var currentTaskId: Long = 0
        private set
    private var taskCompletedFiles: Int = 0
    private var taskFailedFiles: Int = 0
    private var taskCurrentFileProgress: Float = 0f
    private var maxTaskProgress: Float = 0f

    // Throttling for task progress updates to reduce database writes
    private var lastTaskProgressUpdateTime = 0L
    private val TASK_PROGRESS_UPDATE_INTERVAL = 100L // 100ms interval between updates
    private val debugNotificationImportantKeywords = listOf(
        "开始",
        "完成",
        "失败",
        "错误",
        "出错",
        "取消",
        "停止",
        "检测到视频",
        "网页转存",
        "提示"
    )

    fun cancelCurrentDownload() {
        if (downloadJob?.isActive == true) {
            // Signal the downloader to stop at the next checkpoint
            currentDownloader?.stopDownload()
            downloadJob?.cancel()
            _uiState.update { it.copy(isDownloading = false, progressLabel = "已取消") }
            if (currentTaskId > 0) {
                TaskManager.completeTask(currentTaskId, false, "用户取消")
            }
        }
    }

    private fun formatSpeed(bytesPerSecond: Double): String {
        return when {
            bytesPerSecond >= 1024 * 1024 -> { // >= 1 MB/s
                val mbps = bytesPerSecond / (1024 * 1024)
                "${String.format("%.2f", mbps)}MB/s"
            }
            bytesPerSecond >= 1024 -> { // >= 1 KB/s
                val kbps = bytesPerSecond / 1024
                "${String.format("%.1f", kbps)}KB/s"
            }
            else -> {
                "${bytesPerSecond.toInt()}B/s"
            }
        }
    }

    private fun resetDownloadTracking() {
        currentDownloadStartTime = System.currentTimeMillis()
        currentDownloadStartBytes = 0
        currentDownloadTotalBytes = 0
        currentDownloadedBytes = 0
        lastSpeedCalculationTime = 0
        lastCalculatedSpeed = "0KB/s"
        lastOverallProgress = 0f // Reset the last overall progress when starting a new download

        // Update UI to show initial state
        _uiState.update { currentState ->
            currentState.copy(downloadProgressText = "0.0%｜0KB/s")
        }
    }

    private fun isTerminalDownloadError(status: String): Boolean {
        val normalized = status.lowercase(Locale.ROOT)
        return normalized.contains("failed to download after") ||
            normalized.contains("exception downloading") ||
            normalized.contains("download failed") ||
            normalized.contains("io error downloading file") ||
            normalized.contains("security exception while downloading file") ||
            normalized.contains("non-media response received") ||
            normalized.contains("both image and video failed to download separately")
    }

    fun updateUrl(value: String) {
        _uiState.update { it.copy(urlInput = value, showWebCrawl = false) }
    }

    fun pasteLinkFromClipboard() {
        viewModelScope.launch {
            try {
                val clipboardManager = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = clipboardManager.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val clipboardText = clipData.getItemAt(0).text?.toString() ?: ""
                    if (clipboardText.isNotEmpty()) {
                        _uiState.update { it.copy(urlInput = clipboardText) }
                        appendStatus("已从剪贴板粘贴链接")
                    } else {
                        appendStatus("剪贴板内容为空")
                    }
                } else {
                    appendStatus("剪贴板无内容")
                }
            } catch (e: Exception) {
                appendStatus("读取剪贴板失败: ${e.message}")
            }
        }
    }

    /**
     * Record the parse result of a download attempt and, if the user is retrying the
     * same URL but the parsed media count changed, surface a dialog offering to export
     * the diagnostic logs. Safe to call from any thread.
     */
    private fun recordParseAttemptAndDetect(url: String, mediaCount: Int, mode: String) {
        val previous = lastParseAttempt
        val current = ParseAttempt(url, mediaCount, System.currentTimeMillis(), mode)
        lastParseAttempt = current

        if (previous != null && previous.url == url && previous.mediaCount != mediaCount) {
            val logContent = buildInconsistencyLog(previous, current)
            _uiState.update { state ->
                state.copy(
                    inconsistentRetry = InconsistentRetryDialogState(
                        show = true,
                        url = url,
                        previousCount = previous.mediaCount,
                        currentCount = mediaCount,
                        logContent = logContent
                    )
                )
            }
        }
    }

    private fun buildInconsistencyLog(previous: ParseAttempt, current: ParseAttempt): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        fun line(label: String, attempt: ParseAttempt) = buildString {
            appendLine("[$label]")
            appendLine("time: ${fmt.format(java.util.Date(attempt.timestampMillis))}")
            appendLine("mode: ${attempt.mode}")
            appendLine("parsedMediaCount: ${attempt.mediaCount}")
        }
        return buildString {
            appendLine("XHS Downloader - parse inconsistency diagnostic")
            appendLine("generatedAt: ${fmt.format(java.util.Date())}")
            appendLine("appVersion: ${com.neoruaa.xhsdn.BuildConfig.VERSION_NAME} (${com.neoruaa.xhsdn.BuildConfig.VERSION_CODE})")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("androidVersion: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("url: ${current.url}")
            appendLine("countDelta: ${previous.mediaCount} -> ${current.mediaCount}")
            appendLine()
            append(line("previousAttempt", previous))
            appendLine()
            append(line("currentAttempt", current))
        }
    }

    fun dismissInconsistentRetryDialog() {
        _uiState.update { it.copy(inconsistentRetry = InconsistentRetryDialogState()) }
    }

    fun saveInconsistentRetryLogs(onResult: (String) -> Unit, onError: (String) -> Unit) {
        val content = _uiState.value.inconsistentRetry.logContent
        val app = getApplication<Application>()
        if (content.isBlank()) {
            _uiState.update { it.copy(inconsistentRetry = InconsistentRetryDialogState()) }
            onError(app.getString(R.string.retry_inconsistent_save_failed))
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val savedLocation = runCatching { writeDiagnosticLogToDownloads(content) }.getOrNull()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(inconsistentRetry = InconsistentRetryDialogState()) }
                if (savedLocation != null) {
                    onResult(app.getString(R.string.retry_inconsistent_save_success, savedLocation))
                } else {
                    onError(app.getString(R.string.retry_inconsistent_save_failed))
                }
            }
        }
    }

    private fun writeDiagnosticLogToDownloads(content: String): String {
        val app = getApplication<Application>()
        val fileName = "xhsdn_diagnostic_${System.currentTimeMillis()}.txt"
        val subDir = "xhsdn"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = app.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + File.separator + subDir
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Failed to create MediaStore entry")
            (resolver.openOutputStream(uri)
                ?: throw IOException("Failed to open output stream")).use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            }
            val finalize = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, finalize, null, null)
            return "Download/$subDir/$fileName"
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                subDir
            )
            if (!dir.exists() && !dir.mkdirs()) {
                throw IOException("Failed to create directory: ${dir.absolutePath}")
            }
            val file = File(dir, fileName)
            file.writeText(content, Charsets.UTF_8)
            return file.absolutePath
        }
    }

    fun startSelectiveDownload(onError: (String) -> Unit) {
        val targetUrl = _uiState.value.urlInput.trim()
        if (targetUrl.isEmpty()) {
            onError("请输入链接")
            return
        }

        currentUrl = targetUrl
        currentTaskId = 0
        downloadedCount = 0
        totalMediaCount = 0
        displayedFiles.clear()
        resetDownloadTracking()

        val selectiveCacheRoot = File(getApplication<Application>().cacheDir, "selective_download")
        cleanupSelectiveCache(selectiveCacheRoot.absolutePath)
        val sessionDir = File(selectiveCacheRoot, System.currentTimeMillis().toString())

        _uiState.update {
            it.copy(
                isDownloading = true,
                status = listOf("缓存中：$targetUrl"),
                mediaItems = emptyList(),
                progressLabel = "",
                progress = 0f,
                showWebCrawl = false,
                showVideoWarning = false,
                selectiveDownload = SelectiveDownloadUiState(
                    show = true,
                    phase = SelectiveDownloadPhase.Caching,
                    status = "正在缓存媒体",
                    noteUrl = targetUrl,
                    cacheDir = sessionDir.absolutePath
                )
            )
        }

        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            val downloader = XHSDownloader(
                getApplication(),
                createSelectiveCacheCallback(this)
            )
            currentDownloader = downloader
            downloader.setShouldStopOnVideo(false)
            downloader.resetStopDownload()

            try {
                totalMediaCount = runCatching { XHSDownloader(getApplication()).getMediaCount(targetUrl) }
                    .getOrElse { 0 }
                updateSelectiveProgress()
                recordParseAttemptAndDetect(targetUrl, totalMediaCount, "selective")

                val result = downloader.downloadContentToCache(targetUrl, sessionDir)
                coroutineContext[Job]?.ensureActive()

                val noteContent = runCatching {
                    XHSDownloader(getApplication(), null).getNoteDescription(targetUrl)
                }.getOrNull()

                withContext(Dispatchers.Main) {
                    val existingItems = _uiState.value.selectiveDownload.items
                    val resultItems = result.files.map {
                        CachedMediaItem(it.path, it.displayName, detectMediaType(it.path))
                    }
                    val mergedItems = (existingItems + resultItems)
                        .distinctBy { it.path }
                    if (result.success && mergedItems.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                selectiveDownload = state.selectiveDownload.copy(
                                    phase = SelectiveDownloadPhase.Ready,
                                    progress = 1f,
                                    progressLabel = "${mergedItems.size}/${mergedItems.size}",
                                    progressText = "100.0%｜0KB/s",
                                    status = "缓存完成",
                                    items = mergedItems,
                                    selectedPaths = mergedItems.map { it.path }.toSet(),
                                    noteContent = noteContent
                                )
                            )
                        }
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                selectiveDownload = state.selectiveDownload.copy(
                                    phase = SelectiveDownloadPhase.Error,
                                    status = "缓存失败",
                                    errorMessage = "未缓存到可选择的媒体"
                                )
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                cleanupSelectiveCache(sessionDir.absolutePath)
                withContext(NonCancellable + Dispatchers.Main) {
                    resetSelectiveDownloadState()
                }
            } catch (e: Exception) {
                withContext(NonCancellable + Dispatchers.Main) {
                    _uiState.update { state ->
                        state.copy(
                            isDownloading = true,
                            selectiveDownload = state.selectiveDownload.copy(
                                phase = SelectiveDownloadPhase.Error,
                                status = "缓存出错",
                                errorMessage = e.message ?: "未知错误"
                            )
                        )
                    }
                }
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    if (_uiState.value.selectiveDownload.phase == SelectiveDownloadPhase.Caching) {
                        _uiState.update { it.copy(isDownloading = false) }
                    }
                    currentDownloader = null
                }
            }
        }
    }

    fun cancelSelectiveDownload() {
        val cacheDir = _uiState.value.selectiveDownload.cacheDir
        currentDownloader?.stopDownload()
        downloadJob?.cancel()
        cleanupSelectiveCache(cacheDir)
        resetSelectiveDownloadState()
    }

    fun toggleSelectiveItem(path: String) {
        _uiState.update { state ->
            val selected = state.selectiveDownload.selectedPaths
            val nextSelected = if (selected.contains(path)) {
                selected - path
            } else {
                selected + path
            }
            state.copy(
                selectiveDownload = state.selectiveDownload.copy(
                    selectedPaths = nextSelected
                )
            )
        }
    }

    fun saveSelectedMedia(onError: (String) -> Unit) {
        val selectiveState = _uiState.value.selectiveDownload
        val selectedItems = selectiveState.items.filter { selectiveState.selectedPaths.contains(it.path) }
        if (selectiveState.phase != SelectiveDownloadPhase.Ready || selectedItems.isEmpty()) {
            onError("请选择要下载的内容")
            return
        }

        _uiState.update { state ->
            state.copy(
                selectiveDownload = state.selectiveDownload.copy(
                    phase = SelectiveDownloadPhase.Saving,
                    progress = 0f,
                    progressLabel = "0/${selectedItems.size}",
                    progressText = "0.0%｜0KB/s",
                    status = "正在保存选中内容"
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val taskId = TaskManager.createTask(
                noteUrl = selectiveState.noteUrl,
                noteTitle = extractTitleFromUrl(selectiveState.noteUrl),
                noteType = if (selectedItems.any { it.type == MediaType.VIDEO }) NoteType.VIDEO else NoteType.IMAGE,
                totalFiles = selectedItems.size,
                noteContent = selectiveState.noteContent
            )
            TaskManager.startTask(taskId)

            val saver = FileDownloader(getApplication(), null)
            val savedPaths = mutableListOf<String>()
            var failedCount = 0

            selectedItems.forEachIndexed { index, item ->
                val savedFile = runCatching {
                    saver.copyCachedFileToMediaStore(File(item.path))
                }.getOrNull()
                if (savedFile != null && savedFile.exists()) {
                    savedPaths.add(savedFile.absolutePath)
                    TaskManager.addFilePath(taskId, savedFile.absolutePath)
                } else {
                    failedCount++
                }

                val completed = savedPaths.size
                TaskManager.updateProgress(taskId, completed, failedCount, 0f)
                withContext(Dispatchers.Main) {
                    val progress = (index + 1) / selectedItems.size.toFloat()
                    _uiState.update { state ->
                        state.copy(
                            selectiveDownload = state.selectiveDownload.copy(
                                progress = progress,
                                progressLabel = "${index + 1}/${selectedItems.size}",
                                progressText = "${String.format("%.1f", progress * 100)}%｜0KB/s"
                            )
                        )
                    }
                }
            }

            val success = savedPaths.isNotEmpty() && failedCount == 0
            TaskManager.completeTask(
                taskId,
                success,
                when {
                    success -> null
                    savedPaths.isNotEmpty() -> "部分文件保存失败 ($failedCount)"
                    else -> "保存失败"
                }
            )

            cleanupSelectiveCache(selectiveState.cacheDir)
            withContext(Dispatchers.Main) {
                resetSelectiveDownloadState()
                appendStatus(if (success) "✅ 已保存选中内容" else "⚠️ 部分内容保存失败")
            }
        }
    }

    fun startDownload(onError: (String) -> Unit) {
        val targetUrl = _uiState.value.urlInput.trim()
        if (targetUrl.isEmpty()) {
            onError("请输入链接")
            return
        }
        // Do NOT cancel any existing download job — let the old task complete in the background
        currentUrl = targetUrl
        downloadedCount = 0
        totalMediaCount = 0
        displayedFiles.clear()

        // Reset download tracking variables
        resetDownloadTracking()

        _uiState.update {
            it.copy(
                isDownloading = true,
                status = listOf("处理中：$targetUrl"),
                mediaItems = emptyList(),
                progressLabel = "",
                progress = 0f,
                showWebCrawl = false
            )
        }

        // 在协程外部立即创建新任务并设置currentTaskId，以便外部可以立即访问
        val initialTaskId = TaskManager.createTask(
            noteUrl = targetUrl,
            noteTitle = extractTitleFromUrl(targetUrl),
            noteType = NoteType.IMAGE,
            totalFiles = 1 // We'll update this later after getting the count
        )
        TaskManager.startTask(initialTaskId)

        currentTaskId = initialTaskId
        // 重置任务跟踪计数器
        taskCompletedFiles = 0
        taskFailedFiles = 0

        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            totalMediaCount = runCatching { XHSDownloader(getApplication()).getMediaCount(targetUrl) }
                .getOrElse { 0 }
            updateProgress()
            recordParseAttemptAndDetect(targetUrl, totalMediaCount, "normal")

            // Update the task with the actual total file count
            if (totalMediaCount > 0) {
                TaskManager.updateTask(initialTaskId) { task ->
                    task.copy(totalFiles = totalMediaCount)
                }
            }

            val myTaskId = initialTaskId // Capture taskId locally for this coroutine

            // Track completed/failed files for this task (shared between callback and completion logic)
            val localCompletedFiles = java.util.concurrent.atomic.AtomicInteger(0)
            val localFailedFiles = java.util.concurrent.atomic.AtomicInteger(0)

            val downloader = XHSDownloader(
                getApplication(),
                createDownloadCallback(myTaskId, localCompletedFiles, localFailedFiles, this)
            )

            // Store reference so cancelCurrentDownload() can signal this downloader
            currentDownloader = downloader

            // If user has continued after video warning, don't stop on video detection
            if (hasUserContinuedAfterVideoWarning) {
                downloader.setShouldStopOnVideo(false)
            } else {
                downloader.setShouldStopOnVideo(true)
            }
            // Reset the stop flag for new download
            downloader.resetStopDownload()

            try {
                val success = runDownloadWithCancellationCheck(downloader, targetUrl, coroutineContext[Job])
                finalizeTaskCompletion(myTaskId, success, localCompletedFiles.get(), localFailedFiles.get())
            } catch (e: Exception) {
                if (e is CancellationException) {
                     // Check if this cancellation was for WAITING_FOR_USER
                     if (e.message == "WAITING_FOR_USER") {
                         Log.d("MainViewModel", "Download cancelled for user input")
                         // Do NOT complete task as failed. Leave it as WAITING_FOR_USER.
                     } else {
                        withContext(NonCancellable + Dispatchers.Main) {
                            appendStatus("⏹️ 下载已取消")
                            TaskManager.completeTask(myTaskId, false, "下载已取消")
                        }
                     }
                } else {
                    withContext(NonCancellable + Dispatchers.Main) {
                        appendStatus("❌ 下载出错: ${e.message}")
                        TaskManager.completeTask(myTaskId, false, e.message ?: "未知错误")
                    }
                }
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    // Only reset UI-related state if this is still the active task
                    if (currentTaskId == myTaskId) {
                        resetDownloadTracking()
                        currentTaskId = 0
                        taskCompletedFiles = 0
                        taskFailedFiles = 0
                        hasUserContinuedAfterVideoWarning = false
                        currentDownloader = null
                    }
                }
            }
        }
    }

    /**
     * 重试失败的任务（复用已有 taskId）
     */
    fun retryTask(task: DownloadTask, onError: (String) -> Unit) {
        // 重置任务状态
        TaskManager.resetTask(task.id)
        currentTaskId = task.id

        val targetUrl = task.noteUrl
        currentUrl = targetUrl
        updateUrl(targetUrl)

        // Reset tracking
        downloadedCount = 0
        totalMediaCount = 0
        taskCompletedFiles = 0
        taskFailedFiles = 0
        displayedFiles.clear()
        resetDownloadTracking()

        _uiState.update {
            it.copy(
                isDownloading = true,
                progressLabel = "",
                progress = 0f,
                showWebCrawl = false,
                showVideoWarning = false,
                mediaItems = emptyList()
            )
        }

        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            totalMediaCount = runCatching { XHSDownloader(getApplication()).getMediaCount(targetUrl) }
                .getOrElse { 0 }
            updateProgress()
            recordParseAttemptAndDetect(targetUrl, totalMediaCount, "retry")

            val myTaskId = task.id
            val localCompletedFiles = java.util.concurrent.atomic.AtomicInteger(taskCompletedFiles)
            val localFailedFiles = java.util.concurrent.atomic.AtomicInteger(taskFailedFiles)

            val downloader = XHSDownloader(
                getApplication(),
                createDownloadCallback(myTaskId, localCompletedFiles, localFailedFiles, this)
            )
            
            // Store reference so cancelCurrentDownload() can signal this downloader
            currentDownloader = downloader

            downloader.setShouldStopOnVideo(true)
            downloader.resetStopDownload()

            try {
                val success = runDownloadWithCancellationCheck(downloader, targetUrl, coroutineContext[Job])
                finalizeTaskCompletion(myTaskId, success, localCompletedFiles.get(), localFailedFiles.get())
            } catch (e: CancellationException) {
                withContext(NonCancellable + Dispatchers.Main) {
                    if (localCompletedFiles.get() == 0) {
                        TaskManager.completeTask(myTaskId, false, "下载已取消")
                    }
                }
            } catch (e: Exception) {
                withContext(NonCancellable + Dispatchers.Main) {
                    TaskManager.completeTask(myTaskId, false, e.message ?: "未知错误")
                }
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    _uiState.update { it.copy(isDownloading = false, showVideoWarning = false) }
                    resetDownloadTracking()
                    if (currentTaskId == myTaskId) {
                        currentTaskId = 0
                    }
                    taskCompletedFiles = 0
                    taskFailedFiles = 0
                    hasUserContinuedAfterVideoWarning = false
                }
            }
        }
    }

    fun copyDescription(onResult: (String) -> Unit, onError: (String) -> Unit) {
        val targetUrl = _uiState.value.urlInput.trim()
        if (targetUrl.isEmpty()) {
            onError("请输入链接")
            return
        }
        appendStatus("获取笔记文案中…")
        viewModelScope.launch(Dispatchers.IO) {
            val desc = XHSDownloader(getApplication(), null).getNoteDescription(targetUrl)
            withContext(Dispatchers.Main) {
                if (!desc.isNullOrEmpty()) {
                    appendStatus("已提取文案：\n$desc")

                    // 如果当前有任务ID，则更新任务的笔记内容
                    if (currentTaskId > 0) {
                        val currentTask = TaskManager.getTaskById(currentTaskId)
                        if (currentTask != null) {
                            // 创建更新后的任务，保留原有数据但更新笔记内容
                            val updatedTask = currentTask.copy(noteContent = desc)

                            // 更新TaskManager中的任务
                            TaskManager.updateTask(currentTaskId) { updatedTask }
                        }
                    }

                    onResult(desc)
                } else {
                    appendStatus("未获取到文案")
                    onError("未获取到文案")
                }
            }
        }
    }

    fun onWebCrawlResult(urls: List<String>, content: String?, taskId: Long? = null) {
        appendStatus("onWebCrawlResult 被调用，URL数量: ${urls.size}")

        if (urls.isEmpty()) {
            appendStatus("网页未发现可下载的资源")
            return
        }

        appendStatus("接收到 ${urls.size} 个原始URL")

        // Filter duplicate videos logic
        // 1. Separate videos and images
        // Helper to check if URL is a video
        fun isVideo(url: String): Boolean {
            return url.contains(".mp4") ||
                   url.contains("sns-video") ||
                   url.contains("blob:")
        }

        val (videoUrls, imageUrls) = urls.partition { isVideo(it) }

        appendStatus("分离后: 视频 ${videoUrls.size} 个, 图片 ${imageUrls.size} 个")

        // 2. Deduplicate videos (Prioritize HD from sns-video-bd.xhscdn.com)
        // Since xhs_extractor.js pushes the main video (originVideoKey) first,
        // we can safely prioritize the first video that matches our quality criteria.
        val finalVideoUrls = if (videoUrls.size > 1) {
            val hdVideos = videoUrls.filter { it.contains("sns-video") } // Broadened check
            if (hdVideos.isNotEmpty()) {
                // Determine valid HD videos
                val distinctHd = hdVideos.distinct()

                // User requirement: Keep only the highest quality one.
                // Assuming the first one (from originVideoKey) is the best.
                listOf(distinctHd.first())
            } else {
                // No HD videos found, keep the first available video to avoid duplicates
                listOf(videoUrls.distinct().first())
            }
        } else {
            videoUrls // 0 or 1 video, just keep it
        }

        // 3. Combine and deduplicate everything
        val finalUrls = (imageUrls + finalVideoUrls).distinct()

        appendStatus("去重后最终URL数量: ${finalUrls.size}")

        if (finalUrls.isEmpty()) {
             appendStatus("过滤后未发现有效资源")
             return
        }

        downloadedCount = 0
        totalMediaCount = finalUrls.size

        // Update task status to DOWNLOADING if taskId is provided
        val myTaskId = taskId ?: 0L
        if (myTaskId > 0) {
            TaskManager.updateTaskStatus(myTaskId, TaskStatus.DOWNLOADING)
            currentTaskId = myTaskId
        }

        // Reset download tracking variables for web crawl
        resetDownloadTracking()
        var webCrawlFailedFiles = 0

        _uiState.update {
            it.copy(
                isDownloading = true,
                progressLabel = "$downloadedCount/$totalMediaCount",
                progress = 0f,
                showWebCrawl = false,
                showVideoWarning = false
        )
        }
        updateProgress()
        appendStatus("开始爬取，请等待任务完成")
        
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            val localCompletedFiles = java.util.concurrent.atomic.AtomicInteger(0)
            val localFailedFiles = java.util.concurrent.atomic.AtomicInteger(0)

            val downloader = XHSDownloader(
                getApplication(),
                createDownloadCallback(myTaskId, localCompletedFiles, localFailedFiles, this, isWebCrawl = true)
            )

            // Store reference so cancelCurrentDownload() can signal this downloader
            currentDownloader = downloader

            // If user has continued after video warning, don't stop on video detection
            if (hasUserContinuedAfterVideoWarning) {
                downloader.setShouldStopOnVideo(false)
            } else {
                downloader.setShouldStopOnVideo(true)
            }
            // Reset the stop flag for new download
            downloader.resetStopDownload()

            // For web crawl, we already have the URLs from the WebView, so we don't need to extract them again
            // Use the finalUrls that were passed to this function
            val postIdTemp = currentDownloadStartTime.toString()
            val postId = "webview_$postIdTemp"

            try {
                appendStatus("开始下载 ${finalUrls.size} 个文件")

                finalUrls.forEachIndexed { index, rawUrl ->
                    // Check for cancellation BEFORE starting each file download
                    coroutineContext[Job]?.ensureActive()
                    
                    appendStatus("正在下载第 ${index + 1}/${finalUrls.size} 个文件: $rawUrl")
                    val transformed = downloader.transformXhsCdnUrl(rawUrl).takeUnless { it.isNullOrEmpty() } ?: rawUrl
                    val extension = determineFileExtension(transformed)
                    val fileName = "${postId}_${index + 1}.$extension"
                    appendStatus("准备下载文件: $fileName, URL: $transformed")
                    
                    val success = downloader.downloadFile(rawUrl, fileName)
                    
                    // Check for cancellation AFTER download (in case it was stopped via stopDownload())
                    // We check shouldStopDownload() explicitly to throw immediately even if Job cancellation hasn't propagated yet
                    if (downloader.shouldStopDownload()) {
                        throw CancellationException("Download stopped by user")
                    }
                    coroutineContext[Job]?.ensureActive()
                    
                    appendStatus(
                        if (success) "完成下载第 ${index + 1} 个文件"
                        else "第 ${index + 1} 个文件下载失败"
                    )
                }

                // P2: Perform success logic outside NonCancellable, ensuring we can still respond to cancellation
                coroutineContext[Job]?.ensureActive()
                finalizeTaskCompletion(myTaskId, true, localCompletedFiles.get(), localFailedFiles.get(), isWebCrawl = true)
            } catch (e: Exception) {
                withContext(NonCancellable + Dispatchers.Main) {
                    if (e is CancellationException) {
                        appendStatus("⏹️ 网页爬取已取消")
                        if (myTaskId > 0) {
                            TaskManager.completeTask(myTaskId, false, "下载已取消")
                        }
                    } else {
                        appendStatus("网页爬取出错: ${e.message}")
                        e.printStackTrace()
                        // Mark task as failed if myTaskId was provided
                        if (myTaskId > 0) {
                            TaskManager.completeTask(myTaskId, false, "网页爬取出错: ${e.message}")
                        }
                    }
                }
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    // Only reset UI-related state if this is still the active task
                    if (currentTaskId == myTaskId) {
                        resetDownloadTracking()
                        _uiState.update { it.copy(showWebCrawl = false, isDownloading = false) }
                        currentTaskId = 0
                        taskCompletedFiles = 0
                        taskFailedFiles = 0
                        hasUserContinuedAfterVideoWarning = false
                        currentDownloader = null
                        downloadJob = null
                    }
                }
            }
        }
    }

    fun resetWebCrawlFlag() {
        _uiState.update { it.copy(showWebCrawl = false) }
    }

    fun notifyWebCrawlSuggestion() {
        _uiState.update { it.copy(showWebCrawl = true) }
    }

    fun continueAfterVideoWarning() {
        hasUserContinuedAfterVideoWarning = true
        _uiState.update { it.copy(showVideoWarning = false) }
        // Restart the download with the same URL to continue after video warning
        currentUrl?.let { url ->
            startDownload { msg -> appendStatus("错误: $msg") }
        }
    }

    fun continueTask(task: DownloadTask) {
        updateUrl(task.noteUrl)
        continueAfterVideoWarning()
    }

    fun resetVideoWarning() {
        _uiState.update { it.copy(showVideoWarning = false) }
        hasUserContinuedAfterVideoWarning = false
    }

    private fun createSelectiveCacheCallback(scope: kotlinx.coroutines.CoroutineScope): DownloadCallback {
        return object : DownloadCallback {
            override fun onFileDownloaded(filePath: String) {
                // onFileDownloaded is invoked concurrently from the download thread pool
                // (up to 4 threads). displayedFiles / downloadedCount are not thread-safe,
                // so all shared-state mutations are serialized on the main dispatcher to
                // avoid a race that could randomly drop a cached item from the preview list.
                scope.launch(Dispatchers.Main) {
                    if (displayedFiles.add(filePath)) {
                        downloadedCount++
                        currentFileProgress = 0f
                        val item = CachedMediaItem(
                            path = filePath,
                            displayName = File(filePath).name,
                            type = detectMediaType(filePath)
                        )
                        _uiState.update { state ->
                            state.copy(
                                selectiveDownload = state.selectiveDownload.copy(
                                    items = state.selectiveDownload.items + item
                                )
                            )
                        }
                        updateSelectiveProgress()
                    }
                }
            }

            override fun onDownloadProgress(status: String) {
                scope.launch(Dispatchers.Main) {
                    _uiState.update { state ->
                        state.copy(
                            selectiveDownload = state.selectiveDownload.copy(status = status)
                        )
                    }
                    appendStatus(status)
                }
            }

            override fun onDownloadProgressUpdate(downloaded: Long, total: Long) {
                val fileProgress = if (total > 0) downloaded.toFloat() / total else 0f
                val progressPercent = if (total > 0) (downloaded.toFloat() / total * 100) else 0f
                val currentTime = System.currentTimeMillis()

                scope.launch(Dispatchers.Main) {
                    currentFileProgress = fileProgress

                    if (currentTime - lastSpeedCalculationTime >= 500) {
                        val deltaBytes = if (downloaded >= currentDownloadedBytes) downloaded - currentDownloadedBytes else downloaded
                        val deltaTimeSec = (currentTime - lastSpeedCalculationTime).toDouble() / 1000.0
                        val speedBps = if (deltaTimeSec > 0) deltaBytes / deltaTimeSec else 0.0
                        lastCalculatedSpeed = formatSpeed(speedBps)
                        lastSpeedCalculationTime = currentTime
                    }
                    currentDownloadedBytes = downloaded
                    currentDownloadTotalBytes = total
                    updateSelectiveProgress()

                    _uiState.update { state ->
                        state.copy(
                            selectiveDownload = state.selectiveDownload.copy(
                                progressText = "${String.format("%.1f", progressPercent)}%｜$lastCalculatedSpeed"
                            )
                        )
                    }
                }
            }

            override fun onDownloadError(status: String, originalUrl: String) {
                scope.launch(Dispatchers.Main) {
                    appendStatus("错误：$status ($originalUrl)")
                    _uiState.update { state ->
                        state.copy(
                            selectiveDownload = state.selectiveDownload.copy(
                                status = status,
                                errorMessage = status
                            )
                        )
                    }
                }
            }

            override fun onVideoDetected() {
                scope.launch(Dispatchers.Main) {
                    appendStatus("检测到视频文件，继续缓存...")
                }
            }

            override fun isCancelled(): Boolean = !scope.isActive
        }
    }

    private fun updateSelectiveProgress() {
        val label = if (totalMediaCount > 0) {
            "$downloadedCount/$totalMediaCount"
        } else {
            "$downloadedCount/?"
        }
        val calculatedProgress = if (totalMediaCount > 0) {
            (downloadedCount + currentFileProgress) / totalMediaCount.toFloat()
        } else {
            0f
        }
        val overallProgress = maxOf(calculatedProgress, lastOverallProgress)
        lastOverallProgress = overallProgress

        _uiState.update { state ->
            state.copy(
                progressLabel = label,
                progress = overallProgress,
                selectiveDownload = state.selectiveDownload.copy(
                    progressLabel = label,
                    progress = overallProgress
                )
            )
        }
    }

    private fun resetSelectiveDownloadState() {
        _uiState.update {
            it.copy(
                isDownloading = false,
                progressLabel = "",
                progress = 0f,
                downloadProgressText = "0.0%｜0KB/s",
                selectiveDownload = SelectiveDownloadUiState()
            )
        }
        resetDownloadTracking()
        displayedFiles.clear()
        downloadedCount = 0
        totalMediaCount = 0
        currentDownloader = null
        downloadJob = null
    }

    private fun cleanupSelectiveCache(cacheDir: String?) {
        if (!cacheDir.isNullOrEmpty()) {
            runCatching { File(cacheDir).deleteRecursively() }
        }
    }

    private fun addMedia(filePath: String) {
        val type = detectMediaType(filePath)
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems + MediaItem(filePath, type))
        }
    }

    fun removeMediaItem(mediaItem: MediaItem) {
        _uiState.update { state ->
            state.copy(mediaItems = state.mediaItems.filter { it.path != mediaItem.path })
        }
    }

    private fun runDownloadWithCancellationCheck(downloader: XHSDownloader, targetUrl: String, job: Job?): Boolean {
        // Create a thread to run the download
        var result = false
        val thread = Thread {
            result = downloader.downloadContent(targetUrl)
        }
        thread.start()

        // Periodically check if the SPECIFIC job was cancelled
        while (thread.isAlive) {
            if (job?.isActive == false) {
                downloader.stopDownload()
                // Interrupt the thread
                thread.interrupt()
                // Wait a bit for the thread to respond to interruption
                Thread.sleep(100)
                break
            }
            Thread.sleep(100) // Check every 100ms
        }

        // Wait for thread to finish gracefully
        try {
            thread.join(1000) // Wait up to 1 second for graceful shutdown
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        // If the job was cancelled, throw a CancellationException to jump to the catch block.
        // This prevents the caller from incorrectly marking the task as "FAILED" with a generic error message.
        // Using ensureActive() is the standard way to check for cancellation and throw the exception.
        job?.ensureActive()

        return result
    }

    private fun detectMediaType(filePath: String): MediaType {
        val lower = filePath.lowercase(Locale.getDefault())
        return when {
            lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi") ||
                lower.endsWith(".mkv") || lower.contains("sns-video") || lower.contains("video") -> MediaType.VIDEO

            lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".gif") || lower.endsWith(".webp") -> MediaType.IMAGE

            else -> MediaType.OTHER
        }
    }

    private fun determineFileExtension(url: String?): String {
        val lower = url?.lowercase(Locale.getDefault()) ?: return "jpg"
        return when {
            lower.contains(".mp4") -> "mp4"
            lower.contains(".mov") -> "mov"
            lower.contains(".avi") -> "avi"
            lower.contains(".mkv") -> "mkv"
            lower.contains(".webm") -> "webm"
            lower.contains(".png") -> "png"
            lower.contains(".gif") -> "gif"
            lower.contains(".webp") -> "webp"
            lower.contains("video") || lower.contains("sns-video") -> "mp4"
            else -> "jpg"
        }
    }

    private fun updateProgress() {
        val label = if (totalMediaCount > 0) {
            "$downloadedCount/$totalMediaCount"
        } else {
            "$downloadedCount/?"
        }
        val calculatedProgress = if (totalMediaCount > 0) {
            // Calculate progress as (completed files + current file progress) / total files
            (downloadedCount + currentFileProgress) / totalMediaCount.toFloat()
        } else {
            0f
        }

        // Ensure progress doesn't regress (go backwards)
        val overallProgress = maxOf(calculatedProgress, lastOverallProgress)
        lastOverallProgress = overallProgress

        _uiState.update { it.copy(progressLabel = label, progress = overallProgress) }

        // Also update the corresponding task in TaskManager if it exists
        if (currentTaskId > 0) {
            // Calculate the new task progress based on taskCompletedFiles and taskCurrentFileProgress
            val newTaskProgress = if (totalMediaCount > 0) {
                (taskCompletedFiles + taskCurrentFileProgress) / totalMediaCount.toFloat()
            } else {
                0f
            }

            // Ensure task progress doesn't regress by using maxTaskProgress
            maxTaskProgress = maxOf(maxTaskProgress, newTaskProgress)

            // Update task progress with throttling
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTaskProgressUpdateTime >= TASK_PROGRESS_UPDATE_INTERVAL) {
                TaskManager.updateProgress(
                    currentTaskId,
                    taskCompletedFiles,
                    taskFailedFiles,
                    taskCurrentFileProgress
                )
                lastTaskProgressUpdateTime = currentTime
            }
        }
    }

    private fun appendStatus(message: String) {
        _uiState.update { it.copy(status = it.status + message) }
        Log.d("XHSDownloader", message)
        showDebugNotification(message)
    }

    private fun showDebugNotification(message: String) {
        val appContext = getApplication<Application>()
        val title = appContext.getString(R.string.debug_notifications)
        val important = isImportantStatusMessage(message)
        NotificationHelper.showOrUpdateDebugNotification(appContext, title, message, important)
    }

    private fun isImportantStatusMessage(message: String): Boolean {
        if (message.contains("✅") || message.contains("❌") || message.contains("⏹️")) {
            return true
        }
        return debugNotificationImportantKeywords.any { keyword -> message.contains(keyword) }
    }

    private fun extractTitleFromUrl(url: String): String? {
        // Extract title from URL if possible, otherwise return null
        return null // For now, return null - the actual title extraction would happen elsewhere
    }

    fun clearHistory() {
        viewModelScope.launch {
            TaskManager.clearAllTasks()
            appendStatus("历史记录已清除")
        }
    }

    /**
     * 抖音：直链解析兜底（WebView 取不到资源或用户手动触发"直链解析"）。
     * 流程：DouyinParser 解析分享链接 → 拿到去水印播放地址 → FileDownloader 下载。
     */
    fun startDouyinDownload(shareUrl: String) {
        val rawInput = shareUrl.trim()
        if (rawInput.isEmpty()) {
            appendStatus("❌ 输入为空")
            return
        }

        // 抖音分享文案通常混有标题和短链，先提取真正的 http/https 链接
        val targetUrl = UrlUtils.extractFirstUrl(rawInput) ?: rawInput
        if (!targetUrl.startsWith("http://", true) && !targetUrl.startsWith("https://", true)) {
            DownloadLogger.logFailure(getApplication(), "douyin", rawInput, "无法从剪贴板提取有效链接: $rawInput")
            appendStatus("❌ 无法提取有效链接，请确认复制的是分享文案")
            return
        }

        val myTaskId = TaskManager.createTask(
            noteUrl = targetUrl,
            noteTitle = null,
            noteType = NoteType.VIDEO,
            totalFiles = 1,
            source = "douyin"
        )
        TaskManager.startTask(myTaskId)
        currentTaskId = myTaskId
        totalMediaCount = 1
        downloadedCount = 0
        displayedFiles.clear()
        resetDownloadTracking()
        _uiState.update {
            it.copy(
                isDownloading = true,
                status = listOf("抖音解析中：$targetUrl"),
                mediaItems = emptyList(),
                progressLabel = "0/1",
                progress = 0f
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val info = runCatching { DouyinParser.parse(targetUrl) }.getOrElse { e ->
                DownloadLogger.logFailure(getApplication(), "douyin", targetUrl, "解析失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    appendStatus("❌ 抖音解析失败: ${e.message}")
                    appendStatus("日志已保存至: ${DownloadLogger.getLogFilePath(getApplication())}")
                    TaskManager.completeTask(myTaskId, false, "解析失败: ${e.message}")
                    resetDownloadTracking()
                    if (currentTaskId == myTaskId) currentTaskId = 0
                    _uiState.update { it.copy(isDownloading = false) }
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                appendStatus("抖音解析成功，开始下载：${info.title}")
            }

            val fileName = "${info.title}.mp4"
            val callback = createDownloadCallback(
                myTaskId,
                java.util.concurrent.atomic.AtomicInteger(0),
                java.util.concurrent.atomic.AtomicInteger(0),
                this
            )
            val downloader = FileDownloader(getApplication(), callback)
            // 照搬 DouyinDL：下载只用随机 UA，不带 Referer（传空字符串）
            val success = runCatching {
                downloader.downloadFile(info.videoUrl, fileName, "", info.userAgent)
            }.getOrElse { e ->
                DownloadLogger.logFailure(getApplication(), "douyin", info.videoUrl, "下载异常: ${e.message}")
                withContext(Dispatchers.Main) { appendStatus("❌ 抖音下载出错: ${e.message}") }
                false
            }

            withContext(Dispatchers.Main) {
                if (!success) {
                    DownloadLogger.logFailure(getApplication(), "douyin", info.videoUrl, "下载失败(返回false)")
                    appendStatus("❌ 抖音下载失败，日志已保存至: ${DownloadLogger.getLogFilePath(getApplication())}")
                } else {
                    appendStatus("✅ 抖音下载完成")
                }
                TaskManager.completeTask(myTaskId, success, if (success) null else "下载失败")
                resetDownloadTracking()
                if (currentTaskId == myTaskId) currentTaskId = 0
                _uiState.update { it.copy(isDownloading = false) }
            }
        }
    }

    /**
     * 抖音：WebView 已成功提取到直链视频时，直接下载提取到的地址（已去水印）。
     * WebView 已创建的任务会复用，否则新建任务。
     */
    fun startDouyinDownloadFromExtracted(urls: List<String>, content: String?, taskId: Long?) {
        val videoUrl = urls.firstOrNull { it.startsWith("http") } ?: run {
            appendStatus("❌ 抖音未提取到有效视频地址")
            return
        }

        val title = deriveDouyinTitle(content)
        val myTaskId = if (taskId != null && taskId > 0) {
            TaskManager.updateTaskStatus(taskId, TaskStatus.DOWNLOADING)
            taskId
        } else {
            val t = TaskManager.createTask(
                noteUrl = videoUrl,
                noteTitle = title,
                noteType = NoteType.VIDEO,
                totalFiles = 1,
                noteContent = content,
                source = "douyin"
            )
            TaskManager.startTask(t)
            t
        }

        currentTaskId = myTaskId
        totalMediaCount = 1
        downloadedCount = 0
        displayedFiles.clear()
        resetDownloadTracking()
        _uiState.update {
            it.copy(
                isDownloading = true,
                status = listOf("抖音下载中：$title"),
                mediaItems = emptyList(),
                progressLabel = "0/1",
                progress = 0f
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val fileName = "$title.mp4"
            val callback = createDownloadCallback(
                myTaskId,
                java.util.concurrent.atomic.AtomicInteger(0),
                java.util.concurrent.atomic.AtomicInteger(0),
                this
            )
            val downloader = FileDownloader(getApplication(), callback)
            val success = runCatching {
                downloader.downloadFile(videoUrl, fileName, DouyinParser.REFERER, DouyinParser.MOBILE_UA)
            }.getOrElse { e ->
                withContext(Dispatchers.Main) { appendStatus("❌ 抖音下载出错: ${e.message}") }
                false
            }

            withContext(Dispatchers.Main) {
                TaskManager.completeTask(myTaskId, success, if (success) null else "下载失败")
                appendStatus(if (success) "✅ 抖音下载完成" else "❌ 抖音下载失败")
                resetDownloadTracking()
                if (currentTaskId == myTaskId) currentTaskId = 0
            }
        }
    }

    private fun deriveDouyinTitle(content: String?): String {
        val raw = content?.lines()?.firstOrNull { it.isNotBlank() } ?: ""
        return raw
            .replace(Regex("""[\\/:*?"<>|#\n\r]"""), "_")
            .replace(Regex("""\.{2,}"""), ".")
            .trim(' ', '.')
            .take(80)
            .ifEmpty { "douyin_${System.currentTimeMillis()}" }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("xhsdn", text))
    }

    /**
     * Creates a unified DownloadCallback for any download task.
     */
    private fun createDownloadCallback(
        taskId: Long,
        completedFiles: java.util.concurrent.atomic.AtomicInteger,
        failedFiles: java.util.concurrent.atomic.AtomicInteger,
        scope: kotlinx.coroutines.CoroutineScope,
        isWebCrawl: Boolean = false
    ): DownloadCallback {
        return object : DownloadCallback {
            override fun onFileDownloaded(filePath: String) {
                val completed = completedFiles.incrementAndGet()
                TaskManager.updateProgress(taskId, completed, failedFiles.get(), 0f)
                TaskManager.addFilePath(taskId, filePath)

                if (taskId == currentTaskId) {
                    scope.launch(Dispatchers.Main) {
                        if (displayedFiles.add(filePath)) {
                            addMedia(filePath)
                            downloadedCount++
                            taskCompletedFiles = completedFiles.get()
                            currentFileProgress = 0f
                            taskCurrentFileProgress = 0f
                            updateProgress()
                        }
                    }
                }
            }

            override fun onDownloadProgress(status: String) {
                if (taskId == currentTaskId) {
                    scope.launch(Dispatchers.Main) {
                        appendStatus(status)
                    }
                }
            }

            override fun onDownloadProgressUpdate(downloaded: Long, total: Long) {
                val progressPercent = if (total > 0) (downloaded.toFloat() / total * 100) else 0f
                val fileProgress = if (total > 0) downloaded.toFloat() / total else 0f

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTaskProgressUpdateTime >= TASK_PROGRESS_UPDATE_INTERVAL) {
                    TaskManager.updateProgress(taskId, completedFiles.get(), failedFiles.get(), fileProgress)
                    lastTaskProgressUpdateTime = currentTime
                }

                if (taskId == currentTaskId) {
                    scope.launch(Dispatchers.Main) {
                        currentFileProgress = fileProgress
                        taskCurrentFileProgress = fileProgress
                        
                        if (currentTime - lastSpeedCalculationTime >= 500) {
                            val deltaBytes = if (downloaded >= currentDownloadedBytes) downloaded - currentDownloadedBytes else downloaded
                            val deltaTimeSec = (currentTime - lastSpeedCalculationTime).toDouble() / 1000.0
                            val speedBps = if (deltaTimeSec > 0) deltaBytes / deltaTimeSec else 0.0
                            lastCalculatedSpeed = formatSpeed(speedBps)
                            lastSpeedCalculationTime = currentTime
                        }
                        currentDownloadedBytes = downloaded
                        currentDownloadTotalBytes = total
                        updateProgress()

                        val progressText = "${String.format("%.1f", progressPercent)}%｜$lastCalculatedSpeed"
                        _uiState.update { it.copy(downloadProgressText = progressText) }
                    }
                }
            }

            override fun onDownloadError(status: String, originalUrl: String) {
                if (isTerminalDownloadError(status)) {
                    val failed = failedFiles.incrementAndGet()
                    TaskManager.updateProgress(taskId, completedFiles.get(), failed, 0f)
                    DownloadLogger.logFailure(getApplication(), "task#$taskId", originalUrl, status)
                    if (taskId == currentTaskId) {
                        taskFailedFiles = failed
                        taskCurrentFileProgress = 0f
                    }
                }
                
                if (taskId == currentTaskId) {
                    scope.launch(Dispatchers.Main) {
                        appendStatus("错误：$status ($originalUrl)")
                        if (isTerminalDownloadError(status)) {
                            appendStatus("失败详情见日志（更多 → 失败日志）：\n${DownloadLogger.getLogFilePath(getApplication())}")
                        }
                        if (!isWebCrawl && (status.contains("No media URLs found", true) ||
                            status.contains("Failed to fetch post details", true) ||
                            status.contains("Could not extract post ID", true))) {
                            _uiState.update { it.copy(showWebCrawl = true) }
                        }
                    }
                }
            }

            override fun onVideoDetected() {
                TaskManager.updateTaskType(taskId, NoteType.VIDEO)
                if (taskId == currentTaskId) {
                    val shouldPauseForVideoChoice = !isWebCrawl && !hasUserContinuedAfterVideoWarning
                    if (shouldPauseForVideoChoice) {
                        TaskManager.updateTaskStatus(taskId, TaskStatus.WAITING_FOR_USER, "检测到视频，请选择下载方式")
                    }

                    viewModelScope.launch(Dispatchers.Main) {
                        if (isWebCrawl) {
                            _uiState.update { it.copy(showVideoWarning = true) }
                        } else if (shouldPauseForVideoChoice) {
                            _uiState.update { it.copy(showVideoWarning = true, isDownloading = false) }
                            appendStatus("下载因检测到视频而停止")
                            appendStatus("提示：检测到视频，请选择坚持下载(720P)或网页爬取")
                        } else {
                            appendStatus("检测到视频文件，继续下载...")
                        }
                    }

                    if (shouldPauseForVideoChoice) {
                        downloadJob?.cancel(CancellationException("WAITING_FOR_USER"))
                    }
                }
            }

            override fun isCancelled(): Boolean = !scope.isActive
        }
    }

    /**
     * Finalizes task status and updates UI.
     */
    private suspend fun finalizeTaskCompletion(
        taskId: Long,
        success: Boolean,
        completedCount: Int,
        failedCount: Int,
        isWebCrawl: Boolean = false
    ) {
        val isStrictSuccess = success && failedCount == 0 && completedCount > 0
        val errorMsg = when {
            isStrictSuccess -> null
            !success -> if (isWebCrawl) "网页爬取中断" else "解析或下载中断"
            completedCount == 0 -> "未发现可下载资源"
            failedCount > 0 -> "部分下载失败 ($failedCount)"
            else -> "下载过程异常"
        }
        
        TaskManager.completeTask(taskId, isStrictSuccess, errorMsg)

        if (taskId == currentTaskId) {
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isDownloading = false) }
                val uiStatus = when {
                    isStrictSuccess -> "✅ 下载完成"
                    failedCount > 0 && completedCount > 0 -> "⚠️ 部分下载失败 ($failedCount)"
                    else -> "❌ 下载失败"
                }
                appendStatus(uiStatus)
            }
        }
    }
}
