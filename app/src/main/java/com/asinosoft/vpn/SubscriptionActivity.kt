package com.asinosoft.vpn

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.asinosoft.vpn.ui.SubscriptionView
import com.asinosoft.vpn.ui.theme.VpnForDummiesTheme
import timber.log.Timber

class SubscriptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VpnForDummiesTheme {
                SubscriptionView { succeed ->
                    Timber.d("SubscriptionActivity result = $succeed")

                    val result = if (succeed) Activity.RESULT_OK else Activity.RESULT_CANCELED
                    setResult(result)
                    finish()
                }
            }
        }
    }
}
