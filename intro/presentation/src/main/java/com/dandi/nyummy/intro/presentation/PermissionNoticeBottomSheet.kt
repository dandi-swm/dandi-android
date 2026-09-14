package com.dandi.nyummy.intro.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.component.NyummyModalScrim
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 앱 시작 시 1회 노출하는 접근권한 안내 바텀시트.
 *
 * [visible] 전환에 맞춰 스크림은 페이드, 시트는 아래에서 슬라이드로 등장/퇴장한다.
 * 정보통신망법상 접근권한 고지를 겸하므로 스크림/back 으로 닫을 수 없고
 * [onConfirm](확인 → 시스템 권한 요청)만 제공한다. 선택 권한뿐이므로 거부해도 진행된다.
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
            // onDismissRequest 미지정 — 입력을 소비만 해서 바깥 터치로 닫히지 않는다(고지 목적).
            NyummyModalScrim()
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
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PermissionNoticeSheetTag)
            .semantics { paneTitle = TITLE },
        // 화면 하단에 붙는 시트이므로 위쪽 모서리만 둥글린다.
        shape = RoundedCornerShape(
            topStart = DesignSystemThemeImpl.designSystemRadius.radius24,
            topEnd = DesignSystemThemeImpl.designSystemRadius.radius24,
        ),
        color = colors.bgDefaultLevel1,
        contentColor = colors.contentDefaultLevel0,
    ) {
        Column(
            // 시트는 내용만큼만 감싸고, 확인 버튼이 마지막 요소로 시트 바닥(제스처 바 인셋 위)에 붙는다.
            modifier = Modifier
                .padding(horizontal = spacing.space20)
                .padding(top = spacing.space20)
                .navigationBarsPadding(),
        ) {
            DandiText(
                text = TITLE,
                modifier = Modifier.semantics { heading() },
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.displayRegularL,
            )
            Spacer(Modifier.height(spacing.space20))
            PermissionRow(
                iconRes = CommonR.drawable.nyummy_navigation_camera,
                name = "카메라",
                description = "식단 사진 촬영에 사용해요",
            )
            Spacer(Modifier.height(spacing.space12))
            PermissionRow(
                iconRes = CommonR.drawable.nyummy_icon_bell,
                name = "알림",
                description = "식단 기록 리마인드 알림에 사용해요",
            )
            Spacer(Modifier.height(spacing.space16))
            DandiText(
                text = "허용하지 않아도 서비스를 이용할 수 있으며, 필요한 시점에 다시 요청할 수 있어요.",
                color = colors.contentDefaultLevel2,
                maxLines = 2,
                style = DesignSystemThemeImpl.typeScale.labelRegularXS,
            )
            Spacer(Modifier.height(spacing.space24))
            NyummyButton(
                label = "확인",
                modifier = Modifier.fillMaxWidth(),
                size = NyummyButtonSize.Large,
                onClick = onConfirm,
            )
        }
    }
}

/** 아이콘 + 권한명("선택" 표기) + 용도 설명 2줄 구성의 권한 안내 행. */
@Composable
private fun PermissionRow(
    @DrawableRes iconRes: Int,
    name: String,
    description: String,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(RowIconBackdropSize)
                .clip(CircleShape)
                .background(colors.bgBrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(RowIconSize),
                tint = colors.contentAccent,
            )
        }
        Spacer(Modifier.width(spacing.space12))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DandiText(
                    text = name,
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.textStrongM,
                )
                Spacer(Modifier.width(spacing.space4))
                DandiText(
                    text = "선택",
                    color = colors.contentDefaultLevel2,
                    style = DesignSystemThemeImpl.typeScale.labelRegularXS,
                )
            }
            DandiText(
                text = description,
                color = colors.contentDefaultLevel2,
                style = DesignSystemThemeImpl.typeScale.textRegularS,
            )
        }
    }
}

private const val TITLE = "냐미에서 다음 앱 권한을 사용해요"
private const val PermissionNoticeSheetTag = "intro_permission_notice_sheet"
private const val SheetAnimationMillis = 300
private val RowIconBackdropSize = 40.dp
private val RowIconSize = 20.dp

@Preview(name = "Permission Notice", showBackground = true, widthDp = 390, heightDp = 500)
@Composable
private fun PermissionNoticeBottomSheetPreview() {
    DesignSystemTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0),
        ) {
            PermissionNoticeBottomSheet(visible = true, onConfirm = {})
        }
    }
}
