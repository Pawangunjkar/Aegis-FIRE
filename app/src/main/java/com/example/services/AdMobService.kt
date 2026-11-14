package com.example.services

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobService {
    // Official Google AdMob test unit IDs
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewarded = false

    private var lastInterstitialTimeMs: Long = 0
    private const val INTERSTITIAL_COOLDOWN_MS = 240_000L // 4 minutes
    private var interstitialImpressions = 0
    private const val MAX_INTERSTITIAL_IMPRESSIONS = 3

    fun preloadAds(context: Context) {
        preloadInterstitial(context)
        preloadRewarded(context)
    }

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || !isNetworkAvailable(context)) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun preloadRewarded(context: Context) {
        if (rewardedAd != null || isLoadingRewarded || !isNetworkAvailable(context)) return
        isLoadingRewarded = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                }
            }
        )
    }

    fun showInterstitial(context: Context, onDismiss: () -> Unit) {
        val now = System.currentTimeMillis()
        val activity = context.findActivity()
        if (interstitialAd != null &&
            activity != null &&
            (now - lastInterstitialTimeMs > INTERSTITIAL_COOLDOWN_MS) &&
            interstitialImpressions < MAX_INTERSTITIAL_IMPRESSIONS
        ) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastInterstitialTimeMs = System.currentTimeMillis()
                    interstitialImpressions++
                    preloadInterstitial(context)
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    interstitialAd = null
                    preloadInterstitial(context)
                    onDismiss()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onDismiss()
        }
    }

    fun showRewardedWithFallback(
        context: Context,
        onLoadingChanged: (Boolean) -> Unit = {},
        onReward: () -> Unit
    ) {
        val activity = context.findActivity()
        if (activity == null || !isNetworkAvailable(context)) {
            Log.d("AdMobService", "Offline or no activity, granting reward via fallback")
            onReward()
            return
        }

        // Fast path: if already preloaded, show immediately with zero wait
        val preloaded = rewardedAd
        if (preloaded != null) {
            rewardedAd = null
            showLoadedRewarded(activity, preloaded, onReward)
            preloadRewarded(context)
            return
        }

        var isHandled = false
        val handler = Handler(Looper.getMainLooper())
        onLoadingChanged(true)

        val timeoutRunnable = Runnable {
            if (!isHandled) {
                isHandled = true
                onLoadingChanged(false)
                Log.d("AdMobService", "Rewarded ad load timeout, providing fallback")
                onReward()
            }
        }

        handler.postDelayed(timeoutRunnable, 4000)

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    if (isHandled) {
                        rewardedAd = ad
                        return
                    }
                    isHandled = true
                    handler.removeCallbacks(timeoutRunnable)
                    onLoadingChanged(false)
                    showLoadedRewarded(activity, ad, onReward)
                    preloadRewarded(context)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (isHandled) return
                    isHandled = true
                    handler.removeCallbacks(timeoutRunnable)
                    onLoadingChanged(false)
                    onReward()
                }
            }
        )
    }

    private fun showLoadedRewarded(activity: Activity, ad: RewardedAd, onReward: () -> Unit) {
        var rewardEarned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (!rewardEarned) {
                    // Graceful unlock if dismissed cleanly in test mode or after close
                    onReward()
                }
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                onReward()
            }
        }
        ad.show(activity) {
            rewardEarned = true
            onReward()
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
