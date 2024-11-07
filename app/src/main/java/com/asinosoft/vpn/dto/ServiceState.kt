package com.asinosoft.vpn.dto

import com.google.gson.Gson

data class ServiceState(
    val config: Config,
    val adsTime: Long
) {
    companion object {
        fun fromJson(json: String?): ServiceState = Gson().fromJson(json, ServiceState::class.java)
    }

    fun toJson(): String = Gson().toJson(this)
}
