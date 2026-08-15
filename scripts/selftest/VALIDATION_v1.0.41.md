# DyXhsDL v1.0.41 验证报告 —— 「直达下载」模式（不跳提取页，直接解析直接下）

> 用户诉求：**不要跳到那个抖音提取网页，要粘贴链接后直接解析、直接下载**。

## 一、做了什么

新增 WebViewActivity 的 `direct`（直达下载）模式，把"可见的抖音/快手/小红书网页"藏起来：

1. **透明主题 `Theme.WebViewDirect`**（`styles.xml`）：`windowIsTranslucent` + 透明背景，背后的 MainActivity 直接透出来。
2. **WebView 仍在后台跑，但不可见**：`direct && !fallback` 时 WebView 设 `View.INVISIBLE`——它仍参与布局、JS 正常执行（a_bogus 读取的 `window`/`document` 尺寸依旧有效），只是不绘制给用户看。
3. **只给用户一个「正在解析视频，请稍候…」浮层**（透明 Box 里居中的小卡片 + 进度条），不渲染地址栏 / 按钮 / 登录引导卡 / 视频网页本身。
4. **成功后直接下载**：`onResult` 仍走 `setResult + finish`，由 MainActivity 的 `onActivityResult` 调 `DownloadService` 下载——因 Activity 透明，用户全程只见 MainActivity，无任何"页面跳转"。
5. **失败才降级为可见页**：提取超时（轮询 150×800ms≈120s 仍 0 条）时自动 `fallback=true`，翻转成原来的完整 UI（可见 WebView + 登录/重试入口），并 toast「自动解析未完成，已为你打开页面手动提取/登录」——保留 v1.0.38 的登录引导兜底。
6. **小红书也走自动提取**：`direct` 模式把 `autoExtract` 条件从「仅 douyin/kuaishou」扩到「direct 或 douyin/kuaishou」，小红书不再需要用户点「爬取」。

## 二、入口全部切到直达模式

`MainActivity` 的所有拉起入口都加 `putExtra("direct", true)`：
- `launchDouyinWebView` / `launchKuaishouWebView` / `launchWebView`（含重试任务、手动输入、剪贴板气泡、手动输入下载等全部调用方）。
- 小红书分支内联的 `webViewIntent`（剪贴板自动识别 xhs 路径）。

## 三、技术现实（重申，避免误解）

- 抖音的 `a_bogus` 反爬签名**必须在浏览器引擎里算**（读 `navigator`/`window`/`document`/`canvas`，检测无头），纯 Java/OkHttp 复刻不了——所以"完全不用 WebView"不可行；但把 WebView 藏到后台、对用户不可见，就实现了你想要的"不跳页、直接下"。
- 你贴的 `iesdouyin.com/share/video/{id}` 是**分享网页（HTML 空壳）**，不是视频文件；"直接从那条链接下载视频"在物理上不成立，必须先把门控后的真实 CDN 直链解析出来再下。v1.0.41 的直达模式干的正是这件事：后台 WebView 加载→算 a_bogus+抓 token→拿直链→直接下载。
- "免登录"成立：游客 `ttwid` 与运行时 `msToken` 由抖音页面在 WebView 自动生成，非登录 cookie（v1.0.40 已落实）。

## 四、构建验证

- `assembleRelease` **BUILD SUCCESSFUL**（clean 后 10m，无错误；仅有既有 deprecation 警告，如 `LinearProgressIndicator(progress)`、`statusBarColor` 等，均不影响功能）。
- 归档：`apks/DyXhsDL-v1.0.41-release.apk`（versionCode 63 / versionName 1.0.41）。
- 首版构建曾因 `dexBuilderRelease` 写 `graph.bin` 报"拒绝访问"（Access Denied）失败——属 Gradle 中间产物文件锁/残留，**`clean` 后重构建通过**，非代码问题。

## 五、真机实测步骤

1. 装 `apks/DyXhsDL-v1.0.41-release.apk`，「更多 → 关于」核对 **1.0.41 / 63**。
2. 粘贴抖音/快手/小红书链接 → 应**只闪一下"解析中"浮层**，随后直接开始下载（通知/进度）。
3. 若长时间（约 2 分钟）仍在"解析中"后弹出"已为你打开页面"——说明自动提取失败，此时会显示平台网页，可点「去登录」登录后「重试提取」（保留 v1.0.38 兜底）。
4. 仍失败请提供「更多 → 失败日志」里带 `[v1.0.41]` 的条目（**分段贴，避免 400**）。

## 六、维护提示

a_bogus 是逆向签名，抖音会定期改版让它失效。若某天真机又开始 `urls=0` 且日志显 a_bogus 异常，需同步 `assets/abogus.js`（上游 `ylcangel/douyin_sign`）。
