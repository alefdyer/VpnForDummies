package com.asinosoft.vpn.util

import android.content.Context
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.R
import kotlinx.coroutines.isActive
import libv2ray.Libv2ray
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.URL
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

object SpeedtestUtil {

    private val tcpTestingSockets = ArrayList<Socket?>()

    suspend fun tcping(url: String, port: Int): Long {
        var time = -1L
        for (k in 0 until 2) {
            val one = socketConnectTime(url, port)
            if (!coroutineContext.isActive) {
                break
            }
            if (one != -1L && (time == -1L || one < time)) {
                time = one
            }
        }
        return time
    }

    fun realPing(config: String): Long {
        return try {
            Libv2ray.measureOutboundDelay(config, AppConfig.DELAY_TEST_URL)
        } catch (e: Exception) {
            Timber.w("realPing: $e")
            -1L
        }
    }

    fun ping(url: String): String {
        try {
            val command = "/system/bin/ping -c 3 $url"
            val process = Runtime.getRuntime().exec(command)
            val allText = process.inputStream.bufferedReader().use { it.readText() }
            if (!TextUtils.isEmpty(allText)) {
                val tempInfo = allText.substring(allText.indexOf("min/avg/max/mdev") + 19)
                val temps =
                    tempInfo.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (temps.count() > 0 && temps[0].length < 10) {
                    return temps[0].toFloat().toInt().toString() + "ms"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "-1ms"
    }

    fun socketConnectTime(url: String, port: Int): Long {
        try {
            val socket = Socket()
            synchronized(this) {
                tcpTestingSockets.add(socket)
            }
            val start = System.currentTimeMillis()
            socket.connect(InetSocketAddress(url, port), 3000)
            val time = System.currentTimeMillis() - start
            synchronized(this) {
                tcpTestingSockets.remove(socket)
            }
            socket.close()
            return time
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        } catch (e: IOException) {
            Timber.w("socketConnectTime IOException: $e")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return -1
    }

    fun closeAllTcpSockets() {
        synchronized(this) {
            tcpTestingSockets.forEach {
                it?.close()
            }
            tcpTestingSockets.clear()
        }
    }

    fun testConnection(context: Context, port: Int): String {
        // return V2RayVpnService.measureV2rayDelay()
        var result: String
        var conn: HttpURLConnection? = null

        try {
            val url = URL(AppConfig.DELAY_TEST_URL)

            conn = url.openConnection(
                Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress("127.0.0.1", port)
                )
            ) as HttpURLConnection
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.setRequestProperty("Connection", "close")
            conn.instanceFollowRedirects = false
            conn.useCaches = false

            val start = SystemClock.elapsedRealtime()
            val code = conn.responseCode
            val elapsed = SystemClock.elapsedRealtime() - start

            if (code == 204 || code == 200 && conn.contentLength == 0) {
                result = context.getString(R.string.connection_test_available, elapsed)
            } else {
                throw IOException(
                    context.getString(
                        R.string.connection_test_error_status_code,
                        code
                    )
                )
            }
        } catch (e: IOException) {
            // network exception
            Timber.w("testConnection IOException: " + Log.getStackTraceString(e))
            result = context.getString(R.string.connection_test_error, e.message)
        } catch (e: Exception) {
            // library exception, eg sumsung
            Timber.w("testConnection Exception: " + Log.getStackTraceString(e))
            result = context.getString(R.string.connection_test_error, e.message)
        } finally {
            conn?.disconnect()
        }

        return result
    }

    fun getLibVersion(): String {
        return Libv2ray.checkVersionX()
    }
}
