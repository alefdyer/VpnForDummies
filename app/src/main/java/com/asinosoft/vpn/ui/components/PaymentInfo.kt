package com.asinosoft.vpn.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.asinosoft.vpn.R
import com.asinosoft.vpn.dto.Payment
import com.asinosoft.vpn.ui.theme.Golden
import com.asinosoft.vpn.ui.theme.Typography
import java.math.BigDecimal
import java.util.Currency
import java.util.UUID

@Composable
fun PaymentInfo(
    payment: Payment,
    modifier: Modifier = Modifier,
) {
    val status = when (payment.status) {
        Payment.Status.COMPLETED -> stringResource(R.string.completed)
        Payment.Status.CANCELED -> stringResource(R.string.canceled)
        else -> stringResource(R.string.waiting)
    }

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row {
            val crown = painterResource(R.drawable.ic_crown)
            val title = stringResource(R.string.payment).capitalize(Locale.current)
            Icon(crown, title, Modifier.size(24.dp), Golden)
            Spacer(Modifier.width(8.dp))
            Text(title, style = Typography.titleLarge)
        }

        Text("${payment.sum} ${payment.currency.symbol}", style = Typography.titleMedium)

        Text(status, style = Typography.titleMedium)
    }
}

@Preview(showBackground = true, locale = "ru", widthDp = 400, heightDp = 400)
@Composable
fun PreviewPaymentInfo() {
    PaymentInfo(
        Payment(
            id = UUID.randomUUID().toString(),
            sum = BigDecimal("299.00"),
            currency = Currency.getInstance("RUB"),
            status = Payment.Status.PENDING,
            confirmationUrl = null,
        )
    )
}
