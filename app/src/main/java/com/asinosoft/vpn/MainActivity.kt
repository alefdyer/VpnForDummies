package com.asinosoft.vpn

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.asinosoft.vpn.dto.Config
import com.asinosoft.vpn.dto.SubscriptionResultContract
import com.asinosoft.vpn.dto.getConfig
import com.asinosoft.vpn.dto.putConfig
import com.asinosoft.vpn.model.MainModel
import com.asinosoft.vpn.ui.MainView
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private val model: MainModel by viewModels()

    private val subscribe =
        registerForActivityResult(SubscriptionResultContract()) {
            if (it) {
                model.autoStart()
            }
        }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (RESULT_OK == it.resultCode) {
                model.config.value?.let {
                    onStartVpn(it)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.i("MainActivity::onCreate")
        super.onCreate(savedInstanceState)
        model.startListenBroadcast()

        setContent {
            VpnForDummiesTheme {
                MainView(
                    onStartVpn = this::onStartVpn,
                    onStopVpn = model::stopVpn,
                    onPremiumClicked = { subscribe.launch(null) },
                    onRestoreSubscription = this::restoreSubscription
                )
            }
        }
    }

    override fun onResume() {
        Timber.i("MainActivity::onResume")
        super.onResume()

        getConfig()?.let { model.startVpn(it) }
    }

    private fun onStartVpn(config: Config) {
        model.starting(config)
        val intent = VpnService.prepare(this)
        if (intent == null) {
            if (0L == config.breakForAdsInterval) {
                model.startVpn(config)
            } else {
                startActivity(
                    Intent(this, StartActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
                        .putConfig(config)
                )
            }
        } else {
            try {
                requestPermission.launch(intent)
            } catch (e: ActivityNotFoundException) {
                model.stopped()
                model.setError(getString(R.string.device_not_supported))
            } catch (e: Throwable) {
                model.stopped()
                model.setError(e.message)
            }
        }
    }

    private fun restoreSubscription() {
        startActivity(Intent(this, RestoreSubscriptionActivity::class.java))
    }
}
