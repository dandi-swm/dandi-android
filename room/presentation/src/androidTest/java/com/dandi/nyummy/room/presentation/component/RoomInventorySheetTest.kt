package com.dandi.nyummy.room.presentation.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.jank.DebugJankReport
import com.dandi.nyummy.common.presentation.jank.JankReporter
import com.dandi.nyummy.common.presentation.jank.LocalJankReporter
import com.dandi.nyummy.room.entity.RoomItemCategory
import com.dandi.nyummy.room.presentation.InventoryItemUiModel
import com.dandi.nyummy.room.presentation.InventoryTabUiModel
import com.dandi.nyummy.room.presentation.MyRoomLoadState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoomInventorySheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var sheetState: RoomSheetState
    private val selectedTabs = mutableListOf<RoomItemCategory>()
    private val interiorClicks = mutableListOf<Long>()
    private val backgroundClicks = mutableListOf<Long>()
    private val reachedEnds = mutableListOf<RoomItemCategory>()

    /** 배경 하나, 침대 [bedCount]개를 가졌고 서버가 침대 탭 이름만 준 보관함. */
    private fun tabs(bedCount: Int = 1) = InventoryTabUiModel.emptyTabs().map { tab ->
        when (tab.category) {
            RoomItemCategory.BACKGROUND -> tab.copy(
                items = persistentListOf(InventoryItemUiModel(100, RoomItemCategory.BACKGROUND, "햇살 방", "", isPlaced = true)),
            )
            RoomItemCategory.BED -> tab.copy(
                name = "침대",
                items = List(bedCount) { InventoryItemUiModel(it + 1L, RoomItemCategory.BED, "침대 ${it + 1}", "", isPlaced = false) }
                    .toImmutableList(),
                nextCursor = 1,
            )
            else -> tab
        }
    }.toImmutableList()

    private fun setSheet(
        expanded: Boolean = true,
        selectedCategory: RoomItemCategory = RoomItemCategory.BED,
        bedCount: Int = 1,
    ) {
        composeRule.setContent {
            sheetState = rememberRoomSheetState(expanded)
            CompositionLocalProvider(LocalJankReporter provides JankReporter(DebugJankReport())) {
                NyummyTheme {
                    RoomInventorySheet(
                        state = sheetState,
                        loadState = MyRoomLoadState.LOADED,
                        tabs = tabs(bedCount),
                        selectedCategory = selectedCategory,
                        onSelectTab = { selectedTabs += it },
                        onInteriorClick = { interiorClicks += it },
                        onBackgroundClick = { backgroundClicks += it },
                        onReachEnd = { reachedEnds += it },
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

        assertEquals(listOf(RoomItemCategory.BACKGROUND), selectedTabs)
        assertEquals(1f, sheetState.expansion)
    }

    @Test
    fun 탭은_종류마다_세_개이고_서버_이름이_없으면_앱_이름을_쓴다() {
        setSheet()

        listOf("배경", "침대", "기타").forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun 배경_탭_칸은_배경을_고르고_다른_탭_칸은_인테리어를_놓는다() {
        setSheet(selectedCategory = RoomItemCategory.BACKGROUND)
        composeRule.onNodeWithText("햇살 방").performClick()

        composeRule.onNodeWithText("침대").performClick()
        assertEquals(listOf(RoomItemCategory.BED), selectedTabs)

        assertEquals(listOf(100L), backgroundClicks)
        assertTrue(interiorClicks.isEmpty())
    }

    @Test
    fun 칸은_가로로_넘기고_끝에_가까워지면_그_종류의_다음_묶음을_부른다() {
        setSheet(bedCount = 20)
        composeRule.onNodeWithText("침대 1").assertIsDisplayed()
        composeRule.onNodeWithText("침대 20").assertDoesNotExist()
        assertTrue("처음에는 끝이 멀어 부르지 않는다", reachedEnds.isEmpty())

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(19)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("침대 20").assertIsDisplayed()
        assertEquals(RoomItemCategory.BED, reachedEnds.distinct().single())
    }
}
