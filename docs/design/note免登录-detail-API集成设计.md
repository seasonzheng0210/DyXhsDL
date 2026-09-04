# 抖音 Note（图文/图集）免登录抓取——App 集成设计文档

> 版本：v1.0（实施完成）
> 日期：2026-09-04
> 状态：**✅ P0-P3 全部落地并实证，已发 v1.12.0**（E2E 五项验收全绿，见 §6.2 实测结果；P4 真机待验）
> 关联版本：1.12.0（versionCode 84，feature 级，语义规则）

**决策记录（2026-09-04 用户拍板）**：D1=B（Kotlin 移植纯算法）；D2=note 页预热；D3=懒预热；D4=msToken 空串；D5=不做登录 UI。

## 1. 背景与问题

### 1.1 现状缺陷

| 路径 | 现状 | 失败原因（已实证） |
|---|---|---|
| video 直解 | 移动端 `aweme/v1/feed`（免 Argus、无需 a_bogus）→ ✅ 模拟器实测下载成功 | — |
| **note 图文** HTTP 直解 | 抓 `www.douyin.com/note/{id}` HTML → 风控壳 | curl/OkHttp 无 JS → 72KB `_$jsvmprt` 挑战壳，无数据 |
| note 图文 WebView 兜底 | BgWebViewParser 渲染 note 页 + douyin_extractor.js 抠 pace 数据 | 匿名会话：渲染层登录墙；extractor 本身沙箱实测正常（非代码 bug） |

**结论**：note 抓取缺口 = 缺一个"不依赖页面渲染"的数据源。沙箱实证（2026-09-04）已锁定正解 = **官方 Web detail API + 完整浏览器指纹 cookie + a_bogus 签名**。

### 1.2 实证结论（决定本设计的事实基础）

```
纯 httpx + 游客 ttwid + a_bogus                  → 403 ArgusSecurityPlugin Uifid Not Found
Playwright(执行JS) 产 37 cookies → httpx + a_bogus → HTTP 200 / status_code 0 / images[0] 无水印直链
```

- 数据中心 IP 匿名抓 note **可行**；门槛 = **两件套**：完整浏览器指纹 cookie（ttwid/UIFID_TEMP/__ac_signature/s_v_web_id…）+ a_bogus 签名。
- UIFID_TEMP 等指纹 cookie **只能由真实浏览器 JS 执行后种下**（纯 HTTP 拿不到）→ App 内必须借助 WebView 预热。
- detail 端点：`https://www.douyin.com/aweme/v1/web/aweme/detail/`
- 图集数据：`aweme_detail.images[].url_list[0]`（无水印 `tplv-dy-aweme-images` 直链，无需二次去水印）
- 标题：`aweme_detail.desc`；图文帖 `aweme_type` 68（含 66/63 等变体）
- a_bogus 生成可参考 **Evil0ctal abogus.py（635 行，纯算法无 JS 引擎）**，签名与请求 User-Agent 强绑定（Chrome/90）

## 2. 目标与非目标

### 目标
1. note 图集免登录可下载（数据中心 IP 环境即可，不再依赖登录态/住宅 IP）。
2. 复用现有组件：BgWebViewParser 的 WebView（cookie 预热）、CookieManager（持久化）、OkHttp HTTP 层。
3. video 直解路径**不受影响**（保持移动 feed 免签名现状）。
4. 失败时优雅降级到现有路径，绝不比现状更差。

### 非目标
- 不做登录态方案（登录引导 UI 属于另一条产品线，本方案纯匿名）。
- 不替换 video 路径、不动快手/小红书。
- 不承诺算法永久有效——抖音升级后 a_bogus 可能失效，需保留降级与跟进机制。

## 3. 方案总览

```
[用户粘贴 note 链接]
      │
      ▼
DouyinParser.parse()  ←──────────────┐
  ① mobile feed 快解（video，免签名）   │ (保留现状)
  ② Web detail API 直解（NEW）───────┼──► 失败(403 Uifid/超时) ──┐
      │  cookie + a_bogus             │                            │
  ③ note HTML 抠图（兜底，现状）       │                            ▼
      └──────────────────────────────┘                [Cookie 预热器]
                                            BgWebViewParser 复用 WebView
                                            加载 douyin.com/note/{id}
                                            等待 JS 种 cookie (≤6s)
                                            → CookieManager 快照
                                            → 重试 ②
```

**时序（解析一次 note，正常路径）**：
1. `DouyinParser.parse()` 先试 ① 移动 feed（video 一次命中，行为不变）。
2. note 类 → 尝试 ② `fetchWebDetail()`：若本地已有有效 cookie（CookieManager 里有 UIFID_TEMP），直接带 cookie + a_bogus 请求 detail。
3. ② 失败且错误为风控（403/Argus/缺 UIFID）→ 进入 **Cookie 预热**：通过 BgWebViewParser 的 WebView 加载目标 note 页，等 JS 执行种 cookie（实测 ~5s 内），期间 CookieManager 自动写入 → 快照后重试 ②（至多 2 次）。
4. ② 最终失败 → ③ note HTML 提取（现状兜底，提取器正常但受匿名限制，作最后尝试）。

**架构原则**：② 是新主路径；预热器只在 cookie 缺失/失效时介入，成功后后续任务直接复用（CookieManager 全局持久化，同 WebViewActivity/BgWebViewParser 共享 app_webview 目录）。

## 4. 关键组件设计

### 4.1 `DouyinAbogus`（新，a_bogus 生成器）

**决策点 D1（§7）**，两条候选路线：

| 路线 | 实现 | 优点 | 代价/风险 |
|---|---|---|---|
| **B. Kotlin 移植纯算法**（推荐） | 参照 Evil0ctal `abogus.py`(635 行) 移植：魔改 SM3 双重哈希 + 魔改 RC4 + 变种 Base64 + 字节重排；SM3 用 BouncyCastle `SM3Digest` 或手写(~150行) | 零 JS 引擎、微秒级、无运行时兼容风险；沙箱 Python 实证可作**逐字节对照基线** | 移植量 400–700 行 Kotlin + 单测；需把 635 行吃透（一次性投入） |
| A. Rhino 跑改造 JS | 拿 assets `abogus.js`(4187行, ylcangel 版) 去除 env 依赖（硬编码 navigator/screen/UA 特征），`org.mozilla:rhino` 执行 `__getABogus(url,method)` | 复用现有 JS，改动小 | Rhino 对 4187 行混淆 JS(vm_decode+ES6+) 兼容/性能**未验证**需 spike；~1MB 依赖；仍是 JS 引擎运行时 |

**推荐 B**，理由：正确性可对照实证脚本逐字节验证（沙箱 Python 已跑通），无引擎黑盒。若评审选 A，先做 spike（Node 跑通改造版 → 再 Rhino）。

接口（路线 B）：
```kotlin
object DouyinAbogus {
    const val SIGN_UA = "Mozilla/5.0 ... Chrome/90.0.4430.212 Safari/537.36" // 与算法绑定，勿改
    fun sign(params: Map<String, String>): String  // 返回 a_bogus 值（实证脚本可比对）
}
```

### 4.2 `DouyinCookieWarmer`（新，cookie 预热/快照）

- 复用 BgWebViewParser 的**同一 WebView 单例**（`getOrCreateWebView` 已存在，串行 Mutex 复用）——避免开第二个重型 WebView。
- 预热 = 后台加载 `https://www.douyin.com/note/{id}`（与 BgWebViewParser 解析同款桌面 UA），等待 `onPageFinished` + 5s（JS 种 cookie 窗口，实测足够），**不执行任何提取**，靠 CookieManager 自动写入。
- 快照 = 主线程读 `CookieManager.getInstance().getCookie("https://www.douyin.com/")` → 返回完整 cookie 串供 OkHttp 使用。
- 有效性判断：快照含 `UIFID_TEMP`（或 `__ac_signature`）即视为可用；否则返回失败触发再次预热。
- 挂接点：预热调用方为 `DouyinParser`（IO 线程），实际 WebView 操作经 `mainHandler.post`（BgWebViewParser 同款模式）；需新增一个 suspend 入口（如 `BgWebViewParser.warmupAndSnapshot(url): String?`），沿用现有 Mutex 串行，避免与解析任务互抢。

### 4.3 `DouyinParser.fetchWebDetail()`（新，②主路径）

```kotlin
// 参数模板（对齐 Evil0ctal BaseRequestModel，全量 28 项）
params = {
  device_platform=webapp, aid=6383, channel=channel_pc_web,
  pc_client_type=1, version_code=290100, version_name=29.1.0,
  cookie_enabled=true, screen_width=1920, screen_height=1080,
  browser_language=zh-CN, browser_platform=Win32,
  browser_name=Chrome, browser_version=130.0.0.0,
  browser_online=true, engine_name=Blink, engine_version=130.0.0.0,
  os_name=Windows, os_version=10, cpu_core_num=12, device_memory=8,
  platform=PC, downlink=10, effective_type=4g, round_trip_time=0,
  aweme_id=<id>, msToken=""   // msToken 空串（实证可行）
}
endpoint = https://www.douyin.com/aweme/v1/web/aweme/detail/?${urlencode(params)}&a_bogus=${DouyinAbogus.sign(params)}
headers = { User-Agent: DouyinAbogus.SIGN_UA, Referer: https://www.douyin.com/,
            Accept-Language: zh-CN,zh;q=0.8, Cookie: <快照> }
```

解析：
- `status_code == 0` 且 `aweme_detail != null` → 成功。
- `images[]` 非空 → 图集：取每张 `url_list[0]`（无水印变体已由 CDN 直给，无需 playwm→play 处理），连同 `desc` 组 `DouyinMediaInfo(IMAGE)`。
- `images[]` 空但 `aweme_detail.video.play_addr` 存在 → 视频帖（罕见但兜住）：取 play_addr 直链。
- `status_code != 0` / HTTP 403 / body 含 `ArgusSecurityPlugin` → 抛特定异常触发预热重试。

### 4.4 现有代码改动清单

| 文件 | 改动 |
|---|---|
| `douyin/DouyinAbogus.kt`（新） | a_bogus 生成（路线 B 移植 or A Rhino） |
| `web/BgWebViewParser.kt` | + `warmupAndSnapshot(url): String?` suspend 入口（复用 WebView/串行）；不破坏现有 parse |
| `douyin/DouyinParser.kt` | `parse()` 插入 ② 分支：note 类优先 `fetchWebDetail()`；失败按 §4.3 异常类型决定是否预热重试；video 保持 ① 不动 |
| `DownloadService.kt` | 无改动（解析失败语义不变，任务状态管理不变）——确认无异常类型需要透传即可 |
| `app/build.gradle` | 路线 A：+`org.mozilla:rhino`；路线 B：+BouncyCastle(SM3)（Android 自带 BC 精简版可能够，需验证） |
| `app/src/test/…/DouyinAbogusTest.kt`（新） | JVM 单测：固定 params 输入 vs 沙箱 Python 输出逐字节比对 |
| 版本 | 1.11.0 → **1.12.0**（versionCode 83→84） |

## 5. 降级与失败语义

- ② detail 失败（网络/签名失效/风控新变种）→ 记日志 → ③ HTML 兜底照旧 → 再失败则与 v1.11.0 相同报"解析失败"，任务 FAILED 可重试。**用户可见行为不劣化**。
- Cookie 预热失败（WebView 崩/超时）→ 直接降级 ③，不阻塞。
- a_bogus 算法失效信号：detail 返回非 Argus 错误（如签名不合法 status_msg）且预热也无效 → 日志标记 `A_BOGUS_OBSOLETE`，便于后续跟进社区更新（Evil0ctal 主分支同步）。

## 6. 测试与验收标准

### 6.1 JVM 单测（先于一切）
- `DouyinAbogusTest`：对沙箱实证用的同一组 params，Kotlin 输出 a_bogus 与 Python 输出**完全一致**（字节级）。这是移植正确性的金标准。

### 6.2 模拟器 E2E（回归 + 新功能）
1. note `7674999258294252665` → 图集下载成功（MediaStore 出现文件）。
2. 同一 note 第二次触发 → 复用 task（查重回归）。
3. video 链接 → 仍走 mobile feed 直解成功（不受影响回归）。
4. 断网/伪造签名 → 优雅降级 HTML 路径，任务 FAILED 可重试（无卡死）。
5. 预热器与解析任务串行不互抢（连续 2 条 note 连发）。

### 6.3 真机验收（最终）
- 住宅 IP + 无登录：note 图集下载成功；录屏/日志存档。

## 7. 待拍板决策点

| # | 问题 | 候选 | 我的推荐 |
|---|---|---|---|
| **D1** | a_bogus 生成路线 | A=Rhino 跑改造 abogus.js；**B=Kotlin 移植纯算法**（对照 Python 版） | **B**：正确性可逐字节验证、无 JS 引擎黑盒；一次投入长期干净 |
| **D2** | 预热加载目标 | 仅首页 `douyin.com` vs 直接 note 页 | note 页（实证走 note 页即种齐 37 cookies，且顺带页面数据可留作 ③ 兜底缓存） |
| **D3** | 预热触发时机 | 懒预热（detail 403 才触发）vs 启动预预热 | 懒预热（省流量；首个 note 解析多 ~6s 可接受，预热成功后 CookieManager 全局复用后续零成本） |
| **D4** | msToken | 空串（实证可行）vs 随机 107 位 | 空串（对齐官方 crawler 现做法，实证 status_code=0） |
| **D5** | 本期是否做登录引导 UI | 否 vs 是 | 否（匿名方案成功后登录需求大减，另行评估） |

## 8. 分阶段实施计划

| 阶段 | 内容 | 验收 |
|---|---|---|
| **P0 spike（半天）** | Kotlin 移植核心算法骨架（SM3+RC4+编码表），跑通 `sign()` 与 Python 对照 | JVM 单测全绿 |
| **P1（1 天）** | `DouyinAbogus` 完整移植 + 单测；`DouyinParser.fetchWebDetail` 接入 | 沙箱逻辑同款参数在 JVM 出与 Python 一致签名；代码走查 |
| **P2（1 天）** | `BgWebViewParser.warmupAndSnapshot` + parse() 编排 + 降级 | 编译 + 模拟器冒烟（note 触发不崩） |
| **P3（1 天）** | 模拟器 E2E 全验收（§6.2）+ 版本 1.12.0 + 归档/推送/记忆 | E2E 5 项全过，APK 归档 |
| **P4（真机）** | 住宅 IP 匿名验收（§6.3） | 录屏 + DownloadLogger 日志 |

## 9. 参考

- 实证：`C:\tmp\dy_probe\probe_detail.py`（可复跑，cookie 需重新 Playwright dump）
- Evil0ctal/Douyin_TikTok_Download_API：`crawlers/douyin/web/abogus.py`（纯 Python a_bogus）、`web_crawler.py` fetch_one_video、`models.py` BaseRequestModel
- 本项目 assets：`abogus.js`（ylcangel/douyin_sign，4187 行 JS 版，env 依赖重）
- 项目记忆：`.workbuddy/memory/MEMORY.md` §沙箱出网/抖音门控（2026-09-04 突破结论）
