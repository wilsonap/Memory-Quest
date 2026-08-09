package com.example.review

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

object InAppReviewManager {
    private const val TAG = "InAppReviewManager"

    fun requestInAppReview(activity: Activity, onComplete: () -> Unit = {}) {
        try {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        val flow = manager.launchReviewFlow(activity, reviewInfo)
                        flow.addOnCompleteListener {
                            Log.i(TAG, "In-App Review flow finished.")
                            onComplete()
                        }
                    } else {
                        onComplete()
                    }
                } else {
                    Log.w(TAG, "Failed to request review flow: ${task.exception?.message}")
                    onComplete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting in-app review: ${e.message}", e)
            onComplete()
        }
    }
}
