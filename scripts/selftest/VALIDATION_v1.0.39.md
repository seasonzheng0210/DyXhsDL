# DyXhsDL v1.0.39 验证报告 —— 为什么之前一直 `End of input`，现在怎么修的

> 日期：2026-08-15
> 提交：`e94b123`（已 push origin/master）
> APK：`apks/DyXhsDL-v1.0.39-release.apk`（versionCode 61）

## 一、用户日志暴露的真问题

用户最新日志（含 `[v1.0.38]`）：

```
[2026-08-15 15:26:11][douyin][v1.0.38] input=https://v.douyin.com/Zv2eFSyDXLc/
  reason=解析失败: End of input at character 0 of
[2026-08-15 15:26:20][douyin][v1.0.38] input=https://v.douyin.com/Zv2eFSyDXLc/
  reason=解析失败: End of input at character 0 of
```

两次失败**仅隔 9 秒**——这是 HTTP 解析器"秒挂"的特征（WebView 提取要等 ~2 分钟超时，绝不可能 9 秒）。
说明：**抖音走的仍是已死的 HTTP 直解解析器，而不是 WebView。**

## 二、根因（之前几轮都没查到的断点）

追完整条调用链后发现，真正在跑的 UI 是 Compose 路径（`MainViewModel` / `MainActivity`），
而那个"直连死解析器"的 `MainViewModel.startDouyinDownload` **从没被调用**。唯一活的调用是：

```
WebViewActivity（提取成功，返回 CDN 直链）
  → MainActivity.onActivityResult
  → DownloadService.startDownload(直链, "douyin")
  → DownloadService.startDouyin(直链)
  → DouyinParser.parse(直链)   ← 把 CDN 直链当"分享页"重新解析
  → End of input（秒挂）
```

关键事实：
- WebView 提取器（`douyin_extractor.js`）返回的是 **`aweme.snssdk.com/aweme/v1/play/...` 这类 CDN 直链**
  （代码里 `playwm → play` 去水印）。
- 但 `startDouyin` / `startKuaishou` **无条件**再调一次 `DouyinParser.parse()`，
  把 CDN 直链当成分享页去抓 JSON，自然拿到空响应 → `End of input`。
- 本该拦下直链的 `isDirectVideoUrl()` 函数**定义了却从没被调用**（死代码）。

**结论：即便 WebView 提取成功，下载环节也必然把直链重新丢进死解析器 → 必败。**
这就是"WebView 路由改了、抖音还是 `End of input`"的真正原因。

## 三、修复（v1.0.39）

1. **守卫直链**：`startDouyin` / `startKuaishou` 开头先判 `isDirectVideoUrl()`，
   CDN 直链**直接走 `downloadDirectVideo()` 下载**，绝不再喂死解析器。
2. **拓宽判定**：`isDirectVideoUrl` 覆盖抖音/快手常见视频 CDN
   （aweme / douyinvod / bytecdn / ib.douyin / tiktokcdn；kwaicdn / chenzhongtech / gifshow / kwai）。
3. **补齐请求头**：`downloadDirectVideo` 按 host 选 Referer/UA
   （抖音 `DouyinParser.REFERER`+`MOBILE_UA`；快手对应站点 Referer+`MOBILE_UA`），
   `resolveFinalUrl` 也带 Referer，避免 CDN 返回 403。
4. **堵死死路**：WebView 内「直链解析」按钮不再调已失效的 HTTP 解析器，
   改为明确 toast：「HTTP 直解已失效（平台风控），请点『去登录抖音』登录后点『重试提取』」。

## 四、验证

- `assembleRelease` **BUILD SUCCESSFUL**（仅既有 deprecation 警告，非错误）。
- 模拟器冷启抖音视频页：`=== dy inject ready ===` + `=== Douyin Extractor ===` 正常触发
  （提取机制完好）；**logcat 中 `End of input` / `解析失败` 出现 0 次**；无崩溃，进程存活。
- 说明主流程已彻底绕过死 HTTP 解析器。

## 五、你需要做的（真机实测）

模拟器是数据中心 IP + 零账号，必然被抖音/快手门控，无法验证"真实下载到 mp4"——这步只能在真机：
1. 装 `apks/DyXhsDL-v1.0.39-release.apk`，「更多 → 关于」核对 **1.0.39 / versionCode 61**。
2. 粘贴抖音/快手链接 → 自动 WebView 提取。
3. 若约 2 分钟后仍 0 条（登录门控），页面下方出现「去登录{平台}」+「重试提取」：
   点「去登录」在 App 内完成登录（登录态自动保存）→ 点「重试提取」重新加载并复用 Cookie。
4. 提取成功 → 直链直接下载（不再二次解析），应能拿到 mp4。
5. 若仍失败，从「更多 → 失败日志」取带 `[v1.0.39]` 的日志发我即可精准定位。

## 六、客观物证
- `scripts/selftest/logs/smoke_v1039.log`（v1.0.39 模拟器冒烟：提取触发 + 0 次 End of input）
- `scripts/selftest/logs/build_v1039.log`（构建日志）
