package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.domain.AuthRepository
import com.dandi.nyummy.common.data.token.TokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AuthDataModule {
    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)


    @Provides
    @Singleton
    fun provideAuthDataSource(apiService: AuthApiService): AuthDataSource =
        AuthDataSource(apiService)

    @Provides
    @Singleton
    fun provideAuthRepository(
        dataSource: AuthDataSource,
        tokenProvider: TokenProvider,
    ): AuthRepository = AuthRepositoryImpl(dataSource, tokenProvider)

}
