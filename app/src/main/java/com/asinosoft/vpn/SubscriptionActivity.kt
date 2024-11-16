package com.asinosoft.vpn

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.asinosoft.vpn.dto.Subscription
import com.asinosoft.vpn.ui.components.SubscriptionMenu

class SubscriptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SubscriptionMenu { onPaymentSelected(it) }
        }
    }

    private fun onPaymentSelected(period: Subscription.Period) {
        Toast.makeText(this, "Payment selected: $period", Toast.LENGTH_SHORT).show()
    }
}