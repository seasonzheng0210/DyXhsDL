package com.neoruaa.xhsdn.taobao

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
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
 * 淘宝（含天猫）分享短链解析骨架（含视频 best-effort 提取）。
 *
 * 目标链接形如手机淘宝分享文案里的短链：
 *   https://e.tb.cn/h.85d1cfjpNBy0DDp?tk=65ingCtOIIy
 * 以及其落地的商品详情页：
 *   https://item.taobao.com/item.htm?id=xxxx  /  https://detail.tmall.com/item.htm?id=xxxx
 *
 * 解析流程（与 DouyinParser 同构）：解短链拿商品 id → 拉详情页 → 提取主图/视频。
 *
 * 视频提取的两档（best-effort，失败不阻断主图）：
 *  1) 无登录态：从详情页抠 videoId，尝试淘宝视频规律直链
 *     https://cloud.video.taobao.com/play/u/1/e/1/t/{videoId}.mp4（HEAD 探活，命中才用）。
 *  2) 带登录态 cookie（[cookie] 字段，可由 App 设置注入）：走 mtop 媒体接口
 *     mtop.media.operator.queryVideoList，带 x-sign（基于 _m_h5_tk 的 MD5）拿真实视频直链。
 *
 * 已知边界（务必了解）：
 *  - 现代淘宝商品详情页是 JS 渲染的 SPA（h5.m.taobao.com/awp/core/detail.htm），主图/视频数据在
 *    页面加载后才通过 mtop 接口异步填充，原始 HTML 里通常没有 auctionImages / videoId。
 *    因此本解析器的匿名 HTML 正则方案对多数新商品会取不到主图而抛异常——这是平台机制，非代码 bug。
 *    真正稳健的「免登录」路径是 App 内用 WebView 加载商品页、等 JS 渲染后从 DOM 抠图（与 GitHub 上
 *    Vossera/TaobaoCrawler、answer2c/imgDown 用浏览器渲染同理），该方案为后续迭代点。
 *  - 淘宝 mtop 接口还要求 x-mini-wua / x-sgext 等抗刷参数，这些是淘宝 App native so 生成的，
 *    纯 Kotlin 无法复现。因此第 2 档（带 cookie 走 mtop）在多数新商品上仍可能被风控拦截；能成则成，不成自动回退主图。
 *  - 若详情页触发风控/登录校验（返回滑块页或登录墙），auctionImages 与 videoId 都取不到，parse 会抛明确异常，
 *    届时需补更完整的 cookie / 签名才能继续——这是后续迭代点。
 */
object TaobaoParser {
    private const val TAG = "TaobaoParser"

    // 手机淘宝分享 UA（AliApp(AP/...)）。淘宝对这类 UA 返回移动版详情，且对 PC Chrome UA 更敏感。
    const val MOBILE_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 AliApp(AP/10.0.1.123008) AlipayClient/10.0.1.123008 Language/zh-Hans"

    // 下载淘宝图/视频时必带的 Referer，否则图片/视频 CDN（img.alicdn.com / cloud.video.taobao.com）防盗链会 403。
    const val TAOBAO_REFERER = "https://item.taobao.com/"

    /** 可选登录态 cookie（含 _m_h5_tk）。由 App 设置注入；为空时只走第 1 档规律直链。 */
    var cookie: String = ""

    // mtop H5 接口常用 AppKey（m.taobao.com Web 端）
    private const val MTOP_APP_KEY = "12574478"

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
        Log.d(TAG, "input=$targetUrl cookieSet=${cookie.isNotBlank()}")

        // 1) 解短链拿商品 id
        val itemId = resolveItemId(targetUrl)
        Log.d(TAG, "itemId=$itemId")

        // 2) 拉详情页，提取主图与 best-effort 视频
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
        // 淘宝短链（e/m/c.tb.cn）现已改为返回 200 HTML 落地页（JS 跳转），
        // 正文里明文是 item.htm?id=... 或 detail.htm?id=...，两种都要匹配。
        Regex("""(?:item|detail)\.htm\?id=(\d{6,})""").find(html)?.groupValues?.getOrNull(1)?.let { return it }
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
            .header("Referer", TAOBAO_REFERER)
            .also { if (cookie.isNotBlank()) it.header("Cookie", cookie) }
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
                    "可尝试在已登录的浏览器里打开该链接，确认页面正常；若需登录态才能抓取，请在 App 设置中填入淘宝网页版 cookie。"
            )
        }

        val title = extractTitle(html, itemId)
        val coverUrl = imageUrls.firstOrNull()

        // 视频：先抠 videoId → 规律直链 / mtop；兜底再扫内联直链
        val videoUrl = resolveVideo(html)
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
     * 解析视频直链（best-effort）。
     *  - 先从详情页抠 videoId，尝试规律直链；
     *  - 带 cookie 时再走 mtop queryVideoList；
     *  - 最后兜底扫页面内联的 cloud.video.taobao.com 直链。
     * 任一拿到即返回；都失败返回 null（不影响主图下载）。
     */
    private fun resolveVideo(html: String): String? {
        val videoId = extractVideoId(html)
        if (videoId != null) {
            // 1) 规律直链（无需签名）
            val guess = "https://cloud.video.taobao.com/play/u/1/e/1/t/$videoId.mp4"
            if (headOk(guess)) {
                Log.d(TAG, "video via guess url: $guess")
                return guess
            }
            // 2) 带登录态 cookie 走 mtop
            if (cookie.isNotBlank()) {
                val mtop = mtopQueryVideoUrl(videoId)
                if (mtop != null) {
                    Log.d(TAG, "video via mtop: $mtop")
                    return mtop
                }
            }
        }
        // 3) 兜底：页面内联直链
        return extractVideoUrlInline(html)
    }

    /** 从详情页 HTML 抠 videoId / vid。 */
    private fun extractVideoId(html: String): String? {
        Regex(""""videoId"\s*:\s*"(\d+)"""").find(html)?.groupValues?.getOrNull(1)?.let { return it }
        Regex("""vid\s*[:=]\s*"?(\d+)"?""").find(html)?.groupValues?.getOrNull(1)?.let { return it }
        // 从内联直链里回抠 videoId
        Regex("""cloud\.video\.taobao\.com/play/u/\d+/e/\d+/t/(\d+)\.mp4""")
            .find(html)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }

    /** HEAD 探活：返回 true 表示该视频直链可访问。 */
    private fun headOk(url: String): Boolean {
        return try {
            val req = Request.Builder().url(url).head().header("User-Agent", MOBILE_UA).build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 带登录态 cookie 走 mtop.media.operator.queryVideoList 拿视频直链。
     * x-sign = md5(_m_h5_tk去掉后缀 & t & appKey & data)。
     * 注意：缺 x-mini-wua 等抗刷参数时多数新商品会被风控拦截，失败返回 null。
     */
    private fun mtopQueryVideoUrl(videoId: String): String? {
        val tk = Regex("""_m_h5_tk=([0-9a-f_]+)""").find(cookie)
            ?.groupValues?.getOrNull(1)?.substringBefore("_") ?: return null
        val api = "mtop.media.operator.queryVideoList"
        val v = "1.0"
        val data = """{"cid":"$videoId","videoId":"$videoId","type":1,"myId":"","source":""}"""
        val t = System.currentTimeMillis().toString()
        val sign = mtopSign(tk, t, MTOP_APP_KEY, data)
        val url = "https://h5api.m.taobao.com/h5/$api/$v/" +
            "?jsv=2.7.0&appKey=$MTOP_APP_KEY&t=$t&sign=$sign&api=$api&v=$v" +
            "&type=original&dataType=json&data=${java.net.URLEncoder.encode(data, "UTF-8")}"

        val req = Request.Builder().url(url)
            .header("User-Agent", MOBILE_UA)
            .header("Cookie", cookie)
            .header("Referer", "https://h5.m.taobao.com/")
            .build()

        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = JSONObject(resp.body?.string() ?: "")
                json.optJSONObject("data")?.optString("url")?.takeIf { it.isNotBlank() }
                    ?: json.optJSONObject("data")?.optJSONArray("list")
                        ?.optJSONObject(0)?.optString("url")?.takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun mtopSign(tk: String, t: String, appKey: String, data: String): String {
        return md5Hex("$tk&$t&$appKey&$data")
    }

    private fun md5Hex(s: String): String {
        val d = MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8))
        return d.joinToString("") { "%02x".format(it) }
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

    /** 兜底：抓详情页内联的视频直链（多模式，任一命中即返回）。 */
    private fun extractVideoUrlInline(html: String): String? {
        // 1) 淘宝视频云直链（最常见）
        Regex("""https?://cloud\.video\.taobao\.com/[^\s"\\]+?(?:\.mp4|\.mov)?""")
            .find(html)?.groupValues?.getOrNull(0)?.let { return it.trimEnd('"', '\\', ')') }
        // 2) 协议相对的视频云直链
        Regex("""//cloud\.video\.taobao\.com/[^\s"\\]+?(?:\.mp4|\.mov)?""")
            .find(html)?.groupValues?.getOrNull(0)?.let { return "https:${it.trimEnd('"', '\\', ')')}" }
        // 3) <video ... src="..."> 标签（部分页面直接内联）
        Regex("""<video[^>]*\ssrc=["']([^"']+)["']""")
            .find(html)?.groupValues?.getOrNull(1)?.let { return it }
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
