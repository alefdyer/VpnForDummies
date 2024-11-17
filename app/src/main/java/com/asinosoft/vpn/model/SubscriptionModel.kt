package com.asinosoft.vpn.model

import android.content.Context
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.api.CreateOrderRequest
import com.asinosoft.vpn.api.ServitorApiFactory
import com.asinosoft.vpn.dto.Order
import com.asinosoft.vpn.dto.Payment
import com.asinosoft.vpn.dto.Subscription
import com.asinosoft.vpn.util.myDeviceId
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class SubscriptionModel : ViewModel() {
    private val _order = MutableLiveData<Order>()
    private val _payment = MutableLiveData<Payment>()

    val order: LiveData<Order>
        get() = _order

    val payment: LiveData<Payment>
        get() = _payment

    private val servitor by lazy {
        ServitorApiFactory().connect(Firebase.remoteConfig.getString(AppConfig.PREF_SERVITOR_URL))
    }

    fun createOrder(context: Context, subscription: Subscription) {
        viewModelScope.launch {
            val order = servitor.createOrder(
                CreateOrderRequest(
                    context.myDeviceId,
                    "${Build.MANUFACTURER} ${Build.MODEL}",
                    "subscription",
                    subscription.period.toString().lowercase(),
                )
            )

            _order.postValue(order)
        }
    }

    fun createPayment(order: Order) {
        viewModelScope.launch {
            val payment = servitor.createPayment(order.id)
            Timber.d("Payment = $payment")

            _payment.postValue(payment)

            checkPaymentForever(payment)
        }
    }

    fun checkPayment(payment: Payment) {
        viewModelScope.launch {
            checkPaymentForever(payment)
        }
    }

    private suspend fun checkPaymentForever(payment: Payment) {
        var p = payment
        while (!p.status.isFinal()) {
            delay(1000)

            p = servitor.checkPayment(payment.id)
            _payment.postValue(p)
            Timber.d("Status = ${p.status}")
        }
    }
}
