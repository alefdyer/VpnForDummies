package com.asinosoft.vpn.dto

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.asinosoft.vpn.StartActivity

class AdvertisementResultContract : ActivityResultContract<Void?, Boolean>() {
    override fun createIntent(context: Context, input: Void?): Intent =
        Intent(context, StartActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        Activity.RESULT_OK == resultCode
}
