package com.asinosoft.vpn.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.R
import com.asinosoft.vpn.dto.Config
import com.asinosoft.vpn.dto.Info
import com.asinosoft.vpn.model.MainModel
import com.asinosoft.vpn.ui.components.Countdown
import com.asinosoft.vpn.ui.components.EllipsisMenu
import com.asinosoft.vpn.ui.components.Location
import com.asinosoft.vpn.ui.components.PremiumButton
import com.asinosoft.vpn.ui.theme.Typography
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest

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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = Typography.titleLarge,
                    )
                },
                actions = { EllipsisMenu(onShowInfo, onRateUs) }
            )
        }
    ) { paddingValues ->
        if (null == config) {
            WaitingForConfig(Modifier.padding(paddingValues))
        } else config?.let {
            when (it.breakForAdsInterval) {
                0L -> PremiumVpnView(
                    modifier = Modifier.padding(paddingValues),
                    config = it,
                    switchPosition = switchPosition,
                    timer = timer,
                    message = message,
                    error = error,
                    onStartVpn = checkPermissionAndStartV2Ray,
                    onStopVpn = model::stopVpn
                )

                else -> FreeVpnView(
                    modifier = Modifier.padding(paddingValues),
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

@Composable
fun WaitingForConfig(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {

        Text(
            text = stringResource(R.string.wait_for_config),
            color = MaterialTheme.colorScheme.onBackground,
            style = Typography.titleLarge
        )

        Switch(checked = false, enabled = false, onCheckedChange = {})
    }
}

@Composable
fun PremiumVpnView(
    modifier: Modifier,
    config: Config,
    switchPosition: Boolean = false,
    message: String? = null,
    error: String? = null,
    timer: String? = null,
    onStartVpn: () -> Unit = {},
    onStopVpn: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Row {
            Location(config.country)
            Spacer(Modifier.width(16.dp))
            timer?.let { Countdown(it) }
        }

        message?.let { Text(text = it, color = MaterialTheme.colorScheme.onBackground) }

        error?.let { Text(text = it, color = MaterialTheme.colorScheme.onBackground) }

        Switch(modifier = Modifier
            .focusRequester(focusRequester)
            .focusable(),
            checked = switchPosition,
            onCheckedChange = {
                if (it) {
                    onStartVpn()
                } else {
                    onStopVpn()
                }
            })
    }
}

@Composable
fun FreeVpnView(
    modifier: Modifier,
    switchPosition: Boolean = false,
    message: String? = null,
    error: String? = null,
    timer: String? = null,
    onStartVpn: () -> Unit = {},
    onStopVpn: () -> Unit = {},
    onPremiumClicked: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    val adSize = LocalConfiguration.current.screenWidthDp
        .coerceAtMost(LocalConfiguration.current.screenHeightDp)

    VpnLayout(
        modifier = modifier.fillMaxSize(),
        content = { mod ->
            Column(
                mod.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PremiumButton(onPremiumClicked)

                message?.let { Text(text = it, color = MaterialTheme.colorScheme.onBackground) }

                timer?.let { Countdown(it) }

                error?.let { Text(text = it, color = MaterialTheme.colorScheme.onBackground) }

                Switch(modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable(),
                    checked = switchPosition,
                    onCheckedChange = {
                        if (it) {
                            onStartVpn()
                        } else {
                            onStopVpn()
                        }
                    })
            }
        },
        advertisement = {
            AndroidView(
                modifier = Modifier
                    .width(adSize.dp)
                    .height(adSize.dp),
                factory = {
                    BannerAdView(it).apply {
                        setAdUnitId("demo-banner-yandex")
                        setAdSize(BannerAdSize.inlineSize(context, adSize, adSize))
                        loadAd(AdRequest.Builder().build())
                    }
                })
        })
}

@Composable
fun VpnLayout(
    modifier: Modifier,
    advertisement: @Composable () -> Unit,
    content: @Composable (modifier: Modifier) -> Unit,
) {
    val screen = LocalConfiguration.current
    if (screen.orientation == Configuration.ORIENTATION_PORTRAIT) {
        Column(modifier, Arrangement.SpaceAround) {
            content(Modifier.weight(1f))
            advertisement()
        }
    } else {
        Row(modifier, Arrangement.SpaceBetween) {
            advertisement()
            content(Modifier.weight(1f))
        }
    }
}
