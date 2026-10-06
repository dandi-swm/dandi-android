package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

enum class NyummyInlineNoticeTone {
    /** 회색 바탕 + info 아이콘. 미리 알려 둘 안내. */
    Info,

    /** 연분홍 바탕 + 경고 아이콘. 같은 자리에서 Info를 오류로 바꿀 때 쓴다. */
    Danger,
}

/**
 * 화면 안 안내 카드. 아이콘 20 + 제목(label/m) + 설명(body/s), 여백 16×14, radius m.
 * Info와 Danger는 같은 자리에서 바꿔 끼우고, Danger는 접근성 서비스에 바로 알린다.
 */
@Composable
fun NyummyInlineNotice(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tone: NyummyInlineNoticeTone = NyummyInlineNoticeTone.Info,
) {
    val theme = NyummyTheme
    val danger = tone == NyummyInlineNoticeTone.Danger
    val accent = if (danger) theme.colors.content.danger else theme.colors.content.info
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (danger) theme.colors.bg.dangerSubtle else theme.colors.bg.infoSubtle,
                shape = RoundedCornerShape(theme.radius.m),
            )
            .semantics(mergeDescendants = true) { if (danger) liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = theme.spacing.s16, vertical = NoticeVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(NoticeIconGap),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(if (danger) R.drawable.nyummy_ic_circle_alert else R.drawable.nyummy_ic_info),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(theme.size.iconM),
        )
        Column(verticalArrangement = Arrangement.spacedBy(theme.spacing.s2)) {
            NyummyText(text = title, style = theme.typography.labelM, color = accent)
            NyummyText(text = body, style = theme.typography.bodyS, color = theme.colors.content.secondary)
        }
    }
}

private val NoticeVerticalPadding = 14.dp
private val NoticeIconGap = 10.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyInlineNoticePreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        ) {
            NyummyInlineNotice(title = "가입 후 프로필은 바꿀 수 없어요.", body = "입력 내용을 한 번 더 확인해주세요.")
            NyummyInlineNotice(
                title = "필수 정보를 확인해주세요.",
                body = "집사 이름은 비워둘 수 없어요.",
                tone = NyummyInlineNoticeTone.Danger,
            )
        }
    }
}
