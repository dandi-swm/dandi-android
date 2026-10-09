package com.dandi.nyummy.mailbox.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.presentation.R
import com.dandi.nyummy.mailbox.presentation.model.labelRes

/**
 * 문의 상세. 목록에서 누른 내 문의(유형, 보낸 시각, 내용)를 먼저 보여 주고,
 * 그 아래 카드에 냐미 팀 답장을 둔다. 아직 답장이 없으면 같은 카드에 기다리는 중이라고만 적는다.
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
                .padding(top = NyummyTheme.spacing.s16, bottom = NyummyTheme.spacing.s24),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s24),
        ) {
            MyInquiry(inquiry)
            ReplyCard(inquiry.answer)
        }
    }
}

/** 내가 보낸 문의. 목록 행처럼 위에 유형과 보낸 시각을 두고 아래에 전문을 쓴다. */
@Composable
private fun MyInquiry(inquiry: InquiryDetailUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
        MetaRow(
            label = stringResource(inquiry.category.labelRes),
            time = inquiry.createdAtLabel,
            isLabelEmphasized = false,
        )
        NyummyText(
            text = inquiry.content,
            style = NyummyTheme.typography.bodyL,
        )
    }
}

/** 냐미 팀 답장 카드. 답장이 없으면 같은 자리에 기다리는 중이라고만 쓴다. */
@Composable
private fun ReplyCard(answer: InquiryAnswerUiModel?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.surfaceSunken, RoundedCornerShape(NyummyTheme.radius.m))
            .padding(
                start = NyummyTheme.spacing.s16,
                end = NyummyTheme.spacing.s16,
                top = CardTopPadding,
                bottom = NyummyTheme.spacing.s16,
            ),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        if (answer != null) {
            MetaRow(
                label = stringResource(R.string.mailbox_detail_reply_from),
                time = answer.answeredAtLabel,
                isLabelEmphasized = true,
            )
            NyummyText(
                text = answer.content,
                style = NyummyTheme.typography.bodyM,
            )
        } else {
            NyummyText(
                text = stringResource(R.string.mailbox_status_waiting),
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.tertiary,
            )
        }
    }
}

/** 왼쪽 라벨(유형이나 보낸 사람)과 오른쪽 시각. */
@Composable
private fun MetaRow(label: String, time: String, isLabelEmphasized: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MetaGap),
    ) {
        NyummyText(
            text = label,
            style = NyummyTheme.typography.labelS,
            color = if (isLabelEmphasized) NyummyTheme.colors.content.brand else NyummyTheme.colors.content.tertiary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        NyummyText(
            text = time,
            style = NyummyTheme.typography.labelS,
            color = NyummyTheme.colors.content.tertiary,
            maxLines = 1,
        )
    }
}

private val CardTopPadding = 14.dp

private val MetaGap = 6.dp

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
                    content = "히스토리에서 사진을 크게 보고 싶어요. 식사 사진을 눌렀을 때 화면 가득 보여 주면 좋겠어요.",
                    createdAtLabel = "9월 28일 오후 7:05",
                    answer = null,
                ),
            ),
            onIntent = {},
        )
    }
}
