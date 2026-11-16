package com.dandi.nyummy.meal.presentation

import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.meal.domain.MealFoodRepository
import com.dandi.nyummy.meal.entity.FoundFoodVO
import com.dandi.nyummy.meal.entity.MealFoodConfirmVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** 식사 기록 화면 테스트들이 같이 쓰는 가짜 구현. */

internal class RecordingNavigationHelper : NavigationHelper {
    val calls = mutableListOf<String>()

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) {
        calls += "to:${page.toRoute().path}"
    }
    override fun navigateToAppLink(link: String, asRoot: Boolean) = Unit
    override fun navigateToBack() {
        calls += "back"
    }
    override fun navigateToAsRoot(page: Page) = Unit
    override fun navigateToInitial() = Unit
    override fun navigateToExternalLink(url: String) = Unit
}

internal class FakeResourceHelper : ResourceHelper {
    override fun getString(resource: StringResource): String = resource.name
}

/** 리소스로 띄운 스낵바만 모아 둔다. */
internal class RecordingMessageHelper : MessageHelper {
    val snackBarRes = mutableListOf<Int>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showToast(messageRes: Int) = Unit
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
    ) {
        snackBarRes += messageRes
    }

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

internal class FakeTTIHelper : TTIHelper {
    override fun startTTITracking(page: TTIPage) = Unit
    override fun startTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITracking() = Unit
    override fun shotTTILogging() = Unit
    override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
}

/** 찾은 음식 확인 API를 흉내 낸다. 무엇을 보냈고 취소했는지 모아 둔다. */
internal class FakeMealFoodRepository : MealFoodRepository {
    var confirm = MealFoodConfirmVO()
    var loadError: Exception? = null
    var submitError: Exception? = null
    var cancelError: Exception? = null

    /** 값이 있으면 이 값이 채워질 때까지 보내기 응답을 미룬다. */
    var submitGate: CompletableDeferred<Unit>? = null
    var loadCount = 0
    val submitted = mutableListOf<Pair<Long, List<FoundFoodVO>>>()
    val canceled = mutableListOf<Long>()

    override suspend fun getFoundFoods(mealId: Long): MealFoodConfirmVO {
        loadCount++
        loadError?.let { throw it }
        return confirm
    }

    override suspend fun submitFoods(mealId: Long, foods: List<FoundFoodVO>) {
        submitGate?.await()
        submitError?.let { throw it }
        submitted += mealId to foods
    }

    override suspend fun cancelMeal(mealId: Long) {
        cancelError?.let { throw it }
        canceled += mealId
    }
}
