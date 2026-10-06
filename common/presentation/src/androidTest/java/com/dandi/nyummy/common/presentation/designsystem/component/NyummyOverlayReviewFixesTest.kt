package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** #92 리뷰에서 지적된 좁은 화면, 큰 글꼴, 닫기 동작 문제를 확인한다. */
@RunWith(AndroidJUnit4::class)
class NyummyOverlayReviewFixesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val longText = List(30) { "냐미가 밥을 기다리고 있어요." }.joinToString(" ")

    @Test
    fun 다이얼로그_본문이_길어도_버튼은_카드_안에_보인다() {
        composeRule.setContent {
            NyummyTheme {
                Box(Modifier.width(320.dp).height(300.dp).testTag("area")) {
                    NyummyDialogCard(title = "제목", confirmText = "확인", onConfirm = {}, onDismiss = {}, body = longText)
                }
            }
        }

        val area = composeRule.onNodeWithTag("area").getBoundsInRoot()
        val confirm = composeRule.onNodeWithText("확인").getBoundsInRoot()
        assertTrue("확인 버튼 아래 ${confirm.bottom} <= 영역 아래 ${area.bottom}", confirm.bottom <= area.bottom)
        composeRule.onNodeWithText("확인").assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun 확인_시트는_높이가_작아도_버튼이_보인다() {
        composeRule.setContent {
            NyummyTheme {
                Box(Modifier.width(390.dp).height(280.dp).testTag("area")) {
                    NyummyConfirmSheetContent(
                        title = "기록을 그만둘까요?",
                        body = longText,
                        primaryText = "계속 기록하기",
                        onPrimary = {},
                        secondaryText = "그만두기",
                        onSecondary = {},
                        pose = NyummyPose.Worry,
                        destructive = false,
                    )
                }
            }
        }

        val area = composeRule.onNodeWithTag("area").getBoundsInRoot()
        val secondary = composeRule.onNodeWithText("그만두기").getBoundsInRoot()
        assertTrue("그만두기 아래 ${secondary.bottom} <= 영역 아래 ${area.bottom}", secondary.bottom <= area.bottom)
        composeRule.onNodeWithText("계속 기록하기").assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun 스낵바는_큰_글꼴의_두_줄_문구에서_늘어난다() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                NyummyTheme {
                    NyummySnackbar(
                        message = "식사 기록을 삭제했어요. 실행 취소를 누르면 되돌릴 수 있어요",
                        actionLabel = "실행 취소",
                        modifier = Modifier.width(350.dp).testTag("snackbar"),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("snackbar").assertHeightIsAtLeast(80.dp)
    }

    @Test
    fun 한_줄_스낵바는_높이_56을_유지한다() {
        composeRule.setContent {
            NyummyTheme { NyummySnackbar(message = "저장했어요", modifier = Modifier.width(350.dp).testTag("snackbar")) }
        }

        composeRule.onNodeWithTag("snackbar").assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun 닫기_요청한_스낵바는_X로_닫힌다() {
        var result: SnackbarResult? = null
        composeRule.setContent {
            NyummyTheme {
                val hostState = remember { SnackbarHostState() }
                LaunchedEffect(Unit) {
                    result = hostState.showSnackbar("업로드 중이에요", withDismissAction = true, duration = SnackbarDuration.Indefinite)
                }
                NyummySnackbarHost(hostState = hostState)
            }
        }

        composeRule.onNodeWithContentDescription("알림 닫기").performClick()
        composeRule.waitForIdle()

        assertEquals(SnackbarResult.Dismissed, result)
        composeRule.onNodeWithText("업로드 중이에요").assertDoesNotExist()
    }

    @Test
    fun 닫기를_요청하지_않으면_X가_없다() {
        composeRule.setContent {
            NyummyTheme { NyummySnackbar(message = "저장했어요") }
        }

        composeRule.onNodeWithContentDescription("알림 닫기").assertDoesNotExist()
    }
}
