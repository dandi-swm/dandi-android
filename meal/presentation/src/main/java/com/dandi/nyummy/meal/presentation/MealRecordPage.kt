package com.dandi.nyummy.meal.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.domain.helper.AppPermission
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.cat.catNameStringResource
import com.dandi.nyummy.common.presentation.coachmark.coachMarkTarget
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPose
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyPoseImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.image.NyummyCdn
import com.dandi.nyummy.common.presentation.permission.rememberPermissionRequester
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.meal.presentation.component.MealCameraOverlay
import com.dandi.nyummy.meal.presentation.component.MealCameraPreview
import com.dandi.nyummy.meal.presentation.component.ceremony.MealCeremony

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

    val activity = LocalActivity.current
    val context = LocalContext.current
    // 거부 직후 다시 물을 수 있는지(rationale)를 함께 본다. 두 번 거부해 "다시 묻지 않음"이 되면 false라 기기 설정으로 안내한다.
    val permissionRequester = rememberPermissionRequester { result ->
        val granted = result[AppPermission.CAMERA] == true
        val canAskAgain = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: true
        viewModel.onIntent(MealRecordIntent.PermissionResult(granted = granted, canAskAgain = canAskAgain))
    }
    // 기기 설정에서 카메라를 켜고 돌아오면 바로 촬영 화면으로 바꾼다.
    LifecycleResumeEffect(uiState.cameraPermission) {
        val isRejected = uiState.cameraPermission == MealCameraPermission.Denied ||
            uiState.cameraPermission == MealCameraPermission.Blocked
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (isRejected && granted) viewModel.onIntent(MealRecordIntent.PermissionResult(granted = true))
        onPauseOrDispose { }
    }
    // 진입 직후(초기 상태 Requesting)와 거부 화면의 재요청 모두 이 한 곳에서 시스템 팝업을 띄운다.
    // 이미 허용된 상태면 팝업 없이 즉시 허용 콜백이 온다. 앱이 종료됐다 사진 확인이나 연출 단계로 되살아났으면
    // 카메라가 필요 없으니, 다시 찍으러 프리뷰로 돌아갈 때 묻는다.
    val isPreview = uiState.phase is MealCameraPhase.Preview
    LaunchedEffect(uiState.cameraPermission, isPreview, uiState.isTutorialChecked) {
        if (isPreview && uiState.isTutorialChecked && uiState.cameraPermission == MealCameraPermission.Requesting) {
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
        onOpenAppSettings = { openAppSettings(context) },
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
    onOpenAppSettings: () -> Unit = {},
) {
    // 촬영본 확인 중의 시스템 백은 이탈 대신 재촬영 복귀로, 연출 중에는 닫기와 같게 처리한다
    // (접수되기 전에는 ViewModel 이 무시한다).
    BackHandler(enabled = uiState.phase !is MealCameraPhase.Preview) {
        when (uiState.phase) {
            // 튜토리얼은 다시 찍을 사진이 없어 닫는다(기록하지 않고 홈으로).
            is MealCameraPhase.Captured ->
                onIntent(if (uiState.phase.isTutorial) MealRecordIntent.ClickClose else MealRecordIntent.ClickRetake)
            is MealCameraPhase.Ceremony -> onIntent(MealRecordIntent.ClickClose)
            MealCameraPhase.Preview -> Unit
        }
    }

    val phase = uiState.phase
    MealTutorialCoachMark(phase = phase, skipConfirm = uiState.tutorialSkipConfirm, onIntent = onIntent)
    if (phase is MealCameraPhase.Ceremony) {
        MealCeremony(
            photoPath = phase.photoPath,
            stage = phase.stage,
            cutscene = phase.cutscene,
            rewardBoxGrade = phase.rewardBoxGrade,
            sendsRewardBox = phase.sendsRewardBox(uiState.isMailGiftEnabled),
            roomBackgroundUrl = uiState.roomBackgroundItemId?.let(NyummyCdn::itemUrl),
            onStageFinished = { onIntent(MealRecordIntent.CeremonyStageFinished(it)) },
            onSkipClick = { onIntent(MealRecordIntent.ClickSkipCeremony) },
            onCloseClick = { onIntent(MealRecordIntent.ClickClose) },
            onDoneClick = { onIntent(MealRecordIntent.ClickDone) },
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.surface)
            // 식사 기록 화면은 연출을 위해 시스템 바 뒤까지 그린다. 촬영과 확인 단계는 시스템 바를 피한다.
            .systemBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        MealRecordHeader(
            showSubtitle = true,
            onCloseClick = { onIntent(MealRecordIntent.ClickClose) },
        )
        Spacer(Modifier.height(NyummyTheme.spacing.s40))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .background(NyummyTheme.colors.bg.surfaceSubtle),
        ) {
            when (phase) {
                MealCameraPhase.Preview -> when (uiState.cameraPermission) {
                    MealCameraPermission.Denied -> PermissionDeniedContent(
                        isBlocked = false,
                        onActionClick = { onIntent(MealRecordIntent.ClickRequestPermission) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    MealCameraPermission.Blocked -> PermissionDeniedContent(
                        isBlocked = true,
                        onActionClick = onOpenAppSettings,
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
                    model = mealPhotoModel(phase.photoPath),
                    contentDescription = stringResource(
                        R.string.meal_record_captured_photo_content_description,
                    ),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                is MealCameraPhase.Ceremony -> Unit
            }
            // 권한이 거부된 안내 화면에서는 장식 오버레이(브래킷)를 숨긴다.
            if (uiState.cameraPermission != MealCameraPermission.Denied &&
                uiState.cameraPermission != MealCameraPermission.Blocked
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
                    onCaptureClick = { onIntent(MealRecordIntent.ClickShutter) },
                    modifier = Modifier.fillMaxWidth(),
                )

                is MealCameraPhase.Captured -> CapturedActionBar(
                    showRetake = !phase.isTutorial,
                    onRetakeClick = { onIntent(MealRecordIntent.ClickRetake) },
                    onSubmitClick = { onIntent(MealRecordIntent.ClickSubmit) },
                    submitModifier = Modifier.coachMarkTarget(MealSubmitCoachMarkKey) { onIntent(MealRecordIntent.ClickSubmit) },
                    modifier = Modifier.fillMaxWidth(),
                )

                is MealCameraPhase.Ceremony -> Unit
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
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s8),
        ) {
            NyummyIconButton(
                icon = CommonR.drawable.nyummy_ic_x,
                contentDescription = stringResource(R.string.meal_record_close_content_description),
                onClick = onCloseClick,
                modifier = Modifier.align(Alignment.CenterStart),
                style = NyummyIconButtonStyle.Ghost,
            )
        }
        NyummyText(
            text = stringResource(R.string.meal_record_title),
            modifier = Modifier.fillMaxWidth(),
            color = NyummyTheme.colors.content.primary,
            textAlign = TextAlign.Center,
            style = NyummyTheme.typography.titleXl,
        )
        if (showSubtitle) {
            NyummyText(
                text = stringResource(R.string.meal_record_subtitle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = NyummyTheme.spacing.s4),
                color = NyummyTheme.colors.content.tertiary,
                textAlign = TextAlign.Center,
                style = NyummyTheme.typography.bodyL,
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    isBlocked: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = NyummyTheme.spacing.s24),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(CommonR.drawable.nyummy_ic_circle_alert),
            contentDescription = null,
            tint = NyummyTheme.colors.content.tertiary,
            modifier = Modifier.size(PermissionIconSize),
        )
        Spacer(Modifier.height(NyummyTheme.spacing.s12))
        NyummyText(
            text = stringResource(R.string.meal_record_permission_denied_title),
            color = NyummyTheme.colors.content.primary,
            textAlign = TextAlign.Center,
            style = NyummyTheme.typography.titleS,
        )
        Spacer(Modifier.height(NyummyTheme.spacing.s8))
        NyummyText(
            text = stringResource(
                if (isBlocked) R.string.meal_record_permission_blocked_body else R.string.meal_record_permission_denied_body,
            ),
            color = NyummyTheme.colors.content.secondary,
            textAlign = TextAlign.Center,
            // 기기 설정에서 켜는 방법까지 담은 여러 줄 안내라 한 줄로 자르지 않는다.
            maxLines = Int.MAX_VALUE,
            style = NyummyTheme.typography.bodyM,
        )
        Spacer(Modifier.height(NyummyTheme.spacing.s16))
        NyummyButton(
            text = stringResource(
                if (isBlocked) R.string.meal_record_permission_open_settings else R.string.meal_record_permission_retry,
            ),
            style = NyummyButtonStyle.Tonal,
            size = NyummyButtonSize.S,
            onClick = onActionClick,
        )
    }
}

/** 이 앱의 상세 설정 화면을 연다. 권한 화면은 여기서 한 번 더 들어간다. */
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    runCatching { context.startActivity(intent) }
}

/** 프리뷰 단계 하단 바: 갤러리 첨부(회색 사각 버튼) · `담기`(촬영) 버튼. 닫기는 위 X가 맡는다. */
@Composable
private fun PreviewActionBar(
    captureEnabled: Boolean,
    galleryEnabled: Boolean,
    onGalleryClick: () -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = NyummyTheme.spacing.s20),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val galleryShape = RoundedCornerShape(NyummyTheme.radius.m)
        Box(
            modifier = Modifier
                .size(GalleryButtonSize)
                .nyummyClickable(onClick = onGalleryClick, enabled = galleryEnabled)
                .background(NyummyTheme.colors.bg.actionNeutral, galleryShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_meal_gallery),
                contentDescription = stringResource(R.string.meal_record_gallery_content_description),
                tint = if (galleryEnabled) NyummyTheme.colors.content.primary else NyummyTheme.colors.content.disabled,
                modifier = Modifier.size(NyummyTheme.size.iconL),
            )
        }
        NyummyButton(
            text = stringResource(R.string.meal_record_capture),
            modifier = Modifier.weight(1f),
            style = NyummyButtonStyle.Primary,
            size = NyummyButtonSize.L,
            enabled = captureEnabled,
            onClick = onCaptureClick,
        )
    }
}

/** 촬영 확인 단계 하단 바: 다시 찍기 · `냐미에게 주기`(주 액션) 버튼. */
@Composable
private fun CapturedActionBar(
    showRetake: Boolean,
    onRetakeClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
    submitModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = NyummyTheme.spacing.s20),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showRetake) {
            NyummyButton(
                text = stringResource(R.string.meal_record_retake),
                style = NyummyButtonStyle.Neutral,
                size = NyummyButtonSize.L,
                onClick = onRetakeClick,
            )
        }
        NyummyButton(
            text = catNameStringResource(R.string.meal_record_submit),
            modifier = Modifier
                .weight(1f)
                .then(submitModifier),
            style = NyummyButtonStyle.Primary,
            size = NyummyButtonSize.L,
            onClick = onSubmitClick,
        )
    }
}

private val BottomBarHeight = 120.dp
private val GalleryButtonSize = 56.dp
private val PermissionIconSize = 32.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealRecordScreenPreviewPhase() {
    NyummyTheme {
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
    NyummyTheme {
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
private fun MealRecordScreenCeremonyWaiting() {
    NyummyTheme {
        MealRecordScreen(
            uiState = MealRecordUIState(
                phase = MealCameraPhase.Ceremony(
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
private fun MealRecordScreenPermissionDenied() {
    NyummyTheme {
        MealRecordScreen(
            uiState = MealRecordUIState(cameraPermission = MealCameraPermission.Denied),
            onIntent = {},
            onGalleryClick = {},
        )
    }
}
