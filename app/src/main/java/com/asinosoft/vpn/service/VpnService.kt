package com.asinosoft.vpn.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.os.StrictMode
import androidx.annotation.RequiresApi
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.dto.Config
import com.asinosoft.vpn.dto.ERoutingMode
import com.asinosoft.vpn.dto.ServiceState
import com.asinosoft.vpn.dto.getConfig
import com.asinosoft.vpn.util.MessageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.lang.ref.SoftReference
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import android.net.VpnService as AndroidVpnService

class VpnService : AndroidVpnService(), ServiceControl {
    companion object {
        private const val VPN_MTU = 1500
        private const val PRIVATE_VLAN4_CLIENT = "26.26.26.1"
        private const val PRIVATE_VLAN4_ROUTER = "26.26.26.2"
        private const val PRIVATE_VLAN6_CLIENT = "da26:2626::1"
        private const val PRIVATE_VLAN6_ROUTER = "da26:2626::2"
    }

    private lateinit var config: Config

    private var isRunning = false
    private lateinit var mInterface: ParcelFileDescriptor
    private lateinit var process: Process

    private var breakForAdsTimer: Timer? = null
    private val breakForAds = object : TimerTask() {
        override fun run() {
            Timber.w("Break for Ads")
            stopService()
        }
    }

    /**destroy
     * Unfortunately registerDefaultNetworkCallback is going to return our VPN interface: https://android.googlesource.com/platform/frameworks/base/+/dda156ab0c5d66ad82bdcf76cda07cbc0a9c8a2e
     *
     * This makes doing a requestNetwork with REQUEST necessary so that we don't get ALL possible networks that
     * satisfies default network capabilities but only THE default network. Unfortunately we need to have
     * android.permission.CHANGE_NETWORK_STATE to be able to call requestNetwork.
     *
     * Source: https://android.googlesource.com/platform/frameworks/base/+/2df4c7d/services/core/java/com/android/server/ConnectivityService.java#887
     */
    @delegate:RequiresApi(Build.VERSION_CODES.P)
    private val defaultNetworkRequest by lazy {
        NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED).build()
    }

    private val connectivity by lazy { getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    @delegate:RequiresApi(Build.VERSION_CODES.P)
    private val defaultNetworkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                setUnderlyingNetworks(arrayOf(network))
            }

            override fun onCapabilitiesChanged(
                network: Network, networkCapabilities: NetworkCapabilities
            ) {
                // it's a good idea to refresh capabilities
                setUnderlyingNetworks(arrayOf(network))
            }

            override fun onLost(network: Network) {
                setUnderlyingNetworks(null)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        ServiceManager.serviceControl = SoftReference(this)
    }

    override fun onRevoke() {
        stopV2Ray()
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceManager.cancelNotification()
    }

    private fun setup() {
        val prepare = prepare(this)
        if (prepare != null) {
            return
        }

        // If the old interface has exactly the same parameters, use it!
        // Configure a builder while parsing the parameters.
        val builder = Builder()

        val routingMode = ERoutingMode.BYPASS_LAN_MAINLAND.value

        builder.setMtu(VPN_MTU)
        builder.addAddress(PRIVATE_VLAN4_CLIENT, 30)
        if (routingMode == ERoutingMode.BYPASS_LAN.value || routingMode == ERoutingMode.BYPASS_LAN_MAINLAND.value) {
            AppConfig.BYPASS_IP_ADDRESSES.forEach { ip ->
                builder.addRoute(ip.address, ip.length)
            }
        } else {
            builder.addRoute("0.0.0.0", 0)
        }

        if (AppConfig.PREFER_IPV6) {
            builder.addAddress(PRIVATE_VLAN6_CLIENT, 126)
            if (routingMode == ERoutingMode.BYPASS_LAN.value || routingMode == ERoutingMode.BYPASS_LAN_MAINLAND.value) {
                builder.addRoute("2000::", 3) //currently only 1/8 of total ipV6 is in use
            } else {
                builder.addRoute("::", 0)
            }
        }

        builder.addDnsServer(AppConfig.DNS_DIRECT)
        builder.addDnsServer(AppConfig.DNS_PROXY)

        builder.setSession(AppConfig.SESSION_NAME)

        AppConfig.ALLOWED_APPS.forEach {
            builder.addAllowedApplication(it)
        }

        // Close the old interface since the parameters have been changed.
        try {
            mInterface.close()
        } catch (ignored: Exception) {
            // ignored
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                connectivity.requestNetwork(defaultNetworkRequest, defaultNetworkCallback)
            } catch (e: Exception) {
                MessageUtil.sendMsg2UI(
                    this,
                    AppConfig.MSG_ERROR_MESSAGE,
                    "Request network error: ${e.message}"
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        // Create a new interface using the builder and save the parameters.
        try {
            mInterface = builder.establish()!!
            isRunning = true
            runTun2socks()
            scheduleBreakForAds()
            MessageUtil.sendMsg2UI(
                this,
                AppConfig.MSG_STATE_START_SUCCESS,
                getState().toJson()
            )
        } catch (e: Exception) {
            MessageUtil.sendMsg2UI(
                this,
                AppConfig.MSG_ERROR_MESSAGE,
                "Failed to setup: ${e.message}"
            )
            stopV2Ray()
        }
    }

    private fun runTun2socks() {
        val tun2socks = File(
            applicationContext.applicationInfo.nativeLibraryDir,
            System.mapLibraryName("tun2socks")
        )

        if (!tun2socks.canExecute()) {
            throw Exception("$tun2socks is not executable!")
        }

        val cmd: MutableList<String> = arrayListOf(
            tun2socks.absolutePath,
            "--netif-ipaddr", PRIVATE_VLAN4_ROUTER,
            "--netif-netmask", "255.255.255.252",
            "--socks-server-addr", "127.0.0.1:${AppConfig.SOCKS_PORT}",
            "--tunmtu", VPN_MTU.toString(),
            "--sock-path", "sock_path",
            "--enable-udprelay",
            "--loglevel", "notice"
        )

        if (AppConfig.PREFER_IPV6) {
            cmd.add("--netif-ip6addr")
            cmd.add(PRIVATE_VLAN6_ROUTER)
        }

        Timber.d(cmd.toString())

        try {
            process =
                ProcessBuilder(cmd).redirectErrorStream(true).directory(applicationContext.filesDir)
                    .start()

            thread {
                Timber.d("tun2socks check")
                val result = process.waitFor()
                Timber.d("tun2socks exited with $result")

                if (isRunning) {
                    Timber.d("tun2socks restart")
                    Handler(mainLooper).postDelayed({
                        runTun2socks()
                    }, 100)
                }
            }

            sendFd()
        } catch (e: Exception) {
            Timber.w(e.toString())
            MessageUtil.sendMsg2UI(
                this,
                AppConfig.MSG_ERROR_MESSAGE,
                "Failed to start tun2socks: ${e.message}"
            )
        }
    }

    private fun sendFd() {
        val fd = mInterface.fileDescriptor
        val path = File(applicationContext.filesDir, "sock_path").absolutePath
        Timber.d(path)

        CoroutineScope(Dispatchers.IO).launch {
            var tries = 0
            while (true) try {
                Thread.sleep(50L shl tries)
                Timber.d("sendFd tries: $tries")
                LocalSocket().use { localSocket ->
                    localSocket.connect(
                        LocalSocketAddress(
                            path,
                            LocalSocketAddress.Namespace.FILESYSTEM
                        )
                    )
                    localSocket.setFileDescriptorsForSend(arrayOf(fd))
                    localSocket.outputStream.write(42)
                }
                break
            } catch (e: Exception) {
                if (tries > 5) {
                    MessageUtil.sendMsg2UI(
                        this@VpnService,
                        AppConfig.MSG_ERROR_MESSAGE,
                        "Failed to sendFd: ${e.message}"
                    )
                }
                tries += 1
            }
        }
    }

    private fun scheduleBreakForAds() {
        val adsTime: Long = config.subscription?.endAt?.time?.let { it - Date().time }
            ?: TimeUnit.MINUTES.toMillis(config.breakForAdsInterval)

        breakForAdsTimer = Timer().apply { schedule(breakForAds, adsTime) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        config = intent?.getConfig() ?: throw Exception("No config")
        ServiceManager.startV2rayPoint(Uri.parse(config.url))
        return START_STICKY
    }

    private fun stopV2Ray(isForced: Boolean = true) {
        isRunning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                connectivity.unregisterNetworkCallback(defaultNetworkCallback)
            } catch (ignored: Exception) {
                // ignored
            }
        }

        try {
            Timber.d("tun2socks destroy")
            process.destroy()
        } catch (e: Exception) {
            Timber.d(e.toString())
        }

        ServiceManager.stopV2rayPoint()

        breakForAdsTimer?.cancel()

        if (isForced) {
            //stopSelf has to be called ahead of mInterface.close(). otherwise v2ray core cannot be stooped
            //It's strage but true.
            //This can be verified by putting stopself() behind and call stopLoop and startLoop
            //in a row for several times. You will find that later created v2ray core report port in use
            //which means the first v2ray core somehow failed to stop and release the port.
            stopSelf()

            try {
                mInterface.close()
            } catch (ignored: Exception) {
                // ignored
            }
        }
    }

    override fun getService(): Service {
        return this
    }

    override fun startService() {
        setup()
    }

    override fun stopService() {
        stopV2Ray(true)
    }

    override fun vpnProtect(socket: Int): Boolean {
        return protect(socket)
    }

    override fun getState(): ServiceState =
        ServiceState(config, breakForAds.scheduledExecutionTime())
}
