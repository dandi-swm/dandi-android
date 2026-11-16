package com.dandi.nyummy.history.presentation.detail.v2

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealFoodVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealDetailV2ScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private val meal = MealHistoryVO(
        id = "1",
        name = "한정식",
        recordedAt = "08:10",
        calorieKcal = 612,
        carbohydrateGram = 82,
        proteinGram = 24,
        fatGram = 18,
        orderIndex = 1,
        date = HistoryDateVO(2026, 11, 8),
        catComment = "골고루 먹었네!",
    )

    private val foods = persistentListOf(
        MealFoodVO(1, "밥", 1.0, true, 300, 66, 6, 1, foodIconId = "5"),
        MealFoodVO(2, "된장찌개", 1.0, true, 110, 8, 7, 5, foodIconId = "15"),
        MealFoodVO(3, "불고기", 1.5, true, 150, 6, 9, 9, foodIconId = "14"),
    )

    private fun setScreen(state: MealDetailV2UIState, onIntent: (MealDetailV2Intent) -> Unit = {}) {
        composeRule.setContent { NyummyTheme { MealDetailV2Screen(uiState = state, onIntent = onIntent) } }
    }

    @Test
    fun 음식_줄을_모두_보여_주고_대표는_첫_줄에만_붙는다() {
        setScreen(MealDetailV2UIState(meal = meal, foods = foods, isLoading = false))

        foods.forEach { composeRule.onNodeWithText(it.name).performScrollTo().assertIsDisplayed() }
        composeRule.onAllNodesWithText(text(R.string.history_detail_v2_main_badge)).assertCountEquals(1)
        composeRule.onNodeWithText(text(R.string.history_detail_v2_portion, "1.5")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun 식사_합계_영양을_보여_준다() {
        setScreen(MealDetailV2UIState(meal = meal, foods = foods, isLoading = false))

        composeRule.onNodeWithText(text(R.string.history_nutrition_kcal, "612")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun 연필과_음식_추가와_휴지통이_입력을_보낸다() {
        val intents = mutableListOf<MealDetailV2Intent>()
        setScreen(MealDetailV2UIState(meal = meal, foods = foods, isLoading = false)) { intents += it }

        composeRule.onNodeWithContentDescription(text(R.string.history_detail_v2_edit_food_description, "불고기"))
            .performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.history_detail_v2_add_food)).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(text(R.string.history_detail_v2_edit_name_description)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.history_detail_delete)).performClick()

        assertEquals(
            listOf(
                MealDetailV2Intent.ClickEditFood(3),
                MealDetailV2Intent.ClickAddFood,
                MealDetailV2Intent.ClickEditName,
                MealDetailV2Intent.ClickDelete,
            ),
            intents,
        )
    }

    @Test
    fun 계산_중이면_안내를_보여_주고_연필이_눌리지_않는다() {
        val intents = mutableListOf<MealDetailV2Intent>()
        setScreen(
            MealDetailV2UIState(
                meal = meal,
                foods = persistentListOf(MealFoodVO(1, "김치찌개", 1.0, hasNutrition = false)),
                isLoading = false,
                isCalculating = true,
            ),
        ) { intents += it }

        composeRule.onNodeWithTag(MealDetailV2CalculatingTag).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.history_detail_v2_food_pending)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.history_detail_v2_edit_food_description, "김치찌개"))
            .performScrollTo().performClick()

        assertEquals(emptyList<MealDetailV2Intent>(), intents)
    }

    @Test
    fun 계산_중_안내는_마지막_음식_줄과_음식_추가_사이에_뜬다() {
        setScreen(MealDetailV2UIState(meal = meal, foods = foods, isLoading = false, isCalculating = true))

        val notice = composeRule.onNodeWithTag(MealDetailV2CalculatingTag).performScrollTo().getUnclippedBoundsInRoot()
        val lastFood = composeRule.onNodeWithText("불고기").getUnclippedBoundsInRoot()
        val add = composeRule.onNodeWithText(text(R.string.history_detail_v2_add_food)).getUnclippedBoundsInRoot()

        assertTrue(lastFood.bottom <= notice.top)
        assertTrue(notice.bottom <= add.top)
    }

    /** 순서를 바꾸면 ViewModel처럼 목록에 곧바로 반영하는 화면. 받은 순서 변경을 [moves]에 모은다. */
    private fun setReorderableScreen(moves: MutableList<MealDetailV2Intent.MoveFood>) {
        var state by mutableStateOf(MealDetailV2UIState(meal = meal, foods = foods, isLoading = false))
        composeRule.setContent {
            NyummyTheme {
                MealDetailV2Screen(
                    uiState = state,
                    onIntent = { intent ->
                        if (intent is MealDetailV2Intent.MoveFood) {
                            moves += intent
                            val moved = state.foods.toMutableList().apply { add(intent.toIndex, removeAt(intent.fromIndex)) }
                            state = state.copy(foods = moved.toImmutableList())
                        }
                    },
                )
            }
        }
    }

    private fun topOf(name: String): Dp = composeRule.onNodeWithText(name).getUnclippedBoundsInRoot().top

    private fun handleOf(name: String) =
        composeRule.onNodeWithContentDescription(text(R.string.history_detail_v2_drag_description, name))

    @Test
    fun 끌어_놓은_줄과_비켜_준_줄이_튀지_않고_새_자리에_앉는다() {
        val moves = mutableListOf<MealDetailV2Intent.MoveFood>()
        setReorderableScreen(moves)
        val handle = handleOf("밥")
        handle.performScrollTo()
        composeRule.waitForIdle()
        val firstSlot = topOf("밥")
        val secondSlot = topOf("된장찌개")
        val stridePx = with(composeRule.density) { (secondSlot - firstSlot).toPx() }

        // 첫 줄을 한 칸 아래로 끌어 놓고, 놓은 뒤 프레임마다 두 줄이 첫 칸과 둘째 칸 사이에만 있는지 본다.
        composeRule.mainClock.autoAdvance = false
        handle.performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, stridePx / 10)) }
        }
        // 둘째 줄이 첫 칸으로 다 비켜난 뒤에 놓는다.
        composeRule.mainClock.advanceTimeBy(1_000)
        assertEquals(firstSlot, topOf("된장찌개"))
        handle.performTouchInput { up() }
        val allowed = (firstSlot - 1.dp)..(secondSlot + 1.dp)
        repeat(60) { frame ->
            composeRule.mainClock.advanceTimeByFrame()
            val dragged = topOf("밥")
            val shifted = topOf("된장찌개")
            assertTrue("프레임 $frame: 끌어 놓은 줄 위치 $dragged", dragged in allowed)
            assertTrue("프레임 $frame: 비켜 준 줄 위치 $shifted", shifted in allowed)
        }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertEquals(listOf(MealDetailV2Intent.MoveFood(0, 1)), moves)
        assertEquals(firstSlot, topOf("된장찌개"))
        assertEquals(secondSlot, topOf("밥"))
    }

    @Test
    fun 끄는_줄은_목록_밖으로_나가지_않는다() {
        val moves = mutableListOf<MealDetailV2Intent.MoveFood>()
        setReorderableScreen(moves)
        val handle = handleOf("밥")
        handle.performScrollTo()
        composeRule.waitForIdle()
        val firstSlot = topOf("밥")
        val thirdSlot = topOf("불고기")
        val stridePx = with(composeRule.density) { (topOf("된장찌개") - firstSlot).toPx() }

        // 첫 줄을 위로 세 칸만큼 끌어도 첫 칸에 머문다.
        composeRule.mainClock.autoAdvance = false
        handle.performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, -stridePx * 3 / 10)) }
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        assertEquals(firstSlot, topOf("밥"))

        // 그대로 아래로 열 칸만큼 끌어도 마지막 칸에 머문다.
        handle.performTouchInput { repeat(10) { moveBy(Offset(0f, stridePx)) } }
        composeRule.mainClock.advanceTimeBy(1_000)
        assertEquals(thirdSlot, topOf("밥"))

        handle.performTouchInput { up() }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(listOf(MealDetailV2Intent.MoveFood(0, 2)), moves)
    }

    @Test
    fun 한_줄을_끄는_동안_다른_줄은_끌리지_않는다() {
        val moves = mutableListOf<MealDetailV2Intent.MoveFood>()
        setReorderableScreen(moves)
        val handle = handleOf("밥")
        handle.performScrollTo()
        composeRule.waitForIdle()
        val firstSlot = topOf("밥")
        val secondSlot = topOf("된장찌개")
        val thirdSlot = topOf("불고기")
        val stridePx = with(composeRule.density) { (secondSlot - firstSlot).toPx() }

        // 첫 줄을 한 칸 아래로 끈 채, 다른 손가락으로 셋째 줄 손잡이를 위로 끈다.
        composeRule.mainClock.autoAdvance = false
        handle.performTouchInput {
            down(0, center)
            repeat(10) { moveBy(0, Offset(0f, stridePx / 10)) }
        }
        composeRule.mainClock.advanceTimeBy(1_000)
        val draggedTop = topOf("밥")
        handle.performTouchInput {
            down(1, center + Offset(0f, stridePx * 2))
            repeat(10) { moveBy(1, Offset(0f, -stridePx / 10)) }
        }
        composeRule.mainClock.advanceTimeBy(1_000)

        assertEquals(thirdSlot, topOf("불고기"))
        assertEquals(draggedTop, topOf("밥"))
        assertEquals(firstSlot, topOf("된장찌개"))

        handle.performTouchInput {
            up(1)
            up(0)
        }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(listOf(MealDetailV2Intent.MoveFood(0, 1)), moves)
    }

    @Test
    fun 음식이_한_줄이면_순서_손잡이와_안내가_없다() {
        setScreen(MealDetailV2UIState(meal = meal, foods = foods.take(1).toImmutableList(), isLoading = false))

        composeRule.onAllNodesWithText(text(R.string.history_detail_v2_foods_hint)).assertCountEquals(0)
    }

    @Test
    fun 음식이_열_줄이면_음식_추가가_없다() {
        val ten = List(MealFoodVO.MAX_FOODS) { MealFoodVO(it + 1L, "음식${it + 1}", 1.0, true, 100, 1, 1, 1) }
        setScreen(MealDetailV2UIState(meal = meal, foods = ten.toImmutableList(), isLoading = false))

        composeRule.onAllNodesWithText(text(R.string.history_detail_v2_add_food)).assertCountEquals(0)
    }
}
