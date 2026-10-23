package com.neoruaa.xhsdn.web

import org.json.JSONObject

/**
 * 小红书 API 响应解析（v3.3.3，Spider_XHS 内核配套）。
 *
 * 纯 JSON 解析、无 Android 依赖，供 MainActivity（列表）与 DownloadService（详情）复用。
 * 契约来源（勿凭手感改字段）：
 *  - `note_card` 结构在 user_posted 列表项与 feed `data.items[0]` 中**同构**
 *    （Spider_XHS `data_util.handle_note_info` 对两者同一解析）→ 核心提取 [parseXhsNoteCard] 共用。
 *  - 图 = `image_list[].info_list[1].url`（原图档，退 [0]，再退 url*）；
 *  - 视频 = `video.media.stream.{h264,EF4,h265,EF5,…}[].master_url|url|backup_urls[0]`，
 *    无 stream 退 `video.consumer.origin_video_key` → `https://sns-video-bd.xhscdn.com/{key}`。
 *    编码键优先 h264（兼容性最好；新版 Web 把编码名混淆为 EF4/EF5）。
 *  - 列表条目字段 = `xhs_home_sniff.js trimNote` 同契约：`{id|note_id, xsec_token, note_card:{…}}`。
 * 字段蛇形（API）为主、驼峰（SSR 岛）兜底 —— 两条链共用此解析。
 */

/** user_posted 列表条目（note_card 同构，媒体直链能提就提；缺的由批量阶段 feed 补）。 */
data class XhsNoteBrief(
    val noteId: String,
    val xsecToken: String,
    val title: String,
    val isVideo: Boolean,
    val coverUrl: String?,
    val videoUrl: String? = null,
    val imageUrls: List<String> = emptyList()
)

/** feed / note_card 媒体直链。视频笔记的 image_list 只是封面（data_util 同款语义）→ imageUrls 仅图集非空。 */
data class XhsNoteMedia(
    val isVideo: Boolean,
    val title: String,
    val videoUrl: String?,
    val imageUrls: List<String>,
    /** 首图（视频=封面；图集=第 1 张）。 */
    val coverUrl: String?
)

/** user_posted 分页收齐结果（XhsApi.getUserPostedAll）。 */
data class XhsPostedPage(
    val notes: List<XhsNoteBrief>,
    val nickname: String?,
    val total: Int,
    val hasMore: Boolean,
    /** 风控拒绝的 HTTP 码（实测 461）；0 = 正常。上层据此提示「登录失效」而不是「没有作品」。 */
    val riskStatus: Int = 0
)

/**
 * 核心提取：note_card → 媒体直链（图集/视频）+ 标题。
 * 列表项与 feed 详情的 note_card 同构，共用本函数。
 */
fun parseXhsNoteCard(card: JSONObject): XhsNoteMedia {
    val type = card.optString("type")
    val isVideo = type != "normal" && type.isNotBlank()
    val title = card.optString("title").ifBlank { card.optString("display_title") }

    // image_list：图集=可下载图片集；视频=仅封面（video_cover=image_list[0]，勿当图集下载）
    val imageUrls = mutableListOf<String>()
    var firstImage = ""
    val imageList = card.optJSONArray("image_list") ?: card.optJSONArray("imageList")
    if (imageList != null) {
        for (i in 0 until imageList.length()) {
            val img = imageList.optJSONObject(i) ?: continue
            val il = img.optJSONArray("info_list") ?: img.optJSONArray("infoList")
            var u = ""
            // data_util 契约：info_list[1] 是原图档，退 [0] 再退 url* 字段
            if (il != null && il.length() > 1) u = il.optJSONObject(1)?.optString("url").orEmpty()
            if (u.isBlank() && il != null && il.length() > 0) {
                u = il.optJSONObject(0)?.optString("url").orEmpty()
            }
            if (u.isBlank()) {
                u = img.optString("url_default").ifBlank { img.optString("url_pre") }
                    .ifBlank { img.optString("urlDefault") }.ifBlank { img.optString("url") }
            }
            if (u.isNotBlank()) {
                if (firstImage.isBlank()) firstImage = u
                if (!isVideo) imageUrls.add(u)
            }
        }
    }

    var videoUrl: String? = null
    if (isVideo) {
        val video = card.optJSONObject("video") ?: JSONObject()
        val streamMap = video.optJSONObject("media")?.optJSONObject("stream") ?: JSONObject()
        val preferred = listOf("h264", "EF4", "h265", "EF5")
        val codecs = preferred + streamMap.keys().asSequence().toList().filter { it !in preferred }
        codecLoop@ for (codec in codecs) {
            val arr = streamMap.optJSONArray(codec) ?: continue
            for (j in 0 until arr.length()) {
                val s = arr.optJSONObject(j) ?: continue
                val u = s.optString("master_url").ifBlank { s.optString("masterUrl") }
                    .ifBlank { s.optString("url") }
                    .ifBlank { s.optJSONArray("backup_urls")?.optString(0).orEmpty() }
                    .ifBlank { s.optJSONArray("backupUrls")?.optString(0).orEmpty() }
                if (u.isNotBlank()) {
                    videoUrl = u
                    break@codecLoop
                }
            }
        }
        if (videoUrl == null) {
            val consumer = video.optJSONObject("consumer")
            val key = consumer?.optString("origin_video_key")
                ?.ifBlank { consumer.optString("originVideoKey") }
            if (!key.isNullOrBlank()) videoUrl = "https://sns-video-bd.xhscdn.com/$key"
        }
    }
    return XhsNoteMedia(
        isVideo = isVideo, title = title, videoUrl = videoUrl,
        imageUrls = imageUrls, coverUrl = firstImage.takeIf { it.isNotBlank() }
    )
}

/** 解析 user_posted 单条；无 noteId 返回 null。 */
fun parseXhsPostedNote(n: JSONObject): XhsNoteBrief? {
    val card = n.optJSONObject("note_card") ?: n.optJSONObject("noteCard") ?: n
    val id = n.optString("id").ifBlank { n.optString("note_id") }
        .ifBlank { card.optString("note_id") }
        .takeIf { it.isNotBlank() } ?: return null
    val token = n.optString("xsec_token").ifBlank { n.optString("xsecToken") }
    val media = parseXhsNoteCard(card)
    val isVideo = media.isVideo

    var cover = ""
    val coverObj = card.optJSONObject("cover") ?: n.optJSONObject("cover")
    if (coverObj != null) {
        cover = coverObj.optString("url_pre").ifBlank { coverObj.optString("url_default") }
            .ifBlank { coverObj.optString("urlPre") }.ifBlank { coverObj.optString("urlDefault") }
        if (cover.isBlank()) {
            val il = coverObj.optJSONArray("info_list") ?: coverObj.optJSONArray("infoList")
            if (il != null) {
                var best = ""
                for (i in 0 until il.length()) {
                    val u = il.optJSONObject(i)?.let {
                        it.optString("url").ifBlank { it.optString("url_default") }
                            .ifBlank { it.optString("urlDefault") }
                    } ?: ""
                    if (u.length > best.length) best = u
                }
                cover = best
            }
        }
    }
    if (cover.isBlank()) cover = media.coverUrl ?: ""
    return XhsNoteBrief(
        noteId = id,
        xsecToken = token,
        title = media.title.ifBlank { card.optString("display_title") },
        isVideo = isVideo,
        coverUrl = cover.takeIf { it.isNotBlank() },
        videoUrl = media.videoUrl,
        imageUrls = media.imageUrls
    )
}

/**
 * 解析 feed 详情（root = feed 响应 JSON）：取 `data.items[0].note_card` 提媒体直链。
 * 无媒体也返回（调用方按 videoUrl/imageUrls 判空）；结构不合法返回 null。
 */
fun parseXhsFeedMedia(root: JSONObject): XhsNoteMedia? {
    val item = root.optJSONObject("data")?.optJSONArray("items")?.optJSONObject(0) ?: return null
    val card = item.optJSONObject("note_card") ?: item.optJSONObject("noteCard") ?: item
    return parseXhsNoteCard(card)
}
