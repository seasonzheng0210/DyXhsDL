package com.neoruaa.xhsdn

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neoruaa.xhsdn.data.DownloadTask
import com.neoruaa.xhsdn.data.NoteType
import com.neoruaa.xhsdn.data.TaskManager
import com.neoruaa.xhsdn.data.TaskStatus
import com.neoruaa.xhsdn.ui.glass.GlassColorSchemes
import com.neoruaa.xhsdn.ui.glass.GlassPrimaryWrap
import com.neoruaa.xhsdn.ui.glass.GlassTokens
import com.neoruaa.xhsdn.ui.glass.WallpaperLayer
import com.neoruaa.xhsdn.utils.DownloadLogger
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI v2 S2 —— 任务详情三态页（新增独立页，2026-09-07）。
 *  - 下载中：大进度条 + 完成文件数 + 停止下载；
 *  - 已完成：完成时间 + 文件总量 + 打开 / 查看文件 / 复制链接；
 *  - 失败：末因直显 + 重试 / 复制链接 / 失败日志。
 * 点击任务卡进入；已完成态内点「查看文件」进 DetailActivity 文件浏览页。
 * 玻璃皮肤与全 App 一致（WallpaperLayer 底 + 内容层玻璃卡 + 玻璃态橙主按钮）。
 */
class TaskDetailActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_TASK_ID = "extra_task_id"

        fun newIntent(context: Context, taskId: Long): Intent =
            Intent(context, TaskDetailActivity::class.java).putExtra(EXTRA_TASK_ID, taskId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val context = this
        val failureLogContent = DownloadLogger.getFailureLogContent(context)

        setContent {
            val controller = ThemeController(
                colorSchemeMode = ColorSchemeMode.System,
                lightColors = GlassColorSchemes.light(),
                darkColors = GlassColorSchemes.dark()
            )
            val tasks by TaskManager.getAllTasks().collectAsStateWithLifecycle(initialValue = emptyList())
            val task = tasks.find { it.id == taskId }
            var showLogDialog by remember { mutableStateOf(false) }

            MiuixTheme(controller = controller) {
                Box(modifier = Modifier.fillMaxSize()) {
                    WallpaperLayer(modifier = Modifier.fillMaxSize())
                    TaskDetailScreen(
                        task = task,
                        onBack = { finish() },
                        onCopyUrl = {
                            task?.noteUrl?.let { url ->
                                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("xhs_url", url))
                                Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onStop = { task?.let { DownloadService.stopTask(context, it.id) } },
                        onRedownload = {
                            task?.let {
                                TaskManager.resetTask(it.id)
                                TaskManager.startTask(it.id)
                                DownloadService.startDownload(context, it.noteUrl, it.source.ifBlank { "xhs" }, it.id)
                            }
                        },
                        onRetry = { retryTask(context, it) },
                        onDelete = {
                            task?.let {
                                TaskManager.deleteTask(it.id)
                                Toast.makeText(context, "任务已删除", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        },
                        onOpenFirst = { task?.filePaths?.firstOrNull()?.let { openFile(File(it)) } },
                        onBrowseFiles = { task?.let { openFilesBrowser(it) } },
                        onShowFailureLog = { showLogDialog = true }
                    )
                    // 失败日志弹窗（同一 Composition 内状态驱动，避免二次 setContent）
                    if (showLogDialog) {
                        WindowDialog(
                            title = "失败日志",
                            summary = if (failureLogContent.isBlank()) "暂无日志" else "最近下载失败记录（含解析原因）",
                            show = true,
                            onDismissRequest = { showLogDialog = false }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState())
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MiuixTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = if (failureLogContent.isBlank()) "暂无日志" else failureLogContent,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(text = "关闭", onClick = { showLogDialog = false })
                                Spacer(Modifier.width(12.dp))
                                TextButton(
                                    text = "复制",
                                    onClick = {
                                        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cb.setPrimaryClip(ClipData.newPlainText("failure_log", failureLogContent))
                                        Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                                        showLogDialog = false
                                    },
                                    colors = ButtonDefaults.textButtonColorsPrimary()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** 重试：与 MainActivity 语义一致（抖音/快手/小红书走服务内 HTTP+隐 WebView 兜底解析）。 */
    private fun retryTask(context: Context, task: DownloadTask) {
        when (task.source) {
            "douyin_home" -> {
                // 主页批量是整批爬取任务，单卡重试会丢批次上下文：删旧卡，引导去「首页」重新解析
                TaskManager.deleteTask(task.id)
                Toast.makeText(context, "主页批量请在「首页」重新解析该主页", Toast.LENGTH_LONG).show()
                finish()
            }
            else -> {
                TaskManager.resetTask(task.id)
                TaskManager.startTask(task.id)
                val source = when (task.source) {
                    "kuaishou" -> "kuaishou"
                    "douyin" -> "douyin"
                    else -> "xhs"
                }
                DownloadService.startDownload(context, task.noteUrl, source, task.id)
            }
        }
    }

    private fun openFilesBrowser(task: DownloadTask) {
        val intent = DetailActivity.newIntent(
            this,
            task.id.toString(),
            task.noteTitle ?: task.noteUrl,
            task.filePaths,
            task.noteContent,
            task.noteUrl
        )
        startActivity(intent)
    }

    private fun openFile(file: File) {
        if (!file.exists()) {
            Toast.makeText(this, "文件不存在：${file.path}", Toast.LENGTH_SHORT).show()
            return
        }
        val mimeType = when (file.extension.lowercase()) {
            "mp4", "mkv", "mov", "webm", "avi", "m4v", "flv" -> "video/*"
            "jpg", "jpeg", "png", "gif", "webp", "heic", "bmp" -> "image/*"
            else -> "*/*"
        }
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        kotlin.runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, "无法打开文件：${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

/* ================= UI ================= */

@Composable
private fun TaskDetailScreen(
    task: DownloadTask?,
    onBack: () -> Unit,
    onCopyUrl: () -> Unit,
    onStop: () -> Unit,
    onRedownload: () -> Unit,
    onRetry: (DownloadTask) -> Unit,
    onDelete: () -> Unit,
    onOpenFirst: () -> Unit,
    onBrowseFiles: () -> Unit,
    onShowFailureLog: () -> Unit
) {
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topBarState)
    val dark = isSystemInDarkTheme()
    val tp = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight
    val ts = if (dark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight
    val cardBg = if (dark) GlassTokens.CardDark else GlassTokens.CardLight

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = "任务详情",
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
        if (task == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = "任务不存在或已删除", color = ts, fontSize = 14.sp)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            HeaderCard(task, tp, ts, cardBg)
            Spacer(Modifier.height(12.dp))
            when (task.status) {
                TaskStatus.DOWNLOADING, TaskStatus.QUEUED -> ActiveCard(
                    task, tp, ts, cardBg, dark, onStop, onRedownload,
                    showPauseHint = task.status == TaskStatus.QUEUED, waiting = false
                )
                TaskStatus.WAITING_FOR_USER -> ActiveCard(
                    task, tp, ts, cardBg, dark, onStop, onRedownload,
                    showPauseHint = false, waiting = true
                )
                TaskStatus.COMPLETED -> CompletedCard(task, tp, ts, cardBg, dark, onOpenFirst, onBrowseFiles, onCopyUrl, onDelete)
                TaskStatus.FAILED -> FailedCard(task, tp, ts, cardBg, dark, onRetry, onCopyUrl, onShowFailureLog, onDelete)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 头部信息卡：来源/状态徽章 + 标题 + 元信息 */
@Composable
private fun HeaderCard(task: DownloadTask, tp: Color, ts: Color, cardBg: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        colors = CardDefaults.defaultColors(color = cardBg)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourcePill(task.source)
                Spacer(Modifier.width(8.dp))
                StatusPill(task.status)
                Spacer(Modifier.weight(1f))
                Text(
                    text = fmtTime(
                        if (task.status == TaskStatus.COMPLETED || task.status == TaskStatus.FAILED) {
                            task.completedAt ?: task.createdAt
                        } else task.createdAt
                    ),
                    color = ts,
                    fontSize = 11.5.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = task.noteTitle ?: task.noteUrl,
                color = tp,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = typeText(task) + " · " + task.totalFiles + " 个文件",
                color = ts,
                fontSize = 12.5.sp
            )
        }
    }
}

@Composable
private fun SourcePill(source: String) {
    val label = when (source) {
        "kuaishou" -> "快手"
        "douyin", "douyin_home" -> "抖音"
        else -> "小红书"
    }
    val dark = isSystemInDarkTheme()
    val color = when (source) {
        "kuaishou" -> if (dark) Color(0xFFFF7A3D) else Color(0xFFFE5000)
        "douyin", "douyin_home" -> if (dark) Color(0xFFE8EAEF) else Color(0xFF26292F)
        else -> if (dark) Color(0xFFFF6B81) else Color(0xFFFE2C55)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusPill(status: TaskStatus) {
    val dark = isSystemInDarkTheme()
    val label = when (status) {
        TaskStatus.QUEUED -> "排队中"
        TaskStatus.DOWNLOADING -> "下载中"
        TaskStatus.COMPLETED -> "已完成"
        TaskStatus.FAILED -> "下载失败"
        TaskStatus.WAITING_FOR_USER -> "等待选择"
    }
    // 液态玻璃语义色（与任务列表同 token，明暗两态）
    val color = when (status) {
        TaskStatus.QUEUED -> GlassTokens.QueueGray
        TaskStatus.DOWNLOADING -> if (dark) GlassTokens.DownloadingDark else GlassTokens.DownloadingLight
        TaskStatus.COMPLETED -> if (dark) GlassTokens.SuccessGreenDark else GlassTokens.SuccessGreen
        TaskStatus.FAILED -> if (dark) GlassTokens.FailRedDark else GlassTokens.FailRed
        TaskStatus.WAITING_FOR_USER -> if (dark) GlassTokens.WaitAmberDark else GlassTokens.WaitAmber
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = if (dark) 0.22f else 0.13f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** 进行中 / 排队 / 等待选择卡 */
@Composable
private fun ActiveCard(
    task: DownloadTask,
    tp: Color, ts: Color, cardBg: Color, dark: Boolean,
    onStop: () -> Unit,
    onRedownload: () -> Unit,
    showPauseHint: Boolean,
    waiting: Boolean
) {
    val fg = if (dark) GlassTokens.OrangeOnDark else Color.White
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        colors = CardDefaults.defaultColors(color = cardBg)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            if (waiting) {
                Text(text = "等待你选择要保存的内容", color = tp, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text(text = "图文笔记下载前可先挑选；重新下载将下载全部内容", color = ts, fontSize = 12.5.sp)
                Spacer(Modifier.height(18.dp))
                GlassPrimaryWrap(Modifier.fillMaxWidth().height(48.dp), cornerRadius = 24.dp) {
                    Button(onClick = onRedownload, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White)) {
                        Text("重新下载全部", color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${(task.progress * 100).toInt()}%",
                        color = if (dark) GlassTokens.DownloadingDark else GlassTokens.OrangeTextDeep,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (task.status == TaskStatus.QUEUED) "排队中…" else "下载中",
                        color = ts,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = task.progress,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${task.completedFiles} / ${task.totalFiles} 个文件",
                        color = tp,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (task.failedFiles > 0) {
                        Text(text = "${task.failedFiles} 个失败", color = if (dark) GlassTokens.FailRedDark else GlassTokens.FailRed, fontSize = 12.5.sp)
                    }
                }
                if (showPauseHint) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = "排队中，可停止等待", color = ts, fontSize = 12.sp)
                }
                Spacer(Modifier.height(20.dp))
                GlassPrimaryWrap(Modifier.fillMaxWidth().height(48.dp), cornerRadius = 24.dp) {
                    Button(onClick = onStop, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White)) {
                        Text("停止下载", color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

/** 已完成卡 */
@Composable
private fun CompletedCard(
    task: DownloadTask,
    tp: Color, ts: Color, cardBg: Color, dark: Boolean,
    onOpenFirst: () -> Unit,
    onBrowseFiles: () -> Unit,
    onCopyUrl: () -> Unit,
    onDelete: () -> Unit
) {
    val fg = if (dark) GlassTokens.OrangeOnDark else Color.White
    val totalBytes = task.filePaths.sumOf { runCatching { File(it).length() }.getOrDefault(0L) }
    val folder = task.filePaths.firstOrNull()?.let { runCatching { File(it).parentFile?.absolutePath }.getOrNull() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        colors = CardDefaults.defaultColors(color = cardBg)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = MiuixIcons.Play, contentDescription = null, modifier = Modifier.size(20.dp), tint = GlassTokens.SuccessGreen)
                Spacer(Modifier.width(6.dp))
                Text(text = "下载完成", color = GlassTokens.SuccessGreen, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(text = fmtTime(task.completedAt ?: task.createdAt), color = ts, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            InfoLine("总大小", if (totalBytes > 0) sizeHuman(totalBytes) else "—", tp, ts)
            Spacer(Modifier.height(8.dp))
            InfoLine("保存位置", folder ?: "本机存储", tp, ts, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            InfoLine("内容类型", typeText(task), tp, ts)
            if (task.failedFiles > 0) {
                Spacer(Modifier.height(8.dp))
                InfoLine("失败文件", "${task.failedFiles} 个", if (dark) GlassTokens.FailRedDark else GlassTokens.FailRed, ts, warn = true)
            }
            Spacer(Modifier.height(22.dp))
            if (task.filePaths.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassPrimaryWrap(Modifier.weight(1f).height(46.dp), cornerRadius = 23.dp) {
                        Button(onClick = onOpenFirst, modifier = Modifier.fillMaxWidth().height(46.dp), colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White)) {
                            Text("打开", color = fg, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    GlassPrimaryWrap(Modifier.weight(1f).height(46.dp), cornerRadius = 23.dp) {
                        Button(onClick = onBrowseFiles, modifier = Modifier.fillMaxWidth().height(46.dp), colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White)) {
                            Text("查看文件", color = fg, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                Text(text = "本任务未保存文件（可能是网页爬取/批量任务）", color = ts, fontSize = 12.5.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(text = "复制链接", onClick = onCopyUrl)
                TextButton(text = "删除任务", onClick = onDelete)
            }
        }
    }
}

/** 失败卡：末因直显 + 重试 / 日志 */
@Composable
private fun FailedCard(
    task: DownloadTask,
    tp: Color, ts: Color, cardBg: Color, dark: Boolean,
    onRetry: (DownloadTask) -> Unit,
    onCopyUrl: () -> Unit,
    onShowFailureLog: () -> Unit,
    onDelete: () -> Unit
) {
    val fg = if (dark) GlassTokens.OrangeOnDark else Color.White
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        colors = CardDefaults.defaultColors(color = cardBg)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(if (dark) GlassTokens.FailRedDark else GlassTokens.FailRed)
                )
                Spacer(Modifier.width(8.dp))
                Text(text = "下载失败", color = if (dark) GlassTokens.FailRedDark else GlassTokens.FailRed, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(text = fmtTime(task.completedAt ?: task.createdAt), color = ts, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(text = "失败原因", color = ts, fontSize = 11.5.sp)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (dark) Color.White.copy(alpha = 0.08f) else GlassTokens.FailRed.copy(alpha = 0.07f))
                    .padding(14.dp)
            ) {
                Text(
                    text = task.errorMessage ?: "未知错误（可查看失败日志定位）",
                    color = if (dark) GlassTokens.TextPrimaryDark else Color(0xFF8A2A2A),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
            Spacer(Modifier.height(22.dp))
            GlassPrimaryWrap(Modifier.fillMaxWidth().height(48.dp), cornerRadius = 24.dp) {
                Button(
                    onClick = { onRetry(task) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("重试", color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(text = "复制链接", onClick = onCopyUrl)
                TextButton(text = "查看失败日志", onClick = onShowFailureLog, colors = ButtonDefaults.textButtonColorsPrimary())
                TextButton(text = "删除", onClick = onDelete)
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, tp: Color, ts: Color, maxLines: Int = 1, warn: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = ts, fontSize = 12.5.sp, modifier = Modifier.width(64.dp))
        Text(
            text = value,
            color = if (warn) (if (isSystemInDarkTheme()) GlassTokens.FailRedDark else GlassTokens.FailRed) else tp,
            fontSize = 12.5.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun typeText(task: DownloadTask): String = when (task.noteType) {
    NoteType.IMAGE -> "图文"
    NoteType.VIDEO -> "视频"
    else -> "内容"
}

private fun sizeHuman(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024f * 1024 * 1024))
        bytes >= 1024L * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024))
        bytes >= 1024L -> String.format(Locale.getDefault(), "%.0f KB", bytes / 1024f)
        else -> "$bytes B"
    }
}

private fun fmtTime(timestamp: Long): String {
    return try {
        val d = Date(timestamp)
        val now = Date()
        val sdfDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        if (sdfDay.format(d) == sdfDay.format(now)) {
            SimpleDateFormat("今天 HH:mm", Locale.getDefault()).format(d)
        } else {
            SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(d)
        }
    } catch (e: Exception) {
        ""
    }
}
