package com.dandi.nyummy.settings.presentation.inquiry.list

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgressSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMailRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySwipeToDelete
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyLoadMoreEffect
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.jank.JankScrollWatcher
import com.dandi.nyummy.settings.entity.InquiryCategory
import com.dandi.nyummy.settings.presentation.R
import com.dandi.nyummy.settings.presentation.inquiry.labelRes
import kotlinx.collections.immutable.persistentListOf

/**
 * 문의 내역. 설정의 문의하기에서 들어온다. 내가 보낸 문의를 최신순으로 보여 주고, 답장이 왔는지 알려 준다.
 * 상단 바 오른쪽과 빈 화면에서 문의하기로 간다. 행을 왼쪽으로 밀면 지우고, 스낵바에서 되돌린다.
 */
@Composable
fun InquiryListPage(
    modifier: Modifier = Modifier,
    viewModel: InquiryListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 문의를 보내고 돌아오면 새 문의가 맨 위에 보여야 하므로 화면이 보일 때마다 다시 읽는다.
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(InquiryListIntent.ScreenResumed)
        onPauseOrDispose { }
    }

    InquiryListScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun InquiryListScreen(
    uiState: InquiryListUIState,
    onIntent: (InquiryListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        NyummyTopBar(
            title = stringResource(R.string.inquiry_list_title),
            onBackClick = { onIntent(InquiryListIntent.ClickBack) },
            trailing = {
                NyummyTextButton(
                    text = stringResource(R.string.inquiry_write_action),
                    onClick = { onIntent(InquiryListIntent.ClickWrite) },
                )
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (uiState.isEmpty) {
                InquiryListEmpty(
                    onWriteClick = { onIntent(InquiryListIntent.ClickWrite) },
                    modifier = Modifier.align(EmptyAlignment),
                )
            } else {
                val listState = rememberLazyListState()
                JankScrollWatcher(listState)
                NyummyLoadMoreEffect(
                    listState = listState,
                    itemCount = uiState.inquiries.size,
                    onReachEnd = { onIntent(InquiryListIntent.ReachListEnd) },
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
                            onDelete = { onIntent(InquiryListIntent.DeleteInquiry(row.inquiryId)) },
                            modifier = Modifier.animateItem(),
                        ) {
                            NyummyMailRow(
                                type = stringResource(row.category.labelRes),
                                time = row.timeLabel,
                                title = row.title,
                                status = stringResource(
                                    if (row.isAnswered) R.string.inquiry_status_answered else R.string.inquiry_status_waiting,
                                ),
                                isStatusEmphasized = row.isAnswered,
                                onClick = { onIntent(InquiryListIntent.ClickInquiry(row.inquiryId)) },
                                isUnread = row.hasUnreadAnswer,
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

/** 보낸 문의가 없을 때(v2 State View). 캐릭터 없이 제목, 안내, 작은 톤 "문의하기" 버튼. */
@Composable
private fun InquiryListEmpty(
    onWriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = NyummyTheme.spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        NyummyText(
            text = stringResource(R.string.inquiry_empty_title),
            style = NyummyTheme.typography.titleS,
        )
        NyummyText(
            text = stringResource(R.string.inquiry_empty_body),
            style = NyummyTheme.typography.bodyM,
            color = NyummyTheme.colors.content.secondary,
        )
        NyummyButton(
            text = stringResource(R.string.inquiry_write_action),
            onClick = onWriteClick,
            style = NyummyButtonStyle.Tonal,
            size = NyummyButtonSize.S,
            modifier = Modifier.padding(top = NyummyTheme.spacing.s8),
        )
    }
}

private const val LOADING_MORE_KEY = "loadingMore"

/** 상단 바와 첫 행 사이. */
private val ListTopGap = 12.dp

/** 빈 화면 묶음은 가운데보다 조금 위에 둔다. */
private val EmptyAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.38f)

private val ContentMaxWidth = 480.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun InquiryListScreenPreview() {
    NyummyTheme {
        InquiryListScreen(
            uiState = InquiryListUIState(
                inquiries = persistentListOf(
                    InquiryRowUiModel(3, InquiryCategory.BUG_REPORT, "사진을 찍은 뒤 기록이 사라졌어요", "10월 4일", true, true),
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
private fun InquiryListScreenEmptyPreview() {
    NyummyTheme {
        InquiryListScreen(uiState = InquiryListUIState(isLoaded = true), onIntent = {})
    }
}
