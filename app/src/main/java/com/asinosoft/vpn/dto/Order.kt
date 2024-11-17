package com.asinosoft.vpn.dto

import java.math.BigDecimal
import java.util.Currency

data class Order(
    val id: String,
    val item: String,
    val content: Content,
    val sum: BigDecimal,
    val currency: Currency
) {
    data class Content(
        val period: String
    )
}
