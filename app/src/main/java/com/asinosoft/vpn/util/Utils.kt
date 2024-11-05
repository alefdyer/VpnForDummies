package com.asinosoft.vpn.util

import android.content.Context
import android.provider.Settings
import android.util.Base64
import timber.log.Timber
import java.net.URLDecoder
import java.net.URLEncoder

object Utils {
    fun getDeviceIdForXUDPBaseKey(): String {
        val androidId = Settings.Secure.ANDROID_ID.toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(androidId.copyOf(32), Base64.NO_PADDING.or(Base64.URL_SAFE))
    }

    fun userAssetPath(context: Context?): String {
        if (context == null)
            return ""
        val extDir = context.getExternalFilesDir("assets")
            ?: return context.getDir("assets", 0).absolutePath
        return extDir.absolutePath
    }


    /**
     * base64 encode
     */
    fun encode(text: String): String {
        return try {
            Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * base64 decode
     */
    fun decode(text: String?): String {
        return tryDecodeBase64(text) ?: text?.trimEnd('=')?.let { tryDecodeBase64(it) }.orEmpty()
    }

    private fun tryDecodeBase64(text: String?): String? {
        try {
            return Base64.decode(text, Base64.NO_WRAP).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.i("Parse base64 standard failed $e")
        }
        try {
            return Base64.decode(text, Base64.NO_WRAP.or(Base64.URL_SAFE)).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            Timber.w("Parse base64 url safe failed $e")
        }
        return null
    }


    fun urlDecode(url: String): String {
        return try {
            URLDecoder.decode(url, Charsets.UTF_8.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            url
        }
    }

    fun urlEncode(url: String): String {
        return try {
            URLEncoder.encode(url, Charsets.UTF_8.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            url
        }
    }

    fun isIpAddress(value: String): Boolean {
        try {
            var addr = value
            if (addr.isEmpty() || addr.isBlank()) {
                return false
            }
            //CIDR
            if (addr.indexOf("/") > 0) {
                val arr = addr.split("/")
                if (arr.count() == 2 && Integer.parseInt(arr[1]) > -1) {
                    addr = arr[0]
                }
            }

            // "::ffff:192.168.173.22"
            // "[::ffff:192.168.173.22]:80"
            if (addr.startsWith("::ffff:") && '.' in addr) {
                addr = addr.drop(7)
            } else if (addr.startsWith("[::ffff:") && '.' in addr) {
                addr = addr.drop(8).replace("]", "")
            }

            // addr = addr.toLowerCase()
            val octets = addr.split('.').toTypedArray()
            if (octets.size == 4) {
                if (octets[3].indexOf(":") > 0) {
                    addr = addr.substring(0, addr.indexOf(":"))
                }
                return isIpv4Address(addr)
            }

            // Ipv6addr [2001:abc::123]:8080
            return isIpv6Address(addr)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun isIpv4Address(value: String): Boolean {
        val regV4 =
            Regex("^([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])$")
        return regV4.matches(value)
    }

    fun getIpv6Address(address: String?): String {
        if (address == null) {
            return ""
        }
        return if (isIpv6Address(address) && !address.contains('[') && !address.contains(']')) {
            String.format("[%s]", address)
        } else {
            address
        }
    }

    fun isIpv6Address(value: String): Boolean {
        var addr = value
        if (addr.indexOf("[") == 0 && addr.lastIndexOf("]") > 0) {
            addr = addr.drop(1)
            addr = addr.dropLast(addr.count() - addr.lastIndexOf("]"))
        }
        val regV6 =
            Regex("^((?:[0-9A-Fa-f]{1,4}))?((?::[0-9A-Fa-f]{1,4}))*::((?:[0-9A-Fa-f]{1,4}))?((?::[0-9A-Fa-f]{1,4}))*|((?:[0-9A-Fa-f]{1,4}))((?::[0-9A-Fa-f]{1,4})){7}$")
        return regV6.matches(addr)
    }

    fun isPureIpAddress(value: String): Boolean {
        return isIpv4Address(value) || isIpv6Address(value)
    }

    fun fixIllegalUrl(str: String): String = str
        .replace(" ", "%20")
        .replace("|", "%7C")

    fun removeWhiteSpace(str: String?): String? =
        str?.replace(" ", "")

    fun readTextFromAssets(context: Context, fileName: String): String =
        context.assets.open(fileName).bufferedReader().use {
            it.readText()
        }
}