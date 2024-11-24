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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.asinosoft.vpn.R
import com.asinosoft.vpn.dto.Order
import com.asinosoft.vpn.ui.theme.Golden
import com.asinosoft.vpn.ui.theme.Typography
import java.math.BigDecimal
import java.util.Currency
import java.util.UUID

@Composable
fun OrderInfo(
    order: Order,
    modifier: Modifier = Modifier,
) {
    val period = when (order.content.period) {
        "day" -> pluralStringResource(R.plurals.days, 1, 1)
        "week" -> pluralStringResource(R.plurals.weeks, 1, 1)
        "month" -> pluralStringResource(R.plurals.month, 1, 1)
        "year" -> pluralStringResource(R.plurals.year, 1, 1)
        else -> stringResource(R.string.unlimit)
    }

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row {
            val crown = painterResource(R.drawable.ic_crown)
            val title = stringResource(R.string.subscription)
            Icon(crown, title, Modifier.size(24.dp), Golden)
            Spacer(Modifier.width(8.dp))
            Text(title, style = Typography.titleLarge)
        }

        Text("На $period", style = Typography.titleMedium)

        Text("${order.sum} ${order.currency.symbol}", style = Typography.titleMedium)
    }
}

@Preview(showBackground = true, locale = "ru", widthDp = 400, heightDp = 400)
@Composable
fun PreviewOrderInfo() {
    val order = Order(
        id = UUID.randomUUID().toString(),
        item = "subscription",
        content = Order.Content(period = "month"),
        sum = BigDecimal("299.00"),
        currency = Currency.getInstance("RUB")
    )

    OrderInfo(order)
}