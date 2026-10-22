package com.neoruaa.xhsdn.web

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONStringer
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * XhsApi —— PC 端签名接口客户端（v3.3.3，Spider_XHS 内核移植）。
 *
 * 对齐 Spider_XHS `xhs_utils/xhs_pc/params.py + apis/xhs_pc_apis.py` 的
 * 浏览器实抓契约（vendored 内核 cv-cat/Spider_XHS）：
 *  - 必有：x-s / x-t / x-s-common / x-b3-traceid / x-xray-traceid
 *  - 条件：x-rap-param（RAP 白名单）、xy-direction（仅 homefeed/feed，murmur3 分片）
 *  - header 顺序 = PC_RAP_GET / PC_RAP_POST / PC_XY_RAP_POST / PC_BUSINESS_SIGNED_GET
 *  - tier 门禁 x3：0101→mns0101_(205)、0201→mns0201_(208)、0301→mns0301_(200)
 *  - 406 概率闸门：HTTP 406 或 body code:-1 → 升档（0101→0301）换新签名重试一次
 *
 * 注意：body 的字节串参与签名（RequestHash = xxHash32(url+body)），
 * 字段顺序与紧凑序列化（"," ":"）都是契约的一部分 → 用 JSONStringer 手排。
 */
class XhsApi private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val session = XhsSession.get(context)
    private val signer = XhsSigner.get(context)

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // ------------------------------------------------------------------
    // 对外接口
    // ------------------------------------------------------------------

    /**
     * user_posted —— 作者全部笔记列表（RAP GET，无 xy-direction）。
     * 返回服务端 JSON：{success,msg,code,data:{notes,cursor,has_more}}。
     */
    suspend fun getUserPosted(
        userId: String,
        cursor: String = "",
        xsecToken: String = "",
        xsecSource: String = "",
    ): JSONObject {
        val params = listOf(
            "num" to "30",
            "cursor" to cursor,
            "user_id" to userId,
            "image_formats" to "jpg,webp,avif",
            "xsec_token" to xsecToken,
            "xsec_source" to xsecSource,
        )
        val api = "/api/sns/web/v1/user_posted?" + encodeQuery(params)
        return request(api, data = "", method = "GET", needsRap = true, needsXy = false)
    }

    /**
     * feed —— 笔记详情（RAP POST + xy-direction）。
     * @param noteId 32 位 hex note id
     */
    suspend fun getNoteFeed(
        noteId: String,
        xsecToken: String = "",
        xsecSource: String = "pc_search",
    ): JSONObject {
        val body = JSONStringer().`object`()
            .key("source_note_id").value(noteId)
            .key("image_formats").array().value("jpg").value("webp").value("avif").endArray()
            .key("extra").`object`().key("need_body_topic").value("1").endObject()
            .key("xsec_source").value(xsecSource)
            .key("xsec_token").value(xsecToken)
            .endObject().toString()
        return request("/api/sns/web/v1/feed", data = body, method = "POST", needsRap = true, needsXy = true)
    }

    /** user/me —— 拿登录用户 user_id（xy-direction 分片用）；顺带写入 session。 */
    suspend fun bootstrapUserMe(): JSONObject {
        val res = request("/api/sns/web/v2/user/me", data = "", method = "GET", needsRap = false, needsXy = false)
        val uid = res.optJSONObject("data")?.optString("user_id") ?: ""
        if (uid.isNotEmpty()) session.setUserId(uid)
        return res
    }

    // ------------------------------------------------------------------
    // 请求装配（对齐 _request_params + build_pc_business_headers）
    // ------------------------------------------------------------------

    private suspend fun request(
        api: String,
        data: String,
        method: String,
        needsRap: Boolean,
        needsXy: Boolean,
    ): JSONObject {
        // xy-direction 需要 user_id（登录用户），缺则先 bootstrap
        if (needsXy && session.userId().isEmpty()) {
            runCatching { bootstrapUserMe() }
        }
        var tierOverride: String? = null
        var attempt = 0
        while (true) {
            attempt++
            val result = executeSigned(api, data, method, needsRap, needsXy, tierOverride)
            val json = result.json
            val gateHit = result.httpCode == 406 || json?.optInt("code", 0) == -1
            if (gateHit && attempt < 2) {
                // 406 概率闸门：浏览器冷启动 0101 被打时升档 0301 重签重试
                tierOverride = "0301"
                continue
            }
            return json ?: throw IOException("xhs api $api 返回非 JSON (HTTP ${result.httpCode})")
        }
    }

    private data class RawResult(val httpCode: Int, val json: JSONObject?)

    private suspend fun executeSigned(
        api: String,
        data: String,
        method: String,
        needsRap: Boolean,
        needsXy: Boolean,
        tierOverride: String?,
    ): RawResult = withContext(Dispatchers.IO) {
        val ctx = session.nextSignContext(api, tierOverride)
        val b1 = session.currentB1()
        val dslPair = session.dslPair()
        val now = ctx.getLong("now")

        val signInput = JSONObject().apply {
            put("api", api)
            put("data", data)
            put("cookie", session.documentCookie())
            put("a1", session.a1())
            put("b1", b1)
            put("dslPair", dslPair)
            put("tier", ctx.getString("tier"))
            put("now", now)
            put("version", ctx.getLong("version"))
            put("loadts", ctx.getLong("loadts"))
            put("seq", ctx.getInt("seq"))
            put("envConst", ctx.getInt("envConst"))
            put("envFpTail", ctx.get("envFpTail"))
            put("webBuild", ctx.getString("webBuild"))
            put("signVersion", ctx.getString("signVersion"))
            put("appId", ctx.getString("appId"))
            put("platform", ctx.getString("platform"))
            put("deviceTag", "")
        }
        val sign = signer.signFull(signInput)
        val xs = sign.getString("xs")
        val xt = sign.getString("xt")
        val xsCommon = sign.getString("xs_common")
        val x3 = sign.optString("x3")

        // tier 硬门禁（与 runtime._TIER_GATE 同）
        val gate = TIER_GATE[ctx.getString("tier")]
            ?: throw IllegalStateException("未知 tier ${ctx.getString("tier")}")
        if (x3.isEmpty() || !x3.startsWith(gate.first) || x3.length != gate.second) {
            throw IllegalStateException(
                "x3 门禁失败 tier=${ctx.getString("tier")} prefix=${x3.take(8)} len=${x3.length}"
            )
        }

        // x-rap-param（RAP 白名单；GET 空 body 也签）
        val rapParam = if (needsRap) signer.buildRap(api, data) else null

        val url = BASE + api
        val ordered = assembleHeaders(
            api = api, method = method,
            xs = xs, xt = xt, xsCommon = xsCommon,
            rapParam = rapParam,
            needsXy = needsXy,
            hasBody = method == "POST" && data.isNotEmpty(),
            xray = xrayTraceId(now, ctx.getInt("seq")),
        )

        val rb = Request.Builder().url(url)
        ordered.forEach { (k, v) -> rb.addHeader(k, v) }
        if (method == "POST") {
            rb.post(data.toRequestBody("application/json;charset=UTF-8".toMediaType()))
        } else {
            rb.get()
        }
        val resp = http.newCall(rb.build()).execute()
        resp.use { r ->
            val raw = r.body?.let { decodeBody(it.bytes(), r.header("Content-Encoding")) } ?: ByteArray(0)
            val text = raw.toString(Charsets.UTF_8)
            val json = runCatching { JSONObject(text) }.getOrNull()
            return@withContext RawResult(r.code, json)
        }
    }

    /**
     * header 顺序 = Spider_XHS build_pc_business_headers 的实抓契约
     * （include_client_hints=False 分支）。顺序表直接照抄 params.py 的
     * PC_*_HEADER_ORDER，勿凭手感重排——顺序是浏览器指纹的一部分。
     */
    private fun assembleHeaders(
        api: String,
        method: String,
        xs: String,
        xt: String,
        xsCommon: String,
        rapParam: String?,
        needsXy: Boolean,
        hasBody: Boolean,
        xray: String,
    ): List<Pair<String, String>> {
        val values = linkedMapOf(
            "referer" to "https://www.xiaohongshu.com/",
            "x-xray-traceid" to xray,
            "x-t" to xt,
            "x-b3-traceid" to randomHex16(),
            "x-s-common" to xsCommon,
            "user-agent" to XhsSession.UA,
            "accept" to "application/json, text/plain, */*",
            "x-s" to xs,
            "accept-encoding" to "gzip",
            "accept-language" to "zh-CN,zh;q=0.9,en;q=0.8,zh-TW;q=0.7,ja;q=0.6",
            "cookie" to session.networkCookie(),
            "origin" to "https://www.xiaohongshu.com",
            "priority" to "u=1, i",
            "sec-fetch-dest" to "empty",
            "sec-fetch-mode" to "cors",
            "sec-fetch-site" to "same-site",
        )
        if (rapParam != null) values["x-rap-param"] = rapParam
        if (hasBody) values["content-type"] = "application/json;charset=UTF-8"
        if (needsXy) {
            val uid = session.userId()
            require(uid.isNotEmpty()) { "xy-direction 需要 user_id" }
            values["xy-direction"] = xyDirection(uid).toString()
        }

        // PC_*_HEADER_ORDER（params.py 同源；content-type 无 body 时整项去除）
        val order: List<String> = when {
            needsXy -> listOf(
                "xy-direction", "referer", "x-xray-traceid", "x-t", "x-b3-traceid",
                "x-s-common", "x-rap-param", "accept", "content-type", "x-s",
                "user-agent", "accept-encoding", "accept-language", "cookie",
                "origin", "priority", "sec-fetch-dest", "sec-fetch-mode", "sec-fetch-site",
            )
            rapParam != null && method == "GET" -> listOf(
                "referer", "x-xray-traceid", "x-t", "x-b3-traceid",
                "x-s-common", "x-rap-param", "user-agent", "accept", "x-s",
                "accept-encoding", "accept-language", "cookie",
                "origin", "priority", "sec-fetch-dest", "sec-fetch-mode", "sec-fetch-site",
            )
            rapParam != null -> listOf(
                "referer", "x-xray-traceid", "x-t", "x-b3-traceid",
                "x-s-common", "x-rap-param", "accept", "content-type", "x-s",
                "user-agent", "accept-encoding", "accept-language", "cookie",
                "origin", "priority", "sec-fetch-dest", "sec-fetch-mode", "sec-fetch-site",
            )
            else -> listOf(
                "referer", "x-xray-traceid", "x-t", "x-b3-traceid",
                "x-s-common", "user-agent", "accept", "x-s",
                "accept-encoding", "accept-language", "cookie",
                "origin", "priority", "sec-fetch-dest", "sec-fetch-mode", "sec-fetch-site",
            )
        }

        val headerPath = api.substringBefore("?").trimEnd('/')
        val withCache = headerPath == "/api/sns/web/v2/user/me" ||
            headerPath == "/api/sns/web/v1/config" ||
            headerPath == "/api/sns/web/v1/system/config"

        val out = mutableListOf<Pair<String, String>>()
        for (name in order) {
            // cache-control/pragma 插入点（ordered_wire_headers 同规则）
            if (withCache && name == "cookie") out.add("cache-control" to "no-cache")
            if (withCache && name == "priority") out.add("pragma" to "no-cache")
            val v = values[name] ?: continue
            out.add(name to v)
        }
        return out
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private fun encodeQuery(params: List<Pair<String, String>>): String =
        params.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }

    /** x-xray-traceid = hex16((now_ms<<23)|seq) + hex16(random64)（自 xhs_xray.js 还原）。 */
    private fun xrayTraceId(nowMs: Long, seq: Int): String {
        // 无符号 64 位语义：%016x 对 Long 负值打印补码 = 正是无符号 hex
        val part1 = (nowMs shl 23) or (seq.toLong() and 0x7fffff)
        val part2 = java.util.Random().nextLong()
        return String.format("%016x%016x", part1, part2)
    }

    private fun randomHex16(): String {
        val chars = "abcdef0123456789"
        val r = java.util.Random()
        return buildString { repeat(16) { append(chars[r.nextInt(16)]) } }
    }

    /** xy-direction = murmurHash3_32(userId, 151488) % 100 + 1（对齐 index.bundle）。 */
    private fun xyDirection(userId: String): Int =
        (((murmur3_32(userId, 151488).toLong()) and 0xffffffffL) % 100 + 1).toInt()

    /** Spider_XHS murmur_hash3_32 的逐字节移植（尾字节只混合一次，勿「修正」）。 */
    private fun murmur3_32(key: String, seed: Int): Int {
        val data = key.toByteArray(Charsets.UTF_8)
        val length = data.size
        var h = seed
        val nblocks = length / 4
        for (i in 0 until nblocks) {
            var k = (data[4 * i].toInt() and 0xff) or
                ((data[4 * i + 1].toInt() and 0xff) shl 8) or
                ((data[4 * i + 2].toInt() and 0xff) shl 16) or
                ((data[4 * i + 3].toInt() and 0xff) shl 24)
            k = (k * 0xcc9e2d51.toInt())
            k = (k shl 15) or (k ushr 17)
            k = (k * 0x1b873593)
            h = h xor k
            h = (h shl 13) or (h ushr 19)
            h = (h * 5 + 0xe6546b64.toInt())
        }
        var k = 0
        val rem = length % 4
        val tail = 4 * nblocks
        if (rem == 3) k = k xor ((data[tail + 2].toInt() and 0xff) shl 16)
        if (rem >= 2) k = k xor ((data[tail + 1].toInt() and 0xff) shl 8)
        if (rem >= 1) {
            k = k xor (data[tail].toInt() and 0xff)
            k = (k * 0xcc9e2d51.toInt())
            k = (k shl 15) or (k ushr 17)
            k = (k * 0x1b873593)
            h = h xor k
        }
        h = h xor length
        h = h xor (h ushr 16)
        h = (h * 0x85ebca6b.toInt())
        h = h xor (h ushr 13)
        h = (h * 0xc2b2ae35.toInt())
        h = h xor (h ushr 16)
        return h
    }

    private fun decodeBody(bytes: ByteArray, contentEncoding: String?): ByteArray {
        if (contentEncoding?.contains("gzip") == true) {
            return GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
        }
        return bytes
    }

    companion object {
        private const val BASE = "https://edith.xiaohongshu.com"

        /** runtime._TIER_GATE 同源。 */
        private val TIER_GATE = mapOf(
            "0101" to ("mns0101_" to 205),
            "0201" to ("mns0201_" to 208),
            "0301" to ("mns0301_" to 200),
        )

        @Volatile
        private var instance: XhsApi? = null

        fun get(context: Context): XhsApi =
            instance ?: synchronized(this) {
                instance ?: XhsApi(context).also { instance = it }
            }
    }
}
