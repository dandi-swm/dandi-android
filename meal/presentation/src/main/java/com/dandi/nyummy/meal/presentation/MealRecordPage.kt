package com.dandi.nyummy.meal.presentation

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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

    // 촬영본 확인 중의 시스템 백은 이탈 대신 재촬영 복귀로 처리해 제출 전 실수 이탈을 막는다.
    BackHandler(enabled = uiState.phase is MealCameraPhase.Captured) {
        onIntent(MealRecordIntent.ClickRetake)
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
            }
            if (uiState.cameraPermission != MealCameraPermission.Denied) {
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
        CapturedActionText(
            label = stringResource(R.string.meal_record_retake),
            onClick = onRetakeClick,
        )
        Spacer(Modifier.weight(1f))
        CapturedActionText(
            label = stringResource(R.string.meal_record_submit),
            onClick = onSubmitClick,
        )
    }
}

/** 촬영본 확인 단계의 텍스트 액션. 시안의 무배경 텍스트 버튼을 최소 터치 타깃으로 감싼다. */
@Composable
private fun CapturedActionText(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        onClick = onClick,
        modifier = modifier.height(DesignSystemThemeImpl.designSystemSize.minimumTouchTarget),
        color = colors.bgSurfaceIvory,
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = DesignSystemThemeImpl.designSystemSpacing.space16,
            ),
            contentAlignment = Alignment.Center,
        ) {
            DandiText(
                text = label,
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
            )
        }
    }
}

private val BottomBarHeight = 120.dp
private val ShutterButtonSize = 76.dp
private val ShutterRingWidth = 3.dp
private val ShutterIconSize = 28.dp

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
private fun MealRecordScreenPermissionDenied() {
    DesignSystemTheme {
        MealRecordScreen(
            uiState = MealRecordUIState(cameraPermission = MealCameraPermission.Denied),
            onIntent = {},
        )
    }
}
