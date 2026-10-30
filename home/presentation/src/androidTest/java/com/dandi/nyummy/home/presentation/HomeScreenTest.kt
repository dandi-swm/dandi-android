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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.common.entity.cat.CatName
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteClip
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpriteSheet
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.home.entity.HomeSummaryVO
import com.dandi.nyummy.home.presentation.component.HomeHud
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.NumberFormat

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun text(id: Int, vararg args: Any): String = context.getString(id, *args)

    /** 고양이 이름이 든 문구. 이름을 공급하지 않으면 화면은 기본 이름을 쓴다. */
    private fun catText(id: Int): String =
        CatName.resolveParticles(context.getString(id, context.getString(CommonR.string.nyummy_cat_default_name)))

    private val recordedSummary = HomeSummaryVO(
        coinBalance = 1240,
        streakDays = 7,
        todayRecordedCount = 3,
        todayCalorieKcal = 1350,
        goalCalorieKcal = 1800,
    )

    private fun setScreen(state: HomeUIState, onIntent: (HomeIntent) -> Unit = {}) {
        composeRule.setContent { NyummyTheme { HomeScreen(uiState = state, onIntent = onIntent) } }
    }

    @Test
    fun 상단에_보유_코인과_연속_기록_일수를_보여준다() {
        setScreen(HomeUIState(summary = recordedSummary))

        composeRule.onNodeWithText(NumberFormat.getIntegerInstance().format(1240), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_streak_days, 7), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(catText(R.string.home_streak_message), useUnmergedTree = true).assertIsDisplayed()
    }

    // 테스트에서는 시트를 받지 못해 기본 냐미로 대신 그려진다.
    private val catMotion = HomeCatMotion(
        clips = persistentListOf(
            NyummySpriteClip(
                sheet = NyummySpriteSheet(
                    url = "file:///nyummy/not-found.png",
                    frameWidth = 136,
                    frameHeight = 136,
                    totalFrames = 8,
                    framesPerRow = 4,
                    frameDurationMillis = 100,
                ),
            ),
        ),
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
                catState = CatState.HOME_DAY,
                catLine = "이제 느긋하게 쉬어도 돼",
            ),
        ) { intents += it }

        composeRule.onNodeWithText("이제 느긋하게 쉬어도 돼").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_speech_recorded)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(catText(R.string.home_character_description)).performClick()

        // 냐미를 그리면 홈 TTI용 CatShown, 대사를 다 찍으면 CatLineRevealed도 보내므로 누른 의도만 본다.
        assertEquals(
            listOf<HomeIntent>(HomeIntent.ClickCat),
            intents.filterNot { it == HomeIntent.CatShown || it is HomeIntent.CatLineRevealed },
        )
    }

    @Test
    fun 냐미_애니메이션을_받지_못하면_기본_냐미와_기본_대사를_보여준다() {
        setScreen(HomeUIState(summary = recordedSummary.copy(todayRecordedCount = 0), isCatAnimationFailed = true))

        composeRule.onNodeWithContentDescription(catText(R.string.home_character_description)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_speech_waiting)).assertIsDisplayed()
    }

    @Test
    fun 냐미_애니메이션을_받는_중에는_냐미를_그리지_않는다() {
        setScreen(HomeUIState(summary = recordedSummary))

        composeRule.onNodeWithContentDescription(catText(R.string.home_character_description)).assertDoesNotExist()
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
