package com.dandi.nyummy.room.data

import com.dandi.nyummy.room.domain.RoomRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomDataModule {

    @Provides
    @Singleton
    fun provideRoomApiService(retrofit: Retrofit): RoomApiService =
        retrofit.create(RoomApiService::class.java)

    @Provides
    @Singleton
    fun provideRoomDataSource(apiService: RoomApiService): RoomDataSource =
        RoomDataSource(apiService)

    @Provides
    @Singleton
    fun provideRoomRepository(dataSource: RoomDataSource): RoomRepository =
        RoomRepositoryImpl(dataSource)
}
