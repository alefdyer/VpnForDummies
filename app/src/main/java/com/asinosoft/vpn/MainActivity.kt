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
        super.onCreate(savedInstanceState)
        model.startListenBroadcast()

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
        super.onResume()

        intent.getConfig()
            ?.let { model.startVpn(it) }
            ?: model.retrieveConfig()
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        setIntent(intent)
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
