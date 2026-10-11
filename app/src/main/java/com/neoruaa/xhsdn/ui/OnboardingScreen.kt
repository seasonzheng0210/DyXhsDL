package com.neoruaa.xhsdn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoruaa.xhsdn.ui.glass.GlassPrimaryWrap
import com.neoruaa.xhsdn.ui.glass.GlassSpec
import com.neoruaa.xhsdn.ui.glass.GlassTokens
import com.neoruaa.xhsdn.ui.glass.WallpaperLayer
import com.neoruaa.xhsdn.ui.glass.accentBg
import com.neoruaa.xhsdn.ui.glass.accentFg
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Play

/**
 * UI v2 S5 —— 首次引导三页（仅全新安装首启展示一次，可跳过）。
 *  - ① 品牌：logo + 名称 + 标语；
 *  - ② 能力：无水印原画 / 主页批量 / 自动识别 三玻璃卡；
 *  - ③ 开始前：通知 / 存储两项说明 + 隐私行。
 * 玻璃皮肤与全 App 一致（WallpaperLayer 底 + 内容层玻璃卡 + 玻璃态橙主按钮）。
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var page by remember { mutableStateOf(0) }
    val dark = isSystemInDarkTheme()
    val textPrimary = if (dark) GlassTokens.TextPrimaryDark else GlassTokens.TextPrimaryLight
    val textSecondary = if (dark) GlassTokens.TextSecondaryDark else GlassTokens.TextSecondaryLight

    BackHandler {
        if (page > 0) page -= 1 // 首屏不允许返回退出，避免误触直接跳过引导
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.navigationBars))
    ) {
        // 壁纸层（最底）
        WallpaperLayer(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部行：返回（次屏起）+ 跳过（非末屏）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (page > 0) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = "返回",
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { page -= 1 },
                        // 美工：原用 textSecondary（次级灰）→ 压在暖色壁纸上对比度仅 ≈2.5:1，
                        // 真机/模拟器上都几乎看不见。改用主字色。
                        tint = textPrimary
                    )
                }
                Spacer(Modifier.weight(1f))
                if (page < 2) {
                    TextButton(text = "跳过", onClick = onDone)
                }
            }

            // 主内容区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // 美工：原内容区无水平内边距 → 标题（"能做什么"/"开始前"…）贴屏幕左边缘，
                    // 真机上视觉呈现为"首位截字"。与底部按钮列统一 28dp 边距。
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                when (page) {
                    0 -> BrandPage(textPrimary, textSecondary)
                    1 -> SkillsPage(textPrimary, textSecondary, dark)
                    else -> PermissionsPage(textPrimary, textSecondary, dark)
                }
            }

            // 底部：指示点 + 主按钮
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { i ->
                        val active = i == page
                        Box(
                            modifier = Modifier
                                .size(if (active) 8.dp else 7.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    // 指示点：选中=实心语义橙，未选中=极淡同色/白色
                                    // ★原先选中态在亮色下用 OrangePrimaryLight（半透明打底色 @0.88），
                                    //   是同类 token 误用；统一走 accentFg / accentBg。
                                    if (active) {
                                        accentFg()
                                    } else {
                                        if (dark) Color.White.copy(alpha = 0.30f) else accentBg(0.28f)
                                    }
                                )
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                GlassPrimaryWrap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    cornerRadius = 26.dp
                ) {
                    Button(
                        onClick = {
                            when (page) {
                                0 -> page = 1
                                1 -> page = 2
                                else -> onDone()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text(
                            text = when (page) {
                                0 -> "了解能力"
                                1 -> "下一步"
                                else -> "开始使用"
                            },
                            color = if (dark) GlassTokens.OrangeOnDark else Color.White,
                            // v3.1.7：16sp/Medium → 收口到 `.btn` 规范 14.5sp/650/.2px。
                            // 它是 52dp 高、26dp 圆角的**同一颗** `.btn`，字号却是全 App 唯一一处 16sp，
                            // 与其它主按钮并列看会明显偏大。真源见 GlassSpec。
                            fontSize = GlassSpec.BtnFontSize,
                            fontWeight = GlassSpec.BtnFontWeight,
                            letterSpacing = GlassSpec.BtnLetterSpacing
                        )
                    }
                }
            }
        }
    }
}

/** ① 品牌页 */
@Composable
private fun BrandPage(textPrimary: Color, textSecondary: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 玻璃 logo 圆：橙渐变 + 白色播放符（★token 化，原先硬编码 0xFFFF8A3C/0xFFF06500）
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GlassTokens.GradientStart, GlassTokens.DownloadingLight)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = MiuixIcons.Play,
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                tint = Color.White
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            text = "抖音小红书下载器",
            color = textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "抖音 · 快手 · 小红书无水印保存，一次到位",
            color = textSecondary,
            fontSize = 14.sp
        )
    }
}

/** ② 能力页 */
@Composable
private fun SkillsPage(textPrimary: Color, textSecondary: Color, dark: Boolean) {
    val cardColor = if (dark) GlassTokens.CardDark else GlassTokens.CardLight
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "能做什么",
            color = textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "三件核心能力",
            color = textSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))
        SkillRow(cardColor, dark, MiuixIcons.Download, "无水印原画", "视频与图文原画质保存", textPrimary, textSecondary)
        Spacer(Modifier.height(12.dp))
        SkillRow(cardColor, dark, MiuixIcons.File, "主页批量", "一次抓取博主全部作品", textPrimary, textSecondary)
        Spacer(Modifier.height(12.dp))
        SkillRow(cardColor, dark, MiuixIcons.Link, "自动识别", "剪贴板与分享直达解析", textPrimary, textSecondary)
    }
}

@Composable
private fun SkillRow(
    cardColor: Color,
    dark: Boolean,
    icon: ImageVector,
    name: String,
    desc: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        colors = CardDefaults.defaultColors(color = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    // ★浅底用 --fill（#FF6A2E@.16/.22），不要用 primary（半透明打底色）
                    .background(accentBg()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp),
                    tint = accentFg()
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(text = name, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(text = desc, color = textSecondary, fontSize = 12.5.sp)
            }
        }
    }
}

/** ③ 开始前（权限说明）页 */
@Composable
private fun PermissionsPage(textPrimary: Color, textSecondary: Color, dark: Boolean) {
    val cardColor = if (dark) GlassTokens.CardDark else GlassTokens.CardLight
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "开始前",
            color = textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "两项系统权限，可随时在设置里调整",
            color = textSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))
        PermRow(cardColor, dark, MiuixIcons.Info, "下载完成通知", "下载完成或失败时在通知栏提醒你", textPrimary, textSecondary)
        Spacer(Modifier.height(12.dp))
        PermRow(cardColor, dark, MiuixIcons.Download, "保存到本机", "视频与图文保存在本机文件夹", textPrimary, textSecondary)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(GlassTokens.SuccessGreen)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "所有文件仅保存在本机，不上传任何服务器",
                color = textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PermRow(
    cardColor: Color,
    dark: Boolean,
    icon: ImageVector,
    name: String,
    desc: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        colors = CardDefaults.defaultColors(color = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = accentFg()
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = name, color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(text = desc, color = textSecondary, fontSize = 12.sp)
            }
        }
    }
}
