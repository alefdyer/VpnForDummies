package com.asinosoft.vpn.util

import android.content.Context
import android.content.Intent
import com.asinosoft.vpn.AppConfig

object MessageUtil {

    fun sendMsg2Service(ctx: Context, what: Int, content: String? = null) {
        sendMsg(ctx, AppConfig.BROADCAST_ACTION_SERVICE, what, content)
    }

    fun sendMsg2UI(ctx: Context, what: Int, content: String? = null) {
        sendMsg(ctx, AppConfig.BROADCAST_ACTION_ACTIVITY, what, content)
    }

    private fun sendMsg(ctx: Context, action: String, what: Int, content: String? = null) {
        try {
            val intent = Intent()
            intent.action = action
            intent.`package` = AppConfig.PACKAGE
            intent.putExtra("key", what)
            intent.putExtra("content", content)
            ctx.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
