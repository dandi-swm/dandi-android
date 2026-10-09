package com.dandi.nyummy.meal.presentation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.domain.helper.AppPermission
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyMascot
import com.dandi.nyummy.common.presentation.component.NyummyMascotPose
import com.dandi.nyummy.common.presentation.permission.rememberPermissionRequester
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.meal.presentation.component.MealCameraOverlay
import com.dandi.nyummy.meal.presentation.component.MealCameraPreview
import com.dandi.nyummy.meal.presentation.component.MealFeedCeremony
import com.dandi.nyummy.meal.presentation.component.rememberMealPixelChain
import java.io.File

/**
 * 식사 기록(카메라) 화면입니다.
 *
 * 실시간 프리뷰에서 촬영하거나 갤러리에서 사진을 고르면 같은 자리에서 촬영본을 확인하고
 * 취소(재촬영)·먹이기를 선택합니다. 이 컴포저블은 상태 수집, 카메라 권한 요청,
 * 갤러리 선택기 실행, [MealRecordIntent] 전달만 담당합니다.
 */
@Composable
fun MealRecordPage(
    modifier: Modifier = Modifier,
    viewModel: MealRecordViewModel = hiltViewModel<MealRecordViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionRequester = rememberPermissionRequester { result ->
        viewModel.onIntent(MealRecordIntent.PermissionResult(result[AppPermission.CAMERA] == true))
    }
    // 진입 직후(초기 상태 Requesting)와 거부 화면의 재요청 모두 이 한 곳에서 시스템 팝업을 띄운다.
    // 이미 허용된 상태면 팝업 없이 즉시 허용 콜백이 온다.
    LaunchedEffect(uiState.cameraPermission) {
        if (uiState.cameraPermission == MealCameraPermission.Requesting) {
            permissionRequester.request(listOf(AppPermission.CAMERA))
        }
    }

    // 연타로 선택기가 겹쳐 뜨지 않도록 열려 있는 동안을 기억한다(선택기 뒤에서 재생성돼도 유지).
    var isGalleryPickerOpen by rememberSaveable { mutableStateOf(false) }
    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        isGalleryPickerOpen = false
        viewModel.onIntent(MealRecordIntent.GalleryPhotoPicked(uri?.toString()))
    }

    MealRecordScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onGalleryClick = {
            if (!isGalleryPickerOpen) {
                isGalleryPickerOpen = true
                galleryPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun MealRecordScreen(
    uiState: MealRecordUIState,
    onIntent: (MealRecordIntent) -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing

    // 확인 단계부터 픽셀 체인을 프리웜해 세리머니 시작 지연과 업로드 재작성 경합을 없앤다.
    val chainResult = rememberMealPixelChain(uiState.phase.photoPathOrNull)
    // phase 타입을 키로 써서 제출 성공 copy 로는 유지되고, 세리머니 이탈 시에만 리셋된다.
    var ceremonyIdle by remember(uiState.phase::class) { mutableStateOf(false) }
    val showNext = uiState.isSubmitSucceeded && ceremonyIdle

    // 촬영본 확인 중의 시스템 백은 이탈 대신 재촬영 복귀로, 세리머니 중에는 `다음` 과,
    // 완료 화면에서는 `완료` 와 동일하게 처리한다(업로드 진행 중에는 ViewModel 이 무시).
    BackHandler(enabled = uiState.phase !is MealCameraPhase.Preview) {
        when (uiState.phase) {
            is MealCameraPhase.Captured -> onIntent(MealRecordIntent.ClickRetake)
            is MealCameraPhase.Feeding -> onIntent(MealRecordIntent.ClickNext)
            MealCameraPhase.Done -> onIntent(MealRecordIntent.ClickDone)
            MealCameraPhase.Preview -> Unit
        }
    }

    // 세리머니·완료 단계는 카드 박스 대신 패턴 배경 위 풀블리드 연출로 전환된다.
    val isCeremonyPhase = uiState.phase is MealCameraPhase.Feeding ||
        uiState.phase is MealCameraPhase.Done

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgSurfaceIvory),
    ) {
        if (isCeremonyPhase) {
            Image(
                painter = painterResource(CommonR.drawable.nyummy_pattern_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
        MealRecordHeader(
            // 촬영 안내 문구는 촬영·확인 단계에서만 의미가 있다.
            showSubtitle = uiState.phase is MealCameraPhase.Preview ||
                uiState.phase is MealCameraPhase.Captured,
            onCloseClick = { onIntent(MealRecordIntent.ClickClose) },
        )
        if (uiState.phase is MealCameraPhase.Feeding) {
            Spacer(Modifier.height(CeremonyHeadlineTopGap))
            DandiText(
                text = stringResource(R.string.meal_record_ceremony_title_line1),
                modifier = Modifier.fillMaxWidth(),
                color = colors.contentDefaultLevel0,
                textAlign = TextAlign.Center,
                style = DesignSystemThemeImpl.typeScale.displayRegularXXL,
            )
            DandiText(
                text = stringResource(R.string.meal_record_ceremony_title_line2),
                modifier = Modifier.fillMaxWidth(),
                color = colors.contentDefaultLevel0,
                textAlign = TextAlign.Center,
                style = DesignSystemThemeImpl.typeScale.displayRegularXXL,
            )
            Spacer(Modifier.height(spacing.space12))
            DandiText(
                text = stringResource(R.string.meal_record_ceremony_subtitle),
                modifier = Modifier.fillMaxWidth(),
                color = colors.contentDefaultLevel1,
                textAlign = TextAlign.Center,
                style = DesignSystemThemeImpl.typeScale.textRegularL,
            )
        }
        Spacer(Modifier.height(spacing.space16))
        Box(
            modifier = if (isCeremonyPhase) {
                // 패턴 배경 위 풀블리드: 카드 프레임 없이 연출만 얹는다.
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            } else {
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space20)
                    .clip(RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius24))
                    .background(colors.bgMealPhoto)
            },
        ) {
            when (val phase = uiState.phase) {
                MealCameraPhase.Preview -> when (uiState.cameraPermission) {
                    MealCameraPermission.Denied -> PermissionDeniedContent(
                        onRetryClick = { onIntent(MealRecordIntent.ClickRequestPermission) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    MealCameraPermission.Granted -> MealCameraPreview(
                        isCapturing = uiState.isCapturing,
                        onCaptured = { onIntent(MealRecordIntent.PhotoCaptured(it)) },
                        onCaptureFailed = { onIntent(MealRecordIntent.CaptureFailed) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    MealCameraPermission.Requesting -> Unit // 권한 팝업 응답 대기: 어두운 뒤판만 노출
                }

                is MealCameraPhase.Captured -> AsyncImage(
                    model = File(phase.photoPath),
                    contentDescription = stringResource(
                        R.string.meal_record_captured_photo_content_description,
                    ),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                is MealCameraPhase.Feeding -> MealFeedCeremony(
                    photoPath = phase.photoPath,
                    chainResult = chainResult,
                    feedReady = showNext,
                    onIdleChanged = { ceremonyIdle = it },
                    onFedToCat = { onIntent(MealRecordIntent.ClickNext) },
                    modifier = Modifier.fillMaxSize(),
                )

                MealCameraPhase.Done -> MealFeedDoneContent(
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // 세리머니·완료 화면에서는 장식 오버레이(브래킷)가 연출과 겹치지 않게 숨긴다.
            if (uiState.cameraPermission != MealCameraPermission.Denied &&
                (
                    uiState.phase is MealCameraPhase.Preview ||
                        uiState.phase is MealCameraPhase.Captured
                    )
            ) {
                MealCameraOverlay()
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarHeight),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState.phase) {
                MealCameraPhase.Preview -> PreviewActionBar(
                    captureEnabled = !uiState.isCapturing &&
                        !uiState.isImportingGalleryPhoto &&
                        uiState.cameraPermission == MealCameraPermission.Granted,
                    // 갤러리 첨부는 카메라 권한과 무관하므로 권한이 거부된 상태에서도 열어 둔다.
                    galleryEnabled = !uiState.isCapturing && !uiState.isImportingGalleryPhoto,
                    onGalleryClick = onGalleryClick,
                    onCancelClick = { onIntent(MealRecordIntent.ClickClose) },
                    onCaptureClick = { onIntent(MealRecordIntent.ClickShutter) },
                    modifier = Modifier.fillMaxWidth(),
                )

                is MealCameraPhase.Captured -> CapturedActionBar(
                    onRetakeClick = { onIntent(MealRecordIntent.ClickRetake) },
                    onSubmitClick = { onIntent(MealRecordIntent.ClickSubmit) },
                    modifier = Modifier.fillMaxWidth(),
                )

                is MealCameraPhase.Feeding -> FeedingBottomBar(
                    showNext = showNext,
                    modifier = Modifier.fillMaxSize(),
                )

                MealCameraPhase.Done -> NyummyButton(
                    label = stringResource(R.string.meal_record_done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.space24),
                    style = NyummyButtonStyle.Primary,
                    size = NyummyButtonSize.Large,
                    onClick = { onIntent(MealRecordIntent.ClickDone) },
                )
            }
        }
        }
    }
}

@Composable
private fun MealRecordHeader(
    showSubtitle: Boolean,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space16, vertical = spacing.space8),
        ) {
            NyummyIconButton(
                contentDescription = stringResource(R.string.meal_record_close_content_description),
                modifier = Modifier.align(Alignment.CenterStart),
                style = NyummyIconButtonStyle.Filled,
                onClick = onCloseClick,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                )
            }
            DandiText(
                text = stringResource(R.string.meal_record_title),
                modifier = Modifier.align(Alignment.Center),
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.titleStrongL,
            )
        }
        if (showSubtitle) {
            DandiText(
                text = stringResource(R.string.meal_record_subtitle),
                modifier = Modifier.fillMaxWidth(),
                color = colors.contentDefaultLevel1,
                textAlign = TextAlign.Center,
                style = DesignSystemThemeImpl.typeScale.textRegularM,
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    Column(
        modifier = modifier.padding(horizontal = spacing.space24),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DandiText(
            text = stringResource(R.string.meal_record_permission_denied_title),
            color = colors.contentDefaultLevel0,
            textAlign = TextAlign.Center,
            style = DesignSystemThemeImpl.typeScale.textStrongL,
        )
        Spacer(Modifier.height(spacing.space8))
        DandiText(
            text = stringResource(R.string.meal_record_permission_denied_body),
            color = colors.contentDefaultLevel1,
            textAlign = TextAlign.Center,
            style = DesignSystemThemeImpl.typeScale.textRegularM,
        )
        Spacer(Modifier.height(spacing.space24))
        NyummyButton(
            label = stringResource(R.string.meal_record_permission_retry),
            style = NyummyButtonStyle.Secondary,
            size = NyummyButtonSize.Medium,
            onClick = onRetryClick,
        )
    }
}

/** 프리뷰 단계 하단 바: 갤러리 첨부 · 취소 · `담기`(촬영) 버튼. */
@Composable
private fun PreviewActionBar(
    captureEnabled: Boolean,
    galleryEnabled: Boolean,
    onGalleryClick: () -> Unit,
    onCancelClick: () -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    Row(
        modifier = modifier.padding(horizontal = spacing.space20),
        horizontalArrangement = Arrangement.spacedBy(spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyIconButton(
            contentDescription = stringResource(R.string.meal_record_gallery_content_description),
            style = NyummyIconButtonStyle.Filled,
            enabled = galleryEnabled,
            onClick = onGalleryClick,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_meal_gallery),
                contentDescription = null,
            )
        }
        NyummyButton(
            label = stringResource(R.string.meal_record_cancel),
            style = NyummyButtonStyle.Secondary,
            size = NyummyButtonSize.Large,
            onClick = onCancelClick,
        )
        NyummyButton(
            label = stringResource(R.string.meal_record_capture),
            modifier = Modifier.weight(1f),
            style = NyummyButtonStyle.Primary,
            size = NyummyButtonSize.Large,
            enabled = captureEnabled,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_meal_camera),
                    contentDescription = null,
                    modifier = Modifier.size(CaptureIconSize),
                    tint = colors.contentInverseDefault,
                )
            },
            onClick = onCaptureClick,
        )
    }
}

/** 촬영 확인 단계 하단 바: 다시 찍기 · `냐미에게 주기`(주 액션) 버튼. */
@Composable
private fun CapturedActionBar(
    onRetakeClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    Row(
        modifier = modifier.padding(horizontal = spacing.space20),
        horizontalArrangement = Arrangement.spacedBy(spacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyButton(
            label = stringResource(R.string.meal_record_retake),
            style = NyummyButtonStyle.Secondary,
            size = NyummyButtonSize.Large,
            onClick = onRetakeClick,
        )
        NyummyButton(
            label = stringResource(R.string.meal_record_submit),
            modifier = Modifier.weight(1f),
            style = NyummyButtonStyle.Primary,
            size = NyummyButtonSize.Large,
            onClick = onSubmitClick,
        )
    }
}

/** 완료 단계: 맛있게 먹은 냐미와 완료 카피. */
@Composable
private fun MealFeedDoneContent(
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    Column(
        modifier = modifier.padding(horizontal = spacing.space24),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NyummyMascot(
            pose = NyummyMascotPose.Eating,
            contentDescription = stringResource(R.string.meal_record_done_mascot_content_description),
            modifier = Modifier.size(DoneMascotSize),
        )
        Spacer(Modifier.height(spacing.space16))
        DandiText(
            text = stringResource(R.string.meal_record_done_title),
            color = colors.contentDefaultLevel0,
            textAlign = TextAlign.Center,
            style = DesignSystemThemeImpl.typeScale.titleStrongXL,
        )
        Spacer(Modifier.height(spacing.space8))
        DandiText(
            text = stringResource(R.string.meal_record_done_body),
            color = colors.contentDefaultLevel1,
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = DesignSystemThemeImpl.typeScale.textRegularM,
        )
    }
}

/**
 * 세리머니 중의 하단 영역. 배달 대기 동안 도트와 힌트 텍스트가 맥박치고,
 * 준비가 끝나면(먹이기 가능) 조용히 사라진다. 맨 아래에는 nyummy 워드마크를 둔다.
 */
@Composable
private fun FeedingBottomBar(
    showNext: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(showNext) {
        if (showNext) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !showNext,
            enter = fadeIn(),
            exit = fadeOut(tween(FeedingHintFadeOutMillis)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            val pulse = rememberInfiniteTransition(label = "FeedingHintPulse")
            val hintAlpha by pulse.animateFloat(
                initialValue = FeedingHintMinAlpha,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(FeedingHintPulseMillis),
                    RepeatMode.Reverse,
                ),
                label = "hintAlpha",
            )
            Column(
                modifier = Modifier.graphicsLayer { alpha = hintAlpha },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(FeedingDotGap)) {
                    repeat(FeedingDotCount) {
                        Box(
                            modifier = Modifier
                                .size(FeedingDotSize)
                                .background(colors.dataCalendarRecorded, CircleShape),
                        )
                    }
                }
                Spacer(Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space12))
                DandiText(
                    text = stringResource(R.string.meal_record_feeding_in_progress),
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.titleStrongL,
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = DesignSystemThemeImpl.designSystemSpacing.space8),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_meal_paw),
                contentDescription = null,
                modifier = Modifier.size(WordmarkPawSize),
                tint = colors.contentBrandWordmark,
            )
            DandiText(
                text = stringResource(R.string.meal_record_wordmark),
                color = colors.contentBrandWordmark,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
            )
        }
    }
}

private val BottomBarHeight = 120.dp
private val CeremonyHeadlineTopGap = 48.dp
private val CaptureIconSize = 24.dp
private val DoneMascotSize = 180.dp
private const val FeedingHintFadeOutMillis = 150
private const val FeedingHintPulseMillis = 1100
private const val FeedingHintMinAlpha = 0.55f
private const val FeedingDotCount = 3
private val FeedingDotSize = 8.dp
private val FeedingDotGap = 10.dp
private val WordmarkPawSize = 16.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealRecordScreenPreviewPhase() {
    DesignSystemTheme {
        MealRecordScreen(
            uiState = MealRecordUIState(cameraPermission = MealCameraPermission.Granted),
            onIntent = {},
            onGalleryClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealRecordScreenCapturedPhase() {
    DesignSystemTheme {
        MealRecordScreen(
            uiState = MealRecordUIState(
                phase = MealCameraPhase.Captured(
                    photoPath = "/cache/meal_capture_preview.jpg",
                    source = MealPhotoSource.CAMERA,
                ),
                cameraPermission = MealCameraPermission.Granted,
            ),
            onIntent = {},
            onGalleryClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealRecordScreenFeedingPhase() {
    DesignSystemTheme {
        MealRecordScreen(
            uiState = MealRecordUIState(
                phase = MealCameraPhase.Feeding(
                    photoPath = "/cache/meal_capture_preview.jpg",
                    source = MealPhotoSource.CAMERA,
                ),
                cameraPermission = MealCameraPermission.Granted,
                isSubmitSucceeded = true,
            ),
            onIntent = {},
            onGalleryClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealRecordScreenDonePhase() {
    DesignSystemTheme {
        MealRecordScreen(
            uiState = MealRecordUIState(
                phase = MealCameraPhase.Done,
                cameraPermission = MealCameraPermission.Granted,
                isSubmitSucceeded = true,
            ),
            onIntent = {},
            onGalleryClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealRecordScreenPermissionDenied() {
    DesignSystemTheme {
        MealRecordScreen(
            uiState = MealRecordUIState(cameraPermission = MealCameraPermission.Denied),
            onIntent = {},
            onGalleryClick = {},
        )
    }
}
