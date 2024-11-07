package com.asinosoft.vpn.util.fmt

import android.net.Uri
import com.asinosoft.vpn.dto.EConfigType
import com.asinosoft.vpn.dto.ServerConfig
import com.asinosoft.vpn.util.Utils

object VlessFmt {
    fun parseVless(uri: Uri): ServerConfig? {
        val config = ServerConfig.create(EConfigType.VLESS)

        if (uri.query.isNullOrEmpty()) return null
        val queryParam = uri.query!!.split("&")
            .associate { it.split("=").let { (k, v) -> k to Utils.urlDecode(v) } }

        val streamSetting = config.outboundBean?.streamSettings ?: return null

        config.remarks = Utils.urlDecode(uri.fragment.orEmpty())
        config.outboundBean.settings?.vnext?.get(0)?.let { vnext ->
            vnext.address = uri.host!!
            vnext.port = uri.port
            vnext.users[0].id = uri.userInfo!!
            vnext.users[0].encryption = queryParam["encryption"] ?: "none"
            vnext.users[0].flow = queryParam["flow"].orEmpty()
        }

        val sni = streamSetting.populateTransportSettings(
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
        val allowInsecure = (queryParam["allowInsecure"].orEmpty()) == "1"
        streamSetting.populateTlsSettings(
            queryParam["security"].orEmpty(),
            allowInsecure,
            queryParam["sni"] ?: sni,
            queryParam["fp"].orEmpty(),
            queryParam["alpn"],
            queryParam["pbk"].orEmpty(),
            queryParam["sid"].orEmpty(),
            queryParam["spx"].orEmpty()
        )

        return config
    }
}
