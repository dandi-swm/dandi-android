package com.dandi.nyummy.common.presentation

import android.content.Context
import com.dandi.nyummy.common.domain.coroutine.TtiDispatcher
import com.dandi.nyummy.common.domain.helper.MealAnalysisEventHelper
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.presentation.helper.MealAnalysisEventHelperImpl
import com.dandi.nyummy.common.presentation.helper.MessageHelperImpl
import com.dandi.nyummy.common.presentation.helper.NavigationHelperImpl
import com.dandi.nyummy.common.presentation.helper.ResourceHelperImpl
import com.dandi.nyummy.tti.DebugTTILogger
import com.dandi.nyummy.tti.NoOpTTIReporter
import com.dandi.nyummy.tti.RemoteTTILogger
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIHelperImpl
import com.dandi.nyummy.tti.TTILogger
import com.dandi.nyummy.tti.TTIReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonPresentationModule {
    @Provides
    @Singleton
    fun provideMessageHelper(@ApplicationContext context: Context): MessageHelper =
        MessageHelperImpl(context)

    @Provides
    @Singleton
    fun provideNavigationHelper(): NavigationHelper = NavigationHelperImpl()

    @Provides
    @Singleton
    fun provideMealAnalysisEventHelper(): MealAnalysisEventHelper = MealAnalysisEventHelperImpl()

    @Provides
    @Singleton
    fun provideResourceHelper(@ApplicationContext context: Context): ResourceHelper =
        ResourceHelperImpl(context)

    @Provides
    @Singleton
    fun provideTTILogger(): TTILogger {
        return if (BuildConfig.DEBUG) DebugTTILogger() else RemoteTTILogger()
    }

    @Provides
    @Singleton
    fun provideTTIReporter(): TTIReporter = NoOpTTIReporter
}

/**
 * ViewModel 하나에 하나씩 생기는 의존성. TTIHelper 는 ViewModel 인스턴스당 하나 만들어
 * 그 ViewModel 과, 거기에 주입되는 UseCase 들이 같은 측정 인스턴스를 공유한다.
 */
@Module
@InstallIn(ViewModelComponent::class)
object CommonViewModelModule {
    @Provides
    @ViewModelScoped
    fun provideTTIHelper(
        reporter: TTIReporter,
        logger: TTILogger,
        @TtiDispatcher ttiDispatcher: CoroutineDispatcher,
        lifecycle: ViewModelLifecycle,
    ): TTIHelper = TTIHelperImpl(
        reporter = reporter,
        logger = logger,
        dispatcher = ttiDispatcher,
    ).also { helper ->
        // 화면을 떠나 ViewModel 이 사라질 때 아직 보내지 않은 측정을 보낸다. 이미 보냈으면 무시된다.
        lifecycle.addOnClearedListener { helper.shotTTILogging() }
    }
}
