package com.asinosoft.vpn

import android.content.ActivityNotFoundException
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.asinosoft.vpn.dto.AdvertisementResultContract
import com.asinosoft.vpn.dto.SubscriptionResultContract
import com.asinosoft.vpn.model.MainModel
import com.asinosoft.vpn.ui.MainView
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private val model: MainModel by viewModels()

    private val subscribe =
        registerForActivityResult(SubscriptionResultContract()) {
            if (it) {
                val servitorUrl = Firebase.remoteConfig.getString(AppConfig.PREF_SERVITOR_URL)
                model.retrieveConfig(servitorUrl)
            }
        }

    private val advertise =
        registerForActivityResult(AdvertisementResultContract()) {
            if (it) {
                model.startVpn()
            }
        }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (RESULT_OK == it.resultCode) {
                onStartVpn()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.startListenBroadcast()

        getConfig()

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

    private fun getConfig() {
        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener {
            Timber.d("Config fetched")
            val servitorUrl = Firebase.remoteConfig.getString(AppConfig.PREF_SERVITOR_URL)
            model.retrieveConfig(servitorUrl)
        }.addOnFailureListener { e ->
            Timber.e("Couldn't fetch remote config: $e")
        }
    }

    private fun onStartVpn() {
        model.starting()
        val intent = VpnService.prepare(this)
        if (intent == null) {
            if (0L == model.config.value?.breakForAdsInterval) {
                model.startVpn()
            } else {
                advertise.launch(null)
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
}
