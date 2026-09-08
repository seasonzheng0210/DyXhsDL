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
 * 液态玻璃主操作按钮装饰（v3.0.2 修正：渐变底在内容层之下）。
 *
 * v3.0.2 排版验收发现的两个问题（截图实证）：
 *  1. 扫光 Canvas 原先叠在 content 之上 → 34% 粉扫光 + 50% 白高光把白色按钮文字冲糊；
 *  2. FAB 的内容是裸 Box（无底色）→ 渐变层又只在 enabled 叠加 → FAB 实际没底色，浅得隐形。
 *
 * 现结构（对齐 mockup .btn-mini.grad / .fab 的完整渐变实底）：
 *  - enabled 时先画「全量液态渐变底」（120° #FF8A3C→#FF5F6D）+ 顶部高光 + 1px 折射描边；
 *  - content() 画在渐变之上 → 白字永远清晰；
 *  - 调用方的 Button/Box 底色应透明（ContainerColor=Transparent）让渐变透出。
 *  - disabled 时渐变层整体不画 → 露出调用方灰化底（FAB 禁用态）。
 *
 * @param enabled 是否启用态；false 时隐藏渐变底/高光（供 FAB 禁用态用）。
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
        // 液态渐变底 + 高光 + 折射描边（在内容层之下，文字不被盖）
        if (enabled) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // 全量渐变底（mockup: linear-gradient(120deg,#FF8A3C,#FF5F6D)）
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(GlassTokens.GradientStart, GlassTokens.GradientEnd),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.85f, size.height)
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
        content()
    }
}
