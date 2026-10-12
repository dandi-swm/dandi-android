package com.dandi.nyummy.common.data.di

import com.dandi.nyummy.common.data.image.ImageStorageApiService
import com.dandi.nyummy.common.data.image.ImageUploadApiService
import com.dandi.nyummy.common.data.image.ImageUploadDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** 용도별 이미지 업로드(발급 + 스토리지 업로드) 공용 바인딩. 식사, 문의 등 이미지를 올리는 기능이 같이 쓴다. */
@Module
@InstallIn(SingletonComponent::class)
object ImageUploadModule {

    @Provides
    @Singleton
    fun provideImageUploadApiService(retrofit: Retrofit): ImageUploadApiService =
        retrofit.create(ImageUploadApiService::class.java)

    // presigned 업로드는 Bearer 헤더·401 재발급이 붙으면 안 되므로 공개 클라이언트로 생성한다.
    @Provides
    @Singleton
    fun provideImageStorageApiService(@NoAuthApi retrofit: Retrofit): ImageStorageApiService =
        retrofit.create(ImageStorageApiService::class.java)

    @Provides
    @Singleton
    fun provideImageUploadDataSource(
        apiService: ImageUploadApiService,
        storageApiService: ImageStorageApiService,
    ): ImageUploadDataSource = ImageUploadDataSource(apiService, storageApiService)
}
