package com.asinosoft.vpn.api

import com.asinosoft.vpn.dto.Config
import retrofit2.http.GET
import retrofit2.http.Query

interface ServitorApi {
    @GET("config")
    suspend fun getConfig(
        @Query("deviceId") deviceId: String,
    ): Config
}