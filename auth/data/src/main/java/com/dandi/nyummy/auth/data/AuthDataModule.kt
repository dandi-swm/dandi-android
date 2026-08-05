package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.domain.AuthRepository
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
    fun provideAuthRepository(dataSource: AuthDataSource): AuthRepository =
        AuthRepositoryImpl(dataSource)

    @Provides
    @Singleton
    fun provideAuthDataSource(apiService: AuthApiService): AuthDataSource =
        AuthDataSource(apiService)

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)
}
