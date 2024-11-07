package com.asinosoft.vpn.util.fmt

import android.net.Uri
import com.asinosoft.vpn.dto.EConfigType
import com.asinosoft.vpn.dto.ServerConfig
import com.asinosoft.vpn.dto.V2rayConfig
import com.asinosoft.vpn.util.Utils

object TrojanFmt {
    fun parseTrojan(uri: Uri): ServerConfig {
        val config = ServerConfig.create(EConfigType.TROJAN)

        config.remarks = Utils.urlDecode(uri.fragment.orEmpty())

        var flow = ""
        var fingerprint = config.outboundBean?.streamSettings?.tlsSettings?.fingerprint
        if (uri.query == null) {
            config.outboundBean?.streamSettings?.populateTlsSettings(
                V2rayConfig.TLS,
                false,
                "",
                fingerprint,
                null,
                null,
                null,
                null
            )
        } else {
            val queryParam = uri.query!!.split("&")
                .associate { it.split("=").let { (k, v) -> k to Utils.urlDecode(v) } }

            val sni = config.outboundBean?.streamSettings?.populateTransportSettings(
                queryParam["type"] ?: "tcp",
                queryParam["headerType"],
                queryParam["host"],
                queryParam["path"],
                queryParam["seed"],
                queryParam["quicSecurity"],
                queryParam["key"],
                queryParam["mode"],
                queryParam["serviceName"],
                queryParam["authority"]
            )
            fingerprint = queryParam["fp"].orEmpty()
            val allowInsecure = (queryParam["allowInsecure"].orEmpty()) == "1"
            config.outboundBean?.streamSettings?.populateTlsSettings(
                queryParam["security"] ?: V2rayConfig.TLS,
                allowInsecure,
                queryParam["sni"] ?: sni.orEmpty(),
                fingerprint,
                queryParam["alpn"],
                null,
                null,
                null
            )
            flow = queryParam["flow"].orEmpty()
        }
        config.outboundBean?.settings?.servers?.get(0)?.let { server ->
            server.address = uri.host!!
            server.port = uri.port
            server.password = uri.userInfo!!
            server.flow = flow
        }

        return config
    }
}
