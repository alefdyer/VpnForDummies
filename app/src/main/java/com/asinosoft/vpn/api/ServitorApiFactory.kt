package com.asinosoft.vpn.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ServitorApiFactory {
    fun connect(url: String): ServitorApi {
        val builder = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api: ServitorApi = builder.create(ServitorApi::class.java)
        return api
    }
}
