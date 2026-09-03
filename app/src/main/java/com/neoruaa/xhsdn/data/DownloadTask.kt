package com.neoruaa.xhsdn.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * 下载任务状态
 */
enum class TaskStatus {
    QUEUED,      // 排队中
    DOWNLOADING, // 下载中
    COMPLETED,   // 下载完成
    FAILED,      // 下载失败
    WAITING_FOR_USER // 等待用户操作 (如视频选择)
}

/**
 * 笔记类型
 */
enum class NoteType {
    IMAGE,  // 图文笔记
    VIDEO,  // 视频笔记
    UNKNOWN // 未知
}

/**
 * 下载任务数据类
 */
data class DownloadTask(
    val id: Long,
    val noteUrl: String,           // 笔记链接
    val noteTitle: String?,        // 笔记标题
    val noteType: NoteType,        // 笔记类型
    val totalFiles: Int,           // 总文件数
    val completedFiles: Int = 0,   // 已完成文件数
    val failedFiles: Int = 0,      // 失败文件数
    val currentFileProgress: Float = 0f, // 当前文件下载进度 (0.0 to 1.0)
    val status: TaskStatus,        // 任务状态
    val createdAt: Long,           // 创建时间
    val completedAt: Long? = null, // 完成时间
    val errorMessage: String? = null, // 错误信息
    val filePaths: List<String> = emptyList(), // 下载的文件路径列表
    val noteContent: String? = null, // 笔记内容
    val source: String = "xhs" // 来源平台: "xhs"(小红书) / "douyin"(抖音)
) {
    val progress: Float
        get() = if (totalFiles > 0) {
            val calculatedProgress = (completedFiles + currentFileProgress) / totalFiles.toFloat()
            // Ensure progress is between 0.0 and 1.0
            calculatedProgress.coerceIn(0f, 1f)
        } else 0f
    
    val isActive: Boolean
        get() = status == TaskStatus.QUEUED || status == TaskStatus.DOWNLOADING || status == TaskStatus.WAITING_FOR_USER
    
    val isCompleted: Boolean
        get() = status == TaskStatus.COMPLETED || status == TaskStatus.FAILED
    
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("noteUrl", noteUrl)
            put("noteTitle", noteTitle ?: "")
            put("noteType", noteType.name)
            put("totalFiles", totalFiles)
            put("completedFiles", completedFiles)
            put("failedFiles", failedFiles)
            put("currentFileProgress", currentFileProgress)
            put("status", status.name)
            put("createdAt", createdAt)
            put("completedAt", completedAt ?: 0L)
            put("errorMessage", errorMessage ?: "")
            put("filePaths", JSONArray(filePaths))
            put("noteContent", noteContent ?: "")
            put("source", source)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DownloadTask {
            return DownloadTask(
                id = json.getLong("id"),
                noteUrl = json.getString("noteUrl"),
                noteTitle = json.getString("noteTitle").takeIf { it.isNotEmpty() },
                noteType = try { NoteType.valueOf(json.getString("noteType")) } catch (e: Exception) { NoteType.UNKNOWN },
                totalFiles = json.getInt("totalFiles"),
                completedFiles = json.optInt("completedFiles", 0),
                failedFiles = json.optInt("failedFiles", 0),
                currentFileProgress = json.optDouble("currentFileProgress", 0.0).toFloat(),
                status = try { TaskStatus.valueOf(json.getString("status")) } catch (e: Exception) { TaskStatus.COMPLETED },
                createdAt = json.getLong("createdAt"),
                completedAt = json.optLong("completedAt", 0L).takeIf { it > 0 },
                errorMessage = json.optString("errorMessage").takeIf { it.isNotEmpty() },
                filePaths = json.optJSONArray("filePaths")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                noteContent = json.optString("noteContent").takeIf { it.isNotEmpty() },
                source = json.optString("source").takeIf { it.isNotEmpty() } ?: "xhs"
            )
        }
    }
}

/**
 * 同链接查重决策结果
 * @param taskId 可复用的已存在任务 ID；null 表示无同链接任务（应新建）
 * @param ignore  true=同链接任务正在下载中，本次触发应直接忽略
 */
data class DownloadPlan(
    val taskId: Long?,
    val ignore: Boolean
)

/**
 * 任务管理器 - SharedPreferences 持久化版本
 */
object TaskManager {
    private const val PREFS_NAME = "task_history"
    private const val KEY_TASKS = "tasks"
    private const val KEY_NEXT_ID = "next_id"
    
    private var prefs: SharedPreferences? = null
    private var nextId = 1L
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    
    /**
     * 初始化 TaskManager（在 Application 或 Activity 的 onCreate 中调用）
     */
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadTasks()
        }
    }
    
    private fun loadTasks() {
        prefs?.let { p ->
            nextId = p.getLong(KEY_NEXT_ID, 1L)
            val tasksJson = p.getString(KEY_TASKS, "[]") ?: "[]"
            try {
                val jsonArray = JSONArray(tasksJson)
                val tasks = mutableListOf<DownloadTask>()
                for (i in 0 until jsonArray.length()) {
                    try {
                        tasks.add(DownloadTask.fromJson(jsonArray.getJSONObject(i)))
                    } catch (e: Exception) {
                        // Skip invalid task
                    }
                }
                _tasks.value = tasks
            } catch (e: Exception) {
                _tasks.value = emptyList()
            }
            // 进程启动清扫：上一次进程崩溃/被杀前遗留的「活跃态」任务收编为失败，
            // 避免重启后 UI 出现永远转圈的「卡在停止」僵尸卡（重启后下载链路已断，不可能再续传）。
            reapStaleActiveTasks()
        }
    }

    /**
     * 链接归一化查重键：把抖音/快手/小红书的分享短链/长链归一到「作品条目 ID」维度，
     * 用于同链接查重（用户确认语义：复用同一条任务，不堆重复记录）。
     * 取不到 ID 时回退「host + path」（分享短链本身保持原样即可精确匹配同一分享码）。
     */
    fun normalizeUrlKey(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val url = raw.trim().substringBefore('#')
        // 平台作品 ID（douyin video/note、modal_id、xhs explore/discovery、kuaishou short-video/photo）
        val idPatterns = arrayOf(
            Regex("""douyin\.com/(?:video|note)/(\d+)"""),
            Regex("""iesdouyin\.com/share/(?:video|note)/(\d+)"""),
            Regex("""modal_id=(\d+)"""),
            Regex("""xiaohongshu\.com/(?:explore|discovery/item)/([0-9A-Za-z]+)"""),
            Regex("""kuaishou\.com/(?:short-video|photo)/([0-9A-Za-z]+)""")
        )
        for (re in idPatterns) {
            re.find(url)?.groupValues?.getOrNull(1)?.let { id ->
                return when {
                    url.contains("douyin.com") || url.contains("iesdouyin") -> "douyin:$id"
                    url.contains("xiaohongshu") -> "xhs:$id"
                    url.contains("kuaishou") -> "kuaishou:$id"
                    else -> "item:$id"
                }
            }
        }
        // 回退：host + path（去协议/查询参数/尾斜杠）。短链 v.douyin.com/xxx 等无 ID 时按分享码精确匹配。
        return runCatching {
            val u = java.net.URI(if (url.startsWith("http", true)) url else "https://$url")
            val host = u.host?.lowercase()?.removePrefix("www.")?.removePrefix("m.") ?: url.lowercase()
            val path = u.path?.trimEnd('/') ?: ""
            "$host$path"
        }.getOrElse { url.trim().lowercase() }
    }
    
    private fun saveTasks() {
        prefs?.edit()?.apply {
            val jsonArray = JSONArray()
            _tasks.value.forEach { task ->
                jsonArray.put(task.toJson())
            }
            putString(KEY_TASKS, jsonArray.toString())
            putLong(KEY_NEXT_ID, nextId)
            apply()
        }
    }
    
    /**
     * 获取所有任务（按创建时间降序）
     * 同一链接（按 ID 归一化）仅保留最新一条，避免重复记录刷屏。
     */
    fun getAllTasks(): Flow<List<DownloadTask>> = _tasks.map { tasks ->
        tasks.groupBy { normalizeUrlKey(it.noteUrl).ifBlank { it.noteUrl } }
            .mapValues { (_, list) -> list.maxByOrNull { it.createdAt } ?: list.first() }
            .values
            .sortedByDescending { it.createdAt }
    }
    
    /**
     * 根据 ID 获取任务
     */
    fun getTaskById(taskId: Long): DownloadTask? {
        return _tasks.value.find { it.id == taskId }
    }
    
    /**
     * 获取进行中的任务
     */
    fun getActiveTasks(): Flow<List<DownloadTask>> = _tasks.map {
        it.filter { task -> task.isActive }.sortedByDescending { task -> task.createdAt }
    }
    
    /**
     * 获取已完成的任务
     */
    fun getCompletedTasks(): Flow<List<DownloadTask>> = _tasks.map {
        it.filter { task -> task.isCompleted }.sortedByDescending { task -> task.createdAt }
    }
    
    /**
     * 创建新任务
     */
    fun createTask(
        noteUrl: String,
        noteTitle: String?,
        noteType: NoteType,
        totalFiles: Int,
        noteContent: String? = null,
        source: String = "xhs"
    ): Long {
        val taskId = nextId++
        val task = DownloadTask(
            id = taskId,
            noteUrl = noteUrl,
            noteTitle = noteTitle,
            noteType = noteType,
            totalFiles = totalFiles,
            status = TaskStatus.QUEUED,
            createdAt = System.currentTimeMillis(),
            noteContent = noteContent,
            source = source
        )
        _tasks.value = _tasks.value + task
        saveTasks()
        return taskId
    }
    
    /**
     * 开始下载任务
     */
    fun startTask(taskId: Long) {
        updateTask(taskId) { it.copy(status = TaskStatus.DOWNLOADING) }
    }
    
    /**
     * 更新任务进度
     */
    fun updateProgress(taskId: Long, completedFiles: Int, failedFiles: Int, currentFileProgress: Float = 0f) {
        updateTask(taskId) { task ->
            val totalCompleted = completedFiles + failedFiles
            val newStatus = when {
                totalCompleted >= task.totalFiles -> {
                    if (failedFiles > 0) TaskStatus.FAILED else TaskStatus.COMPLETED
                }
                else -> TaskStatus.DOWNLOADING
            }

            // Calculate the new progress to compare with the current progress
            val newProgress = if (task.totalFiles > 0) {
                (completedFiles + currentFileProgress) / task.totalFiles.toFloat()
            } else {
                0f
            }

            val currentProgress = if (task.totalFiles > 0) {
                (task.completedFiles + task.currentFileProgress) / task.totalFiles.toFloat()
            } else {
                0f
            }

            // Only update if the new progress is greater than or equal to the current progress to prevent regression
            val shouldUpdate = newProgress >= currentProgress

            if (shouldUpdate) {
                task.copy(
                    status = newStatus,
                    completedFiles = completedFiles,
                    failedFiles = failedFiles,
                    currentFileProgress = currentFileProgress,
                    completedAt = if (newStatus in listOf(TaskStatus.COMPLETED, TaskStatus.FAILED))
                        System.currentTimeMillis()
                    else null,
                    errorMessage = if (failedFiles > 0) "部分文件下载失败" else null
                )
            } else {
                // Return the task unchanged to prevent progress regression
                task
            }
        }
    }

    /**
     * 添加文件路径到任务
     */
    fun addFilePath(taskId: Long, path: String) {
        updateTask(taskId) { task ->
            task.copy(filePaths = task.filePaths + path)
        }
    }
    
    /**
     * 标记任务完成
     */
    fun completeTask(taskId: Long, success: Boolean, errorMessage: String? = null) {
        val url = getTaskById(taskId)?.noteUrl
        updateTask(taskId) { task ->
            task.copy(
                status = if (success) TaskStatus.COMPLETED else TaskStatus.FAILED,
                completedAt = System.currentTimeMillis(),
                errorMessage = errorMessage
            )
        }
        // 成功后清除同链接（归一化）的其他「失败」记录（失败任务重试成功 → 自动删掉那条失败的）
        if (success && url != null) {
            val key = normalizeUrlKey(url)
            val before = _tasks.value
            val after = before.filterNot {
                it.id != taskId && it.status == TaskStatus.FAILED &&
                    (if (key.isNotBlank()) normalizeUrlKey(it.noteUrl) == key else it.noteUrl == url)
            }
            if (after.size != before.size) {
                _tasks.value = after
                saveTasks()
            }
        }
    }

    /**
     * 链接是否命中同一作品（归一化比较；取不到 ID 时退化为原始串比较）
     */
    private fun isSameItemUrl(taskUrl: String, targetUrl: String): Boolean {
        val key = normalizeUrlKey(targetUrl)
        val taskKey = normalizeUrlKey(taskUrl)
        return if (key.isNotBlank() && taskKey.isNotBlank()) taskKey == key
        else taskUrl == targetUrl
    }
    
    /**
     * 删除任务
     */
    fun deleteTask(taskId: Long) {
        _tasks.value = _tasks.value.filter { it.id != taskId }
        saveTasks()
    }
    
    /**
     * 清空所有任务
     */
    fun clearAllTasks() {
        _tasks.value = emptyList()
        saveTasks()
    }
    
    /**
     * 获取当前进行中的任务
     */
    fun getCurrentActiveTask(): DownloadTask? {
        return _tasks.value.firstOrNull { it.status == TaskStatus.DOWNLOADING }
    }

    /**
     * 检查是否存在最近的相同任务 (防止重复下载)
     * @param url 笔记链接
     * @param durationMillis 时间阈值 (默认 1 小时)，在此时间内已创建的任务如果在进行中或已完成，则视为存在
     */
    fun hasRecentTask(url: String, durationMillis: Long = 3600_000): Boolean {
        val threshold = System.currentTimeMillis() - durationMillis
        return _tasks.value.any { task ->
            // URL 相同（归一化）且 (任务活跃 或 是最近创建的)
            isSameItemUrl(task.noteUrl, url) && (task.isActive || task.createdAt > threshold)
        }
    }
    
    /**
     * 根据 URL 查找最近的非终态任务 ID（用于下载入口去重，避免同链接创建多条记录）
     * @return 匹配的任务 ID，若无匹配则返回 null
     */
    fun findActiveTaskIdByUrl(url: String): Long? {
        return _tasks.value.find { task ->
            isSameItemUrl(task.noteUrl, url) && !task.isCompleted
        }?.id
    }

    /**
     * 同链接查重决策（用户确认语义：复用同一条任务）。
     * @return [DownloadPlan]：ignore=true 表示已有同链接任务正在下载中，应忽略本次触发；
     *         taskId != null 表示已有同链接终态(完成/失败)任务，可原地 resetTask 复用重下；
     *         taskId == null 表示无同链接任务，应新建。
     */
    fun planDownloadTask(url: String): DownloadPlan {
        val existing = _tasks.value
            .filter { isSameItemUrl(it.noteUrl, url) }
            .maxByOrNull { it.createdAt }
            ?: return DownloadPlan(null, false)
        if (!existing.isCompleted) return DownloadPlan(existing.id, true)
        // 复用同一条终态任务前，顺手清掉同链接更早的失败旧记录，保持列表单条可追溯
        val before = _tasks.value
        val pruned = before.filterNot {
            it.id != existing.id && it.status == TaskStatus.FAILED && isSameItemUrl(it.noteUrl, url)
        }
        if (pruned.size != before.size) {
            _tasks.value = pruned
            saveTasks()
        }
        return DownloadPlan(existing.id, false)
    }

    /**
     * 清除指定页签范围内已成功(COMPLETED)的任务；失败/下载中/排队中一律保留（可继续重试）。
     */
    fun clearCompletedTasks(scopeFilter: (DownloadTask) -> Boolean) {
        val before = _tasks.value
        val after = before.filterNot { it.status == TaskStatus.COMPLETED && scopeFilter(it) }
        if (after.size != before.size) {
            _tasks.value = after
            saveTasks()
        }
    }

    /**
     * 仅当任务仍处于活跃态时置为 FAILED（终态兜底）。
     * 用于下载协程异常/进程被杀后的收尾：绝不覆盖已完成的成功/已取消状态（避免竞态）。
     */
    fun failIfActive(taskId: Long, errorMessage: String?) {
        updateTask(taskId) { task ->
            if (task.isActive) {
                task.copy(
                    status = TaskStatus.FAILED,
                    errorMessage = errorMessage,
                    completedAt = System.currentTimeMillis()
                )
            } else {
                task
            }
        }
    }

    /**
     * 进程启动清扫：把遗留的「活跃态(排队中/下载中/等待用户)」任务收编为失败。
     * 超过 [maxAgeMillis] 仍未完成说明其下载链路已随上次进程退出而中断（不可能续传），
     * 直接标记失败让用户可见、可重试，杜绝「卡在停止」的僵尸卡片。
     */
    fun reapStaleActiveTasks(maxAgeMillis: Long = 15 * 60_000L) {
        val cutoff = System.currentTimeMillis() - maxAgeMillis
        val before = _tasks.value
        val after = before.map { task ->
            if (task.isActive && task.createdAt < cutoff) {
                task.copy(
                    status = TaskStatus.FAILED,
                    errorMessage = "下载中断（进程退出），可重试",
                    completedAt = System.currentTimeMillis()
                )
            } else {
                task
            }
        }
        if (after != before) {
            _tasks.value = after
            saveTasks()
        }
    }
    
    /**
     * Update task type (e.g., when video is detected)
     */
    fun updateTaskType(taskId: Long, noteType: NoteType) {
        updateTask(taskId) { it.copy(noteType = noteType) }
    }

    /**
     * 重置任务以供重试（清空进度、文件、错误信息）
     */
    fun resetTask(taskId: Long) {
        updateTask(taskId) { task ->
            task.copy(
                status = TaskStatus.DOWNLOADING,
                completedFiles = 0,
                failedFiles = 0,
                filePaths = emptyList(),
                errorMessage = null,
                completedAt = null
            )
        }
    }

    /**
     * 更新任务状态和错误信息
     */
    fun updateTaskStatus(taskId: Long, status: TaskStatus, errorMessage: String? = null) {
        updateTask(taskId) { it.copy(status = status, errorMessage = errorMessage) }
    }

    /**
     * 通用任务更新函数
     */
    fun updateTask(taskId: Long, update: (DownloadTask) -> DownloadTask) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) update(task) else task
        }
        saveTasks()
    }
}
