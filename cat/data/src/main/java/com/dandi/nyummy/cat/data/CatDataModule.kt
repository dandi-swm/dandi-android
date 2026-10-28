package com.dandi.nyummy.cat.data

import android.content.Context
import com.dandi.nyummy.cat.data.line.CatLineRepositoryImpl
import com.dandi.nyummy.cat.domain.CatLineRepository
import com.dandi.nyummy.cat.domain.CatRepository
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CatDataModule {

    @Provides
    @Singleton
    fun provideCatApiService(retrofit: Retrofit): CatApiService =
        retrofit.create(CatApiService::class.java)

    @Provides
    @Singleton
    fun provideCatDataSource(apiService: CatApiService): CatDataSource =
        CatDataSource(apiService)

    @Provides
    @Singleton
    fun provideCatAssetDataSource(@ApplicationContext context: Context, json: Json): CatAssetDataSource =
        CatAssetDataSource(context, json)

    @Provides
    @Singleton
    fun provideCatRepository(
        dataSource: CatDataSource,
        assetDataSource: CatAssetDataSource,
        companionMemory: CompanionMemoryHelper,
        tokenProvider: TokenProvider,
    ): CatRepository = CatRepositoryImpl(dataSource, assetDataSource, companionMemory, tokenProvider)

    @Provides
    @Singleton
    fun provideCatLineRepository(@ApplicationContext context: Context, json: Json): CatLineRepository =
        CatLineRepositoryImpl(context, json)
}
