package com.asinosoft.vpn.dto

import java.util.Date

data class Subscription(
    val period: Period,
    val startAt: Date,
    val endAt: Date,
) {
    enum class Period(value: String) {
        DAY("day"),
        WEEK("week"),
        MONTH("month"),
        YEAR("year"),
    }
}
