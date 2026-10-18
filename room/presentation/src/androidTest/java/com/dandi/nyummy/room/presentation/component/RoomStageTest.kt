package com.dandi.nyummy.room.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.room.presentation.RoomPlacementUiModel
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoomStageTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val selections = mutableListOf<Long?>()
    private val moves = mutableListOf<Move>()
    private var frontCount = 0
    private var putAwayCount = 0

    private data class Move(val id: Long, val x: Float, val y: Float, val scale: Float)

    /** 왼쪽 아래 방석(1)과 오른쪽 캣타워(2). 그림 주소가 없어 정사각형으로 잰다. */
    private val items = persistentListOf(
        RoomPlacementUiModel(roomItemId = 1, imageUrl = "", x = 0.3f, y = 0.85f, scale = 1f),
        RoomPlacementUiModel(roomItemId = 2, imageUrl = "", x = 0.75f, y = 0.8f, scale = 1f),
    )

    private fun setStage(selectedItemId: Long? = null) {
        composeRule.setContent {
            NyummyTheme {
                Box(Modifier.size(width = 360.dp, height = 480.dp)) {
                    RoomStage(
                        background = null,
                        items = items,
                        selectedItemId = selectedItemId,
                        cat = null,
                        bottomInset = 0.dp,
                        onCatMotionFinished = {},
                        onSelect = { selections += it },
                        onMove = { id, x, y, scale -> moves += Move(id, x, y, scale) },
                        onBringToFront = { frontCount++ },
                        onPutAway = { putAwayCount++ },
                        modifier = Modifier.size(width = 360.dp, height = 480.dp),
                    )
                }
            }
        }
    }

    /** 무대 크기(px)에서 [item]의 가운데. 무대와 같은 계산을 쓴다. */
    private fun centerOf(item: RoomPlacementUiModel, width: Float, height: Float): Offset =
        RoomFrame.of(stageWidth = width, stageHeight = height, bottomInset = 0f).itemRect(item, aspect = null).center

    @Test
    fun 아이템을_누르면_그_아이템을_고른다() {
        setStage()

        composeRule.onRoot().performTouchInput { click(centerOf(items[1], width.toFloat(), height.toFloat())) }

        assertEquals(listOf<Long?>(2L), selections)
        assertTrue(moves.isEmpty())
    }

    @Test
    fun 빈_곳을_누르면_고르기를_푼다() {
        setStage(selectedItemId = 2)

        composeRule.onRoot().performTouchInput { click(Offset(width * 0.5f, height * 0.05f)) }

        assertEquals(listOf<Long?>(null), selections)
    }

    @Test
    fun 아이템을_끌면_손을_뗄_때_한_번만_새_자리를_알린다() {
        setStage()

        composeRule.onRoot().performTouchInput {
            val start = centerOf(items[0], width.toFloat(), height.toFloat())
            down(start)
            moveBy(Offset(30f, 0f))
            moveBy(Offset(60f, -20f))
            up()
        }

        val move = moves.single()
        assertEquals(1L, move.id)
        assertTrue("오른쪽으로 옮겨야 한다: $move", move.x > items[0].x)
        assertTrue("위로 옮겨야 한다: $move", move.y < items[0].y)
        assertEquals(1f, move.scale)
        // 끌기 시작할 때 그 아이템을 고른다.
        assertEquals(listOf<Long?>(1L), selections)
    }

    @Test
    fun 두_손가락으로_벌리면_고른_아이템이_커진다() {
        setStage(selectedItemId = 2)

        composeRule.onRoot().performTouchInput {
            val center = centerOf(items[1], width.toFloat(), height.toFloat())
            pinch(
                start0 = center - Offset(20f, 0f),
                end0 = center - Offset(80f, 0f),
                start1 = center + Offset(20f, 0f),
                end1 = center + Offset(80f, 0f),
            )
        }

        val move = moves.single()
        assertEquals(2L, move.id)
        assertTrue("커져야 한다: $move", move.scale > 1f)
    }

    @Test
    fun 도구_모음으로_맨_앞으로_보내고_보관함에_넣는다() {
        setStage(selectedItemId = 1)

        composeRule.onNodeWithContentDescription("맨 앞으로").performClick()
        composeRule.onNodeWithContentDescription("보관함에 넣기").performClick()

        assertEquals(1, frontCount)
        assertEquals(1, putAwayCount)
        assertTrue("도구 모음을 눌러도 방 고르기가 바뀌면 안 된다: $selections", selections.isEmpty())
    }
}
