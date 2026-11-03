package com.dandi.nyummy.widget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.domain.helper.WidgetHelper
import com.dandi.nyummy.widget.domain.WidgetRefresher
import com.dandi.nyummy.widget.domain.WidgetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "home_widget")

@Module
@InstallIn(SingletonComponent::class)
object WidgetDataModule {

    @Provides
    @Singleton
    fun provideWidgetDataSource(@ApplicationContext context: Context, json: Json): WidgetDataSource =
        WidgetDataSource(context.widgetDataStore, json)

    @Provides
    @Singleton
    fun provideWidgetRepository(dataSource: WidgetDataSource, tokenProvider: TokenProvider): WidgetRepository =
        WidgetRepositoryImpl(dataSource, tokenProvider)

    @Provides
    @Singleton
    fun provideWidgetHelper(repository: WidgetRepository, refresher: WidgetRefresher): WidgetHelper =
        WidgetHelperImpl(repository, refresher)
}
