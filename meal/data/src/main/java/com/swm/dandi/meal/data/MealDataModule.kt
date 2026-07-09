package com.swm.dandi.meal.data

import android.content.Context
import com.swm.dandi.meal.domain.MealRepository
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
object MealDataModule {

    @Provides
    @Singleton
    fun provideMealRepository(dataSource: MealDataSource): MealRepository =
        MealRepositoryImpl(dataSource)

    @Provides
    @Singleton
    fun provideMealDataSource(
        apiService: MealApiService,
        skeletonResponseLoader: MealSkeletonResponseLoader,
    ): MealDataSource =
        MealDataSource(apiService, skeletonResponseLoader)

    @Provides
    @Singleton
    fun provideMealSkeletonResponseLoader(
        @ApplicationContext context: Context,
        json: Json,
    ): MealSkeletonResponseLoader =
        MealSkeletonResponseLoader(context.assets, json)

    @Provides
    @Singleton
    fun provideMealApiService(retrofit: Retrofit): MealApiService =
        retrofit.create(MealApiService::class.java)
}
