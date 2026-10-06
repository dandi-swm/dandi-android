package com.dandi.nyummy.common.presentation.designsystem.component

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Intro와 Auth 이전 중에 추가하거나 보완한 공통 컴포넌트 계약. */
@RunWith(AndroidJUnit4::class)
class NyummyAuthAdditionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun 오류_안내_카드는_접근성_서비스에_바로_알리고_안내_카드는_알리지_않는다() {
        composeRule.setContent {
            NyummyTheme {
                Column {
                    NyummyInlineNotice(title = "안내", body = "프로필은 바꿀 수 없어요.")
                    NyummyInlineNotice(title = "오류", body = "이름을 입력해주세요.", tone = NyummyInlineNoticeTone.Danger)
                }
            }
        }

        composeRule.onNodeWithText("오류", substring = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        composeRule.onNodeWithText("안내", substring = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun 다이얼로그_닫기_버튼은_onDismissClick을_따로_호출한다() {
        var dismissRequested = 0
        var dismissClicked = 0
        composeRule.setContent {
            NyummyTheme {
                NyummyDialog(
                    title = "권한이 필요해요",
                    confirmText = "설정으로 이동",
                    onConfirm = {},
                    onDismissRequest = { dismissRequested++ },
                    onDismissClick = { dismissClicked++ },
                )
            }
        }

        composeRule.onNodeWithText("닫기").performClick()

        assertEquals(1, dismissClicked)
        assertEquals(0, dismissRequested)
    }

    @Test
    fun 닫을_수_없는_다이얼로그는_뒤로_가기로_닫히지_않는다() {
        var dismissRequested = 0
        composeRule.setContent {
            NyummyTheme {
                NyummyDialog(
                    title = "업데이트가 필요해요",
                    confirmText = "업데이트",
                    onConfirm = {},
                    onDismissRequest = { dismissRequested++ },
                    type = NyummyDialogType.Alert,
                    dismissible = false,
                )
            }
        }

        composeRule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()

        composeRule.onNode(isDialog()).assertExists()
        assertEquals(0, dismissRequested)
    }

    @Test
    fun nyummyClickable은_버튼_역할로_클릭을_전달하고_비활성이면_막는다() {
        var clicks = 0
        composeRule.setContent {
            NyummyTheme {
                Column {
                    NyummyText(text = "다시 보내기", style = NyummyTheme.typography.bodyM, modifier = Modifier.nyummyClickable(onClick = { clicks++ }))
                    NyummyText(text = "막힘", style = NyummyTheme.typography.bodyM, modifier = Modifier.nyummyClickable(onClick = { clicks++ }, enabled = false))
                }
            }
        }

        composeRule.onNodeWithText("다시 보내기")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        composeRule.onNodeWithText("막힘").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun 인증_코드_셀은_받은_폭의_양끝까지_펼친다() {
        composeRule.setContent {
            NyummyTheme {
                Box(Modifier.width(CodeInputWidth)) {
                    NyummyCodeInput(value = "123456", onValueChange = {}, length = 6)
                }
            }
        }

        val first = composeRule.onNodeWithText("1", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val last = composeRule.onNodeWithText("6", useUnmergedTree = true).getUnclippedBoundsInRoot()
        // 숫자는 48dp 셀 가운데에 있으므로 첫 셀 중심은 24, 마지막 셀 중심은 폭 - 24다.
        assertEquals(24f, ((first.left + first.right) / 2).value, 1f)
        assertEquals((CodeInputWidth - 24.dp).value, ((last.left + last.right) / 2).value, 1f)
    }

    @Test
    fun 닫을_수_없는_다이얼로그는_바깥을_눌러도_닫히지_않는다() {
        var dismissRequested = 0
        composeRule.setContent {
            NyummyTheme {
                NyummyDialog(
                    title = "업데이트가 필요해요",
                    confirmText = "업데이트",
                    onConfirm = {},
                    onDismissRequest = { dismissRequested++ },
                    type = NyummyDialogType.Alert,
                    dismissible = false,
                )
            }
        }

        composeRule.onNode(isDialog()).performTouchInput { click(Offset(10f, 10f)) }

        composeRule.onNode(isDialog()).assertExists()
        assertEquals(0, dismissRequested)
    }

    @Test
    fun 닫을_수_있는_다이얼로그는_바깥을_누르면_닫기를_요청한다() {
        var dismissRequested = 0
        composeRule.setContent {
            NyummyTheme {
                NyummyDialog(
                    title = "기록을 그만둘까요?",
                    confirmText = "나가기",
                    onConfirm = {},
                    onDismissRequest = { dismissRequested++ },
                )
            }
        }

        composeRule.onNode(isDialog()).performTouchInput { click(Offset(10f, 10f)) }

        assertEquals(1, dismissRequested)
    }

    @Test
    fun 스크림은_아래_화면의_클릭을_막고_onClick이_있으면_닫기_동작을_알린다() {
        var underneath = 0
        var scrimClicks = 0
        composeRule.setContent {
            NyummyTheme {
                Box(Modifier.size(200.dp)) {
                    NyummyButton(text = "아래 버튼", onClick = { underneath++ })
                    NyummyScrim(modifier = Modifier.testTag("scrim"), onClick = { scrimClicks++ })
                }
            }
        }

        composeRule.onNodeWithText("아래 버튼").performClick()
        composeRule.onNodeWithTag("scrim")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .performClick()

        assertEquals(0, underneath)
        assertEquals(2, scrimClicks)
    }

    @Test
    fun 입력만_막는_스크림은_접근성_동작이_없다() {
        composeRule.setContent {
            NyummyTheme { NyummyScrim(modifier = Modifier.testTag("scrim")) }
        }

        composeRule.onNodeWithTag("scrim").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun nyummyClickable은_글자보다_작아도_터치_영역이_48dp다() {
        composeRule.setContent {
            NyummyTheme {
                NyummyText(
                    text = "다시 보내기",
                    style = NyummyTheme.typography.labelM,
                    modifier = Modifier.nyummyClickable(onClick = {}),
                )
            }
        }

        composeRule.onNodeWithText("다시 보내기")
            .assertTouchHeightIsEqualTo(48.dp)
            .assertHeightIsAtLeast(1.dp)
    }

    @Test
    fun 스피너는_설명이_있을_때만_읽힌다() {
        composeRule.setContent {
            NyummyTheme {
                Column {
                    NyummySpinner(contentDescription = "불러오는 중", modifier = Modifier.testTag("with"))
                    NyummySpinner(contentDescription = null, modifier = Modifier.testTag("without"))
                }
            }
        }

        composeRule.onNodeWithTag("with").assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("불러오는 중")))
        composeRule.onNodeWithTag("without").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    private companion object {
        val CodeInputWidth = 358.dp
    }
}
