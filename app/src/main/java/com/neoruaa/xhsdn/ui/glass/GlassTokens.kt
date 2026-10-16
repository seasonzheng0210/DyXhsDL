package com.neoruaa.xhsdn.ui.glass

import androidx.compose.ui.graphics.Color

/**
 * 液态玻璃视觉令牌 —— **v3.2.2 起是「当前主题」的一层代理**。
 *
 * ## 这一版做了什么
 * 颜色值全部搬到 [GlassPalette]（5 套主题），本文件的每个成员改成 `get()` 委托到
 * [GlassThemeState.palette]。**129 处 `GlassTokens.X` 的调用点一行都不用改**，
 * 但换主题时读到的就是新值。
 *
 * ## 为什么能即时生效
 * [GlassThemeState.current] 是 Compose 快照态，composable 里读这些 getter 即订阅该状态；
 * 非 composable 里读也安全（取当前值）。
 *
 * ## 三条不能破的规则（前几轮都是实机事故换来的）
 * 1. **表面只有三级**：① 控制层玻璃（`SurfaceControl*`，顶栏/dock/操作条/分段/FAB）
 *    ② 内容层实心（`CardLight/Dark`）③ 浮层最实（`CardElevated*`）。
 *    照 Apple HIG 材质指引：**「不要在内容层中使用 Liquid Glass」**。
 * 2. **暗色梯子必须是亮色侧的镜像**：壁纸最暗 → 内容卡次亮 → 浮层最亮。
 *    写反的后果是卡片/空态图标砖在壁纸上「消失」（v3.2.0 实机复现过）。
 * 3. **控制层必须偏实**：Android 无 `backdrop-filter`，太透会让内容**穿透 dock**
 *    （v3.0.9 用户实测打回）。现在控制层直接由内容卡色推导，不可能再过透。
 *
 * @see GlassPalette 真源 —— 改配色改那里，不要在本文件写死取值。
 */
object GlassTokens {

    /** 当前调色板（读它会取得快照态，composable 里读即订阅主题变化）。 */
    private val p: GlassPalette get() = GlassThemeState.palette

    // ════════════════════════════════════════════════════════════════
    // 壁纸
    // ════════════════════════════════════════════════════════════════
    /**
     * ★壁纸一律**低饱和** —— 不只是审美：Compose 没有 `backdrop-filter`，只靠**纯色叠加**做玻璃，
     * 高饱和壁纸会把每层表面的色相拉飞、也压不住正文。这是没模糊时唯一能做层次的前提。
     */
    val WallpaperLight: List<Color> get() = p.wallpaperLight
    val WallpaperDark: List<Color> get() = p.wallpaperDark

    // ════════════════════════════════════════════════════════════════
    // ① 控制层玻璃（顶栏 / dock / 操作条 / 分段 / FAB）
    // ════════════════════════════════════════════════════════════════
    val SurfaceControlLight: Color get() = p.surfaceControlLight
    val SurfaceControlDark: Color get() = p.surfaceControlDark
    val SurfaceControlLineLight: Color get() = p.surfaceControlLineLight
    val SurfaceControlLineDark: Color get() = p.surfaceControlLineDark
    val SurfaceControlHiLight: Color get() = p.surfaceControlHiLight
    val SurfaceControlHiDark: Color get() = p.surfaceControlHiDark

    val ChromeLight: Color get() = p.chromeLight
    val ChromeHighlightLight: Color get() = p.chromeHighlightLight
    val BorderLight: Color get() = p.borderLight
    val ChromeDark: Color get() = p.chromeDark
    val ChromeHighlightDark: Color get() = p.chromeHighlightDark
    val BorderDark: Color get() = p.borderDark

    // ════════════════════════════════════════════════════════════════
    // 页面薄纱 + ② 内容层实心 + ③ 浮层最实
    // ════════════════════════════════════════════════════════════════
    /**
     * 页面面板（`surface` 角色，Settings / WebView 整页底）。
     * ★**全 App 唯一保留半透**的表面 —— 它本质是「壁纸上的整页薄纱」，变实心会把壁纸整页盖掉。
     */
    val PanelLight: Color get() = p.panelLight
    val PanelDark: Color get() = p.panelDark

    /**
     * 内容卡（`surfaceVariant`）—— 11 处 `CardDefaults.defaultColors(color = surfaceVariant)` 都走这里。
     * ★自 v0.2 起是**实心**（Apple HIG：内容层不用 Liquid Glass）。
     */
    val CardLight: Color get() = p.cardLight
    val CardDark: Color get() = p.cardDark

    /** 浮层最实（sheet / 对话框 / 常驻操作条）★三级里最亮。 */
    val CardElevatedLight: Color get() = p.cardElevatedLight
    val CardElevatedDark: Color get() = p.cardElevatedDark

    /**
     * mockup 留档值（`.sheet` 的 `--cardHi`）。**不参与渲染**：mockup 的 sheet 靠
     * `backdrop-filter:blur(34px)`，Compose 没有该能力，直接铺会让下层文字整片穿透
     * （v3.1.7 首版实测）→ 浮层一律走 `GlassAccent.glassSolid()` 实心表面。
     */
    val CardHiLight: Color get() = p.cardHiLight
    val CardHiDark: Color get() = p.cardHiDark

    /** 轨道底（`--track`）：步进方钮 / 开关关闭态 / 分隔线。★是中性底，别拿 `Fill*` 顶替。 */
    val TrackLight: Color get() = p.trackLight
    val TrackDark: Color get() = p.trackDark

    /**
     * 「实心玻璃」暗色基色 —— `GlassAccent.glassSolid()` 用。取浮层色保证三级里最亮；
     * 近黑会让常驻操作条 / sheet 像在页面里挖了个洞。
     */
    val SolidDark: Color get() = p.solidDark

    // ════════════════════════════════════════════════════════════════
    // 强调色（v3.2.2：跟随主题，不再恒为小米橙）
    // ════════════════════════════════════════════════════════════════
    /** 主按钮液态渐变（起→止）。白字压其上，两端均已体检 ≥3:1。 */
    val GradientStart: Color get() = p.gradientStart
    val GradientEnd: Color get() = p.gradientEnd

    /**
     * 文字级强调色 —— 取代旧「橙」的全局入口（`GlassAccent.accentFg()` 走这里）。
     * ⚠️ 不要拿 [OrangePrimaryLight] 当文字色：那是**半透明打底色**，暗色下只有 ~1.9:1。
     */
    val DownloadingLight: Color get() = p.accentLight
    val DownloadingDark: Color get() = p.accentDark

    /** 旧名兼容：`OrangeTextDeep` 与 [DownloadingLight] 同值。 */
    val OrangeTextDeep: Color get() = p.accentLight

    /**
     * ⚠️ **半透明打底色**，只给实底主按钮当底（上面还会叠一层液态渐变）。
     * 当文字/图标色用会看不见（见 GlassAccent 注释）。
     */
    val OrangePrimaryLight: Color get() = p.primaryLight
    val OrangePrimaryDark: Color get() = p.primaryDark

    /** 禁用态主操作（渐变层在 disabled 时不画，露出这层灰）。 */
    val DisabledPrimaryLight: Color get() = p.disabledPrimaryLight
    val DisabledPrimaryDark: Color get() = p.disabledPrimaryDark

    /** 强调浅底（chip / 头像底 / 来源条）：亮 .16 / 暗 .22。是**打底色**，不可当字。 */
    val FillLight: Color get() = p.fillLight
    val FillDark: Color get() = p.fillDark

    /** 渐变上的字（两态都是白 —— 渐变两端已保证 ≥3:1）。 */
    val OrangeOnLight: Color get() = p.onPrimaryLight
    val OrangeOnDark: Color get() = p.onPrimaryDark

    // ════════════════════════════════════════════════════════════════
    // 文字三级
    // ════════════════════════════════════════════════════════════════
    val TextPrimaryLight: Color get() = p.textPrimaryLight
    val TextSecondaryLight: Color get() = p.textSecondaryLight
    val TextPrimaryDark: Color get() = p.textPrimaryDark
    val TextSecondaryDark: Color get() = p.textSecondaryDark
    /** 三级文字（`--text3`）：未选中态控件描边用。 */
    val TextTertiaryLight: Color get() = p.textTertiaryLight
    val TextTertiaryDark: Color get() = p.textTertiaryDark

    // ════════════════════════════════════════════════════════════════
    // 平台品牌色 —— ★不随主题变（抖音黑/白、小红书红、快手橙是外部品牌资产，不能染色）
    // ════════════════════════════════════════════════════════════════
    val KuaishouBrandLight = Color(0xFFFE5000)
    val KuaishouBrandDark = Color(0xFFFF7A3D)
    val DouyinBrandLight = Color(0xFF26292F)
    val DouyinBrandDark = Color(0xFFE8EAEF)
    val XhsBrandLight = Color(0xFFFE2C55)
    val XhsBrandDark = Color(0xFFFF6B81)

    // ════════════════════════════════════════════════════════════════
    // 语义色 —— 五套共用（状态语义必须跨主题一致，否则同一任务换个主题就「换表情」）
    // ════════════════════════════════════════════════════════════════
    val SuccessGreen = Color(0xFF00895C)
    val SuccessGreenDark = Color(0xFF7AE0B8)
    val FailRed = Color(0xFFE23145)
    val FailRedDark = Color(0xFFFF9AA6)
    val WaitAmber = Color(0xFFC26A00)
    val WaitAmberDark = Color(0xFFFFC77D)
    val QueueGray = Color(0xFF9AA0A8)
}
