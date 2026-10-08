package com.dandi.nyummy.home.presentation

import android.content.Context
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteClip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteFrame
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.entity.NutrientProgressVO
import com.dandi.nyummy.home.entity.HomeSummaryVO
import com.dandi.nyummy.home.presentation.component.HomeHud
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.NumberFormat
import com.dandi.nyummy.common.presentation.R as CommonR

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    private val recordedSummary = HomeSummaryVO(
        coinBalance = 1240,
        streakDays = 7,
        todayRecordedCount = 3,
        todayCalorieKcal = 1350,
        goalCalorieKcal = 1800,
    )

    private val meals = persistentListOf(
        MealHistoryVO(id = "1", name = "토스트와 우유", recordedAt = "08:10"),
        MealHistoryVO(id = "2", name = "닭가슴살 샐러드", recordedAt = "12:24"),
        MealHistoryVO(id = "3", recordedAt = "18:40", status = MealAnalysisStatus.ANALYZING),
    )

    private val nutrition = DailyNutritionVO(
        currentCalorieKcal = 1350,
        targetCalorieKcal = 1800,
        carbohydrate = NutrientProgressVO(dailyGram = 185, goalGram = 250),
        protein = NutrientProgressVO(dailyGram = 42, goalGram = 60),
        fat = NutrientProgressVO(dailyGram = 31, goalGram = 50),
    )

    private fun setScreen(state: HomeUIState, onIntent: (HomeIntent) -> Unit = {}) {
        composeRule.setContent { NyummyTheme { HomeScreen(uiState = state, onIntent = onIntent) } }
    }

    @Test
    fun 상단에_보유_코인과_연속_기록_일수를_보여준다() {
        setScreen(HomeUIState(summary = recordedSummary))

        composeRule.onNodeWithText(NumberFormat.getIntegerInstance().format(1240), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_streak_days, 7), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_streak_message), useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun 기록_전에는_첫_끼_기록하기를_보여주고_누르면_오늘_바_의도를_보낸다() {
        val intents = mutableListOf<HomeIntent>()
        setScreen(HomeUIState(summary = recordedSummary.copy(todayRecordedCount = 0))) { intents += it }

        composeRule.onNodeWithText(text(R.string.home_speech_waiting)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_today_empty_body)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_today_empty_title)).performClick()

        assertEquals(listOf<HomeIntent>(HomeIntent.ClickTodayBar), intents)
    }

    @Test
    fun 기록_후에는_오늘_기록_횟수와_고마움_대사를_보여준다() {
        setScreen(HomeUIState(summary = recordedSummary))

        composeRule.onNodeWithText(text(R.string.home_today_record_count, 3)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_speech_recorded)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_today_empty_title)).assertDoesNotExist()
    }

    @Test
    fun 방_메뉴는_접혀_있다가_펼치면_세_가지_메뉴가_나온다() {
        val intents = mutableListOf<HomeIntent>()
        setScreen(HomeUIState(summary = recordedSummary)) { intents += it }

        composeRule.onNodeWithContentDescription(text(R.string.home_menu_my_room)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(text(R.string.home_menu_expand)).performClick()
        assertEquals(listOf<HomeIntent>(HomeIntent.ToggleRoomMenu), intents)
    }

    @Test
    fun 펼친_방_메뉴의_각_버튼은_해당_의도를_보낸다() {
        val intents = mutableListOf<HomeIntent>()
        setScreen(HomeUIState(summary = recordedSummary, isRoomMenuExpanded = true)) { intents += it }

        composeRule.onNodeWithContentDescription(text(R.string.home_menu_my_room)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.home_menu_share_friend)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.home_menu_nyami_status)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.home_menu_collapse)).performClick()

        assertEquals(
            listOf(
                HomeIntent.ClickMyRoom,
                HomeIntent.ClickShareFriend,
                HomeIntent.ClickNyamiStatus,
                HomeIntent.ToggleRoomMenu,
            ),
            intents,
        )
    }

    @Test
    fun 오늘_식사_시트는_탄단지_그램과_식사_목록을_보여주고_남은_kcal은_보여주지_않는다() {
        setScreen(
            HomeUIState(
                summary = recordedSummary,
                todayNutrition = nutrition,
                todayMeals = meals,
                isTodaySheetVisible = true,
            ),
        )

        composeRule.onNodeWithText(text(R.string.home_sheet_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_sheet_recorded, 3)).assertIsDisplayed()
        composeRule.onNodeWithText(text(CommonR.string.nyummy_nutrient_grams, 185), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(text(CommonR.string.nyummy_nutrient_grams, 42), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(text(CommonR.string.nyummy_nutrient_grams, 31), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("닭가슴살 샐러드", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(
            text(R.string.home_meal_time, text(R.string.home_meal_pm), 12, 24),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            text(R.string.home_meal_time, text(R.string.home_meal_am), 8, 10),
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_meal_analyzing_badge), useUnmergedTree = true).assertIsDisplayed()
        // 영양 압박 금지: 남은 kcal 문구가 없어야 한다.
        composeRule.onNodeWithText("kcal", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun 전에_읽은_내용이_있어도_다시_읽기에_실패하면_다시_시도를_보여준다() {
        setScreen(
            HomeUIState(
                summary = recordedSummary,
                todayNutrition = nutrition,
                todayMeals = meals,
                isTodaySheetVisible = true,
                isTodayMealsFailed = true,
            ),
        )

        composeRule.onNodeWithText(text(R.string.home_sheet_retry)).assertIsDisplayed()
        composeRule.onNodeWithText("닭가슴살 샐러드", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun 식사가_많아도_스크롤해서_식사_추가하기를_누를_수_있다() {
        val intents = mutableListOf<HomeIntent>()
        val many = (1..12).map { MealHistoryVO(id = "$it", name = "간식 $it", recordedAt = "15:00") }.toImmutableList()
        setScreen(
            HomeUIState(
                summary = recordedSummary,
                todayNutrition = nutrition,
                todayMeals = many,
                isTodaySheetVisible = true,
            ),
        ) { intents += it }

        composeRule.onNodeWithText(text(R.string.home_sheet_add_meal)).performScrollTo().performClick()

        assertEquals(listOf<HomeIntent>(HomeIntent.ClickAddMeal), intents)
    }

    @Test
    fun 오늘_식사를_읽지_못하면_다시_시도를_누를_수_있다() {
        val intents = mutableListOf<HomeIntent>()
        setScreen(
            HomeUIState(summary = recordedSummary, isTodaySheetVisible = true, isTodayMealsFailed = true),
        ) { intents += it }

        composeRule.onNodeWithText(text(R.string.home_sheet_load_failed)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_sheet_retry)).performClick()
        composeRule.onNodeWithText(text(R.string.home_sheet_add_meal)).performClick()

        assertEquals(listOf(HomeIntent.RetryTodayMeals, HomeIntent.ClickAddMeal), intents)
    }

    // 테스트에서는 시트를 받지 못해 기본 냐미로 대신 그려진다.
    private val catMotion = HomeCatMotion(
        clips = persistentListOf(NyummySpriteClip(url = "file:///nyummy/not-found.png", frames = 8)),
        frame = NyummySpriteFrame(width = 136, height = 136, framesPerRow = 4, durationMs = 100),
        sheetUrls = persistentListOf("file:///nyummy/not-found.png"),
        playId = 1,
        restMillis = 6_000L,
    )

    @Test
    fun 냐미_대사가_있으면_말풍선에_보여주고_냐미를_누르면_의도를_보낸다() {
        val intents = mutableListOf<HomeIntent>()
        setScreen(
            HomeUIState(
                summary = recordedSummary,
                catMotion = catMotion,
                catState = CatState.RELAXED,
                catLine = "이제 느긋하게 쉬어도 돼",
            ),
        ) { intents += it }

        composeRule.onNodeWithText("이제 느긋하게 쉬어도 돼").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_speech_recorded)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(text(R.string.home_character_description)).performClick()

        assertEquals(listOf<HomeIntent>(HomeIntent.ClickCat), intents)
    }

    @Test
    fun 냐미_애니메이션을_받지_못하면_기본_냐미와_기본_대사를_보여준다() {
        setScreen(HomeUIState(summary = recordedSummary.copy(todayRecordedCount = 0), isCatAnimationFailed = true))

        composeRule.onNodeWithContentDescription(text(R.string.home_character_description)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_speech_waiting)).assertIsDisplayed()
    }

    @Test
    fun 냐미_애니메이션을_받는_중에는_냐미를_그리지_않는다() {
        setScreen(HomeUIState(summary = recordedSummary))

        composeRule.onNodeWithContentDescription(text(R.string.home_character_description)).assertDoesNotExist()
    }

    @Test
    fun 좁은_화면에서도_지갑_카드의_코인_숫자가_잘리지_않는다() {
        // 360 화면에서 지갑 카드는 140 폭이다. 셰브론을 빼서 숫자 자리를 확보한다.
        composeRule.setContent {
            NyummyTheme {
                HomeHud(
                    coinBalance = 1240,
                    hasUnreadNotice = false,
                    onWalletClick = {},
                    onMailClick = {},
                    onNoticeClick = {},
                    onSettingsClick = {},
                    modifier = Modifier.width(320.dp),
                )
            }
        }

        val node = composeRule.onNodeWithText(NumberFormat.getIntegerInstance().format(1240), useUnmergedTree = true)
        node.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        assertFalse(layouts.single().isLineEllipsized(0))
        assertFalse(layouts.single().hasVisualOverflow)
    }
}
