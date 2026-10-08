package com.dandi.nyummy.quest.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyComingSoonPage
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** 퀘스트 화면. 기능 공개 전까지 준비 중 안내를 보여준다. */
@Composable
fun QuestPage(
    viewModel: QuestViewModel = hiltViewModel(),
) {
    QuestScreen()
}

@Composable
private fun QuestScreen() {
    NyummyComingSoonPage(
        title = stringResource(R.string.quest_title),
        message = stringResource(R.string.quest_coming_title),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 767)
@Composable
private fun QuestScreenPreview() {
    NyummyTheme {
        QuestScreen()
    }
}
