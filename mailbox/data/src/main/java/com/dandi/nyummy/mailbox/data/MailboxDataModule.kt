package com.dandi.nyummy.mailbox.data

import com.dandi.nyummy.mailbox.domain.MailboxRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideMailboxRepository(dataSource: MailboxDataSource): MailboxRepository =
        MailboxRepositoryImpl(dataSource)
}
