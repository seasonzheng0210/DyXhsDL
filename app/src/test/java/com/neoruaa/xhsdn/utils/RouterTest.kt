package com.neoruaa.xhsdn.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 路由接缝测试（对应 docs/specs/taobao-flow.md「边界：漏传 source 不得把淘宝当小红书」）。
 * 锁死 v1.0.17 回归点：默认 xhs 但 URL 实为淘宝/抖音域名时必须纠正。
 */
class RouterTest {

    @Test
    fun `taobao url with default xhs source is corrected to taobao`() {
        assertEquals("taobao", Router.resolveWebViewSource("https://item.taobao.com/item.htm?id=1", "xhs"))
    }

    @Test
    fun `taobao short link with null source is corrected to taobao`() {
        assertEquals("taobao", Router.resolveWebViewSource("https://e.tb.cn/h.abc?tk=x", null))
    }

    @Test
    fun `douyin url with default xhs source is corrected to douyin`() {
        assertEquals("douyin", Router.resolveWebViewSource("https://www.douyin.com/video/1", "xhs"))
    }

    @Test
    fun `explicit taobao source is respected even if url unrecognized`() {
        assertEquals("taobao", Router.resolveWebViewSource("https://example.com/foo", "taobao"))
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
