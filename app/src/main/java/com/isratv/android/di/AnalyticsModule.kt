package com.isratv.android.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.isratv.android.data.analytics.FirebaseAnalyticsLogger
import com.isratv.android.domain.AnalyticsLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideAnalyticsLogger(firebaseAnalytics: FirebaseAnalytics): AnalyticsLogger {
        return FirebaseAnalyticsLogger(firebaseAnalytics)
    }
}
