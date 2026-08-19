package com.neoruaa.xhsdn.utils

object UrlUtils {
    /**
     * 从文本中提取第一个 URL
     */
    fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        // 1. 标准带协议 URL
        val re = Regex("https?://[\\w\\-.]+(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?")
        re.find(text)?.value?.let { return it }
        // 2. 无协议但含已知域名的分享短链（如 v.douyin.com/xxx、iesdouyin.com/share/note/xxx、
        //    xhslink.com/xxx、kuaishou.com/xxx）。补全 https:// 后返回，避免「链接暂不可识别」。
        val re2 = Regex("(?:[\\w\\-]+\\.)?(douyin\\.com|iesdouyin\\.com|kuaishou\\.com|kuaishou\\.cn|gifshow\\.com|xiaohongshu\\.com|xhslink\\.com|xhslink\\.cn)(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?")
        re2.find(text)?.value?.let { return "https://$it" }
        return null
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
        val u = url.lowercase()
        return u.contains("douyin.com") || u.contains("iesdouyin.com")
    }

    /**
     * 检查是否为有效的快手链接（含手机分享短链 v.kuaishou.com、作品页、直播站、旧域名 gifshow.com）
     */
    fun isKuaishouLink(url: String?): Boolean {
        if (url == null) return false
        val u = url.lowercase()
        return u.contains("kuaishou.com") || u.contains("kuaishou.cn") ||
            u.contains("gifshow.com") || u.contains("chenzhongtech.com")
    }

    /**
     * 判断文本是否为「抖音主页链接」。
     * 命中场景：
     *  - 分享文案含「查看TA的更多作品」「查看更多作品」等主页分享特征；
     *  - 链接已解析为 www.douyin.com/user/{sec_uid} 主页直链。
     * 视频链接（/video/{id}）不命中，需经 feed 接口反查作者后再走主页下载。
     */
    fun isDouyinHomepageLink(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val u = text.lowercase()
        if (text.contains("查看ta的更多作品") || text.contains("查看更多作品") ||
            text.contains("查看ta的作品") || text.contains("more works") ||
            text.contains("more videos")) return true
        if (Regex("""douyin\.com/user/[A-Za-z0-9_\-]+""").containsMatchIn(u)) return true
        return false
    }

    /**
     * 从文本中提取抖音主页链接（主页分享文案或 /user/{sec_uid} 直链）。
     * 非主页链接返回 null。
     */
    fun extractDouyinHomepageUrl(text: String?): String? {
        if (!isDouyinHomepageLink(text)) return null
        return extractFirstUrl(text)
    }

    /**
     * 综合判断文本所属平台：抖音 / 快手 / 小红书 / 未知（null）。
     * 优先按 URL 域名判断，其次按文本中的中文/英文关键词判断，
     * 以便从分享文案（常含“抖音”“快手”“小红书”等字眼）直接分类。
     */
    fun detectPlatform(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val url = extractFirstUrl(text)
        // 1. 域名判断
        if (isDouyinLink(url)) return "douyin"
        if (isKuaishouLink(url)) return "kuaishou"
        if (isXhsLink(url)) return "xhs"
        // 2. 关键词判断（兼容中英文分享文案）
        if (text.contains("抖音") || text.contains("douyin", ignoreCase = true)) return "douyin"
        if (text.contains("快手") || text.contains("kuaishou", ignoreCase = true)) return "kuaishou"
        if (text.contains("小红书") || text.contains("xiaohongshu", ignoreCase = true) || text.contains("xhslink", ignoreCase = true)) return "xhs"
        return null
    }
}
