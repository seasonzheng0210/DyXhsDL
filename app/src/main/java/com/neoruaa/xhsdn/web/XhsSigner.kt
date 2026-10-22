package com.neoruaa.xhsdn.web

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * XhsSigner —— 签名内核执行桥（v3.3.3，Spider_XHS 内核移植）。
 *
 * 把 `app/src/main/assets/xhs_sign_bundle.js`（由 tools/build_xhs_sign_bundle.mjs 生成，
 * 内核来源 cv-cat/Spider_XHS）载入一个空白 WebView 的 JS 上下文，之后所有签名调用
 * 通过 evaluateJavascript 执行。bundle 是纯计算（不碰网络/DOM），金标测试已证明
 * 与 Node 原版字节级一致（signFull 4/4 + buildRap）。
 *
 * 公共 API（global.XhsSign）：
 *   - signFull(inputJson) -> {xs, xt, x3, xs_common} 的 JSON 字符串
 *   - generateB1(optionsJson) -> b1 字符串
 *   - buildRap(api, dataJson, fingerprintHex?, optsJson?) -> x-rap-param base64
 *
 * 线程模型：WebView 只能在主线程操作；本类把调用统一抛到主线程执行后
 * 挂起等待结果。所有方法串行（AtomicBoolean 忙等互斥 + 主线程队列天然串行）。
 */
class XhsSigner(private val appContext: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private val bundleLoaded = AtomicBoolean(false)

    /** 载入 bundle（幂等）。失败抛异常。 */
    suspend fun ensureReady() {
        if (bundleLoaded.get()) return
        suspendCancellableCoroutine { cont ->
            mainHandler.post {
                try {
                    val wv = getOrCreateWebView()
                    if (!bundleLoaded.get()) {
                        val src = appContext.assets.open(BUNDLE_ASSET)
                            .bufferedReader(Charsets.UTF_8).use { it.readText() }
                        // ★bundle 是 IIFE 无 return → evaluateJavascript 返回 null/undefined
                        // 是正常的。验证方式：eval 后检查 global.XhsSign 是否存在。
                        wv.evaluateJavascript(src) {
                            wv.evaluateJavascript("typeof XhsSign") { probe ->
                                val ok = probe != null && probe.contains("object")
                                if (ok) {
                                    bundleLoaded.set(true)
                                    if (cont.isActive) cont.resume(Unit)
                                } else if (cont.isActive) {
                                    cont.resumeWithException(
                                        IllegalStateException("xhs_sign_bundle 载入失败 (probe=$probe)")
                                    )
                                }
                            }
                        }
                    } else if (cont.isActive) cont.resume(Unit)
                } catch (t: Throwable) {
                    if (cont.isActive) cont.resumeWithException(t)
                }
            }
        }
    }

    /** signFull：输入 run_signer payload 的 JSON，返回 {xs,xt,x3,xs_common}。 */
    suspend fun signFull(input: JSONObject): JSONObject {
        val raw = eval("XhsSign.signFull(" + JSONObject.quote(input.toString()) + ")")
        return JSONObject(raw)
    }

    /** generateB1：输入 to_b1_options 的 JSON，返回 b1 字符串。 */
    suspend fun generateB1(options: JSONObject): String {
        return eval("XhsSign.generateB1(" + JSONObject.quote(options.toString()) + ")")
    }

    /**
     * buildRap：生成 x-rap-param。
     * @param api 含 query 的接口路径（如 `/api/sns/web/v1/user_posted?num=30…`）
     * @param data 请求体（GET 传 ""）
     * @param fingerprintHex 留空则 bundle 内取 rap_fingerprint_template.json 的 bodyUnmaskedHex
     */
    suspend fun buildRap(api: String, data: String, fingerprintHex: String? = null): String {
        val args = listOf(
            JSONObject.quote(api),
            JSONObject.quote(data),
            JSONObject.quote(fingerprintHex ?: ""),
        ).joinToString(",")
        return eval("XhsSign.buildRap($args)")
    }

    /** 统一求值入口：JS 侧 try/catch 包裹，异常以 {ok:false,e} 返回。 */
    private suspend fun eval(expr: String): String {
        ensureReady()
        val wrapped = "(function(){try{return JSON.stringify({ok:true,r:($expr)})}" +
            "catch(e){return JSON.stringify({ok:false,e:String(e&&e.message||e)})}})()"
        val raw = suspendCancellableCoroutine<String?> { cont ->
            mainHandler.post {
                try {
                    webView?.evaluateJavascript(wrapped) { r ->
                        if (cont.isActive) cont.resume(r)
                    } ?: run {
                        if (cont.isActive) cont.resumeWithException(
                            IllegalStateException("XhsSigner WebView 已销毁")
                        )
                    }
                } catch (t: Throwable) {
                    if (cont.isActive) cont.resumeWithException(t)
                }
            }
        } ?: throw IllegalStateException("XhsSigner eval 返回 null（页面导航中？）")
        // evaluateJavascript 的返回值是 JS 值的 JSON 文本（字符串带引号+转义）
        val outer = JSONObject(unescapeJsonString(raw))
        if (!outer.optBoolean("ok", false)) {
            throw IllegalStateException("xhs_sign bundle 抛错: ${outer.optString("e")}")
        }
        return outer.get("r").let { r ->
            if (r is String) r else r.toString()
        }
    }

    private fun getOrCreateWebView(): WebView {
        webView?.let { return it }
        return WebView(appContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            // bundle 纯计算，不需要 UA/视口
        }.also { webView = it }
    }

    fun destroy() {
        mainHandler.post {
            runCatching { webView?.stopLoading() }
            runCatching { webView?.destroy() }
            webView = null
            bundleLoaded.set(false)
        }
    }

    companion object {
        private const val BUNDLE_ASSET = "xhs_sign_bundle.js"

        @Volatile
        private var instance: XhsSigner? = null

        fun get(context: Context): XhsSigner =
            instance ?: synchronized(this) {
                instance ?: XhsSigner(context.applicationContext).also { instance = it }
            }

        /**
         * evaluateJavascript 的返回值是「JS 值的 JSON 文本」——若 JS 返回的是
         * 字符串，raw 形如 "\"{\\\"ok\\\":true…}\""。先按 JSON 字符串解一层。
         */
        internal fun unescapeJsonString(raw: String): String {
            val t = raw.trim()
            if (t == "null" || t.isEmpty()) return "{\"ok\":false,\"e\":\"eval null\"}"
            return if (t.startsWith("\"")) JSONObject("{\"v\":$t}").getString("v") else t
        }
    }
}
