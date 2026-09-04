package com.neoruaa.xhsdn.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 操作级埋码（events.log）：记录每个功能点的用户操作，供导出分析「哪些功能点用得少可去掉」。
 *
 * 与 [DownloadLogger] 的 normal/failures.log 分开：events.log 只记「操作事件 + 结果」，
 * 一行一条，便于按事件名聚合计数。写入目录与下载日志一致：
 * Android/data/<pkg>/files/Download/download_logs/events.log（极端回退内部私有目录）。
 *
 * 事件命名约定：{功能点}_{动作}，如 download_start / parse_success / parse_fail /
 * download_done / tab_switch / entry_download / clear_completed / retry_task /
 * homepage_download / settings_toggle / logs_view。
 * 属性用 k=v 空格分隔。只埋操作级事件，不埋进度类高频噪音。
 */
object EventTracker {
    private const val TAG = "EventTracker"
    private const val FILE_EVENTS = "events.log"

    @Synchronized
    fun track(context: Context, event: String, attrs: Map<String, String>? = null) {
        runCatching {
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val line = if (attrs.isNullOrEmpty()) {
                "[$ts][$event]\n"
            } else {
                "[$ts][$event] " + attrs.entries.joinToString(" ") { (k, v) ->
                    "$k=${v.replace("\n", " ").replace(" ", "_").take(120)}"
                } + "\n"
            }
            File(DownloadLogger.resolveDir(context), FILE_EVENTS).appendText(line)
        }.onFailure {
            android.util.Log.e(TAG, "track failed: ${it.message}")
        }
    }

    fun getEventsLogContent(context: Context): String =
        runCatching {
            val f = File(DownloadLogger.resolveDir(context), FILE_EVENTS)
            if (f.exists()) f.readText() else ""
        }.getOrDefault("")

    fun getEventsLogPath(context: Context) =
        File(DownloadLogger.resolveDir(context), FILE_EVENTS).absolutePath

    @Synchronized
    fun clear(context: Context) {
        runCatching {
            File(DownloadLogger.resolveDir(context), FILE_EVENTS).delete()
        }.onFailure {
            android.util.Log.e(TAG, "clear events failed: ${it.message}")
        }
    }
}
