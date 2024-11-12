package com.asinosoft.vpn.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun MainView(modifier: Modifier = Modifier) {
    var showInfo by remember { mutableStateOf<Uri?>(null) }

    showInfo?.let {
        InfoView(it) { showInfo = null }
    } ?: VpnView(modifier) { showInfo = it }
}
