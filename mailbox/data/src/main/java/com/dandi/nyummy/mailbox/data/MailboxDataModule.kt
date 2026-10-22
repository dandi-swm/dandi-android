package com.dandi.nyummy.mailbox.data

import android.content.Context
import com.dandi.nyummy.common.data.image.ImageUploadDataSource
import com.dandi.nyummy.mailbox.data.image.InquiryImageDataSource
import com.dandi.nyummy.mailbox.domain.MailboxRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MailboxDataModule {

    @Provides
    @Singleton
    fun provideMailboxApiService(retrofit: Retrofit): MailboxApiService =
        retrofit.create(MailboxApiService::class.java)

    @Provides
    @Singleton
    fun provideMailboxDataSource(apiService: MailboxApiService): MailboxDataSource =
        MailboxDataSource(apiService)

    @Provides
    @Singleton
    fun provideInquiryImageDataSource(@ApplicationContext context: Context): InquiryImageDataSource =
        InquiryImageDataSource(context)

    // 문의 첨부 사진의 업로드 URL 발급과 스토리지 업로드는 공통 이미지 업로드(common:data ImageUploadModule)를 쓴다.
    @Provides
    @Singleton
    fun provideMailboxRepository(
        dataSource: MailboxDataSource,
        imageUploadDataSource: ImageUploadDataSource,
        inquiryImageDataSource: InquiryImageDataSource,
    ): MailboxRepository = MailboxRepositoryImpl(dataSource, imageUploadDataSource, inquiryImageDataSource)
}
