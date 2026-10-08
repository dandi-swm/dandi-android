package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 아직 열지 않은 탭 화면. 위에 탭 이름을 큰 제목으로 두고, 남은 공간 가운데에
 * 인사하는 냐미와 [message], "조금만 기다려 주세요"를 둔다.
 */
@Composable
fun NyummyComingSoonPage(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = NyummyComponentDimens.ContentMaxWidth)
                .padding(horizontal = NyummyTheme.spacing.gutter)
                .padding(top = PageTopGap),
        ) {
            NyummyText(
                text = title,
                style = NyummyTheme.typography.displayM,
                modifier = Modifier.semantics { heading() },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(MessageGap, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NyummyPoseImage(pose = NyummyPose.Hello, size = NyummyTheme.size.characterM)
                NyummyText(
                    text = message,
                    style = NyummyTheme.typography.titleS,
                    textAlign = TextAlign.Center,
                )
                NyummyText(
                    text = stringResource(R.string.nyummy_coming_soon_body),
                    style = NyummyTheme.typography.bodyS,
                    color = NyummyTheme.colors.content.tertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val PageTopGap = 12.dp
private val MessageGap = 6.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 767)
@Composable
private fun NyummyComingSoonPagePreview() {
    NyummyTheme {
        NyummyComingSoonPage(title = "퀘스트", message = "퀘스트를 준비하고 있어요")
    }
}
