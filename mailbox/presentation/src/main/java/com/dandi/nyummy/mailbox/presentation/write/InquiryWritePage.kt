package com.dandi.nyummy.mailbox.presentation.write

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomCta
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRowTrailing
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySelectField
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextArea
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.presentation.R
import com.dandi.nyummy.mailbox.presentation.model.labelRes

/**
 * 문의하기. 유형을 고르고 내용을 써서 보낸다. 답장은 우편함으로 온다.
 * 빈 내용으로 보내면 입력칸 아래에 오류를 띄우고, 보내면 스낵바와 함께 우편함으로 돌아간다.
 */
@Composable
fun InquiryWritePage(
    modifier: Modifier = Modifier,
    viewModel: InquiryWriteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InquiryWriteScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun InquiryWriteScreen(
    uiState: InquiryWriteUIState,
    onIntent: (InquiryWriteIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 시스템 바 인셋은 앱 Scaffold가 이미 밀어 두었다. 여기서는 키보드가 내비게이션 바보다 올라온 만큼만 버튼을 올린다.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas)
            .consumeWindowInsets(WindowInsets.systemBars)
            .imePadding(),
    ) {
        NyummyTopBar(
            title = stringResource(R.string.mailbox_write_title),
            onBackClick = { onIntent(InquiryWriteIntent.ClickBack) },
        )
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
            NyummyText(
                text = stringResource(R.string.mailbox_write_reply_notice),
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.secondary,
            )
            Spacer(Modifier.height(NyummyTheme.spacing.s20))
            NyummySelectField(
                value = stringResource(uiState.category.labelRes),
                label = stringResource(R.string.mailbox_write_category_label),
                onClick = { onIntent(InquiryWriteIntent.ClickCategory) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(NyummyTheme.spacing.s20))
            NyummyTextArea(
                value = uiState.content,
                onValueChange = { onIntent(InquiryWriteIntent.ChangeContent(it)) },
                label = stringResource(R.string.mailbox_write_content_label),
                placeholder = stringResource(R.string.mailbox_write_content_placeholder),
                errorMessage = if (uiState.isContentEmptyError) {
                    stringResource(R.string.mailbox_write_content_empty_error)
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        NyummyBottomCta(
            primaryText = stringResource(R.string.mailbox_write_send),
            onPrimaryClick = { onIntent(InquiryWriteIntent.ClickSend) },
            primaryEnabled = !uiState.isSending,
        )
    }

    if (uiState.isCategorySheetVisible) {
        InquiryCategorySheet(
            selected = uiState.category,
            onSelect = { onIntent(InquiryWriteIntent.SelectCategory(it)) },
            onDismiss = { onIntent(InquiryWriteIntent.DismissCategorySheet) },
        )
    }
}

/** 문의 유형 시트. 제목 아래에 라디오 행 세 개를 둔다. */
@Composable
private fun InquiryCategorySheet(
    selected: InquiryCategory,
    onSelect: (InquiryCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    NyummyBottomSheet(onDismissRequest = onDismiss) {
        Column {
            NyummyText(
                text = stringResource(R.string.mailbox_write_category_label),
                style = NyummyTheme.typography.titleM,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(NyummyTheme.spacing.s8))
            SheetCategories.forEach { category ->
                NyummyListRow(
                    title = stringResource(category.labelRes),
                    trailing = NyummyListRowTrailing.Radio(
                        selected = category == selected,
                        onClick = { onSelect(category) },
                    ),
                )
            }
        }
    }
}

/** 시트에 보여 줄 순서. */
private val SheetCategories = listOf(
    InquiryCategory.BUG_REPORT,
    InquiryCategory.QUESTION,
    InquiryCategory.SUGGESTION,
)

/** 상단 바와 안내 문구 사이. */
private val ContentTopGap = 8.dp

private val ContentMaxWidth = 480.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun InquiryWriteScreenPreview() {
    NyummyTheme {
        InquiryWriteScreen(
            uiState = InquiryWriteUIState(content = "사진을 찍은 뒤 기록이 사라졌어요. 다시 찍어도 같은 문제가 있어요."),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun InquiryWriteScreenErrorPreview() {
    NyummyTheme {
        InquiryWriteScreen(
            uiState = InquiryWriteUIState(isContentEmptyError = true),
            onIntent = {},
        )
    }
}
