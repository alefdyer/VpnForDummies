package com.asinosoft.vpn.model

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.asinosoft.vpn.App
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.api.CreateOrderRequest
import com.asinosoft.vpn.api.ServitorApiFactory
import com.asinosoft.vpn.dto.Order
import com.asinosoft.vpn.dto.Payment
import com.asinosoft.vpn.dto.Subscription
import com.asinosoft.vpn.util.myDeviceId
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import qrcode.QRCode
import timber.log.Timber

class SubscriptionModel(application: Application) : AndroidViewModel(application) {
    private val app: App
        get() = getApplication()

    private val _order = MutableLiveData<Order>()
    private val _payment = MutableLiveData<Payment>()
    private val _qrcode = MutableLiveData<ImageBitmap>()
    private val _error = MutableLiveData<String?>(null)

    val order: LiveData<Order>
        get() = _order

    val payment: LiveData<Payment>
        get() = _payment

    val qrcode: LiveData<ImageBitmap>
        get() = _qrcode

    val error: LiveData<String?>
        get() = _error

    private val servitor by lazy {
        ServitorApiFactory().connect(Firebase.remoteConfig.getString(AppConfig.PREF_SERVITOR_URL))
    }

    fun createOrder(subscriptionPeriod: Subscription.Period) {
        Timber.i("Create order for $subscriptionPeriod")
        Firebase.analytics.logEvent("subscribe_${subscriptionPeriod.toString().lowercase()}", Bundle.EMPTY)
        viewModelScope.launch { innerCreateOrder(subscriptionPeriod) }
    }

    fun createPayment(order: Order) {
        Timber.i("Create payment for ${order.id}")
        viewModelScope.launch { innerCreatePayment(order) }
    }

    private suspend fun innerCreateOrder(subscriptionPeriod: Subscription.Period) {
        var tries = 0
        while(_order.value == null) {
            try {
                val order = servitor.createOrder(
                    CreateOrderRequest(
                        app.myDeviceId,
                        "${Build.MANUFACTURER} ${Build.MODEL}",
                        "subscription",
                        subscriptionPeriod.toString().lowercase(),
                    )
                )

                _order.postValue(order)
                _error.postValue(null)
            } catch (ex: Exception) {
                Timber.e(ex)
                if(tries++ > 5) {
                    _error.postValue(ex.message)
                    return
                }
                delay(1000)
            }
        }
    }

    private suspend fun innerCreatePayment(order: Order) {
        var tries = 0
        while (_payment.value == null) {
            try {
                val payment = servitor.createPayment(order.id)
                Timber.d("Payment = $payment")

                _payment.postValue(payment)
                _error.postValue(null)

                innerCheckPayment(payment)

                payment.confirmationUrl?.let { generateQrCode(it) }
            } catch (ex: Exception) {
                Timber.e(ex)
                if (tries++ > 5) {
                    _error.postValue(ex.message)
                    return
                }
                delay(1000)
            }
        }
    }

    private suspend fun innerCheckPayment(payment: Payment) {
        var p = payment
        while (!p.status.isFinal()) {
            delay(5000)

            try {
                Timber.d("Check payment ${payment.id}")
                p = servitor.checkPayment(payment.id)
                Timber.d("Payment: $p")
                _payment.postValue(p)
                _error.postValue(null)

                p.confirmationUrl?.let { generateQrCode(it) }
                Timber.d("Status = ${p.status}")
            } catch (ex: Exception) {
                Timber.e(ex)
                _error.postValue(ex.message)
            }
        }
    }

    private fun generateQrCode(url: String) {
        Timber.d("Generate QR for $url")

        val bitmap = QRCode.ofCircles()
            .build(url)
            .render()
            .nativeImage() as Bitmap
        _qrcode.postValue(bitmap.asImageBitmap())
    }
}
