package com.asinosoft.vpn

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.asinosoft.vpn.dto.Config
import com.asinosoft.vpn.dto.getConfig
import com.asinosoft.vpn.dto.putConfig
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import timber.log.Timber

class StartActivity : AppCompatActivity(), RewardedAdLoadListener, RewardedAdEventListener {
    private lateinit var config: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.i("StartActivity::onCreate ${intent.extras?.keySet()?.joinToString(", ")}")
        super.onCreate(savedInstanceState)
        config = intent?.getConfig() ?: return finish()

        RewardedAdLoader(this).apply {
            val adUnitId = getString(R.string.yandex_reward_unit_id)
            val adConfig = AdRequestConfiguration.Builder(adUnitId).build()
            setAdLoadListener(this@StartActivity)
            loadAd(adConfig)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                stopVpn()
            }
        })

        setContent { WaitingForTheAds() }
    }

    override fun onAdLoaded(rewarded: RewardedAd) {
        rewarded.apply {
            setAdEventListener(this@StartActivity)
            show(this@StartActivity)
        }
    }

    override fun onAdFailedToLoad(error: AdRequestError) = startVpn()

    override fun onAdShown() {}

    override fun onRewarded(reward: Reward) = startVpn()

    override fun onAdFailedToShow(adError: AdError) = startVpn()

    override fun onAdDismissed() = startVpn()

    override fun onAdClicked() {}

    override fun onAdImpression(impressionData: ImpressionData?) {}

    private fun startVpn() {
        Timber.d("StartActivity::startVpn")
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putConfig(config)
        )
    }

    private fun stopVpn() {
        Timber.d("StartActivity::stopVpn")
        finish()
    }
}

@Composable
fun WaitingForTheAds() {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
