package com.neoruaa.xhsdn.ui.glass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 「橙」语义色对 —— 全 App 的**橙色文字 / 橙色图标 / 橙色浅底胶囊**都必须走这里。
 *
 * ⚠️ 不要再直接引用 `MiuixTheme.colorScheme.primary` 当文字或图标色。
 * `primary` 在本项目是**半透明色**（`OrangePrimaryLight = #FF6A2E@0.88` /
 * `OrangePrimaryDark = #FF6A2E@0.45`），它是给「实底主按钮」用的**打底色**——
 * 暗色下它自己叠在深色玻璃上会合成成深棕橙，当前景用对比度只有 ~1.9:1，等于看不见。
 *
 * 定稿规范（`docs/design/首页下载-交互重构-mockup.html` `:root`）：
 * ```
 *   亮   --primaryText:#E2560E      --fill:rgba(255,106,46,.16)
 *   暗   --primaryText:#FFB37A      --fill:rgba(255,106,46,.22)
 * ```
 * 代码里对应的现成令牌就是 `GlassTokens.DownloadingLight/Dark`
 * （与 `OrangeTextDeep` 同值）以及新增的 `GlassTokens.FillLight/FillDark`。
 *
 * 实测对比度（自建模型，已用实测 1.16:1 / 5.29:1 两个点标定通过）：
 * | 场景 | 比值 | 判定 |
 * |---|---|---|
 * | 暗色 primary 当前景 | **1.92:1** | ✗ 缺陷（本文件的由来） |
 * | 暗色 primary@0.16 底 + primary 前景 | **1.78:1** | ✗ 缺陷（对应实测 1.16:1） |
 * | 暗色 FillDark 底 + [#FFB37A] 前景 | **6.70:1** | ✓（对应实测 5.29:1） |
 * | 暗色 [#FFB37A] 直接当前景 | **9.34:1** | ✓ |
 * | 亮色 primary 当前景 | 2.34:1 | △ 品牌取舍 |
 * | 亮色 [#E2560E] 当前景（规范值） | **3.35:1** | △ 品牌取舍，但优于代码 |
 * 结论：**暗色侧是真缺陷，亮色侧是品牌/渲染取舍**（规范自身也只有 3.35:1）。
 */

/** 「橙文字 / 橙图标」前景色（亮 = `--primaryText #E2560E`，暗 = `#FFB37A`）。 */
@Composable
fun accentFg(): Color =
    if (isSystemInDarkTheme()) GlassTokens.DownloadingDark else GlassTokens.DownloadingLight

/**
 * 「橙浅底」（胶囊 / chip / 来源条 / 头像底 / 推荐标）——
 * 亮 = `--fill` 的 `.16`，暗 = `--fill` 的 `.22`，均为 `#FF6A2E`（规范原值）。
 *
 * @param lightAlpha 仅亮色生效（规范 `.16`）；暗色固定走规范 `.22`，
 *   因为暗色下浅底太淡会在深玻璃上完全消失。
 */
@Composable
fun accentBg(lightAlpha: Float = 0.16f): Color =
    if (isSystemInDarkTheme()) GlassTokens.FillDark
    else GlassTokens.FillLight.copy(alpha = lightAlpha)

/**
 * 「实心玻璃」表面 —— 需要压住下方内容时用（常驻操作条 / 统计格 / 最近作者芯片）。
 *
 * ⚠️ **不要写 `MiuixTheme.colorScheme.surface.copy(alpha = X)`**。
 * 本项目 `surface` 的暗色值是 `GlassTokens.PanelDark = 白@14%` —— 一块**白色半透覆盖层**。
 * `copy(alpha)` 是**替换**而非相乘，所以 `.copy(alpha = 0.94f)` 得到的是 **白@94%**，
 * 暗色下就是一块**白板**。实测后果：主页解析操作条底 = (242,242,242)，
 * 而条上状态文字用 `onSurface`(暗=#F5F6F8) → **1.04:1，字完全看不见**（亮色侧正常 16:1）。
 * 同一模式还命中「最近作者」芯片 0.7、统计格 0.6、以及设置的禁用卡 0.5（禁用比可用还亮）。
 *
 * 亮色侧保持**纯白叠加**（与旧渲染逐像素一致，零回归）；暗色侧改用**近黑实心**。
 *
 * @param lightAlpha 亮色不透明度（= 旧 `surface.copy(alpha=…)` 的取值）
 * @param darkAlpha  暗色不透明度（压近黑，默认 0.90）
 */
@Composable
fun glassSolid(lightAlpha: Float, darkAlpha: Float = 0.90f): Color =
    if (isSystemInDarkTheme()) GlassTokens.SolidDark.copy(alpha = darkAlpha)
    else Color.White.copy(alpha = lightAlpha)
