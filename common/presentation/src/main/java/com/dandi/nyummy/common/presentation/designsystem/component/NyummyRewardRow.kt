package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 받은 보상 행(우편함 아이템 탭). 앞 52 칸(움푹한 둥근 사각 + 그림), 이름, 출처, 남은 기간, 오른쪽 위 도착 시각과 아래 행동.
 * 받기 전에는 [actionText] 버튼(받기, 보상 상자는 열기)을 두고, 받은 뒤에는 그림과 글자를 흐리게 하고 [claimedText]를 쓴다.
 * 남은 기간이 [isExpiring](1일 이하)이면 빨갛게 쓴다. 아래에 1px 구분선을 긋는다.
 *
 * @param imageModel 앞 칸 그림. 서버 이미지 URL이나 drawable 리소스 id(Coil 모델).
 * @param largeImage 보상 상자처럼 앞 칸을 거의 채우는 그림(48)이면 true. 코인, 아이템은 36.
 * @param remaining 남은 기간(예 "6일 남았어요"). 받은 뒤이거나 사라지지 않는 보상이면 null.
 * @param actionEnabled 받는 중에는 false로 두어 두 번 받지 않게 한다.
 * @param actionModifier 받기(열기) 버튼에 덧붙일 Modifier(코치마크 대상 등록 등).
 */
@Composable
fun NyummyRewardRow(
    title: String,
    source: String,
    time: String,
    imageModel: Any?,
    isClaimed: Boolean,
    actionText: String,
    claimedText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    largeImage: Boolean = false,
    remaining: String? = null,
    isExpiring: Boolean = false,
    actionEnabled: Boolean = true,
    actionModifier: Modifier = Modifier,
) {
    val dividerColor = NyummyTheme.colors.border.subtle
    val dividerWidth = NyummyTheme.borderWidth.hairline
    val textColor = if (isClaimed) NyummyTheme.colors.content.tertiary else NyummyTheme.colors.content.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val y = size.height - dividerWidth.toPx() / 2
                drawLine(dividerColor, Offset(0f, y), Offset(size.width, y), dividerWidth.toPx())
            }
            .padding(vertical = NyummyComponentDimens.MailRowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(NyummyComponentDimens.MailRowLeadingSize)
                .alpha(if (isClaimed) ClaimedImageAlpha else 1f)
                .background(NyummyTheme.colors.bg.surfaceSunken, RoundedCornerShape(NyummyTheme.radius.m)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(
                    if (largeImage) NyummyComponentDimens.RewardRowLargeImageSize else NyummyComponentDimens.RewardRowImageSize,
                ),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2),
        ) {
            NyummyText(text = title, style = NyummyTheme.typography.titleS, color = textColor, maxLines = 1)
            NyummyText(
                text = source,
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
                maxLines = 1,
            )
            if (!isClaimed && remaining != null) {
                NyummyText(
                    text = remaining,
                    style = NyummyTheme.typography.labelS,
                    color = if (isExpiring) NyummyTheme.colors.content.danger else NyummyTheme.colors.content.tertiary,
                    maxLines = 1,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(NyummyComponentDimens.RewardRowTrailingGap),
        ) {
            NyummyText(
                text = time,
                style = NyummyTheme.typography.labelS,
                color = NyummyTheme.colors.content.tertiary,
                maxLines = 1,
            )
            if (isClaimed) {
                NyummyText(
                    text = claimedText,
                    style = NyummyTheme.typography.labelM,
                    color = NyummyTheme.colors.content.tertiary,
                    maxLines = 1,
                )
            } else {
                NyummyButton(
                    text = actionText,
                    onClick = onActionClick,
                    size = NyummyButtonSize.S,
                    enabled = actionEnabled,
                    modifier = actionModifier,
                )
            }
        }
    }
}

/** 받은 보상의 앞 칸 그림 투명도. */
private const val ClaimedImageAlpha = 0.5f

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyRewardRowPreview() {
    NyummyTheme {
        Column(modifier = Modifier.padding(horizontal = NyummyTheme.spacing.gutter)) {
            NyummyRewardRow(
                title = "코인 100개",
                source = "첫 기록 보상",
                time = "오후 1:05",
                imageModel = R.drawable.nyummy_asset_coin,
                isClaimed = false,
                actionText = "받기",
                claimedText = "받았어요",
                onActionClick = {},
                remaining = "6일 남았어요",
            )
            NyummyRewardRow(
                title = "코인 50개",
                source = "출석 보상",
                time = "어제",
                imageModel = R.drawable.nyummy_asset_coin,
                isClaimed = false,
                actionText = "받기",
                claimedText = "받았어요",
                onActionClick = {},
                remaining = "1일 남았어요",
                isExpiring = true,
            )
            NyummyRewardRow(
                title = "코인 300개",
                source = "7일 연속 기록 보상",
                time = "9월 28일",
                imageModel = R.drawable.nyummy_asset_coin,
                isClaimed = true,
                actionText = "받기",
                claimedText = "받았어요",
                onActionClick = {},
            )
        }
    }
}
