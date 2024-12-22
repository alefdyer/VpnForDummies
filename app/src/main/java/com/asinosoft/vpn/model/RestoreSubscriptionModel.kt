package com.asinosoft.vpn.model

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asinosoft.vpn.AppConfig
import com.asinosoft.vpn.api.RestoreSubscriptionRequest
import com.asinosoft.vpn.api.ServitorApiFactory
import com.asinosoft.vpn.util.myDeviceId
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RestoreSubscriptionUiState {
    data object EnterEmail : RestoreSubscriptionUiState()

    data class SendRequest(val email: String) : RestoreSubscriptionUiState()

    data class RequestSucceed(val email: String) : RestoreSubscriptionUiState()

    data class RequestFailed(val error: String) : RestoreSubscriptionUiState()
}

class RestoreSubscriptionModel(val app: Application) : AndroidViewModel(app) {
    private val servitor by lazy {
        val servitorUrl = Firebase.remoteConfig.getString(AppConfig.PREF_SERVITOR_URL)
        ServitorApiFactory().connect(servitorUrl)
    }

    private val _state =
        MutableStateFlow<RestoreSubscriptionUiState>(RestoreSubscriptionUiState.EnterEmail)

    val state: StateFlow<RestoreSubscriptionUiState> get() = _state.asStateFlow()

    fun restore(email: String) {
        _state.value = RestoreSubscriptionUiState.SendRequest(email)

        viewModelScope.launch {
            val request = RestoreSubscriptionRequest(
                email = email,
                deviceId = app.myDeviceId,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            )

            try {
                servitor.restoreSubscription(request)
                _state.value = RestoreSubscriptionUiState.RequestSucceed(email)
            } catch (e: Exception) {
                _state.value = RestoreSubscriptionUiState.RequestFailed(e.message.toString())
            }
        }
    }
}
