package com.asinosoft.vpn.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.R
import com.asinosoft.vpn.model.MainModel
import com.asinosoft.vpn.ui.theme.Typography

@Composable
fun VpnView(
    modifier: Modifier = Modifier,
    model: MainModel = viewModel(),
    onShowInfo: (Uri) -> Unit,
) {
    val config by model.config.observeAsState(null)
    val isReady by model.isReady.observeAsState(false)
    val switchPosition by model.switchPosition.observeAsState(false)
    val message by model.message.observeAsState("")
    val error by model.error.observeAsState("")
    val timer by model.timer.observeAsState("")
    val context = LocalContext.current

    val requestVpnPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                model.startVpn()
            }
        }

    val checkPermissionAndStartV2Ray = {
        val intent = VpnService.prepare(context)
        if (intent == null) {
            model.startVpn()
        } else try {
            requestVpnPermission.launch(intent)
        } catch (e: ActivityNotFoundException) {
            model.setError(context.getString(R.string.device_not_supported))
        } catch (e: Throwable) {
            model.setError(e.message)
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        EllipsisMenu(
            onShowUrl = onShowInfo,
            onRateUs = { context.startActivity(Intent(Intent.ACTION_VIEW, AppConfig.RATE_US)) }
        )

        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            config?.let {
                Location(it.country)
            }

            if (config == null) {
                Text(
                    text = stringResource(R.string.wait_for_config),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = Typography.titleLarge
                )
            }

            message?.let { Text(text = it, color = MaterialTheme.colorScheme.onBackground) }

            error?.let { Text(text = it, color = MaterialTheme.colorScheme.onBackground) }

            timer?.let { Text(text = it, color = MaterialTheme.colorScheme.onBackground) }

            Switch(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable(),
                checked = switchPosition,
                enabled = isReady,
                onCheckedChange = {
                    if (it) {
                        checkPermissionAndStartV2Ray()
                    } else {
                        model.stopVpn()
                    }
                })
        }
    }
}
