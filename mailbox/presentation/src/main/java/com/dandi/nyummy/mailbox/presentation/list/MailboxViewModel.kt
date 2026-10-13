package com.dandi.nyummy.mailbox.presentation.list

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.coroutine.IoScope
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.mailbox.domain.DeleteInquiryUseCase
import com.dandi.nyummy.mailbox.domain.GetInquiriesUseCase
import com.dandi.nyummy.mailbox.domain.InquiryDetailPage
import com.dandi.nyummy.mailbox.domain.InquiryWritePage
import com.dandi.nyummy.mailbox.domain.RestoreInquiryUseCase
import com.dandi.nyummy.mailbox.entity.InquiryVO
import com.dandi.nyummy.mailbox.presentation.model.inquiryListTimeLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val getInquiries: GetInquiriesUseCase,
    private val deleteInquiry: DeleteInquiryUseCase,
    private val restoreInquiry: RestoreInquiryUseCase,
    private val navigationHelper: NavigationHelper,
    private val messageHelper: MessageHelper,
    @IoScope private val ioScope: CoroutineScope,
) : MviViewModel<MailboxIntent, MailboxUIState, MailboxReducerEvent>(MailboxUIState.empty) {

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null

    override fun onIntent(intent: MailboxIntent) {
        when (intent) {
            MailboxIntent.ScreenResumed -> loadInquiries()
            MailboxIntent.ClickBack -> navigationHelper.navigateToBack()
            MailboxIntent.ReachListEnd -> loadMoreInquiries()
            MailboxIntent.ClickWrite -> navigationHelper.navigateTo(InquiryWritePage)
            is MailboxIntent.ClickInquiry -> navigationHelper.navigateTo(InquiryDetailPage(intent.inquiryId))
            is MailboxIntent.DeleteInquiry -> delete(intent.inquiryId)
            is MailboxIntent.UndoDelete -> undoDelete(intent.row, intent.index)
        }
    }

    override fun reduce(state: MailboxUIState, event: MailboxReducerEvent): MailboxUIState =
        when (event) {
            is MailboxReducerEvent.InquiriesLoaded -> state.copy(
                inquiries = event.inquiries,
                isLoaded = true,
                nextCursor = event.nextCursor,
                isLoadingMore = false,
            )
            MailboxReducerEvent.LoadMoreStarted -> state.copy(isLoadingMore = true)
            is MailboxReducerEvent.MoreInquiriesLoaded -> {
                // 이어 붙이기만 한다. 다시 정렬하면 LazyColumn key가 흔들린다.
                val knownIds = state.inquiries.mapTo(HashSet()) { it.inquiryId }
                state.copy(
                    inquiries = (state.inquiries + event.inquiries.filterNot { it.inquiryId in knownIds })
                        .toImmutableList(),
                    nextCursor = event.nextCursor,
                    isLoadingMore = false,
                )
            }
            MailboxReducerEvent.LoadMoreStopped -> state.copy(isLoadingMore = false)
            is MailboxReducerEvent.InquiryRemoved ->
                state.copy(inquiries = state.inquiries.filterNot { it.inquiryId == event.inquiryId }.toImmutableList())
            is MailboxReducerEvent.InquiryRestored -> {
                val rows = state.inquiries.filterNot { it.inquiryId == event.row.inquiryId }.toPersistentList()
                state.copy(inquiries = rows.add(event.index.coerceIn(0, rows.size), event.row))
            }
        }

    /**
     * 첫 묶음부터 다시 읽는다. 상세를 보고 돌아왔을 때 목록이 줄어 스크롤 위치를 잃지 않도록,
     * 이미 받아 둔 만큼(서버 최대치까지)을 한 번에 읽는다.
     */
    private fun loadInquiries() {
        loadJob?.cancel()
        // 다시 읽는 동안 끝난 이전 다음 묶음 요청이 새 목록 뒤에 붙지 않게 멈춘다.
        if (loadMoreJob?.isActive == true) {
            loadMoreJob?.cancel()
            dispatch(MailboxReducerEvent.LoadMoreStopped)
        }
        val size = currentState.inquiries.size
            .coerceIn(GetInquiriesUseCase.PAGE_SIZE, GetInquiriesUseCase.MAX_PAGE_SIZE)
        loadJob = viewModelScope.launch {
            // 실패하면 공통 처리(다이얼로그)는 UseCase가 하고, 화면은 받아 둔 목록을 그대로 둔다.
            getInquiries(size = size).onSuccess { page ->
                dispatch(MailboxReducerEvent.InquiriesLoaded(page.inquiries.toRowUiModels(), page.nextCursor))
            }
        }
    }

    /** 다음 묶음을 읽어 이어 붙인다. 첫 묶음을 읽는 중이거나 이미 읽는 중이면 기다린다. */
    private fun loadMoreInquiries() {
        val state = currentState
        val cursor = state.nextCursor
        if (!state.isLoaded || state.isLoadingMore || cursor == null || loadJob?.isActive == true) return
        dispatch(MailboxReducerEvent.LoadMoreStarted)
        loadMoreJob = viewModelScope.launch {
            getInquiries(cursor = cursor)
                .onSuccess { page ->
                    dispatch(MailboxReducerEvent.MoreInquiriesLoaded(page.inquiries.toRowUiModels(), page.nextCursor))
                }
                // 공통 오류는 UseCase가 안내한다. 다시 끝으로 스크롤하면 다시 시도한다.
                .onFailure { dispatch(MailboxReducerEvent.LoadMoreStopped) }
        }
    }

    /** 목록에서 바로 빼고 서버에서 지운다. 실패하면 원래 자리에 되살리고 안내한다. */
    private fun delete(inquiryId: Long) {
        val index = currentState.inquiries.indexOfFirst { it.inquiryId == inquiryId }
        if (index < 0) return
        val row = currentState.inquiries[index]
        dispatch(MailboxReducerEvent.InquiryRemoved(inquiryId))
        viewModelScope.launch {
            deleteInquiry(inquiryId)
                .onSuccess {
                    messageHelper.showSnackBar(
                        messageText = DELETED_MESSAGE,
                        callToActionText = UNDO_LABEL,
                        onClickCTA = { onIntent(MailboxIntent.UndoDelete(row, index)) },
                    )
                }
                .onFailure {
                    dispatch(MailboxReducerEvent.InquiryRestored(row, index))
                    messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = DELETE_FAILED_MESSAGE)
                }
        }
    }

    /**
     * 되돌리기. 서버는 소프트 삭제라 복구 API로 되살린다. 화면에는 바로 되살리고, 실패하면 다시 빼고 안내한다.
     * 스낵바는 화면을 떠난 뒤에도 눌릴 수 있어 복구 요청은 앱 전역 [ioScope]에서 보낸다.
     */
    private fun undoDelete(row: InquiryRowUiModel, index: Int) {
        dispatch(MailboxReducerEvent.InquiryRestored(row, index))
        ioScope.launch {
            restoreInquiry(row.inquiryId).onFailure {
                dispatch(MailboxReducerEvent.InquiryRemoved(row.inquiryId))
                messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = RESTORE_FAILED_MESSAGE)
            }
        }
    }

    private companion object {
        const val DELETED_MESSAGE = "우편을 지웠어요"
        const val UNDO_LABEL = "되돌리기"
        const val DELETE_FAILED_MESSAGE = "우편을 지우지 못했어요. 잠시 후 다시 시도해 주세요."
        const val RESTORE_FAILED_MESSAGE = "우편을 되돌리지 못했어요. 잠시 후 다시 시도해 주세요."
    }
}

private fun List<InquiryVO>.toRowUiModels(): ImmutableList<InquiryRowUiModel> {
    val now = System.currentTimeMillis()
    return map { it.toRowUiModel(now) }.toImmutableList()
}

private fun InquiryVO.toRowUiModel(nowMillis: Long) = InquiryRowUiModel(
    inquiryId = inquiryId,
    category = category,
    title = title,
    timeLabel = inquiryListTimeLabel(createdAtMillis, nowMillis),
    isAnswered = isAnswered,
)
