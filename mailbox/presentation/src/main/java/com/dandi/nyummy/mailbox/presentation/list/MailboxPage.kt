package com.dandi.nyummy.mailbox.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgressSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMailRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySwipeToDelete
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.jank.JankScrollWatcher
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.presentation.R
import com.dandi.nyummy.mailbox.presentation.model.labelRes
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 우편함. 1차는 내가 보낸 문의 목록만 보여 준다(아이템 탭은 2차).
 * 상단 바 오른쪽과 빈 화면에서 문의하기로 간다.
 */
@Composable
fun MailboxPage(
    modifier: Modifier = Modifier,
    viewModel: MailboxViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 문의를 보내고 돌아오면 새 문의가 맨 위에 보여야 하므로 화면이 보일 때마다 다시 읽는다.
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(MailboxIntent.ScreenResumed)
        onPauseOrDispose { }
    }

    MailboxScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun MailboxScreen(
    uiState: MailboxUIState,
    onIntent: (MailboxIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        NyummyTopBar(
            title = stringResource(R.string.mailbox_title),
            onBackClick = { onIntent(MailboxIntent.ClickBack) },
            trailing = {
                NyummyTextButton(
                    text = stringResource(R.string.mailbox_write_action),
                    onClick = { onIntent(MailboxIntent.ClickWrite) },
                )
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (uiState.isEmpty) {
                MailboxEmpty(
                    onWriteClick = { onIntent(MailboxIntent.ClickWrite) },
                    modifier = Modifier.align(EmptyAlignment),
                )
            } else {
                val listState = rememberLazyListState()
                JankScrollWatcher(listState)
                LoadMoreEffect(
                    listState = listState,
                    itemCount = uiState.inquiries.size,
                    onReachEnd = { onIntent(MailboxIntent.ReachListEnd) },
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = ContentMaxWidth),
                    contentPadding = PaddingValues(
                        start = NyummyTheme.spacing.gutter,
                        end = NyummyTheme.spacing.gutter,
                        top = ListTopGap,
                        bottom = NyummyTheme.spacing.s24,
                    ),
                ) {
                    items(uiState.inquiries, key = { it.inquiryId }) { row ->
                        NyummySwipeToDelete(
                            onDelete = { onIntent(MailboxIntent.DeleteInquiry(row.inquiryId)) },
                            modifier = Modifier.animateItem(),
                        ) {
                            NyummyMailRow(
                                type = stringResource(row.category.labelRes),
                                time = row.timeLabel,
                                title = row.title,
                                status = stringResource(
                                    if (row.isAnswered) R.string.mailbox_status_answered else R.string.mailbox_status_waiting,
                                ),
                                isStatusEmphasized = row.isAnswered,
                                onClick = { onIntent(MailboxIntent.ClickInquiry(row.inquiryId)) },
                            )
                        }
                    }
                    if (uiState.isLoadingMore) {
                        item(key = LOADING_MORE_KEY) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = NyummyTheme.spacing.s16),
                                contentAlignment = Alignment.Center,
                            ) {
                                NyummyCircularProgress(size = NyummyCircularProgressSize.S)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 마지막 문의 행이 [LOAD_MORE_THRESHOLD]행 안쪽으로 들어오면 [onReachEnd]를 부른다.
 *
 * 로딩 표시 행은 세지 않는다. 그래야 다음 묶음 읽기가 실패해 로딩 표시가 사라져도 곧바로 다시 부르지 않고,
 * 사용자가 다시 스크롤하거나 목록이 늘었을 때만 부른다. 중복 요청은 ViewModel이 거른다.
 */
@Composable
private fun LoadMoreEffect(
    listState: LazyListState,
    itemCount: Int,
    onReachEnd: () -> Unit,
) {
    val currentItemCount by rememberUpdatedState(itemCount)
    val currentOnReachEnd by rememberUpdatedState(onReachEnd)
    LaunchedEffect(listState) {
        snapshotFlow {
            val count = currentItemCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            count to lastVisible.coerceAtMost(count - 1)
        }
            .distinctUntilChanged()
            .filter { (count, lastVisible) -> count > 0 && lastVisible >= count - 1 - LOAD_MORE_THRESHOLD }
            .collect { currentOnReachEnd() }
    }
}

/** 보낸 문의가 없을 때. 캐릭터 없이 제목, 안내, 문의하기 글자 버튼만 둔다. */
@Composable
private fun MailboxEmpty(
    onWriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = NyummyTheme.spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EmptyGap),
    ) {
        NyummyText(
            text = stringResource(R.string.mailbox_empty_title),
            style = NyummyTheme.typography.titleS,
        )
        NyummyText(
            text = stringResource(R.string.mailbox_empty_body),
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.tertiary,
        )
        NyummyTextButton(
            text = stringResource(R.string.mailbox_write_action),
            onClick = onWriteClick,
            trailingIcon = CommonR.drawable.nyummy_ic_chevron_right,
        )
    }
}

/** 끝에서 몇 행 전에 다음 묶음을 읽기 시작할지. */
private const val LOAD_MORE_THRESHOLD = 3

private const val LOADING_MORE_KEY = "loadingMore"

/** 상단 바와 첫 행 사이. */
private val ListTopGap = 12.dp

private val EmptyGap = 6.dp

/** 빈 화면 묶음은 가운데보다 조금 위에 둔다. */
private val EmptyAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.38f)

private val ContentMaxWidth = 480.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun MailboxScreenPreview() {
    NyummyTheme {
        MailboxScreen(
            uiState = MailboxUIState(
                inquiries = persistentListOf(
                    InquiryRowUiModel(3, InquiryCategory.BUG_REPORT, "사진을 찍은 뒤 기록이 사라졌어요", "10월 4일", true),
                    InquiryRowUiModel(2, InquiryCategory.SUGGESTION, "히스토리에서 사진을 크게 보고 싶어요", "9월 28일", false),
                ),
                isLoaded = true,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun MailboxScreenEmptyPreview() {
    NyummyTheme {
        MailboxScreen(uiState = MailboxUIState(isLoaded = true), onIntent = {})
    }
}
