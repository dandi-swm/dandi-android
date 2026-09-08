package com.dandi.nyummy.meal.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.dandi.nyummy.common.presentation.permission.rememberPermissionRequester
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.meal.presentation.component.MealCameraOverlay
import com.dandi.nyummy.meal.presentation.component.MealCameraPreview
import com.dandi.nyummy.meal.presentation.component.MealFeedCeremony
import com.dandi.nyummy.meal.presentation.component.rememberMealPixelChain
import java.io.File

/**
 * 식사 기록(카메라) 화면입니다.
 *
 * 실시간 프리뷰에서 촬영하면 같은 자리에서 촬영본을 확인하고 취소(재촬영)·먹이기를
 * 선택합니다. 이 컴포저블은 상태 수집, 카메라 권한 요청, [MealRecordIntent] 전달만 담당합니다.
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

    MealRecordScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
private fun MealRecordScreen(
    uiState: MealRecordUIState,
    onIntent: (MealRecordIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing

    // 확인 단계부터 픽셀 체인을 프리웜해 세리머니 시작 지연과 업로드 재작성 경합을 없앤다.
    val chainResult = rememberMealPixelChain(uiState.phase.photoPathOrNull)
    // phase 타입을 키로 써서 제출 성공 copy 로는 유지되고, 세리머니 이탈 시에만 리셋된다.
    var ceremonyIdle by remember(uiState.phase::class) { mutableStateOf(false) }
    val showNext = uiState.isSubmitSucceeded && ceremonyIdle

    // 촬영본 확인 중의 시스템 백은 이탈 대신 재촬영 복귀로, 세리머니 중에는 `다음` 과
    // 동일하게 처리한다(업로드 진행 중에는 ViewModel 이 무시).
    BackHandler(enabled = uiState.phase !is MealCameraPhase.Preview) {
        when (uiState.phase) {
            is MealCameraPhase.Captured -> onIntent(MealRecordIntent.ClickRetake)
            is MealCameraPhase.Feeding -> onIntent(MealRecordIntent.ClickNext)
            MealCameraPhase.Preview -> Unit
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgSurfaceIvory),
    ) {
        MealRecordHeader(
            onCloseClick = { onIntent(MealRecordIntent.ClickClose) },
        )
        Spacer(Modifier.height(spacing.space16))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = spacing.space20)
                .clip(RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius24))
                .background(colors.bgMealPhoto),
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
                    showSuccessCaption = showNext,
                    onIdleChanged = { ceremonyIdle = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // 세리머니 중에는 장식 오버레이(브래킷·마스코트)가 연출과 겹치지 않게 숨긴다.
            if (uiState.cameraPermission != MealCameraPermission.Denied &&
                uiState.phase !is MealCameraPhase.Feeding
            ) {
                MealCameraOverlay(showHint = uiState.phase is MealCameraPhase.Preview)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarHeight),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState.phase) {
                MealCameraPhase.Preview -> ShutterButton(
                    enabled = !uiState.isCapturing &&
                        uiState.cameraPermission == MealCameraPermission.Granted,
                    onClick = { onIntent(MealRecordIntent.ClickShutter) },
                )

                is MealCameraPhase.Captured -> CapturedActionBar(
                    onRetakeClick = { onIntent(MealRecordIntent.ClickRetake) },
                    onSubmitClick = { onIntent(MealRecordIntent.ClickSubmit) },
                    modifier = Modifier.fillMaxWidth(),
                )

                is MealCameraPhase.Feeding -> FeedingBottomBar(
                    showNext = showNext,
                    onNextClick = { onIntent(MealRecordIntent.ClickNext) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MealRecordHeader(
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
        DandiText(
            text = stringResource(R.string.meal_record_subtitle),
            modifier = Modifier.fillMaxWidth(),
            color = colors.contentDefaultLevel1,
            textAlign = TextAlign.Center,
            style = DesignSystemThemeImpl.typeScale.textRegularM,
        )
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

@Composable
private fun ShutterButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        onClick = onClick,
        modifier = modifier.size(ShutterButtonSize),
        enabled = enabled,
        shape = CircleShape,
        color = colors.bgActionPrimaryDefault,
        border = BorderStroke(ShutterRingWidth, colors.contentInverseDefault),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_meal_camera),
                contentDescription = stringResource(R.string.meal_record_shutter_content_description),
                modifier = Modifier.size(ShutterIconSize),
                tint = colors.contentInverseDefault,
            )
        }
    }
}

@Composable
private fun CapturedActionBar(
    onRetakeClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    Row(
        modifier = modifier.padding(horizontal = spacing.space24),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyButton(
            label = stringResource(R.string.meal_record_retake),
            style = NyummyButtonStyle.Ghost,
            size = NyummyButtonSize.Large,
            onClick = onRetakeClick,
        )
        Spacer(Modifier.weight(1f))
        NyummyButton(
            label = stringResource(R.string.meal_record_submit),
            style = NyummyButtonStyle.Ghost,
            size = NyummyButtonSize.Large,
            onClick = onSubmitClick,
        )
    }
}

/**
 * 세리머니 중의 하단 바. 스피너 없이 힌트 텍스트가 맥박치다가, 세리머니가 부유 단계에
 * 도달하고 응답까지 도착하면 `다음` 버튼이 스프링으로 등장한다.
 */
@Composable
private fun FeedingBottomBar(
    showNext: Boolean,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(showNext) {
        if (showNext) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = !showNext,
            enter = fadeIn(),
            exit = fadeOut(tween(FeedingHintFadeOutMillis)),
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
            DandiText(
                text = stringResource(R.string.meal_record_feeding_in_progress),
                modifier = Modifier.graphicsLayer { alpha = hintAlpha },
                color = colors.contentDefaultLevel1,
                style = DesignSystemThemeImpl.typeScale.textRegularM,
            )
        }
        AnimatedVisibility(
            visible = showNext,
            enter = scaleIn(
                initialScale = NextButtonStartScale,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ) + fadeIn(),
        ) {
            NyummyButton(
                label = stringResource(R.string.meal_record_next),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space24),
                style = NyummyButtonStyle.Primary,
                size = NyummyButtonSize.Large,
                onClick = onNextClick,
            )
        }
    }
}

private val BottomBarHeight = 120.dp
private val ShutterButtonSize = 76.dp
private val ShutterRingWidth = 3.dp
private val ShutterIconSize = 28.dp
private const val FeedingHintFadeOutMillis = 150
private const val FeedingHintPulseMillis = 1100
private const val FeedingHintMinAlpha = 0.45f
private const val NextButtonStartScale = 0.5f

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealRecordScreenPreviewPhase() {
    DesignSystemTheme {
        MealRecordScreen(
            uiState = MealRecordUIState(cameraPermission = MealCameraPermission.Granted),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealRecordScreenCapturedPhase() {
    DesignSystemTheme {
        MealRecordScreen(
            uiState = MealRecordUIState(
                phase = MealCameraPhase.Captured(photoPath = "/cache/meal_capture_preview.jpg"),
                cameraPermission = MealCameraPermission.Granted,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MealRecordScreenFeedingPhase() {
    DesignSystemTheme {
        MealRecordScreen(
            uiState = MealRecordUIState(
                phase = MealCameraPhase.Feeding(photoPath = "/cache/meal_capture_preview.jpg"),
                cameraPermission = MealCameraPermission.Granted,
                isSubmitSucceeded = true,
            ),
            onIntent = {},
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
        )
    }
}
