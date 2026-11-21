package com.dandi.nyummy.shop.presentation.coin

import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.shop.entity.CoinChargePackageVO
import com.dandi.nyummy.shop.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 충전 확인 시트. 결제 응답을 기다리는 동안에는 어떤 방법으로도 닫히지 않아야 한다. */
@RunWith(AndroidJUnit4::class)
class CoinChargeConfirmSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val pack = CoinChargePackageVO(coinPackageId = 1L, coins = 1_000, price = 3_000)

    /** 시트 제목. 화면처럼 코인 수에 천 단위 쉼표를 넣는다. */
    private val title: String get() = context.getString(R.string.shop_charge_sheet_title, "1,000")

    private var dismissRequested = 0

    private fun showSheet(isPaying: Boolean) {
        composeRule.setContent {
            NyummyTheme {
                CoinChargeConfirmSheet(
                    pack = pack,
                    balance = 120,
                    isPaying = isPaying,
                    onPay = {},
                    onDismiss = { dismissRequested++ },
                )
            }
        }
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()
    }

    /** 시트 창의 맨 위(시트 바깥 딤)를 누른다. 시트 내용이 짧아 위쪽은 딤이다. */
    private fun clickOutside() {
        composeRule.onAllNodes(isRoot()).onLast().performTouchInput { click(Offset(centerX, height * 0.1f)) }
        composeRule.waitForIdle()
    }

    @Test
    fun 결제_중에는_뒤로_가기로_닫히지_않는다() {
        showSheet(isPaying = true)

        pressBack()

        composeRule.onNodeWithText(title).assertIsDisplayed()
        assertEquals(0, dismissRequested)
    }

    @Test
    fun 결제_중에는_바깥을_눌러도_닫히지_않는다() {
        showSheet(isPaying = true)

        clickOutside()

        composeRule.onNodeWithText(title).assertIsDisplayed()
        assertEquals(0, dismissRequested)
    }

    @Test
    fun 결제_중에는_끌어내려도_닫히지_않는다() {
        showSheet(isPaying = true)

        composeRule.onNodeWithText(title).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(title).assertIsDisplayed()
        assertEquals(0, dismissRequested)
    }

    @Test
    fun 결제_중에는_취소_버튼을_누를_수_없다() {
        showSheet(isPaying = true)

        composeRule.onNodeWithText(context.getString(R.string.shop_purchase_cancel)).assertIsNotEnabled()
    }

    @Test
    fun 결제_전에는_뒤로_가기로_닫기를_요청한다() {
        showSheet(isPaying = false)

        pressBack()

        assertEquals(1, dismissRequested)
    }

    @Test
    fun 결제_전에는_바깥을_누르면_닫기를_요청한다() {
        showSheet(isPaying = false)

        clickOutside()

        assertEquals(1, dismissRequested)
    }
}
