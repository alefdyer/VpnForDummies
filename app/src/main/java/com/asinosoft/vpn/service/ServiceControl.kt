package com.asinosoft.vpn.service

import android.app.Service
import com.asinosoft.vpn.dto.ServiceState

interface ServiceControl {
    fun getService(): Service

    fun startService()

    fun stopService()

    fun vpnProtect(socket: Int): Boolean

    fun getState(): ServiceState
}