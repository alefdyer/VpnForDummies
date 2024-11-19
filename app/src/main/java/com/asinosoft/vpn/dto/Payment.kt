package com.asinosoft.vpn.dto

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.util.Currency

data class Payment(
    val id: String,
    val sum: BigDecimal,
    val currency: Currency,
    val status: Status = Status.NEW,
    @SerializedName("confirmation_url")
    val confirmationUrl: String?,
) {
    enum class Status {
        @SerializedName("new")
        NEW,

        @SerializedName("pending")
        PENDING,

        @SerializedName("waiting")
        WAITING,

        @SerializedName("completed")
        COMPLETED,

        @SerializedName("canceled")
        CANCELED,

        @SerializedName("refunded")
        REFUNDED;

        fun isComplete() = this == COMPLETED

        fun isFinal() = this == COMPLETED || this == CANCELED || this == REFUNDED
    }
}
