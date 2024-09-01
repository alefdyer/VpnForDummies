package com.asinosoft.vpn.util

fun Long.toSpeedString(): String = this.toTrafficString() + "/s"

const val THRESHOLD = 1000L
const val DIVISOR = 1024.0

fun Long.toTrafficString(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var size = this.toDouble()
    var unitIndex = 0
    while (size >= THRESHOLD && unitIndex < units.size - 1) {
        size /= DIVISOR
        unitIndex++
    }
    return String.format(null, "%.1f %s", size, units[unitIndex])
}
