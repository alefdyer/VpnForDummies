package com.asinosoft.vpn.util

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.dto.ERoutingMode
import com.asinosoft.vpn.dto.V2rayConfig
import com.google.gson.Gson

object V2rayConfigUtil {
    fun getV2rayConfig(
        context: Context,
        outbound: V2rayConfig.OutboundBean,
        remarks: String,
    ): String {
        val assets = Utils.readTextFromAssets(context, "v2ray_config.json")
        if (TextUtils.isEmpty(assets)) {
            return trace("Bundled config template (v2ray_config.json) not found")
        }

        val v2rayConfig = Gson().fromJson(assets, V2rayConfig::class.java)
            ?: return trace("Invalid bundled config template (v2ray_config.json)")

        v2rayConfig.log.loglevel = AppConfig.PREF_LOG_LEVEL_DEFAULT

        inbounds(v2rayConfig)

        updateOutboundWithGlobalSettings(outbound)
        v2rayConfig.outbounds[0] = outbound

        routing(v2rayConfig)

        dns(v2rayConfig)

        v2rayConfig.remarks = remarks

        return v2rayConfig.toPrettyPrinting()
    }

    private fun inbounds(v2rayConfig: V2rayConfig) {
        v2rayConfig.inbounds.forEach { curInbound ->
            curInbound.listen = "127.0.0.1"
        }
        v2rayConfig.inbounds[0].port = AppConfig.SOCKS_PORT
        val fakedns = AppConfig.PREF_FAKE_DNS_DEFAULT
        val sniffAllTlsAndHttp = true
        v2rayConfig.inbounds[0].sniffing?.enabled = fakedns || sniffAllTlsAndHttp
        v2rayConfig.inbounds[0].sniffing?.routeOnly = false
        if (!sniffAllTlsAndHttp) {
            v2rayConfig.inbounds[0].sniffing?.destOverride?.clear()
        }
        if (fakedns) {
            v2rayConfig.inbounds[0].sniffing?.destOverride?.add("fakedns")
        }

        v2rayConfig.inbounds[1].port = AppConfig.HTTP_PORT
    }

    private fun routing(v2rayConfig: V2rayConfig): Boolean {
        try {
            val routingMode = ERoutingMode.BYPASS_LAN_MAINLAND.value

            v2rayConfig.routing.domainStrategy = "IPIfNonMatch"

            // Hardcode googleapis.cn gstatic.com
            val googleapisRoute = V2rayConfig.RoutingBean.RulesBean(
                outboundTag = AppConfig.TAG_PROXY,
                domain = arrayListOf("domain:googleapis.cn", "domain:gstatic.com")
            )

            when (routingMode) {
                ERoutingMode.BYPASS_LAN.value -> {
                    routingGeo("", "private", AppConfig.TAG_DIRECT, v2rayConfig)
                }

                ERoutingMode.BYPASS_MAINLAND.value -> {
                    routingGeo("", "cn", AppConfig.TAG_DIRECT, v2rayConfig)
                    v2rayConfig.routing.rules.add(0, googleapisRoute)
                }

                ERoutingMode.BYPASS_LAN_MAINLAND.value -> {
                    routingGeo("", "private", AppConfig.TAG_DIRECT, v2rayConfig)
                    routingGeo("", "cn", AppConfig.TAG_DIRECT, v2rayConfig)
                    v2rayConfig.routing.rules.add(0, googleapisRoute)
                }

                ERoutingMode.GLOBAL_DIRECT.value -> {
                    val globalDirect = V2rayConfig.RoutingBean.RulesBean(
                        outboundTag = AppConfig.TAG_DIRECT,
                    )
                    if (v2rayConfig.routing.domainStrategy != "IPIfNonMatch") {
                        globalDirect.port = "0-65535"
                    } else {
                        globalDirect.ip = arrayListOf("0.0.0.0/0", "::/0")
                    }
                    v2rayConfig.routing.rules.add(globalDirect)
                }
            }

            if (routingMode != ERoutingMode.GLOBAL_DIRECT.value) {
                val globalProxy = V2rayConfig.RoutingBean.RulesBean(
                    outboundTag = AppConfig.TAG_PROXY,
                )
                if (v2rayConfig.routing.domainStrategy != "IPIfNonMatch") {
                    globalProxy.port = "0-65535"
                } else {
                    globalProxy.ip = arrayListOf("0.0.0.0/0", "::/0")
                }
                v2rayConfig.routing.rules.add(globalProxy)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun routingGeo(
        ipOrDomain: String,
        code: String,
        tag: String,
        v2rayConfig: V2rayConfig
    ) {
        try {
            if (!TextUtils.isEmpty(code)) {
                //IP
                if (ipOrDomain == "ip" || ipOrDomain == "") {
                    val rulesIP = V2rayConfig.RoutingBean.RulesBean()
                    rulesIP.outboundTag = tag
                    rulesIP.ip = ArrayList()
                    rulesIP.ip?.add("geoip:$code")
                    v2rayConfig.routing.rules.add(rulesIP)
                }

                if (ipOrDomain == "domain" || ipOrDomain == "") {
                    //Domain
                    val rulesDomain = V2rayConfig.RoutingBean.RulesBean()
                    rulesDomain.outboundTag = tag
                    rulesDomain.domain = ArrayList()
                    rulesDomain.domain?.add("geosite:$code")
                    v2rayConfig.routing.rules.add(rulesDomain)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun routingUserRule(userRule: String, tag: String, v2rayConfig: V2rayConfig) {
        try {
            if (!TextUtils.isEmpty(userRule)) {
                //Domain
                val rulesDomain = V2rayConfig.RoutingBean.RulesBean()
                rulesDomain.outboundTag = tag
                rulesDomain.domain = ArrayList()

                //IP
                val rulesIP = V2rayConfig.RoutingBean.RulesBean()
                rulesIP.outboundTag = tag
                rulesIP.ip = ArrayList()

                userRule.split(",").map { it.trim() }.forEach {
                    if (it.startsWith("ext:") && it.contains("geoip")) {
                        rulesIP.ip?.add(it)
                    } else if (Utils.isIpAddress(it) || it.startsWith("geoip:")) {
                        rulesIP.ip?.add(it)
                    } else if (it.isNotEmpty()) {
                        rulesDomain.domain?.add(it)
                    }
                }
                if ((rulesDomain.domain?.size ?: 0) > 0) {
                    v2rayConfig.routing.rules.add(rulesDomain)
                }
                if ((rulesIP.ip?.size ?: 0) > 0) {
                    v2rayConfig.routing.rules.add(rulesIP)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dns(v2rayConfig: V2rayConfig): Boolean {
        try {
            val hosts = mutableMapOf<String, Any>()
            val servers = ArrayList<Any>()

            //remote Dns
            val remoteDns = listOf(AppConfig.DNS_PROXY)
            val proxyDomain = userRule2Domain("")

            servers.addAll(remoteDns)

            if (proxyDomain.size > 0) {
                servers.add(
                    V2rayConfig.DnsBean.ServersBean(
                        remoteDns.first(),
                        53,
                        proxyDomain,
                        null
                    )
                )
            }

            // domestic DNS
            val domesticDns = listOf(AppConfig.DNS_DIRECT)
            val directDomain = userRule2Domain("")
            val routingMode = ERoutingMode.BYPASS_LAN_MAINLAND.value
            val isCnRoutingMode =
                (routingMode == ERoutingMode.BYPASS_MAINLAND.value || routingMode == ERoutingMode.BYPASS_LAN_MAINLAND.value)
            val geoipCn = arrayListOf("geoip:cn")

            if (directDomain.size > 0) {
                servers.add(
                    V2rayConfig.DnsBean.ServersBean(
                        domesticDns.first(),
                        53,
                        directDomain,
                        if (isCnRoutingMode) geoipCn else null
                    )
                )
            }
            if (isCnRoutingMode) {
                val geositeCn = arrayListOf("geosite:cn")
                servers.add(
                    V2rayConfig.DnsBean.ServersBean(
                        domesticDns.first(),
                        53,
                        geositeCn,
                        geoipCn
                    )
                )
            }

            if (Utils.isPureIpAddress(domesticDns.first())) {
                v2rayConfig.routing.rules.add(
                    0, V2rayConfig.RoutingBean.RulesBean(
                        outboundTag = AppConfig.TAG_DIRECT,
                        port = "53",
                        ip = arrayListOf(domesticDns.first()),
                        domain = null
                    )
                )
            }

            //block dns
            val blkDomain = userRule2Domain("")
            if (blkDomain.size > 0) {
                hosts.putAll(blkDomain.map { it to "127.0.0.1" })
            }

            // hardcode googleapi rule to fix play store problems
            hosts["domain:googleapis.cn"] = "googleapis.com"

            // hardcode popular Android Private DNS rule to fix localhost DNS problem
            hosts["dns.pub"] = arrayListOf("1.12.12.12", "120.53.53.53")
            hosts["dns.alidns.com"] =
                arrayListOf("223.5.5.5", "223.6.6.6", "2400:3200::1", "2400:3200:baba::1")
            hosts["one.one.one.one"] =
                arrayListOf("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001")
            hosts["dns.google"] =
                arrayListOf("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844")

            // DNS dns对象
            v2rayConfig.dns = V2rayConfig.DnsBean(
                servers = servers,
                hosts = hosts
            )

            // DNS routing
            if (Utils.isPureIpAddress(remoteDns.first())) {
                v2rayConfig.routing.rules.add(
                    0, V2rayConfig.RoutingBean.RulesBean(
                        outboundTag = AppConfig.TAG_PROXY,
                        port = "53",
                        ip = arrayListOf(remoteDns.first()),
                        domain = null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun userRule2Domain(userRule: String): ArrayList<String> {
        val domain = ArrayList<String>()
        userRule.split(",").map { it.trim() }.forEach {
            if (it.startsWith("geosite:") || it.startsWith("domain:")) {
                domain.add(it)
            }
        }
        return domain
    }

    private fun updateOutboundWithGlobalSettings(outbound: V2rayConfig.OutboundBean): Boolean {
        try {
            outbound.mux?.enabled = false
            outbound.mux?.concurrency = -1

            if (outbound.streamSettings?.network == V2rayConfig.DEFAULT_NETWORK
                && outbound.streamSettings?.tcpSettings?.header?.type == V2rayConfig.HTTP
            ) {
                val path = outbound.streamSettings?.tcpSettings?.header?.request?.path
                val host = outbound.streamSettings?.tcpSettings?.header?.request?.headers?.Host

                val requestString: String by lazy {
                    """{"version":"1.1","method":"GET","headers":{"User-Agent":["Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36","Mozilla/5.0 (iPhone; CPU iPhone OS 10_0_2 like Mac OS X) AppleWebKit/601.1 (KHTML, like Gecko) CriOS/53.0.2785.109 Mobile/14A456 Safari/601.1.46"],"Accept-Encoding":["gzip, deflate"],"Connection":["keep-alive"],"Pragma":"no-cache"}}"""
                }
                outbound.streamSettings?.tcpSettings?.header?.request = Gson().fromJson(
                    requestString,
                    V2rayConfig.OutboundBean.StreamSettingsBean.TcpSettingsBean.HeaderBean.RequestBean::class.java
                )
                outbound.streamSettings?.tcpSettings?.header?.request?.path =
                    if (path.isNullOrEmpty()) {
                        listOf("/")
                    } else {
                        path
                    }
                outbound.streamSettings?.tcpSettings?.header?.request?.headers?.Host = host
            }


        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun trace(message: String): String {
        Log.d(AppConfig.TAG, message)
        return ""
    }
}
