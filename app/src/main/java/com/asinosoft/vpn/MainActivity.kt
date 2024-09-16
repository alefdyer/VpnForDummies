package com.asinosoft.vpn

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.asinosoft.vpn.model.MainModel
import com.asinosoft.vpn.service.ServiceManager
import com.asinosoft.vpn.ui.theme.Typography
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig

class MainActivity : ComponentActivity() {
    private val remoteConfig = Firebase.remoteConfig
    private val model: MainModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.startListenBroadcast()

        setContent {
            VpnForDummiesTheme {
                MainView()
            }
        }

        retrieveConfig()
    }

    private fun retrieveConfig() {
        remoteConfig.fetchAndActivate().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val keys = remoteConfig.getKeysByPrefix(AppConfig.PREF_CONNECTION_PREFIX)
                Log.d(AppConfig.TAG, "Config complete: $keys")

                val connection: String = remoteConfig.all.filter { entry ->
                    entry.key.startsWith(AppConfig.PREF_CONNECTION_PREFIX)
                }.values.random().asString()

                val config = Uri.parse(connection)
                Log.d(AppConfig.TAG, "Config: $config")

                model.setConfig(config)
            } else {
                Toast.makeText(
                    this, getString(R.string.config_error, task.exception), Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainView(
    modifier: Modifier = Modifier, model: MainModel = viewModel()
) {
    var showInfo by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val config by model.config.observeAsState()
    val connectionName by model.connectionName.observeAsState(stringResource(id = R.string.wait_for_config))
    val isReady by model.isReady.observeAsState(false)
    val switchPosition by model.switchPosition.observeAsState(false)
    val message by model.message.observeAsState("")
    val error by model.error.observeAsState("")
    val context = LocalContext.current

    val requestVpnPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                ServiceManager.startV2Ray(context, config!!)
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
                    loadUrl(showInfo!!)
                }
            },
            modifier = Modifier.wrapContentHeight()
        )
        BackHandler {
            showInfo = null
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, stringResource(R.string.settings))
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.licenses)) },
                        onClick = {
                            showInfo = AppConfig.LICENSES.toString()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.private_policy)) },
                        onClick = {
                            showInfo = AppConfig.PRIVATE_POLICY.toString()
                            showMenu = false
                        })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rate_us)) },
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, AppConfig.RATE_US))
                            showMenu = false
                        }
                    )
                }
            }

            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = connectionName, style = Typography.titleLarge
                )

                message?.let { Text(text = it) }

                error?.let { Text(text = it) }

                Switch(checked = switchPosition, enabled = isReady, onCheckedChange = {
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
