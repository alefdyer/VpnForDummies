package com.asinosoft.vpn.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.asinosoft.vpn.R
import com.asinosoft.vpn.ui.theme.Golden
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme

@Composable
fun PremiumButton(onClick: () -> Unit) {
    val title = stringResource(R.string.get_premium)
    val icon = painterResource(R.drawable.ic_crown)

    Button(onClick) {
        Icon(icon, title, Modifier.size(24.dp), Golden)
        Spacer(Modifier.width(8.dp))
        Text(title)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    var count by remember { mutableIntStateOf(0) }
    VpnForDummiesTheme {
        PremiumButton { count += 1 }
        Text("$count")
    }
}
