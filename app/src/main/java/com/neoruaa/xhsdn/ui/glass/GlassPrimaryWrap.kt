package com.neoruaa.xhsdn.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃主操作按钮装饰（v3.0：橙→珊瑚粉渐变扫光 + 折射描边 + 顶部高光）。
 *
 * 用法：包裹任意主操作（FAB Card / Miuix primary Button / 对话框确认钮），
 * 子组件负责底色与点击；本层只加：
 *  - 轻投影（亮色可见，暗色压低，符合深底不投影惯例）；
 *  - **液态渐变扫光**：右下→左上对角叠加「珊瑚粉→透明」低透明渐变 + 顶部白高光，
 *    与半透明 primary 底 + 高饱和壁纸共同形成橙→粉渐变感知（不遮文字/按压反馈）；
 *  - 1px 半透折射描边（水珠边缘光）；
 *  - 统一裁切圆角（内容自身的圆角不必与本层一致，本层裁切会收敛视觉）。
 *
 * @param enabled 是否启用态；false 时隐藏渐变扫光/高光并压低投影（供 FAB 禁用态用）。
 */
@Composable
fun GlassPrimaryWrap(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    enabled: Boolean = true,
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
        // 液态渐变扫光 + 折射描边（enabled 才叠加，禁用态露出灰化底）
        if (enabled) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // 右下珊瑚粉扫光（对角，模拟玻璃对壁纸色的折射透出）
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GlassTokens.GradientEnd.copy(alpha = if (dark) 0.26f else 0.34f),
                            Color.Transparent
                        ),
                        start = Offset(size.width, size.height),
                        end = Offset(0f, 0f)
                    )
                )
                // 顶部 1.5px 高光（暗色压低）
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = if (dark) 0.2f else 0.5f),
                            0.25f to Color.White.copy(alpha = if (dark) 0.06f else 0.18f),
                            0.6f to Color.White.copy(alpha = 0f)
                        )
                    ),
                    size = Size(size.width, size.height * 0.55f)
                )
                // 1px 折射描边（边缘光）
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (dark) GlassTokens.BorderDark else GlassTokens.BorderLight,
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(0f, size.height * 0.4f)
                    ),
                    size = Size(size.width, 1.dp.toPx())
                )
            }
        }
    }
}
