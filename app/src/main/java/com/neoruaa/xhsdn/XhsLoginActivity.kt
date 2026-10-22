package com.neoruaa.xhsdn

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.neoruaa.xhsdn.web.XhsApi
import com.neoruaa.xhsdn.web.XhsSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * XhsLoginActivity —— 小红书扫码登录（v3.3.3，用户拍板路线：WebView 官方登录页收割 cookie）。
 *
 * 流程：WebView 打开 www.xiaohongshu.com（桌面 UA，站点自带登录二维码 + 全套安全链），
 * 用户用小红书 App 扫码登录 → 站点把 web_session/a1 等写进 CookieManager →
 * 点「我已扫码登录」从 CookieManager 收割进 XhsSession → XhsApi.bootstrapUserMe()
 * 用移植的签名内核做端到端校验（顺带拿 user_id 供 xy-direction 分片）。
 *
 * 为什么不直接渲染二维码：二维码要走 qrcode/create 签名接口，那需要冷启动全链
 * （websectiga/web_ssk/webprofile，尚未 vendor）。官方页面本身就是最成熟的登录实现。
 */
class XhsLoginActivity : ComponentActivity() {

    internal var webViewRef: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XhsLoginScreen(
                onWebView = { webViewRef = it },
                onConfirm = { after -> confirmLogin(after) },
            )
        }
    }

    override fun onDestroy() {
        runCatching { webViewRef?.stopLoading() }
        runCatching { webViewRef?.destroy() }
        webViewRef = null
        super.onDestroy()
    }

    /** 收割 CookieManager → XhsSession → user/me 校验。 */
    private fun confirmLogin(after: (String) -> Unit) {
        lifecycleScope.launch {
            after("正在校验登录状态…")
            val harvested = withContext(Dispatchers.IO) { harvestCookies() }
            if (harvested["web_session"].isNullOrEmpty()) {
                after(getString(R.string.xhs_login_no_cookie))
                return@launch
            }
            XhsSession.get(this@XhsLoginActivity).setCookies(harvested, merge = true)
            val result = withContext(Dispatchers.IO) {
                runCatching { XhsApi.get(this@XhsLoginActivity).bootstrapUserMe() }
            }
            result.fold(
                onSuccess = { json ->
                    val data = json.optJSONObject("data")
                    val ok = json.optBoolean("success", false) &&
                        !data?.optString("user_id").isNullOrEmpty()
                    if (ok) {
                        val name = data?.optString("nickname")
                            ?.takeIf { it.isNotEmpty() } ?: data!!.getString("user_id")
                        after(getString(R.string.xhs_login_success, name))
                        Toast.makeText(
                            this@XhsLoginActivity,
                            getString(R.string.xhs_login_success, name),
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        after(
                            getString(
                                R.string.xhs_login_verify_failed,
                                json.optString("msg", "user/me 未通过")
                            )
                        )
                    }
                },
                onFailure = { e ->
                    after(getString(R.string.xhs_login_verify_failed, e.message ?: "网络异常"))
                }
            )
        }
    }

    /** CookieManager 里 .xiaohongshu.com 全部 cookie（www 与 edith 都取，并集）。 */
    private fun harvestCookies(): Map<String, String> {
        val cm = CookieManager.getInstance()
        val out = linkedMapOf<String, String>()
        listOf(
            "https://www.xiaohongshu.com",
            "https://edith.xiaohongshu.com",
            "https://as.xiaohongshu.com",
        ).forEach { url ->
            val raw = cm.getCookie(url) ?: return@forEach
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
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun XhsLoginScreen(
    onWebView: (WebView) -> Unit,
    onConfirm: (after: (String) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(context.getString(R.string.xhs_login_hint)) }
    var cookieSeen by remember { mutableStateOf(false) }

    // 轮询 CookieManager：web_session 出现后提示用户收割（不自动收割——访客也拿
    // web_session，必须由用户确认扫码完成后再校验）。
    // ★cookieSeen 只做「初始提示」：把 status 从默认 hint 换成 detected 提示一次，
    // 之后 confirmLogin 的 after 回调可自由覆盖 status（v3.3.3 E2E 发现 cookieSeen
    // 恒 true 会盖住成功/失败消息）。
    DisposableEffect(Unit) {
        val job = scope.launch {
            while (true) {
                delay(2000)
                val raw = runCatching {
                    CookieManager.getInstance().getCookie("https://www.xiaohongshu.com")
                }.getOrNull()
                if (raw != null && raw.contains("web_session=") && !cookieSeen) {
                    cookieSeen = true
                    // 只在 status 仍是默认 hint 时替换，不覆盖 confirmLogin 回调消息
                    if (status == context.getString(R.string.xhs_login_hint)) {
                        status = context.getString(R.string.xhs_login_detected)
                    }
                }
            }
        }
        onDispose { job.cancel() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = context.getString(R.string.xhs_login_page_title),
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = status,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.userAgentString = com.neoruaa.xhsdn.web.BgWebViewParser.DESKTOP_UA
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    webViewClient = WebViewClient()
                    loadUrl("https://www.xiaohongshu.com/")
                    onWebView(this)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm { status = it } },
                modifier = Modifier.weight(1f)
            ) {
                Text(context.getString(R.string.xhs_login_confirm), fontSize = 14.sp)
            }
            androidx.compose.material3.TextButton(
                onClick = {
                    (context as? XhsLoginActivity)?.webViewRef?.reload()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(context.getString(R.string.xhs_login_reload), fontSize = 14.sp)
            }
        }
    }
}
