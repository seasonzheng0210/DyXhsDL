package com.neoruaa.xhsdn.ui.glass

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * 五态可插拔调色板（v3.2.2）。
 *
 * 背景：v3.2.0 的「暖砂」是**写死在 `GlassTokens` 里的单一配色**，用户不喜欢也没有退路。
 * 本文件把「颜色」从常量改成**按当前主题返回**的一等对象，之后加减主题只改这里。
 *
 * ## 为什么调用点零改动
 * `GlassTokens` 的 46 个成员全部改写成 getter，内部委托到这里；
 * 129 处 `GlassTokens.X` 的写法不用动一行。
 *
 * ## 为什么能响应式
 * [GlassThemeState.current] 是 Compose **快照态**（`mutableStateOf`）。
 * 凡是**在 composable 里读** `GlassTokens.X` 的地方都会订阅它 → 换主题立即重组合；
 * 非 composable 处读也无副作用（取当前值）。
 *
 * ## 三条不能破的规则（前两轮都用实机事故换来的）
 * 1. **表面只有三级**：① 控制层玻璃 ② 内容层实心 ③ 浮层最实。
 * 2. **暗色梯子 = 亮色侧镜像**：壁纸最暗 → 内容卡次亮 → 浮层最亮。
 *    写反的后果是卡片/空态图标砖在壁纸上「消失」（v3.2.0 实机复现过一次）。
 * 3. **控制层必须偏实**：Android 无 `backdrop-filter`，太透会让内容**穿透 dock**
 *    （v3.0.9 用户实测打回）。这里直接由内容卡色推导 @90%，永不过透。
 *
 * 壁纸一律**低饱和** —— 这不是审美洁癖：Compose 没有实时模糊，只靠纯色叠加做玻璃，
 * 高饱和壁纸会把每一层表面的色相拉飞、正文也压不住。
 */

enum class ThemeId(val key: String, val label: String) {
    MIST("mist", "晨雾蓝"),
    GRAPHITE("graphite", "石墨"),
    MIDNIGHT("midnight", "极夜靛"),
    PINE("pine", "苔原绿"),
    GLAZE("glaze", "暮釉")
}

/**
 * 一套完整配色。
 *
 * 只需声明**主题真正之间有差别的**那几个色（壁纸 / 三级表面 / 强调 / 渐变 / 文字），
 * 其余（Chrome 玻璃、描边、轨道、禁用态、品牌色、语义色）由默认值或构造函数派生，
 * 避免每套主题都抄一遍 46 个色导致漂移。
 */
data class GlassPalette(
    val id: ThemeId,

    // ── 壁纸（低饱和三停，对角渐变）──────────────────────────────────
    val wallpaperLight: List<Color>,
    val wallpaperDark: List<Color>,

    // ── ② 内容层实心（卡片 / 统计格 / 缩略图 / 任务卡）────────────────
    val cardLight: Color,
    val cardDark: Color,

    // ── ③ 浮层最实（操作条 / sheet / 对话框）──────────────────────────
    val cardElevatedLight: Color = Color(0xFFFFFFFF),
    val cardElevatedDark: Color,

    // ── 强调色（取代固定小米橙；链接/图标/浅底/下载中都走它）────────────
    val accentLight: Color,
    val accentDark: Color,
    /** 主按钮液态渐变（起→止）。白字压其上，两端都要求 ≥3:1（已体检）。 */
    val gradientStart: Color,
    val gradientEnd: Color,

    // ── 文字三级 ────────────────────────────────────────────────────
    val textPrimaryLight: Color,
    val textPrimaryDark: Color,
    val textSecondaryLight: Color = textPrimaryLight.copy(alpha = 0.55f),
    val textSecondaryDark: Color = textPrimaryDark.copy(alpha = 0.55f),
    val textTertiaryLight: Color = textPrimaryLight.copy(alpha = 0.38f),
    val textTertiaryDark: Color = textPrimaryDark.copy(alpha = 0.36f),

    // ── 页面薄纱（全 App 唯一保留半透的整页底）────────────────────────
    val panelLight: Color = Color(0x80FFFFFF),
    val panelDark: Color = Color(0x1CFFFFFF),

    // ── 轨道 / 中立灰───────────────────────────────────────────────
    val trackLight: Color = textPrimaryLight.copy(alpha = 0.10f),
    val trackDark: Color = Color(0x24FFFFFF)
) {
    // ── ① 控制层玻璃：由内容卡色推导，永不过透 ─────────────────────────
    val surfaceControlLight: Color = Color(0xE8FFFFFF)      // 亮：白 91%
    val surfaceControlDark: Color = cardDark.copy(alpha = 0.90f)
    val surfaceControlLineLight: Color = Color(0xB8FFFFFF)
    val surfaceControlLineDark: Color = Color(0x33FFFFFF)
    val surfaceControlHiLight: Color = Color(0xF2FFFFFF)
    val surfaceControlHiDark: Color = Color(0x38FFFFFF)

    // ── Chrome 玻璃（顶栏/底栏/分段容器）与折射描边 ────────────────────
    val chromeLight: Color = Color(0x80FFFFFF)
    val chromeHighlightLight: Color = Color(0xE6FFFFFF)
    val borderLight: Color = Color(0xB8FFFFFF)
    val chromeDark: Color = Color(0x21FFFFFF)
    val chromeHighlightDark: Color = Color(0x29FFFFFF)
    val borderDark: Color = Color(0x33FFFFFF)

    /** mockup 留档值（`.sheet` 的 `--cardHi`）；★不参与渲染，见 GlassTokens 注释。 */
    val cardHiLight: Color = Color(0x9EFFFFFF)
    val cardHiDark: Color = Color(0x2EFFFFFF)

    /** 实心玻璃暗色基色（`GlassAccent.glassSolid` 用）—— 取浮层最实色，保证最亮。 */
    val solidDark: Color = cardElevatedDark

    // ── 主按钮打底色 / 禁用态 ─────────────────────────────────────────
    /**
     * ⚠️ 这是**半透明打底色**，只给实底主按钮当底（上面还会叠一层液态渐变）。
     * 当前景/文字用会看不见 —— 文字级强调色请走 [accentLight] / [accentDark]。
     */
    val primaryLight: Color = accentLight.copy(alpha = 0.88f)
    val primaryDark: Color = accentDark.copy(alpha = 0.45f)
    val disabledPrimaryLight: Color = Color(0xFFCFC7D6).copy(alpha = 0.7f)
    val disabledPrimaryDark: Color = Color(0xFF8A8A95).copy(alpha = 0.35f)

    /** 强调浅底（chip / 头像底 / 来源条）：亮 .16 / 暗 .22。 */
    val fillLight: Color = accentLight.copy(alpha = 0.16f)
    val fillDark: Color = accentDark.copy(alpha = 0.22f)

    /** 渐变上的字。 */ 
    val onPrimaryLight: Color = Color.White
    val onPrimaryDark: Color = Color.White
}

object GlassThemes {
    /** ① 晨雾蓝 —— 冷调干净，最中性安全，默认主题。 */
    val Mist = GlassPalette(
        id = ThemeId.MIST,
        wallpaperLight = listOf(Color(0xFFE9EEF4), Color(0xFFD7DFEA), Color(0xFFC2CEDD)),
        wallpaperDark = listOf(Color(0xFF161C24), Color(0xFF10151B), Color(0xFF0A0E13)),
        cardLight = Color(0xFFF7F9FB),
        cardDark = Color(0xFF1E242C),
        cardElevatedDark = Color(0xFF2A323C),
        accentLight = Color(0xFF0A5AA6),
        accentDark = Color(0xFF8FCAF5),
        gradientStart = Color(0xFF1E6FD4),
        gradientEnd = Color(0xFF4590DE),
        textPrimaryLight = Color(0xFF101720),
        textPrimaryDark = Color(0xFFEEF2F7)
    )

    /** ② 石墨 —— 近单色高对比，主操作近黑/近白，最「专业」，也最不抢内容的注意力。 */
    val Graphite = GlassPalette(
        id = ThemeId.GRAPHITE,
        wallpaperLight = listOf(Color(0xFFEEEEF0), Color(0xFFE1E1E4), Color(0xFFD2D2D6)),
        wallpaperDark = listOf(Color(0xFF17181A), Color(0xFF101113), Color(0xFF0B0C0D)),
        cardLight = Color(0xFFF8F8F9),
        cardDark = Color(0xFF212225),
        cardElevatedDark = Color(0xFF2E2F32),
        accentLight = Color(0xFF1B1C1F),
        accentDark = Color(0xFFF0F1F3),
        gradientStart = Color(0xFF2B2C31),
        gradientEnd = Color(0xFF4C4E55),
        textPrimaryLight = Color(0xFF151618),
        textPrimaryDark = Color(0xFFF2F3F5)
    )

    /** ③ 极夜靛 —— 深靛提质，暗色侧质感最强。 */
    val Midnight = GlassPalette(
        id = ThemeId.MIDNIGHT,
        wallpaperLight = listOf(Color(0xFFE8EAF4), Color(0xFFD4D8EA), Color(0xFFBDC3DF)),
        wallpaperDark = listOf(Color(0xFF191C2C), Color(0xFF12151F), Color(0xFF0C0E16)),
        cardLight = Color(0xFFF7F8FD),
        cardDark = Color(0xFF212539),
        cardElevatedDark = Color(0xFF2D3247),
        accentLight = Color(0xFF3D47C6),
        accentDark = Color(0xFFAEB5F8),
        gradientStart = Color(0xFF4A54D6),
        gradientEnd = Color(0xFF7C85F0),
        textPrimaryLight = Color(0xFF14162B),
        textPrimaryDark = Color(0xFFEDF0FB)
    )

    /** ④ 苔原绿 —— 低饱和松绿，暖中有冷，长时间看不刺眼。 */
    val Pine = GlassPalette(
        id = ThemeId.PINE,
        wallpaperLight = listOf(Color(0xFFE5EBE4), Color(0xFFD3DDD1), Color(0xFFBFCFBE)),
        wallpaperDark = listOf(Color(0xFF151C18), Color(0xFF0F1512), Color(0xFF0A0E0C)),
        cardLight = Color(0xFFF6F9F6),
        cardDark = Color(0xFF1D2420),
        cardElevatedDark = Color(0xFF29322C),
        accentLight = Color(0xFF14603C),
        accentDark = Color(0xFF86D7AC),
        gradientStart = Color(0xFF1D7A4E),
        gradientEnd = Color(0xFF419E6C),
        textPrimaryLight = Color(0xFF111A14),
        textPrimaryDark = Color(0xFFEAF4EE)
    )

    /** ⑤ 暮釉 —— 梅紫/玫瑰釉，唯一偏女性向的一套，也是最不像「工具类」的一套。 */
    val Glaze = GlassPalette(
        id = ThemeId.GLAZE,
        wallpaperLight = listOf(Color(0xFFF0E8EF), Color(0xFFE0D4E0), Color(0xFFCDBFCF)),
        wallpaperDark = listOf(Color(0xFF1D1723), Color(0xFF16111C), Color(0xFF100C14)),
        cardLight = Color(0xFFF9F6FA),
        cardDark = Color(0xFF251E2B),
        cardElevatedDark = Color(0xFF312839),
        accentLight = Color(0xFF832F6E),
        accentDark = Color(0xFFEFA9D8),
        gradientStart = Color(0xFF9C3A84),
        gradientEnd = Color(0xFFC56AAE),
        textPrimaryLight = Color(0xFF1A1120),
        textPrimaryDark = Color(0xFFF5EDF8)
    )

    val all: List<GlassPalette> = listOf(Mist, Graphite, Midnight, Pine, Glaze)

    fun of(id: ThemeId): GlassPalette = when (id) {
        ThemeId.MIST -> Mist
        ThemeId.GRAPHITE -> Graphite
        ThemeId.MIDNIGHT -> Midnight
        ThemeId.PINE -> Pine
        ThemeId.GLAZE -> Glaze
    }
}

/**
 * 主题状态：持久化在 `XHSDownloaderPrefs` 的 `ui_theme` 键。
 *
 * ★[current] 必须是 Compose 快照态 —— composable 里通过 `GlassTokens.X` 读它，
 * 换主题才能立即重组合，不用回退 Catalyst/重启 App。
 */
object GlassThemeState {
    private const val PREFS = "XHSDownloaderPrefs"
    private const val KEY = "ui_theme"

    var current: ThemeId by mutableStateOf(ThemeId.MIST)
        private set

    val palette: GlassPalette get() = GlassThemes.of(current)

    /** Application.onCreate 里调一次；之后读写都用内存态。 */
    fun init(context: Context) {
        val saved = runCatching {
            val key = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, ThemeId.MIST.key)
            ThemeId.entries.firstOrNull { it.key == key }
        }.getOrNull()
        current = saved ?: ThemeId.MIST
    }

    fun select(id: ThemeId, context: Context? = null) {
        current = id
        val ctx = context ?: return
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, id.key).apply()
    }
}
