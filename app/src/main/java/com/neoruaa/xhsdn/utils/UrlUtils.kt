package com.neoruaa.xhsdn.utils

object UrlUtils {
    /**
     * 从文本中提取第一个 URL
     */
    fun extractFirstUrl(text: String): String? {
        val regex = Regex("https?://[\\w\\-.]+(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?")
        return regex.find(text)?.value
    }

    /**
     * 检查是否为有效的小红书链接
     */
    fun isXhsLink(url: String?): Boolean {
        if (url == null) return false
        return url.contains("xhslink.com") || url.contains("xhslink.cn") || url.contains("xiaohongshu.com")
    }

    /**
     * 检查是否为有效的抖音链接
     */
    fun isDouyinLink(url: String?): Boolean {
        if (url == null) return false
        return url.contains("douyin.com") || url.contains("iesdouyin.com")
    }

    /**
     * 综合判断文本所属平台：抖音 / 小红书 / 未知（null）。
     * 优先按 URL 域名判断，其次按文本中的中文/英文关键词判断，
     * 以便从分享文案（常含“抖音”“小红书”等字眼）直接分类。
     */
    fun detectPlatform(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val url = extractFirstUrl(text)
        // 1. 域名判断
        if (isDouyinLink(url)) return "douyin"
        if (isXhsLink(url)) return "xhs"
        // 2. 关键词判断（兼容中英文分享文案）
        if (text.contains("抖音") || text.contains("douyin", ignoreCase = true)) return "douyin"
        if (text.contains("小红书") || text.contains("xiaohongshu", ignoreCase = true) || text.contains("xhslink", ignoreCase = true)) return "xhs"
        return null
    }
}
