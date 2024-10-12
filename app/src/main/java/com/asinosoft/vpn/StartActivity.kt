package com.asinosoft.vpn

import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.asinosoft.vpn.model.MainModel
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader

class StartActivity : AppCompatActivity(), InterstitialAdLoadListener, InterstitialAdEventListener {
    private var adLoader: InterstitialAdLoader? = null
    private var ad: InterstitialAd? = null

    private lateinit var config: Uri
    private val model: MainModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = intent.data ?: return finish()
        adLoader = InterstitialAdLoader(this).apply {
            val adUnitId = getString(R.string.yandex_ads_unit_id)
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

    override fun onAdLoaded(interstitialAd: InterstitialAd) {
        ad = interstitialAd.apply {
            setAdEventListener(this@StartActivity)
            show(this@StartActivity)
        }
    }

    override fun onAdFailedToLoad(error: AdRequestError) = startVpn()

    override fun onAdShown() {}

    override fun onAdFailedToShow(adError: AdError) = startVpn()

    override fun onAdDismissed() = startVpn()

    override fun onAdClicked() {}

    override fun onAdImpression(impressionData: ImpressionData?) {}

    private fun startVpn() {
        model.reallyStartVpn(config)
        finish()
    }

    private fun stopVpn() {
        model.stopVpn()
        finish()
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
