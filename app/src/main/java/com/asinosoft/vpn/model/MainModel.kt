package com.asinosoft.vpn.model

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainModel : ViewModel() {
    val config = MutableLiveData<Uri>()
    val isReady = MutableLiveData<Boolean>(false)

    fun setConfig(uri: Uri) {
        config.postValue(uri)
        isReady.postValue(true)
    }
}
