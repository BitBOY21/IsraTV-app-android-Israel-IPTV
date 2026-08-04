package com.isratv.android.domain

import android.os.Bundle

interface AnalyticsLogger {
    fun logEvent(eventName: String, params: Bundle?)
    fun setUserProperty(propertyName: String, propertyValue: String)
}