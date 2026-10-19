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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.room.LocalRoomCardSize
import com.dandi.nyummy.common.presentation.room.RoomCardSizeHolder
import com.dandi.nyummy.home.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRoomCardSizeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val holder = RoomCardSizeHolder()
    private var cardHeight by mutableStateOf(600.dp)
    private var myRoomClicks = 0
    private val myRoomLabel = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.home_menu_my_room)

    private fun setCard() {
        composeRule.setContent {
            CompositionLocalProvider(LocalRoomCardSize provides holder) {
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
                            isMenuExpanded = true,
                            onToggleMenu = {},
                            onMyRoomClick = { myRoomClicks++ },
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
    fun 마이룸_버튼을_누르는_순간의_카드_크기를_넘긴다() {
        setCard()
        // 그려지기만 해서는 넘기지 않는다.
        assertNull(holder.size)

        composeRule.onNodeWithContentDescription(myRoomLabel).performClick()

        assertEquals(1, myRoomClicks)
        assertEquals(DpSize(344.dp, 600.dp), holder.size)

        // 다른 화면으로 넘어가며 하단 탭이 사라져 카드가 길게 다시 그려져도, 넘긴 크기는 그대로다.
        cardHeight = 677.dp
        composeRule.waitForIdle()
        assertEquals(DpSize(344.dp, 600.dp), holder.size)
    }
}
