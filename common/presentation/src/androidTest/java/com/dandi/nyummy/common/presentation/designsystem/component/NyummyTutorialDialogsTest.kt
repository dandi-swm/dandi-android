package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.entity.cat.CatName
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NyummyTutorialDialogsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** 이름을 공급하지 않으면 화면은 기본 이름을 쓴다. */
    private val catName: String get() = context.getString(R.string.nyummy_cat_default_name)

    private var continues = 0
    private var skips = 0
    private var nexts = 0

    private fun setSkipDialog() {
        composeRule.setContent {
            NyummyTheme {
                NyummyTutorialSkipDialog(
                    remaining = persistentListOf(NyummyTutorialLesson.OpenBox, NyummyTutorialLesson.CatHealth),
                    onContinue = { continues++ },
                    onSkip = { skips++ },
                )
            }
        }
    }

    @Test
    fun 건너뛰기_확인은_남은_안내를_보여_준다() {
        setSkipDialog()

        composeRule.onNodeWithText(context.getString(R.string.nyummy_tutorial_lesson_box)).assertIsDisplayed()
        composeRule.onNodeWithText(CatName.resolveParticles(context.getString(R.string.nyummy_tutorial_lesson_health, catName))).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.nyummy_tutorial_lesson_record)).assertDoesNotExist()
    }

    @Test
    fun 계속_배울래요를_누르면_계속한다() {
        setSkipDialog()

        composeRule.onNodeWithText(context.getString(R.string.nyummy_tutorial_skip_continue)).performClick()

        assertEquals(1, continues)
        assertEquals(0, skips)
    }

    @Test
    fun 그래도_건너뛸게요를_누르면_건너뛴다() {
        setSkipDialog()

        composeRule.onNodeWithText(context.getString(R.string.nyummy_tutorial_skip_anyway)).performClick()

        assertEquals(1, skips)
        assertEquals(0, continues)
    }

    @Test
    fun 건강_이야기는_장_번호와_체형_게이지를_보이고_다음을_부른다() {
        composeRule.setContent {
            NyummyTheme {
                NyummyHealthStoryDialog(
                    page = NyummyHealthStoryPage.Starving,
                    pageIndex = 1,
                    title = StoryTitle,
                    body = "다시 챙겨 주면 금방 돌아와!",
                    buttonText = NextText,
                    onNext = { nexts++ },
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.nyummy_health_story_chip, catName, 2, 3)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.nyummy_health_gauge_lean)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.nyummy_health_gauge_plump)).assertIsDisplayed()
        composeRule.onNodeWithText(StoryTitle).assertIsDisplayed()
        composeRule.onNodeWithText(NextText).performClick()

        assertEquals(1, nexts)
    }

    private companion object {
        const val StoryTitle = "끼니를 거르면 내가 홀쭉해져"
        const val NextText = "다음"
    }
}
