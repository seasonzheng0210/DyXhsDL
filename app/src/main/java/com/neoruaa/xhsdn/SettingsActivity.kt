package com.neoruaa.xhsdn

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBarState
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.window.WindowDialog
import com.neoruaa.xhsdn.ui.glass.GlassColorSchemes
import com.neoruaa.xhsdn.ui.glass.GlassDialog
import com.neoruaa.xhsdn.ui.glass.GlassLogBody
import com.neoruaa.xhsdn.ui.glass.GlassSpec
import com.neoruaa.xhsdn.ui.glass.GlassThemeState
import com.neoruaa.xhsdn.ui.glass.GlassThemes
import com.neoruaa.xhsdn.ui.glass.ThemeId
import com.neoruaa.xhsdn.ui.glass.accentFg
import com.neoruaa.xhsdn.ui.glass.WallpaperLayer
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.neoruaa.xhsdn.utils.DownloadLogger
import com.neoruaa.xhsdn.utils.EventTracker
import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor

private const val PREFS_NAME = "XHSDownloaderPrefs"

data class SettingsUiState(
    val createLivePhotos: Boolean = true,
    val useCustomNaming: Boolean = false,
    val template: TextFieldValue = TextFieldValue(NamingFormat.DEFAULT_TEMPLATE),
    val tokens: List<NamingFormat.TokenDefinition> = emptyList(),
    val debugNotificationEnabled: Boolean = false,
    val downloadCompleteNotification: Boolean = true,
    val selectiveDownload: Boolean = false,
    val keepScreenOn: Boolean = false,
    val showClipboardBubble: Boolean = true,
    val autoReadClipboard: Boolean = false,
    val manualInputLinks: Boolean = false
)

class SettingsViewModel(private val prefs: SharedPreferences) : ViewModel() {
    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<SettingsUiState> = _state
    private var hasChanges = false

    private fun loadState(): SettingsUiState {
        val createLivePhotos = prefs.getBoolean("create_live_photos", true)
        val hasNewFlag = prefs.contains("use_custom_naming_format")
        val legacyFlag = prefs.getBoolean("use_metadata_file_names", false)
        val useCustomNaming = if (hasNewFlag) prefs.getBoolean("use_custom_naming_format", false) else legacyFlag
        var template = prefs.getString("custom_naming_template", NamingFormat.DEFAULT_TEMPLATE)
        if (template.isNullOrEmpty()) {
            template = NamingFormat.DEFAULT_TEMPLATE
        }
        val debugNotificationEnabled = prefs.getBoolean("debug_notification_enabled", false)
        val downloadCompleteNotification = prefs.getBoolean("download_complete_notification", true)
        val selectiveDownload = prefs.getBoolean("selective_download", false)
        val keepScreenOn = prefs.getBoolean("keep_screen_on", false)
        val showClipboardBubble = prefs.getBoolean("show_clipboard_bubble", true) // Default true
        val autoReadClipboard = prefs.getBoolean("auto_read_clipboard", false)
        val manualInputLinks = prefs.getBoolean("manual_input_links", false)
        return SettingsUiState(
            createLivePhotos = createLivePhotos,
            useCustomNaming = useCustomNaming,
            template = TextFieldValue(template),
            tokens = NamingFormat.getAvailableTokens(),
            debugNotificationEnabled = debugNotificationEnabled,
            downloadCompleteNotification = downloadCompleteNotification,
            selectiveDownload = selectiveDownload,
            keepScreenOn = keepScreenOn,
            showClipboardBubble = showClipboardBubble,
            autoReadClipboard = autoReadClipboard,
            manualInputLinks = manualInputLinks
        )
    }



    fun onCreateLivePhotosChange(enabled: Boolean) = updateState {
        it.copy(createLivePhotos = enabled).also { newState ->
            persist(newState)
        }
    }

    fun onUseCustomNamingChange(enabled: Boolean) = updateState {
        it.copy(useCustomNaming = enabled).also { newState ->
            persist(newState)
        }
    }

    fun onTemplateChange(value: TextFieldValue) = updateState {
        it.copy(template = value).also { newState ->
            persist(newState)
        }
    }

    fun onResetTemplate() = updateState {
        it.copy(template = TextFieldValue(NamingFormat.DEFAULT_TEMPLATE)).also { newState ->
            persist(newState)
        }
    }



    fun onDebugNotificationChange(enabled: Boolean) = updateState {
        it.copy(debugNotificationEnabled = enabled).also { newState ->
            persist(newState)
        }
    }

    fun onDownloadCompleteNotificationChange(enabled: Boolean) = updateState {
        it.copy(downloadCompleteNotification = enabled).also { newState ->
            persist(newState)
        }
    }

    fun onSelectiveDownloadChange(enabled: Boolean) = updateState {
        it.copy(selectiveDownload = enabled).also { newState ->
            persist(newState)
        }
    }

    fun onKeepScreenOnChange(enabled: Boolean) = updateState {
        it.copy(keepScreenOn = enabled).also { newState ->
            persist(newState)
        }
    }

    fun onShowClipboardBubbleChange(enabled: Boolean) = updateState {
        it.copy(showClipboardBubble = enabled).also { newState ->
            persist(newState)
        }
    }

    fun onAutoReadClipboardChange(enabled: Boolean) = updateState {
        it.copy(autoReadClipboard = enabled).also { newState ->
            persist(newState)
        }
    }

    fun onManualInputLinksChange(enabled: Boolean) = updateState {
        it.copy(manualInputLinks = enabled).also { newState ->
            persist(newState)
        }
    }

    private fun persist(state: SettingsUiState) {
        hasChanges = true
        prefs.edit()
            .putBoolean("create_live_photos", state.createLivePhotos)
            .putBoolean("use_custom_naming_format", state.useCustomNaming)
            .putString("custom_naming_template", state.template.text.ifBlank { NamingFormat.DEFAULT_TEMPLATE })
            .putBoolean("debug_notification_enabled", state.debugNotificationEnabled)
            .putBoolean("download_complete_notification", state.downloadCompleteNotification)
            .putBoolean("selective_download", state.selectiveDownload)
            .putBoolean("keep_screen_on", state.keepScreenOn)
            .putBoolean("show_clipboard_bubble", state.showClipboardBubble)
            .putBoolean("auto_read_clipboard", state.autoReadClipboard)
            .putBoolean("manual_input_links", state.manualInputLinks)
            .remove("use_metadata_file_names")
            .apply()
    }

    fun hasChanges(): Boolean = hasChanges

    private fun updateState(block: (SettingsUiState) -> SettingsUiState) {
        viewModelScope.launch {
            _state.emit(block(_state.value))
        }
    }
}

class SettingsViewModelFactory(private val prefs: SharedPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SettingsActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(getSharedPreferences(PREFS_NAME, MODE_PRIVATE))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT
            )
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = AndroidColor.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = !isNightMode
        setContent {
            // 玻璃态：固定玻璃色板（亮/暗随系统），不跟随 Monet 动态色
            val controller = ThemeController(
                colorSchemeMode = ColorSchemeMode.System,
                lightColors = GlassColorSchemes.light(),
                darkColors = GlassColorSchemes.dark()
            )
            val uiState by viewModel.state.collectAsStateWithLifecycle()
            val topBarState = rememberTopAppBarState()
            MiuixTheme(controller = controller) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 玻璃态壁纸层（最底）
                    WallpaperLayer(modifier = Modifier.fillMaxSize())
                    SettingsScreen(
                        uiState = uiState,
                        onBack = { finishWithResult() },
                        onCreateLivePhotosChange = viewModel::onCreateLivePhotosChange,
                        onUseCustomNamingChange = viewModel::onUseCustomNamingChange,
                        onTemplateChange = viewModel::onTemplateChange,
                        onResetTemplate = viewModel::onResetTemplate,
                        onDebugNotificationChange = viewModel::onDebugNotificationChange,
                        onDownloadCompleteNotificationChange = viewModel::onDownloadCompleteNotificationChange,
                        onSelectiveDownloadChange = viewModel::onSelectiveDownloadChange,
                        onKeepScreenOnChange = viewModel::onKeepScreenOnChange,
                        onShowClipboardBubbleChange = viewModel::onShowClipboardBubbleChange,
                        onAutoReadClipboardChange = viewModel::onAutoReadClipboardChange,
                        onManualInputLinksChange = viewModel::onManualInputLinksChange,
                        topBarState = topBarState
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        checkAccessibilityState()
    }
    
    private fun checkAccessibilityState() {
        // No-op
    }

    private fun finishWithResult() {
        setResult(if (viewModel.hasChanges()) RESULT_OK else RESULT_CANCELED)
        finish()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    // UI v2：embedded=true 时内嵌主界面「我的」tab——不自绘状态栏 inset、不显示顶栏（避免双重留白）
    embedded: Boolean = false,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onCreateLivePhotosChange: (Boolean) -> Unit,
    onUseCustomNamingChange: (Boolean) -> Unit,
    onTemplateChange: (TextFieldValue) -> Unit,
    onResetTemplate: () -> Unit,
    onDebugNotificationChange: (Boolean) -> Unit,
    onDownloadCompleteNotificationChange: (Boolean) -> Unit,
    onSelectiveDownloadChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onShowClipboardBubbleChange: (Boolean) -> Unit,
    onAutoReadClipboardChange: (Boolean) -> Unit,
    onManualInputLinksChange: (Boolean) -> Unit,
    topBarState: TopAppBarState
) {
    val context = LocalContext.current
    // 读它是 Compose 快照态 → 选主题后本页自动重组合，不用重启
    val currentTheme = GlassThemeState.current
    val dark = isSystemInDarkTheme()
    val scrollBehavior = top.yukonga.miuix.kmp.basic.MiuixScrollBehavior(state = topBarState)
    val contentInsets = if (embedded) {
        WindowInsets(0, 0, 0, 0)
    } else {
        WindowInsets.statusBars.union(WindowInsets.displayCutout)
    }
    // UI v2 S3 日志与诊断：弹窗状态（失败日志 / 正常日志 / 埋码事件）
    var showFailureLogDialog by remember { mutableStateOf(false) }
    var showNormalLogDialog by remember { mutableStateOf(false) }
    var showEventsLogDialog by remember { mutableStateOf(false) }
    var failureLogVersion by remember { mutableStateOf(0) }
    var normalLogVersion by remember { mutableStateOf(0) }
    var eventsLogVersion by remember { mutableStateOf(0) }

    top.yukonga.miuix.kmp.basic.Scaffold(
        modifier = modifier,
        contentWindowInsets = contentInsets,
        // 玻璃态：容器底色透明，让最底 WallpaperLayer 透出
        containerColor = Color.Transparent,
        topBar = {
            if (!embedded) {
                TopAppBar(
                    title = stringResource(R.string.settings),
                    navigationIcon = {
                        if (onBack != null) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .clickable { onBack() }
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // UI v2：embedded=true 时顶栏不渲染，挂 scrollBehavior 反而吞掉 LazyColumn 滚动事件
                //（TopAppBar 不存在时 MiuixScrollBehavior 仍持有 nestedScrollConnection）
                .then(
                    if (!embedded) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier
                )
                .background(MiuixTheme.colorScheme.surface)
                .padding(padding)
                // v3.0.9：embedded 时列表在 dock 上缘截断（与 MainActivity 任务列表同款处理）——
                // 无 backdrop-blur，内容穿透 dock 会直接堆在 dock 下方手势区
                // v3.1.6：上缘对齐 **dock 胶囊实测上缘** `navbar + GlassSpec.DockTopOffset`
                //（DockBottomInset 10 + DockCapsuleH 64 = 74dp；v3.1.3 实测胶囊高 62.9dp、顶边 navbar+73dp）。
                // 旧值 56dp 是 v3.1.2 之前 38dp 老 dock 时期的残值，会让内容钻到胶囊下面 18dp。
                .then(
                    if (embedded) Modifier.padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + GlassSpec.DockTopOffset
                    ) else Modifier
                ),
            // embedded=内嵌「我的」tab：dock overlay 浮层，预留 dock 高防末项被遮
            // v3.0.7（P3-9）：实测 110dp 仍不够——「项目主页」末项（y≈2194）压在 dock 内不可见 → 提到 150dp
            // v3.1.6 重核：150dp 是**旧假设**（认为内容会钻到 dock 下面，故按「dock 高 96 + 描述行 24 +
            //   安全间距 30」补偿）。现在视口底已被 dock 上缘钳住（上面那条 padding），滚动到底时末项底边
            //   天然落在 `navbar + 74 + contentPadding` 处，150dp 只会让列表尾部空掉一大块 → 收到 12dp 呼吸余量。
            // 注意：SettingsPage **没有 FAB**（FAB 只挂 HistoryPage），所以这里不需要 FabBottomOffset。
            contentPadding = PaddingValues(
                bottom = if (embedded) 12.dp
                else 20.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.size(12.dp))
            }

            // ★v3.2.2：主题分组 —— 五套配色可直接切换。
            // 旧版「暖砂」是写死在 GlassTokens 里的单一配色，用户不喜欢也没有退路。
            item {
                SettingsGroup(stringResource(R.string.theme_group_title)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeId.entries.forEach { id ->
                            ThemeSwatch(
                                id = id,
                                selected = currentTheme == id,
                                onClick = { GlassThemeState.select(id, context) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                SettingsGroup(stringResource(R.string.download_options)) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.create_live_photos),
                        description = stringResource(R.string.create_live_photos_desc),
                        checked = uiState.createLivePhotos,
                        onCheckedChange = onCreateLivePhotosChange
                    )

                    MiuixSwitchWidget(
                        title = stringResource(R.string.selective_download),
                        description = stringResource(R.string.selective_download_desc),
                        checked = uiState.selectiveDownload,
                        onCheckedChange = onSelectiveDownloadChange
                    )

                    MiuixSwitchWidget(
                        title = stringResource(R.string.debug_notifications),
                        description = stringResource(R.string.debug_notifications_desc),
                        checked = uiState.debugNotificationEnabled,
                        onCheckedChange = onDebugNotificationChange
                    )

                    MiuixSwitchWidget(
                        title = stringResource(R.string.keep_screen_on),
                        description = stringResource(R.string.keep_screen_on_desc),
                        checked = uiState.keepScreenOn,
                        onCheckedChange = onKeepScreenOnChange
                    )
                }
            }

            item {
                SettingsGroup(stringResource(R.string.clipboard)) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.manual_input_links),
                        description = stringResource(R.string.manual_input_links_desc),
                        checked = uiState.manualInputLinks,
                        onCheckedChange = onManualInputLinksChange
                    )

                    if (!uiState.manualInputLinks) {
                        MiuixSwitchWidget(
                            title = stringResource(R.string.show_clipboard_bubble),
                            description = stringResource(R.string.show_clipboard_bubble_desc),
                            checked = uiState.showClipboardBubble,
                            onCheckedChange = onShowClipboardBubbleChange
                        )

                        MiuixSwitchWidget(
                            title = stringResource(R.string.auto_read_clipboard),
                            description = stringResource(R.string.auto_read_clipboard_desc),
                            checked = uiState.autoReadClipboard,
                            onCheckedChange = onAutoReadClipboardChange
                        )
                    }
                }
            }

            item {
                SettingsGroup(stringResource(R.string.file_naming)) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.enable_custom_naming),
                        description = stringResource(R.string.enable_custom_naming_desc),
                        checked = uiState.useCustomNaming,
                        onCheckedChange = onUseCustomNamingChange
                    )

                    if (uiState.useCustomNaming) {
                        TextField(
                            value = uiState.template,
                            onValueChange = onTemplateChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            label = stringResource(R.string.naming_template),
                            enabled = uiState.useCustomNaming,
                            singleLine = false,
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { }),
                            trailingIcon = {
                                if (uiState.useCustomNaming) {
                                    Icon(
                                        imageVector = MiuixIcons.Refresh,
                                        contentDescription = stringResource(R.string.reset_template),
                                        modifier = Modifier
                                            .padding(end = 16.dp)
                                            .size(20.dp)
                                            .clickable { onResetTemplate() }
                                    )
                                }
                            }
                        )

                        Text(
                            text = stringResource(R.string.insert_placeholder_hint),
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )

                        TokenGrid(
                            tokens = uiState.tokens,
                            enabled = uiState.useCustomNaming,
                            onInsert = { placeholder ->
                                val current = uiState.template
                                val selectionStart = current.selection.start.coerceAtLeast(0)
                                val selectionEnd = current.selection.end.coerceAtLeast(0)
                                val min = minOf(selectionStart, selectionEnd)
                                val max = maxOf(selectionStart, selectionEnd)
                                val newText = buildString {
                                    append(current.text.substring(0, min))
                                    append(placeholder)
                                    append(current.text.substring(max))
                                }
                                val newSelection = min + placeholder.length
                                onTemplateChange(
                                    TextFieldValue(
                                        text = newText,
                                        selection = androidx.compose.ui.text.TextRange(newSelection)
                                    )
                                )
                            }
                        )
                    }
                }
            }

            item {
                SettingsGroup(stringResource(R.string.notification_group_title)) {
                    MiuixSwitchWidget(
                        title = stringResource(R.string.download_complete_notification),
                        description = stringResource(R.string.download_complete_notification_desc),
                        checked = uiState.downloadCompleteNotification,
                        onCheckedChange = onDownloadCompleteNotificationChange
                    )
                }
            }

            item {
                SettingsGroup(stringResource(R.string.diagnostics_group_title)) {
                    BasicComponent(
                        title = stringResource(R.string.failure_log_title),
                        summary = stringResource(R.string.failure_log_settings_desc),
                        onClick = { showFailureLogDialog = true }
                    )
                    BasicComponent(
                        title = stringResource(R.string.normal_log_title),
                        summary = stringResource(R.string.normal_log_settings_desc),
                        onClick = { showNormalLogDialog = true }
                    )
                    BasicComponent(
                        title = stringResource(R.string.events_log_title),
                        summary = stringResource(R.string.events_log_settings_desc),
                        onClick = { showEventsLogDialog = true }
                    )
                }
            }

            item {
                SettingsGroup(stringResource(R.string.about)) {
                    BasicComponent(
                        title = stringResource(R.string.version),
                        summary = stringResource(R.string.version_with_prefix, BuildConfig.VERSION_NAME),
                        onClick = { /* No action */ }
                    )
                    BasicComponent(
                        title = stringResource(R.string.project_home),
                        summary = stringResource(R.string.project_home_desc),
                        onClick = {
                            kotlin.runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/seasonzheng0210/DyXhsDL"))
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // UI v2 S3 日志与诊断弹窗（与 MainActivity 顶部入口共用文案；数据同源）
    // v3.0.8：WindowDialog 在自绘玻璃主题下无面板底（真机全透明）→ 统一改 GlassDialog
    if (showFailureLogDialog) {
        val logCtx = LocalContext.current
        val logContent = remember(failureLogVersion) { DownloadLogger.getLogContent(logCtx) }
        GlassDialog(
            title = stringResource(R.string.failure_log_title),
            onDismiss = { showFailureLogDialog = false }
        ) {
            GlassLogBody(
                text = logContent,
                emptyText = stringResource(R.string.failure_log_empty)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    text = stringResource(R.string.clear_failure_log),
                    onClick = {
                        DownloadLogger.clearFailureLog(logCtx)
                        failureLogVersion++
                        android.widget.Toast.makeText(logCtx, logCtx.getString(R.string.failure_log_cleared), android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    text = stringResource(R.string.copy_log),
                    onClick = {
                        val cm = logCtx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("failure_log", logContent))
                        android.widget.Toast.makeText(logCtx, logCtx.getString(R.string.log_copied), android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = { showFailureLogDialog = false }
                )
            }
        }
    }

    if (showNormalLogDialog) {
        val logCtx = LocalContext.current
        val logContent = remember(normalLogVersion) { DownloadLogger.getNormalLogContent(logCtx) }
        GlassDialog(
            title = stringResource(R.string.normal_log_title),
            onDismiss = { showNormalLogDialog = false }
        ) {
            GlassLogBody(
                text = logContent,
                emptyText = stringResource(R.string.normal_log_empty)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    text = stringResource(R.string.clear_normal_log),
                    onClick = {
                        DownloadLogger.clearNormalLog(logCtx)
                        normalLogVersion++
                        android.widget.Toast.makeText(logCtx, logCtx.getString(R.string.normal_log_cleared), android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    text = stringResource(R.string.copy_log),
                    onClick = {
                        val cm = logCtx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("normal_log", logContent))
                        android.widget.Toast.makeText(logCtx, logCtx.getString(R.string.log_copied), android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = { showNormalLogDialog = false }
                )
            }
        }
    }

    if (showEventsLogDialog) {
        val evCtx = LocalContext.current
        val eventsContent = remember(eventsLogVersion) { EventTracker.getEventsLogContent(evCtx) }
        GlassDialog(
            title = stringResource(R.string.events_log_title),
            summary = stringResource(R.string.events_log_desc),
            onDismiss = { showEventsLogDialog = false }
        ) {
            GlassLogBody(
                text = eventsContent,
                emptyText = stringResource(R.string.events_log_empty),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    text = stringResource(R.string.clear_events_log),
                    onClick = {
                        EventTracker.clear(evCtx)
                        eventsLogVersion++
                        android.widget.Toast.makeText(evCtx, evCtx.getString(R.string.events_log_cleared), android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    text = stringResource(R.string.copy_log),
                    onClick = {
                        val cm = evCtx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("events_log", eventsContent))
                        android.widget.Toast.makeText(evCtx, evCtx.getString(R.string.log_copied), android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(Modifier.width(8.dp))
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = { showEventsLogDialog = false }
                )
            }
        }
    }
}

/**
 * ★v3.2.2：分组容器 —— **标题并入容器内**，成为组内第一行。
 *
 * 旧实现是 `SmallTitle` 单独一个 item **浮在页面背景上**、下面再跟一个**实心** `Card`：
 * 标题处没有容器、容器上面没有标题，两块材质不同 → 用户反馈的「割裂感」。
 * 现在标题与所有行同在一个容器、同一种材质，看上去是一整组。
 *
 * 标题用强调色（跟随主题）：MIUI 设置的分节标题就是这么做的 —— 标题比正文小一档时，
 * 用颜色拉开层级比用留白更清楚。
 */
@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentFg(),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
        )
        content()
    }
}

/**
 * 主题色卡：渐变方块 + 名字。选中态 = 白勾 + 加粗 + 强调色。
 *
 * ★5 张卡横排：屏宽 360dp − 卡左右各 16 = 328dp，减去 4 个 10dp 间距，
 * 每张 ≈57.6dp —— 单看色块宽 > 48dp 达标；高度上色块 44dp + 标签 ≈ 68dp 整体可点。
 */
@Composable
private fun ThemeSwatch(
    id: ThemeId,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = GlassThemes.of(id)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(p.gradientStart, p.gradientEnd)))
                .then(
                    if (selected) Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = MiuixIcons.Ok,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = id.label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accentFg() else MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

@Composable
private fun MiuixSwitchWidget(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val toggleAction = {
        onCheckedChange(!checked)
    }

    BasicComponent(
        title = title,
        summary = description,
        onClick = toggleAction,
        endActions = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun TokenGrid(
    tokens: List<NamingFormat.TokenDefinition>,
    enabled: Boolean,
    onInsert: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 12.dp, bottom = 16.dp)
    ) {
        tokens.forEach { token ->
            TokenChip(
                token = token,
                enabled = enabled,
                onInsert = onInsert,
                modifier = Modifier.fillMaxWidth(0.48f)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TokenChip(
    token: NamingFormat.TokenDefinition,
    enabled: Boolean,
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MiuixTheme.colorScheme.surface
    Card(
        modifier = modifier
            .border(width = 1.dp, color = borderColor, shape = ContinuousRoundedRectangle(16.dp))
            .then(if (enabled) Modifier.clickable { onInsert(token.placeholder) } else Modifier),
        cornerRadius = 14.dp,
        colors = CardDefaults.defaultColors(
            // ★surfaceVariant 也是**白色半透覆盖层**（亮 白@55% / 暗 白@15%）。
            //   旧写法 .copy(alpha = 0.5f) 是**替换** alpha → 暗色下把 15% 抬成 50%，
            //   结果「禁用」卡比「可用」卡还亮（affordance 反了）。
            //   按 token 自身 alpha 等比降低才对：亮 0.55→0.48（几乎不变），暗 0.15→0.13。
            color = if (enabled) MiuixTheme.colorScheme.surfaceVariant
            else {
                val sv = MiuixTheme.colorScheme.surfaceVariant
                sv.copy(alpha = sv.alpha * 0.88f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(token.labelResId),
                fontSize = 16.sp,
                color = if (enabled) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = token.placeholder,
                fontSize = 14.sp,
                color = if (enabled) MiuixTheme.colorScheme.onSurfaceVariantSummary else MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.5f)
            )
        }
    }
}
