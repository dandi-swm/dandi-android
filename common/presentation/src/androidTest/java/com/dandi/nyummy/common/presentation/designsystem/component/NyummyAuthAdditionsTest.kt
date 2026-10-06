package com.dandi.nyummy.common.presentation.designsystem.component

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private companion object {
        val CodeInputWidth = 358.dp
    }
}
