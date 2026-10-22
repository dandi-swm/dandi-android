package com.dandi.nyummy.settings.data

import android.content.Context
import com.dandi.nyummy.common.data.image.ImageUploadDataSource
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.settings.data.image.InquiryImageDataSource
import com.dandi.nyummy.settings.domain.InquiryRepository
import com.dandi.nyummy.settings.domain.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsDataModule {

    @Provides
    @Singleton
    fun provideSettingsApiService(retrofit: Retrofit): SettingsApiService =
        retrofit.create(SettingsApiService::class.java)

    @Provides
    @Singleton
    fun provideSettingsRemoteDataSource(apiService: SettingsApiService): SettingsRemoteDataSource =
        SettingsRemoteDataSource(apiService)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        remoteDataSource: SettingsRemoteDataSource,
        tokenProvider: TokenProvider,
    ): SettingsRepository = SettingsRepositoryImpl(remoteDataSource, tokenProvider)

    @Provides
    @Singleton
    fun provideInquiryApiService(retrofit: Retrofit): InquiryApiService =
        retrofit.create(InquiryApiService::class.java)

    @Provides
    @Singleton
    fun provideInquiryRemoteDataSource(apiService: InquiryApiService): InquiryRemoteDataSource =
        InquiryRemoteDataSource(apiService)

    @Provides
    @Singleton
    fun provideInquiryImageDataSource(@ApplicationContext context: Context): InquiryImageDataSource =
        InquiryImageDataSource(context)

    // 문의 첨부 사진의 업로드 URL 발급과 스토리지 업로드는 공통 이미지 업로드(common:data ImageUploadModule)를 쓴다.
    @Provides
    @Singleton
    fun provideInquiryRepository(
        remoteDataSource: InquiryRemoteDataSource,
        imageUploadDataSource: ImageUploadDataSource,
        inquiryImageDataSource: InquiryImageDataSource,
    ): InquiryRepository = InquiryRepositoryImpl(remoteDataSource, imageUploadDataSource, inquiryImageDataSource)
}
