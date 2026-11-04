package com.dandi.nyummy.shop.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dandi.nyummy.common.data.di.AppPreferenceDataStore
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.shop.domain.CoinChargeRepository
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
    fun provideCartApiService(retrofit: Retrofit): CartApiService =
        retrofit.create(CartApiService::class.java)

    @Provides
    @Singleton
    fun providePurchaseApiService(retrofit: Retrofit): PurchaseApiService =
        retrofit.create(PurchaseApiService::class.java)

    @Provides
    @Singleton
    fun provideCoinApiService(retrofit: Retrofit): CoinApiService =
        retrofit.create(CoinApiService::class.java)

    @Provides
    @Singleton
    fun provideShopDataSource(
        apiService: ShopApiService,
        cartApiService: CartApiService,
        purchaseApiService: PurchaseApiService,
        coinApiService: CoinApiService,
    ): ShopDataSource = ShopDataSource(apiService, cartApiService, purchaseApiService, coinApiService)

    @Provides
    @Singleton
    fun provideShopRepository(dataSource: ShopDataSource, json: Json): ShopRepository =
        ShopRepositoryImpl(dataSource, json)

    @Provides
    @Singleton
    fun provideCoinChargeLocalDataSource(
        @AppPreferenceDataStore dataStore: DataStore<Preferences>,
    ): CoinChargeLocalDataSource = CoinChargeLocalDataSource(dataStore)

    @Provides
    @Singleton
    fun provideCoinChargeRepository(
        remoteConfigHelper: RemoteConfigHelper,
        json: Json,
        localDataSource: CoinChargeLocalDataSource,
        dataSource: ShopDataSource,
    ): CoinChargeRepository = CoinChargeRepositoryImpl(remoteConfigHelper, json, localDataSource, dataSource)
}
