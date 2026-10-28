package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NyummyNoticeDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val hideToday get() = context.getString(R.string.nyummy_notice_hide_today)
    private val close get() = context.getString(R.string.nyummy_dialog_dismiss)

    private var todayChanges = mutableListOf<Boolean>()
    private var closes = 0
    private var ctaPages = mutableListOf<Int>()

    private val update = NyummyNoticePage(badge = "업데이트", imageUrl = null, title = UpdateTitle, body = "화면을 새로 꾸몄어요.")
    private val event = NyummyNoticePage(
        badge = "이벤트",
        imageUrl = null,
        meta = "10월 28일 ~ 11월 10일",
        title = EventTitle,
        body = "이벤트 페이지에서 자세히 알아보세요.",
        ctaText = CtaText,
    )

    private fun setContent(
        pages: ImmutableList<NyummyNoticePage>,
        todayChecked: Boolean = false,
        modifier: Modifier = Modifier.size(width = 334.dp, height = 760.dp),
    ) {
        composeRule.setContent {
            NyummyTheme {
                Box(modifier) {
                    NyummyNoticeContent(
                        pages = pages,
                        todayChecked = todayChecked,
                        onTodayCheckedChange = { todayChanges += it },
                        onClose = { closes++ },
                        onCtaClick = { ctaPages += it },
                    )
                }
            }
        }
    }

    @Test
    fun 옆으로_밀면_다음_공지가_보이고_아래_줄은_움직이지_않는다() {
        setContent(persistentListOf(update, event))
        val before = composeRule.onNodeWithText(close).getUnclippedBoundsInRoot()

        composeRule.onNodeWithText(UpdateTitle).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(EventTitle).assertIsDisplayed()
        composeRule.onNodeWithText(UpdateTitle).assertDoesNotExist()
        assertEquals(before, composeRule.onNodeWithText(close).getUnclippedBoundsInRoot())
    }

    @Test
    fun 공지가_하나면_점이_없다() {
        setContent(persistentListOf(update))

        composeRule.onNodeWithTag(NoticeDotsTestTag).assertDoesNotExist()
    }

    @Test
    fun 공지가_여럿이면_점이_보인다() {
        setContent(persistentListOf(update, event))

        composeRule.onNodeWithTag(NoticeDotsTestTag).assertIsDisplayed()
    }

    @Test
    fun 버튼이_없는_공지는_버튼_없이_제목과_설명만_보인다() {
        setContent(persistentListOf(update))

        composeRule.onNodeWithText(UpdateTitle).assertIsDisplayed()
        composeRule.onNodeWithText("화면을 새로 꾸몄어요.").assertIsDisplayed()
        composeRule.onNodeWithText(CtaText).assertDoesNotExist()
    }

    @Test
    fun 버튼을_누르면_그_공지의_순서를_알린다() {
        setContent(persistentListOf(update, event))
        composeRule.onNodeWithText(UpdateTitle).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(CtaText).performClick()

        assertEquals(listOf(1), ctaPages)
    }

    @Test
    fun 오늘_하루_보지_않기는_체크를_바꾸고_닫기는_닫는다() {
        setContent(persistentListOf(update), todayChecked = false)

        composeRule.onNodeWithText(hideToday).performClick()
        composeRule.onNodeWithText(close).performClick()

        assertEquals(listOf(true), todayChanges)
        assertEquals(1, closes)
    }

    @Test
    fun 작은_화면에서도_버튼과_아래_줄이_화면_안에_있다() {
        // 360×640 화면에서 시스템 바(약 72)와 다이얼로그 위아래 여백(48)을 뺀 영역, 좌우 여백 28.
        setContent(persistentListOf(event, update), modifier = Modifier.size(width = 304.dp, height = 520.dp))

        val row = composeRule.onNodeWithText(close).getUnclippedBoundsInRoot()
        assertTrue("닫기 줄이 영역을 넘었다: ${row.bottom}", row.bottom <= 520.dp)
        composeRule.onNodeWithText(CtaText).assertIsDisplayed()
    }

    private companion object {
        const val UpdateTitle = "냐미가 새단장을 했어요!"
        const val EventTitle = "가을 한 상 이벤트가 열렸어요"
        const val CtaText = "자세히 보기"
    }
}
