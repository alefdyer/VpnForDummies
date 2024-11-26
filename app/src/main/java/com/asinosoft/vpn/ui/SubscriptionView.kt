package com.asinosoft.vpn.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.asinosoft.vpn.R
import com.asinosoft.vpn.dto.Order
import com.asinosoft.vpn.dto.Payment
import com.asinosoft.vpn.dto.Subscription
import com.asinosoft.vpn.ui.components.OrderInfo
import com.asinosoft.vpn.ui.components.SubscriptionMenu
import com.asinosoft.vpn.ui.theme.Typography
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme
import qrcode.QRCode
import timber.log.Timber
import java.math.BigDecimal
import java.util.Currency

@Composable
fun SubscriptionView(
    order: Order? = null,
    payment: Payment? = null,
    qrcode: ImageBitmap? = null,
    error: String? = null,
    onCreateOrder: (Subscription.Period) -> Unit = {},
    onClose: () -> Unit = {},
) {
    val height = Modifier.height(LocalConfiguration.current.screenHeightDp.div(5).dp)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        order?.let { order ->
            OrderInfo(order, height)

            if (null == payment) {
                CircularProgressIndicator()
            }

            error?.let { error ->
                Text(
                    error,
                    modifier = Modifier.fillMaxWidth(),
                    style = Typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                Button(onClick = onClose) {
                    Text(stringResource(R.string.close))
                }
            }

            if (null == error) {
                qrcode?.let { qrcode ->
                    Image(
                        qrcode,
                        contentDescription = "Qr Code",
                        modifier = Modifier.padding(40.dp)
                    )
                }
            }

            if (null != error && null == qrcode) {
                CircularProgressIndicator()
            }
        }

        if (null == order) {
            SubscriptionMenu(onCreateOrder)
        }
    }
}

@Preview(showSystemUi = true, locale = "ru", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSubscriptionView() {
    val order = Order(
        "9d82a8fe-c795-4758-af2b-ace42f6ab936",
        "subscription",
        Order.Content("month"),
        BigDecimal("234.56"),
        Currency.getInstance("RUB")
    )

    val payment = Payment(
        "9d83f51b-3ebb-4581-8368-e5fe5ec566e6",
        BigDecimal("234.56"),
        Currency.getInstance("RUB"),
        Payment.Status.WAITING,
        "https://api.yookassa.ru/v3"
    )

    val logo = LocalContext.current.classLoader.getResourceAsStream("/res/drawable/ic_yookassa.png")
        ?.readBytes()
    Timber.d("Logo size ${logo?.size}")

    val qrcode = QRCode.ofCircles()
        .withLogo(logo, 92, 92)
        .build("https://asinosoft.ru/vpn.html")
        .render()
        .nativeImage() as Bitmap

    VpnForDummiesTheme {
        SubscriptionView(
            order,
            payment,
            qrcode.asImageBitmap(),
            " Failed to choose config with EGL_SWAP_BEHAVIOR_PRESERVED, retrying without...",
        )
    }
}
