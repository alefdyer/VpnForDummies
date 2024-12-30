package com.asinosoft.vpn.ui

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.asinosoft.vpn.R
import com.asinosoft.vpn.dto.Order
import com.asinosoft.vpn.dto.Payment
import com.asinosoft.vpn.dto.PreviewOrderProvider
import com.asinosoft.vpn.dto.Subscription
import com.asinosoft.vpn.model.SubscriptionUiState
import com.asinosoft.vpn.ui.components.EmailTextField
import com.asinosoft.vpn.ui.components.OrderInfo
import com.asinosoft.vpn.ui.components.SubscriptionMenu
import com.asinosoft.vpn.ui.theme.DarkGreen
import com.asinosoft.vpn.ui.theme.Typography
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun SubscriptionView(
    state: SubscriptionUiState,
    onCreateOrder: (Subscription.Period) -> Unit = {},
    onCreatePayment: (Order, String) -> Unit = { _, _ -> },
    onClose: (succeed: Boolean) -> Unit = {},
) {
    when (state) {
        is SubscriptionUiState.SelectSubscription -> SubscriptionMenu(onCreateOrder)
        is SubscriptionUiState.WaitForOrder -> WaitForOrder(state.order)
        is SubscriptionUiState.EnterEmail -> EnterEmail(state.order, state.email, onCreatePayment)
        is SubscriptionUiState.WaitForQrCode -> WaitForOrder(state.order)
        is SubscriptionUiState.WaitForPayment -> WaitForPayment(
            state.order, state.payment, state.qrcode
        )

        is SubscriptionUiState.Success -> SuccessView(state.order) { onClose(true) }
        is SubscriptionUiState.Error -> ErrorView(state.message, state.order) { onClose(false) }
    }
}

@Preview(locale = "ru")
@Composable
fun WaitForOrder(
    @PreviewParameter(PreviewOrderProvider::class) order: Order
) {
    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OrderInfo(order)

            CircularProgressIndicator()
        }
    }
}

@Preview(locale = "ru")
@Composable
fun EnterEmail(
    @PreviewParameter(PreviewOrderProvider::class) order: Order,
    defaultEmail: String? = null,
    onEmailEntered: (order: Order, email: String) -> Unit = { _, _ -> },
) {
    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var email by rememberSaveable { mutableStateOf(defaultEmail ?: "") }
            val send = {
                if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    onEmailEntered(order, email)
                }
            }

            OrderInfo(order)

            Text(
                stringResource(R.string.enter_email_prompt),
                style = Typography.labelLarge,
            )

            EmailTextField(
                email = email,
                onValueChange = { email = it },
                onSend = send,
            )

            Button(
                onClick = send,
                enabled = Patterns.EMAIL_ADDRESS.matcher(email).matches(),
                modifier = Modifier.padding(16.dp),
            ) {
                Text(stringResource(R.string.pay))
            }
        }
    }
}

@Preview(locale = "ru")
@Composable
fun SuccessView(
    @PreviewParameter(PreviewOrderProvider::class)
    order: Order,
    onClose: () -> Unit = {},
) {
    LaunchedEffect(order) {
        Timber.d("SuccessView Launched")
        delay(3000)
        onClose()
    }

    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OrderInfo(order)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    imageVector = Icons.Rounded.Check,
                    modifier = Modifier.size(24.dp),
                    contentDescription = "Success",
                    colorFilter = ColorFilter.tint(DarkGreen),
                )

                Text(
                    text = stringResource(R.string.paid),
                    style = Typography.titleMedium,
                )
            }
        }
    }
}

@Composable
fun ErrorView(
    error: String,
    order: Order? = null,
    onClose: () -> Unit = {},
) {
    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            order?.let { OrderInfo(it) }

            Text(
                error,
                modifier = Modifier.fillMaxWidth(),
                style = Typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Button(onClick = onClose) {
                Text(stringResource(R.string.close))
            }
        }
    }
}

@Composable
fun WaitForPayment(
    order: Order?,
    payment: Payment,
    qrcode: ImageBitmap,
) {
    val context = LocalContext.current
    val openBrowser =
        { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(payment.confirmationUrl))) }

    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            order?.let { OrderInfo(it) }

            Image(
                qrcode,
                contentDescription = "Qr Code",
                modifier = Modifier.padding(40.dp)
            )

            OutlinedButton(onClick = openBrowser) {
                Text(stringResource(R.string.pay))
            }
        }
    }
}
