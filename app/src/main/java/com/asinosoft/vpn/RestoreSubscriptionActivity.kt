package com.asinosoft.vpn

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.asinosoft.vpn.model.RestoreSubscriptionModel
import com.asinosoft.vpn.ui.RestoreSubscriptionView
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme

class RestoreSubscriptionActivity : AppCompatActivity() {
    private val model: RestoreSubscriptionModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by model.state.collectAsState()

            VpnForDummiesTheme {
                RestoreSubscriptionView(
                    state = state,
                    onRestoreSubscription = model::restore,
                    onClose = { finish() }
                )
            }
        }
    }
}
