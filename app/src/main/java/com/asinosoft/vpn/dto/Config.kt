package com.asinosoft.vpn.dto

import android.content.Intent
import com.google.gson.Gson

data class Config(
    val url: String,
    val country: String,
    val location: String?,
    val breakForAdsInterval: Long = 0,
    val subscription: Subscription? = null,
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): Config = Gson().fromJson(json, Config::class.java)
    }
}

fun Intent.putConfig(config: Config): Intent = putExtra("config", config.toJson())

fun Intent.getConfig(): Config? = getStringExtra("config")?.let { Config.fromJson(it) }
