package com.asinosoft.vpn.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.asinosoft.vpn.R
import com.asinosoft.vpn.ui.components.Countdown
import com.asinosoft.vpn.ui.components.PremiumButton
import com.asinosoft.vpn.ui.components.Switcher
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest

@Preview
@Composable
fun FreeVpnView(
    switchPosition: Boolean = false,
    message: String? = null,
    error: String? = null,
    timer: String? = null,
    onStartVpn: () -> Unit = {},
    onStopVpn: () -> Unit = {},
    onPremiumClicked: () -> Unit = {},
) {
    VpnLayout(
        modifier = Modifier.fillMaxSize(),
        content = { mod ->
            Column(
                mod.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PremiumButton(onPremiumClicked)

                message?.let { Text(text = it, color = MaterialTheme.colorScheme.onBackground) }

                timer?.let { Countdown(it) }

                error?.let { Text(text = it, color = MaterialTheme.colorScheme.onBackground) }

                Switcher(switchPosition, onStartVpn, onStopVpn)
            }
        },
        advertisement = { modifier ->
            val screen = LocalConfiguration.current
            val adSize =
                if (screen.orientation == Configuration.ORIENTATION_PORTRAIT)
                    screen.smallestScreenWidthDp
                else
                    screen.smallestScreenWidthDp.div(2)

            AndroidView(
                modifier = modifier
                    .fillMaxSize()
                    .clickable(onClick = onPremiumClicked),
                factory = {
                    BannerAdView(it).apply {
                        setAdUnitId(context.getString(R.string.yandex_banner_unit_id))
                        setAdSize(BannerAdSize.inlineSize(context, adSize, adSize))
                        loadAd(AdRequest.Builder().build())
                    }
                })
        })
}

@Composable
fun VpnLayout(
    modifier: Modifier = Modifier,
    advertisement: @Composable (modifier: Modifier) -> Unit,
    content: @Composable (modifier: Modifier) -> Unit,
) {
    val screen = LocalConfiguration.current
    if (screen.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Column(modifier, Arrangement.SpaceAround) {
            content(Modifier.weight(1f))
            advertisement(Modifier.weight(1f))
        }
    } else {
        Row(modifier, Arrangement.SpaceBetween) {
            advertisement(Modifier.weight(1f))
            content(Modifier.weight(2f))
        }
    }
}
