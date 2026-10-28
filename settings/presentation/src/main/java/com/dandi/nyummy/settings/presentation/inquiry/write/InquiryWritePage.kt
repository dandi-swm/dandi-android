package com.dandi.nyummy.settings.presentation.inquiry.write

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomCta
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRowTrailing
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySelectField
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextArea
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextField
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyHiddenWhileTyping
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClearFocusOnTap
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.settings.entity.InquiryCategory
import com.dandi.nyummy.settings.presentation.R
import com.dandi.nyummy.settings.presentation.inquiry.labelRes
import java.io.File
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 문의하기. 유형을 고르고 내용을 써서 보낸다. 사진은 한 장까지 붙일 수 있다. 답장은 문의 내역에서 본다.
 * 빈 내용으로 보내면 입력칸 아래에 오류를 띄우고, 보내면 스낵바와 함께 문의 내역으로 돌아간다.
 */
@Composable
fun InquiryWritePage(
    modifier: Modifier = Modifier,
    viewModel: InquiryWriteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 연타로 선택기가 겹쳐 뜨지 않도록 열려 있는 동안을 기억한다(선택기 뒤에서 재생성돼도 유지).
    var isImagePickerOpen by rememberSaveable { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        isImagePickerOpen = false
        viewModel.onIntent(InquiryWriteIntent.ImagePicked(uri?.toString()))
    }

    InquiryWriteScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onAddImageClick = {
            if (!isImagePickerOpen) {
                isImagePickerOpen = true
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        },
        modifier = modifier,
    )
}

@Composable
internal fun InquiryWriteScreen(
    uiState: InquiryWriteUIState,
    onIntent: (InquiryWriteIntent) -> Unit,
    onAddImageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 시스템 바 인셋은 앱 Scaffold가 이미 밀어 두었다. 여기서는 키보드가 내비게이션 바보다 올라온 만큼만 본문을 줄인다.
    // 입력하는 동안 보내기 버튼은 접어 두어 입력칸을 가리지 않게 하고, 키보드를 내리면 다시 보인다.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas)
            .consumeWindowInsets(WindowInsets.systemBars)
            .imePadding()
            .nyummyClearFocusOnTap(),
    ) {
        NyummyTopBar(
            title = stringResource(R.string.inquiry_write_title),
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
            NyummySelectField(
                value = stringResource(uiState.category.labelRes),
                label = stringResource(R.string.inquiry_write_category_label),
                onClick = { onIntent(InquiryWriteIntent.ClickCategory) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(NyummyTheme.spacing.s20))
            NyummyTextField(
                value = uiState.title,
                // 서버가 받는 제목 길이까지만 받는다.
                onValueChange = { onIntent(InquiryWriteIntent.ChangeTitle(it.take(TITLE_MAX_LENGTH))) },
                label = stringResource(R.string.inquiry_write_title_label),
                placeholder = stringResource(R.string.inquiry_write_title_placeholder),
                errorMessage = if (uiState.isTitleEmptyError) {
                    stringResource(R.string.inquiry_write_title_empty_error)
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(NyummyTheme.spacing.s20))
            NyummyTextArea(
                value = uiState.content,
                onValueChange = { onIntent(InquiryWriteIntent.ChangeContent(it)) },
                label = stringResource(R.string.inquiry_write_content_label),
                placeholder = stringResource(R.string.inquiry_write_content_placeholder),
                maxLength = CONTENT_MAX_LENGTH,
                errorMessage = if (uiState.isContentEmptyError) {
                    stringResource(R.string.inquiry_write_content_empty_error)
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(NyummyTheme.spacing.s20))
            InquiryImageField(
                imagePath = uiState.imagePath,
                isLoading = uiState.isImageLoading,
                onAddClick = onAddImageClick,
                onRemoveClick = { onIntent(InquiryWriteIntent.ClickRemoveImage) },
            )
        }
        NyummyHiddenWhileTyping {
            NyummyBottomCta(
                primaryText = stringResource(R.string.inquiry_write_send),
                onPrimaryClick = { onIntent(InquiryWriteIntent.ClickSend) },
                primaryEnabled = !uiState.isSending && !uiState.isImageLoading,
                caption = stringResource(R.string.inquiry_write_reply_notice),
            )
        }
    }

    if (uiState.isCategorySheetVisible) {
        InquiryCategorySheet(
            selected = uiState.category,
            onSelect = { onIntent(InquiryWriteIntent.SelectCategory(it)) },
            onDismiss = { onIntent(InquiryWriteIntent.DismissCategorySheet) },
        )
    }
}

/**
 * 사진 첨부 칸. 비어 있으면 누르면 사진 선택기가 열리는 칸이고, 붙이면 그 칸이 썸네일이 된다.
 * 한 장만 붙일 수 있어서 바꾸려면 썸네일 오른쪽 위 버튼으로 빼고 다시 고른다.
 */
@Composable
private fun InquiryImageField(
    imagePath: String?,
    isLoading: Boolean,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.s)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(FieldLabelGap),
    ) {
        NyummyText(
            text = stringResource(R.string.inquiry_write_image_label),
            style = NyummyTheme.typography.labelM,
            color = NyummyTheme.colors.content.secondary,
        )
        when {
            isLoading -> Box(
                modifier = Modifier
                    .size(ImageTileSize)
                    .background(NyummyTheme.colors.bg.surface, shape)
                    .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.default, shape),
                contentAlignment = Alignment.Center,
            ) {
                NyummySpinner(contentDescription = stringResource(R.string.inquiry_write_image_loading))
            }
            imagePath != null -> Box(modifier = Modifier.size(ImageTileSize)) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = stringResource(R.string.inquiry_attached_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                        .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.subtle, shape),
                )
                // 보이는 버튼은 작아도 누르는 범위는 썸네일 오른쪽 위 48dp다.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(NyummyTheme.size.touchTarget)
                        .nyummyClickable(onClick = onRemoveClick),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(RemoveButtonInset)
                            .size(RemoveButtonSize)
                            .background(NyummyTheme.colors.bg.scrim, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(CommonR.drawable.nyummy_ic_x),
                            contentDescription = stringResource(R.string.inquiry_write_image_remove),
                            tint = NyummyTheme.colors.content.onAction,
                            modifier = Modifier.size(NyummyTheme.size.iconXs),
                        )
                    }
                }
            }
            else -> Box(
                modifier = Modifier
                    .nyummyClickable(onClick = onAddClick)
                    .size(ImageTileSize)
                    .background(NyummyTheme.colors.bg.surfaceSubtle, shape),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
                ) {
                    Icon(
                        painter = painterResource(CommonR.drawable.nyummy_ic_plus),
                        contentDescription = null,
                        tint = NyummyTheme.colors.content.secondary,
                        modifier = Modifier.size(NyummyTheme.size.iconM),
                    )
                    NyummyText(
                        text = stringResource(R.string.inquiry_write_image_add),
                        style = NyummyTheme.typography.bodyS,
                        color = NyummyTheme.colors.content.secondary,
                    )
                }
            }
        }
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
                text = stringResource(R.string.inquiry_write_category_label),
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

/** 입력칸 라벨과 칸 사이. 공통 입력칸과 같다. */
private val FieldLabelGap = 6.dp

private val ImageTileSize = 80.dp

private val RemoveButtonSize = 22.dp

/** 썸네일 모서리에서 빼기 버튼까지. */
private val RemoveButtonInset = 4.dp

/** 서버가 받는 문의 제목 최대 길이. */
private const val TITLE_MAX_LENGTH = 100

/** 서버가 받는 문의 내용 최대 길이. */
private const val CONTENT_MAX_LENGTH = 2_000

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun InquiryWriteScreenPreview() {
    NyummyTheme {
        InquiryWriteScreen(
            uiState = InquiryWriteUIState(
                title = "사진을 찍은 뒤 기록이 사라졌어요",
                content = "다시 찍어도 같은 문제가 있어요.",
            ),
            onIntent = {},
            onAddImageClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun InquiryWriteScreenErrorPreview() {
    NyummyTheme {
        InquiryWriteScreen(
            uiState = InquiryWriteUIState(isTitleEmptyError = true, isContentEmptyError = true),
            onIntent = {},
            onAddImageClick = {},
        )
    }
}
