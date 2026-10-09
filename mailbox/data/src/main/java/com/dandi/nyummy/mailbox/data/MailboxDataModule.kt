package com.dandi.nyummy.mailbox.data

import android.content.Context
import com.dandi.nyummy.mailbox.domain.MailboxRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MailboxDataModule {

    // TODO(server): 문의 API가 생기면 Retrofit ApiService를 만들어 MailboxDataSource에 넘긴다.
    @Provides
    @Singleton
    fun provideMailboxDataSource(@ApplicationContext context: Context, json: Json): MailboxDataSource =
        MailboxDataSource(context, json)

    @Provides
    @Singleton
    fun provideMailboxRepository(dataSource: MailboxDataSource): MailboxRepository =
        MailboxRepositoryImpl(dataSource)
}
