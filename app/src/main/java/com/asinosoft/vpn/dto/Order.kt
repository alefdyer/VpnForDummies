package com.asinosoft.vpn.dto

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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

class PreviewOrderProvider : PreviewParameterProvider<Order> {
    override val values = sequenceOf(
        Order(
            "518578e3-0d30-4811-b0ff-18a3a3694f76",
            "subscription",
            Order.Content("year"),
            BigDecimal(123.45),
            Currency.getInstance("RUB")
        ),
    )
}
