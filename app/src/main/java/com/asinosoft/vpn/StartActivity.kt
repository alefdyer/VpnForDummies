package com.asinosoft.vpn

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.asinosoft.vpn.dto.Config
import com.asinosoft.vpn.service.ServiceManager
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

class StartActivity : AppCompatActivity(), RewardedAdLoadListener, RewardedAdEventListener {
    private var adLoader: RewardedAdLoader? = null
    private var ad: RewardedAd? = null

    private lateinit var config: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = Config.fromIntent(intent) ?: return finish()
        adLoader = RewardedAdLoader(this).apply {
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

    override fun onDestroy() {
        super.onDestroy()
        closeAd()
    }

    override fun onAdLoaded(rewarded: RewardedAd) {
        ad = rewarded.apply {
            setAdEventListener(this@StartActivity)
            show(this@StartActivity)
        }

        adLoader?.setAdLoadListener(null)
        adLoader = null
    }

    override fun onAdFailedToLoad(error: AdRequestError) = startVpn()

    override fun onAdShown() {}

    override fun onRewarded(reward: Reward) = startVpn()

    override fun onAdFailedToShow(adError: AdError) = startVpn()

    override fun onAdDismissed() = startVpn()

    override fun onAdClicked() {}

    override fun onAdImpression(impressionData: ImpressionData?) {}

    private fun startVpn() {
        ServiceManager.startV2Ray(application, config)
        finishAndRemoveTask()
    }

    private fun stopVpn() {
        ServiceManager.stopV2Ray(application)
        finishAndRemoveTask()
    }

    private fun closeAd() {
        adLoader?.setAdLoadListener(null)
        adLoader = null

        ad?.setAdEventListener(null)
        ad = null
    }
}

@Composable
fun WaitingForTheAds() {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        DotLottieAnimation(
            source = DotLottieSource.Asset("spinner.json"),
            autoplay = true,
            loop = true
        )
    }
}
