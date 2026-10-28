package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberNyummyInteractionSource
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 새 소식 공지 한 장.
 *
 * @property badge 사진 위 왼쪽 배지 글자. 비어 있으면 배지를 그리지 않는다.
 * @property imageUrl 10:9 사진 주소. null이거나 받지 못하면 연초록 바탕만 보인다.
 * @property meta 제목 위 작은 글(이벤트 기간 등)
 * @property ctaText 버튼 글자. null이면 버튼이 없다.
 */
@Immutable
data class NyummyNoticePage(
    val badge: String,
    val imageUrl: String?,
    val title: String,
    val body: String,
    val meta: String? = null,
    val ctaText: String? = null,
)

/** 사진 칸. 사진은 가운데를 기준으로 칸을 채우고(10:9가 아니면 가장자리가 잘린다), 위 왼쪽에 배지를 얹는다. */
@Composable
internal fun NoticeHero(page: NyummyNoticePage, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(NyummyTheme.colors.bg.brandSubtle)) {
        if (page.imageUrl != null) {
            AsyncImage(
                model = page.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (page.badge.isNotBlank()) {
            NyummyText(
                text = page.badge,
                style = NyummyTheme.typography.labelS,
                color = NyummyTheme.colors.content.onAction,
                maxLines = 1,
                modifier = Modifier
                    .padding(NyummyTheme.spacing.s16)
                    .background(NyummyTheme.colors.bg.actionPrimary, RoundedCornerShape(NyummyTheme.radius.full))
                    .padding(horizontal = NyummyTheme.spacing.s10, vertical = NyummyTheme.spacing.s4),
            )
        }
    }
}

/**
 * 사진 아래 글 칸(작은 글, 제목, 설명 → 버튼). [fillHeight]면 받은 높이를 채워 버튼을 맨 아래에 두고,
 * 높이가 모자라면 글만 스크롤한다. 아니면 내용 높이만큼만 차지한다(높이를 잴 때).
 * 장이 여럿이면 아래에 고정된 점([NoticeDots])이 오므로 아래 여백을 점과의 간격으로 줄인다.
 */
@Composable
internal fun NoticeBody(
    page: NyummyNoticePage,
    pageCount: Int,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = NyummyTheme.spacing.s24,
                end = NyummyTheme.spacing.s24,
                top = NyummyTheme.spacing.s20,
                bottom = if (pageCount > 1) NyummyComponentDimens.NoticeFooterGap else NyummyTheme.spacing.s20,
            ),
    ) {
        val headModifier = if (fillHeight) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier
        Column(modifier = headModifier, verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6)) {
            page.meta?.let {
                NyummyText(text = it, style = NyummyTheme.typography.bodyS, color = NyummyTheme.colors.content.tertiary)
            }
            NyummyText(text = page.title, style = NyummyTheme.typography.titleL)
            if (page.body.isNotBlank()) {
                NyummyText(text = page.body, style = NyummyTheme.typography.bodyM, color = NyummyTheme.colors.content.secondary)
            }
        }
        page.ctaText?.let {
            Spacer(Modifier.height(NyummyTheme.spacing.s16))
            NyummyButton(text = it, onClick = onCtaClick, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 카드 맨 아래에 고정한 장 표시 점. 내용을 넘겨도 움직이지 않고 지금 장만 칠한다. */
@Composable
internal fun NoticeDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = NyummyTheme.spacing.s20)
            .testTag(NoticeDotsTestTag),
        contentAlignment = Alignment.Center,
    ) {
        PageDots(count = count, current = current)
    }
}

/** 카드 아래 줄. 왼쪽은 "오늘 하루 보지 않기" 체크, 오른쪽은 "닫기"다. 딤 위라 흰 글자로 쓴다. */
@Composable
internal fun NoticeDismissRow(
    todayChecked: Boolean,
    onTodayCheckedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(NyummyComponentDimens.NoticeDismissRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .toggleable(
                    value = todayChecked,
                    interactionSource = rememberNyummyInteractionSource(),
                    indication = null,
                    role = Role.Checkbox,
                    onValueChange = onTodayCheckedChange,
                ),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NoticeCheckbox(checked = todayChecked)
            NyummyText(
                text = stringResource(R.string.nyummy_notice_hide_today),
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.onAction,
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.fillMaxHeight().nyummyClickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            NyummyText(
                text = stringResource(R.string.nyummy_dialog_dismiss),
                style = NyummyTheme.typography.labelM,
                color = NyummyTheme.colors.content.onAction,
            )
        }
    }
}

/** 딤 위 체크박스(20). 꺼지면 흰 테두리만, 켜지면 초록 채움에 흰 체크. 누르기는 줄 전체가 받는다. */
@Composable
private fun NoticeCheckbox(checked: Boolean) {
    val shape = RoundedCornerShape(NyummyComponentDimens.CheckboxRadius)
    Box(
        modifier = Modifier
            .size(NyummyComponentDimens.NoticeCheckboxSize)
            .background(if (checked) NyummyTheme.colors.bg.actionPrimary else Color.Transparent, shape)
            .border(NyummyTheme.borderWidth.bold, if (checked) Color.Transparent else NyummyTheme.colors.content.onAction, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.nyummy_ic_check_bold),
                contentDescription = null,
                tint = NyummyTheme.colors.content.onAction,
                modifier = Modifier.size(NyummyComponentDimens.CheckboxCheckSize),
            )
        }
    }
}

/** 장 표시 점(테스트용). */
internal const val NoticeDotsTestTag = "nyummy_notice_dots"
