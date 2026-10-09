package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.onboarding.entity.CatVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** 서버 에러 바디의 `code` 를 cause 로 담는 실제 변환(BaseRemoteDataSource)과 같은 형태로 만든다. */
internal fun httpException(code: Int, errorCode: String? = null): HttpResponseException =
    HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/api/v1/cats",
        msg = "Http Request Failed ($code)",
        cause = errorCode?.let(::Throwable),
    )

internal class FakeOnboardingRepository(
    var error: Exception? = null,
) : OnboardingRepository {
    val registeredNames = mutableListOf<String>()
    var onboardingCompleteCount = 0
        private set

    override suspend fun registerCat(name: String): CatVO {
        registeredNames += name
        error?.let { throw it }
        return CatVO(id = 1L, name = name)
    }

    override suspend fun markOnboardingComplete() {
        onboardingCompleteCount++
    }

    val savedMealTimes = mutableListOf<MealTimesVO>()
    var saveMealTimesError: Exception? = null

    override suspend fun saveMealTimes(mealTimes: MealTimesVO) {
        saveMealTimesError?.let { throw it }
        savedMealTimes += mealTimes
    }
}

internal class RecordingNavigationHelper : NavigationHelper {
    val rootPages = mutableListOf<Page>()
    var initialCount = 0

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) = Unit
    override fun navigateDeepLink(route: NavRoute) = Unit
    override fun navigateToBack() = Unit
    override fun navigateToAsRoot(page: Page) {
        rootPages += page
    }

    override fun navigateToInitial() {
        initialCount++
    }

    override fun navigateToExternalLink(url: String) = Unit
}

internal class RecordingMessageHelper : MessageHelper {
    val dialogs = mutableListOf<String>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) = Unit

    override fun showSnackBar(
        iconType: IconType,
        messageRes: Int,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) = Unit

    override fun showOneButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        buttonText: String,
        onClickButton: (() -> Unit)?,
    ) {
        dialogs += descText
    }

    override fun showTwoButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        leftButtonText: String,
        onClickLeftButton: (() -> Unit)?,
        rightButtonText: String,
        onClickRightButton: (() -> Unit)?,
    ) = Unit
}

internal class FakeResourceHelper : ResourceHelper {
    override fun getString(resource: StringResource): String = ""
}

internal class FakeTTIHelper : TTIHelper {
    override fun startTTITracking(page: TTIPage) = Unit
    override fun startTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITracking() = Unit
    override fun shotTTILogging() = Unit
    override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
}
