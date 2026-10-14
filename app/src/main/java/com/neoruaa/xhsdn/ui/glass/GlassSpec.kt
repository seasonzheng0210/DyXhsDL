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
 * 5) ★★**v0.2 按钮阶梯（分级放大，2026-10-14 用户拍板）** —— 真源
 *    `docs/design/视觉基线稿-v0.2-分级按钮-玻璃只留控制层.html`
 *
 *    | 层级 | 高度 | 圆角 | 字号 | 水平内边距 | 用途 |
 *    |---|---|---|---|---|---|
 *    | **L1 主 CTA** | **60dp** | 30dp | 16sp | 28dp | 确认下载 / 解析作品 / 开始使用 / 完成 |
 *    | L2 次级 | 52dp | 26dp | 14.5sp | 20dp | 从剪贴板粘贴 / 重试 / 查看文件（＝原 `.btn`） |
 *    | L3 图标按钮 | **48dp** | 24dp | 图标 24dp | — | 返回 / ✕ / 更多 |
 *    | FAB | **56dp** | 圆形 | 图标 24dp | — | 右下角主操作 |
 *
 *    ★**为什么只涨 L1、不一起涨**：Apple HIG「**使用样式而不是大小在视觉上区分多个选项中的首选项**」
 *      且「在每个视图中保留一到两个突出的按钮」——把两个不同大小的按钮并排会显得不一致。
 *      层级靠**颜色与形状**表达，大小只用来把「唯一的主 CTA」抬高一级。
 *    ★**依据**：Apple HIG 按钮（tap ≥44×44pt、必须带按压态）/ Google 无障碍（触摸目标 ≥48×48dp、
 *      相邻目标间距 ≥8dp）/ M3 Buttons（默认高 **40dp**，现状 52dp 本来就高于它）/
 *      M3 Expressive 五档 XS32·S40·M48·L56·XL64（**L1 = XL**）/ NN/g（主 CTA ≈2cm ≈ 最小值的两倍）。
 *
 * 6) ★**按压态**（Apple HIG：「始终为自定义按钮包括按压状态」）：
 *    圆角 30→26（形状形变）、缩放 [BtnPressScale]、内阴影加深；**120ms 进入 / 180ms 回弹**。
 *
 * ★ 使用陷阱（踩过两次，改按钮前必读）：
 *   1. **`GlassPrimaryWrap` 的内层 Button 必须显式 `.height(BtnHeight)`**。外层是 52dp 的 `Box`，
 *      内层不给高度就按 Miuix 默认（≈41dp）量测，而 `Box` 默认 `contentAlignment = TopStart`
 *      → **文字整体偏上**（实测偏 5.6dp）。`insideMargin` 的 vertical 一律给 0。
 *   2. **不写 `fontSize` 的 Miuix Button 吃默认样式 ≈17sp** → 玻璃主按钮必须显式写
 *      `fontSize / fontWeight / letterSpacing`。
 */
object GlassSpec {

    // ── L2 次级动作（＝原 `.btn`，值不变 → 已装机的 3 处主按钮继续保持 52dp）────
    /** L2 次级动作高度 52dp（＝mockup `.btn` 的 52px）。**本值不参与 L1**。 */
    val BtnHeight = 52.dp

    /** L2 圆角 26dp（= 高度一半，正胶囊）。 */
    val BtnCornerRadius = 26.dp

    /** L2 字号 14.5sp。 */
    val BtnFontSize = 14.5.sp

    /** 字距 0.2sp（L1 / L2 共用）。 */
    val BtnLetterSpacing = 0.2.sp

    /** 字重 650（Compose 无 650，取 SemiBold=600）。 */
    val BtnFontWeight = FontWeight.SemiBold

    /** `.actionbar .btn{min-width:132px}` —— 固定宽度而非 minWidth，避免换文案时宽度跳动。 */
    val BtnBarWidth = 132.dp

    /** `.actionbar .btn{padding:0 20px}`。 */
    val BtnBarPadH = 20.dp

    // ── ★★L1 主 CTA（v0.2 新档，只有「每个视图唯一的主操作」用它）──────────────
    /**
     * L1 主 CTA 高度 **60dp** —— 比 L2 高 8dp。
     *
     * 用法（必读）：`GlassPrimaryWrap` 的内层 Button 必须显式 `.height(BtnL1Height)`，
     * 否则按 Miuix 默认 ≈41dp 量测、外层 Box `contentAlignment=TopStart` 会让**文字整体偏上**。
     * 真源 = `视觉基线稿-v0.2` 的 `.b1`。
     */
    val BtnL1Height = 60.dp

    /** L1 圆角 30dp（正胶囊；按下时形变到 [BtnPressCornerRadius]）。 */
    val BtnL1CornerRadius = 30.dp

    /** L1 字号 16sp。 */
    val BtnL1FontSize = 16.sp

    /** L1 水平内边距 28dp。 */
    val BtnL1PadH = 28.dp

    // ── L3 图标按钮（返回 / ✕ / 更多）───────────────────────────────────────
    /** L3 图标按钮边长 **48dp** —— Google 无障碍触摸目标底线，不得再小。 */
    val IconBtnSize = 48.dp

    /** L3 圆角 24dp（正圆内切方形）。 */
    val IconBtnCornerRadius = 24.dp

    /** L3 图标本身边长 24dp（＝24dp 图标网格的视觉主体在 20dp 那一档的上一级）。 */
    val IconBtnIconSize = 24.dp

    /** 相邻触摸目标之间的最小间距（Google 无障碍：48×48dp + **≥8dp** 间距）。 */
    val MinTouchGap = 8.dp

    // ── 按压态（Apple HIG 要求「始终为自定义按钮包括按压状态」）────────────────
    /** 按下缩放：0.975（`视觉基线稿-v0.2` 的 `.btn.down{transform:scale(.975)}`）。 */
    const val BtnPressScale = 0.975f

    /** 按下时圆角（= L1 的 30 − 4，制造「方一点」的形状形变）。 */
    val BtnPressCornerRadius = 26.dp

    /** 按下进入时长（ms）。 */
    const val BtnPressInMs = 120

    /** 松开回弹时长（ms）。 */
    const val BtnPressOutMs = 180

    // ── dock / FAB（mockup 的 .dock + LGv3 的 .fab）──────────────────────────
    /** mockup `.dock{height:64px}` —— dock 胶囊的目标视觉高度。 */
    val DockCapsuleH = 64.dp

    /** LGv3 `.nav{bottom:10px}` —— 胶囊底边距导航栏（手势条）顶边的内边距。 */
    val DockBottomInset = 10.dp

    /** dock 胶囊顶边相对导航栏顶边的偏移（FAB / 气泡的最低下界）。 */
    val DockTopOffset = DockBottomInset + DockCapsuleH

    /** 底部导航项图标：Material bottom nav 标准 24dp。实测 mockup `.dock .tab .g` 渲染高正好 24px。 */
    val DockIconSize = 24.dp

    /**
     * FAB 直径 **56dp** —— M3 FAB regular 标准档（原按 LGv3 `.fab{46px}` 取 46dp，比 M3 小 10dp；
     * v0.2 按「按钮尽量大」+ M3 规范上调）。
     * ★改本值会自动带动 [FabBottomOffset] 下游的 `listBottomReserve`/气泡偏移 —— 它们都从 dock/FAB 推导，别写死。
     */
    val FabSize = 56.dp

    /** LGv3 `.fab{right:16px}`。 */
    val FabEndPadding = 16.dp

    /** FAB 内图标 24dp（M3 FAB regular 前导图标标准；原 22dp）。 */
    val FabIconSize = 24.dp

    /** FAB 与 dock 顶边的间隙：LGv3 实测 66 - 49 = **17px**。 */
    val FabDockGap = 17.dp

    /** FAB 底边偏移 = dock 顶边 + 间隙。 */
    val FabBottomOffset = DockTopOffset + FabDockGap

    /** 剪贴板气泡底边偏移 = FAB 顶边 + 8dp 间隙。 */
    val ClipBubbleBottomOffset = FabBottomOffset + FabSize + 8.dp
}
