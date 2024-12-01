package com.asinosoft.vpn.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.R
import com.asinosoft.vpn.dto.Info
import com.asinosoft.vpn.model.MainModel
import com.asinosoft.vpn.ui.components.EllipsisMenu
import com.asinosoft.vpn.ui.components.WaitingForConfig
import com.asinosoft.vpn.ui.theme.Typography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnView(
    modifier: Modifier = Modifier,
    model: MainModel = viewModel(),
    onShowInfo: (Info) -> Unit = {},
    onPremiumClicked: () -> Unit = {},
) {
    val config by model.config.observeAsState(null)
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
        } else {
            try {
                requestVpnPermission.launch(intent)
            } catch (e: ActivityNotFoundException) {
                model.setError(context.getString(R.string.device_not_supported))
            } catch (e: Throwable) {
                model.setError(e.message)
            }
        }
    }

    val onRateUs = { context.startActivity(Intent(Intent.ACTION_VIEW, AppConfig.RATE_US)) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = Typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = { EllipsisMenu(config, onShowInfo, onRateUs) }
            )
        }
    ) { paddingValues ->
        if (null == config) {
            WaitingForConfig(Modifier.padding(paddingValues))
        } else config?.let {
            Box(Modifier.padding(paddingValues)) {
                when (it.breakForAdsInterval) {
                    0L -> PremiumVpnView(
                        config = it,
                        switchPosition = switchPosition,
                        timer = timer,
                        message = message,
                        error = error,
                        onStartVpn = checkPermissionAndStartV2Ray,
                        onStopVpn = model::stopVpn
                    )

                    else -> FreeVpnView(
                        switchPosition = switchPosition,
                        timer = timer,
                        message = message,
                        error = error,
                        onStartVpn = checkPermissionAndStartV2Ray,
                        onStopVpn = model::stopVpn,
                        onPremiumClicked = onPremiumClicked,
                    )
                }
            }
        }
    }
}
