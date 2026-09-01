package com.dandi.nyummy.meal.data

import com.dandi.nyummy.common.data.di.NoAuthApi
import com.dandi.nyummy.meal.domain.MealRecordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MealRecordDataModule {

    @Provides
    @Singleton
    fun provideMealRecordApiService(retrofit: Retrofit): MealRecordApiService =
        retrofit.create(MealRecordApiService::class.java)

    // presigned 업로드는 Bearer 헤더·401 재발급이 붙으면 안 되므로 공개 클라이언트로 생성한다.
    @Provides
    @Singleton
    fun provideMealImageUploadApiService(@NoAuthApi retrofit: Retrofit): MealImageUploadApiService =
        retrofit.create(MealImageUploadApiService::class.java)

    @Provides
    @Singleton
    fun provideMealRecordDataSource(
        apiService: MealRecordApiService,
        uploadApiService: MealImageUploadApiService,
    ): MealRecordDataSource = MealRecordDataSource(apiService, uploadApiService)

    @Provides
    @Singleton
    fun provideMealRecordRepository(dataSource: MealRecordDataSource): MealRecordRepository =
        MealRecordRepositoryImpl(dataSource)
}
