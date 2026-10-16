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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.neoruaa.xhsdn.data.DownloadTask
import com.neoruaa.xhsdn.data.NoteType
import com.neoruaa.xhsdn.data.TaskManager
import com.neoruaa.xhsdn.data.TaskStatus
import com.neoruaa.xhsdn.ui.glass.GlassColorSchemes
import com.neoruaa.xhsdn.ui.glass.GlassDialog
import com.neoruaa.xhsdn.ui.glass.GlassLogBody
import com.neoruaa.xhsdn.ui.glass.GlassPrimaryWrap
import com.neoruaa.xhsdn.ui.glass.GlassTokens
import com.neoruaa.xhsdn.ui.glass.accentBg
import com.neoruaa.xhsdn.ui.glass.accentFg
import com.neoruaa.xhsdn.ui.glass.WallpaperLayer
import com.neoruaa.xhsdn.ui.glass.glassSolid
import com.neoruaa.xhsdn.ui.glass.GlassSpec
import com.neoruaa.xhsdn.ui.glass.platformBrandColor
import com.neoruaa.xhsdn.ui.glass.platformLabel
import com.neoruaa.xhsdn.utils.DownloadLogger
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
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

/**
 * 悬浮「复制链接」胶囊占掉的底部空间（navbar 之外）：底距 16 + 胶囊高 52 + 呼吸 16。
 * ★页面必须让位这么多，否则滚动到底时末项会被胶囊压住（同 FAB 让位铁律）。
 */
private val CopyCapsuleReserve = 84.dp

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
                        onShowFailureLog = { showLogDialog = true },
                        onOpenAuthorHome = { openAuthorHomepage(it) }
                    )
                    // 失败日志弹窗（同一 Composition 内状态驱动，避免二次 setContent）
                    // v3.1：WindowDialog → GlassDialog（v3.0.8 已把其余弹窗迁移到自绘玻璃卡，
                    // 本屏漏迁 → 真机上标题/日志直浮壁纸无底盘；改用 GlassLogBody 保持与
                    // 设置页「失败日志」弹窗同款样式，避免样式漂移）
                    if (showLogDialog) {
                        GlassDialog(
                            title = "失败日志",
                            onDismiss = { showLogDialog = false }
                        ) {
                            GlassLogBody(
                                text = failureLogContent,
                                emptyText = "暂无日志"
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
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

    /**
     * E7：跳到 MainActivity 的主页下载承载面（唯一承载面 + 唯一入口通道）。
     * 有 authorSecUid 就零网络开销直达作者主页；没有（老任务 / 快手）退化为按 noteUrl
     * 反查作者 —— 反查耗时正好被解析第一阶段「定位作者主页」掩盖。
     */
    private fun openAuthorHomepage(task: DownloadTask) {
        val link = task.authorSecUid?.takeIf { it.isNotBlank() }
            ?.let { "https://www.douyin.com/user/$it" }
            ?: task.noteUrl
        val intent = Intent(this, MainActivity::class.java).apply {
            // singleTop + CLEAR_TOP：复用已有 MainActivity 实例，避免叠出第二份
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN_HOMEPAGE, link)
            putExtra(MainActivity.EXTRA_HOMEPAGE_FROM, MainActivity.HOMEPAGE_FROM_TASK_DETAIL)
        }
        startActivity(intent)
        finish()
    }

    /** 重试：与 MainActivity 语义一致（抖音/快手/小红书走服务内 HTTP+隐 WebView 兜底解析）。 */
    private fun retryTask(context: Context, task: DownloadTask) {
        when (task.source) {
            "douyin_home" -> {
                // 主页批量是整批爬取任务，单卡重试会丢批次上下文 → 直接跳到主页下载承载面重新解析。
                // （原实现删卡 + toast「请在「首页」重新解析该主页」，把用户赶去手工操作；现改为真入口）
                openAuthorHomepage(task)
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
    onShowFailureLog: () -> Unit,
    /** E7 入口：从"单个作品"跳到"该作者主页批量"（仅抖音/快手，小红书本期不支持）。 */
    onOpenAuthorHome: (DownloadTask) -> Unit = {}
) {
    val dark = isSystemInDarkTheme()
    val tp = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight
    val ts = if (dark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight
    val cardBg = if (dark) GlassTokens.CardDark else GlassTokens.CardLight

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.displayCutout),
        containerColor = Color.Transparent,
        topBar = {
            // v3.0.6（2026-09-10 真机 vs 设计稿 P1-5）：Miuix TopAppBar（左大标题）→
            // mockup 样式：圆形玻璃返回钮（左上）+ 居中「任务详情」标题
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                    // v3.2.3：12 → 16dp，顶栏返回钮与页面内容边距一致
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(48.dp)
            ) {
                // 圆形玻璃返回钮（r = 48/2）
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(if (dark) Color(0x33FFFFFF) else Color(0x80FFFFFF))
                        .border(1.dp, if (dark) Color(0x2EFFFFFF) else Color(0x99FFFFFF), CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = "返回",
                        tint = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight,
                        modifier = Modifier.size(24.dp) // v3.2.1 图标网格统一 22→24
                    )
                }
                // 居中标题
                Text(
                    text = "任务详情",
                    color = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { padding ->
        if (task == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = "任务不存在或已删除", color = ts, fontSize = 14.sp)
            }
            return@Scaffold
        }
        // v3.2.2：CompletedCard / FailedCard 的 `onCopyUrl` 参数已移除 —— 复制链接从卡片里
        // 挪到了本页底部的**悬浮胶囊**（用户反馈：和「删除任务」挤在一排很突兀）。
        val showCopy = task.status == TaskStatus.COMPLETED || task.status == TaskStatus.FAILED
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // ★底部让位铁律：悬浮胶囊会压住滚动内容，且 padding 必须写在滚动容器**自身**
                    //（写 contentPadding 只在滚到底时留白，途中照样被压）
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .then(if (showCopy) Modifier.padding(bottom = CopyCapsuleReserve) else Modifier)
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
                    TaskStatus.COMPLETED -> CompletedCard(task, tp, ts, cardBg, dark, onOpenFirst, onBrowseFiles, onDelete)
                    TaskStatus.FAILED -> FailedCard(task, tp, ts, cardBg, dark, onRetry, onShowFailureLog, onDelete)
                }
                // E7：作者主页入口（三态通用）。原实现只有一句"请去首页重新解析"的 toast，
                // 把用户赶去手工操作；这里给出真正可用的一键入口。
                if (task.source == "douyin" || task.source == "douyin_home" || task.source == "kuaishou") {
                    Spacer(Modifier.height(12.dp))
                    AuthorHomeEntryCard(task, tp, ts, cardBg, dark, onOpenAuthorHome)
                }
                Spacer(Modifier.height(24.dp))
            }
            if (showCopy) {
                CopyLinkCapsule(
                    onClick = onCopyUrl,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                )
            }
        }
    }
}

/**
 * ★悬浮「复制链接」胶囊（v3.2.2）。
 *
 * 用户反馈：原来它是 `CompletedCard` / `FailedCard` 里一排 TextButton 的第一项，
 * 和「删除任务」同排 —— 一个复制、一个删除，视觉权重却一样，而且被埋在卡片内容里很突兀。
 * 现在把它提升为**浮在本页最上层**的胶囊：独立于任何卡片，底部居中，带阴影抬起。
 *
 * 为什么用「实心表面 + 阴影」而不是另一种卡片：
 *  浮在上面的一切都要能压住下层 —— Android 没有 backdrop-blur，半透会让下层文字穿透
 *  （v3.0.9 dock 的老病因）。所以这里走 `glassSolid()` 实心 + 6dp 阴影 + 1px 折射边。
 *
 * 高度 52dp = 按钮阶梯的 L2（不是每个视图的主操作，不能占 L1 的 60dp）。
 */
@Composable
private fun CopyLinkCapsule(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(GlassSpec.BtnCornerRadius),
                ambientColor = Color(0x26000000),
                spotColor = if (dark) Color(0x33000000) else Color(0x4D000000)
            )
            .clip(RoundedCornerShape(GlassSpec.BtnCornerRadius))
            .background(glassSolid(0.97f, 0.96f))
            .border(
                1.dp,
                if (dark) GlassTokens.SurfaceControlLineDark else GlassTokens.SurfaceControlLineLight,
                RoundedCornerShape(GlassSpec.BtnCornerRadius)
            )
            .height(GlassSpec.BtnHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = MiuixIcons.Link,
                contentDescription = null,
                tint = accentFg(),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "复制链接",
                color = accentFg(),
                fontSize = GlassSpec.BtnFontSize,
                fontWeight = GlassSpec.BtnFontWeight,
                letterSpacing = GlassSpec.BtnLetterSpacing
            )
        }
    }
}

/**
 * E7 作者主页入口卡（三态通用）：
 * 一行「下载该作者的主页 / 还可下载该博主全部作品」→ 一键落到主页下载承载面。
 * 主页批次任务（douyin_home）语义改为「重新检查新作品」——用户想要的是"看有没有更新"，
 * 不是"把作者全部重下一遍"。
 */
@Composable
private fun AuthorHomeEntryCard(
    task: DownloadTask,
    tp: Color,
    ts: Color,
    cardBg: Color,
    dark: Boolean,
    onOpenAuthorHome: (DownloadTask) -> Unit
) {
    val isBatch = task.source == "douyin_home"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenAuthorHome(task) },
        cornerRadius = 24.dp,
        colors = CardDefaults.defaultColors(color = cardBg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    // ★原来用 primary@0.16 当底 + primary 当图标 —— primary 是半透明打底色，
                    //   暗色下两者一起压成深棕，实测对比度 ~1.4:1（图标几乎看不见）。
                    //   改走共享的 accentBg/accentFg（亮 #E2560E / 暗 #FFB37A）。
                    .background(accentBg()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    // 美工：原借用 Play（播放三角）表达"作者"，语义不符；换人物圆标
                    imageVector = MiuixIcons.ContactsCircle,
                    contentDescription = null,
                    tint = accentFg(),
                    modifier = Modifier.size(20.dp) // v3.2.1 图标网格统一 19→20
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (isBatch) R.string.task_detail_author_home_rescan
                        else R.string.task_detail_author_home
                    ),
                    color = tp,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(
                        if (isBatch) R.string.task_detail_author_home_rescan_sub
                        else R.string.task_detail_author_home_sub
                    ),
                    color = ts,
                    fontSize = 11.5.sp
                )
            }
            Text(text = "›", color = ts, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** 头部信息卡：来源/状态徽章 + 标题 + 元信息 */@Composable
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
    // v3.1.6：色值与平台名走 ui/glass/PlatformBrand.kt（全 App 唯一入口）
    val label = platformLabel(source)
    val color = platformBrandColor(source)
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
                // v3.1.5：按钮几何/字号统一走 GlassSpec（`.btn` 52dp / r26 / 14.5sp / 650）
                GlassPrimaryWrap(Modifier.fillMaxWidth().height(GlassSpec.BtnHeight), cornerRadius = GlassSpec.BtnCornerRadius) {
                    Button(onClick = onRedownload, modifier = Modifier.fillMaxWidth().height(GlassSpec.BtnHeight), colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White), insideMargin = PaddingValues(horizontal = GlassSpec.BtnBarPadH, vertical = 0.dp)) {
                        Text("重新下载全部", color = fg, fontSize = GlassSpec.BtnFontSize, fontWeight = GlassSpec.BtnFontWeight, letterSpacing = GlassSpec.BtnLetterSpacing)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    // v3.0.6（P1-3）：大数字进度 mockup 为液态渐变橙红字（GradientStart→End，120°）
                    Text(
                        text = "${(task.progress * 100).toInt()}%",
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(GlassTokens.GradientStart, GlassTokens.GradientEnd),
                                start = Offset(0f, 0f),
                                end = Offset(90f, 32f)  // 近似 120°
                            )
                        ),
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
                // v3.0.6（P1-4）：mockup 全宽橙红渐变进度条（h8/r4）；单色进度条 → 自绘渐变
                // 进度动画：用 animateFloatAsState 让条平滑（进程内 progress 跳跃更新）
                val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = task.progress.coerceIn(0f, 1f),
                    animationSpec = androidx.compose.animation.core.tween(400)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (dark) Color(0x24FFFFFF) else Color(0x2A241F18))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GlassTokens.GradientStart, GlassTokens.GradientEnd),
                                    start = Offset(0f, 0f),
                                    end = Offset(120f, 8f)
                                )
                            )
                    )
                }
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
                GlassPrimaryWrap(Modifier.fillMaxWidth().height(GlassSpec.BtnHeight), cornerRadius = GlassSpec.BtnCornerRadius) {
                    Button(onClick = onStop, modifier = Modifier.fillMaxWidth().height(GlassSpec.BtnHeight), colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White), insideMargin = PaddingValues(horizontal = GlassSpec.BtnBarPadH, vertical = 0.dp)) {
                        Text("停止下载", color = fg, fontSize = GlassSpec.BtnFontSize, fontWeight = GlassSpec.BtnFontWeight, letterSpacing = GlassSpec.BtnLetterSpacing)
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
                    GlassPrimaryWrap(Modifier.weight(1f).height(GlassSpec.BtnHeight), cornerRadius = GlassSpec.BtnCornerRadius) {
                        Button(onClick = onOpenFirst, modifier = Modifier.fillMaxWidth().height(GlassSpec.BtnHeight), colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White), insideMargin = PaddingValues(horizontal = GlassSpec.BtnBarPadH, vertical = 0.dp)) {
                            Text("打开", color = fg, fontSize = GlassSpec.BtnFontSize, fontWeight = GlassSpec.BtnFontWeight, letterSpacing = GlassSpec.BtnLetterSpacing)
                        }
                    }
                    GlassPrimaryWrap(Modifier.weight(1f).height(GlassSpec.BtnHeight), cornerRadius = GlassSpec.BtnCornerRadius) {
                        Button(onClick = onBrowseFiles, modifier = Modifier.fillMaxWidth().height(GlassSpec.BtnHeight), colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White), insideMargin = PaddingValues(horizontal = GlassSpec.BtnBarPadH, vertical = 0.dp)) {
                            Text("查看文件", color = fg, fontSize = GlassSpec.BtnFontSize, fontWeight = GlassSpec.BtnFontWeight, letterSpacing = GlassSpec.BtnLetterSpacing)
                        }
                    }
                }
            } else {
                Text(text = "本任务未保存文件（可能是网页爬取/批量任务）", color = ts, fontSize = 12.5.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
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
            GlassPrimaryWrap(Modifier.fillMaxWidth().height(GlassSpec.BtnHeight), cornerRadius = GlassSpec.BtnCornerRadius) {
                Button(
                    onClick = { onRetry(task) },
                    modifier = Modifier.fillMaxWidth().height(GlassSpec.BtnHeight),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text("重试", color = fg, fontSize = GlassSpec.BtnFontSize, fontWeight = GlassSpec.BtnFontWeight, letterSpacing = GlassSpec.BtnLetterSpacing)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
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
