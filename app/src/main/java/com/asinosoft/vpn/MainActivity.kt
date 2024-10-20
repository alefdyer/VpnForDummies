package com.asinosoft.vpn

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.asinosoft.vpn.model.MainModel
import com.asinosoft.vpn.ui.EllipsisMenu
import com.asinosoft.vpn.ui.theme.Typography
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme

class MainActivity : ComponentActivity() {
    private val model: MainModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.startListenBroadcast()
        model.retrieveConfig(this)

        setContent {
            VpnForDummiesTheme {
                MainView()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainView(
    modifier: Modifier = Modifier, model: MainModel = viewModel()
) {
    var showInfo by remember { mutableStateOf<Uri?>(null) }
    val connectionName by model.connectionName.observeAsState(stringResource(id = R.string.wait_for_config))
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

    if (showInfo != null) {
        AndroidView(
            factory = {
                WebView(it).apply {
                    loadUrl("$showInfo")
                }
            },
            modifier = Modifier.wrapContentHeight()
        )
        BackHandler {
            showInfo = null
        }
    } else {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }

        Column(
            modifier = modifier.fillMaxSize()
        ) {
            EllipsisMenu(
                onShowUrl = { showInfo = it },
                onRateUs = { context.startActivity(Intent(Intent.ACTION_VIEW, AppConfig.RATE_US)) }
            )

            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = connectionName,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = Typography.titleLarge
                )

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
}

@Preview(device = Devices.TV_720p)
@Composable
fun MainViewPreview() {
    MainView()
}
