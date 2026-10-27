package com.dandi.nyummy.mailbox.presentation.list

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.coroutine.IoScope
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.entity.tutorial.TutorialStep
import com.dandi.nyummy.common.presentation.image.NyummyCdn
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.common.presentation.time.listTimeLabel
import com.dandi.nyummy.mailbox.domain.ClaimAllMailsUseCase
import com.dandi.nyummy.mailbox.domain.ClaimMailUseCase
import com.dandi.nyummy.mailbox.domain.DeleteMailUseCase
import com.dandi.nyummy.mailbox.domain.GetMailsUseCase
import com.dandi.nyummy.mailbox.domain.RestoreMailUseCase
import com.dandi.nyummy.mailbox.entity.MailCategory
import com.dandi.nyummy.mailbox.entity.MailVO
import com.dandi.nyummy.mailbox.entity.RewardVO
import com.dandi.nyummy.mailbox.presentation.model.mailImageUrl
import com.dandi.nyummy.mailbox.presentation.model.mailRemainingDays
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val getMails: GetMailsUseCase,
    private val claimMail: ClaimMailUseCase,
    private val claimAllMails: ClaimAllMailsUseCase,
    private val deleteMail: DeleteMailUseCase,
    private val restoreMail: RestoreMailUseCase,
    private val navigationHelper: NavigationHelper,
    private val messageHelper: MessageHelper,
    private val tutorialHelper: TutorialHelper,
    @IoScope private val ioScope: CoroutineScope,
) : MviViewModel<MailboxIntent, MailboxUIState, MailboxReducerEvent>(MailboxUIState.empty) {

    private var mailLoadJob: Job? = null
    private var mailLoadMoreJob: Job? = null
    private var tutorialJob: Job? = null

    override fun onIntent(intent: MailboxIntent) {
        when (intent) {
            MailboxIntent.ScreenResumed -> loadMails()
            MailboxIntent.ClickBack -> navigationHelper.navigateToBack()
            MailboxIntent.ReachMailListEnd -> loadMoreMails()
            is MailboxIntent.ClaimMail -> claim(intent.mailId)
            MailboxIntent.ClaimAllMails -> claimAll()
            is MailboxIntent.DeleteMail -> deleteMailRow(intent.mailId)
            is MailboxIntent.UndoDeleteMail -> undoDeleteMail(intent.row, intent.index)
            MailboxIntent.SkipBoxOpening,
            MailboxIntent.BoxOpeningBurstFinished,
            -> if (currentState.boxOpening?.phase == BoxOpeningPhase.Opening) {
                dispatch(MailboxReducerEvent.BoxOpeningRevealed)
            }
            MailboxIntent.BoxOpeningFailureShown -> finishFailedBoxOpening()
            MailboxIntent.CloseBoxOpening -> if (currentState.boxOpening?.phase == BoxOpeningPhase.Revealed) {
                dispatch(MailboxReducerEvent.BoxOpeningEnded)
                // 안내대로 상자를 열어 끝까지 봤다.
                if (currentState.tutorial?.isWaitingForBox == true) endTutorial { tutorialHelper.complete(TutorialStep.MAILBOX) }
            }
            MailboxIntent.TutorialAdvance -> if (currentState.tutorial?.boxMailId == null) {
                endTutorial { tutorialHelper.complete(TutorialStep.MAILBOX) }
            }
            MailboxIntent.TutorialTargetClicked -> currentState.tutorial?.let {
                dispatch(MailboxReducerEvent.TutorialChanged(it.copy(isWaitingForBox = true)))
            }
            MailboxIntent.TutorialSkip -> endTutorial { tutorialHelper.skipAll() }
            MailboxIntent.TutorialBack -> endTutorial { tutorialHelper.complete(TutorialStep.MAILBOX) }
        }
    }

    override fun reduce(state: MailboxUIState, event: MailboxReducerEvent): MailboxUIState =
        when (event) {
            is MailboxReducerEvent.BoxOpeningEvent -> state.copy(boxOpening = reduceBoxOpening(state.boxOpening, event))
            is MailboxReducerEvent.TutorialChanged -> state.copy(tutorial = event.tutorial)
            else -> state.copy(items = reduceItems(state.items, event))
        }

    private fun reduceBoxOpening(
        opening: BoxOpeningUiState?,
        event: MailboxReducerEvent.BoxOpeningEvent,
    ): BoxOpeningUiState? = when (event) {
        is MailboxReducerEvent.BoxOpeningStarted -> event.opening
        is MailboxReducerEvent.BoxOpeningOpened -> opening?.copy(
            phase = BoxOpeningPhase.Opening,
            closedImageUrl = event.closedImageUrl,
            openedImageUrl = event.openedImageUrl,
            rewards = event.rewards,
        )
        MailboxReducerEvent.BoxOpeningRevealed -> opening?.copy(phase = BoxOpeningPhase.Revealed)
        is MailboxReducerEvent.BoxOpeningFailed -> opening?.copy(phase = BoxOpeningPhase.Failed, failure = event.failure)
        MailboxReducerEvent.BoxOpeningEnded -> null
    }

    private fun reduceItems(items: MailItemsUiState, event: MailboxReducerEvent): MailItemsUiState =
        when (event) {
            is MailboxReducerEvent.MailsLoaded -> items.copy(
                mails = event.mails,
                isLoaded = true,
                nextCursor = event.nextCursor,
                isLoadingMore = false,
            )
            MailboxReducerEvent.MailLoadMoreStarted -> items.copy(isLoadingMore = true)
            is MailboxReducerEvent.MoreMailsLoaded -> {
                // 이어 붙이기만 한다. 다시 정렬하면 LazyColumn key가 흔들린다.
                val knownIds = items.mails.mapTo(HashSet()) { it.mailId }
                items.copy(
                    mails = (items.mails + event.mails.filterNot { it.mailId in knownIds }).toImmutableList(),
                    nextCursor = event.nextCursor,
                    isLoadingMore = false,
                )
            }
            MailboxReducerEvent.MailLoadMoreStopped -> items.copy(isLoadingMore = false)
            is MailboxReducerEvent.MailClaimStarted ->
                items.copy(claimingMailIds = (items.claimingMailIds + event.mailId).toPersistentSet())
            is MailboxReducerEvent.MailClaimFinished -> items.copy(
                mails = if (event.isClaimed) items.mails.markClaimed { it.mailId == event.mailId } else items.mails,
                claimingMailIds = (items.claimingMailIds - event.mailId).toPersistentSet(),
            )
            MailboxReducerEvent.ClaimAllStarted -> items.copy(isClaimingAll = true)
            is MailboxReducerEvent.ClaimAllFinished -> items.copy(
                mails = if (event.isClaimed) items.mails.markClaimed { true } else items.mails,
                isClaimingAll = false,
            )
            is MailboxReducerEvent.MailRemoved ->
                items.copy(mails = items.mails.filterNot { it.mailId == event.mailId }.toImmutableList())
            is MailboxReducerEvent.MailRestored -> {
                val rows = items.mails.filterNot { it.mailId == event.row.mailId }.toPersistentList()
                items.copy(mails = rows.add(event.index.coerceIn(0, rows.size), event.row))
            }
            is MailboxReducerEvent.BoxOpeningEvent,
            is MailboxReducerEvent.TutorialChanged,
            -> items
        }

    /** 우편 첫 묶음부터 다시 읽는다. 이미 받아 둔 만큼(서버 최대치까지)을 한 번에 읽어 스크롤 위치를 지킨다. */
    private fun loadMails() {
        mailLoadJob?.cancel()
        if (mailLoadMoreJob?.isActive == true) {
            mailLoadMoreJob?.cancel()
            dispatch(MailboxReducerEvent.MailLoadMoreStopped)
        }
        val size = currentState.items.mails.size.coerceIn(GetMailsUseCase.PAGE_SIZE, GetMailsUseCase.MAX_PAGE_SIZE)
        mailLoadJob = viewModelScope.launch {
            // 실패하면 공통 처리는 UseCase가 하고, 화면은 받아 둔 목록을 그대로 둔다.
            getMails(size = size).onSuccess { page ->
                dispatch(MailboxReducerEvent.MailsLoaded(page.mails.toMailRowUiModels(), page.nextCursor))
                updateTutorial()
            }
        }
    }

    /**
     * 우편함 안내가 남아 있으면 띄운다. 받지 않은 상자가 있으면 맨 위 상자의 열기를 가리키고, 없으면 우편함을 소개한다.
     * 이미 띄워 두었으면 그대로 둔다.
     */
    private fun updateTutorial() {
        if (currentState.tutorial != null || tutorialJob?.isActive == true) return
        tutorialJob = viewModelScope.launch {
            if (!tutorialHelper.isPending(TutorialStep.MAILBOX)) return@launch
            val box = currentState.items.mails.firstOrNull { !it.isClaimed && it.category == MailCategory.REWARD_BOX }
            dispatch(MailboxReducerEvent.TutorialChanged(MailboxTutorialUiState(boxMailId = box?.mailId)))
        }
    }

    /** 튜토리얼을 닫고 [record]로 남긴다(우편함 안내 마침 또는 전체 건너뛰기). */
    private fun endTutorial(record: suspend () -> Unit) {
        if (currentState.tutorial == null) return
        dispatch(MailboxReducerEvent.TutorialChanged(null))
        tutorialJob = viewModelScope.launch { record() }
    }

    /** 우편 다음 묶음을 읽어 이어 붙인다. 첫 묶음을 읽는 중이거나 이미 읽는 중이면 기다린다. */
    private fun loadMoreMails() {
        val items = currentState.items
        val cursor = items.nextCursor
        if (!items.isLoaded || items.isLoadingMore || cursor == null || mailLoadJob?.isActive == true) return
        dispatch(MailboxReducerEvent.MailLoadMoreStarted)
        mailLoadMoreJob = viewModelScope.launch {
            getMails(cursor = cursor)
                .onSuccess { page ->
                    dispatch(MailboxReducerEvent.MoreMailsLoaded(page.mails.toMailRowUiModels(), page.nextCursor))
                }
                .onFailure { dispatch(MailboxReducerEvent.MailLoadMoreStopped) }
        }
    }

    /**
     * 우편 하나를 받는다. 받는 동안 버튼을 막고, 끝나면 그 행을 받았어요로 바꾼다.
     * 보상 상자는 누르는 즉시 상자 열기 연출을 띄우고, 응답을 기다리는 동안 상자가 무대에서 기다린다.
     */
    private fun claim(mailId: Long) {
        val items = currentState.items
        val row = items.mails.firstOrNull { it.mailId == mailId } ?: return
        if (row.isClaimed || items.isClaiming(mailId) || currentState.boxOpening != null) return
        val opensBox = row.category == MailCategory.REWARD_BOX
        dispatch(MailboxReducerEvent.MailClaimStarted(mailId))
        if (opensBox) {
            dispatch(
                MailboxReducerEvent.BoxOpeningStarted(
                    BoxOpeningUiState(
                        mailId = mailId,
                        closedImageUrl = boxImageUrl(row.imageId, opened = false),
                        openedImageUrl = boxImageUrl(row.imageId, opened = true),
                        mailTitle = row.title,
                    ),
                ),
            )
        }
        viewModelScope.launch {
            claimMail(mailId)
                .onSuccess { reward ->
                    if (reward == null) {
                        // 이미 받았거나 없거나 기한이 지난 우편이다. 지금 상태를 다시 받아 보여 준다.
                        dispatch(MailboxReducerEvent.MailClaimFinished(mailId, isClaimed = false))
                        if (opensBox) {
                            dispatch(MailboxReducerEvent.BoxOpeningFailed(BoxOpeningFailure.AlreadyGone))
                        } else {
                            messageHelper.showSnackBar(messageText = ALREADY_GONE_MESSAGE)
                            loadMails()
                        }
                    } else {
                        dispatch(MailboxReducerEvent.MailClaimFinished(mailId, isClaimed = true))
                        if (opensBox) {
                            // 응답의 상자 번호가 우선이다. 없으면 목록의 번호를 그대로 쓴다.
                            val boxImageId = reward.rewardBox?.imageId ?: row.imageId
                            dispatch(
                                MailboxReducerEvent.BoxOpeningOpened(
                                    closedImageUrl = boxImageUrl(boxImageId, opened = false),
                                    openedImageUrl = boxImageUrl(boxImageId, opened = true),
                                    rewards = reward.toBoxRewards(),
                                ),
                            )
                        }
                        // TODO: 코인, 아이템 우편의 보상 연출. 방식은 따로 정한다.
                    }
                }
                .onFailure {
                    dispatch(MailboxReducerEvent.MailClaimFinished(mailId, isClaimed = false))
                    if (opensBox) {
                        dispatch(MailboxReducerEvent.BoxOpeningFailed(BoxOpeningFailure.Error))
                    } else {
                        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = CLAIM_FAILED_MESSAGE)
                    }
                }
        }
    }

    /** 받지 못한 상자가 무대에서 걷혔다. 연출을 닫고, 받지 못한 까닭에 맞는 안내를 띄운다. */
    private fun finishFailedBoxOpening() {
        val opening = currentState.boxOpening ?: return
        if (opening.phase != BoxOpeningPhase.Failed) return
        dispatch(MailboxReducerEvent.BoxOpeningEnded)
        when (opening.failure) {
            BoxOpeningFailure.AlreadyGone -> {
                messageHelper.showSnackBar(messageText = ALREADY_GONE_MESSAGE)
                // 안내하던 상자가 사라졌다. 목록을 다시 받은 뒤 새로 고른다.
                if (currentState.tutorial?.isWaitingForBox == true) dispatch(MailboxReducerEvent.TutorialChanged(null))
                loadMails()
            }
            else -> {
                messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = CLAIM_FAILED_MESSAGE)
                // 받지 못했으니 같은 상자의 열기를 다시 가리킨다.
                currentState.tutorial?.takeIf { it.isWaitingForBox }?.let {
                    dispatch(MailboxReducerEvent.TutorialChanged(it.copy(isWaitingForBox = false)))
                }
            }
        }
    }

    /**
     * 안 받은 우편을 모두 받는다. 상자 하나를 열 때와 같은 연출로, 누르는 즉시 상자가 무대에 나와 응답을 기다리고
     * 받은 보상의 합계를 상자에서 꺼내 보여 준다. 상자는 받을 우편 중 가장 높은 등급이고, 상자 우편이 없으면 그림 없이 빛만 보인다.
     * 서버는 불러오지 않은 묶음까지 받으므로, 화면의 우편을 받았어요로 바꾼 뒤 목록을 다시 받는다.
     */
    private fun claimAll() {
        val items = currentState.items
        if (items.isClaimingAll || !items.hasUnclaimed || currentState.boxOpening != null) return
        // 상자 번호가 클수록 높은 등급이다(1 브론즈, 2 실버, 3 골드).
        val boxImageId = items.mails
            .filter { !it.isClaimed && it.category == MailCategory.REWARD_BOX }
            .mapNotNull { it.imageId }
            .maxOrNull()
        dispatch(MailboxReducerEvent.ClaimAllStarted)
        dispatch(
            MailboxReducerEvent.BoxOpeningStarted(
                BoxOpeningUiState(
                    mailId = CLAIM_ALL_OPENING_ID,
                    closedImageUrl = boxImageUrl(boxImageId, opened = false),
                    openedImageUrl = boxImageUrl(boxImageId, opened = true),
                    mailTitle = "",
                ),
            ),
        )
        viewModelScope.launch {
            claimAllMails()
                .onSuccess { reward ->
                    val rewards = reward.toBoxRewards()
                    if (rewards.isEmpty()) {
                        // 그사이 다른 기기에서 받았거나 기한이 지나 받을 우편이 없었다. 무대가 걷히면 안내하고 목록을 다시 받는다.
                        dispatch(MailboxReducerEvent.ClaimAllFinished(isClaimed = false))
                        dispatch(MailboxReducerEvent.BoxOpeningFailed(BoxOpeningFailure.AlreadyGone))
                        return@onSuccess
                    }
                    dispatch(MailboxReducerEvent.ClaimAllFinished(isClaimed = true))
                    dispatch(
                        MailboxReducerEvent.BoxOpeningOpened(
                            closedImageUrl = boxImageUrl(boxImageId, opened = false),
                            openedImageUrl = boxImageUrl(boxImageId, opened = true),
                            rewards = rewards,
                        ),
                    )
                    loadMails()
                }
                .onFailure {
                    dispatch(MailboxReducerEvent.ClaimAllFinished(isClaimed = false))
                    dispatch(MailboxReducerEvent.BoxOpeningFailed(BoxOpeningFailure.Error))
                }
        }
    }

    /** 받은 우편을 목록에서 바로 빼고 서버에서 지운다. 실패하면 원래 자리에 되살리고 안내한다. 안 받은 우편은 지울 수 없다. */
    private fun deleteMailRow(mailId: Long) {
        val index = currentState.items.mails.indexOfFirst { it.mailId == mailId }
        if (index < 0) return
        val row = currentState.items.mails[index]
        if (!row.isClaimed) return
        dispatch(MailboxReducerEvent.MailRemoved(mailId))
        viewModelScope.launch {
            deleteMail(mailId)
                .onSuccess {
                    messageHelper.showSnackBar(
                        messageText = DELETED_MESSAGE,
                        callToActionText = UNDO_LABEL,
                        onClickCTA = { onIntent(MailboxIntent.UndoDeleteMail(row, index)) },
                    )
                }
                .onFailure {
                    dispatch(MailboxReducerEvent.MailRestored(row, index))
                    messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = DELETE_FAILED_MESSAGE)
                }
        }
    }

    /** 우편 되돌리기. 화면에는 바로 되살리고, 스낵바는 화면을 떠난 뒤에도 눌릴 수 있어 복구 요청은 [ioScope]에서 보낸다. */
    private fun undoDeleteMail(row: MailRowUiModel, index: Int) {
        dispatch(MailboxReducerEvent.MailRestored(row, index))
        ioScope.launch {
            restoreMail(row.mailId).onFailure {
                dispatch(MailboxReducerEvent.MailRemoved(row.mailId))
                messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = RESTORE_FAILED_MESSAGE)
            }
        }
    }

    private companion object {
        /** 모두 받기 연출의 [BoxOpeningUiState.mailId]. 서버 우편 id는 양수라 겹치지 않는다. */
        const val CLAIM_ALL_OPENING_ID = -1L
        const val ALREADY_GONE_MESSAGE = "이미 받았거나 기한이 지난 우편이에요"
        const val CLAIM_FAILED_MESSAGE = "보상을 받지 못했어요. 잠시 후 다시 시도해 주세요."
        const val DELETED_MESSAGE = "우편을 지웠어요"
        const val UNDO_LABEL = "되돌리기"
        const val DELETE_FAILED_MESSAGE = "우편을 지우지 못했어요. 잠시 후 다시 시도해 주세요."
        const val RESTORE_FAILED_MESSAGE = "우편을 되돌리지 못했어요. 잠시 후 다시 시도해 주세요."
    }
}

/** 받은 우편으로 바꾼다. 보상 상자는 열린 상자 그림으로 바뀐다. */
private fun ImmutableList<MailRowUiModel>.markClaimed(predicate: (MailRowUiModel) -> Boolean): ImmutableList<MailRowUiModel> =
    map { if (predicate(it)) it.claimed() else it }.toImmutableList()

private fun MailRowUiModel.claimed(): MailRowUiModel = copy(
    isClaimed = true,
    remainingDays = null,
    imageUrl = if (category == MailCategory.REWARD_BOX && imageId != null) {
        mailImageUrl(category, imageId, isClaimed = true)
    } else {
        imageUrl
    },
)

/** 상자 그림 주소. 상자 번호가 없으면 빈 값(그림 없이 빛과 효과만 보인다). */
private fun boxImageUrl(imageId: Long?, opened: Boolean): String = when {
    imageId == null -> ""
    opened -> NyummyCdn.rewardBoxOpenedUrl(imageId)
    else -> NyummyCdn.rewardBoxClosedUrl(imageId)
}

/** 상자에서 나온 보상. 코인을 먼저, 아이템은 받은 순서대로 둔다. */
private fun RewardVO.toBoxRewards(): ImmutableList<BoxRewardUiModel> = buildList {
    coin?.takeIf { it.quantity > 0 }?.let { coin ->
        add(BoxRewardUiModel.Coin(imageUrl = coin.imageId?.let(NyummyCdn::coinUrl).orEmpty(), quantity = coin.quantity))
    }
    items.forEach { item ->
        add(
            BoxRewardUiModel.Item(
                imageUrl = item.imageId?.let(NyummyCdn::itemUrl).orEmpty(),
                name = item.name,
                quantity = item.quantity,
            ),
        )
    }
}.toImmutableList()

private fun List<MailVO>.toMailRowUiModels(): ImmutableList<MailRowUiModel> {
    val now = System.currentTimeMillis()
    return map { it.toRowUiModel(now) }.toImmutableList()
}

private fun MailVO.toRowUiModel(nowMillis: Long) = MailRowUiModel(
    mailId = mailId,
    category = category,
    title = title,
    content = content,
    imageUrl = mailImageUrl(category, imageId, isClaimed),
    timeLabel = listTimeLabel(createdAtMillis, nowMillis),
    remainingDays = expiresAtMillis?.takeUnless { isClaimed }?.let { mailRemainingDays(it, nowMillis) },
    isClaimed = isClaimed,
    imageId = imageId,
)
