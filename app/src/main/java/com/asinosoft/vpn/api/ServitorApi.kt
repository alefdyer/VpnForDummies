package com.asinosoft.vpn.api

import com.asinosoft.vpn.dto.Config
import com.asinosoft.vpn.dto.Order
import com.asinosoft.vpn.dto.Payment
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ServitorApi {
    @GET("config")
    suspend fun getConfig(
        @Query("deviceId") deviceId: String,
    ): Config

    @POST("order")
    suspend fun createOrder(
        @Body data: CreateOrderRequest
    ): Order

    @POST("order/{orderId}/payment")
    suspend fun createPayment(
        @Path("orderId") orderId: String,
    ): Payment

    @POST("payment/{paymentId}/check")
    suspend fun checkPayment(
        @Path("paymentId") paymentId: String,
    ): Payment
}

data class CreateOrderRequest(
    val deviceId: String,
    val deviceModel: String,
    val item: String,
    val period: String,
)
