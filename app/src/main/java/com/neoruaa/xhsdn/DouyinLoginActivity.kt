package com.neoruaa.xhsdn

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.neoruaa.xhsdn.web.DouyinSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 登录页专用桌面 UA（与 BgWebViewParser.DESKTOP_UA 同值，独立声明避免跨页耦合）。 */
private const val DESKTOP_UA_LOGIN =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.0.0 Safari/537.36"

/**
 * DouyinLoginActivity —— 抖音账号登录（v3.3.15，对齐 [XhsLoginActivity] 先例）。
 *
 * 为什么需要：抖音主页作品列表对游客有硬上限（服务端只给首屏一页，实测 18~21 条，
 * 页面显示「登录后查看更多作品」），L1 自算 a_bogus 直调 post API 对游客大概率 403。
 * 未登录时主页下载拿不到全量 —— 与其事后在失败文案里猜「可能需要登录抖音」，
 * 不如给一个能真正把用户送进登录态的入口。
 *
 * 流程：WebView **移动 UA** 打开 m.douyin.com（站点自带登录入口：手机号验证码 / 扫码）
 * → 用户登录 → passport 把 sessionid 等写进 CookieManager → 轮询 [DouyinSession.hasLogin]
 * 命中后自动 setResult(RESULT_OK) + finish → 主界面据此重解析主页。
 * 保留「我已登录」手动确认按钮，兜住轮询漏检（子域 cookie 时机差异）。
 *
 * ★v3.3.17 窄屏适配（用户 2026-11-16 三连反馈「电脑端界面/黑屏/退不出」的定案）：
 *  - 抖音对 WebView 只出 PC 版：m.douyin.com 已 404（H5 站下线）、passport.douyin.com
 *    DNS 不解析、移动 UA 也一样是 PC 布局（服务端不看 UA）；
 *  - PC 页面按 ~1600px 布局，720~1080px 视口右侧被裁 → 右上角「登录」只露半截
 *    （「登」字）→ 用户看到「电脑端 + 点不到」；内容不满屏时 WebView 黑底露出来
 *    →「黑屏」；系统返回键其实一直有效，「退不出」是前两者的观感。
 *  - 解法 = onPageFinished 后 JS 轮询页面真实宽度，CSS zoom 缩到整页可见 +
 *    内建双指缩放（放大后 1:1 操作）+ 白底消黑块。
 *  - 已知局限：zoom 后直接 input tap「登录」弹窗不弹（触摸映射与 CSS zoom 的
 *    已知坑，两轮实测），需用户双指放大后点击。
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
            .background(Color.White)
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
                    // ★桌面 UA + 强制缩放（v3.3.17 第二轮实测定案）：
                    //  - m.douyin.com 已 404（H5 站下线）；sso.douyin.com 302 回 PC 主页；
                    //  - PC 版在 720px 视口按 ~1600px 布局渲染，右上角「登录」被裁掉一半
                    //    （用户看到「电脑端界面 + 右上角半个登字 + 下半截黑屏」三连）；
                    //  - loadWithOverviewMode 对该页不生效（页面自带 viewport meta）→
                    //    只能显式 setInitialScale 把整页缩进视口，另开双指缩放供放大操作。
                    //  45% ≈ 720/1600；登录后 cookie 照常落 .douyin.com 域。
                    settings.userAgentString = DESKTOP_UA_LOGIN
                    settings.setSupportZoom(true)
                    settings.setBuiltInZoomControls(true)
                    settings.setDisplayZoomControls(false)
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    setBackgroundColor(android.graphics.Color.WHITE)
                    // ★缩放注入（v3.3.17 定案）：setInitialScale/loadWithOverviewMode 都被
                    // 页面自己的 viewport meta 无视，移动 UA 也是 PC 版 → 只能在页面加载完
                    // 后用 CSS zoom 把 ~1600px 的 PC 布局缩进 720px 视口（0.45），
                    // 右上角「登录」才点得到；配合 setSupportZoom 双指放大操作。
                    // ★v3.3.18 消除「打开黑屏」：抖音页面重 + 模拟器软渲染慢，
                    // 加载的 4~10s 里黑底只有个「加载中」，用户会误判卡死。
                    // 先把 WebView 透明化，页面渲染完（onPageFinished + 首帧）再显示。
                    alpha = 0f
                    // 加载进度实时上屏：白屏期间用户看进度而不是一片黑
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            status = if (newProgress >= 90) {
                                context.getString(R.string.douyin_login_loaded)
                            } else {
                                context.getString(R.string.douyin_login_loading, newProgress)
                            }
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            view.animate().alpha(1f).setDuration(250).start()
                            // 状态行更新：提示文案是「整页可见后点登录」，
                            // 此刻起才是准确状态（之前显示的加载提示留给 status）
                            // 按「CSS 视口宽 / 页面实际宽」动态算 zoom：
                            // 720 屏（density 2.0 → CSS 宽 360）与 1080 屏（density 2.625 → 411）
                            // 各自适应，不再硬编码 0.45。
                            val cssWidth = view.width.takeIf { it > 0 }
                                ?.div(resources.displayMetrics.density) ?: 411f
                            // ★横向滚到最右（v3.3.17 定案）：PC 版登录按钮在页面右上角，
                            // 窄屏视口装不下整页。缩放方案（setInitialScale/CSS zoom）全部被
                            // 页面 viewport meta 或触摸映射击败：整页缩到 0.45 后视觉上按钮
                            // 可见但 input tap 不触发（两轮实测）。改为不缩放 + 滚到最右，
                            // 登录按钮以 1:1 进入视口，触摸命中天然可靠。SPA 渲染完成后
                            // scrollWidth 才变大 → 每 0.5s 轮询一次，最长 10s。
                            val js = "javascript:(function(){" +
                                "var n=0;" +
                                "var t=setInterval(function(){" +
                                "var w=Math.max(document.documentElement.scrollWidth," +
                                "document.body?document.body.scrollWidth:0);" +
                                "if(w>screen.width+40){" +
                                "window.scrollTo(w,0);" +
                                "document.documentElement.scrollLeft=w;" +
                                "document.body&&(document.body.scrollLeft=w);" +
                                "}" +
                                "if(++n>20)clearInterval(t);" +
                                "},500);" +
                                "})()"
                            view.evaluateJavascript(js, null)
                        }
                    }
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
