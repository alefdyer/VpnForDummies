package com.asinosoft.vpn.util.fmt

import com.asinosoft.vpn.dto.EConfigType
import com.asinosoft.vpn.dto.ServerConfig
import com.asinosoft.vpn.dto.V2rayConfig
import com.asinosoft.vpn.util.Utils

object SocksFmt {
    fun parseSocks(str: String): ServerConfig? {
        val config = ServerConfig.create(EConfigType.SOCKS)
        var result = str.replace(EConfigType.SOCKS.protocolScheme, "")
        val indexSplit = result.indexOf("#")

        if (indexSplit > 0) {
            try {
                config.remarks =
                    Utils.urlDecode(result.substring(indexSplit + 1, result.length))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            result = result.substring(0, indexSplit)
        }

        //part decode
        val indexS = result.indexOf("@")
        result = if (indexS > 0) {
            Utils.decode(result.substring(0, indexS)) + result.substring(
                indexS,
                result.length
            )
        } else {
            Utils.decode(result)
        }

        val legacyPattern = "^(.*):(.*)@(.+?):(\\d+?)$".toRegex()
        val match =
            legacyPattern.matchEntire(result) ?: return null

        config.outboundBean?.settings?.servers?.get(0)?.let { server ->
            server.address = match.groupValues[3].removeSurrounding("[", "]")
            server.port = match.groupValues[4].toInt()
            val socksUsersBean =
                V2rayConfig.OutboundBean.OutSettingsBean.ServersBean.SocksUsersBean()
            socksUsersBean.user = match.groupValues[1]
            socksUsersBean.pass = match.groupValues[2]
            server.users = listOf(socksUsersBean)
        }

        return config
    }
}
