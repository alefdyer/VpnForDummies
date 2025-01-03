package com.asinosoft.vpn.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.asinosoft.vpn.R
import com.asinosoft.vpn.ui.theme.Typography

@Composable
fun WaitingForConfig(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {

        Text(
            text = stringResource(R.string.wait_for_config),
            style = Typography.titleLarge
        )

        Switch(checked = false, enabled = false, onCheckedChange = {})
    }
}

@Preview(widthDp = 320, heightDp = 640)
@Composable
fun PreviewWaitingForConfig() {
    Surface {
        WaitingForConfig()
    }
}
