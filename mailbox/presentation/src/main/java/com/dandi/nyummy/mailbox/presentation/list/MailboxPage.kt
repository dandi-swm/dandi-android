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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomCta
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyCircularProgressSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPose
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPoseImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyRewardRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySwipeToDelete
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.jank.JankScrollWatcher
import com.dandi.nyummy.mailbox.presentation.component.LoadMoreEffect
import com.dandi.nyummy.mailbox.entity.MailCategory
import com.dandi.nyummy.mailbox.presentation.R
import com.dandi.nyummy.mailbox.presentation.model.MAIL_EXPIRING_DAYS
import kotlinx.collections.immutable.persistentListOf

/**
 * 우편함. 받은 보상, 코인, 아이템을 받는 곳이다. 문의는 설정의 문의하기로 옮겼다.
 * 기능 플래그가 꺼져 있으면 홈 우편 버튼이 이 화면으로 오지 않는다.
 */
@Composable
fun MailboxPage(
    modifier: Modifier = Modifier,
    viewModel: MailboxViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 상자를 열거나 다른 화면에서 돌아오면 받은 상태가 바뀌어 있을 수 있어 화면이 보일 때마다 다시 읽는다.
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
        )
        MailItemsContent(
            items = uiState.items,
            onIntent = onIntent,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
    uiState.boxOpening?.let { opening ->
        // 상자마다 새 장면으로 시작한다(닫히는 중 상태가 다음 상자로 넘어가지 않게).
        key(opening.mailId) {
            RewardBoxOpeningDialog(opening = opening, onIntent = onIntent)
        }
    }
}

/**
 * 받은 우편(보상, 코인, 아이템) 목록과 보관 기간 안내, 안 받은 우편이 있으면 아래에 모두 받기 버튼.
 * 받은 우편만 밀어서 지울 수 있다(안 받은 보상은 기한이 지나면 저절로 사라진다).
 */
@Composable
private fun MailItemsContent(
    items: MailItemsUiState,
    onIntent: (MailboxIntent) -> Unit,
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
                        top = ListTopGap,
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

private const val LOADING_MORE_KEY = "loadingMore"

private const val RETENTION_NOTICE_KEY = "retentionNotice"

/** 상단 바와 첫 행 사이. */
private val ListTopGap = 12.dp

private val EmptyGap = 6.dp

/** 빈 화면 묶음은 가운데보다 조금 위에 둔다. */
private val EmptyAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.38f)

private val ContentMaxWidth = 480.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MailboxScreenPreview() {
    NyummyTheme {
        MailboxScreen(
            uiState = MailboxUIState(
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
private fun MailboxScreenEmptyPreview() {
    NyummyTheme {
        MailboxScreen(uiState = MailboxUIState(items = MailItemsUiState(isLoaded = true)), onIntent = {})
    }
}
