package com.asinosoft.vpn.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.R
import com.asinosoft.vpn.dto.Info
import com.asinosoft.vpn.ui.theme.Typography
import java.text.DateFormat
import java.util.Date

@Composable
fun EllipsisMenu(
    onShowInfo: (Info) -> Unit,
    onRateUs: () -> Unit,
) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(LocalContext.current.packageName, 0)
    val version = stringResource(
        R.string.version,
        "${packageInfo.versionName}",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else ""
    )
    val lastUpdate = stringResource(
        R.string.last_update,
        DateFormat.getDateInstance().format(Date(packageInfo.lastUpdateTime))
    )

    var showMenu by remember { mutableStateOf(false) }

    val licenses = Info(AppConfig.LICENSES, stringResource(R.string.licenses))
    val policies = Info(AppConfig.PRIVATE_POLICY, stringResource(R.string.private_policy))

    Box {
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
                    onShowInfo(licenses)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.private_policy)) },
                onClick = {
                    showMenu = false
                    onShowInfo(policies)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rate_us)) },
                onClick = {
                    showMenu = false
                    onRateUs()
                }
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                lastUpdate,
                style = Typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(16.dp, 0.dp)
            )
            Text(
                version,
                style = Typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(16.dp, 0.dp)
            )
        }
    }
}
