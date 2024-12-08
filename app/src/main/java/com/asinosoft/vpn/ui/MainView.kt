package com.asinosoft.vpn.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.asinosoft.vpn.dto.Info

@Composable
fun MainView(
    modifier: Modifier = Modifier,
    onPremiumClicked: () -> Unit = {},
) {
    var showInfo by remember { mutableStateOf<Info?>(null) }

    showInfo?.let {
        InfoView(it) { showInfo = null }
    } ?: VpnView(
        modifier,
        onShowInfo = { showInfo = it },
        onPremiumClicked = onPremiumClicked
    )
}
