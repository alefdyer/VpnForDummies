package com.asinosoft.vpn.model

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
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

    fun createOrder(subscription: Subscription) {
        viewModelScope.launch { innerCreateOrder(subscription) }
    }

    fun createPayment(order: Order) {
        viewModelScope.launch { innerCreatePayment(order) }
    }

    private suspend fun innerCreateOrder(subscription: Subscription) {
        while (_order.value == null) {
            try {
                val order = servitor.createOrder(
                    CreateOrderRequest(
                        app.myDeviceId,
                        "${Build.MANUFACTURER} ${Build.MODEL}",
                        "subscription",
                        subscription.period.toString().lowercase(),
                    )
                )

                _order.postValue(order)
                _error.postValue(null)
            } catch (ex: Exception) {
                _error.postValue(ex.message)
                delay(500)
            }
        }
    }

    private suspend fun innerCreatePayment(order: Order) {
        while (_payment.value == null) {
            try {
                val payment = servitor.createPayment(order.id)
                Timber.d("Payment = $payment")

                _payment.postValue(payment)
                _error.postValue(null)

                innerCheckPayment(payment)

                payment.confirmationUrl?.let { generateQrCode(it) }
            } catch (ex: Exception) {
                _error.postValue(ex.message)
                delay(500)
            }
        }
    }

    private suspend fun innerCheckPayment(payment: Payment) {
        var p = payment
        while (!p.status.isFinal()) {
            delay(1000)

            try {
                p = servitor.checkPayment(payment.id)
                Timber.d("Payment: $p")
                _payment.postValue(p)
                _error.postValue(null)

                p.confirmationUrl?.let { generateQrCode(it) }
                Timber.d("Status = ${p.status}")
            } catch (ex: Exception) {
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
