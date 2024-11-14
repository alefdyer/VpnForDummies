package com.asinosoft.vpn.ui

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.asinosoft.vpn.R
import com.asinosoft.vpn.dto.Info

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoView(
    info: Info,
    onClose: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(info.title) },
            windowInsets = WindowInsets(0.dp),
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            },
        )
        AndroidView(
            factory = {
                WebView(it).apply {
                    loadUrl(info.url.toString())
                }
            },
            modifier = Modifier.wrapContentHeight()
        )
    }

    BackHandler(onBack = onClose)
}
