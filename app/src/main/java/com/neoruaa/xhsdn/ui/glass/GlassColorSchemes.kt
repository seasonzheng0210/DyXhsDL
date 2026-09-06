package com.neoruaa.xhsdn.ui.glass

import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 玻璃色板：基于 Miuix lightColorScheme()/darkColorScheme() 默认工厂，
 * **只覆写背景族 + 主色**（其余 53 角色沿用 Miuix 默认，含文字/禁用态层级），
 * 返回 [Colors] 注入 MiuixTheme(colors = …)。
 *
 * 亮/暗各一套；调用方按 isSystemInDarkTheme() 二选一。
 * 注：注入固定色板后不再跟随 Android 12+ Monet 系统动态色（锁品牌徕卡橙，已裁定）。
 */
object GlassColorSchemes {

    fun light(): Colors = lightColorScheme(
        // 背景族：background 透明 → 露出最底层 WallpaperLayer 渐变
        background = Color_Transparent,
        // 页面面板（HistoryPage 大卡等 surface 容器）
        surface = GlassTokens.PanelLight,
        // 内容卡（任务卡/预览卡/设置分组 surfaceVariant 容器）
        surfaceVariant = GlassTokens.CardLight,
        // 悬浮/浮层内分组
        surfaceContainer = GlassTokens.CardElevatedLight,
        surfaceContainerHigh = GlassTokens.CardLight,
        surfaceContainerHighest = GlassTokens.PanelLight,
        // 主操作（Button/开关/进度走 primary）
        primary = GlassTokens.OrangePrimaryLight,
        onPrimary = GlassTokens.OrangeOnLight,
        onSurface = GlassTokens.TextPrimaryLight,
        onSurfaceSecondary = GlassTokens.TextSecondaryLight,
        onBackground = GlassTokens.TextPrimaryLight,
        onBackgroundVariant = GlassTokens.TextSecondaryLight
    )

    fun dark(): Colors = darkColorScheme(
        background = Color_Transparent,
        surface = GlassTokens.PanelDark,
        surfaceVariant = GlassTokens.CardDark,
        surfaceContainer = GlassTokens.CardElevatedDark,
        surfaceContainerHigh = GlassTokens.CardDark,
        surfaceContainerHighest = GlassTokens.PanelDark,
        primary = GlassTokens.OrangePrimaryDark,
        onPrimary = GlassTokens.OrangeOnDark,
        onSurface = GlassTokens.TextPrimaryDark,
        onSurfaceSecondary = GlassTokens.TextSecondaryDark,
        onBackground = GlassTokens.TextPrimaryDark,
        onBackgroundVariant = GlassTokens.TextSecondaryDark
    )

    // lightColorScheme/darkColorScheme 的参数默认即含透明语义兜底；
    // 直接以 0 透明度 Color 常量注入（避免依赖 Miuix 内是否提供 transparent 角色）。
    private val Color_Transparent = androidx.compose.ui.graphics.Color.Transparent
}
