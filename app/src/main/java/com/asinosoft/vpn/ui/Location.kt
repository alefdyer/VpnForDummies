package com.asinosoft.vpn.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.asinosoft.vpn.R
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme
import java.util.Locale

@SuppressLint("DiscouragedApi")
@Composable
fun Location(country: String) {
    val context = LocalContext.current
    val icon = context.resources.getIdentifier(country.lowercase(), "drawable", context.packageName)
        .let { if (it == 0) R.drawable.jolly_roger else it }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(icon),
            contentDescription = country,
            modifier = Modifier.size(21.dp, 15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(Locale("", country).displayCountry)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLocation() {
    VpnForDummiesTheme(isInDarkTheme = false) {
        Column {
            Location("af")
            Location("fi")
            Location("ru")
            Location("zw")
        }
    }
}
