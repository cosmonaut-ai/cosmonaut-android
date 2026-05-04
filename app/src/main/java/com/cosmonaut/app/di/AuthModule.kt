package com.cosmonaut.app.di

import com.cosmonaut.app.auth.AuthManager
import com.cosmonaut.app.data.remote.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideTokenAuthenticator(authManager: AuthManager): TokenAuthenticator = TokenAuthenticator(authManager)
}
