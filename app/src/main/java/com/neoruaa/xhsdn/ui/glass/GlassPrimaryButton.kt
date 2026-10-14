package com.neoruaa.xhsdn.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import kotlin.math.min

/** 按钮尺寸档位（v0.2 阶梯）。真值全在 [GlassSpec]，这里只是选择项。 */
enum class GlassBtnTier { L1, L2 }

@Composable
private fun pressScale(interactionSource: MutableInteractionSource): Pair<Float, Boolean> {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) GlassSpec.BtnPressScale else 1f,
        animationSpec = tween(if (pressed) GlassSpec.BtnPressInMs else GlassSpec.BtnPressOutMs),
        label = "btnPressScale"
    )
    return scale to pressed
}

/** 按下时圆角收一档（正胶囊 → 略方，M3E 的 shape morph on press）。L2 的 26dp 与按压值相同 → 无形变，只有 L1 会形变。 */
private fun pressRadius(base: Dp, pressed: Boolean): Dp =
    if (pressed) min(base.value, GlassSpec.BtnPressCornerRadius.value).dp else base

/**
 * 主操作按钮（v0.2 按钮阶梯的**唯一落点**）。
 *
 * 为什么要有这一层：之前「主按钮」是各页手写 `GlassPrimaryWrap { Button { Text } }`，
 * 20 处重复、尺寸各写各的，而且**没有任何按压态** —— Apple HIG 明确要求
 * **「始终为自定义按钮包括按压状态」**。这里把尺寸档位 / 按压态 / 排版收成一个真源，各页只选档位。
 *
 * | 档 | 高 | 圆角 | 字号 | 水平内边距 |
 * |---|---|---|---|---|
 * | [GlassBtnTier.L1] 主 CTA | **60dp** | 30dp | 16sp | 28dp |
 * | [GlassBtnTier.L2] 次级 | 52dp | 26dp | 14.5sp | 20dp |
 *
 * ★**只给「每个视图唯一的主操作」用 L1**。Apple HIG：「使用样式而不是大小在视觉上区分多个选项中的
 * 首选项」，两个不同大小的按钮并排会显得不一致 —— 层级靠颜色与形状表达，大小只把 L1 抬一级。
 *
 * ⚠️ **高度必须显式写**（[GlassSpec.BtnL1Height] / [GlassSpec.BtnHeight]）：不写高度时
 * Miuix Button 按默认 ≈41dp 量测，而外层 Box 默认 `contentAlignment = TopStart`
 * → **文字整体偏上 5.6dp**（v3.1.5 的坑，别再踩）。
 *
 * @param tier 尺寸档位；默认 [GlassBtnTier.L2]，与既有按钮一致，避免误升级
 * @param barWidth 传值则固定宽度（操作条传 [GlassSpec.BtnBarWidth]，换文案时宽度不跳动）
 */
@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tier: GlassBtnTier = GlassBtnTier.L2,
    enabled: Boolean = true,
    barWidth: Dp? = null,
    maxLines: Int = 1
) {
    val l1 = tier == GlassBtnTier.L1
    val h = if (l1) GlassSpec.BtnL1Height else GlassSpec.BtnHeight
    val r = if (l1) GlassSpec.BtnL1CornerRadius else GlassSpec.BtnCornerRadius
    val fs = if (l1) GlassSpec.BtnL1FontSize else GlassSpec.BtnFontSize
    val padH = if (l1) GlassSpec.BtnL1PadH else GlassSpec.BtnBarPadH

    val interactionSource = remember { MutableInteractionSource() }
    val (scale, pressed) = pressScale(interactionSource)
    val radius = pressRadius(r, pressed)

    val sized = (if (barWidth != null) modifier.width(barWidth) else modifier.fillMaxWidth()).height(h)

    if (enabled) {
        GlassPrimaryWrap(
            modifier = sized
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(radius))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            cornerRadius = radius
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = fs,
                    fontWeight = GlassSpec.BtnFontWeight,
                    letterSpacing = GlassSpec.BtnLetterSpacing,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = padH)
                )
            }
        }
    } else {
        // 禁用态不走渐变（会把白字一起打掉）→ 换成真正的禁用态胶囊，**尺寸完全一致**
        // （"位置稳定 > 省空间"在禁用态同样成立，v3.1.5 定过）。
        Button(
            onClick = { },
            enabled = false,
            modifier = sized,
            cornerRadius = r,
            colors = ButtonDefaults.buttonColors()
        ) {
            Text(
                text = text,
                fontSize = fs,
                fontWeight = GlassSpec.BtnFontWeight,
                letterSpacing = GlassSpec.BtnLetterSpacing,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 次级（非渐变）玻璃按钮：与 [GlassPrimaryButton] **同档同尺寸**，只有底不同（mockup `.btn.ghost`）。
 * 同样带按压态，避免同一屏里「主按钮有反馈、次级没有」。
 */
@Composable
fun GlassTonalButton(
    text: String,
    onClick: () -> Unit,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    tier: GlassBtnTier = GlassBtnTier.L2
) {
    val l1 = tier == GlassBtnTier.L1
    val h = if (l1) GlassSpec.BtnL1Height else GlassSpec.BtnHeight
    val r = if (l1) GlassSpec.BtnL1CornerRadius else GlassSpec.BtnCornerRadius
    val fs = if (l1) GlassSpec.BtnL1FontSize else GlassSpec.BtnFontSize

    val interactionSource = remember { MutableInteractionSource() }
    val (scale, pressed) = pressScale(interactionSource)
    val radius = pressRadius(r, pressed)

    Box(
        modifier = modifier
            .height(h)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(radius))
            .background(background, RoundedCornerShape(radius))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = fs,
            fontWeight = GlassSpec.BtnFontWeight,
            letterSpacing = GlassSpec.BtnLetterSpacing
        )
    }
}
