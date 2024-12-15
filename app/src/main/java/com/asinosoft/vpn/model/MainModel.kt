package com.asinosoft.vpn.model

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.R
import com.asinosoft.vpn.api.ServitorApi
import com.asinosoft.vpn.api.ServitorApiFactory
import com.asinosoft.vpn.dto.Config
import com.asinosoft.vpn.dto.ServiceState
import com.asinosoft.vpn.service.ServiceManager
import com.asinosoft.vpn.util.MessageUtil
import com.asinosoft.vpn.util.myDeviceId
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class MainModel(private val application: Application) : AndroidViewModel(application) {
    private var servitor: ServitorApi? = null
    private val adsTimer = Timer()
    private var adsTimerTask: TimerTask? = null

    private var autoRestart = false

    val config = MutableLiveData<Config>(null)
    val isRunning = MutableLiveData(false)
    val switchPosition = MutableLiveData(false)
    val testResult = MutableLiveData<String>()
    val message = MutableLiveData<String?>(null)
    val error = MutableLiveData<String?>(null)
    val timer = MutableLiveData<String?>(null)

    init {
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener {
            val url = Firebase.remoteConfig.getString(AppConfig.PREF_SERVITOR_URL)
            servitor = ServitorApiFactory().connect(url)
            viewModelScope.launch(Dispatchers.IO) { requestConfig(true) }
        }.addOnFailureListener { e ->
            setError(application.getString(R.string.config_error, e))
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    fun setError(message: String?) {
        error.postValue(message)
    }

    fun starting(cfg: Config) {
        config.postValue(cfg)
        switchPosition.postValue(true)
        message.postValue(application.getString(R.string.starting))
        error.postValue(null)
    }

    fun stopped() {
        switchPosition.postValue(false)
        message.postValue(application.getString(R.string.stopped))
        error.postValue(null)
    }

    fun startVpn(config: Config) {
        Timber.i("Start VPN")
        Firebase.analytics.logEvent("vpn_start", Bundle.EMPTY)

        starting(config)
        ServiceManager.startV2Ray(application, config)
    }

    fun stopVpn() {
        Timber.i("Stop VPN")
        Firebase.analytics.logEvent("vpn_stop", Bundle.EMPTY)

        stopTimer()
        ServiceManager.stopV2Ray(application)
        switchPosition.postValue(false)
        message.postValue(
            if (true == isRunning.value)
                application.getString(R.string.stopping)
            else
                application.getString(R.string.ready_to_start)
        )
        error.postValue(null)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun startListenBroadcast() {
        val broadcast = IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(mMsgReceiver, broadcast, Context.RECEIVER_EXPORTED)
        } else {
            application.registerReceiver(mMsgReceiver, broadcast)
        }

        MessageUtil.sendMsg2Service(application, AppConfig.MSG_REGISTER_CLIENT)
    }

    fun autoStart() {
        Timber.d("MainModel::autoStart")
        if (true == isRunning.value) {
            autoRestart = true
            stopVpn()
        } else {
            viewModelScope.launch(Dispatchers.IO) { requestConfig(true) }
        }
    }

    private suspend fun requestConfig(autoStartPremium: Boolean) {
        val deviceId = application.myDeviceId
        Timber.w("Device ID: $deviceId")

        while (true) try {
            val servitorConfig = servitor?.getConfig(deviceId)
                ?: return setError(application.getString(R.string.config_error, "Server not found"))

            if (true == isRunning.value) return

            Timber.w("$servitorConfig")
            config.postValue(servitorConfig)
            message.postValue(application.getString(R.string.ready_to_start))
            error.postValue(null)

            if (autoStartPremium) {
                servitorConfig.subscription?.run { startVpn(servitorConfig) }
            }

            break
        } catch (e: Exception) {
            Timber.e("VPN config error: ${e.message}")
            setError(application.getString(R.string.config_error, e.message))
            delay(AppConfig.RETRY_DELAY_MS)
        }
    }

    private val mMsgReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_STATE_RUNNING ->
                    onVpnRunning(ServiceState.fromJson(intent.getStringExtra("content")))

                AppConfig.MSG_STATE_NOT_RUNNING -> onVpnNotRunning()

                AppConfig.MSG_STATE_START_SUCCESS ->
                    onVpnStarted(ServiceState.fromJson(intent.getStringExtra("content")))

                AppConfig.MSG_STATE_START_FAILURE -> onVpnFailed()

                AppConfig.MSG_STATE_STOP -> {
                    stopTimer()
                    switchPosition.postValue(false)
                    message.postValue(
                        if (true == isRunning.value)
                            application.getString(R.string.stopping)
                        else
                            application.getString(R.string.ready_to_start)
                    )
                    error.postValue(null)
                }

                AppConfig.MSG_STATE_STOP_SUCCESS -> onVpnStopped()

                AppConfig.MSG_MEASURE_DELAY_SUCCESS -> {
                    intent.getStringExtra("content")?.let {
                        testResult.postValue(it)
                    }
                }

                AppConfig.MSG_ERROR_MESSAGE -> {
                    intent.getStringExtra("content")?.let {
                        setError(it.ifEmpty { null })
                    }
                }
            }
        }
    }

    private fun startTimer(till: Long) {
        adsTimerTask?.cancel()
        adsTimerTask = null

        adsTimerTask = object : TimerTask() {
            override fun run() {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(till - System.currentTimeMillis())

                if (0L >= seconds) {
                    return stopTimer()
                }

                val days = TimeUnit.SECONDS.toDays(seconds).toInt()
                val time =
                    if (days > 100) {
                        application.resources.getQuantityString(R.plurals.days, days, days)
                    } else {
                        DateUtils.formatElapsedTime(seconds)
                    }

                timer.postValue(time)
            }
        }

        adsTimer.schedule(adsTimerTask, 0L, TimeUnit.SECONDS.toMillis(1))
    }

    private fun stopTimer() {
        Timber.d("MainModel::stopTimer")
        adsTimerTask?.cancel()
        adsTimerTask = null
        timer.postValue(null)
    }

    private fun onVpnRunning(serviceState: ServiceState) {
        Timber.d("MainModel::onVpnRunning")
        config.postValue(serviceState.config)
        startTimer(serviceState.adsTime)

        isRunning.postValue(true)
        switchPosition.postValue(true)
        message.postValue(application.getString(R.string.started))
        error.postValue(null)
    }

    private fun onVpnNotRunning() {
        Timber.d("MainModel::onVpnNotRunning")
        isRunning.postValue(false)
        switchPosition.postValue(false)
        message.postValue(application.getString(R.string.stopped))
        error.postValue(null)
    }

    private fun onVpnStarted(serviceState: ServiceState) {
        Timber.d("MainModel::onVpnStarted")
        startTimer(serviceState.adsTime)

        config.postValue(serviceState.config)
        isRunning.postValue(true)
        switchPosition.postValue(true)
        message.postValue(application.getString(R.string.started))
        error.postValue(null)
        Toast.makeText(application, R.string.started, Toast.LENGTH_SHORT).show()
    }

    private fun onVpnStopped() {
        Timber.d("MainModel::onVpnStopped")
        stopTimer()

        isRunning.postValue(false)
        switchPosition.postValue(false)
        message.postValue(application.getString(R.string.stopped))
        error.postValue(null)
        Toast.makeText(application, R.string.stopped, Toast.LENGTH_SHORT).show()

        viewModelScope.launch(Dispatchers.IO) {
            requestConfig(autoRestart)
            autoRestart = false
        }
    }

    private fun onVpnFailed() {
        Timber.d("MainModel::onVpnFailed")
        isRunning.postValue(false)
        switchPosition.postValue(false)
        message.postValue(null)
    }
}
