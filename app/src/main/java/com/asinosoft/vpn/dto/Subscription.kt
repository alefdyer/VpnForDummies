package com.asinosoft.vpn.dto

data class Subscription(
    val period: Period
) {
    enum class Period(value: String) {
        DAY("day"),
        WEEK("week"),
        MONTH("month"),
        YEAR("year"),
    }
}
