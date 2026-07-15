package com.neoruaa.xhsdn.douyin

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 抖音直链解析。逻辑完整照搬可独立工作的 DouyinDL 项目（com.noctiro.douyindl）：
 *  - 使用随机 iPhone UA（Safari/CriOS/EdgiOS/FxiOS），抖音对固定安卓 Chrome UA 更敏感；
 *  - 解析重定向时手动读取 Location 头（不自动跟随），与 DouyinDL 一致；
 *  - 请求分享页时只带 UA、不带 Referer（DouyinDL 实测可用，带 Referer 易被抖音 security 风控拦截）。
 * 流程：解析重定向拿到 videoId → 请求 iesdouyin 分享页 → 提取 window._ROUTER_DATA
 * 中的 play_addr.url_list 并替换 playwm→play（去水印）。
 */
data class DouyinVideoInfo(
    val videoUrl: String,
    val title: String,
    val coverUrl: String?,
    val videoId: String,
    val userAgent: String
)

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

    suspend fun parse(url: String): DouyinVideoInfo = withContext(Dispatchers.IO) {
        val ua = randomUserAgent()
        val finalUrl = resolveRedirects(url, ua)
        val videoId = finalUrl.split("?")[0].trimEnd('/').split("/").last()
        val shareUrl = "https://www.iesdouyin.com/share/video/$videoId"

        Log.d(TAG, "finalUrl=$finalUrl videoId=$videoId shareUrl=$shareUrl ua=$ua")

        // 照搬 DouyinDL：只带 UA，不带 Referer
        val request = Request.Builder()
            .url(shareUrl)
            .header("User-Agent", ua)
            .build()

        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("获取抖音页面失败: HTTP ${response.code}")
            response.body?.string() ?: throw Exception("页面内容为空")
        }

        val pattern = Regex("""window\._ROUTER_DATA\s*=\s*(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(html) ?: throw Exception(
            "未找到 _ROUTER_DATA（疑似抖音 security 风控页）；finalUrl=$finalUrl；html前300字=${(html.take(300)).replace("\n", " ")}"
        )
        val jsonStr = match.groupValues[1].trim()

        val json = JSONObject(jsonStr)
        val loaderData = json.optJSONObject("loaderData") ?: throw Exception("loaderData 缺失")

        val pageKey = loaderData.keys().asSequence().firstOrNull { k ->
            (k.startsWith("video_") || k.startsWith("note_")) && k.endsWith("/page")
        } ?: throw Exception("未找到视频页面数据")

        val page = loaderData.optJSONObject(pageKey) ?: throw Exception("页面对象缺失")
        val videoInfoRes = page.optJSONObject("videoInfoRes") ?: throw Exception("videoInfoRes 缺失")
        val itemList = videoInfoRes.optJSONArray("item_list") ?: throw Exception("item_list 缺失")
        if (itemList.length() == 0) throw Exception("item_list 为空")

        val data = itemList.getJSONObject(0)
        val video = data.optJSONObject("video") ?: throw Exception("video 字段缺失")

        var videoUrl: String? = video.optJSONObject("play_addr")
            ?.optJSONArray("url_list")
            ?.optString(0)
            ?.takeIf { it.isNotBlank() }
            ?.replace("playwm", "play")

        if (videoUrl == null) {
            val bitRate = video.optJSONArray("bit_rate")
            if (bitRate != null && bitRate.length() > 0) {
                videoUrl = bitRate.getJSONObject(0)
                    .optJSONObject("play_addr")
                    ?.optJSONArray("url_list")
                    ?.optString(0)
                    ?.replace("playwm", "play")
            }
        }
        if (videoUrl == null) throw Exception("未提取到视频地址")

        val coverUrl = video.optJSONObject("cover")?.optJSONArray("url_list")?.optString(0)

        val desc = data.optString("desc", "").trim().ifEmpty { "douyin_$videoId" }
        val safeTitle = desc
            .replace(Regex("""[\\/:*?"<>|#\n\r]"""), "_")
            .replace(Regex("""\.{2,}"""), ".")
            .trim(' ', '.')
            .take(80)

        DouyinVideoInfo(videoUrl, safeTitle, coverUrl, videoId, ua)
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
