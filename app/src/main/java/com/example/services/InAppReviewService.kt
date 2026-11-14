package com.example.services

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

object InAppReviewService {
    private const val TAG = "InAppReviewService"

    fun requestAndLaunchReview(
        activity: Activity,
        reviewManager: ReviewManager? = null,
        onFlowFinished: (Boolean) -> Unit = {}
    ) {
        try {
            val manager = reviewManager ?: ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = manager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener {
                        onFlowFinished(true)
                    }
                } else {
                    Log.w(TAG, "Play In-App Review request failed: ${task.exception?.message}")
                    onFlowFinished(false)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Play In-App Review unavailable on this device: ${e.message}")
            onFlowFinished(false)
        }
    }
}
