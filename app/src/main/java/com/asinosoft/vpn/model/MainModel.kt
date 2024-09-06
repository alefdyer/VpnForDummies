package com.asinosoft.vpn.model

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainModel : ViewModel() {
    val config = MutableLiveData<Uri>()
    val connectionName = MutableLiveData<String>()
    val isReady = MutableLiveData(false)

    fun setConfig(uri: Uri) {
        config.postValue(uri)
        connectionName.postValue(uri.fragment)
        isReady.postValue(true)
    }
}
