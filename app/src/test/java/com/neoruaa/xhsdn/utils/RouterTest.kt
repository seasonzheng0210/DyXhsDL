package com.neoruaa.xhsdn.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 路由接缝测试。锁死回归点：默认 xhs 但 URL 实为快手/抖音域名时必须纠正。
 */
class RouterTest {

    @Test
    fun `kuaishou url with default xhs source is corrected to kuaishou`() {
        assertEquals("kuaishou", Router.resolveWebViewSource("https://www.kuaishou.com/short-video/abc", "xhs"))
    }

    @Test
    fun `kuaishou short link with null source is corrected to kuaishou`() {
        assertEquals("kuaishou", Router.resolveWebViewSource("https://v.kuaishou.com/abc", null))
    }

    @Test
    fun `douyin url with default xhs source is corrected to douyin`() {
        assertEquals("douyin", Router.resolveWebViewSource("https://www.douyin.com/video/1", "xhs"))
    }

    @Test
    fun `explicit kuaishou source is respected even if url unrecognized`() {
        assertEquals("kuaishou", Router.resolveWebViewSource("https://example.com/foo", "kuaishou"))
    }

    @Test
    fun `explicit douyin source is respected`() {
        assertEquals("douyin", Router.resolveWebViewSource("https://example.com/foo", "douyin"))
    }

    @Test
    fun `xhs url stays xhs`() {
        assertEquals("xhs", Router.resolveWebViewSource("https://www.xiaohongshu.com/explore/1", "xhs"))
    }

    @Test
    fun `unrecognized url with null source defaults to xhs`() {
        assertEquals("xhs", Router.resolveWebViewSource("https://example.com/foo", null))
    }
}
