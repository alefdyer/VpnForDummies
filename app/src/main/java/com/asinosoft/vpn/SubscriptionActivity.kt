package com.asinosoft.vpn

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.asinosoft.vpn.dto.Order
import com.asinosoft.vpn.dto.Payment
import com.asinosoft.vpn.dto.Subscription
import com.asinosoft.vpn.model.SubscriptionModel
import com.asinosoft.vpn.ui.SubscriptionView

class SubscriptionActivity : AppCompatActivity() {
    private val model: SubscriptionModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.order.observe(this, this::pay)
        model.payment.observe(this, this::confirm)

        setContent {
            val order by model.order.observeAsState(null)
            val payment by model.payment.observeAsState(null)
            val qrcode by model.qrcode.observeAsState(null)

            SubscriptionView(order, payment, qrcode) { period ->
                model.createOrder(Subscription(period))
            }
        }
    }

    private fun pay(order: Order) {
        model.createPayment(order)
    }

    private fun confirm(payment: Payment) {
        if (payment.status.isComplete()) {
            finish()
        }
    }
}
