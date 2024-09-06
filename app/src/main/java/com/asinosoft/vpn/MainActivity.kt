package com.asinosoft.vpn

import android.app.Activity
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
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
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
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
                        this,
                        getString(R.string.config_error, task.exception),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MainView(
    modifier: Modifier = Modifier,
    model: MainModel = viewModel()
) {
    val config by model.config.observeAsState()
    val connectionName by model.connectionName.observeAsState(stringResource(id = R.string.wait_for_config))
    val isReady by model.isReady.observeAsState(false)
    val switchPosition by model.switchPosition.observeAsState(false)
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
        } else {
            requestVpnPermission.launch(intent)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Text(
            text = connectionName,
            style = Typography.titleLarge
        )

        Switch(
            checked = switchPosition,
            enabled = isReady,
            onCheckedChange = {
                if (it) {
                    checkPermissionAndStartV2Ray()
                } else {
                    model.stopVpn()
                }
            }
        )
    }
}

@Preview(device = Devices.TV_720p)
@Composable
fun MainViewPreview() {
    MainView()
}
