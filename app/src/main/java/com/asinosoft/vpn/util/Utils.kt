package com.asinosoft.vpn.util

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.Base64
import timber.log.Timber
import java.net.URLDecoder
import java.util.UUID

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

    private fun isIpv4Address(value: String): Boolean {
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

    private fun isIpv6Address(value: String): Boolean {
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

    fun readTextFromAssets(context: Context, fileName: String): String =
        context.assets.open(fileName).bufferedReader().use {
            it.readText()
        }
}

val Context.myDeviceId: String
    get() {
        val preferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        var deviceId = preferences.getString("device_id", null)
        if (null == deviceId) {
            deviceId = UUID.randomUUID().toString()
            preferences.edit().putString("device_id", deviceId).apply()
        }
        return deviceId
    }

/**
 * Convert system time into milliseconds, accepted by Handler::postAtTime
 */
fun Long.toUptimeMillis(): Long = SystemClock.uptimeMillis() + (this - System.currentTimeMillis())