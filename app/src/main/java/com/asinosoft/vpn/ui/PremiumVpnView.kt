package com.asinosoft.vpn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asinosoft.vpn.dto.Config
import com.asinosoft.vpn.ui.components.Countdown
import com.asinosoft.vpn.ui.components.Location
import com.asinosoft.vpn.ui.components.Switcher

@Composable
fun PremiumVpnView(
    config: Config,
    switchPosition: Boolean = false,
    message: String? = null,
    error: String? = null,
    timer: String? = null,
    onStartVpn: () -> Unit = {},
    onStopVpn: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize(),
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

        Switcher(switchPosition, onStartVpn, onStopVpn)
    }
}
