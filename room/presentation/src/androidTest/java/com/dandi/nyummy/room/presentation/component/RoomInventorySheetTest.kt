package com.dandi.nyummy.room.presentation.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.jank.DebugJankReport
import com.dandi.nyummy.common.presentation.jank.JankReporter
import com.dandi.nyummy.common.presentation.jank.LocalJankReporter
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.presentation.InventoryItemUiModel
import com.dandi.nyummy.room.presentation.MyRoomLoadState
import com.dandi.nyummy.room.presentation.MyRoomTab
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoomInventorySheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var sheetState: RoomSheetState
    private val selectedTabs = mutableListOf<MyRoomTab>()

    private fun setSheet(expanded: Boolean = true) {
        composeRule.setContent {
            sheetState = rememberRoomSheetState(expanded)
            CompositionLocalProvider(LocalJankReporter provides JankReporter(DebugJankReport())) {
                NyummyTheme {
                    RoomInventorySheet(
                        state = sheetState,
                        loadState = MyRoomLoadState.LOADED,
                        selectedTab = MyRoomTab.INTERIOR,
                        interiorItems = persistentListOf(
                            InventoryItemUiModel(1, RoomItemCategory.ETC, "털실 공", "", isPlaced = false),
                        ),
                        backgrounds = persistentListOf(),
                        onSelectTab = { selectedTabs += it },
                        onInteriorClick = {},
                        onBackgroundClick = {},
                        onReachEnd = {},
                        onShopClick = {},
                        onRetry = {},
                    )
                }
            }
        }
    }

    @Test
    fun 손잡이를_누르면_접히고_다시_누르면_펼쳐진다() {
        setSheet()
        composeRule.onNodeWithText("상점에서 더 찾기").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("보관함 접기").performClick()
        composeRule.waitForIdle()

        assertEquals(0f, sheetState.expansion)
        composeRule.onNodeWithText("상점에서 더 찾기").assertIsNotDisplayed()

        composeRule.onNodeWithContentDescription("보관함 펼치기").performClick()
        composeRule.waitForIdle()

        assertEquals(1f, sheetState.expansion)
        composeRule.onNodeWithText("상점에서 더 찾기").assertIsDisplayed()
    }

    @Test
    fun 손잡이를_아래로_밀면_접히고_위로_밀면_펼쳐진다() {
        setSheet()

        // 손잡이는 낮아서 손잡이 밖까지 길게 민다(실제로 시트를 끌어내리는 것처럼).
        composeRule.onNodeWithContentDescription("보관함 접기").performTouchInput {
            swipe(start = center, end = center + Offset(0f, height * 12f), durationMillis = 300)
        }
        composeRule.waitForIdle()
        assertEquals(0f, sheetState.expansion)
        val collapsedHeight = sheetState.heightPx

        composeRule.onNodeWithContentDescription("보관함 펼치기").performTouchInput {
            swipe(start = center, end = center - Offset(0f, height * 12f), durationMillis = 300)
        }
        composeRule.waitForIdle()
        assertEquals(1f, sheetState.expansion)
        assertTrue("펼치면 시트가 칸 영역만큼 높아진다", sheetState.heightPx >= collapsedHeight + sheetState.bodyHeightPx)
        assertEquals(collapsedHeight, sheetState.collapsedHeightPx)
    }

    @Test
    fun 접힌_채_탭을_누르면_탭을_바꾸고_펼친다() {
        setSheet(expanded = false)

        composeRule.onNodeWithText("배경").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(MyRoomTab.BACKGROUND), selectedTabs)
        assertEquals(1f, sheetState.expansion)
    }
}
