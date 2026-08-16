# 验证报告 —— DyXhsDL v1.0.42（抖音/快手「HTTP 直解优先、WebView 兜底」）

## 一、用户诉求
1. 「我不要跳到提取的页面，我要你直接解析出来直接下载」——粘贴链接后不要弹出抖音/快手网页，后台解析完直接下载。
2. 「始终下载不了，你就拿这个链接 https://v.douyin.com/Zv2eFSyDXLc/ 试，最终我要下载下来」——要真把 MP4 下下来。

## 二、关键突破（沙箱实跑逐层定位）
用 Playwright 真 Chrome 跑该链接，逐层排除：

- **死路 1**：抖音 Web 端点 `www.douyin.com/aweme/v1/web/aweme/detail` 自算 `a_bogus` 必返
  `403 Blocked by ArgusSecurityPlugin Sign Invalid`——外部自算签名已被抖音拒签。
- **死路 2（沙箱特有）**：沙箱出网必须经本地代理 `127.0.0.1:7897`（env `HTTPS_PROXY`）。
  Playwright 启动的 Chrome 默认不读该代理 → douyinstatic CDN / API 全部 `status:0` 连接失败。
  加 `--proxy-server=http://127.0.0.1:7897` 后 CDN 全部 200。
- **死路 3**：即便加代理，Chrome 经代理并发几十请求会 `ERR_INSUFFICIENT_RESOURCES` /
  `ERR_EMPTY_RESPONSE`，把关键 detail API 压垮，抓不到 play_addr。
- **活路（终局）**：绕过重型 SPA，**直接打移动端接口**
  `https://aweme.snssdk.com/aweme/v1/feed/?type=7&aweme_id={ID}&iid=0&device_id=0&version_code=27.0.0`
  ——该端点**不触发 Argus、无需 a_bogus**，curl 走代理直接返回 659KB 完整 JSON，
  其中 `aweme_list[0].video.play_addr.url_list[0]` 即**无水印**播放直链。

### 实测下载结果（沙箱，2026-08-16）
- 链接：`https://v.douyin.com/Zv2eFSyDXLc/` → 解析 aweme_id `7673176284709230186`
- 移动端接口返回 `play_addr`（无水印）：`https://v5-default.365yg.com/.../video/tos/cn/...`
- 下载：`curl -L -A "iPhone..." -e "https://www.douyin.com/"` 成功
- 落盘：`scripts/selftest/downloads/douyin_7673176284709230186.mp4`
  - 大小 **1,920,439 字节（≈1.83 MB）**
  - `ffprobe`：format=mov/mp4，**时长 8.83s**，ftyp=`isom`（合法 MP4）
  - 无水印（`play_addr` 而非带水印的 `download_addr`）
  - 内容：视频《还是略显微胖的 #ootd女生穿搭》

**结论：用户给的链接已在沙箱真实下载成功，诉求 2 达成。**

## 三、App 改动（v1.0.42，让真机也走这条活路）
1. **DouyinParser.kt**
   - 新增**第一候选**移动端 `aweme/v1/feed` 端点 + `parseFromMobileFeed()`。
   - 复用既有 `extractVideoUrl` / `extractDouyinImages`，取 `play_addr`（无水印）。
   - 候选顺序：feed → iteminfo → share/video → share/note。
2. **DownloadService.kt**
   - 抖音视频下载 Referer 由 `""` 改为 `DouyinParser.REFERER`（365yg / api-play CDN 不带会 403）。
3. **MainActivity.kt**
   - `launchDouyinWebView` / `launchKuaishouWebView` 改为 **HTTP 直解优先**：
     `lifecycleScope` 内先 `DouyinParser/KuaishouParser.parse()`，成功即 `DownloadService.startDownload`
     （**不跳 WebView 页，直接下**）；失败才回退原 WebView（`startXxxWebViewFallback`）。
   - 覆盖所有抖音/快手入口（剪贴板自动读 / 手动输入 / 任务列表重试）。
4. 版本号 → **v1.0.42（versionCode 64）**。

## 四、构建验证
- `.\gradlew.bat clean assembleRelease --no-daemon` → **BUILD SUCCESSFUL**（8m34s）。
- APK 归档：`apks/DyXhsDL-v1.0.42-release.apk`（apks/ 被 gitignore，仅本地归档供安装）。

## 五、真机实测指引
装 `apks/DyXhsDL-v1.0.42-release.apk`，核对「关于」显示 1.0.42 / 64：
- 粘贴抖音/快手链接 → App **先 HTTP 直解**（移动端接口，无水印）→ 成功则**直接下载、不跳页**；
- 仅当接口被风控（极少见）才回退到 WebView 真浏览器解析（v1.0.38 登录引导仍可用）。
- 此路径不依赖 a_bogus / Argus，住宅 IP 真机成功率应大幅高于此前纯 WebView 方案。
- 失败请提供带 `[v1.0.42]` 的失败日志（「更多 → 失败日志」），分段贴防 400。

## 六、附：沙箱复现脚本
- `scripts/selftest/download_douyin.mjs`：Playwright + 代理 + 请求拦截，抓 play_addr 直链并下载。
- `scripts/selftest/diag.mjs`：诊断 chunk 加载 / 捕获情况用。
- 关键：Playwright 必须显式 `--proxy-server=http://127.0.0.1:7897`（沙箱出网代理）。
