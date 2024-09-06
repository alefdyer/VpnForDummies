package com.asinosoft.vpn.model

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.R
import com.asinosoft.vpn.service.ServiceManager
import com.asinosoft.vpn.util.MessageUtil

class MainModel(private val application: Application) : AndroidViewModel(application) {
    val config = MutableLiveData<Uri>()
    val connectionName = MutableLiveData<String>()
    val isReady = MutableLiveData(false)
    val isRunning = MutableLiveData(false)
    val switchPosition = MutableLiveData(false)
    val testResult = MutableLiveData<String>()

    init {
        MessageUtil.sendMsg2Service(application, AppConfig.MSG_REGISTER_CLIENT, "")
    }

    fun setConfig(uri: Uri) {
        config.postValue(uri)
        connectionName.postValue(uri.fragment)
        isReady.postValue(true)
    }

    fun startVpn() {
        ServiceManager.startV2Ray(application, config.value!!)
        switchPosition.postValue(true)
    }

    fun stopVpn() {
        ServiceManager.stopV2Ray(application)
        switchPosition.postValue(false)
    }

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

    private val mMsgReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_STATE_RUNNING -> {
                    isRunning.postValue(true)
                    switchPosition.postValue(true)
                }

                AppConfig.MSG_STATE_NOT_RUNNING -> {
                    isRunning.postValue(false)
                    switchPosition.postValue(false)
                }

                AppConfig.MSG_STATE_START_SUCCESS -> {
                    isRunning.postValue(true)
                    switchPosition.postValue(true)
                    Toast.makeText(application, R.string.started, Toast.LENGTH_SHORT).show()
                }

                AppConfig.MSG_STATE_START_FAILURE -> {
                    isRunning.postValue(false)
                    switchPosition.postValue(false)
                }

                AppConfig.MSG_STATE_STOP_SUCCESS -> {
                    isRunning.postValue(false)
                    switchPosition.postValue(false)
                    Toast.makeText(application, R.string.stopped, Toast.LENGTH_SHORT).show()
                }

                AppConfig.MSG_MEASURE_DELAY_SUCCESS -> {
                    intent.getStringExtra("content")?.let {
                        testResult.postValue(it)
                    }
                }
            }
        }
    }
}
