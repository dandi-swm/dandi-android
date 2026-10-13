package com.dandi.nyummy.settings.domain

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
import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class FakeSettingsRepository : SettingsRepository {
    var mealTimes: MealTimesVO = MealTimesVO.default
    var getMealTimesError: Exception? = null
    var saveMealTimesError: Exception? = null
    val savedMealTimes = mutableListOf<MealTimesVO>()

    var notificationSettings: NotificationSettingsVO = NotificationSettingsVO(isMarketingEnabled = true)
    var getNotificationSettingsError: Exception? = null
    var updateNotificationSettingsError: Exception? = null
    val notificationChanges = mutableListOf<NotificationSettingsChange>()

    override suspend fun getNotificationSettings(): NotificationSettingsVO {
        getNotificationSettingsError?.let { throw it }
        return notificationSettings
    }

    override suspend fun updateNotificationSettings(change: NotificationSettingsChange) {
        updateNotificationSettingsError?.let { throw it }
        notificationChanges += change
    }

    override suspend fun getMealTimes(): MealTimesVO {
        getMealTimesError?.let { throw it }
        return mealTimes
    }

    override suspend fun saveMealTimes(mealTimes: MealTimesVO) {
        saveMealTimesError?.let { throw it }
        savedMealTimes += mealTimes
    }

    var withdrawError: Exception? = null
    var withdrawCount = 0

    override suspend fun withdraw() {
        withdrawCount++
        withdrawError?.let { throw it }
    }
}

internal class RecordingMessageHelper : MessageHelper {
    data class SnackBarCall(
        val iconType: IconType,
        val messageText: String,
        val callToActionText: String?,
        val onClickCTA: (() -> Unit)?,
    )

    data class DialogCall(val descText: String, val cantIgnore: Boolean, val onClickButton: (() -> Unit)?)

    val snackBars = mutableListOf<SnackBarCall>()
    val dialogs = mutableListOf<DialogCall>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) {
        snackBars += SnackBarCall(iconType, messageText, callToActionText, onClickCTA)
    }

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
        dialogs += DialogCall(descText, cantIgnore, onClickButton)
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

internal class RecordingNavigationHelper : NavigationHelper {
    var initialCount = 0
    var backCount = 0

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) = Unit
    override fun navigateDeepLink(route: NavRoute) = Unit
    override fun navigateToBack() {
        backCount++
    }

    override fun navigateToAsRoot(page: Page) = Unit
    override fun navigateToInitial() {
        initialCount++
    }

    override fun navigateToExternalLink(url: String) = Unit
}

internal object FakeResourceHelper : ResourceHelper {
    override fun getString(resource: StringResource): String = ""
}

internal object FakeTTIHelper : TTIHelper {
    override fun startTTITracking(page: TTIPage) = Unit
    override fun startTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITracking() = Unit
    override fun shotTTILogging() = Unit
    override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
}

/** 서버 에러 바디의 `code` 를 cause 로 담는 실제 변환(BaseRemoteDataSource)과 같은 형태로 만든다. */
internal fun httpException(code: Int, errorCode: String? = null): HttpResponseException = HttpResponseException(
    status = HttpResponseStatus.create(code),
    rawCode = code,
    errorRequestUrl = "https://test/api/v1/users/me",
    msg = "Http Request Failed ($code)",
    cause = errorCode?.let(::Throwable),
)
