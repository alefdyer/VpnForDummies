package com.asinosoft.vpn

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asinosoft.vpn.model.SubscriptionModel
import com.asinosoft.vpn.ui.SubscriptionView
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme
import timber.log.Timber

class SubscriptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VpnForDummiesTheme {
                val model: SubscriptionModel = viewModel()
                val state = model.state.collectAsState()

                SubscriptionView(
                    state = state.value,
                    onCreateOrder = model::createOrder,
                    onCreatePayment = model::createPayment,
                    onClose = this::close
                )
            }
        }
    }

    private fun close(succeed: Boolean) {
        Timber.d("SubscriptionActivity result = $succeed")

        val result = if (succeed) Activity.RESULT_OK else Activity.RESULT_CANCELED
        setResult(result)
        finish()
    }
}
