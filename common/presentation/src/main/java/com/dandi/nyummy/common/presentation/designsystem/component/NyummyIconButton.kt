package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyPressScale
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyPressable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow

enum class NyummyIconButtonStyle {
    /** 배경 없음. 툴바, 상단 바에 쓴다. */
    Ghost,

    /** 흰 원 + soft 그림자(v2 HUD 버튼). 일러스트 위 HUD에 쓴다. */
    Filled,

    /** 흰 원 + 가는 테두리(피그마 `v2 / Shop / Add Button`). 상품 그림 위 담기 버튼에 쓴다. */
    Outlined,
}

/**
 * 원 지름과 아이콘 크기. 원이 48보다 작아도 누르는 영역은 Compose가 최소 터치 영역(48)까지 넓힌다.
 */
enum class NyummyIconButtonSize {
    /** 48, 아이콘 24. 상단 바·HUD. */
    L,

    /** 40, 아이콘 24. 상점 상단 바의 장바구니. */
    M,

    /** 32, 아이콘 16. 수량 스테퍼의 −/+. */
    S,

    /** 30, 아이콘 18. 상품 그림 위 담기. */
    Xs,

    /** 아이콘(20) 크기 그대로. 줄 안에 글자처럼 들어가는 버튼(장바구니 줄의 빼기). */
    Inline,
}

/**
 * 원형 아이콘 버튼. 기본은 48dp 원에 아이콘 24dp, 색은 [tint](기본 content/primary).
 * 눌리면 Ghost는 선택 배경, Filled·Outlined는 눌림 배경으로 바뀌고 0.94배로 줄어든다.
 *
 * @param size 원과 아이콘 크기. 작은 크기도 누르는 영역은 48이다
 * @param badgeCount 0보다 크면 원 오른쪽 위에 빨간 수 배지를 단다(장바구니 담긴 종류 수). 수는 [contentDescription]에도 넣는다
 */
@Composable
fun NyummyIconButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NyummyIconButtonStyle = NyummyIconButtonStyle.Ghost,
    size: NyummyIconButtonSize = NyummyIconButtonSize.L,
    enabled: Boolean = true,
    tint: Color = NyummyTheme.colors.content.primary,
    badgeCount: Int = 0,
) {
    val interactionSource = rememberNyummyInteractionSource()
    val pressed by interactionSource.collectIsPressedAsState()
    val raised = style != NyummyIconButtonStyle.Ghost
    val (containerSize, iconSize) = when (size) {
        NyummyIconButtonSize.L -> NyummyTheme.size.touchTarget to NyummyTheme.size.iconL
        NyummyIconButtonSize.M -> NyummyComponentDimens.IconButtonMediumSize to NyummyTheme.size.iconL
        NyummyIconButtonSize.S -> NyummyComponentDimens.IconButtonSmallSize to NyummyTheme.size.iconS
        NyummyIconButtonSize.Xs -> NyummyComponentDimens.IconButtonCompactSize to NyummyComponentDimens.IconButtonCompactIconSize
        NyummyIconButtonSize.Inline -> NyummyTheme.size.iconM to NyummyTheme.size.iconM
    }

    val container = when {
        raised && pressed -> NyummyTheme.colors.bg.actionNeutralPressed
        raised -> NyummyTheme.colors.bg.surface
        pressed -> NyummyTheme.colors.bg.selected
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .nyummyPressable(
                interactionSource = interactionSource,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                pressedScale = NyummyPressScale.Circle,
            )
            .size(containerSize)
            .then(
                if (style == NyummyIconButtonStyle.Filled && !pressed) {
                    Modifier.nyummyShadow(CircleShape, NyummyTheme.elevation.soft)
                } else {
                    Modifier
                },
            )
            .background(container, CircleShape)
            .then(
                if (style == NyummyIconButtonStyle.Outlined) {
                    Modifier.border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.subtle, CircleShape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) tint else NyummyTheme.colors.content.disabled,
            modifier = Modifier.size(iconSize),
        )
        if (badgeCount > 0) {
            CountBadge(
                count = badgeCount,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = containerSize - NyummyComponentDimens.IconButtonBadgeSize, y = NyummyComponentDimens.IconButtonBadgeTopOffset),
            )
        }
    }
}

/** 빨간 바탕 + 흰 테두리의 수 배지. 수는 버튼 설명이 읽으므로 따로 읽지 않는다. */
@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(NyummyTheme.radius.full)
    Box(
        modifier = modifier
            .height(NyummyComponentDimens.IconButtonBadgeSize)
            .defaultMinSize(minWidth = NyummyComponentDimens.IconButtonBadgeSize)
            .background(NyummyTheme.colors.bg.actionDanger, shape)
            .border(NyummyTheme.borderWidth.bold, NyummyTheme.colors.bg.surface, shape)
            .padding(horizontal = NyummyTheme.spacing.s4)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        NyummyText(
            text = count.toString(),
            style = NyummyTheme.typography.labelXs,
            color = NyummyTheme.colors.content.onAction,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NyummyIconButtonPreview() {
    NyummyTheme {
        Row(
            modifier = Modifier.padding(NyummyTheme.spacing.s16),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            NyummyIconButton(R.drawable.nyummy_ic_bell, "알림", {})
            NyummyIconButton(R.drawable.nyummy_ic_bell, "알림", {}, style = NyummyIconButtonStyle.Filled)
            NyummyIconButton(R.drawable.nyummy_ic_cart, "장바구니 2개", {}, size = NyummyIconButtonSize.M, badgeCount = 2)
            NyummyIconButton(
                R.drawable.nyummy_ic_cart,
                "담기",
                {},
                style = NyummyIconButtonStyle.Outlined,
                size = NyummyIconButtonSize.Xs,
                tint = NyummyTheme.colors.content.brand,
            )
            NyummyIconButton(R.drawable.nyummy_ic_plus, "늘리기", {}, size = NyummyIconButtonSize.S)
            NyummyIconButton(R.drawable.nyummy_ic_x, "빼기", {}, size = NyummyIconButtonSize.Inline)
        }
    }
}
