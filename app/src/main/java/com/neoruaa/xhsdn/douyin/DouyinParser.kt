package com.neoruaa.xhsdn.douyin

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 抖音媒体类型：视频 或 图集/图文（图片）。
 */
enum class DouyinMediaType { VIDEO, IMAGE }

/**
 * 抖音解析结果。
 *  - VIDEO：videoUrl 不为空，imageUrls 为空；
 *  - IMAGE ：imageUrls 不为空，videoUrl 为 null。
 */
data class DouyinMediaInfo(
    val type: DouyinMediaType,
    val title: String,
    val videoUrl: String?,
    val imageUrls: List<String>,
    val coverUrl: String?,
    val videoId: String,
    val userAgent: String
)

/**
 * 抖音直链解析。逻辑完整照搬可独立工作的 DouyinDL 项目（com.noctiro.douyindl）：
 *  - 使用随机 iPhone UA（Safari/CriOS/EdgiOS/FxiOS），抖音对固定安卓 Chrome UA 更敏感；
 *  - 解析重定向时手动读取 Location 头（不自动跟随），与 DouyinDL 一致；
 *  - 请求分享页时只带 UA、不带 Referer（DouyinDL 实测可用，带 Referer 易被抖音 security 风控拦截）。
 * 流程：解析重定向拿到 id → 请求 iesdouyin 分享页 → 提取 window._ROUTER_DATA
 * 中的 play_addr.url_list（视频，替换 playwm→play 去水印）或 image_post_info.image_list
 * （图集，逐张取 url_list 第一张）。
 *
 * 注意：抖音图集/图文笔记的媒体不在 video 字段，而在 image_post_info 下；旧逻辑只取 video
 * 字段会在图集上抛 "video 字段缺失"。本实现同时支持视频与图集，并在 share/video 拿不到媒体时
 * 回退到 share/note 端点。
 */
object DouyinParser {
    private const val TAG = "DouyinParser"
    const val MOBILE_UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36"
    const val REFERER = "https://www.douyin.com/"

    // 照搬 DouyinDL：随机 iPhone UA（Safari/CriOS/EdgiOS/FxiOS）
    private fun randomUserAgent(): String {
        val osVersions = listOf("15_0", "15_4", "16_0", "16_3", "16_6", "17_0", "17_1", "17_2", "17_3", "17_4", "17_5", "18_0")
        val safariVersions = listOf("604.1", "605.1.15")
        val chromeVersions = listOf("120.0.6099.119", "121.0.6167.178", "122.0.6261.89", "122.0.6261.105", "123.0.6312.58", "124.0.6367.54")
        val edgeVersions = listOf("121.0.2277.107", "122.0.2365.56", "122.0.2365.92", "123.0.2420.65")
        val firefoxVersions = listOf("121.0", "122.0", "123.0", "124.0")
        val os = "iPhone; CPU iPhone OS ${osVersions.random()} like Mac OS X"
        val webkit = "AppleWebKit/605.1.15 (KHTML, like Gecko)"
        return when ((0..3).random()) {
            0 -> "Mozilla/5.0 ($os) $webkit Version/${osVersions.random().replace('_', '.')} Mobile/15E148 Safari/${safariVersions.random()}"
            1 -> "Mozilla/5.0 ($os) $webkit CriOS/${chromeVersions.random()} Mobile/15E148 Safari/${safariVersions.random()}"
            2 -> "Mozilla/5.0 ($os) $webkit EdgiOS/${edgeVersions.random()} Version/17.0 Mobile/15E148 Safari/${safariVersions.random()}"
            else -> "Mozilla/5.0 ($os) $webkit FxiOS/${firefoxVersions.random()} Mobile/15E148 Safari/605.1.15"
        }
    }

    // 自动跟随重定向的客户端（用于最终抓取分享页 HTML）
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    // 照搬 DouyinDL：手动跟随重定向，读取 Location 头（不自动跟随）
    private val noRedirectClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    fun canParse(url: String): Boolean {
        return url.contains("douyin.com") || url.contains("iesdouyin.com")
    }

    /**
     * 从单个视频链接反查作者主页 URL（https://www.douyin.com/user/{sec_uid}）。
     *  - 解析重定向拿 aweme_id → 调移动端 aweme/v1/feed 接口（不触发 Argus、无需 a_bogus）
     *    取 author.sec_uid → 拼主页直链；
     *  - 用于「主页下载」功能：拿到作者主页后由 WebView 爬取全部 /video/{id} 批量下载。
     *  - 失败（风控/无水印/接口异常）返回 null，调用方兜底直接用原视频链接。
     */
    suspend fun resolveAuthorHomepageUrl(videoUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val ua = randomUserAgent()
            val finalUrl = resolveRedirects(videoUrl, ua)
            val id = extractId(finalUrl)
            val feedUrl = "https://aweme.snssdk.com/aweme/v1/feed/?type=7&aweme_id=$id&iid=0&device_id=0&version_code=27.0.0&version_name=27.0.0"
            val request = Request.Builder()
                .url(feedUrl)
                .header("User-Agent", ua)
                .header("Referer", "https://www.iesdouyin.com/")
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body.string()
            }
            val json = JSONObject(body)
            val list = json.optJSONArray("aweme_list") ?: json.optJSONArray("item_list") ?: return@withContext null
            if (list.length() == 0) return@withContext null
            val data = list.getJSONObject(0)
            val author = data.optJSONObject("author") ?: return@withContext null
            val secUid = author.optString("sec_uid").takeIf { it.isNotBlank() } ?: return@withContext null
            return@withContext "https://www.douyin.com/user/$secUid"
        } catch (e: Exception) {
            Log.w(TAG, "resolveAuthorHomepageUrl failed: ${e.message}")
            return@withContext null
        }
    }

    suspend fun parse(url: String): DouyinMediaInfo = withContext(Dispatchers.IO) {
        val ua = randomUserAgent()
        val finalUrl = resolveRedirects(url, ua)
        val id = extractId(finalUrl)
        Log.d(TAG, "finalUrl=$finalUrl id=$id ua=$ua")

        // 候选顺序（从最稳到兜底）：
        //  1) 移动端 aweme/v1/feed 接口（aweme.snssdk.com）——不触发 Argus 风控、无需 a_bogus，
        //     直接返回 play_addr（无水印）结构化 JSON，成功率最高；
        //  2) 官方 iteminfo 接口（返回干净结构化 JSON，图集字段为 images[]）；
        //  3/4) 分享页 _ROUTER_DATA（share/video、share/note）。
        val candidates = listOf(
            "https://aweme.snssdk.com/aweme/v1/feed/?type=7&aweme_id=$id&iid=0&device_id=0&version_code=27.0.0&version_name=27.0.0",
            "https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=$id",
            "https://www.iesdouyin.com/share/video/$id",
            "https://www.iesdouyin.com/share/note/$id"
        )
        var lastErr: String? = null
        for (c in candidates) {
            try {
                val info = when {
                    c.contains("aweme/v1/feed") -> parseFromMobileFeed(c, ua, id)
                    c.contains("iteminfo") -> parseFromItemInfo(c, ua, id)
                    else -> parseFromShare(c, ua, id)
                }
                if (info != null) return@withContext info
            } catch (e: Exception) {
                lastErr = e.message
                Log.w(TAG, "解析失败 candidate=$c err=${e.message}")
            }
        }
        throw Exception(lastErr ?: "抖音解析失败：未在接口或分享页找到视频/图片")
    }

    /**
     * 从抖音长链接中提取作品 id（aweme_id）。
     * 兼容 video/、note/ 路径，以及 discover?modal_id= 形式；
     * 最后回退到「最后一个纯数字路径段」。
     */
    private fun extractId(url: String): String {
        Regex("""(?:video|note)/(\d+)""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("""modal_id=(\d+)""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        val last = url.split("?")[0].trimEnd('/').split("/").last()
        return last
    }

    /**
     * 解析官方 iteminfo 接口返回的 JSON（干净结构化数据，图集字段为 images[]）。
     *  - 拿到媒体返回 [DouyinMediaInfo]；
     *  - item_list 为空 / 无媒体返回 null，让调用方试下一个源。
     */
    private fun parseFromItemInfo(apiUrl: String, ua: String, id: String): DouyinMediaInfo? {
        val request = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", ua)
            .header("Referer", REFERER)
            .build()

        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("iteminfo 接口失败: HTTP ${response.code}")
            response.body.string()
        }

        val json = JSONObject(body)
        val itemList = json.optJSONArray("item_list")
            ?: json.optJSONObject("data")?.optJSONArray("item_list")
            ?: return null
        if (itemList.length() == 0) return null

        val data = itemList.getJSONObject(0)
        // 校验返回项确为目标作品：iteminfo 按 item_ids 查询通常直接匹配；
        // 但偶发错位/空响应时若不校验 id，会把图文帖误判成视频，故显式校验。
        if (data.optString("aweme_id", "") != id) return null
        val safeTitle = safeTitleOf(data.optString("desc", "").trim().ifEmpty { "douyin_$id" }, id)

        // 图集优先（抖音字段 images[]，回退 image_post_info）
        val imageUrls = extractDouyinImages(data)
        if (imageUrls.isNotEmpty()) {
            val coverUrl = data.optJSONObject("video")
                ?.optJSONObject("cover")?.optJSONArray("url_list")?.optString(0)
            return DouyinMediaInfo(DouyinMediaType.IMAGE, safeTitle, null, imageUrls, coverUrl, id, ua)
        }

        // 视频
        val video = data.optJSONObject("video")
        if (video != null) {
            val vUrl = extractVideoUrl(video) ?: return null
            val coverUrl = video.optJSONObject("cover")?.optJSONArray("url_list")?.optString(0)
            return DouyinMediaInfo(DouyinMediaType.VIDEO, safeTitle, vUrl, emptyList(), coverUrl, id, ua)
        }

        return null
    }

    /**
     * 移动端官方 feed 接口（aweme.snssdk.com/aweme/v1/feed/）解析。
     *  - 该接口不触发 Argus 风控、无需 a_bogus，直接返回干净结构化 JSON；
     *  - 返回字段结构与 iteminfo 一致（aweme_list[0] 内含 video.play_addr / images / desc）；
     *  - play_addr 为无水印播放源，download_addr 为带水印下载源，这里优先取 play_addr。
     *  - 成功返回 [DouyinMediaInfo]；列表为空 / 无媒体返回 null 让调用方试下一个源。
     */
    private fun parseFromMobileFeed(apiUrl: String, ua: String, id: String): DouyinMediaInfo? {
        val request = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", ua)
            .header("Referer", "https://www.iesdouyin.com/")
            .build()

        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("mobile feed 接口失败: HTTP ${response.code}")
            response.body.string()
        }

        val json = JSONObject(body)
        val list = json.optJSONArray("aweme_list")
            ?: json.optJSONArray("item_list")
            ?: return null
        if (list.length() == 0) return null

        // 在列表中查找 aweme_id 匹配目标 id 的作品。
        // 关键修复：移动端 feed 接口在匿名请求（App 的 OkHttp 不带 WebView 登录 Cookie）下会忽略
        // aweme_id、返回泛推荐流（第一条是随机视频）。若不校验 id，就会把图文帖误判成「随机视频」下，
        // 这正是「抖音图文帖下成随机视频」的根因。找不到匹配项则返回 null，让调用方试下一个候选源
        // （最终落到 share/note 页面，其含正确图集数据）。匹配到目标帖时也会正确走 images[] → IMAGE。
        val data = run {
            for (i in 0 until list.length()) {
                val it = list.optJSONObject(i) ?: continue
                if (it.optString("aweme_id", "") == id) return@run it
            }
            null
        } ?: return null
        val safeTitle = safeTitleOf(data.optString("desc", "").trim().ifEmpty { "douyin_$id" }, id)

        // 图集优先
        val imageUrls = extractDouyinImages(data)
        if (imageUrls.isNotEmpty()) {
            val coverUrl = data.optJSONObject("image_post_info")
                ?.optJSONObject("first_frame_image")
                ?.optJSONArray("url_list")?.optString(0)
                ?: data.optJSONObject("video")
                    ?.optJSONObject("cover")?.optJSONArray("url_list")?.optString(0)
            return DouyinMediaInfo(DouyinMediaType.IMAGE, safeTitle, null, imageUrls, coverUrl, id, ua)
        }

        // 视频：优先 play_addr（无水印），回退 bit_rate[0].play_addr
        val video = data.optJSONObject("video")
        if (video != null) {
            val vUrl = extractVideoUrl(video) ?: return null
            val coverUrl = video.optJSONObject("cover")?.optJSONArray("url_list")?.optString(0)
            return DouyinMediaInfo(DouyinMediaType.VIDEO, safeTitle, vUrl, emptyList(), coverUrl, id, ua)
        }
        return null
    }

    /**
     * 解析单个分享页 HTML（_ROUTER_DATA），提取视频或图集。
     *  - 拿到媒体返回 [DouyinMediaInfo]；
     *  - 页面存在但本端点不含媒体（如 video 端点不含图集字段）返回 null，让调用方试下一个端点；
     *  - 风控页 / HTTP 失败等硬错误直接抛异常。
     */
    private fun parseFromShare(shareUrl: String, ua: String, id: String): DouyinMediaInfo? {
        val request = Request.Builder()
            .url(shareUrl)
            .header("User-Agent", ua)
            .build()

        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("获取抖音页面失败: HTTP ${response.code}")
            response.body.string()
        }

        val pattern = Regex("""window\._ROUTER_DATA\s*=\s*(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(html) ?: throw Exception(
            "未找到 _ROUTER_DATA（疑似抖音 security 风控页）；shareUrl=$shareUrl；html前300字=${(html.take(300)).replace("\n", " ")}"
        )
        val jsonStr = match.groupValues[1].trim()

        val json = JSONObject(jsonStr)
        val loaderData = json.optJSONObject("loaderData") ?: return null

        val pageKey = loaderData.keys().asSequence().firstOrNull { k ->
            (k.startsWith("video_") || k.startsWith("note_")) && k.endsWith("/page")
        } ?: return null

        val page = loaderData.optJSONObject(pageKey) ?: return null
        val videoInfoRes = page.optJSONObject("videoInfoRes") ?: return null
        val itemList = videoInfoRes.optJSONArray("item_list") ?: return null
        if (itemList.length() == 0) return null

        val data = itemList.getJSONObject(0)

        val desc = data.optString("desc", "").trim().ifEmpty { "douyin_$id" }
        val safeTitle = safeTitleOf(desc, id)

        // 图集 / 图文笔记优先：抖音图集字段为 images[]（官方 iteminfo 同结构），
        // 旧版 / TikTok 风格为 image_post_info.image_list[]。先取图片，取不到才回退 video。
        val imageUrls = extractDouyinImages(data)
        if (imageUrls.isNotEmpty()) {
            val coverUrl = data.optJSONObject("image_post_info")
                ?.optJSONObject("first_frame_image")
                ?.optJSONArray("url_list")?.optString(0)
                ?: data.optJSONObject("video")
                    ?.optJSONObject("cover")?.optJSONArray("url_list")?.optString(0)
            return DouyinMediaInfo(DouyinMediaType.IMAGE, safeTitle, null, imageUrls, coverUrl, id, ua)
        }

        // 视频
        val video = data.optJSONObject("video")
        if (video != null) {
            val vUrl = extractVideoUrl(video) ?: return null
            val coverUrl = video.optJSONObject("cover")?.optJSONArray("url_list")?.optString(0)
            return DouyinMediaInfo(DouyinMediaType.VIDEO, safeTitle, vUrl, emptyList(), coverUrl, id, ua)
        }

        // 本端点既无图集字段也无 video：返回 null，调用方会试下一个源
        return null
    }

    /**
     * 从作品 JSON 中提取图集图片地址。
     *  - 抖音官方字段 images[i].url_list[0]（优先）；
     *  - 旧版 / TikTok 风格 image_post_info.image_list[i] 的
     *    origin_image / display_image / url_list（清晰度优先）；
     *  - 兜底：在 image_post_info 内递归扫描任意 url_list[0]（兼容抖音字段命名/层级变动，
     *    例如某些图文帖把图集藏在更深一层或改用新键名，导致前两步漏抓 → 误判成视频）。
     * 图文帖被误判成视频（下成"随机视频"）多源于此，故这里尽量多抓。
     */
    private fun extractDouyinImages(data: JSONObject): List<String> {
        val result = mutableListOf<String>()

        // 1) 抖音官方 images[] 字段
        val images = data.optJSONArray("images")
        if (images != null) {
            for (i in 0 until images.length()) {
                val it = images.optJSONObject(i) ?: continue
                val u = it.optJSONArray("url_list")?.optString(0)?.takeIf { s -> s.isNotBlank() }
                if (u != null) result.add(u)
            }
        }
        if (result.isNotEmpty()) return result

        // 2) 旧版 / TikTok 风格 image_post_info.image_list[]
        val ipi = data.optJSONObject("image_post_info") ?: return result
        val imageList = ipi.optJSONArray("image_list") ?: return result
        for (i in 0 until imageList.length()) {
            val img = imageList.optJSONObject(i) ?: continue
            val urlList = img.optJSONObject("origin_image")?.optJSONArray("url_list")
                ?: img.optJSONObject("display_image")?.optJSONArray("url_list")
                ?: img.optJSONArray("url_list")
            val u = urlList?.optString(0)?.takeIf { s -> s.isNotBlank() }
            if (u != null) result.add(u)
        }
        if (result.isNotEmpty()) return result

        // 3) 兜底：递归扫描 image_post_info 内任意 url_list[0]（兼容字段命名/层级变动）
        result.addAll(scanImageUrls(ipi))
        return result
    }

    /**
     * 递归扫描 JSONObject/JSONArray，收集所有 url_list[0] 中以 http(s) 开头的地址。
     * 用于图文帖图集字段结构变动时的兜底提取（避免误判为视频）。深度限制防止异常嵌套。
     */
    private fun scanImageUrls(obj: Any?, depth: Int = 0): List<String> {
        val out = mutableListOf<String>()
        if (depth > 6 || obj == null) return out
        when (obj) {
            is JSONObject -> {
                obj.keys().forEach { k ->
                    if (k == "url_list") {
                        val arr = obj.optJSONArray(k)
                        arr?.optString(0)?.takeIf { it.isNotBlank() && it.startsWith("http") }?.let { out.add(it) }
                    } else {
                        out.addAll(scanImageUrls(obj.opt(k), depth + 1))
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until obj.length()) out.addAll(scanImageUrls(obj.opt(i), depth + 1))
            }
        }
        return out
    }

    /** 把作品描述清洗成安全的文件名（保留前 80 字）。 */
    private fun safeTitleOf(desc: String, id: String): String {
        val raw = desc.ifEmpty { "douyin_$id" }
        return raw
            .replace(Regex("""[\\/:*?"<>|#\n\r]"""), "_")
            .replace(Regex("""\.{2,}"""), ".")
            .trim(' ', '.')
            .take(80)
    }

    private fun extractVideoUrl(video: JSONObject): String? {
        var vUrl = video.optJSONObject("play_addr")
            ?.optJSONArray("url_list")
            ?.optString(0)
            ?.takeIf { it.isNotBlank() }
            ?.replace("playwm", "play")

        if (vUrl == null) {
            val bitRate = video.optJSONArray("bit_rate")
            if (bitRate != null && bitRate.length() > 0) {
                vUrl = bitRate.getJSONObject(0)
                    .optJSONObject("play_addr")
                    ?.optJSONArray("url_list")
                    ?.optString(0)
                    ?.replace("playwm", "play")
            }
        }
        return vUrl
    }

    /**
     * 根据图片 URL 推断文件扩展名（去水印图集通常为 jpg/webp/png）。
     */
    fun mediaExtension(url: String): String {
        val path = url.split("?")[0]
        val ext = path.substringAfterLast('.', "")
        return when (ext.lowercase()) {
            "jpg", "jpeg" -> "jpg"
            "png" -> "png"
            "webp" -> "webp"
            "gif" -> "gif"
            "bmp" -> "bmp"
            else -> "jpg"
        }
    }

    /**
     * 照搬 DouyinDL：手动跟随重定向，读取 Location 头，最多 5 跳。
     * 与自动 followRedirects 不同，这里在每一跳都只发送 UA（不带 Referer）。
     */
    private fun resolveRedirects(url: String, ua: String, maxHops: Int = 5): String {
        var current = url
        repeat(maxHops) {
            val request = Request.Builder()
                .url(current)
                .header("User-Agent", ua)
                .build()
            val response = noRedirectClient.newCall(request).execute()
            val location = response.use { it.header("Location") }
            if (location.isNullOrBlank()) return current
            current = location
        }
        return current
    }
}
