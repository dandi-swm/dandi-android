package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyPressable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 바텀 시트. 위 모서리 radius l, 손잡이 40×4, 아래로 끌어 닫거나 바깥을 눌러 닫는다. 딤은 bg/scrim.
 * 시트 상태는 안에서 만들어 Material3 실험적 API가 호출하는 쪽으로 새지 않게 한다.
 * 안쪽은 좌우 gutter, 위 12, 아래 24 + 내비게이션 바. 요소 사이 16.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NyummyBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = NyummyTheme.radius.l, topEnd = NyummyTheme.radius.l),
        containerColor = NyummyTheme.colors.bg.surface,
        contentColor = NyummyTheme.colors.content.primary,
        tonalElevation = 0.dp,
        scrimColor = NyummyTheme.colors.bg.scrim,
        dragHandle = null,
    ) {
        NyummySheetContent(content = content)
    }
}

/** 시트 안쪽 배치(손잡이 + 내용). 창 없이 그려 프리뷰와 테스트에서 쓴다. */
@Composable
internal fun NyummySheetContent(
    modifier: Modifier = Modifier,
    spacing: Arrangement.Vertical = Arrangement.spacedBy(NyummyTheme.spacing.s16),
    bottomPadding: Dp = NyummyTheme.spacing.s24,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = NyummyTheme.spacing.gutter,
                end = NyummyTheme.spacing.gutter,
                top = NyummyTheme.spacing.s12,
                bottom = bottomPadding,
            ),
        verticalArrangement = spacing,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(NyummyComponentDimens.SheetHandleWidth, NyummyComponentDimens.SheetHandleHeight)
                .background(NyummyTheme.colors.border.default, RoundedCornerShape(NyummyTheme.radius.full)),
        )
        content()
    }
}

/** 시트 제목(title/l, 왼쪽 정렬). [NyummyBottomSheet] 안에서 쓴다. */
@Composable
fun NyummySheetTitle(text: String, modifier: Modifier = Modifier) {
    NyummyText(text = text, style = NyummyTheme.typography.titleL, modifier = modifier.fillMaxWidth())
}

/**
 * 감정이 담긴 순간 전용 확인 시트(기록 중 나가기, 연속 기록 끊김, 계정 삭제). 냐미 + 가운데 정렬 + 세로 버튼.
 * 이탈을 막을 때는 주 버튼이 안전한 행동이고, 사용자가 직접 고른 삭제는 [destructive]로 주 버튼을 빨갛게 한다.
 * 오류, 네트워크, 권한에는 쓰지 않는다(다이얼로그를 쓴다).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NyummyConfirmSheet(
    title: String,
    body: String,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    pose: NyummyPose = NyummyPose.Worry,
    destructive: Boolean = false,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = NyummyTheme.radius.l, topEnd = NyummyTheme.radius.l),
        containerColor = NyummyTheme.colors.bg.surface,
        contentColor = NyummyTheme.colors.content.primary,
        tonalElevation = 0.dp,
        scrimColor = NyummyTheme.colors.bg.scrim,
        dragHandle = null,
    ) {
        NyummyConfirmSheetContent(
            title = title,
            body = body,
            primaryText = primaryText,
            onPrimary = onPrimary,
            secondaryText = secondaryText,
            onSecondary = onSecondary,
            pose = pose,
            destructive = destructive,
        )
    }
}

@Composable
internal fun NyummyConfirmSheetContent(
    title: String,
    body: String,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    pose: NyummyPose,
    destructive: Boolean,
) {
    val dimens = NyummyComponentDimens
    val ground = NyummyTheme.colors.bg.surfaceSunken
    NyummySheetContent(spacing = Arrangement.Top, bottomPadding = NyummyTheme.spacing.s32) {
        // 가로 화면이나 분할 화면처럼 높이가 작아도 버튼이 밀려나지 않게, 냐미와 문구만 스크롤한다.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = NyummyTheme.spacing.s16)
                    .align(Alignment.CenterHorizontally)
                    .size(dimens.ConfirmSheetPoseSize),
                contentAlignment = Alignment.BottomCenter,
            ) {
                // 원 배경 없이 바닥 타원만 깐다.
                Box(
                    Modifier
                        .padding(bottom = NyummyTheme.spacing.s2)
                        .size(dimens.ConfirmSheetGroundWidth, dimens.ConfirmSheetGroundHeight)
                        .drawBehind { drawOval(ground) },
                )
                NyummyPoseImage(pose = pose, size = dimens.ConfirmSheetPoseSize)
            }
            NyummyText(
                text = title,
                style = NyummyTheme.typography.titleL,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = NyummyTheme.spacing.s16),
            )
            NyummyText(
                text = body,
                style = NyummyTheme.typography.bodyL,
                color = NyummyTheme.colors.content.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = NyummyTheme.spacing.s8),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = NyummyTheme.spacing.s24),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
        ) {
            NyummyButton(
                text = primaryText,
                onClick = onPrimary,
                style = if (destructive) NyummyButtonStyle.Danger else NyummyButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
            ConfirmSheetSecondaryAction(
                text = secondaryText,
                onClick = onSecondary,
                destructive = destructive,
            )
        }
    }
}

/**
 * 확인 시트의 글자 행동. Ghost L 버튼과 같은 크기(높이 56)이고 글자색만 다르다.
 * 이탈 방지 시트에서는 "그만두기"가 빨강, 삭제 시트에서는 "닫기"가 보조색이다.
 */
@Composable
private fun ConfirmSheetSecondaryAction(text: String, onClick: () -> Unit, destructive: Boolean) {
    val interactionSource = rememberNyummyInteractionSource()
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .nyummyPressable(
                interactionSource = interactionSource,
                enabled = true,
                role = Role.Button,
                onClick = onClick,
            )
            .height(NyummyTheme.size.buttonL)
            .background(if (pressed) NyummyTheme.colors.bg.actionSecondary else Color.Transparent, shape),
        contentAlignment = Alignment.Center,
    ) {
        NyummyText(
            text = text,
            style = NyummyTheme.typography.labelL,
            color = if (destructive) NyummyTheme.colors.content.secondary else NyummyTheme.colors.content.danger,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummySheetPreview() {
    NyummyTheme {
        Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s24)) {
            NyummySheetContent {
                NyummySheetTitle("오늘 식사 요약")
                NyummyButton(text = "밥 주기", onClick = {}, modifier = Modifier.fillMaxWidth())
            }
            NyummyConfirmSheetContent(
                title = "기록을 그만둘까요?",
                body = "냐미가 밥을 기다리고 있어요",
                primaryText = "계속 기록하기",
                onPrimary = {},
                secondaryText = "그만두기",
                onSecondary = {},
                pose = NyummyPose.Worry,
                destructive = false,
            )
        }
    }
}
