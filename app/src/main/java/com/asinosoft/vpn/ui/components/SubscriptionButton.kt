package com.asinosoft.vpn.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme

@Composable
fun SubscriptionButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Button(onClick, Modifier.width(144.dp), shape = RoundedCornerShape(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title)
            Text(subtitle)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSubscriptionButton() {
    VpnForDummiesTheme {
        SubscriptionButton("На месяц", "299 ₽")
    }
}
