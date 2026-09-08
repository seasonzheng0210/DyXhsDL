package com.neoruaa.xhsdn.ui.glass

import androidx.compose.ui.graphics.Color

/**
 * 液态玻璃视觉令牌（v3.0 定稿，方向裁定 2026-09-08：高饱和液态渐变 + 折射玻璃）。
 * 定稿值来自 docs/design/LiquidGlass-液态玻璃定稿总览-v3.html。
 *
 * 相对 v2.2.0 软光玻璃的换代：
 *  - 壁纸：低饱和暖调 → 高饱和液态渐变（亮=粉彩 / 暗=霓虹），玻璃透出的颜色随壁纸呼吸；
 *  - 玻璃更透：Chrome 白 70%→50%、Card 白 85%→55%，让壁纸色真正透出；
 *  - 折射描边：1px 半透描边 + 1.5px 顶部内高光（水珠边缘光）；
 *  - 主操作：GlassPrimaryWrap 绘制「橙→珊瑚粉」120° 液态渐变装饰层（primary 单色打底 + 渐变叠加）；
 *  - 语义色高饱和：完成翠绿 / 失败珊瑚 / 等待琥珀 / 下载渐变橙，明暗双态。
 *
 * 语义分层不变：Chrome（顶栏/底栏/分段/胶囊）最透 → 内容层（面板/任务卡）稍实防叠字 → 壁纸层。
 * 调参只动本文件 → 全 App 生效（色板在 GlassColorSchemes 里引用本令牌）。
 */
object GlassTokens {

    // ── 壁纸（液态高饱和渐变：亮=粉彩 / 暗=霓虹，对角）─────────────────
    val WallpaperLight = listOf(
        Color(0xFFB7A6FF), // 薰衣草紫
        Color(0xFFFFB7CC), // 蜜桃粉
        Color(0xFFA9E0F2), // 雾蓝
        Color(0xFFFFC9A0)  // 杏橙
    )
    val WallpaperDark = listOf(
        Color(0xFF241B45), // 电紫
        Color(0xFF3B1E38), // 酒红
        Color(0xFF14283E), // 深海蓝
        Color(0xFF3A2415)  // 琥珀
    )

    // ── Chrome 层玻璃（亮）─────────────────────────────────────────
    /** 顶栏/底栏/分段容器：白 50%（v2 的 70% 下调 → 壁纸色透出）。 */
    val ChromeLight = Color(0x80FFFFFF)
    /** Chrome 层顶部 1px 内高光（白 90%）。 */
    val ChromeHighlightLight = Color(0xE6FFFFFF)
    /** 折射描边（亮）：1px 半透白，模拟水珠边缘光。 */
    val BorderLight = Color(0xB8FFFFFF)

    // ── 内容层玻璃（亮）────────────────────────────────────────────
    /** 页面面板/大容器：白 50%（surface 角色，与 Chrome 同透）。 */
    val PanelLight = Color(0x80FFFFFF)
    /** 内容卡（任务卡/预览卡）：白 55%（surfaceVariant 角色），比 v2 透，正文仍可读。 */
    val CardLight = Color(0x8CFFFFFF)
    /** 悬浮/浮层内分组：白 58%。 */
    val CardElevatedLight = Color(0x94FFFFFF)

    // ── 暗色玻璃（白 alpha，深霓虹壁纸之上）──────────────────────────
    val ChromeDark = Color(0x21FFFFFF)      // 白 13%
    val ChromeHighlightDark = Color(0x29FFFFFF) // 白 16%
    val BorderDark = Color(0x33FFFFFF)      // 白 20% 描边
    val PanelDark = Color(0x24FFFFFF)       // 白 14%
    val CardDark = Color(0x26FFFFFF)        // 白 15%
    val CardElevatedDark = Color(0x2BFFFFFF)

    // ── 主操作：液态渐变（橙→珊瑚粉 120°）───────────────────────────
    val GradientStart = Color(0xFFFF8A3C)   // 渐变起：暖橙
    val GradientEnd = Color(0xFFFF5F6D)     // 渐变止：珊瑚粉
    /** 单色打底（primary 角色）：渐变装饰层叠加其上的感知基色。 */
    val OrangePrimaryLight = Color(0xFFFF6A2E).copy(alpha = 0.88f)
    val OrangePrimaryDark = Color(0xFFFF6A2E).copy(alpha = 0.45f)
    /** 亮/暗禁用主操作（灰化，渐变装饰层在 disabled 时隐藏）。 */
    val DisabledPrimaryLight = Color(0xFFCFC7D6).copy(alpha = 0.7f)
    val DisabledPrimaryDark = Color(0xFF8A8A95).copy(alpha = 0.35f)

    // ── 文字（定稿）─────────────────────────────────────────────────
    val TextPrimaryLight = Color(0xFF241F18)
    val TextSecondaryLight = Color(0x8C241F18) // 55% alpha
    val TextPrimaryDark = Color(0xFFF5F6F8)
    val TextSecondaryDark = Color(0x8CF5F6F8)

    // ── 完成态（高饱和翠绿）─────────────────────────────────────────
    val SuccessGreen = Color(0xFF00895C)     // 亮：翠绿（v2 #0E8A56 加深饱和）
    val SuccessGreenDark = Color(0xFF7AE0B8) // 暗：薄荷绿高亮
    // ── 失败态（高饱和珊瑚红）────────────────────────────────────────
    val FailRed = Color(0xFFE23145)          // 亮
    val FailRedDark = Color(0xFFFF9AA6)      // 暗
    // ── 等待态（高饱和琥珀）──────────────────────────────────────────
    val WaitAmber = Color(0xFFC26A00)        // 亮
    val WaitAmberDark = Color(0xFFFFC77D)    // 暗
    // ── 排队态（中性灰）──────────────────────────────────────────────
    val QueueGray = Color(0xFF9AA0A8)        // 亮暗通用
    // ── 下载进行中（渐变橙文字级，亮/暗）──────────────────────────────
    val DownloadingLight = Color(0xFFE2560E)
    val DownloadingDark = Color(0xFFFFB37A)

    // ── 玻璃内元素文字（状态胶囊等彩字浮于玻璃卡上）───────────────────
    val OrangeTextDeep = Color(0xFFE2560E)   // 文字级橙（亮）
    val OrangeOnLight = Color.White
    val OrangeOnDark = Color(0xFFFFD9BE)     // 暗色浅橙字（渐变上白/暖）
}
