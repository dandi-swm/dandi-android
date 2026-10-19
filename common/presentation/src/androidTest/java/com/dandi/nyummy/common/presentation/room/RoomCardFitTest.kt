package com.dandi.nyummy.common.presentation.room

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
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
class RoomCardFitTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setSlot(width: Int, height: Int) {
        composeRule.setContent {
            Box(Modifier.size(width.dp, height.dp)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .fitRoomCard()
                        .testTag(CardTag),
                )
            }
        }
    }

    @Test
    fun 세로로_긴_자리에서는_폭을_채운_2대3_카드를_위아래_가운데에_둔다() {
        setSlot(width = 300, height = 600)

        composeRule.onNodeWithTag(CardTag)
            .assertWidthIsEqualTo(300.dp)
            .assertHeightIsEqualTo(450.dp)
            .assertLeftPositionInRootIsEqualTo(0.dp)
            .assertTopPositionInRootIsEqualTo(75.dp)
    }

    @Test
    fun 높이가_모자란_자리에서는_같은_비율로_줄여_좌우_가운데에_둔다() {
        setSlot(width = 344, height = 480)

        composeRule.onNodeWithTag(CardTag)
            .assertWidthIsEqualTo(320.dp)
            .assertHeightIsEqualTo(480.dp)
            .assertLeftPositionInRootIsEqualTo(12.dp)
            .assertTopPositionInRootIsEqualTo(0.dp)
    }

    private companion object {
        const val CardTag = "room_card"
    }
}
