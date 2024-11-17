package com.asinosoft.vpn.dto

import android.net.Uri
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.util.Currency

data class Payment(
    val id: String,
    val sum: BigDecimal,
    val currency: Currency,
    val status: Status = Status.NEW,
    val confirmationUrl: Uri?,
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

        fun isFinal() = this == COMPLETED || this == CANCELED || this == REFUNDED
    }
}
