package com.dandi.nyummy.mailbox.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomCta
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgressSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMailRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPose
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPoseImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyRewardRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySwipeToDelete
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTabs
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.jank.JankScrollWatcher
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.entity.MailCategory
import com.dandi.nyummy.mailbox.presentation.R
import com.dandi.nyummy.mailbox.presentation.model.MAIL_EXPIRING_DAYS
import com.dandi.nyummy.mailbox.presentation.model.labelRes
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * 우편함. 아이템 탭(받은 보상, 코인, 아이템)과 문의 탭(내가 보낸 문의)이 있다.
 * 아이템 탭은 원격 구성 기능 플래그로 켜고, 꺼져 있으면 탭 없이 문의 목록만 보여 준다.
 * 상단 바 오른쪽과 문의 빈 화면에서 문의하기로 간다.
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
        if (uiState.isItemTabEnabled) {
            NyummyTabs(
                tabs = persistentListOf(
                    stringResource(R.string.mailbox_tab_item),
                    stringResource(R.string.mailbox_tab_inquiry),
                ),
                selectedIndex = uiState.selectedTab.ordinal,
                onSelect = { onIntent(MailboxIntent.SelectTab(MailboxTab.entries[it])) },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = ContentMaxWidth)
                    .padding(horizontal = NyummyTheme.spacing.gutter),
            )
        }
        val listTopGap = if (uiState.isItemTabEnabled) TabListTopGap else ListTopGap
        when (uiState.selectedTab) {
            MailboxTab.ITEM -> MailItemsContent(
                items = uiState.items,
                onIntent = onIntent,
                listTopGap = listTopGap,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            MailboxTab.INQUIRY -> InquiriesContent(
                uiState = uiState,
                onIntent = onIntent,
                listTopGap = listTopGap,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

/**
 * 아이템 탭. 받은 우편(보상, 코인, 아이템) 목록과 보관 기간 안내, 안 받은 우편이 있으면 아래에 모두 받기 버튼.
 * 받은 우편만 밀어서 지울 수 있다(안 받은 보상은 기한이 지나면 저절로 사라진다).
 */
@Composable
private fun MailItemsContent(
    items: MailItemsUiState,
    onIntent: (MailboxIntent) -> Unit,
    listTopGap: Dp,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (items.isEmpty) {
                MailItemsEmpty(modifier = Modifier.align(EmptyAlignment))
            } else if (items.isLoaded) {
                val listState = rememberLazyListState()
                JankScrollWatcher(listState)
                LoadMoreEffect(
                    listState = listState,
                    itemCount = items.mails.size,
                    onReachEnd = { onIntent(MailboxIntent.ReachMailListEnd) },
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
                        top = listTopGap,
                        bottom = NyummyTheme.spacing.s24,
                    ),
                ) {
                    items(items.mails, key = { it.mailId }) { row ->
                        NyummySwipeToDelete(
                            onDelete = { onIntent(MailboxIntent.DeleteMail(row.mailId)) },
                            modifier = Modifier.animateItem(),
                            enabled = row.isClaimed,
                        ) {
                            val isRewardBox = row.category == MailCategory.REWARD_BOX
                            NyummyRewardRow(
                                title = row.title,
                                source = row.content,
                                time = row.timeLabel,
                                imageModel = row.imageUrl,
                                isClaimed = row.isClaimed,
                                actionText = stringResource(
                                    if (isRewardBox) R.string.mailbox_item_open else R.string.mailbox_item_claim,
                                ),
                                claimedText = stringResource(R.string.mailbox_item_claimed),
                                onActionClick = { onIntent(MailboxIntent.ClaimMail(row.mailId)) },
                                largeImage = isRewardBox,
                                remaining = row.remainingDays?.let {
                                    stringResource(R.string.mailbox_item_remaining_days, it)
                                },
                                isExpiring = row.remainingDays != null && row.remainingDays <= MAIL_EXPIRING_DAYS,
                                actionEnabled = !items.isClaiming(row.mailId),
                            )
                        }
                    }
                    if (items.isLoadingMore) {
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
                    item(key = RETENTION_NOTICE_KEY) {
                        Column {
                            Spacer(Modifier.height(NyummyTheme.spacing.s20))
                            NyummyText(
                                text = stringResource(R.string.mailbox_item_retention_notice),
                                style = NyummyTheme.typography.bodyS,
                                color = NyummyTheme.colors.content.tertiary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
        if (items.hasUnclaimed) {
            NyummyBottomCta(
                primaryText = stringResource(R.string.mailbox_item_claim_all),
                onPrimaryClick = { onIntent(MailboxIntent.ClaimAllMails) },
                primaryEnabled = !items.isClaimingAll,
            )
        }
    }
}

/** 도착한 우편이 없을 때. 잠자는 냐미와 안내. */
@Composable
private fun MailItemsEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = NyummyTheme.spacing.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EmptyGap),
    ) {
        NyummyPoseImage(pose = NyummyPose.Sleep, size = NyummyTheme.size.characterM)
        NyummyText(
            text = stringResource(R.string.mailbox_item_empty_title),
            style = NyummyTheme.typography.titleS,
        )
        NyummyText(
            text = stringResource(R.string.mailbox_item_empty_body),
            style = NyummyTheme.typography.bodyS,
            color = NyummyTheme.colors.content.tertiary,
        )
    }
}

/** 문의 탭. 보낸 문의 목록, 없으면 문의하기 안내. */
@Composable
private fun InquiriesContent(
    uiState: MailboxUIState,
    onIntent: (MailboxIntent) -> Unit,
    listTopGap: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
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
                    top = listTopGap,
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

/**
 * 마지막 행이 [LOAD_MORE_THRESHOLD]행 안쪽으로 들어오면 [onReachEnd]를 부른다.
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

private const val RETENTION_NOTICE_KEY = "retentionNotice"

/** 상단 바와 첫 행 사이. */
private val ListTopGap = 12.dp

/** 탭과 첫 행 사이. */
private val TabListTopGap = 4.dp

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
private fun MailboxScreenEmptyPreview() {
    NyummyTheme {
        MailboxScreen(uiState = MailboxUIState(isLoaded = true), onIntent = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MailboxItemTabPreview() {
    NyummyTheme {
        MailboxScreen(
            uiState = MailboxUIState.initial(isItemTabEnabled = true).copy(
                items = MailItemsUiState(
                    mails = persistentListOf(
                        MailRowUiModel(4, MailCategory.REWARD_BOX, "퀘스트 상자", "식사 1번 기록하기", "", "오후 3:10", 6, false),
                        MailRowUiModel(3, MailCategory.COIN, "코인 100개", "첫 기록 보상", "", "오후 1:05", 1, false),
                        MailRowUiModel(2, MailCategory.ITEM, "가을 담요", "상점에서 샀어요", "", "어제", 30, false),
                        MailRowUiModel(1, MailCategory.COIN, "코인 300개", "7일 연속 기록 보상", "", "9월 28일", null, true),
                    ),
                    isLoaded = true,
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MailboxItemTabEmptyPreview() {
    NyummyTheme {
        MailboxScreen(
            uiState = MailboxUIState.initial(isItemTabEnabled = true).copy(items = MailItemsUiState(isLoaded = true)),
            onIntent = {},
        )
    }
}
