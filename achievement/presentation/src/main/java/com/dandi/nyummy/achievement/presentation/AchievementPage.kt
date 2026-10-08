package com.dandi.nyummy.achievement.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyComingSoonPage
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** 업적 화면. 기능 공개 전까지 준비 중 안내를 보여준다. */
@Composable
fun AchievementPage(
    viewModel: AchievementViewModel = hiltViewModel(),
) {
    AchievementScreen()
}

@Composable
private fun AchievementScreen() {
    NyummyComingSoonPage(
        title = stringResource(R.string.achievement_title),
        message = stringResource(R.string.achievement_coming_title),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 767)
@Composable
private fun AchievementScreenPreview() {
    NyummyTheme {
        AchievementScreen()
    }
}
