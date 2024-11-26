package com.asinosoft.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.asinosoft.vpn.dto.SubscriptionResultContract
import com.asinosoft.vpn.model.MainModel
import com.asinosoft.vpn.ui.MainView
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private val model: MainModel by viewModels()

    private val subscribe = registerForActivityResult(SubscriptionResultContract()) {
        val servitorUrl = Firebase.remoteConfig.getString(AppConfig.PREF_SERVITOR_URL)
        model.retrieveConfig(servitorUrl)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.startListenBroadcast()

        Firebase.remoteConfig.fetchAndActivate().addOnCompleteListener {
            Timber.d("Config fetched")
            val servitorUrl = Firebase.remoteConfig.getString(AppConfig.PREF_SERVITOR_URL)
            model.retrieveConfig(servitorUrl)
        }.addOnFailureListener { e ->
            Timber.e("Couldn't fetch remote config: $e")
        }

        setContent {
            VpnForDummiesTheme {
                MainView { subscribe.launch(null) }
            }
        }
    }
}
