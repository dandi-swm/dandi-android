package com.dandi.nyummy.common.presentation.room

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomCardHeightLimitTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setCard(width: Int, height: Int) {
        composeRule.setContent {
            Box(Modifier.size(width.dp, height.dp)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .limitRoomCardHeight()
                        .testTag(CardTag),
                )
            }
        }
    }

    @Test
    fun 아주_긴_자리에서는_카드를_가장_긴_비율까지만_키우고_가운데에_둔다() {
        // 태블릿 세로 비율(폭 / 높이 0.43)을 화면 안에 들어오게 줄인 자리
        setCard(width = 300, height = 700)

        val height = 300f / RoomCardLayout.MinCardAspect
        composeRule.onNodeWithTag(CardTag)
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(height.dp)
            .assertTopPositionInRootIsEqualTo(((700f - height) / 2f).dp)
    }

    @Test
    fun 폰처럼_덜_긴_자리는_그대로_채운다() {
        setCard(width = 344, height = 550)

        composeRule.onNodeWithTag(CardTag)
            .assertWidthIsEqualTo(344.dp)
            .assertHeightIsEqualTo(550.dp)
            .assertTopPositionInRootIsEqualTo(0.dp)
    }

    private companion object {
        const val CardTag = "room_card"
    }
}
