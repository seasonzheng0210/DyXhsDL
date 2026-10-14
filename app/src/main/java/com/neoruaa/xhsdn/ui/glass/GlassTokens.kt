package com.neoruaa.xhsdn.ui.glass

import androidx.compose.ui.graphics.Color

/**
 * 液态玻璃视觉令牌（v3.0 定稿，方向裁定 2026-09-08：高饱和液态渐变 + 折射玻璃）。
 * 定稿值来自 docs/design/LiquidGlass-液态玻璃定稿总览-v3.html。
 *
 * 相对 v2.2.0 软光玻璃的换代：
 *  - 壁纸：低饱和暖调 → 高饱和液态渐变（亮=粉彩 / 暗=霓虹），玻璃透出的颜色随壁纸呼吸；
 *  - 玻璃更透：Chrome 白 70%→50%、Card 白 85%→55%，让壁纸色真正透出；
 *  - 折射描边：1px 半透描边 + 1.5px 顶部内高光（水珠边缘光）；
 *  - 主操作：GlassPrimaryWrap 绘制「橙→珊瑚粉」120° 液态渐变装饰层（primary 单色打底 + 渐变叠加）；
 *  - 语义色高饱和：完成翠绿 / 失败珊瑚 / 等待琥珀 / 下载渐变橙，明暗双态。
 *
 * 语义分层不变：Chrome（顶栏/底栏/分段/胶囊）最透 → 内容层（面板/任务卡）稍实防叠字 → 壁纸层。
 * 调参只动本文件 → 全 App 生效（色板在 GlassColorSchemes 里引用本令牌）。
 */
object GlassTokens {

    // ══════════════════════════════════════════════════════════════════════
    // v0.2 令牌层重写（2026-10-14，「暖砂」配色）
    //
    // 结构改动来自 Apple HIG 材质指引：**「不要在内容层中使用 Liquid Glass」** ——
    // 玻璃只用于**控制与导览层**（顶栏 / dock / 分段 / FAB / 开关），
    // 内容层一律换成**实心标准材质**，浮层（操作条 / sheet / 对话框）用**最实**材质。
    // 这一条根治了「表面 6 级 alpha、5 级挤在 12 个百分点内彼此分辨不出」的老病。
    //
    // 表面只有 **3 级**：
    //   ① 控制层玻璃 [SurfaceControl*]  —— 半透，铺在壁纸上
    //   ② 内容层实心 [CardLight/Dark]    —— 实心，卡片/统计格/缩略图/任务卡
    //   ③ 浮层最实   [CardElevatedLight/Dark] —— 实心，操作条/sheet/对话框
    //
    // ⚠️ **暗色表面梯子必须是亮色侧的镜像**：壁纸最暗 → 内容卡次亮 → 浮层最亮。
    //    v0.2 稿首版把 dtop 取成 #141210（比 dsurf 还暗）→ 卡片糊成一片、操作条像挖了个洞。
    // ══════════════════════════════════════════════════════════════════════

    // ── 壁纸（v0.2 暖砂：低饱和三停渐变）──────────────────────────────
    /**
     * ★壁纸降饱和**不只是审美选择** —— Compose 没有 `backdrop-filter`，只靠**纯色叠加**做玻璃，
     * 高饱和壁纸会把每一层半透表面的色相拉飞、也压不住正文。降饱和是没模糊时唯一能做层次的前提。
     * 真源 = `docs/design/视觉基线稿-v0.1-低饱和玻璃三套.html` 的「暖砂」。
     */
    val WallpaperLight = listOf(
        Color(0xFFEADFCB), // 沙白
        Color(0xFFD8C9B1), // 暖砂
        Color(0xFFC4B299)  // 深砂
    )
    val WallpaperDark = listOf(
        Color(0xFF221D18), // 暖褐
        Color(0xFF1A1613),
        Color(0xFF131110)  // 近黑暖
    )

    // ── ① 控制层玻璃（v3.0.9 实测：无 backdrop-blur，纯半透会让内容穿透 dock）──
    /**
     * 控制层表面：顶栏 / dock / 底部操作条 / 分段容器 / FAB。
     *
     * ⚠️ **值必须偏实**：设计稿写的是「白 50%」，那是**在浏览器里有 `backdrop-filter` 的前提下**；
     * Android 无该能力，v3.0.9 用户实测打回「内容滚过 dock 直接穿透」→ 实测提实到 ≈白 91% / 深 90% 才有等效磨砂观感。
     * 本令牌就是把当初散落在 `MainActivity` 里的硬编码值**收敛成一个真源**（值本身不变，避免破功）。
     * 若后续靠「让位铁律」保证内容不滚到控制层下方，可再逐档下调透明度 —— 必须**实机截图验证**，别照抄 50%。
     */
    val SurfaceControlLight = Color(0xE8FFFFFF)   // 白 91%
    val SurfaceControlDark = Color(0xE62E283E)    // 深灰紫 90%
    val SurfaceControlLineLight = Color(0xB8FFFFFF)
    val SurfaceControlLineDark = Color(0x33FFFFFF)
    val SurfaceControlHiLight = Color(0xF2FFFFFF) // 顶部内高光 95%
    val SurfaceControlHiDark = Color(0x38FFFFFF)  // 22%

    // ── Chrome 层玻璃（亮）─────────────────────────────────────────
    /** 顶栏/底栏/分段容器：白 50%（v2 的 70% 下调 → 壁纸色透出）。 */
    val ChromeLight = Color(0x80FFFFFF)
    /** Chrome 层顶部 1px 内高光（白 90%）。 */
    val ChromeHighlightLight = Color(0xE6FFFFFF)
    /** 折射描边（亮）：1px 半透白，模拟水珠边缘光。 */
    val BorderLight = Color(0xB8FFFFFF)

    // ── ② 内容层：实心标准材质（v0.2 起不再是半透玻璃）────────────────
    /**
     * 页面面板（`surface` 角色，Settings / WebView 的整页底）。
     * ★**全 App 只有这一层保留半透** —— 它本质是「壁纸上的整页薄纱」，变实心会把壁纸整页盖掉。
     * 卡片级一律走 [CardLight] 的实心材质。
     */
    val PanelLight = Color(0x80FFFFFF)
    /**
     * 内容卡（任务卡 / 预览卡 / 统计格 / 缩略图底）—— `surfaceVariant` 角色，
     * 全 App 11 处 `CardDefaults.defaultColors(color = surfaceVariant)` 都走这里。
     * ★v0.2 起是**实心暖白**（原白 55% 半透）。Apple HIG：**内容层不使用 Liquid Glass**。真源 `--card #FBF8F2`。
     */
    val CardLight = Color(0xFFFBF8F2)
    /**
     * 浮层最实（sheet / 对话框 / 常驻操作条）—— 亮 = 纯白。真源 `--top #FFFFFF`。
     * ★与暗色的 [CardElevatedDark] 一起构成「浮层在三级里最实/最亮」。
     */
    val CardElevatedLight = Color(0xFFFFFFFF)
    /**
     * 底部面板（`.sheet`）底 —— 对应 mockup `--cardHi`：亮白 62% / 暗白 18%。
     *
     * ⚠️ **实测不可直接用于 sheet 底色**：mockup 的 `.sheet` 靠 `backdrop-filter:blur(34px) saturate(1.9)`
     * 把下层内容糊掉才成立；Compose **没有 backdrop-filter**，直接铺这两个半透令牌会让
     * 下层页面文字整片穿透叠在面板上（v3.1.7 首版实测：「下载计划 / 新增作品 18」与
     * sheet 的「下载范围 / 仅新增」完全重叠）→ 面板一律改用 `glassSolid()` 实心表面。
     * 保留这两个值只为**留档规范真源**，不参与渲染。
     */
    val CardHiLight = Color(0x9EFFFFFF)
    val CardHiDark = Color(0x2EFFFFFF)
    /**
     * 轨道底（`--track`）：步进方钮 / 开关关闭态 / 下载计划卡的分隔线。
     * 亮 `rgba(36,31,24,.10)` / 暗 `rgba(255,255,255,.14)`。★是**中性底**不是橙，别拿 `--fill` 顶替。
     */
    val TrackLight = Color(0x1A241F18)
    val TrackDark = Color(0x24FFFFFF)

    // ── 暗色玻璃（白 alpha，深霓虹壁纸之上）──────────────────────────
    val ChromeDark = Color(0x21FFFFFF)      // 白 13%
    val ChromeHighlightDark = Color(0x29FFFFFF) // 白 16%
    val BorderDark = Color(0x33FFFFFF)      // 白 20% 描边
    /** 页面面板（`surface`，暗）—— 同上，保留半透（原白 14% → 白 11%，与「三级」拉开距离）。 */
    val PanelDark = Color(0x1CFFFFFF)
    /**
     * 内容卡（暗）—— ★必须是**实心近黑暖**，且**比壁纸中段/底部都亮**，否则卡片糊进壁纸。
     * v0.2 稿首版就是在这里写反的（`dsurf #1C1A16` ≈ 壁纸底部）；本轮实机也复现了一次：
     * 壁纸中段 ≈ `#241F1B` 时，`#282219` 几乎同色 → 空态图标砖在壁纸上「消失」。
     * 真源 `--card #282219`，但**暗色壁纸同步压到 `#221D18→#131110`** 才成立。
     */
    val CardDark = Color(0xFF2A241C)
    /** 浮层最实（暗）—— ★三级里**最亮**。真源 `--top #332C22` 同理上调。 */
    val CardElevatedDark = Color(0xFF37302A)

    /**
     * ⚠️ 注意 [ChromeDark] / [PanelDark] **暗色值也是「白色半透」**（白 11~13%），它们是**覆盖层**，
     * 只能**原样**铺在深壁纸上。（[CardDark] / [CardElevatedDark] 自 v0.2 起已是**实心色**，不受此限。）
     * **绝不要对它们写 `.copy(alpha = X)`** —— `copy(alpha)` 是**替换**不是相乘，
     * 会把「白@13%」换成「白@X%」：X 一大就变成**一块白板**（实测 `surface.copy(alpha=0.94f)`
     * 在暗色下 = 白 94%，状态文字 #F5F6F8 压上去只有 **1.04:1**，完全看不见）。
     * 需要「实心表面」请用 `GlassAccent.glassSolid()`（亮色侧纯白 / 暗色侧走 [SolidDark]）。
     */
    /**
     * 「实心玻璃」暗色基色 —— 供 `GlassAccent.glassSolid()` 使用。
     * ★v0.2 起改为**暖调 `#2E2820`**（原近黑 `#151518`）：照「暗色表面梯子 = 亮色侧镜像」，
     * **浮层必须比内容卡（`#282219`）更亮**；近黑会让常驻操作条 / sheet 像在页面里挖了个洞。
     */
    val SolidDark = Color(0xFF37302A)

    // ── 主操作：液态渐变（橙→珊瑚粉 120°）───────────────────────────
    val GradientStart = Color(0xFFFF8A3C)   // 渐变起：暖橙
    val GradientEnd = Color(0xFFFF5F6D)     // 渐变止：珊瑚粉
    /** 单色打底（primary 角色）：渐变装饰层叠加其上的感知基色。 */
    val OrangePrimaryLight = Color(0xFFFF6A2E).copy(alpha = 0.88f)
    val OrangePrimaryDark = Color(0xFFFF6A2E).copy(alpha = 0.45f)
    /** 亮/暗禁用主操作（灰化，渐变装饰层在 disabled 时隐藏）。 */
    val DisabledPrimaryLight = Color(0xFFCFC7D6).copy(alpha = 0.7f)
    val DisabledPrimaryDark = Color(0xFF8A8A95).copy(alpha = 0.35f)

    /**
     * 「橙浅底」（芯片 / 头像底 / 来源条 / 推荐标）—— 对应 mockup `:root` 的
     * `--fill:rgba(255,106,46,.16)`（暗色 `.22`）。
     * ★注意：这是**打底色**，只作背景用；当文字/图标色用会读不出来（见 GlassAccent）。
     */
    val FillLight = Color(0xFFFF6A2E).copy(alpha = 0.16f)
    val FillDark = Color(0xFFFF6A2E).copy(alpha = 0.22f)

    // ── 文字（定稿）─────────────────────────────────────────────────
    val TextPrimaryLight = Color(0xFF241F18)
    val TextSecondaryLight = Color(0x8C241F18) // 55% alpha
    val TextPrimaryDark = Color(0xFFF5F6F8)
    val TextSecondaryDark = Color(0x8CF5F6F8)
    /** 三级文字（`--text3`）：亮 rgba(36,31,24,.38) / 暗 rgba(245,246,248,.36)。用于未选中态控件描边。 */
    val TextTertiaryLight = Color(0x61241F18)
    val TextTertiaryDark = Color(0x5CF5F6F8)

    // ── 平台品牌色（来源标识；亮/暗两态）──────────────────────────────
    /**
     * 三平台来源色。暗色侧取高亮变体 —— 尤其抖音：品牌青 `#25F4EE` 在白底上不可见，
     * 故亮色用近黑、暗色用近白（见 `PlatformBrand.platformBrandColor()`，全 App 唯一入口）。
     */
    val KuaishouBrandLight = Color(0xFFFE5000)
    val KuaishouBrandDark = Color(0xFFFF7A3D)
    val DouyinBrandLight = Color(0xFF26292F)
    val DouyinBrandDark = Color(0xFFE8EAEF)
    val XhsBrandLight = Color(0xFFFE2C55)
    val XhsBrandDark = Color(0xFFFF6B81)

    // ── 完成态（高饱和翠绿）─────────────────────────────────────────
    val SuccessGreen = Color(0xFF00895C)     // 亮：翠绿（v2 #0E8A56 加深饱和）
    val SuccessGreenDark = Color(0xFF7AE0B8) // 暗：薄荷绿高亮
    // ── 失败态（高饱和珊瑚红）────────────────────────────────────────
    val FailRed = Color(0xFFE23145)          // 亮
    val FailRedDark = Color(0xFFFF9AA6)      // 暗
    // ── 等待态（高饱和琥珀）──────────────────────────────────────────
    val WaitAmber = Color(0xFFC26A00)        // 亮
    val WaitAmberDark = Color(0xFFFFC77D)    // 暗
    // ── 排队态（中性灰）──────────────────────────────────────────────
    val QueueGray = Color(0xFF9AA0A8)        // 亮暗通用
    // ── 下载进行中（渐变橙文字级，亮/暗）──────────────────────────────
    val DownloadingLight = Color(0xFFE2560E)
    val DownloadingDark = Color(0xFFFFB37A)

    // ── 玻璃内元素文字（状态胶囊等彩字浮于玻璃卡上）───────────────────
    val OrangeTextDeep = Color(0xFFE2560E)   // 文字级橙（亮）
    val OrangeOnLight = Color.White
    val OrangeOnDark = Color(0xFFFFD9BE)     // 暗色浅橙字（渐变上白/暖）
}
