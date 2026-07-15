package com.neoruaa.xhsdn.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.neoruaa.xhsdn.viewmodels.MediaType
import java.io.File

fun decodeSampledBitmap(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    return runCatching {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        options.inJustDecodeBounds = false
        BitmapFactory.decodeFile(filePath, options)
    }.getOrNull()
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

fun createVideoThumbnail(file: File): Bitmap? {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) // 获取第1秒的帧
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

fun detectMediaType(path: String): MediaType {
    val extension = path.substringAfterLast(".", "")
    return if (extension.lowercase() in listOf("mp4", "mov", "avi", "mkv", "wmv", "flv", "webm")) {
        MediaType.VIDEO
    } else if (extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif")) {
        MediaType.IMAGE
    } else {
        MediaType.OTHER
    }
}