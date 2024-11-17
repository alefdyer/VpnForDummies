package com.asinosoft.vpn

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.asinosoft.vpn.dto.Order
import com.asinosoft.vpn.dto.Payment
import com.asinosoft.vpn.dto.Subscription
import com.asinosoft.vpn.model.SubscriptionModel
import com.asinosoft.vpn.ui.components.OrderInfo
import com.asinosoft.vpn.ui.components.PaymentInfo
import com.asinosoft.vpn.ui.components.SubscriptionMenu

class SubscriptionActivity : AppCompatActivity() {
    private val model: SubscriptionModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.order.observe(this, this::pay)
        model.payment.observe(this, this::confirm)

        setContent {
            val order by model.order.observeAsState(null)
            val payment by model.payment.observeAsState(null)
            val height = Modifier.height(LocalConfiguration.current.screenHeightDp.div(3).dp)

            Column {
                order?.let { OrderInfo(it, height) }

                payment?.let { PaymentInfo(it, height) }

                if (null == order && null == payment) {
                    SubscriptionMenu { period ->
                        model.createOrder(this@SubscriptionActivity, Subscription(period))
                    }
                }
            }
        }
    }

    private fun pay(order: Order) {
        model.createPayment(order)
    }

    private fun confirm(payment: Payment) {
        if (payment.status.isFinal()) {
            finish()
        }
    }
}
