package com.dandi.nyummy.intro.presentation

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBadge
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 앱 시작 시 1회 노출하는 접근권한 안내 바텀시트.
 *
 * [visible] 전환에 맞춰 딤은 페이드, 시트는 아래에서 슬라이드로 등장하고 퇴장한다.
 * 정보통신망법상 접근권한 고지를 겸하므로 딤이나 뒤로 가기로 닫을 수 없고
 * [onConfirm](확인 → 시스템 권한 요청)만 제공한다. 선택 권한뿐이라 거부해도 진행된다.
 */
@Composable
fun PermissionNoticeBottomSheet(
    visible: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        BackHandler(enabled = visible) {}
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(SheetAnimationMillis)),
            exit = fadeOut(tween(SheetAnimationMillis)),
            label = "PermissionNoticeScrim",
        ) {
            // 탭을 소비만 해서 바깥을 눌러도 닫히지 않게 한다(고지 목적).
            Box(
                Modifier
                    .fillMaxSize()
                    .background(NyummyTheme.colors.bg.scrim)
                    .pointerInput(Unit) { detectTapGestures { } },
            )
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(SheetAnimationMillis)) { fullHeight -> fullHeight },
            exit = slideOutVertically(tween(SheetAnimationMillis)) { fullHeight -> fullHeight },
            label = "PermissionNoticeSheet",
        ) {
            PermissionNoticeSheetSurface(onConfirm = onConfirm)
        }
    }
}

@Composable
private fun PermissionNoticeSheetSurface(
    onConfirm: () -> Unit,
) {
    val theme = NyummyTheme
    val title = stringResource(R.string.intro_permission_title)
    val shape = RoundedCornerShape(topStart = theme.radius.l, topEnd = theme.radius.l)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PermissionNoticeSheetTag)
            .semantics { paneTitle = title }
            .nyummyShadow(shape, theme.elevation.float)
            .background(theme.colors.bg.surface, shape)
            // 시트는 내용만큼만 감싸고, 확인 버튼이 마지막 요소로 시트 바닥(제스처 바 인셋 위)에 붙는다.
            .navigationBarsPadding()
            .padding(start = theme.spacing.s20, end = theme.spacing.s20, top = SheetTopPadding, bottom = theme.spacing.s40),
    ) {
        NyummyText(
            text = title,
            style = theme.typography.titleL,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(theme.spacing.s20))
        Column(verticalArrangement = Arrangement.spacedBy(theme.spacing.s12)) {
            PermissionRow(
                iconRes = CommonR.drawable.nyummy_ic_camera,
                name = stringResource(R.string.intro_permission_camera),
                description = stringResource(R.string.intro_permission_camera_desc),
            )
            PermissionRow(
                iconRes = CommonR.drawable.nyummy_ic_bell,
                name = stringResource(R.string.intro_permission_notification),
                description = stringResource(R.string.intro_permission_notification_desc),
            )
        }
        Spacer(Modifier.height(theme.spacing.s16))
        NyummyText(
            text = stringResource(R.string.intro_permission_notice),
            style = theme.typography.bodyS,
            color = theme.colors.content.tertiary,
        )
        Spacer(Modifier.height(theme.spacing.s24))
        NyummyButton(
            text = stringResource(R.string.intro_permission_confirm),
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 아이콘(연민트 원 40) + 권한명과 "선택" 배지 + 용도 설명으로 된 권한 안내 행. */
@Composable
private fun PermissionRow(
    @DrawableRes iconRes: Int,
    name: String,
    description: String,
) {
    val theme = NyummyTheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(RowIconBackdropSize)
                .background(theme.colors.bg.selected, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(theme.size.iconM),
                tint = theme.colors.content.brand,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(theme.spacing.s2)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(NameBadgeGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NyummyText(text = name, style = theme.typography.titleS)
                NyummyBadge(text = stringResource(R.string.intro_permission_optional))
            }
            NyummyText(
                text = description,
                style = theme.typography.bodyS,
                color = theme.colors.content.tertiary,
            )
        }
    }
}

private const val PermissionNoticeSheetTag = "intro_permission_notice_sheet"
private const val SheetAnimationMillis = 300
private val SheetTopPadding = 28.dp
private val RowIconBackdropSize = 40.dp
private val NameBadgeGap = 6.dp

@Preview(name = "Permission Notice", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PermissionNoticeBottomSheetPreview() {
    NyummyTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            IntroSplashContent(isComplete = false, isPermissionNoticeVisible = true)
            PermissionNoticeBottomSheet(visible = true, onConfirm = {})
        }
    }
}
