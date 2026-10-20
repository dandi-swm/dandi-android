package com.dandi.nyummy.room.domain

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

/** 마이룸 API를 흉내 내는 저장소. [error]가 있으면 모든 요청이 그 예외를 던진다. */
internal class FakeRoomRepository(
    var room: RoomVO = RoomVO(),
    var ownedPage: OwnedRoomItemPageVO = OwnedRoomItemPageVO(),
    var error: Exception? = null,
) : RoomRepository {
    val roomRequests = mutableListOf<Int>()
    val ownedRequests = mutableListOf<Triple<RoomItemCategory, Long?, Int>>()
    val saved = mutableListOf<List<RoomPlacementVO>>()

    override suspend fun getRoom(size: Int): RoomVO {
        roomRequests += size
        error?.let { throw it }
        return room
    }

    override suspend fun getOwnedItems(category: RoomItemCategory, cursor: Long?, size: Int): OwnedRoomItemPageVO {
        ownedRequests += Triple(category, cursor, size)
        error?.let { throw it }
        return ownedPage
    }

    override suspend fun saveRoom(placements: List<RoomPlacementVO>) {
        saved += placements
        error?.let { throw it }
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
    errorRequestUrl = "https://test/api/v1/rooms",
    msg = "Http Request Failed ($code)",
    cause = errorCode?.let(::Throwable),
)
