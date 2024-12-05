package com.asinosoft.vpn

import android.app.Application
import com.google.firebase.ktx.BuildConfig
import com.yandex.mobile.ads.common.MobileAds
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        MobileAds.initialize(this) {}
    }
}
