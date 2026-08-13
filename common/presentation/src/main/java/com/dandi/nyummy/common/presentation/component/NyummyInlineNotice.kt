package com.dandi.nyummy.common.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 인라인 안내/경고 카드 (Figma `회원가입`의 privacy·Validation Error 패턴).
 *
 * [NyummyInlineNoticeTone.Info]는 아이콘 없는 안내문, [NyummyInlineNoticeTone.Danger]는
 * 종 아이콘과 강조 제목이 붙는 검증 오류 배너를 그린다.
 */
enum class NyummyInlineNoticeTone { Info, Danger }

@Composable
fun NyummyInlineNotice(
    title: String,
    modifier: Modifier = Modifier,
    tone: NyummyInlineNoticeTone = NyummyInlineNoticeTone.Info,
    description: String? = null,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape = DesignSystemThemeImpl.designSystemShape.cardDefault
    val background = when (tone) {
        NyummyInlineNoticeTone.Info -> colors.bgInfoSoft
        NyummyInlineNoticeTone.Danger -> colors.bgDangerSoft
    }
    Row(
        modifier = modifier
            .background(background, shape)
            .padding(
                horizontal = DesignSystemThemeImpl.designSystemSpacing.space16,
                vertical = NoticeVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tone == NyummyInlineNoticeTone.Danger) {
            Icon(
                painter = painterResource(R.drawable.nyummy_icon_bell),
                contentDescription = null,
                modifier = Modifier.size(NoticeIconSize),
                tint = colors.contentError,
            )
            Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space12))
        }
        Column(verticalArrangement = Arrangement.spacedBy(NoticeTextSpacing)) {
            DandiText(
                text = title,
                color = when (tone) {
                    NyummyInlineNoticeTone.Info -> colors.contentInfo
                    NyummyInlineNoticeTone.Danger -> colors.contentError
                },
                maxLines = 2,
                style = when (tone) {
                    NyummyInlineNoticeTone.Info -> DesignSystemThemeImpl.typeScale.textRegularS
                    NyummyInlineNoticeTone.Danger -> DesignSystemThemeImpl.typeScale.textStrongM
                },
            )
            if (description != null) {
                DandiText(
                    text = description,
                    color = when (tone) {
                        NyummyInlineNoticeTone.Info -> colors.contentInfo
                        NyummyInlineNoticeTone.Danger -> colors.contentDefaultLevel1
                    },
                    maxLines = 2,
                    style = DesignSystemThemeImpl.typeScale.textRegularS,
                )
            }
        }
    }
}

private val NoticeVerticalPadding = 13.dp
private val NoticeIconSize = 24.dp
private val NoticeTextSpacing = 2.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyInlineNoticePreview() {
    DesignSystemTheme {
        Column(
            modifier = Modifier.padding(DesignSystemThemeImpl.designSystemLayout.mobileGutter),
            verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16),
        ) {
            NyummyInlineNotice(
                title = "가입 후 프로필은 바꿀 수 없어요.",
                modifier = Modifier.fillMaxWidth(),
                description = "입력 내용을 한 번 더 확인해주세요.",
            )
            NyummyInlineNotice(
                title = "필수 정보를 확인해주세요.",
                modifier = Modifier.fillMaxWidth(),
                tone = NyummyInlineNoticeTone.Danger,
                description = "집사 이름은 비워둘 수 없어요.",
            )
        }
    }
}
