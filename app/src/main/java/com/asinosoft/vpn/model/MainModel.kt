package com.asinosoft.vpn.model

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.R
import com.asinosoft.vpn.StartActivity
import com.asinosoft.vpn.service.ServiceManager
import com.asinosoft.vpn.util.MessageUtil
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig

class MainModel(private val application: Application) : AndroidViewModel(application) {
    private val remoteConfig = Firebase.remoteConfig
    val config = MutableLiveData<Uri>()
    val connectionName = MutableLiveData<String>()
    val isReady = MutableLiveData(false)
    val isRunning = MutableLiveData(false)
    val switchPosition = MutableLiveData(false)
    val testResult = MutableLiveData<String>()
    val message = MutableLiveData<String?>(null)
    val error = MutableLiveData<String?>(null)

    init {
        MessageUtil.sendMsg2Service(application, AppConfig.MSG_REGISTER_CLIENT, "")
    }

    fun setError(message: String?) {
        error.postValue(message)
    }

    fun startVpn() {
        val intent = Intent(application, StartActivity::class.java).apply {
            data = config.value!!
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        application.startActivity(intent)
        switchPosition.postValue(true)
        message.postValue(application.getString(R.string.starting))
        error.postValue(null)
    }

    fun reallyStartVpn(config: Uri) {
        Log.i(AppConfig.TAG, "startVpn: $config")
        ServiceManager.startV2Ray(application, config)
    }

    fun stopVpn() {
        Log.i(AppConfig.TAG, "stopVpn")
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(
                mMsgReceiver,
                IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY),
                Context.RECEIVER_EXPORTED
            )
        } else {
            application.registerReceiver(
                mMsgReceiver,
                IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY)
            )
        }

        MessageUtil.sendMsg2Service(application, AppConfig.MSG_REGISTER_CLIENT, "")
    }

    fun retrieveConfig(activity: Activity) {
        remoteConfig.fetchAndActivate().addOnCompleteListener(activity) { task ->
            if (task.isSuccessful) {
                val uri = getRandomConfig()
                if (!config.isInitialized) {
                    config.postValue(uri)
                    connectionName.postValue(uri.fragment)
                    message.postValue(application.getString(R.string.ready_to_start))
                    isReady.postValue(true)
                    error.postValue(null)
                }
            } else {
                setError(application.getString(R.string.config_error, task.exception))
            }
        }
    }

    private fun getRandomConfig(): Uri {
        val keys = remoteConfig.getKeysByPrefix(AppConfig.PREF_CONNECTION_PREFIX)
        Log.d(AppConfig.TAG, "Config complete: $keys")

        val connection: String = remoteConfig.all.filter { entry ->
            entry.key.startsWith(AppConfig.PREF_CONNECTION_PREFIX)
        }.values.random().asString()

        val config = Uri.parse(connection)
        Log.d(AppConfig.TAG, "Config: $config")

        return config
    }

    private val mMsgReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_STATE_RUNNING -> {
                    val currentConfig = Uri.parse(intent.getStringExtra("content"))
                    isRunning.postValue(true)
                    config.postValue(currentConfig)
                    connectionName.postValue(currentConfig.fragment)
                    switchPosition.postValue(true)
                    isReady.postValue(true)
                    message.postValue(application.getString(R.string.started))
                    error.postValue(null)
                }

                AppConfig.MSG_STATE_NOT_RUNNING -> {
                    isRunning.postValue(false)
                    switchPosition.postValue(false)
                    message.postValue(application.getString(R.string.stopped))
                    error.postValue(null)
                }

                AppConfig.MSG_STATE_START_SUCCESS -> {
                    isRunning.postValue(true)
                    switchPosition.postValue(true)
                    Toast.makeText(application, R.string.started, Toast.LENGTH_SHORT).show()
                    message.postValue(application.getString(R.string.started))
                    error.postValue(null)
                }

                AppConfig.MSG_STATE_START_FAILURE -> {
                    isRunning.postValue(false)
                    switchPosition.postValue(false)
                    message.postValue(null)
                }

                AppConfig.MSG_STATE_STOP_SUCCESS -> {
                    isRunning.postValue(false)
                    switchPosition.postValue(false)
                    message.postValue(application.getString(R.string.stopped))
                    error.postValue(null)
                    Toast.makeText(application, R.string.stopped, Toast.LENGTH_SHORT).show()
                }

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
}
