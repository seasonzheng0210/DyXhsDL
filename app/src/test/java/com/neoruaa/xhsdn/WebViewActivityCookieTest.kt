package com.neoruaa.xhsdn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 回归测试：淘宝登录态判定（v1.0.21 修复点）。
 * 关键：cookie2 才是登录态主 cookie；匿名会话虽带 _m_h5_tk / unb，但无 cookie2，
 * 绝不能当作“已登录”——否则会出现“还没登录就说已保存”，且 HTTP 直解用匿名 cookie 撞登录墙。
 */
class WebViewActivityCookieTest {

    @Test
    fun `匿名会话带 _m_h5_tk 但未登录应判为非登录`() {
        val anon = "_m_h5_tk=abc123_def; unb=0; thw=cn; cookie2="
        assertFalse("匿名态(_m_h5_tk 存在但无有效 cookie2)不应判为已登录", isTaobaoLoggedIn(anon))
    }

    @Test
    fun `匿名会话仅含 _m_h5_tk 应判为非登录`() {
        val anon = "_m_h5_tk=abc123_def; thw=cn; cna=xxx"
        assertFalse(isTaobaoLoggedIn(anon))
    }

    @Test
    fun `含有效 cookie2 应判为已登录`() {
        val loggedIn = "_m_h5_tk=abc123_def; cookie2=xxxx-login-token-yyyy; unb=1234567890; thw=cn"
        assertTrue(isTaobaoLoggedIn(loggedIn))
    }

    @Test
    fun `空 cookie 应判为非登录`() {
        assertFalse(isTaobaoLoggedIn(""))
        assertFalse(isTaobaoLoggedIn("   "))
    }

    @Test
    fun `仅含 unb 但无 cookie2 的匿名态应判为非登录`() {
        // 部分匿名页面会带 unb=0，仍不算登录
        val anon = "unb=0; _m_h5_tk=abc; thw=cn"
        assertFalse(isTaobaoLoggedIn(anon))
    }
}
