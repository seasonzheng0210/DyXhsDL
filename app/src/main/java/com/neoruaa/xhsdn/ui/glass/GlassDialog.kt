package com.neoruaa.xhsdn.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 液态玻璃弹窗（v3.0.8 统一）。
 *
 * 背景：Miuix 的 WindowDialog 在自绘玻璃主题下「无面板底 + 内容直浮」→ 真机上表现为
 * 全透明（用户报「点失败日志也是透明的」「底部按钮透明」）。本项目 v3.0.4 起已把
 * 「清除已完成」「手动输入链接」两处改为自绘玻璃卡（r22 + 近实玻璃底 + 折射描边），
 * 本文件把该语言收敛成通用组件，替换剩余全部 WindowDialog。
 *
 * 结构对齐「清除已完成」弹窗：
 *  - 原生 [Dialog] 自带 scrim（dim 遮罩），保证弹窗与背景有明确层级；
 *  - 圆角 22 + 近实玻璃底（亮 0xF7FFFFFF / 暗 0xF21E1B28）+ 1px 折射描边 + 阴影；
 *  - 标题 16sp SemiBold 主文字色；副标题 13sp 次级色；
 *  - 按钮行：左侧取消为 TextButton，右侧确认为 GlassPrimaryWrap 渐变胶囊（可选）。
 *
 * @param onDismiss 点遮罩 / 返回键触发
 * @param title 标题
 * @param summary 可选副标题（说明文案）
 * @param confirmText 确认按钮文案；null 表示不渲染确认按钮（如纯日志查看弹窗）
 * @param cancelText 取消按钮文案；null 表示不渲染
 * @param onConfirm 确认回调
 * @param contentPadding 内容区自定义（日志弹窗用）
 */
@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    title: String,
    summary: String? = null,
    confirmText: String? = null,
    cancelText: String? = null,
    onConfirm: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null
) {
    val dark = isSystemInDarkTheme()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(if (dark) Color(0xF51E1B28) else Color(0xF9FFFFFF))
                .border(
                    1.dp,
                    if (dark) GlassTokens.BorderDark else GlassTokens.BorderLight,
                    RoundedCornerShape(22.dp)
                )
                .padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight
            )
            if (!summary.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = summary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = if (dark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight
                )
            }
            if (content != null) {
                Spacer(Modifier.height(12.dp))
                content()
            }
            if (confirmText != null || cancelText != null) {
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (cancelText != null) {
                        TextButton(
                            text = cancelText,
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (confirmText != null) {
                        GlassPrimaryWrap(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            cornerRadius = 26.dp
                        ) {
                            Button(
                                onClick = { onConfirm?.invoke() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(Color.Transparent, Color.White),
                                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = confirmText,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 玻璃弹窗内的日志正文容器（等宽字体 + 可滚动 + 上限高度）。
 * 供「失败日志 / 正常日志 / 埋码日志」三处复用，避免样式漂移。
 */
@Composable
fun GlassLogBody(
    text: String,
    emptyText: String,
    maxHeight: androidx.compose.ui.unit.Dp = 320.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp
) {
    val dark = isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(if (dark) Color(0x14FFFFFF) else Color(0x0F000000))
            .border(
                1.dp,
                if (dark) Color(0x1FFFFFFF) else Color(0x14000000),
                RoundedCornerShape(14.dp)
            )
            .padding(10.dp)
    ) {
        if (text.isBlank()) {
            Text(
                text = emptyText,
                fontSize = 13.sp,
                color = if (dark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight
            )
        } else {
            Text(
                text = text,
                fontSize = fontSize,
                lineHeight = (fontSize.value * 1.45f).sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}
