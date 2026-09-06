package com.neoruaa.xhsdn.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 玻璃态主操作按钮装饰（对齐 UI v2 令牌：rgba(255,105,0,.82) + 顶内高光 + 微投影）。
 *
 * 用法：包裹任意主操作（FAB Card / Miuix primary Button / 对话框确认钮），
 * 子组件负责底色与点击；本层只加：
 *  - 轻投影（亮色可见，暗色几乎归零，符合深底不投影惯例）；
 *  - 顶内高光：白色垂直渐变压在最上层，形成玻璃反光条；
 *  - 统一裁切圆角（内容自身的圆角不必与本层一致，本层裁切会收敛视觉）。
 */
@Composable
fun GlassPrimaryWrap(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .shadow(
                elevation = if (dark) 1.dp else 4.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color(0x26000000),
                spotColor = if (dark) Color(0x0A000000) else Color(0x4D000000)
            )
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        content()
        // 顶内高光：顶部 1px 级白色渐变（暗色压低避免刺眼）
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.White.copy(alpha = if (dark) 0.15f else 0.42f),
                        0.22f to Color.White.copy(alpha = if (dark) 0.05f else 0.16f),
                        0.55f to Color.White.copy(alpha = 0f)
                    )
                ),
                size = Size(size.width, size.height * 0.55f)
            )
        }
    }
}
