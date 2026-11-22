package com.dandi.nyummy.shop.presentation.cart

import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
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
import com.dandi.nyummy.shop.entity.CartSummaryVO
import com.dandi.nyummy.shop.entity.ShopItemVO
import com.dandi.nyummy.shop.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 장바구니 구매 확인 시트. 구매 응답을 기다리는 동안에는 어떤 방법으로도 닫히지 않아야 한다. */
@RunWith(AndroidJUnit4::class)
class PurchaseConfirmSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val items = listOf(ShopItemVO(productId = 1, itemId = 101, name = "상품 1", category = "BED"))

    private val summary = CartSummaryVO(quantity = 3, purchasePrice = 620, balance = 1_480, balanceAfter = 860)

    private val title: String get() = context.getString(R.string.shop_purchase_confirm_title, 3)

    private val cancel: String get() = context.getString(R.string.shop_purchase_cancel)

    private var dismissRequested = 0

    private fun showSheet(isPurchasing: Boolean) {
        composeRule.setContent {
            NyummyTheme {
                PurchaseConfirmSheet(
                    items = items,
                    summary = summary,
                    isPurchasing = isPurchasing,
                    onConfirm = {},
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
    fun 구매_중에는_뒤로_가기로_닫히지_않는다() {
        showSheet(isPurchasing = true)

        pressBack()

        composeRule.onNodeWithText(title).assertIsDisplayed()
        assertEquals(0, dismissRequested)
    }

    @Test
    fun 구매_중에는_바깥을_눌러도_닫히지_않는다() {
        showSheet(isPurchasing = true)

        clickOutside()

        composeRule.onNodeWithText(title).assertIsDisplayed()
        assertEquals(0, dismissRequested)
    }

    @Test
    fun 구매_중에는_끌어내려도_닫히지_않는다() {
        showSheet(isPurchasing = true)

        composeRule.onNodeWithText(title).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(title).assertIsDisplayed()
        assertEquals(0, dismissRequested)
    }

    @Test
    fun 구매_중에는_취소_버튼을_누를_수_없다() {
        showSheet(isPurchasing = true)

        composeRule.onNodeWithText(cancel).assertIsNotEnabled()
    }

    @Test
    fun 구매_전에는_취소_버튼으로_닫기를_요청한다() {
        showSheet(isPurchasing = false)

        composeRule.onNodeWithText(cancel).assertIsEnabled()
        composeRule.onNodeWithText(cancel).performTouchInput { click() }
        composeRule.waitForIdle()

        assertEquals(1, dismissRequested)
    }

    @Test
    fun 구매_전에는_뒤로_가기로_닫기를_요청한다() {
        showSheet(isPurchasing = false)

        pressBack()
        assertEquals(1, dismissRequested)
    }

    @Test
    fun 구매_전에는_바깥을_누르면_닫기를_요청한다() {
        showSheet(isPurchasing = false)

        clickOutside()

        assertEquals(1, dismissRequested)
    }
}
