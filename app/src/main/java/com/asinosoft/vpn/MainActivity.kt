package com.asinosoft.vpn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.asinosoft.vpn.model.MainModel
import com.asinosoft.vpn.ui.MainView
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme

class MainActivity : ComponentActivity() {
    private val model: MainModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.startListenBroadcast()
        model.retrieveConfig(this)

        setContent {
            VpnForDummiesTheme {
                MainView { startSubscription() }
            }
        }
    }

    private fun startSubscription() {
        val intent = Intent(this, SubscriptionActivity::class.java)
        startActivity(intent)
    }
}
