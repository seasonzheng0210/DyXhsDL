package com.neoruaa.xhsdn

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.neoruaa.xhsdn.utils.NotificationHelper
import android.content.Context
import android.util.Log

class XHSApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // UI v2：per-app locale 持久化——UI 文案统一走中文（values-zh），不随系统语言漂移。
        // API 33+ 由系统按包持久化；低版本配合 AndroidX AppCompat 回落。
        runCatching {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))
        }
    }

    companion object {
        @Volatile
        var isAppInForeground: Boolean = false
            private set
    }
}
