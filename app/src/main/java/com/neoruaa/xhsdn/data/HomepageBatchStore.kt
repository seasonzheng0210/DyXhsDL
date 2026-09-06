package com.neoruaa.xhsdn.data

import com.neoruaa.xhsdn.douyin.DouyinPostItem

/**
 * 主页批量下载的批次暂存（进程内单例）。
 *
 * 为什么不走 Intent extra：作品列表可达数百条（含直链/图集 URL），序列化成 JSON 塞
 * Intent 有 TransactionTooLarge 风险。MainActivity 预览确认后 putBatch(token)，
 * DownloadService 用 EXTRA_URL=token 取回。App 与 Service 同进程，直接引用安全。
 */
object HomepageBatchStore {
    /** 范围档：仅新增(默认) / 全部作品(含已下载,受 skipDownloaded 控制) / 指定数量 N 条。 */
    enum class Range { ONLY_NEW, ALL, LATEST_N }

    data class Batch(
        val secUid: String,
        val authorNickname: String,
        val homepageUrl: String,
        val range: Range,
        val latestN: Int,
        val includeImages: Boolean,
        val skipDownloaded: Boolean,
        val items: List<DouyinPostItem>
    )

    private val pending = HashMap<String, Batch>()
    private var counter = 0L

    @Synchronized
    fun put(batch: Batch): String {
        val token = "hb_${System.currentTimeMillis()}_${counter++}"
        pending[token] = batch
        // 防泄漏：只保留最近 5 个批次
        while (pending.size > 5) {
            pending.remove(pending.keys.first())
        }
        return token
    }

    @Synchronized
    fun take(token: String): Batch? = pending.remove(token)
}
