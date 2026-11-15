package com.neoruaa.xhsdn.web

import android.util.Log
import android.webkit.CookieManager

/**
 * DouyinSession —— 抖音登录态判定（v3.3.15）。
 *
 * 抖音没有自建签名会话（与 [XhsSession] 不同：小红书要在 App 内跑 bundle 算 X-s，
 * 抖音的 x-secsdk-web-signature / verifyFp 只能由页面 secsdk JS 产出），
 * 所以这里不做 cookie 罐、不做签名，只回答一个问题：**当前 WebView 会话是否已登录抖音**。
 *
 * 判定依据 = passport 登录后才下发的凭据 cookie：
 *   sessionid / sessionid_ss（HttpOnly，主凭据）
 *   sid_tt / sid_guard / sid_ucp_v1 / ssid_ucp_v1（同族）
 *   uid_tt / uid_tt_ss（登录用户的 uid）
 * 游客态只有 ttwid / UIFID_TEMP / __ac_signature / msToken / tt_scid / s_v_web_id
 * ——这些是设备指纹，**不代表登录**，别拿去当登录判据。
 *
 * 为什么需要它：抖音主页作品列表 L1（自算 a_bogus 直调 post API）对游客大概率 403，
 * L2a（页面嗅探）服务端只给首屏一页（实测 18~21 条）。未登录时把「只能拿一页」讲清楚，
 * 并把用户送到官方登录页，比事后在失败文案里猜「可能需要登录」有用得多。
 *
 * ⚠️ getCookie() 能否返回 HttpOnly 的 sessionid 依赖 WebView 实现（Java CookieManager
 * 直读 cookie store，不受 document.cookie 限制）。若真机出现「明明登录了却判未登录」，
 * 先打 [debugCookieNames] 确认 sessionid 是否在串里，别直接怀疑登录态。
 */
object DouyinSession {

    private const val TAG = "DouyinSession"

    /**
     * 取并集的域：登录页在 passport.douyin.com 完成、cookie 落 .douyin.com，
     * 但部分链路（扫码回调）只在子域可见 —— 三个域并集，避免「登录了判不到」。
     */
    private val DOMAIN_URLS = listOf(
        "https://www.douyin.com/",
        "https://passport.douyin.com/",
        "https://m.douyin.com/"
    )

    /** 登录凭据 cookie（任一存在即视为已登录）。按重要性排序，仅用于诊断输出。 */
    private val LOGIN_COOKIE_KEYS = listOf(
        "sessionid", "sessionid_ss", "sid_tt", "sid_guard",
        "sid_ucp_v1", "ssid_ucp_v1", "uid_tt", "uid_tt_ss"
    )

    /** 快照 douyin.com 各子域全部 cookie（name → value，后取到的同名片覆盖）。 */
    fun cookies(): Map<String, String> {
        val cm = runCatching { CookieManager.getInstance() }.getOrNull() ?: return emptyMap()
        val out = LinkedHashMap<String, String>()
        DOMAIN_URLS.forEach { url ->
            val raw = runCatching { cm.getCookie(url) }.getOrNull() ?: return@forEach
            if (raw.isBlank()) return@forEach
            raw.split(";").forEach { pair ->
                val idx = pair.indexOf('=')
                if (idx > 0) {
                    val k = pair.substring(0, idx).trim()
                    val v = pair.substring(idx + 1).trim()
                    if (k.isNotEmpty()) out[k] = v
                }
            }
        }
        return out
    }

    /** 当前会话是否已登录抖音。 */
    fun hasLogin(): Boolean = loginCookieNames().isNotEmpty()

    /** 命中的登录凭据 cookie 名（空 = 未登录）。 */
    fun loginCookieNames(): List<String> {
        val all = cookies()
        if (all.isEmpty()) return emptyList()
        return LOGIN_COOKIE_KEYS.filter { !all[it].isNullOrBlank() }
    }

    /**
     * 诊断用：当前存在的 cookie 名（不含值，避免泄露凭据）。
     * 排队「登录了但被判未登录」时先看这里。
     */
    fun debugCookieNames(): List<String> {
        val names = cookies().keys.toList()
        Log.d(TAG, "hasLogin=${hasLogin()} hit=${loginCookieNames()} all=${names.size}项: $names")
        return names
    }
}
