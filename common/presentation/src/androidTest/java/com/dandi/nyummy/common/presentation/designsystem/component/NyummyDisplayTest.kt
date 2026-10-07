package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NyummyDisplayTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    @Test
    fun 정보_카드는_누를_수_없고_누를_수_있는_카드는_onClick을_호출한다() {
        var clicks = 0
        composeRule.setContent {
            NyummyTheme {
                androidx.compose.foundation.layout.Column {
                    NyummyCard(title = "정보 카드", body = "본문", modifier = Modifier.testTag("info"))
                    NyummyCard(title = "누르는 카드", body = "본문", onClick = { clicks++ }, modifier = Modifier.testTag("pressable"))
                }
            }
        }

        composeRule.onNodeWithTag("info").assertHasNoClickAction()
        composeRule.onNodeWithTag("pressable").assert(hasRole(Role.Button)).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun 이동_행은_행_전체를_누르면_onClick이_호출된다() {
        var clicks = 0
        composeRule.setContent {
            NyummyTheme {
                NyummyListRow(title = "알림", subtitle = "끼니 시간에 알려 드려요", leadingIcon = R.drawable.nyummy_ic_bell, onClick = { clicks++ })
            }
        }

        composeRule.onNodeWithText("알림").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun 목록_행은_최소_높이가_56dp다() {
        composeRule.setContent {
            NyummyTheme { NyummyListRow(title = "한 줄", trailing = NyummyListRowTrailing.None, modifier = Modifier.testTag("row")) }
        }

        composeRule.onNodeWithTag("row").assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun 스위치_행은_행을_누르면_토글되고_역할이_스위치다() {
        composeRule.setContent {
            NyummyTheme {
                var on by remember { mutableStateOf(false) }
                NyummyListRow(
                    title = "알림",
                    trailing = NyummyListRowTrailing.Switch(on) { on = it },
                    modifier = Modifier.testTag("row"),
                )
            }
        }

        composeRule.onNodeWithTag("row").assert(hasRole(Role.Switch)).assertIsOff().performClick()
        composeRule.onNodeWithTag("row").assertIsOn()
    }

    @Test
    fun 체크박스_행과_라디오_행은_행을_눌러_선택한다() {
        composeRule.setContent {
            NyummyTheme {
                var checked by remember { mutableStateOf(false) }
                var selected by remember { mutableStateOf(false) }
                androidx.compose.foundation.layout.Column {
                    NyummyListRow(title = "동의", trailing = NyummyListRowTrailing.Checkbox(checked) { checked = it }, modifier = Modifier.testTag("check"))
                    NyummyListRow(title = "아침", trailing = NyummyListRowTrailing.Radio(selected) { selected = true }, modifier = Modifier.testTag("radio"))
                }
            }
        }

        composeRule.onNodeWithTag("check").assert(hasRole(Role.Checkbox)).performClick()
        composeRule.onNodeWithTag("check").assertIsOn()
        composeRule.onNodeWithTag("radio").assert(hasRole(Role.RadioButton)).performClick()
        composeRule.onNodeWithTag("radio").assertIsSelected()
    }

    @Test
    fun 값_행은_현재_값을_보여준다() {
        composeRule.setContent {
            NyummyTheme { NyummyListRow(title = "앱 버전", trailing = NyummyListRowTrailing.Value("v1.2.0"), onClick = {}) }
        }

        composeRule.onNodeWithText("v1.2.0", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun 배지는_글자를_항상_보여준다() {
        composeRule.setContent {
            NyummyTheme { NyummyBadge(text = "분석 중", tone = NyummyBadgeTone.Warning) }
        }

        composeRule.onNodeWithText("분석 중").assertIsDisplayed()
    }

    @Test
    fun 코인_알약은_코인_수를_읽어주고_더하기가_있으면_누를_수_있다() {
        var clicks = 0
        composeRule.setContent {
            NyummyTheme {
                androidx.compose.foundation.layout.Column {
                    NyummyCoinPill(coins = "1,240", modifier = Modifier.testTag("plain"))
                    NyummyCoinPill(coins = "300", onAddClick = { clicks++ })
                }
            }
        }

        composeRule.onNodeWithTag("plain").assertHasNoClickAction()
        composeRule.onNodeWithContentDescription("코인 300").assertHasClickAction().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun 스트릭_알약은_연속_기록_일수를_읽어준다() {
        composeRule.setContent {
            NyummyTheme { NyummyStreakPill(days = 7) }
        }

        composeRule.onNodeWithContentDescription("연속 기록 7일째").assertIsDisplayed()
    }

    @Test
    fun 막대_진행률은_0에서_1_사이로_잘린다() {
        composeRule.setContent {
            NyummyTheme {
                androidx.compose.foundation.layout.Column {
                    NyummyLinearProgress(progress = 0.45f, modifier = Modifier.width(200.dp).testTag("half"))
                    NyummyLinearProgress(progress = 1.7f, modifier = Modifier.width(200.dp).testTag("over"))
                }
            }
        }

        composeRule.onNodeWithTag("half").assertRangeInfoEquals(ProgressBarRangeInfo(0.45f, 0f..1f))
        composeRule.onNodeWithTag("over").assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..1f))
    }

    @Test
    fun 막대_진행률은_기본_10_얇은_크기_6_두께다() {
        composeRule.setContent {
            NyummyTheme {
                androidx.compose.foundation.layout.Column {
                    NyummyLinearProgress(progress = 0.5f, modifier = Modifier.width(200.dp).testTag("m"))
                    NyummyLinearProgress(
                        progress = 0.5f,
                        size = NyummyLinearProgressSize.S,
                        modifier = Modifier.width(200.dp).testTag("s"),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("m").assertHeightIsEqualTo(10.dp)
        composeRule.onNodeWithTag("s").assertHeightIsEqualTo(6.dp)
    }

    @Test
    fun 영양소_칸은_이름과_그램을_한_덩어리로_읽는다() {
        composeRule.setContent {
            NyummyTheme { NyummyNutrientStat(NyummyNutrient.Protein, grams = 42, modifier = Modifier.testTag("stat")) }
        }

        composeRule.onNodeWithTag("stat").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Text,
                listOf(
                    androidx.compose.ui.text.AnnotatedString("단백질"),
                    androidx.compose.ui.text.AnnotatedString("42g"),
                ),
            ),
        )
    }

    @Test
    fun 식사_행은_이름_시각_배지를_보여주고_onClick이_있을_때만_누를_수_있다() {
        var clicks = 0
        composeRule.setContent {
            NyummyTheme {
                androidx.compose.foundation.layout.Column {
                    NyummyMealRow(
                        title = "닭가슴살 샐러드",
                        subtitle = "오후 12:24",
                        leading = { NyummyMealRowPlaceholder() },
                        modifier = Modifier.testTag("static"),
                    )
                    NyummyMealRow(
                        title = "분석 중이에요",
                        subtitle = "오후 6:40",
                        leading = { NyummyMealRowPlaceholder() },
                        badge = "분석 중",
                        badgeTone = NyummyBadgeTone.Info,
                        onClick = { clicks++ },
                        modifier = Modifier.testTag("clickable"),
                    )
                }
            }
        }

        composeRule.onNodeWithText("오후 12:24", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("분석 중", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("static").assertHasNoClickAction()
        composeRule.onNodeWithTag("clickable").assertHasClickAction().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun 원형_로딩은_끝을_알_수_없는_진행으로_알린다() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            NyummyTheme { NyummyCircularProgress(modifier = Modifier.testTag("loading")) }
        }

        composeRule.onNodeWithTag("loading").assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
    }

    @Test
    fun 섹션_헤더의_전체보기를_누르면_호출된다() {
        var clicks = 0
        composeRule.setContent {
            NyummyTheme { NyummySectionHeader(title = "오늘의 식사", actionText = "전체보기", onActionClick = { clicks++ }) }
        }

        composeRule.onNodeWithText("오늘의 식사").assertIsDisplayed()
        composeRule.onNodeWithText("전체보기").performClick()

        assertEquals(1, clicks)
    }
}
