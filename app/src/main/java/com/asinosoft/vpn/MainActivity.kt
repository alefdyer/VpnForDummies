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
                    Toast.makeText(this, "Configuration fetched", Toast.LENGTH_SHORT).show()

                    val connection: String = remoteConfig.all.filter { entry ->
                        entry.key.startsWith("connection")
                    }.values.random().asString()

                    val config = Uri.parse(connection)
                    Log.d("", "Config: $config")

                    model.setConfig(config)
                } else {
                    Toast.makeText(this, "Fetch failed ${task.exception}", Toast.LENGTH_SHORT).show()
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
    val isReady by model.isReady.observeAsState(false)

    var isRunning by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val startV2Ray = { ServiceManager.startV2Ray(context, model.config.value!!) }

    val stopV2Ray = { ServiceManager.stopV2Ray(context) }

    val requestVpnPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                ServiceManager.startV2Ray(context, config!!)
            }
        }

    val checkPermissionAndStartV2Ray = {
        val intent = VpnService.prepare(context)
        if (intent == null) {
            startV2Ray()
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
            text = if (isRunning) "ON" else "OFF",
            style = Typography.titleLarge
        )

        Switch(
            checked = isRunning,
            enabled = isReady,
            onCheckedChange = {
                isRunning = it

                if (isRunning) checkPermissionAndStartV2Ray()
                else stopV2Ray()
            }
        )
    }
}

@Preview(device = Devices.TV_720p)
@Composable
fun MainViewPreview() {
    MainView()
}
