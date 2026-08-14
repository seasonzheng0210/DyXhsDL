package com.neoruaa.xhsdn.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 下载日志，分两类：
 *  - 正常日志（normal.log）：记录解析成功、下载完成等正常事件，便于与失败日志对照排查；
 *  - 筛选的失败日志（failures.log）：仅记录失败事件（解析/下载失败、风控拦截等）。
 *
 * 两者都写入应用外部私有目录 download_logs/，优先
 * Android/data/<pkg>/files/Download/download_logs/，极端情况下回退内部私有目录。
 *
 * 可用 [packageLogs] 把两类日志压缩成单个 zip，便于通过「更多 → 打包日志」分享给开发者。
 * 也可在 App 内通过「更多 → 失败日志 / 正常日志」直接查看，无需文件管理器。
 */
object DownloadLogger {
    private const val TAG = "DownloadLogger"
    private const val DIR = "download_logs"
    private const val FILE_FAILURE = "failures.log"
    private const val FILE_NORMAL = "normal.log"

    private fun resolveDir(context: Context): File {
        val ext = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val base = if (ext != null) File(ext, DIR) else File(context.filesDir, DIR)
        if (!base.exists()) base.mkdirs()
        return base
    }

    @Synchronized
    private fun append(file: File, source: String, input: String, detail: String) {
        runCatching {
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val ver = com.neoruaa.xhsdn.BuildConfig.VERSION_NAME
            val safeInput = input.replace("\n", " ").take(400)
            val safeDetail = detail.replace("\n", " ").take(800)
            val line = "[$ts][$source][v$ver] input=$safeInput\n  $safeDetail\n\n"
            file.appendText(line)
        }.onFailure {
            android.util.Log.e(TAG, "write log failed: ${it.message}")
        }
    }

    /** 筛选的失败日志：仅记录失败事件。 */
    @Synchronized
    fun logFailure(context: Context, source: String, input: String, detail: String) {
        append(File(resolveDir(context), FILE_FAILURE), source, input, "reason=$detail")
    }

    /** 正常日志：记录解析成功、下载完成等正常事件。 */
    @Synchronized
    fun logInfo(context: Context, source: String, input: String, detail: String) {
        append(File(resolveDir(context), FILE_NORMAL), source, input, detail)
    }

    fun getFailureLogFilePath(context: Context) =
        File(resolveDir(context), FILE_FAILURE).absolutePath

    /**
     * 清除失败日志：仅删除 failures.log，保留 normal.log（正常下载记录）。
     * 删除后下次写入会自动重建文件，不影响后续日志。
     */
    @Synchronized
    fun clearFailureLog(context: Context) {
        runCatching {
            File(resolveDir(context), FILE_FAILURE).delete()
        }.onFailure {
            android.util.Log.e(TAG, "clear failure log failed: ${it.message}")
        }
    }

    fun getNormalLogFilePath(context: Context) =
        File(resolveDir(context), FILE_NORMAL).absolutePath

    fun getFailureLogContent(context: Context) = readFile(context, FILE_FAILURE)
    fun getNormalLogContent(context: Context) = readFile(context, FILE_NORMAL)

    // 兼容旧调用：原先 getLogContent/getLogFilePath 指向失败日志
    fun getLogContent(context: Context) = getFailureLogContent(context)
    fun getLogFilePath(context: Context) = getFailureLogFilePath(context)

    private fun readFile(context: Context, name: String): String {
        return runCatching {
            val file = File(resolveDir(context), name)
            if (file.exists()) file.readText() else ""
        }.getOrDefault("")
    }

    /**
     * 打包：把 正常日志 + 失败日志 压缩为单个 zip，写到 download_logs/DownloadLogs_<ts>.zip。
     * 返回 zip 文件；若两类日志都不存在则返回 null。
     */
    @Synchronized
    fun packageLogs(context: Context): File? {
        val dir = resolveDir(context)
        val normal = File(dir, FILE_NORMAL)
        val failure = File(dir, FILE_FAILURE)
        if (!normal.exists() && !failure.exists()) return null

        val zipFile = File(dir, "DownloadLogs_${System.currentTimeMillis()}.zip")
        val ok = runCatching {
            ZipOutputStream(zipFile.outputStream()).use { zos ->
                if (normal.exists()) addEntry(zos, FILE_NORMAL, normal)
                if (failure.exists()) addEntry(zos, FILE_FAILURE, failure)
            }
        }.onFailure {
            android.util.Log.e(TAG, "package logs failed: ${it.message}")
        }.isSuccess
        return if (ok) zipFile else null
    }

    private fun addEntry(zos: ZipOutputStream, name: String, file: File) {
        val buf = ByteArray(8192)
        FileInputStream(file).use { fis ->
            zos.putNextEntry(ZipEntry(name))
            var len: Int
            while (fis.read(buf).also { len = it } > 0) {
                zos.write(buf, 0, len)
            }
            zos.closeEntry()
        }
    }
}
