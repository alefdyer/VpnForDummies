package com.asinosoft.vpn.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.asinosoft.vpn.R
import com.asinosoft.vpn.ui.components.Countdown
import com.asinosoft.vpn.ui.components.PremiumButton
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest

@Composable
fun FreeVpnView(
    modifier: Modifier = Modifier,
    switchPosition: Boolean = false,
    message: String? = null,
    error: String? = null,
    timer: String? = null,
    onStartVpn: () -> Unit = {},
    onStopVpn: () -> Unit = {},
    onPremiumClicked: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    val adSize = LocalConfiguration.current.screenWidthDp
        .coerceAtMost(LocalConfiguration.current.screenHeightDp)

    VpnLayout(
        modifier = modifier.fillMaxSize(),
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

                Switch(modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable(),
                    checked = switchPosition,
                    onCheckedChange = {
                        if (it) {
                            onStartVpn()
                        } else {
                            onStopVpn()
                        }
                    })
            }
        },
        advertisement = {
            AndroidView(
                modifier = Modifier
                    .width(adSize.dp)
                    .height(adSize.dp),
                factory = {
                    BannerAdView(it).apply {
                        setAdUnitId("demo-banner-yandex")
                        setAdSize(BannerAdSize.inlineSize(context, adSize, adSize))
                        loadAd(AdRequest.Builder().build())
                    }
                })
        })
}

@Composable
fun VpnLayout(
    modifier: Modifier = Modifier,
    advertisement: @Composable () -> Unit,
    content: @Composable (modifier: Modifier) -> Unit,
) {
    val screen = LocalConfiguration.current
    if (screen.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Column(modifier, Arrangement.SpaceAround) {
            content(Modifier.weight(1f))
            advertisement()
        }
    } else {
        Row(modifier, Arrangement.SpaceBetween) {
            advertisement()
            content(Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true, widthDp = 640, heightDp = 480)
@Composable
fun PreviewVpnLayout() {
    VpnLayout(
        content = {
            Text("Content", modifier = Modifier.fillMaxSize(), textAlign = TextAlign.Center)
        },
        advertisement = {
            Image(
                painterResource(R.drawable.ic_crown),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}
