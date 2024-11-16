package com.asinosoft.vpn.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.asinosoft.vpn.R
import com.asinosoft.vpn.dto.Info
import com.asinosoft.vpn.ui.InfoView
import com.asinosoft.vpn.ui.components.SubscriptionMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentView(
    onClose: () -> Unit = {}
) {
    var showInfo by remember { mutableStateOf<Info?>(null) }

    showInfo?.let {
        InfoView(it) { showInfo = null }
    } ?: Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
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
        }
    ) { paddings ->
        Box(Modifier.padding(paddings)) {
            SubscriptionMenu({})
        }
    }
}

@Preview
@Composable
fun Preview() {
    PaymentView({})
}
