package com.asinosoft.vpn

import android.app.ComponentCaller
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
                model.retrieveConfig()
            }
        }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (RESULT_OK == it.resultCode) {
                it.data?.getConfig()?.let { config ->
                    onStartVpn(config)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.i("MainActivity::onCreate ${intent.extras?.keySet()?.joinToString(", ")}")
        super.onCreate(savedInstanceState)
        model.startListenBroadcast()
        model.isRunning.observe(this) { isRunning ->
            if (isRunning) {
                intent.removeExtra("config")
            }
        }

        setContent {
            VpnForDummiesTheme {
                MainView(
                    onStartVpn = this::onStartVpn,
                    onStopVpn = model::stopVpn,
                    onPremiumClicked = { subscribe.launch(null) },
                )
            }
        }
    }

    override fun onResume() {
        Timber.i("MainActivity::onResume ${intent.extras?.keySet()?.joinToString(", ")}")
        super.onResume()
        intent.getConfig()?.let { model.startVpn(it) }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        Timber.i("MainActivity::onNewIntent ${intent.extras?.keySet()?.joinToString(", ")}")
        intent.getConfig()?.let { model.startVpn(it) }
    }

    private fun onStartVpn(config: Config) {
        model.starting(config)
        val intent = VpnService.prepare(this)
        if (intent == null) {
            if (0L == config.breakForAdsInterval) {
                model.startVpn(config)
            } else {
                startActivity(Intent(this, StartActivity::class.java).putConfig(config))
            }
        } else {
            try {
                requestPermission.launch(intent.putConfig(config))
            } catch (e: ActivityNotFoundException) {
                model.stopped()
                model.setError(getString(R.string.device_not_supported))
            } catch (e: Throwable) {
                model.stopped()
                model.setError(e.message)
            }
        }
    }
}
