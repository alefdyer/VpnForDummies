package com.asinosoft.vpn.dto

enum class ERoutingMode(val value: String) {
    BYPASS_LAN("1"),
    BYPASS_MAINLAND("2"),
    BYPASS_LAN_MAINLAND("3"),
    GLOBAL_DIRECT("4");
}
