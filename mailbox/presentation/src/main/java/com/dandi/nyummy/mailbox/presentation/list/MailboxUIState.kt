package com.dandi.nyummy.mailbox.presentation.list

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.mailbox.entity.MailCategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * 우편함 UI 상태. 받은 우편(보상, 코인, 아이템)만 보여 준다. 문의는 설정의 문의하기로 옮겼다.
 *
 * @property items 받은 우편 목록 상태.
 * @property boxOpening 보상 상자를 여는 연출. 열고 있지 않으면 null.
 * @property tutorial 첫 사용자 튜토리얼. 보여 주지 않으면 null.
 */
data class MailboxUIState(
    val items: MailItemsUiState = MailItemsUiState(),
    val boxOpening: BoxOpeningUiState? = null,
    val tutorial: MailboxTutorialUiState? = null,
) : UiState {

    companion object {
        val empty = MailboxUIState()
    }
}

/**
 * 받은 우편 목록 상태.
 *
 * @property isLoaded 한 번이라도 우편 목록을 받았다. 받기 전에는 빈 화면을 보여 주지 않는다.
 * @property nextCursor 다음 묶음을 읽을 cursor. 마지막 묶음까지 받았으면 null.
 * @property claimingMailIds 받기 요청이 진행 중인 우편. 버튼을 막아 두 번 받지 않게 한다.
 * @property isClaimingAll 모두 받기 요청이 진행 중.
 */
@Immutable
data class MailItemsUiState(
    val mails: ImmutableList<MailRowUiModel> = persistentListOf(),
    val isLoaded: Boolean = false,
    val nextCursor: Long? = null,
    val isLoadingMore: Boolean = false,
    val claimingMailIds: ImmutableSet<Long> = persistentSetOf(),
    val isClaimingAll: Boolean = false,
) {
    val isEmpty: Boolean
        get() = isLoaded && mails.isEmpty()

    /** 불러온 우편 중 안 받은 게 있다. 모두 받기 버튼을 보여 준다. */
    val hasUnclaimed: Boolean
        get() = mails.any { !it.isClaimed }

    fun isClaiming(mailId: Long): Boolean = isClaimingAll || mailId in claimingMailIds
}

/**
 * 우편 한 행.
 *
 * @property content 보낸 이유나 받은 내용(예 "첫 기록 보상", "상점에서 샀어요").
 * @property remainingDays 받지 않으면 사라질 때까지 남은 날. 받은 우편이거나 사라지지 않는 우편이면 null.
 * @property imageId 우편 그림 번호(보상 상자는 상자 번호). 받은 뒤 그림을 바꾸거나 상자 열기 연출에 쓴다.
 */
@Immutable
data class MailRowUiModel(
    val mailId: Long,
    val category: MailCategory,
    val title: String,
    val content: String,
    val imageUrl: String,
    val timeLabel: String,
    val remainingDays: Int?,
    val isClaimed: Boolean,
    val imageId: Long? = null,
)

/** 상자 열기 연출의 단계. */
enum class BoxOpeningPhase {
    /** 닫힌 상자가 무대에 나와 서버 응답을 기다린다. */
    Waiting,

    /** 받은 보상이 왔다. 상자가 흔들리다 열린다. */
    Opening,

    /** 열린 상자와 받은 보상을 보여 준다. 계속하기로 닫는다. */
    Revealed,

    /** 받지 못했다. 상자가 고개를 젓고 무대가 걷힌 뒤 안내를 띄운다. */
    Failed,
}

/** 상자를 받지 못한 까닭. 무대가 걷힌 뒤 띄울 안내가 다르다. */
enum class BoxOpeningFailure {
    /** 이미 받았거나 기한이 지나 없는 우편이다. */
    AlreadyGone,

    /** 네트워크나 서버 문제로 받지 못했다. */
    Error,
}

/**
 * 보상 상자 열기 연출. 보상 모두 받기도 같은 연출로 받은 보상의 합계를 보여 준다.
 *
 * @property mailId 여는 우편. 모두 받기면 우편 id와 겹치지 않는 음수다.
 * @property closedImageUrl 닫힌 상자 그림. 등장하고 흔들릴 때 보인다.
 * @property openedImageUrl 열린 상자 그림. 터지는 순간부터 보인다.
 * @property mailTitle 우편 제목. 공개 화면의 제목 아래에 보인다.
 * @property rewards 상자에서 나온 보상(코인 먼저, 아이템은 받은 순서). 응답이 오기 전에는 비어 있다.
 */
@Immutable
data class BoxOpeningUiState(
    val mailId: Long,
    val closedImageUrl: String,
    val openedImageUrl: String,
    val mailTitle: String,
    val phase: BoxOpeningPhase = BoxOpeningPhase.Waiting,
    val rewards: ImmutableList<BoxRewardUiModel> = persistentListOf(),
    val failure: BoxOpeningFailure? = null,
)

/** 상자에서 나온 보상 하나. */
@Immutable
sealed interface BoxRewardUiModel {
    /** 그림 주소. 없으면 빈 값이고, 코인은 앱에 든 코인 그림을 대신 쓴다. */
    val imageUrl: String

    @Immutable
    data class Coin(override val imageUrl: String, val quantity: Int) : BoxRewardUiModel

    @Immutable
    data class Item(override val imageUrl: String, val name: String, val quantity: Int) : BoxRewardUiModel
}
