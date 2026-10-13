package com.dandi.nyummy.settings.presentation

import com.dandi.nyummy.auth.domain.AuthRepository
import com.dandi.nyummy.auth.entity.AuthTokenVO
import com.dandi.nyummy.auth.entity.EmailChallengeVO
import com.dandi.nyummy.auth.entity.EmailVerificationPurpose
import com.dandi.nyummy.auth.entity.EmailVerifiedVO
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.auth.entity.SocialCredentialVO
import com.dandi.nyummy.auth.entity.SocialLoginVO
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.domain.SettingsRepository
import com.dandi.nyummy.settings.entity.NotificationSettingsChange
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** 식사 시각 읽기를 요청마다 따로 정할 수 있는 가짜 저장소. 정해 둔 응답이 없으면 [mealTimes]를 바로 준다. */
internal class FakeSettingsRepository : SettingsRepository {
    var mealTimes: MealTimesVO = MealTimesVO.default
    var getMealTimesError: Exception? = null
    var getMealTimesCount = 0
    val mealTimesResponses = ArrayDeque<suspend () -> MealTimesVO>()

    var saveMealTimesError: Exception? = null
    val savedMealTimes = mutableListOf<MealTimesVO>()
    /** 보내기 시작한 알림 저장 요청. [savedNotifications]는 응답까지 끝난 요청만 담는다. */
    val requestedNotifications = mutableListOf<NotificationSettingsChange>()
    val savedNotifications = mutableListOf<NotificationSettingsChange>()
    var notificationSettings: NotificationSettingsVO = NotificationSettingsVO.default
    var getNotificationSettingsError: Exception? = null

    /** 알림 저장마다 따로 정할 수 있는 응답. 비어 있으면 바로 성공한다. */
    val notificationUpdateResponses = ArrayDeque<suspend (NotificationSettingsChange) -> Unit>()


    override suspend fun getNotificationSettings(): NotificationSettingsVO {
        getNotificationSettingsError?.let { throw it }
        return notificationSettings
    }

    override suspend fun updateNotificationSettings(change: NotificationSettingsChange) {
        requestedNotifications += change
        notificationUpdateResponses.removeFirstOrNull()?.invoke(change)
        savedNotifications += change
    }

    override suspend fun getMealTimes(): MealTimesVO {
        getMealTimesCount++
        mealTimesResponses.removeFirstOrNull()?.let { return it() }
        getMealTimesError?.let { throw it }
        return mealTimes
    }

    override suspend fun saveMealTimes(mealTimes: MealTimesVO) {
        saveMealTimesError?.let { throw it }
        savedMealTimes += mealTimes
    }

    var withdrawCount = 0

    /** 탈퇴 요청의 응답. null 이면 바로 성공한다. */
    var withdrawResponse: (suspend () -> Unit)? = null

    override suspend fun withdraw() {
        withdrawCount++
        withdrawResponse?.invoke()
    }
}

internal class RecordingMessageHelper : MessageHelper {
    data class SnackBarCall(val messageText: String, val onClickCTA: (() -> Unit)?)

    val snackBars = mutableListOf<SnackBarCall>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) {
        snackBars += SnackBarCall(messageText, onClickCTA)
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
    ) = Unit

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
    val pages = mutableListOf<Page>()
    var backCount = 0

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) {
        pages += page
    }

    override fun navigateDeepLink(route: NavRoute) = Unit
    override fun navigateToBack() {
        backCount++
    }

    override fun navigateToAsRoot(page: Page) = Unit
    override fun navigateToInitial() = Unit
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

/** 설정 화면이 로그아웃 UseCase를 만들 때만 쓴다. 이 테스트들은 로그아웃을 부르지 않는다. */
internal object FakeAuthRepository : AuthRepository {
    override suspend fun setOnboardingIncomplete(incomplete: Boolean) = Unit
    override suspend fun socialLogin(credential: SocialCredentialVO): SocialLoginVO = error("not used")
    override suspend fun login(email: String, password: String): AuthTokenVO = error("not used")
    override suspend fun signUp(
        verifiedToken: String,
        password: String?,
        confirmPassword: String?,
        nickname: String,
        gender: Gender?,
        birth: String?,
        height: Int?,
        weight: Int?,
    ) = Unit

    override suspend fun requestEmailVerification(email: String, purpose: EmailVerificationPurpose): EmailChallengeVO =
        error("not used")

    override suspend fun confirmEmailVerification(authCode: String, emailChallengeToken: String): EmailVerifiedVO =
        error("not used")

    override suspend fun logout() = Unit
}

internal object FakePushTokenHelper : PushTokenHelper {
    override fun register(token: String?) = Unit
    override suspend fun deleteToken() = Unit
}
