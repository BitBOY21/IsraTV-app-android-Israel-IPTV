package com.isratv.android.di

import com.isratv.android.data.repository.GitHubUpdateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UpdateModule {

    @Provides
    @Singleton
    fun provideGitHubUpdateRepository(): GitHubUpdateRepository {
        return GitHubUpdateRepository()
    }
}