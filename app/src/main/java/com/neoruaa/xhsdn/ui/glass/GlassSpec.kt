package com.neoruaa.xhsdn.ui.glass

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 按钮 / 底部导航 / FAB 的**排版规范唯一真源**。
 *
 * 真源文档与取值（已用 Chrome 无头渲染实测，不是抄注释）：
 *
 * 1) `docs/design/首页下载-交互重构-mockup.html` —— **1:1 设备权威**
 *    （`.phone{width:390px}`，实测设备 1080×2340 @440dpi → density 2.75，**1 CSS px = 1 dp = 1 sp**）
 *    ```
 *    .btn{height:52px;border-radius:26px;font-size:14.5px;font-weight:650;letter-spacing:.2px}
 *    .btn.primary{background:var(--grad);color:#fff}
 *    .btn.ghost  {background:var(--card);border:1px solid var(--border)}
 *    .actionbar .btn{min-width:132px;padding:0 20px}
 *    .actionbar .lbl{font-size:13.5px;font-weight:650}
 *    .dock{height:64px}  .dock .tab{font-size:11px}  .dock .tab .g{font-size:17px}
 *    ```
 *    ★ **`.btn.sm{height:40px}` 在规范里定义了但从未被任何页面使用** ——
 *      所以全 App 的动作按钮一律 52dp，不要拿 40dp 当"次级按钮"。
 *
 * 2) `docs/design/LiquidGlass-液态玻璃定稿总览-v3.html` —— 只用来取 **FAB**
 *    ```
 *    .nav{left:13px;right:13px;bottom:10px;border-radius:26px;padding:6px 4px}
 *    .nv{font-size:11px;font-weight:600;padding:5px 0}
 *    .fab{right:16px;bottom:66px;width:46px;height:46px;border-radius:999px;font-size:20px}
 *    ```
 *    ★ **该文件里的 px 是设备值，不是缩略图值。** 实测渲染：`.screen` 仅 232×560，
 *      但 `.nv` 仍是 **11px**（与 mockup 390px 画幅下的 `.dock .tab` **同为 11px**）。
 *      若它是按画幅缩放，这里就该是 ~7px。→ 作者是在窄画幅里按设备值排版，
 *      所以 `.fab{46px}` = **46dp**。（渲染图里内容明显塞不下、底部被裁，也印证这点。）
 *    ⚠️ 但同一文件的 `.btn-grad{38px}` / `.btn-mini{32px}` 是 `.tk` 缩略图样例，**不是设备值**。
 *      v3.0.10 曾把任务卡按钮按 h38/r19 实现，属误读，v3.1.5 已纠正为 52/26。
 *
 * 3) FAB 与 dock 的竖直节奏（实测值，非目测）：
 *    LGv3 `.nav` 渲染高 **39px**、底边距 10px → 顶边在 bottom+49px；
 *    `.fab` 底边 66px → **与 nav 顶边间隙 17px**。
 *    本 App 的 dock 是 **icon+label**（mockup `.dock{height:64px}`，比 LGv3 纯文字 nav 高），
 *    所以 dock 顶边更高；FAB 的偏移必须**从 dock 推导**，不能照抄 66px。
 *
 * 4) 字号并集（三份设计稿取并）= {9.5,10,10.5,11,11.5,12,12.5,13,13.5,14,14.5,15,16,16.5,17,
 *    18,19,20,22,24,26,27,30,34,40}。**代码里真正离轨的只有 9 / 15.5 / 21**（v3.1.5 已收口）。
 *
 * ★ 使用陷阱（踩过两次，改按钮前必读）：
 *   1. **`GlassPrimaryWrap` 的内层 Button 必须显式 `.height(BtnHeight)`**。外层是 52dp 的 `Box`，
 *      内层不给高度就按 Miuix 默认（≈41dp）量测，而 `Box` 默认 `contentAlignment = TopStart`
 *      → **文字整体偏上**（实测偏 5.6dp）。`insideMargin` 的 vertical 一律给 0。
 *   2. **不写 `fontSize` 的 Miuix Button 吃默认样式 ≈17sp** → 玻璃主按钮必须显式写
 *      `fontSize / fontWeight / letterSpacing`。
 */
object GlassSpec {

    // ── .btn（mockup，1:1 设备值）────────────────────────────────────────────
    /** `.btn` 高度 52px。 */
    val BtnHeight = 52.dp

    /** `.btn` 圆角 26px（= 高度一半，正胶囊）。 */
    val BtnCornerRadius = 26.dp

    /** `.btn` 字号 14.5px。 */
    val BtnFontSize = 14.5.sp

    /** `.btn` 字距 .2px。 */
    val BtnLetterSpacing = 0.2.sp

    /** `.btn` 字重 650（Compose 无 650，取 SemiBold=600）。 */
    val BtnFontWeight = FontWeight.SemiBold

    /** `.actionbar .btn{min-width:132px}` —— 固定宽度而非 minWidth，避免换文案时宽度跳动。 */
    val BtnBarWidth = 132.dp

    /** `.actionbar .btn{padding:0 20px}`。 */
    val BtnBarPadH = 20.dp

    // ── dock / FAB（mockup 的 .dock + LGv3 的 .fab）──────────────────────────
    /** mockup `.dock{height:64px}` —— dock 胶囊的目标视觉高度。 */
    val DockCapsuleH = 64.dp

    /** LGv3 `.nav{bottom:10px}` —— 胶囊底边距导航栏（手势条）顶边的内边距。 */
    val DockBottomInset = 10.dp

    /** dock 胶囊顶边相对导航栏顶边的偏移（FAB / 气泡的最低下界）。 */
    val DockTopOffset = DockBottomInset + DockCapsuleH

    /** 底部导航项图标：Material bottom nav 标准 24dp。实测 mockup `.dock .tab .g` 渲染高正好 24px。 */
    val DockIconSize = 24.dp

    /** LGv3 `.fab{width:46px;height:46px}` —— 圆形（`border-radius:999px`）。 */
    val FabSize = 46.dp

    /** LGv3 `.fab{right:16px}`。 */
    val FabEndPadding = 16.dp

    /** LGv3 `.fab{font-size:20px}` —— FAB 内图标约 20~22dp。 */
    val FabIconSize = 22.dp

    /** FAB 与 dock 顶边的间隙：LGv3 实测 66 - 49 = **17px**。 */
    val FabDockGap = 17.dp

    /** FAB 底边偏移 = dock 顶边 + 间隙。 */
    val FabBottomOffset = DockTopOffset + FabDockGap

    /** 剪贴板气泡底边偏移 = FAB 顶边 + 8dp 间隙。 */
    val ClipBubbleBottomOffset = FabBottomOffset + FabSize + 8.dp
}
