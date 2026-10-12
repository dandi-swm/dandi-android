package com.dandi.nyummy.meal.data

import android.content.Context
import com.dandi.nyummy.common.data.image.ImageUploadDataSource
import com.dandi.nyummy.meal.domain.MealRecordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @Singleton
    fun provideMealRecordDataSource(apiService: MealRecordApiService): MealRecordDataSource =
        MealRecordDataSource(apiService)

    @Provides
    @Singleton
    fun provideMealGalleryPhotoDataSource(
        @ApplicationContext context: Context,
    ): MealGalleryPhotoDataSource = MealGalleryPhotoDataSource(context)

    // 사진 업로드 URL 발급과 스토리지 업로드는 공통 이미지 업로드(common:data ImageUploadModule)를 쓴다.
    @Provides
    @Singleton
    fun provideMealRecordRepository(
        dataSource: MealRecordDataSource,
        imageUploadDataSource: ImageUploadDataSource,
        galleryPhotoDataSource: MealGalleryPhotoDataSource,
    ): MealRecordRepository = MealRecordRepositoryImpl(dataSource, imageUploadDataSource, galleryPhotoDataSource)
}
