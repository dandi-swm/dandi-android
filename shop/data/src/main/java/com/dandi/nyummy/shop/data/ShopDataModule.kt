package com.dandi.nyummy.shop.data

import com.dandi.nyummy.shop.domain.ShopRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ShopDataModule {

    @Provides
    @Singleton
    fun provideShopApiService(retrofit: Retrofit): ShopApiService =
        retrofit.create(ShopApiService::class.java)

    @Provides
    @Singleton
    fun provideShopDataSource(apiService: ShopApiService): ShopDataSource =
        ShopDataSource(apiService)

    @Provides
    @Singleton
    fun provideShopRepository(dataSource: ShopDataSource, json: Json): ShopRepository =
        ShopRepositoryImpl(dataSource, json)
}
