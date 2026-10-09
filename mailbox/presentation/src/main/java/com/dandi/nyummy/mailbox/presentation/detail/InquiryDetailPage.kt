package com.dandi.nyummy.mailbox.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDivider
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.presentation.R
import com.dandi.nyummy.mailbox.presentation.model.labelRes

/**
 * 문의 상세. 답장이 왔으면 답장 제목, 시각, 본문 아래에 내가 보낸 문의를 둔다.
 * 아직 답장이 없으면 유형과 보낸 시각, 내가 보낸 문의만 보여 준다.
 */
@Composable
fun InquiryDetailPage(
    inquiryId: Long,
    modifier: Modifier = Modifier,
    viewModel: InquiryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(inquiryId) {
        viewModel.onIntent(InquiryDetailIntent.Enter(inquiryId))
    }

    InquiryDetailScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun InquiryDetailScreen(
    uiState: InquiryDetailUIState,
    onIntent: (InquiryDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        NyummyTopBar(
            title = stringResource(R.string.mailbox_title),
            onBackClick = { onIntent(InquiryDetailIntent.ClickBack) },
        )
        val inquiry = uiState.inquiry ?: return@Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = ContentMaxWidth)
                .padding(horizontal = NyummyTheme.spacing.gutter)
                .padding(top = ContentTopGap, bottom = NyummyTheme.spacing.s24),
        ) {
            val answer = inquiry.answer
            if (answer != null) {
                NyummyText(
                    text = stringResource(R.string.mailbox_detail_answer_label),
                    style = NyummyTheme.typography.labelS,
                    color = NyummyTheme.colors.content.brand,
                )
                Spacer(Modifier.height(NyummyTheme.spacing.s4))
                NyummyText(
                    text = stringResource(R.string.mailbox_detail_answer_title),
                    style = NyummyTheme.typography.titleL,
                )
                NyummyText(
                    text = answer.answeredAtLabel,
                    style = NyummyTheme.typography.bodyS,
                    color = NyummyTheme.colors.content.tertiary,
                )
                Spacer(Modifier.height(NyummyTheme.spacing.s16))
                NyummyDivider()
                Spacer(Modifier.height(NyummyTheme.spacing.s16))
                NyummyText(
                    text = answer.content,
                    style = NyummyTheme.typography.bodyL,
                )
                Spacer(Modifier.height(NyummyTheme.spacing.s24))
            } else {
                NyummyText(
                    text = stringResource(inquiry.category.labelRes),
                    style = NyummyTheme.typography.labelS,
                    color = NyummyTheme.colors.content.brand,
                )
                Spacer(Modifier.height(NyummyTheme.spacing.s4))
                NyummyText(
                    text = inquiry.createdAtLabel,
                    style = NyummyTheme.typography.bodyS,
                    color = NyummyTheme.colors.content.tertiary,
                )
                Spacer(Modifier.height(NyummyTheme.spacing.s16))
            }
            MyInquiryCard(content = inquiry.content)
        }
    }
}

/** 내가 보낸 문의 원문. 움푹한 바탕의 둥근 카드. */
@Composable
private fun MyInquiryCard(content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.surfaceSunken, RoundedCornerShape(NyummyTheme.radius.m))
            .padding(horizontal = NyummyTheme.spacing.s16, vertical = CardVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
    ) {
        NyummyText(
            text = stringResource(R.string.mailbox_detail_my_inquiry),
            style = NyummyTheme.typography.labelS,
            color = NyummyTheme.colors.content.tertiary,
        )
        NyummyText(
            text = content,
            style = NyummyTheme.typography.bodyM,
            color = NyummyTheme.colors.content.secondary,
        )
    }
}

/** 상단 바와 첫 줄 사이. */
private val ContentTopGap = 4.dp

private val CardVerticalPadding = 14.dp

private val ContentMaxWidth = 480.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun InquiryDetailAnsweredPreview() {
    NyummyTheme {
        InquiryDetailScreen(
            uiState = InquiryDetailUIState(
                inquiry = InquiryDetailUiModel(
                    category = InquiryCategory.BUG_REPORT,
                    content = "사진을 찍은 뒤 기록이 사라졌어요. 다시 찍어도 같은 문제가 있어요.",
                    createdAtLabel = "10월 4일 오후 9:12",
                    answer = InquiryAnswerUiModel(
                        content = "안녕하세요, 냐미 팀이에요.\n\n사진을 찍은 뒤 기록이 사라지는 문제를 확인했어요.\n\n알려 줘서 정말 고마워요.",
                        answeredAtLabel = "10월 5일 오후 12:42",
                    ),
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun InquiryDetailWaitingPreview() {
    NyummyTheme {
        InquiryDetailScreen(
            uiState = InquiryDetailUIState(
                inquiry = InquiryDetailUiModel(
                    category = InquiryCategory.SUGGESTION,
                    content = "히스토리에서 사진을 크게 보고 싶어요.",
                    createdAtLabel = "9월 28일 오후 7:05",
                    answer = null,
                ),
            ),
            onIntent = {},
        )
    }
}
