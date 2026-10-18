package com.dandi.nyummy.home.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.helper.LocalIsTopScreen
import com.dandi.nyummy.common.presentation.room.LocalRoomCardSize
import com.dandi.nyummy.common.presentation.room.RoomCardSizeHolder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRoomCardSizeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val holder = RoomCardSizeHolder()
    private var isTopScreen by mutableStateOf(true)
    private var cardHeight by mutableStateOf(500.dp)

    private fun setCard() {
        composeRule.setContent {
            CompositionLocalProvider(LocalRoomCardSize provides holder, LocalIsTopScreen provides isTopScreen) {
                NyummyTheme {
                    Box(Modifier.size(344.dp, cardHeight)) {
                        HomeRoomCard(
                            hasRecordedToday = false,
                            catMotion = null,
                            useFallbackCat = false,
                            onCatClick = {},
                            onCatMotionFinished = {},
                            onCatShown = {},
                            speech = "",
                            isMenuExpanded = false,
                            onToggleMenu = {},
                            onMyRoomClick = {},
                            onShareFriendClick = {},
                            onNyummyStatusClick = {},
                            modifier = Modifier.fillMaxSize(),
                        ) {}
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun 다른_화면으로_넘어가며_길게_다시_그려진_크기는_적지_않는다() {
        setCard()
        assertEquals(DpSize(344.dp, 500.dp), holder.size)

        // 마이룸으로 넘어가는 순간: 맨 위 화면이 아니게 되고, 하단 탭이 사라져 카드가 탭 높이만큼 길어진다.
        isTopScreen = false
        cardHeight = 577.dp
        composeRule.waitForIdle()
        assertEquals(DpSize(344.dp, 500.dp), holder.size)

        // 홈으로 돌아오면 다시 잰다.
        isTopScreen = true
        cardHeight = 520.dp
        composeRule.waitForIdle()
        assertEquals(DpSize(344.dp, 520.dp), holder.size)
    }
}
