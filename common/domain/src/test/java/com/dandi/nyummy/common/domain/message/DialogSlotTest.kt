package com.dandi.nyummy.common.domain.message

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 다이얼로그 자리 하나에 새 다이얼로그를 받을지([canShowDialogOver]). 로그인 만료 같은 고정 다이얼로그를 지킨다. */
class DialogSlotTest {

    private val sessionExpired = oneButton("로그인 만료", pinned = true)
    private val notReady = oneButton("준비 중", cantIgnore = true)
    private val business = oneButton("가격이 바뀌었어요")
    private val confirmDelete = MessageEffect.ShowTwoButtonDialog(
        titleText = null,
        descText = "지울까요?",
        cantIgnore = false,
        leftButtonText = "취소",
        onClickLeftButton = null,
        rightButtonText = "지우기",
        onClickRightButton = null,
    )

    @Test
    fun `빈 자리나 고정하지 않은 다이얼로그 위에는 무엇이든 띄운다`() {
        assertTrue(canShowDialogOver(null, business))
        assertTrue(canShowDialogOver(business, notReady))
        assertTrue(canShowDialogOver(notReady, confirmDelete))
        // 로그인 만료 안내는 닫을 수 없는 다른 다이얼로그 위에도 뜬다.
        assertTrue(canShowDialogOver(notReady, sessionExpired))
    }

    @Test
    fun `고정 다이얼로그가 떠 있으면 비즈니스 안내와 닫을 수 없는 안내와 2버튼 다이얼로그를 버린다`() {
        assertFalse(canShowDialogOver(sessionExpired, business))
        assertFalse(canShowDialogOver(sessionExpired, notReady))
        assertFalse(canShowDialogOver(sessionExpired, confirmDelete))
    }

    @Test
    fun `고정 다이얼로그는 다른 고정 다이얼로그로만 바뀐다`() {
        assertTrue(canShowDialogOver(sessionExpired, oneButton("로그인 만료", pinned = true)))
    }

    private fun oneButton(text: String, cantIgnore: Boolean = false, pinned: Boolean = false) =
        MessageEffect.ShowOneButtonDialog(
            titleText = null,
            descText = text,
            cantIgnore = cantIgnore,
            buttonText = "확인",
            onClickButton = null,
            pinned = pinned,
        )
}
