package com.asinosoft.vpn.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.asinosoft.vpn.dto.Config
import com.asinosoft.vpn.ui.components.Countdown
import com.asinosoft.vpn.ui.components.Location

@Composable
fun PremiumVpnView(
    modifier: Modifier,
    config: Config,
    switchPosition: Boolean = false,
    message: String? = null,
    error: String? = null,
    timer: String? = null,
    onStartVpn: () -> Unit = {},
    onStopVpn: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Row {
            Location(config.country)
            Spacer(Modifier.width(16.dp))
            timer?.let { Countdown(it) }
        }

        message?.let { Text(text = it, color = MaterialTheme.colorScheme.onBackground) }

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
}
