package com.dandi.nyummy.home.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.home.presentation.HomeScreen
import com.dandi.nyummy.home.presentation.HomeUIState
import com.dandi.nyummy.home.presentation.homeRoomCardSize
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRoomCardSizeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var screen by mutableStateOf(DpSize(402.dp, 749.dp))
    private var computed = DpSize.Zero

    @Test
    fun 마이룸에_넘기는_카드_크기는_홈에_그려진_카드_크기와_같다() {
        composeRule.setContent {
            NyummyTheme {
                computed = homeRoomCardSize(screen.width, screen.height)
                Box(Modifier.requiredSize(screen)) {
                    HomeScreen(uiState = HomeUIState(), onIntent = {})
                }
            }
        }

        // 보통 폰, 카드가 넓어 폭을 줄이는 아주 작은 폰, 카드가 길어 높이를 줄이는 태블릿 세로.
        listOf(DpSize(402.dp, 749.dp), DpSize(360.dp, 515.dp), DpSize(800.dp, 1180.dp)).forEach { size ->
            screen = size
            composeRule.waitForIdle()

            val card = composeRule.onNodeWithTag(RoomCardTag).getBoundsInRoot()
            assertEquals("$size 폭", computed.width.value, card.width.value, Tolerance)
            assertEquals("$size 높이", computed.height.value, card.height.value, Tolerance)
        }
    }

    private companion object {
        /** 홈은 줄인 카드 크기를 픽셀로 반올림해서 계산값과 1픽셀 안쪽으로 다를 수 있다. */
        const val Tolerance = 0.5f
    }
}
