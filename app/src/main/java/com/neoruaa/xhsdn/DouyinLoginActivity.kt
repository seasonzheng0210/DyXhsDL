package com.neoruaa.xhsdn

import android.annotation.SuppressLint
import android.os.Bundle
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
import com.neoruaa.xhsdn.web.BgWebViewParser
import com.neoruaa.xhsdn.web.DouyinSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * DouyinLoginActivity —— 抖音账号登录（v3.3.15，对齐 [XhsLoginActivity] 先例）。
 *
 * 为什么需要：抖音主页作品列表对游客有硬上限（服务端只给首屏一页，实测 18~21 条，
 * 页面显示「登录后查看更多作品」），L1 自算 a_bogus 直调 post API 对游客大概率 403。
 * 未登录时主页下载拿不到全量 —— 与其事后在失败文案里猜「可能需要登录抖音」，
 * 不如给一个能真正把用户送进登录态的入口。
 *
 * 流程：WebView 桌面 UA 打开 www.douyin.com（站点自带登录弹窗：手机号验证码 / 扫码）
 * → 用户登录 → passport 把 sessionid 等写进 CookieManager → 轮询 [DouyinSession.hasLogin]
 * 命中后自动 setResult(RESULT_OK) + finish → 主界面据此重解析主页。
 * 保留「我已登录」手动确认按钮，兜住轮询漏检（子域 cookie 时机差异）。
 *
 * ⚠️ 不做签名校验：抖音没有 App 内可复算的签名内核，登录态只能以 cookie 为准。
 */
class DouyinLoginActivity : ComponentActivity() {

    internal var webViewRef: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DouyinLoginScreen(
                onWebView = { webViewRef = it },
                onConfirmed = { manual ->
                    val ok = DouyinSession.hasLogin()
                    if (!ok) {
                        Toast.makeText(
                            this,
                            getString(R.string.douyin_login_no_cookie),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@DouyinLoginScreen
                    }
                    Toast.makeText(
                        this,
                        getString(R.string.douyin_login_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                }
            )
        }
    }

    override fun onDestroy() {
        runCatching { webViewRef?.stopLoading() }
        runCatching { webViewRef?.destroy() }
        webViewRef = null
        super.onDestroy()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DouyinLoginScreen(
    onWebView: (WebView) -> Unit,
    onConfirmed: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(context.getString(R.string.douyin_login_hint)) }

    // 轮询登录态：抖音登录成功后 cookie 立即可见，自动返回避免用户多点一步。
    // 前 2s 不检测——页面还在加载，此刻命中的只会是历史登录态（那种情况本来就该直接放行，
    // 但先让页面渲染出来，用户才知道发生了什么）。
    DisposableEffect(Unit) {
        val job = scope.launch {
            delay(2000)
            var waited = 0L
            while (waited < 300_000L) {
                val hit = runCatching { DouyinSession.loginCookieNames() }.getOrDefault(emptyList())
                if (hit.isNotEmpty()) {
                    status = context.getString(R.string.douyin_login_detected)
                    delay(500)
                    runCatching { onConfirmed(false) }
                    break
                }
                delay(1500)
                waited += 1500
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
            text = context.getString(R.string.douyin_login_page_title),
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
                    settings.databaseEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    // 桌面 UA：与 DouyinParser / BgWebViewParser 同一套（移动 UA 打开
                    // www.douyin.com 会被降级到「请在抖音 App 内观看」页，没有登录入口）
                    settings.userAgentString = BgWebViewParser.DESKTOP_UA
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    webViewClient = WebViewClient()
                    loadUrl("https://www.douyin.com/")
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
                onClick = { onConfirmed(true) },
                modifier = Modifier.weight(1f)
            ) {
                Text(context.getString(R.string.douyin_login_confirm), fontSize = 14.sp)
            }
            androidx.compose.material3.TextButton(
                onClick = { (context as? DouyinLoginActivity)?.webViewRef?.reload() },
                modifier = Modifier.weight(1f)
            ) {
                Text(context.getString(R.string.douyin_login_reload), fontSize = 14.sp)
            }
        }
    }
}
