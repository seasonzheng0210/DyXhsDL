# DyXhsDL v1.0.37 模拟器端到端验证报告（2026-08-15）

## 验证方法
- 卸载模拟器残留旧包（v1.0.33，不同签名）→ 重装 `apks/DyXhsDL-v1.0.37-release.apk`（versionCode 59）。
- `am start -S --activity-clear-task` 冷启 MainActivity 触发真实 `auto_download_url` 链路（绕过 singleTop 复用问题），等待 35s 抓取 logcat + App 内部下载日志。

## 一、已确认修复（客观证据）
| 项目 | 证据 |
|---|---|
| 旧致命报错已消失 | logcat 中**不再出现** `End of input at character 0 of`（抖音 HTTP 直解）与 `无法解析快手作品`（快手 HTTP 直解）——这正是用户 8/14 报的错，来自已死的 HTTP 解析器 |
| 抖音不再被拒播 | WebView 成功加载**真实桌面页** `https://www.douyin.com/video/{id}`，而非 `iesdouyin.com/share/video` 的「请在抖音极速版内观看」拒绝页 |
| JS 注入钩子生效 | 日志 `=== dy inject ready ===`（web_inject.js 在 onPageFinished 重注成功） |
| 提取器运行正常 | 日志 `=== Douyin Extractor === urls=0 title="王濛"`（已解析到作者名） |
| 嗅探器运行正常 | 快手用例日志 `Sniffed video URL: https://www.kuaishou.com/short-video/...` |
| 路由修复生效 | 三处自动入口（剪贴板/下载按钮/重试）均改走 WebView，不再直连死 HTTP 解析器 |

## 二、仍未在模拟器拿到 MP4（平台门控，非代码回归）
- **抖音**：提取器多次运行，`urls=0`（拿到作者名「王濛」但**播放地址为 0 条**）。抖音对**未登录 + 数据中心 IP** 的模拟器 WebView 门控了真实播放地址（play_addr XHR 未放行）。
- **快手**：日志中**完全没有** `apollo/photoUrl/playAddr/.mp4/kwaicdn` 数据 → 未登录 WebView 下页面未放出视频数据（登录墙）。嗅探只抓到页面 URL + favicon。
- **对照**：此前 Playwright 真 Chrome（正常住宅 IP）从 `www.douyin.com/video/{id}` 抓到过真实 mp4（`v26-web.douyinvod.com/...`），证明机制本身能工作，卡点纯粹是风控/登录门控。

## 三、结论与用户下一步
1. v1.0.37 **已经消除了用户原始报错的根因**（死 HTTP 解析器 + 移动拒播页）。用户真机若仍看到 `End of input`/`无法解析快手作品`，说明没装上 v1.0.37（请「更多→关于」核对版本号 1.0.37 / versionCode 59）。
2. 模拟器（数据中心 IP、零登录 cookie）是**最差情况**，抖音/快手几乎必然门控播放地址。真实手机（正常 IP、可能已登录）才有机会成功提取。
3. **请用户在真机安装 v1.0.37 实测其真实链接**。若仍失败，从「更多→失败日志」或「打包日志」取带 `[v1.0.37]` 的日志发我——现在失败日志已带 `captured/sniffed` 计数与「WebView 提取超时」诊断，能直接定位是门控还是别的问题。
4. 左滑删除按钮已按代码修复（手势排序修正），构建通过、MainActivity 运行无崩溃，但需真机视觉确认可触发。

## 四、可选增强（如需要）
- 提取 0 条时引导用户在 WebView 内登录抖音/快手后重试（cookie 持久化到应用 WebView）。
- 对明确返回登录墙的页面，直接提示「该视频需登录后下载」而非静默超时。

## 原始日志物证
- `scripts/selftest/logs/v1037_douyin.log`（抖音用例：桌面页加载 + inject ready + extractor urls=0）
- `scripts/selftest/logs/v1037_kuaishou.log`（快手用例：嗅探触发 + 无视频数据）
- `scripts/selftest/logs/real2_douyin.log` / `real2_kuaishou.log`（更早一轮测试）

---

# DyXhsDL v1.0.38 更新：提取失败 → App 内登录引导（2026-08-15 已实现）

> 针对「可选增强」中「提取 0 条时引导用户在 WebView 内登录抖音/快手后重试」的落地实现。
> commit `bd9311e`，本地 APK `apks/DyXhsDL-v1.0.38-release.apk`（versionCode 60）。

## 改动要点（app/src/main/java/com/neoruaa/xhsdn/WebViewActivity.kt）
1. **Cookie 开启 + 持久化**：`CookieManager.setAcceptCookie(true)` + `setAcceptThirdPartyCookies`；退回 / 重载视频页时 `CookieManager.flush()` 落盘 → **登录态跨启动保留**，无需每次登录。
2. **提取超时不再静默结束**：douyin/kuaishou 轮询 150 次（≈2min）仍 0 条时，置 `extractionFailed` 状态并记下 `retryUrl`（失败时的视频页 URL），**保持 Activity 打开**展示引导卡，而非 `onResult(emptyList)` 结束。
3. **「去登录{平台}」按钮**：在 App 内 WebView 打开 `www.douyin.com/` / `www.kuaishou.com/` 登录页，登录态由 CookieManager 自动保存。
4. **「重试提取」按钮**：`retryExtract()` 清空 captured/sniffed、复位状态、重载 `retryUrl`（带上已登录 Cookie）重新跑提取——直接命中「未登录门控」根因。
5. **`onPageStarted` 复位 `extractionFailed`**：新页面加载即收起引导卡，失败再置位。

## 构建 / 冒烟（模拟器，2026-08-15）
- `assembleRelease --no-daemon` **BUILD SUCCESSFUL**（仅既有 deprecation 警告：statusBarColor / allowUniversalAccessFromFileURLs / LinearProgressIndicator 等，非错误）。
- 经 `MainActivity -e auto_download_url` 冷启抖音视频页：日志出现 `=== dy inject ready ===` 与 `=== Douyin Extractor ===`，**无 AndroidRuntime 崩溃**，进程存活。
- 引导卡 UI 与「登录后重试拿 mp4」**依赖真实账号 + 正常 IP**，模拟器（数据中心 IP、无账号）无法验证成功下载——需真机验证（见下）。

## 真机验证步骤（用户侧）
1. 安装 `apks/DyXhsDL-v1.0.38-release.apk`，「更多 → 关于」核对 **1.0.38 / versionCode 60**。
2. 粘贴抖音/快手链接 → 自动走 WebView 提取；若 2 分钟内仍 0 条，页面下方出现「去登录抖音/快手」+「重试提取」引导卡。
3. 点「去登录」在 App 内完成登录（登录态自动保存，下次免登）→ 点「重试提取」重新加载视频页并复用 Cookie 重新提取。
4. 若仍失败，从「更多 → 失败日志」取带 `[v1.0.38]` 的日志发我（含 captured/sniffed 计数，可区分门控/其他问题）。

## 日志物证
- `scripts/selftest/logs/smoke2_v1038.log`（v1.0.38 冒烟：dy inject ready + Douyin Extractor，无崩溃）
- `scripts/selftest/logs/build_v1038.log`（构建输出，BUILD SUCCESSFUL）

