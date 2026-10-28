package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 받은 보상 행(우편함 아이템 탭). 1px 테두리 흰 카드 안에 앞 48 칸(옅은 둥근 사각 + 그림), 이름, 출처, 남은 기간,
 * 오른쪽 위 도착 시각과 아래 행동을 둔다. 행 사이 간격은 목록이 정한다(12).
 * 받기 전에는 [actionText] 톤 버튼(받기, 보상 상자는 열기)을 두고, 받은 뒤에는 그림을 흐리게 하고 글자를 quiet로 바꿔 [claimedText]를 쓴다.
 * 남은 기간은 브랜드색이고, [isExpiring](1일 이하)이면 빨갛게 쓴다.
 *
 * @param imageModel 앞 칸 그림. 서버 이미지 URL이나 drawable 리소스 id(Coil 모델).
 * @param largeImage 보상 상자처럼 앞 칸을 거의 채우는 그림(48)이면 true. 코인, 아이템은 36.
 * @param remaining 남은 기간(예 "6일 남았어요"). 받은 뒤이거나 사라지지 않는 보상이면 null.
 * @param actionEnabled 받는 중에는 false로 두어 두 번 받지 않게 한다.
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
) {
    val content = NyummyTheme.colors.content
    val shape = RoundedCornerShape(NyummyTheme.radius.m)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.surface, shape)
            .border(NyummyTheme.borderWidth.hairline, NyummyTheme.colors.border.default, shape)
            .padding(NyummyTheme.spacing.s16),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(NyummyComponentDimens.RewardRowLeadingSize)
                .alpha(if (isClaimed) ClaimedImageAlpha else 1f),
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
            NyummyText(
                text = title,
                style = NyummyTheme.typography.labelM,
                color = if (isClaimed) content.quiet else content.primary,
                maxLines = 1,
            )
            NyummyText(
                text = source,
                style = NyummyTheme.typography.bodyS,
                color = if (isClaimed) content.quiet else content.secondary,
                maxLines = 1,
            )
            if (!isClaimed && remaining != null) {
                NyummyText(
                    text = remaining,
                    style = NyummyTheme.typography.labelS,
                    color = if (isExpiring) content.danger else content.brand,
                    maxLines = 1,
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        ) {
            NyummyText(
                text = time,
                style = NyummyTheme.typography.bodyS,
                color = content.tertiary,
                maxLines = 1,
            )
            if (isClaimed) {
                NyummyText(
                    text = claimedText,
                    style = NyummyTheme.typography.labelS,
                    color = content.quiet,
                    maxLines = 1,
                )
            } else {
                NyummyButton(
                    text = actionText,
                    onClick = onActionClick,
                    style = NyummyButtonStyle.Tonal,
                    size = NyummyButtonSize.S,
                    enabled = actionEnabled,
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
