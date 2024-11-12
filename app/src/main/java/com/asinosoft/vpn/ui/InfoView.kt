package com.asinosoft.vpn.ui

import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun InfoView(url: Uri, onClose: () -> Unit = {}) {
    AndroidView(
        factory = {
            WebView(it).apply {
                loadUrl("$url")
            }
        },
        modifier = Modifier.wrapContentHeight()
    )
    BackHandler(onBack = onClose)
}
