package com.dandi.nyummy.room.presentation

import com.dandi.nyummy.cat.domain.CatRepository
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.cat.entity.CatAnimationVO
import com.dandi.nyummy.cat.entity.CatMotionVO
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.SpriteClipVO
import com.dandi.nyummy.cat.entity.SpriteFrameVO
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import com.dandi.nyummy.room.domain.RoomRepository
import com.dandi.nyummy.room.entity.OwnedRoomItemPageVO
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.entity.RoomPlacementVO
import com.dandi.nyummy.room.entity.RoomVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/*
 * 마이룸·놓아보기 ViewModel 테스트가 같이 쓰는 가짜들.
 */

/** 메모리로 동작하는 튜토리얼 진행 기록. */
internal class FakeTutorialHelper : TutorialHelper {
    val pending = mutableSetOf<TutorialStep>()

    override suspend fun pendingSteps(): Set<TutorialStep> = pending.toSet()
    override suspend fun startAfterMeal() {
        pending.clear()
        pending += TutorialStep.entries
    }

    override suspend fun complete(step: TutorialStep) {
        pending -= step
    }

    override suspend fun skipToHealthStory() {
        pending.clear()
        pending += TutorialStep.HEALTH_STORY
    }

    override suspend fun finish() = pending.clear()
    override suspend fun clear() = pending.clear()
    override suspend fun isMealTutorialRequired() = false
    override suspend fun setMealTutorialRequired(required: Boolean) = Unit
}

/** 마이룸 API를 흉내 내는 저장소. 불러오기와 저장 실패를 따로 정할 수 있다. */
internal class FakeRoomRepository : RoomRepository {
    var room = RoomVO()
    var ownedPage = OwnedRoomItemPageVO()
    var ownedError: Exception? = null
    var loadError: Exception? = null
    var saveError: Exception? = null
    var roomRequests = 0
    val ownedRequests = mutableListOf<Triple<RoomItemCategory, Long?, Int>>()
    val saved = mutableListOf<List<RoomPlacementVO>>()

    override suspend fun getRoom(size: Int): RoomVO {
        roomRequests++
        loadError?.let { throw it }
        return room
    }

    override suspend fun getOwnedItems(category: RoomItemCategory, cursor: Long?, size: Int): OwnedRoomItemPageVO {
        ownedRequests += Triple(category, cursor, size)
        ownedError?.let { throw it }
        return ownedPage
    }

    override suspend fun saveRoom(placements: List<RoomPlacementVO>) {
        saveError?.let { throw it }
        saved += placements
    }
}

/** 마이룸(ROOM_IDLE) 동작이 둘인 내 고양이 애니메이션. 시트 주소에 동작 번호를 넣어 구분한다. */
internal class FakeCatRepository : CatRepository {
    var error: Exception? = null

    override suspend fun refreshWeight(): Boolean = false

    override suspend fun rename(name: String) = Unit

    override suspend fun getAnimations(): CatAnimationSetVO {
        error?.let { throw it }
        return CatAnimationSetVO(
            weight = "NORMAL",
            animations = mapOf(
                CatState.ROOM_IDLE to CatAnimationVO(
                    state = CatState.ROOM_IDLE,
                    frame = SpriteFrameVO(width = 136, height = 136, framesPerRow = 4, durationMs = 100),
                    motions = listOf(
                        CatMotionVO(name = "idle-0", weight = 1.0, clips = listOf(SpriteClipVO(url = "playful-0", frames = 8))),
                        CatMotionVO(name = "idle-1", weight = 1.0, clips = listOf(SpriteClipVO(url = "playful-1", frames = 8))),
                    ),
                ),
            ),
        )
    }
}

internal fun httpException(code: Int, errorCode: String? = null): HttpResponseException = HttpResponseException(
    status = HttpResponseStatus.create(code),
    rawCode = code,
    errorRequestUrl = "https://test/api/v1/rooms",
    msg = "Http Request Failed ($code)",
    cause = errorCode?.let(::Throwable),
)

internal class RecordingMessageHelper : MessageHelper {
    data class SnackBarCall(
        val iconType: IconType,
        val messageText: String,
        val callToActionText: String?,
        val onClickCTA: (() -> Unit)?,
    )

    data class DialogCall(val descText: String, val cantIgnore: Boolean, val onClickButton: (() -> Unit)?)

    data class TwoButtonDialogCall(
        val descText: String,
        val leftButtonText: String,
        val onClickLeft: (() -> Unit)?,
        val rightButtonText: String,
        val onClickRight: (() -> Unit)?,
    )

    val snackBars = mutableListOf<SnackBarCall>()
    val twoButtonDialogs = mutableListOf<TwoButtonDialogCall>()
    val dialogs = mutableListOf<DialogCall>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showToast(messageRes: Int) = Unit
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
    ) {
        twoButtonDialogs += TwoButtonDialogCall(descText, leftButtonText, onClickLeftButton, rightButtonText, onClickRightButton)
    }
}

internal class RecordingNavigationHelper : NavigationHelper {
    var initialCount = 0
    var backCount = 0
    val pages = mutableListOf<Page>()

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) {
        pages += page
    }

    override fun navigateToAppLink(link: String, asRoot: Boolean) = Unit
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
