package com.asinosoft.vpn

import android.net.Uri
import com.asinosoft.vpn.dto.SubnetAddress

object AppConfig {
    const val PACKAGE = "com.asinosoft.vpn"
    val LICENSES: Uri = Uri.parse("file:///android_asset/licenses.html")
    val PRIVATE_POLICY: Uri = Uri.parse("https://asinosoft.ru/vpn_privacy_policy.html")
    val OFFER_AND_ACCEPTANCE: Uri = Uri.parse("https://asinosoft.ru/vpn_public_offer_ru.html")
    val RATE_US: Uri = Uri.parse("market://details?id=com.asinosoft.vpn")

    const val HTTP_PORT = 10809
    const val SOCKS_PORT = 10808
    const val PREFER_IPV6 = false
    const val SESSION_NAME = "VPN"

    const val TAG_DIRECT = "direct"
    const val TAG_PROXY = "proxy"

    /** Network-related constants. */
    const val UPLINK = "uplink"
    const val DOWNLINK = "downlink"

    const val PREF_SERVITOR_URL = "servitor_url"
    const val RETRY_DELAY_MS = 5000L

    const val DELAY_TEST_URL = "https://www.gstatic.com/generate_204"
    const val DELAY_TEST_URL_2 = "https://www.google.com/generate_204"

    /** Default preferences */
    const val PREF_LOG_LEVEL_DEFAULT = "warning"
    const val PREF_FAKE_DNS_DEFAULT = false

    /** DNS server addresses. */
    const val DNS_PROXY = "1.1.1.1"
    const val DNS_DIRECT = "223.5.5.5"

    /** Notifications */
    const val NOTIFICATION_CHANNEL_ID = "VpnForDummiesNotifications"
    const val NOTIFICATION_CHANNEL_NAME = "VPN for dummies"

    /** Broadcast actions. */
    const val BROADCAST_ACTION_SERVICE = "com.asinosoft.vpn.action.service"
    const val BROADCAST_ACTION_ACTIVITY = "com.asinosoft.vpn.action.activity"

    /** Protocols Scheme **/
    const val VMESS = "vmess"
    const val CUSTOM = ""
    const val SHADOWSOCKS = "ss"
    const val SOCKS = "socks"
    const val VLESS = "vless"
    const val TROJAN = "trojan"
    const val WIREGUARD = "wireguard"

    val ALLOWED_APPS = arrayOf(
        "com.discord",
        "com.facebook.katana",
        "com.facebook.orca",
        "com.google.android.apps.youtube.kids",
        "com.google.android.apps.youtube.music",
        "com.google.android.youtube",
        "com.google.android.youtube.tv",
        "com.google.android.youtube.tvkids",
        "com.instagram.android",
        "com.twitter.android",
    )

    val BYPASS_IP_ADDRESSES = arrayOf(
        SubnetAddress("0.0.0.0", 5),
        SubnetAddress("8.0.0.0", 7),
        SubnetAddress("11.0.0.0", 8),
        SubnetAddress("12.0.0.0", 6),
        SubnetAddress("16.0.0.0", 4),
        SubnetAddress("32.0.0.0", 3),
        SubnetAddress("64.0.0.0", 2),
        SubnetAddress("128.0.0.0", 3),
        SubnetAddress("160.0.0.0", 5),
        SubnetAddress("168.0.0.0", 6),
        SubnetAddress("172.0.0.0", 12),
        SubnetAddress("172.32.0.0", 11),
        SubnetAddress("172.64.0.0", 10),
        SubnetAddress("172.128.0.0", 9),
        SubnetAddress("173.0.0.0", 8),
        SubnetAddress("174.0.0.0", 7),
        SubnetAddress("176.0.0.0", 4),
        SubnetAddress("192.0.0.0", 9),
        SubnetAddress("192.128.0.0", 11),
        SubnetAddress("192.160.0.0", 13),
        SubnetAddress("192.169.0.0", 16),
        SubnetAddress("192.170.0.0", 15),
        SubnetAddress("192.172.0.0", 14),
        SubnetAddress("192.176.0.0", 12),
        SubnetAddress("192.192.0.0", 10),
        SubnetAddress("193.0.0.0", 8),
        SubnetAddress("194.0.0.0", 7),
        SubnetAddress("196.0.0.0", 6),
        SubnetAddress("200.0.0.0", 5),
        SubnetAddress("208.0.0.0", 4),
        SubnetAddress("240.0.0.0", 4)
    )

    /** Message constants for communication. */
    const val MSG_REGISTER_CLIENT = 1
    const val MSG_STATE_RUNNING = 11
    const val MSG_STATE_NOT_RUNNING = 12
    const val MSG_UNREGISTER_CLIENT = 2
    const val MSG_STATE_START = 3
    const val MSG_STATE_START_SUCCESS = 31
    const val MSG_STATE_START_FAILURE = 32
    const val MSG_STATE_STOP = 4
    const val MSG_STATE_STOP_SUCCESS = 41
    const val MSG_STATE_RESTART = 5
    const val MSG_MEASURE_DELAY = 6
    const val MSG_MEASURE_DELAY_SUCCESS = 61
    const val MSG_ERROR_MESSAGE = 111
}
