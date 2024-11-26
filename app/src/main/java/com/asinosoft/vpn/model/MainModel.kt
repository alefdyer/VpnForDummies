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
import com.asinosoft.vpn.StartActivity
import com.asinosoft.vpn.api.ServitorApi
import com.asinosoft.vpn.api.ServitorApiFactory
import com.asinosoft.vpn.dto.Config
import com.asinosoft.vpn.dto.ServiceState
import com.asinosoft.vpn.service.ServiceManager
import com.asinosoft.vpn.util.MessageUtil
import com.asinosoft.vpn.util.myDeviceId
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class MainModel(private val application: Application) : AndroidViewModel(application) {
    private lateinit var servitor: ServitorApi
    private var _config: Config? = null
    private val adsTimer = Timer()
    private var adsTimerTask: TimerTask? = null

    val config = MutableLiveData<Config>(null)
    val isRunning = MutableLiveData(false)
    val switchPosition = MutableLiveData(false)
    val testResult = MutableLiveData<String>()
    val message = MutableLiveData<String?>(null)
    val error = MutableLiveData<String?>(null)
    val timer = MutableLiveData<String?>(null)

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    fun setError(message: String?) {
        error.postValue(message)
    }

    fun startVpn() {
        Timber.i("Start VPN")
        Firebase.analytics.logEvent("vpn_start", Bundle.EMPTY)
        val config = _config ?: return

        if (config.breakForAdsInterval == 0L) {
            ServiceManager.startV2Ray(application, config)
        } else {
            val intent = Intent(application, StartActivity::class.java).apply {
                putExtra("config", Gson().toJson(config))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            application.startActivity(intent)
        }

        switchPosition.postValue(true)
        message.postValue(application.getString(R.string.starting))
        error.postValue(null)
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

    fun retrieveConfig(servitorUrl: String) {
        Timber.i("Fetch remote config from $servitorUrl")
        servitor = ServitorApiFactory().connect(servitorUrl)
        viewModelScope.launch { requestConfig(true) }
    }

    private suspend fun requestConfig(autoStart: Boolean = false) {
        val deviceId = application.myDeviceId
        Timber.w("Device ID: $deviceId")

        while (true) try {
            val servitorConfig = servitor.getConfig(deviceId)
            _config = servitorConfig
            Timber.w("$servitorConfig")
            config.postValue(servitorConfig)
            message.postValue(application.getString(R.string.ready_to_start))
            error.postValue(null)

            if (autoStart && null !== servitorConfig.subscription) {
                startVpn()
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

        val days = TimeUnit.MILLISECONDS.toDays(till - Date().time).toInt()
        if (days > 0) {
            // Just show remained days count without timer
            timer.postValue(application.resources.getQuantityString(R.plurals.days, days, days))
        } else {
            adsTimerTask = object : TimerTask() {
                override fun run() {
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(till - System.currentTimeMillis())
                        .coerceAtLeast(0)

                    if (0L == seconds) {
                        stopTimer()
                    } else {
                        val time = DateUtils.formatElapsedTime(seconds)
                        timer.postValue(time)
                    }
                }
            }

            adsTimer.schedule(adsTimerTask, 0L, TimeUnit.SECONDS.toMillis(1))
        }
    }

    private fun stopTimer() {
        adsTimerTask?.cancel()
        adsTimerTask = null
        timer.postValue(null)

        viewModelScope.launch { requestConfig() }
    }

    private fun onVpnRunning(serviceState: ServiceState) {
        _config = serviceState.config
        config.postValue(serviceState.config)
        startTimer(serviceState.adsTime)

        isRunning.postValue(true)
        switchPosition.postValue(true)
        message.postValue(application.getString(R.string.started))
        error.postValue(null)
    }

    private fun onVpnNotRunning() {
        isRunning.postValue(false)
        switchPosition.postValue(false)
        message.postValue(application.getString(R.string.stopped))
        error.postValue(null)
    }

    private fun onVpnStarted(serviceState: ServiceState) {
        _config = serviceState.config
        startTimer(serviceState.adsTime)

        config.postValue(serviceState.config)
        isRunning.postValue(true)
        switchPosition.postValue(true)
        message.postValue(application.getString(R.string.started))
        error.postValue(null)
        Toast.makeText(application, R.string.started, Toast.LENGTH_SHORT).show()
    }

    private fun onVpnStopped() {
        stopTimer()
        isRunning.postValue(false)
        switchPosition.postValue(false)
        message.postValue(application.getString(R.string.stopped))
        error.postValue(null)
        Toast.makeText(application, R.string.stopped, Toast.LENGTH_SHORT).show()
    }

    private fun onVpnFailed() {
        isRunning.postValue(false)
        switchPosition.postValue(false)
        message.postValue(null)
    }
}
