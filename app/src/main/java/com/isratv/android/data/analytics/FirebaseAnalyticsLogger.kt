package com.isratv.android.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.isratv.android.domain.AnalyticsLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsLogger @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsLogger {

    override fun logEvent(eventName: String, params: Bundle?) {
        firebaseAnalytics.logEvent(eventName, params)
    }

    override fun setUserProperty(propertyName: String, propertyValue: String) {
        firebaseAnalytics.setUserProperty(propertyName, propertyValue)
    }
}