package com.neoruaa.xhsdn.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * 壁纸层：全局最底层 Canvas 渐变（徕卡暖调四色，对角）。
 *
 * - 渐变平滑 → Chrome 半透明白叠其上即有"磨砂玻璃"观感，无需运行时 blur；
 * - 亮/暗自动切换；零资源零成本（无位图、无 RenderEffect）。
 * 放置：Activity 根 Box 最底，先于所有内容绘制。
 */
@Composable
fun WallpaperLayer(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) GlassTokens.WallpaperDark else GlassTokens.WallpaperLight
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )
    }
}
