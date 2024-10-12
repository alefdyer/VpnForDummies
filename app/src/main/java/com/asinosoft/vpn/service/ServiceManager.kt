package com.asinosoft.vpn.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.MainActivity
import com.asinosoft.vpn.R
import com.asinosoft.vpn.dto.EConfigType
import com.asinosoft.vpn.dto.ServerConfig
import com.asinosoft.vpn.util.MessageUtil
import com.asinosoft.vpn.util.Utils
import com.asinosoft.vpn.util.V2rayConfigUtil
import com.asinosoft.vpn.util.fmt.ShadowsocksFmt
import com.asinosoft.vpn.util.toSpeedString
import com.v2ray.ang.util.fmt.SocksFmt
import com.v2ray.ang.util.fmt.TrojanFmt
import com.v2ray.ang.util.fmt.VlessFmt
import go.Seq
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import libv2ray.Libv2ray
import libv2ray.V2RayPoint
import libv2ray.V2RayVPNServiceSupportsSet
import java.lang.ref.SoftReference
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import kotlin.math.min

object ServiceManager {
    private const val NOTIFICATION_ID = 1
    private const val NOTIFICATION_PENDING_INTENT_CONTENT = 0
    private const val NOTIFICATION_PENDING_INTENT_STOP_V2RAY = 1
    private const val NOTIFICATION_ICON_THRESHOLD = 3000

    private val v2rayPoint: V2RayPoint =
        Libv2ray.newV2RayPoint(V2RayCallback(), Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
    private val mMsgReceive = ReceiveMessageHandler()

    var serviceControl: SoftReference<ServiceControl>? = null
        set(value) {
            field = value
            Seq.setContext(value?.get()?.getService()?.applicationContext)
            Libv2ray.initV2Env(
                Utils.userAssetPath(value?.get()?.getService()),
                Utils.getDeviceIdForXUDPBaseKey()
            )
        }
    private var configUri: Uri? = null
    private var currentConfig: ServerConfig? = null
    private var lastQueryTime = 0L
    private var lastZeroSpeed = false
    private var mNotificationManager: NotificationManager? = null
    private var measureSpeedTimer: Timer? = null

    private val channelId by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            // If earlier version channel ID is not used
            // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
            ""
        }
    }

    fun startV2Ray(context: Context, config: Uri) {
        if (v2rayPoint.isRunning) {
            Log.d(AppConfig.TAG, "VNP service already running")
            return
        }

        val intent = Intent(context.applicationContext, VpnService::class.java)
        intent.setData(config)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopV2Ray(context: Context) {
        MessageUtil.sendMsg2Service(context, AppConfig.MSG_STATE_STOP, "")
    }


    private class V2RayCallback : V2RayVPNServiceSupportsSet {
        override fun shutdown(): Long {
            val serviceControl = serviceControl?.get() ?: return -1
            // called by go
            return try {
                serviceControl.stopService()
                0
            } catch (e: Exception) {
                Log.d(AppConfig.PACKAGE, e.toString())
                -1
            }
        }

        override fun prepare(): Long {
            return 0
        }

        override fun protect(l: Long): Boolean {
            val serviceControl = serviceControl?.get() ?: return true
            return serviceControl.vpnProtect(l.toInt())
        }

        override fun onEmitStatus(l: Long, s: String?): Long {
            //Logger.d(s)
            return 0
        }

        override fun setup(s: String): Long {
            val serviceControl = serviceControl?.get() ?: return -1
            //Logger.d(s)
            return try {
                serviceControl.startService()
                startSpeedNotification()
                0
            } catch (e: Exception) {
                Log.d(AppConfig.PACKAGE, e.toString())
                -1
            }
        }
    }

    private fun parseConfig(uri: Uri): ServerConfig? =
        when (uri.scheme) {
            EConfigType.VLESS.protocolScheme -> VlessFmt.parseVless(uri)
            EConfigType.TROJAN.protocolScheme -> TrojanFmt.parseTrojan(uri)
            EConfigType.SOCKS.protocolScheme -> SocksFmt.parseSocks(uri.toString())
            EConfigType.SHADOWSOCKS.protocolScheme -> ShadowsocksFmt.parseShadowsocks(uri.toString())
            else -> null
        }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun startV2rayPoint(uri: Uri) {
        val service =
            serviceControl?.get()?.getService() ?: return trace("VPN service yet not started")

        if (v2rayPoint.isRunning) return trace("V2rayPoint already running")

        val config = parseConfig(uri) ?: return trace("Can't parse config: $uri")

        configUri = uri
        currentConfig = config

        try {
            val mFilter = IntentFilter(AppConfig.BROADCAST_ACTION_SERVICE)
            mFilter.addAction(Intent.ACTION_SCREEN_ON)
            mFilter.addAction(Intent.ACTION_SCREEN_OFF)
            mFilter.addAction(Intent.ACTION_USER_PRESENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                service.registerReceiver(mMsgReceive, mFilter, Context.RECEIVER_EXPORTED)
            } else {
                service.registerReceiver(mMsgReceive, mFilter)
            }
        } catch (e: Exception) {
            Log.w(AppConfig.PACKAGE, e.toString())
        }

        val outbound = config.outboundBean ?: return trace("Config doesn't define outbound")
        v2rayPoint.configureFileContent =
            V2rayConfigUtil.getV2rayConfig(service, outbound, config.remarks)
        v2rayPoint.domainName = config.getV2rayPointDomainAndPort()

        Log.d(AppConfig.TAG, "Connect to ${v2rayPoint.configureFileContent}")

        try {
            v2rayPoint.runLoop(false)
        } catch (e: Exception) {
            Log.w(AppConfig.PACKAGE, e.toString())
        }

        if (v2rayPoint.isRunning) {
            MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_SUCCESS, "")
            showNotification()
        } else {
            MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_START_FAILURE, "")
            cancelNotification()
        }
    }

    fun stopV2rayPoint() {
        val service = serviceControl?.get()?.getService() ?: return

        if (v2rayPoint.isRunning) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    v2rayPoint.stopLoop()
                } catch (e: Exception) {
                    Log.d(AppConfig.PACKAGE, e.toString())
                }
            }
        }

        MessageUtil.sendMsg2UI(service, AppConfig.MSG_STATE_STOP_SUCCESS, "")
        cancelNotification()

        try {
            service.unregisterReceiver(mMsgReceive)
        } catch (e: Exception) {
            Log.d(AppConfig.PACKAGE, e.toString())
        }
    }

    private class ReceiveMessageHandler : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val serviceControl = serviceControl?.get() ?: return
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_REGISTER_CLIENT -> {
                    if (v2rayPoint.isRunning) {
                        MessageUtil.sendMsg2UI(
                            serviceControl.getService(),
                            AppConfig.MSG_STATE_RUNNING,
                            "$configUri"
                        )
                    } else {
                        MessageUtil.sendMsg2UI(
                            serviceControl.getService(),
                            AppConfig.MSG_STATE_NOT_RUNNING,
                            ""
                        )
                    }
                }

                AppConfig.MSG_UNREGISTER_CLIENT -> {
                    // nothing to do
                }

                AppConfig.MSG_STATE_START -> {
                    // nothing to do
                }

                AppConfig.MSG_STATE_STOP -> {
                    serviceControl.stopService()
                }

                AppConfig.MSG_STATE_RESTART -> {
                    startV2rayPoint(intent.data!!)
                }

                AppConfig.MSG_MEASURE_DELAY -> {
                    measureV2rayDelay()
                }
            }
        }
    }

    private fun measureV2rayDelay() {
        CoroutineScope(Dispatchers.IO).launch {
            val service = serviceControl?.get()?.getService() ?: return@launch
            var time = -1L
            var error = ""
            if (v2rayPoint.isRunning) {
                try {
                    time = v2rayPoint.measureDelay(AppConfig.DELAY_TEST_URL)
                } catch (e: Exception) {
                    Log.d(AppConfig.PACKAGE, "measureV2rayDelay: $e")
                    error = e.message?.substringAfter("\":") ?: "empty message"
                }
                if (time == -1L) {
                    try {
                        time = v2rayPoint.measureDelay(AppConfig.DELAY_TEST_URL_2)
                    } catch (e: Exception) {
                        Log.d(AppConfig.PACKAGE, "measureV2rayDelay: $e")
                        error = e.message?.substringAfter("\":") ?: "empty message"
                    }
                }
            }
            val result = if (time == -1L) {
                service.getString(R.string.connection_test_error, error)
            } else {
                service.getString(R.string.connection_test_available, time)
            }

            MessageUtil.sendMsg2UI(service, AppConfig.MSG_MEASURE_DELAY_SUCCESS, result)
        }
    }

    private fun showNotification() {
        val service = serviceControl?.get()?.getService() ?: return

        service.startForeground(NOTIFICATION_ID, getNotification(service).build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = AppConfig.NOTIFICATION_CHANNEL_ID
        val channelName = AppConfig.NOTIFICATION_CHANNEL_NAME
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_HIGH
        )
        chan.lightColor = Color.DKGRAY
        chan.importance = NotificationManager.IMPORTANCE_NONE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getNotificationManager()?.createNotificationChannel(chan)
        return channelId
    }

    fun cancelNotification() {
        val service = serviceControl?.get()?.getService() ?: return
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } else {
            service.stopForeground(true)
        }
    }

    private fun getNotification(service: Service): NotificationCompat.Builder {
        val startMainIntent = Intent(service, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            service,
            NOTIFICATION_PENDING_INTENT_CONTENT, startMainIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val stopV2RayIntent = Intent(AppConfig.BROADCAST_ACTION_SERVICE)
        stopV2RayIntent.`package` = AppConfig.PACKAGE
        stopV2RayIntent.putExtra("key", AppConfig.MSG_STATE_STOP)

        val stopV2RayPendingIntent = PendingIntent.getBroadcast(
            service,
            NOTIFICATION_PENDING_INTENT_STOP_V2RAY, stopV2RayIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        return NotificationCompat.Builder(service, channelId)
            .setSmallIcon(R.drawable.connection)
            .setContentTitle(currentConfig?.remarks)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.delete,
                service.getString(R.string.stop),
                stopV2RayPendingIntent
            )
    }

    private fun updateNotification(contentText: String?, proxyTraffic: Long, directTraffic: Long) {
        val service = serviceControl?.get()?.getService() ?: return

        val notification = getNotification(service)

        if (proxyTraffic < NOTIFICATION_ICON_THRESHOLD && directTraffic < NOTIFICATION_ICON_THRESHOLD) {
            notification.setSmallIcon(R.drawable.connection)
        } else if (proxyTraffic > directTraffic) {
            notification.setSmallIcon(R.drawable.input)
        } else {
            notification.setSmallIcon(R.drawable.output)
        }
        notification.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        notification.setContentText(contentText) // Emui4.1 need content text even if style is set as BigTextStyle

        getNotificationManager()?.notify(NOTIFICATION_ID, notification.build())
    }

    private fun getNotificationManager(): NotificationManager? {
        if (mNotificationManager == null) {
            val service = serviceControl?.get()?.getService() ?: return null
            mNotificationManager =
                service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return mNotificationManager
    }

    private fun startSpeedNotification() {
        val outbounds = currentConfig?.getAllOutboundTags() ?: return

        val measureSpeed = object : TimerTask() {
            override fun run() = ping(outbounds)
        }

        measureSpeedTimer = Timer().apply {
            schedule(measureSpeed, 0L, TimeUnit.SECONDS.toMillis(5))
        }
    }

    private fun ping(outboundTags: List<String>) {
        val queryTime = System.currentTimeMillis()
        val sinceLastQueryInSeconds = (queryTime - lastQueryTime) / 1000.0
        var proxyTotal = 0L
        val text = StringBuilder()
        outboundTags.forEach {
            val up = v2rayPoint.queryStats(it, AppConfig.UPLINK)
            val down = v2rayPoint.queryStats(it, AppConfig.DOWNLINK)
            if (up + down > 0) {
                appendSpeedString(
                    text,
                    it,
                    up / sinceLastQueryInSeconds,
                    down / sinceLastQueryInSeconds
                )
                proxyTotal += up + down
            }
        }
        val directUplink = v2rayPoint.queryStats(AppConfig.TAG_DIRECT, AppConfig.UPLINK)
        val directDownlink = v2rayPoint.queryStats(AppConfig.TAG_DIRECT, AppConfig.DOWNLINK)
        val zeroSpeed = proxyTotal == 0L && directUplink == 0L && directDownlink == 0L
        if (!zeroSpeed || !lastZeroSpeed) {
            if (proxyTotal == 0L) {
                appendSpeedString(text, outboundTags.firstOrNull(), 0.0, 0.0)
            }
            appendSpeedString(
                text, AppConfig.TAG_DIRECT, directUplink / sinceLastQueryInSeconds,
                directDownlink / sinceLastQueryInSeconds
            )
            updateNotification(text.toString(), proxyTotal, directDownlink + directUplink)
        }
        lastZeroSpeed = zeroSpeed
        lastQueryTime = queryTime
    }

    private fun appendSpeedString(text: StringBuilder, name: String?, up: Double, down: Double) {
        var n = name ?: "no tag"
        n = n.substring(0, min(n.length, 6))
        text.append(n)
        for (i in n.length..6 step 2) {
            text.append("\t")
        }
        text.append("•  ${up.toLong().toSpeedString()}↑  ${down.toLong().toSpeedString()}↓\n")
    }

    private fun trace(message: String) {
        Log.d(AppConfig.TAG, message)
    }
}
