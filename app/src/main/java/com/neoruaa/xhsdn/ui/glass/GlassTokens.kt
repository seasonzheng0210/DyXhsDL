package com.neoruaa.xhsdn.ui.glass

import androidx.compose.ui.graphics.Color

/**
 * 玻璃态视觉令牌（定稿值来自 docs/design/UIv2-软光玻璃定稿总览.html）。
 *
 * 语义分层（2026-09-06 用户裁定认可）：
 *  - Chrome 层（顶栏/底栏/胶囊/分段）：高透磨砂，壁纸透出最明显处；
 *  - 内容层（页面面板/任务卡/预览卡）：近实心微透，防滚动叠字与下层穿帮；
 *  - 壁纸层：Canvas 平滑渐变（亮/暗两套），渐变天然平滑，无需运行时 blur。
 * 调参只动本文件 → 全 App 生效（色板在 GlassColorSchemes 里引用本令牌）。
 */
object GlassTokens {

    // ── 壁纸（徕卡暖调四色，对角渐变）───────────────────────────────
    val WallpaperLight = listOf(
        Color(0xFFF4E7D6), // 米白
        Color(0xFFE0A06B), // 焦糖
        Color(0xFFBFD0C6), // 鼠尾草
        Color(0xFFB2CCDC)  // 雾蓝
    )
    val WallpaperDark = listOf(
        Color(0xFF2A2320), // 深棕暖
        Color(0xFF3B3026), // 深焦糖
        Color(0xFF2B3330), // 深鼠尾草
        Color(0xFF25323C)  // 深雾蓝
    )

    // ── Chrome 层玻璃（亮）─────────────────────────────────────────
    /** 顶栏/底栏/搜索胶囊/分段等：白 70%。 */
    val ChromeLight = Color(0xB3FFFFFF)
    /** Chrome 层顶部 1px 内高光（白 90%）。 */
    val ChromeHighlightLight = Color(0xE6FFFFFF)

    // ── 内容层玻璃（亮）────────────────────────────────────────────
    /** 页面面板/大容器：白 90%（surface 角色）。 */
    val PanelLight = Color(0xE6FFFFFF)
    /** 内容卡（任务卡/预览卡）：白 85%（surfaceVariant 角色），近实心防叠字。 */
    val CardLight = Color(0xD9FFFFFF)
    /** 悬浮/浮层内分组：白 78%。 */
    val CardElevatedLight = Color(0xC7FFFFFF)

    // ── 暗色玻璃（白 alpha，深壁纸之上）─────────────────────────────
    val ChromeDark = Color(0x1FFFFFFF)      // 白 12%
    val ChromeHighlightDark = Color(0x24FFFFFF) // 白 14%
    val PanelDark = Color(0x3DFFFFFF)       // 白 24%
    val CardDark = Color(0x33FFFFFF)        // 白 20%
    val CardElevatedDark = Color(0x2BFFFFFF)

    // ── 玻璃态小米橙（主操作）───────────────────────────────────────
    val OrangePrimaryLight = Color(0xFF6900).copy(alpha = 0.82f) // rgba(255,105,0,.82)
    val OrangePrimaryDark = Color(0xFFFF6900).copy(alpha = 0.40f)
    val OrangeTextDeep = Color(0xFFE05A00)   // 文字级橙
    val OrangeOnLight = Color.White
    val OrangeOnDark = Color(0xFFFFC9A3)     // 暗色浅橙字

    // ── 文字（定稿）─────────────────────────────────────────────────
    val TextPrimaryLight = Color(0xFF241F18)
    val TextSecondaryLight = Color(0x8C241F18) // 55% alpha
    val TextPrimaryDark = Color(0xFFF5F6F8)
    val TextSecondaryDark = Color(0x8CF5F6F8)

    // ── 完成态 ─────────────────────────────────────────────────────
    val SuccessGreen = Color(0xFF0E8A56)
}
