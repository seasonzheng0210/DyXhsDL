<!-- ready-for-agent: true -->
<!-- spec-status: draft (人审后可接手实现/测试) -->
<!-- feature: taobao-download-flow -->
<!-- owner: DyXhsDL / com.neoruaa.douyin-xhs -->

# 规格：淘宝主图 / 视频下载流程（Taobao Download Flow）

> 本规格钉死"淘宝链接从进入到下载"的完整行为与边界，作为 SDD 定义控制 + TDD 验收控制的单一真相源。
> 任何改动（新增入口、改路由、改登录提示）必须先让本规格的对应边界仍成立，由 `app/src/test` 单测守护。

## Problem Statement（问题陈述）

用户在 DyXhsDL 里粘贴 / 复制淘宝商品链接（含 `e.tb.cn` 短链、`item.taobao.com/item.htm?id=` 长链、`detail.tmall.com` 天猫链），期望一键下载主图（轮播全部图 + 详情长图）和主视频。但淘宝对**匿名访问返回登录墙**（实测 `item.taobao.com` 仅返回约 5KB 登录页，主图数据不在匿名响应里），且现代淘宝把 `auctionImages` 塞进登录态页面 / 异步 mtop 接口。历史上因此出现两类回归：① 淘宝短链被误判成小红书导致用错提取器（v1.0.17 前）；② HTTP 直解撞登录墙只报错、不引导登录（v1.0.18 前）。用户需要一条"无需手动复制 cookie、登录一次后自动复用、撞墙自动引导登录"的稳妥路径。

## Solution（方案）

淘宝下载走"App 内 WebView 浏览器自动化"思路（与 GitHub 安卓生态 AgentWeb / WebViewTemplate 同构）：

1. **识别**：任意入口拿到链接后，先用平台分类器判定为 `taobao`（含短链），绝不以 `xhs` / `douyin` 错误地提取。
2. **加载即带登录态**：打开 `WebViewActivity(source=taobao)` 前，若设置里已有淘宝 cookie，用 `CookieManager` 注入，绕过匿名登录墙。
3. **按需登录提示**：首次进入且无 cookie，或已配 cookie 但提取失败（失效），显示橙色"淘宝需要登录"横幅；抖音 / 小红书永不提示。
4. **登录后自动保存**：在 WebView 内登录完成（页面含 `cookie2=/unb=/_m_h5_tk` 登录标志）后，自动 `getCookie` 抓回并写入设置；之后 HTTP 直解兜底与下次 WebView 都自动带登录态。
5. **兜底引导**：任何入口的 HTTP 直解若撞登录墙（主图为空），自动打开可登录的淘宝 WebView，而不是只报错。

## User Stories（用户故事）

1. As a 用户, I want 粘贴淘宝短链后自动识别为淘宝并打开淘宝 WebView, so that 不会用小红书提取器白白失败。
2. As a 用户, I want 复制淘宝链接时首页气泡能正确识别淘宝, so that 点气泡直接进淘宝流程。
3. As a 用户, I want 任务列表里淘宝任务的"网页爬取"按钮走淘宝 WebView, so that 不误用小红书提取器。
4. As a 用户, I want 重试之前失败的淘宝任务时自动跳到可登录页面, so that 不用手动重走流程。
5. As a 用户, I want 首次进淘宝且无 cookie 时看到橙色登录提示, so that 知道需要先登录。
6. As a 用户, I want 已配 cookie 但失效时看到"重新登录"提示, so that 知道是登录态过期。
7. As a 用户, I want 抖音 / 小红书链接绝不弹出淘宝登录提示, so that 不被无关提示打扰。
8. As a 用户, I want 在 WebView 内登录淘宝后 cookie 自动保存, so that 以后不用再登录 / 不用手动复制 cookie。
9. As a 用户, I want 设置页能看到"已保存 / 未保存"淘宝登录态并一键清除, so that 能管理登录状态。
10. As a 用户, I want HTTP 直解撞墙时自动打开淘宝登录页, so that 不会卡在报错死胡同。
11. As a 用户, I want 提取成功时登录提示横幅自动消失, so that 知道登录态有效。
12. As a 用户, I want 淘宝标识用橙色与抖音青 / 小红书红区分, so that 一眼知道当前平台。

## Implementation Decisions（实现决策）

- **模块与接口（仅列接缝，不列实现细节）**
  - 平台分类：`UrlUtils.detectPlatform(link) -> "taobao" | "douyin" | "xhs"`，短链 `e.tb.cn` / `h5.tb.cn` 必须返回 `taobao`。
  - 短链解析：`TaobaoParser.resolveItemPageUrl(shortUrl) -> 长链(item.taobao.com/item.htm?id=)`。
  - 主图提取：`TaobaoParser.extractMainImages(html) -> List<String>`（标准 `auctionImages` 正则 + 现代 H5 选择器，见 `assets/taobao_extractor.js`）。
  - WebView 路由：`WebViewActivity(source=taobao)` 加载前 `CookieManager` 注入；`onPageFinished` 跳过短链落地页；「爬取」按钮触发提取。
  - 登录态保存：`WebViewActivity.trySaveTaobaoCookie()` 读取 `CookieManager.getCookie` 并写入 `XHSDownloaderPrefs.taobao_cookie`（仅含登录标志才写）。
  - 撞墙兜底：`DownloadService.startTaobao` 捕获 `TaobaoLoginRequiredException` 后 `FLAG_ACTIVITY_NEW_TASK` 打开 `WebViewActivity(source=taobao)`。
  - 路由收敛（建议接缝）：`MainActivity` 各入口（手动输入 / 剪贴板气泡 / 网页爬取菜单 / 任务列表网页爬取 / 重试）统一经一个纯函数 `Router.route(link, taskSource?) -> Route`，杜绝漏传 `source` 的误判。
- **登录态必走真人登录**：淘宝登录需短信 / 滑块验证，且安卓沙箱无法读取手机淘宝 App 自身 cookie，故"零操作自动获取"不可行；设计目标是"App 内登录一次，之后自动复用"。
- **品牌色约定**：淘宝橙 `#FF5000` / 抖音青 `#25F4EE` / 小红书红 `#FE2C55`，用于平台标识 chip 与登录提示横幅。
- **版本基线**：当前 `versionCode 40 / versionName 1.0.18`，本规格覆盖 v1.0.14~v1.0.18 已落地的淘宝相关行为。

## Testing Decisions（测试决策）

- **好测试**：只测公开接口行为（返回什么平台 / 解析出什么长链 / 主图正则命中哪些样本 / 路由到哪个入口），不测私有方法、不 mock 自有协作者、不耦合实现。
- **被测模块与接缝**：

| 边界（来自 User Story / Solution） | 接缝（公开接口） | 测试类型 | 先验 |
|---|---|---|---|
| 短链 `e.tb.cn` 识别为 taobao（US1/2） | `UrlUtils.detectPlatform` | 表驱动单测 | 无（新增） |
| `item.taobao.com` / `detail.tmall.com` 识别为 taobao | `UrlUtils.detectPlatform` | 表驱动单测 | 无 |
| 抖音 / 小红书链接不误判为 taobao | `UrlUtils.detectPlatform` | 表驱动单测 | 无 |
| 短链 → 长链 id 解析（US1） | `TaobaoParser.resolveItemPageUrl` | 单测（网络部分按系统边界 mock） | 无 |
| 标准 `auctionImages` 命中、现代 `Array(5)` 不命中 | `extractMainImages` | 单测（复用 Node 自测样本） | 已有 Node 自测 |
| 淘宝任务路由到淘宝 WebView（US3/4） | `Router.route`（抽出的纯函数） | 单测 | 需先抽 Router 接缝 |
| 撞墙抛 `TaobaoLoginRequiredException`（US10） | `TaobaoParser.parse` / `DownloadService.startTaobao` | 单测（mock HTTP 返回登录墙） | 无 |
| 抖音 / 小红书不弹淘宝提示（US7） | `WebViewActivity` 提示决策 | 单测（抽纯函数 `shouldPromptLogin(source, hasCookie)`） | 需先抽接缝 |

- **测试底座**：`app/build.gradle` 已声明 `testImplementation 'junit:junit:4.13.2'`（JVM）。测试写在 `app/src/test/java/...`，**仅 JVM、不依赖 instrumented（androidTest）/ 模拟器**，绕开本机 Gradle daemon 偶发崩与构建慢的问题。
- **红色→绿色纪律**：每个边界先写失败单测（红），再写最小实现（绿），一次一个垂直切片；不批量写测试（避免水平切片反模式）。

## Out of Scope（范围之外）

- 逆向 `x-mini-wua` / `x-sign` 签名以匿名调用 mtop 接口（工程量与法律风险，且不稳）。
- 淘宝官方开放平台 `taobao.item.get`（需企业资质 + `session_key`）。
- 读取手机淘宝 App 自身私有 cookie / 跨 App 数据（系统隔离，不可行）。
- 小红书 / 抖音流程的规格（本规格仅覆盖淘宝）。
- WebView 内真实 DOM 抠图选择器的细节微调（属 `taobao_extractor.js` 迭代，由真机反馈驱动，不纳入本规格的单元验收）。

## Further Notes（补充说明）

- 本规格是**特征规格**：描述的是当前已落地且期望保持的行为。改老逻辑前，先用 `app/src/test` 把对应边界锁成"特征测试"（含疑似边界情形），再小步改、每步跑测试；重构与修 bug 必须分 PR。
- 与 `to-tickets` 配合：本规格可按上表"边界"拆成垂直切片工单（接缝抽取 → 单测 → 绿实现），每单一个 PR。
- 真实网络 / 登录墙 / CookieManager 属系统边界，不写单测；只在它们"之上的决策"（分类、路由、是否提示、撞墙兜底）写单测。
