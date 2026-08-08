package com.neoruaa.xhsdn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 回归测试：淘宝登录态判定（v1.0.21 误判修复 + v1.0.25 依据真机实测数据重写）。
 *
 * 真机（模拟器 WebView 匿名会话）实测的 cookie 证明：
 * 匿名访客也有「非空 32 位 hex 的 cookie2=1dc163af1fc2109ee319e0e2a8de3aa4」，因此
 * 「cookie2 非空」不能作为登录标志（v1.0.21 的修复因此失效，导致"还没登录就说已保存"）。
 * 真正区分登录态的是 unb（用户数字ID）与 tracknick（昵称）——只有登录后才存在。
 */
class WebViewActivityCookieTest {

    @Test
    fun `真机实测匿名会话带非空cookie2应判非登录`() {
        // 模拟器 WebView 匿名登录墙会话的真实 cookie（cookie2 为 32 位 hex，无 unb/tracknick）
        val anon = "cna=KFj+IrJRJzQCAXjl0HIiSolH; _samesite_flag_=true; " +
            "cookie2=1dc163af1fc2109ee319e0e2a8de3aa4; t=7a7f455670b8c4cc67ca4dab441effc7; " +
            "_tb_token_=7b35343318d76; _m_h5_tk=e604f330caddd9ff80b6086cf06a9ba4_1786220580827; " +
            "xlly_s=1; isg=BJycKqJsGtgYme4ETKIcAPttZrNOFUA"
        assertFalse("匿名会话即使带非空 cookie2 也不应判为已登录", isTaobaoLoggedIn(anon))
    }

    @Test
    fun `匿名会话仅含_m_h5_tk应判非登录`() {
        val anon = "_m_h5_tk=abc123_def; thw=cn; cna=xxx"
        assertFalse(isTaobaoLoggedIn(anon))
    }

    @Test
    fun `登录态含unb数字ID应判已登录`() {
        val loggedIn = "_m_h5_tk=abc123_def; cookie2=xxxx; unb=1820441234; tracknick=test_user; thw=cn"
        assertTrue(isTaobaoLoggedIn(loggedIn))
    }

    @Test
    fun `登录态仅含tracknick昵称应判已登录`() {
        val loggedIn = "_m_h5_tk=abc; cookie2=xxxx; tracknick=hello%20world"
        assertTrue(isTaobaoLoggedIn(loggedIn))
    }

    @Test
    fun `unb为0应判非登录`() {
        val anon = "unb=0; _m_h5_tk=abc; thw=cn; cookie2=xxxx"
        assertFalse("unb=0 是无效用户ID，不应判为已登录", isTaobaoLoggedIn(anon))
    }

    @Test
    fun `空cookie应判非登录`() {
        assertFalse(isTaobaoLoggedIn(""))
        assertFalse(isTaobaoLoggedIn("   "))
    }
}
