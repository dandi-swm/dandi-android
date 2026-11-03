package com.dandi.nyummy.room.presentation.tryon

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.jank.DebugJankReport
import com.dandi.nyummy.common.presentation.jank.JankReporter
import com.dandi.nyummy.common.presentation.jank.LocalJankReporter
import com.dandi.nyummy.room.presentation.MyRoomLoadState
import com.dandi.nyummy.room.presentation.component.rememberRoomSheetState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoomTryOnSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val interiorClicks = mutableListOf<Long>()
    private val backgroundClicks = mutableListOf<Long>()
    private var retries = 0

    /** 민트 체크 방(배경), 발바닥 방석 2개, 플로어 조명, 판매 끝난 단풍 러그. */
    private val cells = persistentListOf(
        RoomTryOnCellUiModel(key = "9:0", id = -4, name = "민트 체크 방", imageUrl = "", isBackground = true, price = 1_000, isPurchasable = true),
        RoomTryOnCellUiModel(key = "7:0", id = -1, name = "발바닥 방석", imageUrl = "", isBackground = false, price = 150, isPurchasable = true),
        RoomTryOnCellUiModel(key = "7:1", id = -2, name = "발바닥 방석", imageUrl = "", isBackground = false, price = 150, isPurchasable = true),
        RoomTryOnCellUiModel(key = "8:0", id = -3, name = "플로어 조명", imageUrl = "", isBackground = false, price = 320, isPurchasable = true),
        RoomTryOnCellUiModel(key = "5:0", id = -5, name = "단풍 러그", imageUrl = "", isBackground = false, price = 400, isPurchasable = false),
    )

    private fun setSheet(loadState: MyRoomLoadState = MyRoomLoadState.LOADED, fontScale: Float = 1f) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalJankReporter provides JankReporter(DebugJankReport()),
                LocalDensity provides Density(density.density, fontScale),
            ) {
                NyummyTheme {
                    RoomTryOnSheet(
                        state = rememberRoomSheetState(),
                        loadState = loadState,
                        cells = cells,
                        onInteriorClick = { interiorClicks += it },
                        onBackgroundClick = { backgroundClicks += it },
                        onRetry = { retries++ },
                        footer = { NyummyText(text = "결제 바", style = NyummyTheme.typography.bodyS) },
                    )
                }
            }
        }
    }

    @Test
    fun 머리에_칸_수와_저장되지_않는다는_안내를_보이고_아래에_결제_바를_둔다() {
        setSheet()

        composeRule.onNodeWithText("담은 아이템 5개").assertIsDisplayed()
        composeRule.onNodeWithText("놓아 본 모습은 저장되지 않아요").assertIsDisplayed()
        composeRule.onNodeWithText("결제 바").assertIsDisplayed()
    }

    @Test
    fun 칸은_가격을_보이고_살_수_없는_아이템은_살_수_없어요를_보인다() {
        setSheet()

        composeRule.onNodeWithContentDescription("1,000코인", useUnmergedTree = true).assertExists()
        assertEquals(2, composeRule.onAllNodesWithText("발바닥 방석", useUnmergedTree = true).fetchSemanticsNodes().size)
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(cells.lastIndex)
        composeRule.onNodeWithText("살 수 없어요", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun 글자를_크게_해도_칸_아래_가격과_살_수_없어요가_잘리지_않는다() {
        setSheet(fontScale = 2f)

        val row = composeRule.onNode(hasScrollToIndexAction())
        row.performScrollToIndex(cells.lastIndex)
        val rowNode = row.fetchSemanticsNode()
        val unavailable = composeRule.onNodeWithText("살 수 없어요", useUnmergedTree = true)
        unavailable.assertIsDisplayed()
        // "살 수 없어요"는 이름과 같은 글자 스타일이라 높이도 같아야 한다. 칸 줄 높이가 고정이면 가격 자리가 눌려 잘린다.
        val name = composeRule.onAllNodesWithText("단풍 러그", useUnmergedTree = true).fetchSemanticsNodes().first()
        val text = unavailable.fetchSemanticsNode()
        assertTrue("가격 자리가 눌렸다: ${text.size.height} < ${name.size.height}", text.size.height >= name.size.height - 1)
        assertTrue("가격 자리가 칸 줄 아래로 나갔다", text.boundsInRoot.bottom <= rowNode.boundsInRoot.bottom + 0.5f)
    }

    @Test
    fun 배경_칸은_배경을_바꾸고_인테리어_칸은_놓는다() {
        setSheet()

        composeRule.onNodeWithText("민트 체크 방", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("플로어 조명", useUnmergedTree = true).performClick()

        assertEquals(listOf(-4L), backgroundClicks)
        assertEquals(listOf(-3L), interiorClicks)
    }

    @Test
    fun 방을_불러오지_못하면_칸을_숨기고_다시_불러오기를_보인다() {
        setSheet(loadState = MyRoomLoadState.FAILED)

        composeRule.onNodeWithText("방을 불러오지 못했어요").assertIsDisplayed()
        composeRule.onAllNodesWithText("발바닥 방석", useUnmergedTree = true).fetchSemanticsNodes().let { assertEquals(0, it.size) }
        composeRule.onNodeWithText("다시 불러오기", useUnmergedTree = true).performClick()
        assertEquals(1, retries)
    }
}
