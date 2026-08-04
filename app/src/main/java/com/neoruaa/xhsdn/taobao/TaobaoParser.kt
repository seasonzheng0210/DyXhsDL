package com.neoruaa.xhsdn.taobao

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 淘宝媒体类型：视频 或 主图/图集（图片）。
 */
enum class TaobaoMediaType { VIDEO, IMAGE }

/**
 * 淘宝解析结果。
 *  - IMAGE：imageUrls 不为空（主图/图集），videoUrl 可能附带（商品主视频直链，best-effort）；
 *  - VIDEO ：videoUrl 不为空（此时通常 imageUrls 为空，仅视频）。
 */
data class TaobaoMediaInfo(
    val type: TaobaoMediaType,
    val title: String,
    val videoUrl: String?,
    val imageUrls: List<String>,
    val coverUrl: String?,
    val itemId: String,
    val userAgent: String
)

/**
 * 淘宝（含天猫）分享短链解析骨架。
 *
 * 目标链接形如手机淘宝分享文案里的短链：
 *   https://e.tb.cn/h.85d1cfjpNBy0DDp?tk=65ingCtOIIy
 * 以及其落地的商品详情页：
 *   https://item.taobao.com/item.htm?id=xxxx  /  https://detail.tmall.com/item.htm?id=xxxx
 *
 * 解析流程（与 DouyinParser 同构）：解短链拿商品 id → 拉详情页 → 提取主图/视频。
 *
 * 关键说明（best-effort 边界）：
 *  - 短链→id：e.tb.cn / m.tb.cn / c.tb.cn 都靠「跟随 302 重定向 + 抠 id」拿到商品 id，
 *    这一步不需要登录态，基本稳。
 *  - 主图：PC 详情页 item.taobao.com/item.htm?id= 内嵌 auctionImages 数组（主图原图地址），
 *    无需签名即可拿到，这一步也基本稳。
 *  - 视频：淘宝商品主视频的真实播放地址走签名接口（mtop.media.operator.queryVideoList），
 *    直链解析拿不到。本实现只 best-effort 抓取详情页里直接内联的 cloud.video.taobao.com 直链，
 *    抓不到就只下主图（type=IMAGE，videoUrl=null），不会因视频失败而阻断主图下载。
 *  - 若详情页触发风控/登录校验（返回滑块页），auctionImages 会取不到，parse 会抛明确异常，
 *    届时需补 cookie / mtop 签名才能继续——这是后续迭代点，不在本骨架范围内。
 */
object TaobaoParser {
    private const val TAG = "TaobaoParser"

    // 手机淘宝分享 UA（AliApp(AP/...)）。淘宝对这类 UA 返回移动版详情，且对 PC Chrome UA 更敏感。
    const val MOBILE_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 AliApp(AP/10.0.1.123008) AlipayClient/10.0.1.123008 Language/zh-Hans"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun canParse(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("tb.cn") || u.contains("taobao.com") || u.contains("tmall.com")
    }

    suspend fun parse(rawUrl: String): TaobaoMediaInfo = withContext(Dispatchers.IO) {
        val targetUrl = rawUrl.trim()
        Log.d(TAG, "input=$targetUrl")

        // 1) 解短链拿商品 id
        val itemId = resolveItemId(targetUrl)
        Log.d(TAG, "itemId=$itemId")

        // 2) 拉详情页，提取主图（best-effort 视频）
        return@withContext parseItemPage(itemId)
    }

    /**
     * 从短链解析商品 id。
     *  - 已是带 id 的详情链接 → 直接抠；
     *  - 否则请求短链（跟随 302），从最终 URL 抠 id，兜底再扫响应体里的 detail.htm?id=。
     */
    private fun resolveItemId(shortUrl: String): String {
        extractIdFromUrl(shortUrl)?.let { return it }

        val request = Request.Builder()
            .url(shortUrl)
            .header("User-Agent", MOBILE_UA)
            .build()

        val (finalUrl, body) = client.newCall(request).execute().use { resp ->
            val fu = resp.request.url.toString()
            val b = try { resp.body?.string() ?: "" } catch (_: Exception) { "" }
            fu to b
        }

        extractIdFromUrl(finalUrl)?.let { return it }
        extractIdFromBody(body)?.let { return it }

        throw Exception("无法从淘宝链接解析出商品 id（短链=$shortUrl，最终落地=$finalUrl）。可能是短链需登录态或已失效。")
    }

    private fun extractIdFromUrl(url: String): String? {
        // detail.htm?id=123 （id 通常 6 位以上数字）
        Regex("""[?&]id=(\d{6,})""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        // a.m.taobao.com/i123.htm
        Regex("""/i(\d+)\.htm""").find(url)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }

    private fun extractIdFromBody(html: String): String? {
        // 落地页 JS 里偶尔带 detail.htm?id=
        Regex("""detail\.htm\?id=(\d{6,})""").find(html)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }

    /**
     * 拉 PC 详情页（item.taobao.com/item.htm?id=），提取主图与 best-effort 视频直链。
     */
    private fun parseItemPage(itemId: String): TaobaoMediaInfo {
        val url = "https://item.taobao.com/item.htm?id=$itemId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_UA)
            .header("Accept", "text/html,application/xhtml+xml")
            .build()

        val html = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("淘宝详情页请求失败: HTTP ${resp.code}")
            resp.body?.string() ?: throw Exception("详情页内容为空")
        }

        // 主图：详情页内嵌 auctionImages: ["//img.alicdn.com/imgextra/i1/...", ...]
        val imageUrls = extractMainImages(html)
        if (imageUrls.isEmpty()) {
            throw Exception(
                "未从淘宝详情页提取到主图（可能触发风控/登录校验，或该商品无主图）。itemId=$itemId。" +
                    "可尝试在已登录的浏览器里打开该链接，确认页面正常；若需登录态才能抓取，后续需补 cookie。"
            )
        }

        val title = extractTitle(html, itemId)
        val coverUrl = imageUrls.firstOrNull()

        // 视频：best-effort 仅抓详情页内联的 cloud.video.taobao.com 直链
        val videoUrl = extractVideoUrl(html)
        val type = if (imageUrls.isNotEmpty()) TaobaoMediaType.IMAGE else TaobaoMediaType.VIDEO

        return TaobaoMediaInfo(
            type = type,
            title = title,
            videoUrl = videoUrl,
            imageUrls = imageUrls,
            coverUrl = coverUrl,
            itemId = itemId,
            userAgent = MOBILE_UA
        )
    }

    /**
     * 从详情页 HTML 提取主图地址列表。
     * 匹配 "auctionImages":[ ... ] 中的字符串数组，补全 https 协议，去重。
     */
    private fun extractMainImages(html: String): List<String> {
        val result = mutableListOf<String>()
        val m = Regex("""auctionImages"\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
            .find(html) ?: return result
        val arrStr = m.groupValues.getOrNull(1) ?: return result
        Regex(""""([^"]+)"""").findAll(arrStr).forEach { mg ->
            val u = mg.groupValues[1]
            when {
                u.startsWith("//") -> result.add("https:$u")
                u.startsWith("http") -> result.add(u)
            }
        }
        return result.distinct()
    }

    private fun extractTitle(html: String, itemId: String): String {
        val raw = Regex("""<title>(.*?)</title>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.getOrNull(1)?.trim()
            ?.replace(Regex("""[\\/:*?"<>|#\n\r]"""), "_")
            ?.take(80)
            ?: "taobao_$itemId"
        return raw.ifBlank { "taobao_$itemId" }
    }

    /**
     * best-effort 视频直链：只抓详情页内联的 cloud.video.taobao.com 播放地址。
     * 拿不到返回 null（不影响主图下载）。
     */
    private fun extractVideoUrl(html: String): String? {
        Regex("""https?://cloud\.video\.taobao\.com/[^\s"\\]+""")
            .find(html)?.groupValues?.getOrNull(0)?.let { return it }
        return null
    }

    /** 根据图片 URL 推断文件扩展名。 */
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
}
