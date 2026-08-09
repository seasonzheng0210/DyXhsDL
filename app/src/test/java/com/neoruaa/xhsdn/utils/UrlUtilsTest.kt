package com.neoruaa.xhsdn.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 平台识别接缝测试。纯 JVM 单测，不依赖安卓框架、不碰网络。
 */
class UrlUtilsTest {

    @Test
    fun `kuaishou short link v kuaishou com is detected as kuaishou`() {
        assertEquals("kuaishou", UrlUtils.detectPlatform("https://v.kuaishou.com/3xAbCdEfG"))
    }

    @Test
    fun `kuaishou www f short link is detected as kuaishou`() {
        assertEquals("kuaishou", UrlUtils.detectPlatform("https://www.kuaishou.com/f/3xAbCdEfG"))
    }

    @Test
    fun `kuaishou com is detected as kuaishou`() {
        assertEquals("kuaishou", UrlUtils.detectPlatform("https://www.kuaishou.com/short-video/3xAbCdEfG"))
    }

    @Test
    fun `gifshow com is detected as kuaishou`() {
        assertEquals("kuaishou", UrlUtils.detectPlatform("https://www.gifshow.com/photo/3xAbCdEfG"))
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
    fun `keyword 快手 in text is detected as kuaishou`() {
        assertEquals("kuaishou", UrlUtils.detectPlatform("复制打开快手，看看这个视频 https://v.kuaishou.com/xxx/"))
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
