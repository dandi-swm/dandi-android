package com.dandi.nyummy.widget.presentation

import android.content.Context
import com.dandi.nyummy.widget.domain.WidgetRefresher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** 앱이 위젯 값을 바꾼 뒤 위젯을 곧바로 다시 그리게 한다. */
class NyamiWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetRefresher {
    override fun requestRefresh() {
        NyamiWidgetReceiver.requestRefresh(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetPresentationModule {
    @Binds
    @Singleton
    abstract fun bindWidgetRefresher(impl: NyamiWidgetRefresher): WidgetRefresher
}
