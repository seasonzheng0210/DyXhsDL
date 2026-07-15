package com.neoruaa.xhsdn.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 后台下载失败日志。
 * 记录所有下载失败（抖音解析/下载失败、以及通用下载回调中的终态错误），
 * 写入应用私有目录 download_logs/failures.log。
 *
 * 优先写到外部私有目录 Android/data/com.neoruaa.douyinxhs/files/Download/download_logs/，
 * 若该目录不可用（极端情况）则回退到内部私有目录 files/download_logs/，保证一定写得出去。
 *
 * 日志包含：时间、来源、输入/原始链接、失败原因、以及抖音解析失败时分享页 HTML 的前 300 字
 * （用于判断是否被抖音 security 风控拦截），方便后续定位问题。
 *
 * 也可在 App 内通过「更多 → 失败日志」直接查看，无需文件管理器。
 */
object DownloadLogger {
    private const val TAG = "DownloadLogger"
    private const val DIR = "download_logs"
    private const val FILE = "failures.log"

    private fun resolveDir(context: Context): File {
        val ext = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val base = if (ext != null) File(ext, DIR) else File(context.filesDir, DIR)
        if (!base.exists()) base.mkdirs()
        return base
    }

    @Synchronized
    fun logFailure(context: Context, source: String, input: String, detail: String) {
        runCatching {
            val file = File(resolveDir(context), FILE)
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val safeInput = input.replace("\n", " ").take(400)
            val safeDetail = detail.replace("\n", " ").take(800)
            val line = "[$ts][$source] input=$safeInput\n  reason=$safeDetail\n\n"
            file.appendText(line)
        }.onFailure {
            android.util.Log.e(TAG, "write log failed: ${it.message}")
        }
    }

    fun getLogFilePath(context: Context): String {
        return File(resolveDir(context), FILE).absolutePath
    }

    fun getLogContent(context: Context): String {
        return runCatching {
            val file = File(resolveDir(context), FILE)
            if (file.exists()) file.readText() else ""
        }.getOrDefault("")
    }
}
