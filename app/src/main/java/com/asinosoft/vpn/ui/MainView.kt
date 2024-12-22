package com.asinosoft.vpn.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.asinosoft.vpn.dto.Config
import com.asinosoft.vpn.dto.Info

@Composable
fun MainView(
    modifier: Modifier = Modifier,
    onStartVpn: (Config) -> Unit = {},
    onStopVpn: () -> Unit = {},
    onRestoreSubscription: () -> Unit = {},
    onPremiumClicked: () -> Unit = {},
) {
    var showInfo by remember { mutableStateOf<Info?>(null) }

    val info = showInfo

    if (null !== info) {
        InfoView(info) { showInfo = null }
    } else {
        VpnView(
            modifier,
            onStartVpn = onStartVpn,
            onStopVpn = onStopVpn,
            onShowInfo = { showInfo = it },
            onRestoreSubscription = onRestoreSubscription,
            onPremiumClicked = onPremiumClicked
        )
    }
}
