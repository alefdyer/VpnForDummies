package com.asinosoft.vpn.dto

import android.content.Intent
import android.net.Uri
import com.google.gson.Gson

data class Config(
    val url: Uri,
    val country: String,
    val location: String?,
    val breakForAdsInterval: Long = 0,
    val subscription: Subscription? = null,
) {
    private fun toJson(): String = Gson().toJson(this)

    fun toIntent(intent: Intent) = intent.putExtra("config", toJson())

    companion object {
        private fun fromJson(json: String): Config = Gson().fromJson(json, Config::class.java)

        fun fromIntent(intent: Intent?): Config? {
            val json = intent?.getStringExtra("config") ?: return null
            return fromJson(json)
        }
    }
}
