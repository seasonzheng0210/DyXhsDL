package com.neoruaa.xhsdn.kuaishou

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 快手媒体类型：视频 或 图集/图文（图片）。
 */
enum class KuaishouMediaType { VIDEO, IMAGE }

/**
 * 快手解析结果。
 *  - VIDEO：videoUrl 不为空（无水印 photoUrl），imageUrls 为空；
 *  - IMAGE ：imageUrls 不为空（图集），videoUrl 为 null。
 */
data class KuaishouMediaInfo(
    val type: KuaishouMediaType,
    val title: String,
    val videoUrl: String?,
    val imageUrls: List<String>,
    val coverUrl: String?,
    val photoId: String,
    val userAgent: String
)

/**
 * 快手（Kuaishou）直链解析。参考 GitHub 上 KS-Downloader / KuaishouParser 等公开方案：
 *
 * 解析路径（与 DouyinParser 同构）：快手自动下载入口（剪贴板/气泡/手动输入/自动读取）现已改为
 * 先走 WebView 真浏览器解析（最难被风控，与抖音一致）；本 Parser 作为 WebView 抓空时的 HTTP 直解兜底：
 *  1) 从分享链接解析出作品 id（photoId）：
 *     短链 v.kuaishou.com/CODE、www.kuaishou.com/f/CODE 会 302 跳转到
 *     www.kuaishou.com/short-video/{photoId}（中间可能经 v.m.chenzhongtech.com/fw/photo/{id}），
 *     跟随重定向后从最终 URL 抠 photoId。
 *  2) GraphQL visionVideoDetail（POST https://www.kuaishou.com/graphql）拿到无水印视频地址：
 *     关键字段 photo.photoUrl（官方播放源，本身无水印；水印只在 App「保存到相册」链路里烧录，
 *     播放源不含），photo.coverUrl，photo.images（图集），photo.caption，author.name。
 *     GraphQL 需要 did cookie（快手给每个访客下发设备标识 did，没有或太新会被风控返回空数据并引向滑块），
 *     故先 GET 首页拿 did/didv 注入请求头。
 *  3) 兜底：拉详情页 HTML，从内嵌 window.__INITIAL_STATE__ 抠同样的字段（与 WebView extractor 同数据源）。
 *
 * 已知边界：
 *  - 快手对 did 风控较严，匿名 GraphQL 偶尔返回空 → 自动回退 __INITIAL_STATE__，再不行抛错，
 *    此时请用 App 内 WebView 打开链接点「爬取」（WebView 自带 did 且渲染完整 DOM）。
 *  - 快手公开作品一般无需登录即可取无水印源，因此不引入登录态 cookie 机制。
 */
object KuaishouParser {
    private const val TAG = "KuaishouParser"

    // 移动端 UA：快手对移动/桌面返回不同页面结构，移动端 __INITIAL_STATE__ 数据更完整
    const val MOBILE_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"

    private const val GRAPHQL_URL = "https://www.kuaishou.com/graphql"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun canParse(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("kuaishou.com") || u.contains("kuaishou.cn") ||
            u.contains("gifshow.com") || u.contains("chenzhongtech.com")
    }

    suspend fun parse(rawUrl: String): KuaishouMediaInfo = withContext(Dispatchers.IO) {
        val targetUrl = rawUrl.trim()
        Log.d(TAG, "input=$targetUrl")

        val photoId = resolvePhotoId(targetUrl)
        Log.d(TAG, "photoId=$photoId")

        // 1) GraphQL（需 did cookie，返回无水印 photoUrl）
        val graphqlInfo = runCatching { parseViaGraphql(photoId) }.getOrNull()
        if (graphqlInfo != null) return@withContext graphqlInfo

        // 2) 兜底：详情页 __INITIAL_STATE__
        val htmlInfo = runCatching { parseViaInitialState(targetUrl, photoId) }.getOrNull()
        if (htmlInfo != null) return@withContext htmlInfo

        throw Exception(
            "无法解析快手作品（photoId=$photoId）。可能触发风控或链接失效；" +
                "请改用 App 内 WebView 打开该链接并点「爬取」。"
        )
    }

    /**
     * 从分享链接解析 photoId：先直接抠，失败再跟随重定向抠最终 URL。
     * 公开给 DownloadService：转后台 WebView 兜底前先拿 photoId 拼桌面作品页
     * （避开滑动流自动连播）+ 供 pollExtract 按直链 clientCacheKey 锁定原作品。
     * 注意：内部做网络 I/O，调用方须在 IO 调度器/协程里调用。失败抛异常。
     */
    fun resolvePhotoId(rawUrl: String): String {
        extractPhotoIdFromUrl(rawUrl)?.let { return it }

        val request = Request.Builder()
            .url(rawUrl)
            .header("User-Agent", MOBILE_UA)
            .build()
        val finalUrl = client.newCall(request).execute().use { resp -> resp.request.url.toString() }
        extractPhotoIdFromUrl(finalUrl)?.let { return it }

        throw Exception("无法从快手链接解析出作品 id（url=$rawUrl，最终落地=$finalUrl）")
    }

    private fun extractPhotoIdFromUrl(url: String): String? {
        // /short-video/{id}、/photo/{id}、chenzhongtech 的 /fw/photo/{id}
        Regex("""/(?:short-video|photo|fw/photo)/([A-Za-z0-9_-]+)""")
            .find(url)?.groupValues?.getOrNull(1)?.let { return it }
        // live.kuaishou.com/u/{authorId}/{photoId}
        Regex("""live\.kuaishou\.com/u/[^/]+/([A-Za-z0-9_-]+)""")
            .find(url)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }

    /**
     * GraphQL visionVideoDetail：带 did cookie 请求，返回无水印 photoUrl / 图集 images / cover / caption。
     */
    private fun parseViaGraphql(photoId: String): KuaishouMediaInfo? {
        val did = getDidCookie() ?: return null

        val query = """
        query visionVideoDetail(${"\$"}photoId: String, ${"\$"}page: String, ${"\$"}webPageArea: String) {
          visionVideoDetail(photoId: ${"\$"}photoId, page: ${"\$"}page, webPageArea: ${"\$"}webPageArea) {
            status
            photo {
              photoId
              caption
              photoUrl
              coverUrl
              photoH265Url
              manifest
              manifestH265
              type
              images { url }
              viewCount
              likedCount
              authorName
            }
            author { id name headerUrl }
          }
        }
        """.trimIndent()

        val bodyJson = JSONObject().apply {
            put("operationName", "visionVideoDetail")
            put("variables", JSONObject().apply {
                put("photoId", photoId)
                put("page", "detail")
                put("webPageArea", "default")
            })
            put("query", query)
        }.toString()

        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .header("User-Agent", MOBILE_UA)
            .header("Content-Type", "application/json")
            .header("Referer", "https://www.kuaishou.com/")
            .header("Cookie", did)
            .build()

        val json = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "graphql http ${resp.code}")
                return null
            }
            JSONObject(resp.body?.string() ?: "")
        }

        // 风控/空数据：data.visionVideoDetail 缺失或 photo 为空
        val photo = json.optJSONObject("data")
            ?.optJSONObject("visionVideoDetail")
            ?.optJSONObject("photo") ?: return null

        return extractFromPhoto(photo, photoId)
    }

    /**
     * GET 快手首页，从 Set-Cookie 取 did / didv（设备标识）。没有它 GraphQL 会被风控。
     */
    private fun getDidCookie(): String? {
        return try {
            val req = Request.Builder()
                .url("https://www.kuaishou.com/")
                .header("User-Agent", MOBILE_UA)
                .build()
            client.newCall(req).execute().use { resp ->
                val cookies = resp.headers("Set-Cookie")
                val did = cookies.firstOrNull { it.startsWith("did=") }
                    ?.substringAfter("did=")?.substringBefore(";")
                val didv = cookies.firstOrNull { it.startsWith("didv=") }
                    ?.substringAfter("didv=")?.substringBefore(";")
                if (did.isNullOrBlank()) null else buildString {
                    append("did=$did")
                    if (!didv.isNullOrBlank()) append("; didv=$didv")
                    append("; kpf=PC_WEB; clientid=3; kpn=KUAISHOU_VISION")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getDidCookie failed: ${e.message}")
            null
        }
    }

    /**
     * 拉详情页 HTML，从 window.__INITIAL_STATE__ 抠 photo 节点（与 WebView extractor 同数据源）。
     */
    private fun parseViaInitialState(targetUrl: String, photoId: String): KuaishouMediaInfo? {
        val url = if (targetUrl.contains("/short-video/")) targetUrl
        else "https://www.kuaishou.com/short-video/$photoId"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_UA)
            .header("Referer", "https://www.kuaishou.com/")
            .build()

        val html = client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            resp.body?.string() ?: return null
        }

        val jsonStr = Regex(
            """window\.__INITIAL_STATE__\s*=\s*(\{.*?\})\s*;""",
            RegexOption.DOT_MATCHES_ALL
        ).find(html)?.groupValues?.getOrNull(1) ?: return null

        val json = runCatching { JSONObject(jsonStr) }.getOrNull() ?: return null
        val photo = json.optJSONObject("photo")
            ?: json.optJSONObject("detail")?.optJSONObject("photo")
            ?: findPhotoNode(json)
            ?: return null

        return extractFromPhoto(photo, photoId)
    }

    /** 在 __INITIAL_STATE__ 里递归找第一个含 photoUrl/images 的 photo 节点。 */
    private fun findPhotoNode(node: Any?): JSONObject? {
        if (node !is JSONObject) return null
        if (node.has("photoUrl") || node.has("images")) return node
        val keys = node.keys()
        while (keys.hasNext()) {
            val v = node.opt(keys.next()) ?: continue
            if (v is JSONObject) {
                val found = findPhotoNode(v)
                if (found != null) return found
            } else if (v is org.json.JSONArray) {
                for (i in 0 until v.length()) {
                    val found = findPhotoNode(v.opt(i))
                    if (found != null) return found
                }
            }
        }
        return null
    }

    /** 从 photo 节点提取视频/图集/封面/标题。 */
    private fun extractFromPhoto(photo: JSONObject, photoId: String): KuaishouMediaInfo? {
        val videoUrl = photo.optString("photoUrl", "")
            .takeIf { it.isNotBlank() && it.startsWith("http") && !it.endsWith(".m3u8") }
        val coverUrl = photo.optString("coverUrl", "").takeIf { it.isNotBlank() }
        val imageUrls = extractImages(photo)

        if (videoUrl == null && imageUrls.isEmpty()) return null

        val type = if (imageUrls.isNotEmpty() && videoUrl == null) {
            KuaishouMediaType.IMAGE
        } else {
            KuaishouMediaType.VIDEO
        }
        val title = safeTitle(
            photo.optString("caption", "").ifBlank { "kuaishou_$photoId" },
            photoId
        )
        return KuaishouMediaInfo(
            type = type,
            title = title,
            videoUrl = videoUrl,
            imageUrls = imageUrls,
            coverUrl = coverUrl,
            photoId = photoId,
            userAgent = MOBILE_UA
        )
    }

    /** 图集图片地址列表：photo.images[i].url。 */
    private fun extractImages(photo: JSONObject): List<String> {
        val result = mutableListOf<String>()
        val arr = photo.optJSONArray("images") ?: return result
        for (i in 0 until arr.length()) {
            val it = arr.optJSONObject(i) ?: continue
            val u = it.optString("url", "").takeIf { s -> s.isNotBlank() && s.startsWith("http") }
            if (u != null) result.add(u)
        }
        return result.distinct()
    }

    private fun safeTitle(desc: String, id: String): String {
        val raw = desc.ifEmpty { "kuaishou_$id" }
        return raw
            .replace(Regex("""[\\/:*?"<>|#\n\r]"""), "_")
            .replace(Regex("""\.{2,}"""), ".")
            .trim(' ', '.')
            .take(80)
    }

    /**
     * 根据 URL 推断文件扩展名。
     */
    fun mediaExtension(url: String): String {
        val path = url.split("?")[0]
        val ext = path.substringAfterLast('.', "")
        return when (ext.lowercase()) {
            "jpg", "jpeg" -> "jpg"
            "png" -> "png"
            "webp" -> "webp"
            "gif" -> "gif"
            "mp4" -> "mp4"
            "mov" -> "mov"
            else -> "jpg"
        }
    }
}
