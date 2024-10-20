package com.asinosoft.vpn.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.R

@Composable
fun EllipsisMenu(
    onShowUrl: (Uri) -> Unit,
    onRateUs: () -> Unit,
)
{
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.settings),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.licenses)) },
                onClick = {
                    showMenu = false
                    onShowUrl(AppConfig.LICENSES)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.private_policy)) },
                onClick = {
                    showMenu = false
                    onShowUrl(AppConfig.PRIVATE_POLICY)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rate_us)) },
                onClick = {
                    showMenu = false
                    onRateUs()
                }
            )
        }
    }
}
