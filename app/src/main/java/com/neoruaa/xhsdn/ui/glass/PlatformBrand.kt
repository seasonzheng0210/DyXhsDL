package com.neoruaa.xhsdn.ui.glass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 平台品牌色 / 平台名的**唯一入口**。
 *
 * 此前这三段 `when` 在三个文件里各抄了一份（`MainActivity` 任务卡来源条、
 * `TaskDetailActivity.SourcePill`、`WebViewActivity` 平台标识），任一处改色就会不一致。
 * v3.1.6 收成令牌 + 本文件的两个函数。
 *
 * 色值真源 = [GlassTokens] 的 `BrandLight` / `BrandDark` 两族 token（明暗两态）：
 * | 平台 | 亮 | 暗 | 说明 |
 * |---|---|---|---|
 * | 快手 | `#FE5000` | `#FF7A3D` | 品牌橙 |
 * | 抖音 | `#26292F` | `#E8EAEF` | 品牌青 `#25F4EE` 白底不可见 → 改用近黑/近白 |
 * | 小红书 | `#FE2C55` | `#FF6B81` | 品牌红 |
 *
 * ⚠️ 与 `MiuixTheme.colorScheme.primary`（半透明打底色）不同：**品牌色是不透明色**，
 * 所以「`color.copy(alpha = .15)` 当底 + 同色当字」是安全的（暗色实测 4.4~12:1）。
 */
@Composable
fun platformBrandColor(source: String?): Color {
    val dark = isSystemInDarkTheme()
    return when (source) {
        "kuaishou" -> if (dark) GlassTokens.KuaishouBrandDark else GlassTokens.KuaishouBrandLight
        "douyin", "douyin_home" -> if (dark) GlassTokens.DouyinBrandDark else GlassTokens.DouyinBrandLight
        else -> if (dark) GlassTokens.XhsBrandDark else GlassTokens.XhsBrandLight
    }
}

/** 平台中文名。未知来源一律按小红书兜底（与旧实现一致）。 */
fun platformLabel(source: String?): String = when (source) {
    "kuaishou" -> "快手"
    "douyin", "douyin_home" -> "抖音"
    else -> "小红书"
}
