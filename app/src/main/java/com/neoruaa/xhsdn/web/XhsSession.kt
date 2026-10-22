package com.neoruaa.xhsdn.web

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

/**
 * XhsSession —— PC 协议会话状态（v3.3.3，Spider_XHS 内核移植）。
 *
 * 对齐 Spider_XHS 的 `XHSPcAuth + PcDeviceProfile + SessionState` 职责边界：
 *  - cookie 罐（a1/webId/web_session…）；document.cookie 对签名隐藏 web_session 等敏感项
 *  - sign_context：tier/now/version/loadts/seq/envConst/envFpTail/webBuild/signVersion/appId/platform
 *    （MNS 环境材料取自 xhs_reference_profile.json 的 mnsStages，与 Spider_XHS 同源）
 *  - dsl_pair = "null;<_dsl>"（冷链；_dsl 从 as.xiaohongshu.com/api/sec/v1/ds 响应 getdss() 提取）
 *  - b1：经 XhsSign.generateB1 由 b1Reference 模板生成（时间锚 = loadts + timeOriginOffsetMs）
 *
 * ⚠️ 本类不产生/保存登录凭据（web_session 等）——扫码登录是 Task #17 的
 * XhsLogin 的职责，落罐后由 setCookie() 写入。
 */
class XhsSession private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val appContext = context.applicationContext

    // ---- cookie 罐（保持插入顺序） ----
    private val cookieMap: LinkedHashMap<String, String> = LinkedHashMap()

    init {
        loadState()
    }

    // ------------------------------------------------------------------
    // cookie
    // ------------------------------------------------------------------

    @Synchronized
    fun cookies(): Map<String, String> = LinkedHashMap(cookieMap)

    @Synchronized
    fun a1(): String = cookieMap["a1"] ?: ""

    @Synchronized
    fun userId(): String = prefs.getString(KEY_USER_ID, "") ?: ""

    @Synchronized
    fun setUserId(uid: String) {
        prefs.edit().putString(KEY_USER_ID, uid).apply()
    }

    /** HTTP Cookie 头（全量）。 */
    @Synchronized
    fun networkCookie(): String =
        cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }

    /**
     * 签名用 document.cookie —— 隐藏服务端下发的敏感项（与 Spider_XHS
     * `document_cookie` 同一隐藏集）。web_session 是 Network Cookie 专属。
     */
    @Synchronized
    fun documentCookie(): String =
        cookieMap.entries
            .filter { it.key !in HIDDEN_FOR_SIGN }
            .joinToString("; ") { "${it.key}=${it.value}" }

    @Synchronized
    fun setCookies(values: Map<String, String>, merge: Boolean = true) {
        if (!merge) cookieMap.clear()
        values.forEach { (k, v) ->
            if (v.isEmpty()) cookieMap.remove(k) else cookieMap[k] = v
        }
        persistCookies()
    }

    @Synchronized
    fun hasLogin(): Boolean = !cookieMap["web_session"].isNullOrEmpty()

    // ------------------------------------------------------------------
    // dsl_pair
    // ------------------------------------------------------------------

    /**
     * 返回 "null;<_dsl>"。_dsl 缓存 30 分钟；过期或缺失时从
     * `https://as.xiaohongshu.com/api/sec/v1/ds?appId=xhs-pc-web` 拉取
     * （响应是一段含 `function getdss(){return '<digits>'}` 的 JS）。
     */
    suspend fun dslPair(): String {
        val cached = prefs.getString(KEY_DSL, null)
        val at = prefs.getLong(KEY_DSL_AT, 0L)
        if (!cached.isNullOrEmpty() && System.currentTimeMillis() - at < DSL_TTL_MS) {
            return "null;$cached"
        }
        return withContext(Dispatchers.IO) {
            val fetched = fetchDsl()
            if (fetched.isNotEmpty()) {
                prefs.edit().putString(KEY_DSL, fetched)
                    .putLong(KEY_DSL_AT, System.currentTimeMillis()).apply()
                "null;$fetched"
            } else {
                // 拉取失败但有旧值 → 旧值续用（签名只是锚点，短暂陈旧可容忍）
                if (!cached.isNullOrEmpty()) "null;$cached"
                else throw IllegalStateException("dsl 获取失败且无缓存")
            }
        }
    }

    private fun fetchDsl(): String {
        val url = URL("https://as.xiaohongshu.com/api/sec/v1/ds?appId=$APP_ID")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("user-agent", UA)
            setRequestProperty("referer", "https://www.xiaohongshu.com/")
        }
        try {
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val m = GETDSS_RE.find(body) ?: return ""
            return m.groupValues[1]
        } finally {
            conn.disconnect()
        }
    }

    // ------------------------------------------------------------------
    // sign_context / b1
    // ------------------------------------------------------------------

    /**
     * 生成 sign_context（对齐 PcDeviceProfile.next_sign_context）。
     * tier 解析：/api/sec/v1/ 等安全链 → 0201(security)；其余内容接口 →
     * 0101(coldContent)。浏览器冷启动同样先走 0101，之后服务端接 0301，
     * 由 XhsApi 在 406 闸门时自动升档重试。
     */
    @Synchronized
    fun nextSignContext(api: String, tierOverride: String? = null): JSONObject {
        val stage = when {
            tierOverride != null -> stageForTier(tierOverride)
            api.contains("/api/sec/v1/") || api.contains("/api/redcaptcha/") ||
                api.contains("sem_sdk") -> profile.stage("security")
            else -> profile.stage("coldContent")
        }
        val now = System.currentTimeMillis()
        return JSONObject().apply {
            put("tier", stage.tier)
            put("now", now)
            put("version", Random.nextInt(0, Int.MAX_VALUE).toLong() and 0xffffffffL)
            put("loadts", loadts())
            put("seq", nextSeq())
            put("envConst", stage.envConst)
            put("envFpTail", JSONArray(stage.envFpTail))
            put("webBuild", profile.webBuild)
            put("signVersion", profile.signVersion)
            put("appId", APP_ID)
            put("platform", "Windows")
            put("userAgent", UA)
            put("secChUa", profile.secChUa)
        }
    }

    private fun stageForTier(tier: String): MnsStage = when (tier) {
        "0201" -> profile.stage("security")
        "0301" -> profile.stage("steadyContent")
        else -> profile.stage("coldContent")
    }

    /** b1 明文：b1Reference 模板 + 时间锚。失败回退空串（浏览器冷链也是空 b1）。 */
    suspend fun currentB1(): String {
        return try {
            val b1 = profile.b1Reference
            val options = JSONObject().apply {
                put("now", System.currentTimeMillis())
                put("x39", b1.x39)
                put("x50", b1.x50)
                put("secCanvas", b1.secCanvas)
                put("windowKeys", JSONArray(b1.windowKeys))
                put("telemetry", JSONObject().apply {
                    put("profile", b1.telemetryProfile)
                    put("timeOrigin", loadts() + b1.timeOriginOffsetMs)
                    put("mouse", b1.mouse)
                    put("keyboard", b1.keyboard)
                    put("page", b1.page)
                    put("state", b1.state)
                    put("features", b1.features)
                })
                put("overrides", JSONObject().apply {
                    put("x36", b1.frameCount)
                    b1.telemetryTemplate?.takeIf { it.isNotEmpty() }?.let {
                        put("x84", it.replace("__TIME_ORIGIN__", (loadts() + b1.timeOriginOffsetMs).toString()))
                    }
                })
            }
            XhsSigner.get(appContext).generateB1(options)
        } catch (_: Throwable) {
            ""
        }
    }

    // ------------------------------------------------------------------
    // 会话计数
    // ------------------------------------------------------------------

    @Synchronized
    private fun loadts(): Long {
        var v = prefs.getLong(KEY_LOADTS, 0L)
        if (v <= 0L) {
            v = System.currentTimeMillis()
            prefs.edit().putLong(KEY_LOADTS, v).apply()
        }
        return v
    }

    @Synchronized
    private fun nextSeq(): Int {
        val v = prefs.getInt(KEY_SEQ, 0) + 1
        prefs.edit().putInt(KEY_SEQ, v).apply()
        return v
    }

    private fun loadState() {
        val raw = prefs.getString(KEY_COOKIES, null)
        if (!raw.isNullOrEmpty()) {
            runCatching {
                val obj = JSONObject(raw)
                obj.keys().forEach { k -> cookieMap[k] = obj.getString(k) }
            }
        }
    }

    private fun persistCookies() {
        val obj = JSONObject()
        cookieMap.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(KEY_COOKIES, obj.toString()).apply()
    }

    // ------------------------------------------------------------------
    // reference_profile.json 解析（与 Spider_XHS REFERENCE_PROFILE 同源）
    // ------------------------------------------------------------------

    private val profile: ReferenceProfile by lazy {
        val text = appContext.assets.open(PROFILE_ASSET)
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        ReferenceProfile.parse(JSONObject(text))
    }

    data class MnsStage(
        val tier: String,
        val envConst: Int,
        val envFpTail: List<Int>,
    )

    data class B1Reference(
        val frameCount: Int,
        val x39: Int,
        val x50: String,
        val secCanvas: String,
        val timeOriginOffsetMs: Double,
        val telemetryProfile: String,
        val windowKeys: List<String>,
        val mouse: JSONObject,
        val keyboard: JSONObject,
        val page: JSONObject,
        val state: JSONObject,
        val features: JSONObject,
        val telemetryTemplate: String?,
    )

    class ReferenceProfile(
        val webBuild: String,
        val signVersion: String,
        val secChUa: String,
        private val stages: Map<String, MnsStage>,
        val b1Reference: B1Reference,
    ) {
        fun stage(name: String): MnsStage =
            stages[name] ?: throw IllegalStateException("mnsStages 缺 $name")

        companion object {
            fun parse(root: JSONObject): ReferenceProfile {
                val release = root.getJSONObject("release")
                val stages = mutableMapOf<String, MnsStage>()
                val ms = root.getJSONObject("mnsStages")
                ms.keys().forEach { name ->
                    val s = ms.getJSONObject(name)
                    stages[name] = MnsStage(
                        tier = s.getString("tier"),
                        envConst = s.getInt("envConst"),
                        envFpTail = hexTailToBytes(s.getString("envFpTailHex")),
                    )
                }
                val b1 = root.getJSONObject("b1Reference")
                val windowKeys = b1.optJSONArray("selectedGlobalNames")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
                return ReferenceProfile(
                    webBuild = release.getString("webBuild"),
                    signVersion = release.getString("signVersion"),
                    secChUa = release.getString("secChUa"),
                    stages = stages,
                    b1Reference = B1Reference(
                        frameCount = b1.getInt("frameCount"),
                        x39 = b1.getInt("x39"),
                        x50 = b1.getString("x50"),
                        secCanvas = b1.getString("secCanvas"),
                        timeOriginOffsetMs = b1.optDouble("timeOriginOffsetMs", 0.0),
                        telemetryProfile = b1.optString("telemetryProfile", "active"),
                        windowKeys = windowKeys,
                        mouse = b1.optJSONObject("mouse") ?: JSONObject(),
                        keyboard = b1.optJSONObject("keyboard") ?: JSONObject(),
                        page = b1.optJSONObject("page") ?: JSONObject(),
                        state = b1.optJSONObject("state") ?: JSONObject(),
                        features = b1.optJSONObject("features") ?: JSONObject(),
                        telemetryTemplate = b1.optString("telemetryTemplate", ""),
                    ),
                )
            }

            /** envFpTailHex（28 hex = 14 字节）→ 无符号字节列表。 */
            private fun hexTailToBytes(hex: String): List<Int> =
                hex.chunked(2).map { it.toInt(16) }
        }
    }

    companion object {
        private const val PREFS = "xhs_pc_session"
        private const val KEY_COOKIES = "cookies_json"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LOADTS = "loadts"
        private const val KEY_SEQ = "seq"
        private const val KEY_DSL = "dsl"
        private const val KEY_DSL_AT = "dsl_at"
        private const val DSL_TTL_MS = 30 * 60 * 1000L
        private const val PROFILE_ASSET = "xhs_reference_profile.json"
        const val APP_ID = "xhs-pc-web"
        val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36"

        /** 签名 document.cookie 隐藏集（Spider_XHS document_cookie 同源）。 */
        private val HIDDEN_FOR_SIGN = setOf(
            "acw_tc", "web_session", "secure_session", "id_token",
            "customer-sso-sid", "access-token-creator.xiaohongshu.com",
            "galaxy_creator_session_id",
        )

        private val GETDSS_RE =
            Regex("function\\s+getdss\\s*\\(\\s*\\)\\s*\\{\\s*return\\s+'(\\d+)'")

        @Volatile
        private var instance: XhsSession? = null

        fun get(context: Context): XhsSession =
            instance ?: synchronized(this) {
                instance ?: XhsSession(context).also { instance = it }
            }
    }
}
