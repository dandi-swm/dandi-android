package com.dandi.nyummy.room.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.jank.DebugJankReport
import com.dandi.nyummy.common.presentation.jank.JankReporter
import com.dandi.nyummy.common.presentation.jank.LocalJankReporter
import com.dandi.nyummy.common.presentation.room.LocalRoomCardSize
import com.dandi.nyummy.common.presentation.room.RoomCardSizeHolder
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.presentation.MyRoomLoadState.LOADED
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MyRoomScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** 홈 카드가 화면보다 길어서 방을 다 접은 시트 위에 맞게 줄여 보여 주는 경우. */
    private fun setScreen() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalJankReporter provides JankReporter(DebugJankReport()),
                LocalRoomCardSize provides remember { RoomCardSizeHolder().apply { size = DpSize(360.dp, 2000.dp) } },
            ) {
                NyummyTheme {
                    MyRoomScreen(
                        uiState = MyRoomUIState(
                            loadState = LOADED,
                            room = RoomDraft(
                                items = persistentListOf(
                                    RoomPlacementUiModel(roomItemId = 1, imageUrl = "", x = 0.5f, y = 0.85f, scale = 1f),
                                ),
                            ),
                            interiorItems = persistentListOf(
                                InventoryItemUiModel(1, RoomItemCategory.ETC, "털실 공", "", isPlaced = true),
                            ),
                        ),
                        onIntent = {},
                    )
                }
            }
        }
    }

    /** 되돌리기 버튼은 방 왼쪽 끝에 붙어 있어서, 방 폭이 바뀌면 함께 움직인다. */
    private fun undoLeft(): Float = composeRule.onNodeWithContentDescription("되돌리기").fetchSemanticsNode().boundsInRoot.left

    /** 시트 손잡이를 눌러 접거나 펴는 동안 매 프레임 되돌리기 버튼의 왼쪽 위치. */
    private fun undoLeftsWhileToggling(handleDescription: String): List<Float> {
        composeRule.onNodeWithContentDescription(handleDescription).performClick()
        return List(SheetAnimationFrames) {
            composeRule.mainClock.advanceTimeByFrame()
            undoLeft()
        }
    }

    @Test
    fun 시트를_접고_펴는_동안_방_크기가_흔들리지_않는다() {
        setScreen()
        val left = undoLeft()
        composeRule.mainClock.autoAdvance = false

        val whileCollapsing = undoLeftsWhileToggling("보관함 접기")
        val whileExpanding = undoLeftsWhileToggling("보관함 펼치기")

        assertEquals(List(SheetAnimationFrames) { left }, whileCollapsing)
        assertEquals(List(SheetAnimationFrames) { left }, whileExpanding)
    }

    private companion object {
        /** 시트가 붙는 애니메이션(220ms)이 넉넉히 끝나는 프레임 수. */
        const val SheetAnimationFrames = 20
    }
}
