package com.neoruaa.xhsdn.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 平台识别接缝测试（对应 docs/specs/taobao-flow.md「边界：短链 e.tb.cn 必须识别为 taobao」）。
 * 纯 JVM 单测，不依赖安卓框架、不碰网络。
 */
class UrlUtilsTest {

    @Test
    fun `taobao short link e tb cn is detected as taobao`() {
        assertEquals("taobao", UrlUtils.detectPlatform("https://e.tb.cn/h.85d1cfjpNBy0DDp?tk=65ingCtOIIy"))
    }

    @Test
    fun `taobao short link m tb cn is detected as taobao`() {
        assertEquals("taobao", UrlUtils.detectPlatform("https://m.tb.cn/h.abc123xyz"))
    }

    @Test
    fun `item taobao com is detected as taobao`() {
        assertEquals("taobao", UrlUtils.detectPlatform("https://item.taobao.com/item.htm?id=926895986130"))
    }

    @Test
    fun `tmall com is detected as taobao`() {
        assertEquals("taobao", UrlUtils.detectPlatform("https://detail.tmall.com/item.htm?id=1006988355866"))
    }

    @Test
    fun `douyin com is detected as douyin`() {
        assertEquals("douyin", UrlUtils.detectPlatform("https://www.douyin.com/video/123"))
    }

    @Test
    fun `douyin iesdouyin com is detected as douyin`() {
        assertEquals("douyin", UrlUtils.detectPlatform("https://www.iesdouyin.com/share/video/1"))
    }

    @Test
    fun `xiaohongshu com is detected as xhs`() {
        assertEquals("xhs", UrlUtils.detectPlatform("https://www.xiaohongshu.com/explore/abc"))
    }

    @Test
    fun `xhslink com is detected as xhs`() {
        assertEquals("xhs", UrlUtils.detectPlatform("https://xhslink.com/abc"))
    }

    @Test
    fun `keyword 抖音 in text is detected as douyin`() {
        assertEquals("douyin", UrlUtils.detectPlatform("复制打开抖音，看看这个视频 https://v.douyin.com/xxx/"))
    }

    @Test
    fun `keyword 小红书 in text is detected as xhs`() {
        assertEquals("xhs", UrlUtils.detectPlatform("小红书 一起来看 https://xhslink.com/a/b"))
    }

    @Test
    fun `blank text returns null`() {
        assertEquals(null, UrlUtils.detectPlatform("   "))
        assertEquals(null, UrlUtils.detectPlatform(null))
    }

    @Test
    fun `unknown link returns null`() {
        assertEquals(null, UrlUtils.detectPlatform("https://example.com/foo"))
    }
}
