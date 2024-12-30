package com.asinosoft.vpn.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.asinosoft.vpn.R
import com.asinosoft.vpn.ui.components.Countdown
import com.asinosoft.vpn.ui.components.PremiumButton
import com.asinosoft.vpn.ui.components.Switcher

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

                message?.let { Text(text = it) }

                timer?.let { Countdown(it) }

                error?.let { Text(text = it) }

                Switcher(switchPosition, onStartVpn, onStopVpn)
            }
        },
        advertisement = { modifier ->
            Image(
                painter = painterResource(R.drawable.ic_icon),
                modifier = modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentDescription = "Banner"
            )
        })
}

@Composable
fun VpnLayout(
    modifier: Modifier = Modifier,
    advertisement: @Composable (modifier: Modifier) -> Unit,
    content: @Composable (modifier: Modifier) -> Unit,
) {
    Surface {
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
}
