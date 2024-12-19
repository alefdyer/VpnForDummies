package com.asinosoft.vpn.model

import android.app.Activity.MODE_PRIVATE
import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asinosoft.vpn.App
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.R
import com.asinosoft.vpn.api.CreateOrderRequest
import com.asinosoft.vpn.api.CreatePaymentRequest
import com.asinosoft.vpn.api.ServitorApiFactory
import com.asinosoft.vpn.dto.Order
import com.asinosoft.vpn.dto.Payment
import com.asinosoft.vpn.dto.Subscription
import com.asinosoft.vpn.util.myDeviceId
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import qrcode.QRCode
import timber.log.Timber
import java.math.BigDecimal
import java.util.Currency

sealed class SubscriptionUiState {
    data object SelectSubscription : SubscriptionUiState() {}

    data class WaitForOrder(
        val order: Order
    ) : SubscriptionUiState()

    data class EnterEmail(
        val order: Order,
        val email: String?,
    ) : SubscriptionUiState()

    data class WaitForQrCode(
        val order: Order
    ) : SubscriptionUiState()

    data class WaitForPayment(
        val order: Order,
        val payment: Payment,
        val qrcode: ImageBitmap,
    ) : SubscriptionUiState()

    data class Success(
        val order: Order,
        val payment: Payment,
    ) : SubscriptionUiState()

    data class Error(
        val message: String,
        val order: Order? = null,
    ) : SubscriptionUiState()
}

class SubscriptionModel(application: Application) : AndroidViewModel(application) {
    private val app: App
        get() = getApplication()

    private var _state =
        MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.SelectSubscription)

    val state: StateFlow<SubscriptionUiState>
        get() = _state.asStateFlow()

    private val servitor by lazy {
        val servitorUrl = Firebase.remoteConfig.getString(AppConfig.PREF_SERVITOR_URL)
        ServitorApiFactory().connect(servitorUrl)
    }

    fun createOrder(subscriptionPeriod: Subscription.Period) {
        Timber.i("Create order for $subscriptionPeriod")
        Firebase.analytics.logEvent(
            "subscribe_${subscriptionPeriod.toString().lowercase()}",
            Bundle.EMPTY
        )

        // TODO: по-идее это должно придти с бэка, как позиция прайс-листа
        val sum = when (subscriptionPeriod) {
            Subscription.Period.DAY -> 15.0
            Subscription.Period.WEEK -> 99.0
            Subscription.Period.MONTH -> 299.0
            Subscription.Period.YEAR -> 2400.0
        }

        _state.value = SubscriptionUiState.WaitForOrder(
            Order(
                id = "",
                item = "subscription",
                content = Order.Content(subscriptionPeriod.toString()),
                BigDecimal(sum),
                Currency.getInstance("RUB"),
            )
        )

        viewModelScope.launch { innerCreateOrder(subscriptionPeriod) }
    }

    fun createPayment(order: Order, email: String) {
        Timber.i("Create payment for ${order.id} -> $email")

        _state.value = SubscriptionUiState.WaitForQrCode(order)

        viewModelScope.launch { innerCreatePayment(order, email) }
    }

    private suspend fun innerCreateOrder(subscriptionPeriod: Subscription.Period) {
        var tries = 0
        while (true) {
            try {
                val order = servitor.createOrder(
                    CreateOrderRequest(
                        app.myDeviceId,
                        "${Build.MANUFACTURER} ${Build.MODEL}",
                        "subscription",
                        subscriptionPeriod.toString().lowercase(),
                    )
                )

                _state.value = SubscriptionUiState.EnterEmail(order, getDefaultEmail())
                break
            } catch (ex: Exception) {
                Timber.e(ex)
                if (tries++ > 5) {
                    val message = app.getString(R.string.error, ex.message)
                    _state.value = SubscriptionUiState.Error(message)
                    break
                }
                delay(1000)
            }
        }
    }

    private suspend fun innerCreatePayment(order: Order, email: String) {
        putDefaultEmail(email)

        var tries = 0
        while (true) {
            try {
                val payment = servitor.createPayment(order.id, CreatePaymentRequest(email))
                Timber.d("Payment = $payment")

                payment.confirmationUrl?.let {
                    _state.value = SubscriptionUiState.WaitForPayment(
                        order,
                        payment,
                        generateQrCode(it)
                    )
                }

                innerCheckPayment(order, payment)
                break
            } catch (ex: Exception) {
                Timber.e(ex)
                if (tries++ > 5) {
                    val message = app.getString(R.string.error, ex.message)
                    _state.value = SubscriptionUiState.Error(message)
                    break
                }
                delay(1000)
            }
        }
    }

    private suspend fun innerCheckPayment(order: Order, payment: Payment) {
        var p = payment
        while (!p.status.isFinal()) {
            delay(5000)

            try {
                Timber.d("Check payment ${payment.id}")
                p = servitor.checkPayment(payment.id)
                Timber.d("Payment: $p")
            } catch (ex: Exception) {
                Timber.e(ex)
                val message = app.getString(R.string.error, ex.message)
                _state.value = SubscriptionUiState.Error(message)
            }
        }

        _state.value = when (p.status) {
            Payment.Status.SUCCEEDED -> SubscriptionUiState.Success(order, p)
            else -> SubscriptionUiState.Error(
                app.getString(R.string.payment_error),
                order
            )
        }
    }

    private fun generateQrCode(url: String): ImageBitmap {
        Timber.d("Generate QR for $url")

        val bitmap = QRCode.ofCircles()
            .build(url)
            .render()
            .nativeImage() as Bitmap

        return bitmap.asImageBitmap()
    }

    private fun getDefaultEmail(): String? = app
        .getSharedPreferences("settings", MODE_PRIVATE)
        .getString("email", null)

    private fun putDefaultEmail(email: String) = app
        .getSharedPreferences("settings", MODE_PRIVATE)
        .edit()
        .putString("email", email)
        .apply()
}
