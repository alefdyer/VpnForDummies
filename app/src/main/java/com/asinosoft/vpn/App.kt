package com.asinosoft.vpn

import android.app.Application
import android.content.pm.ApplicationInfo
import com.yandex.mobile.ads.common.MobileAds
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val isDebug = 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE

        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        }

        MobileAds.initialize(this) {}
    }
}
