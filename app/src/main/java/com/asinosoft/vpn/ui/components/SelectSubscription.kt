package com.asinosoft.vpn.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.R
import com.asinosoft.vpn.dto.Info
import com.asinosoft.vpn.dto.Subscription
import com.asinosoft.vpn.ui.InfoView
import com.asinosoft.vpn.ui.theme.Golden
import com.asinosoft.vpn.ui.theme.Typography
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme

@Composable
fun SubscriptionMenu(
    onSelectPeriod: (Subscription.Period) -> Unit = {},
) {
    var showInfo by remember { mutableStateOf(false) }
    val offer = Info(AppConfig.OFFER_AND_ACCEPTANCE, stringResource(R.string.offer_and_acceptance))

    if (showInfo) {
        InfoView(offer) { showInfo = false }
    } else {
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
            VerticalSubscriptionMenu(onSelectPeriod) { showInfo = true }
        } else {
            HorizontalSubscriptionMenu(onSelectPeriod) { showInfo = true }
        }
    }
}

@Composable
private fun HorizontalSubscriptionMenu(
    onSelect: (Subscription.Period) -> Unit = {},
    onShowInfo: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize(),
        Arrangement.SpaceAround,
        Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_crown),
                "crown",
                Modifier.size(24.dp),
                Golden
            )
            Spacer(Modifier.width(8.dp))
            Text("Платная подписка", style = Typography.titleMedium)
        }

        Row {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(rememberVectorPainter(Icons.Filled.Check), "check", tint = Golden)
                    Spacer(Modifier.width(8.dp))
                    Text("Отключите рекламу", style = Typography.titleSmall)
                }
                Spacer(Modifier.height(16.dp))
                SubscriptionButton("На сутки", "15 ₽") { onSelect(Subscription.Period.DAY) }
                Spacer(Modifier.height(16.dp))
                SubscriptionButton("На месяц", "299 ₽") { onSelect(Subscription.Period.MONTH) }
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(rememberVectorPainter(Icons.Filled.Check), "check", tint = Golden)
                    Spacer(Modifier.width(8.dp))
                    Text("Скорость для 4К видео", style = Typography.titleSmall)
                }
                Spacer(Modifier.height(16.dp))
                SubscriptionButton("На неделю", "99 ₽") { onSelect(Subscription.Period.WEEK) }
                Spacer(Modifier.height(16.dp))
                SubscriptionButton("На год", "2400 ₽") { onSelect(Subscription.Period.YEAR) }
            }
        }

        Text(
            "Договор оферты",
            modifier = Modifier.clickable { onShowInfo() },
            color = Color.Blue,
            textDecoration = TextDecoration.Underline
        )
    }
}

@Composable
private fun VerticalSubscriptionMenu(
    onSelect: (Subscription.Period) -> Unit = {},
    onShowInfo: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxSize(),
        Arrangement.SpaceAround,
        Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_crown),
                "crown",
                Modifier.size(24.dp),
                Golden
            )
            Spacer(Modifier.width(8.dp))
            Text("Платная подписка", style = Typography.titleMedium)
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(rememberVectorPainter(Icons.Filled.Check), "check", tint = Golden)
                Spacer(Modifier.width(8.dp))
                Text("Отключите рекламу", style = Typography.titleSmall)
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(rememberVectorPainter(Icons.Filled.Check), "check", tint = Golden)
                Spacer(Modifier.width(8.dp))
                Text("Скорость для 4К видео", style = Typography.titleSmall)
            }
        }

        Column {
            SubscriptionButton("На сутки", "15 ₽") { onSelect(Subscription.Period.DAY) }
            Spacer(Modifier.height(16.dp))
            SubscriptionButton("На неделю", "99 ₽") { onSelect(Subscription.Period.WEEK) }
            Spacer(Modifier.height(16.dp))
            SubscriptionButton("На месяц", "299 ₽") { onSelect(Subscription.Period.MONTH) }
            Spacer(Modifier.height(16.dp))
            SubscriptionButton("На год", "2400 ₽") { onSelect(Subscription.Period.YEAR) }
        }

        Text(
            "Договор оферты",
            modifier = Modifier.clickable { onShowInfo() },
            color = Color.Blue,
            textDecoration = TextDecoration.Underline
        )
    }
}

@Preview(
    showSystemUi = true,
    device = "spec:width=411dp, height=891dp, orientation=landscape, dpi=420"
)
@Composable
fun PreviewSubscriptionMenu() {
    VpnForDummiesTheme(isInDarkTheme = true) {
        SubscriptionMenu {}
    }
}
