package com.cosmonaut.app.di

import com.cosmonaut.app.analytics.CosmoAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideCosmoAnalytics(): CosmoAnalytics = CosmoAnalytics()
}
