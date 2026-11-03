package com.dandi.nyummy.shop.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBarTitleAlignment
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 상점 하위 화면 상단 바(피그마 `v2 / Shop / Top Bar`). 공용 상단 바의 왼쪽 제목 모양에 장바구니 버튼(배지)을 단다.
 *
 * @param onCartClick null이면 장바구니 버튼을 그리지 않는다(장바구니 화면 자신)
 */
@Composable
fun ShopTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    cartItemCount: Int = 0,
    onCartClick: (() -> Unit)? = null,
) {
    NyummyTopBar(
        title = title,
        modifier = modifier,
        onBackClick = onBack,
        trailing = onCartClick?.let { onClick -> { ShopCartButton(count = cartItemCount, onClick = onClick) } },
        containerColor = NyummyTheme.colors.bg.surface,
        titleAlignment = NyummyTopBarTitleAlignment.Start,
    )
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ShopTopBarPreview() {
    NyummyTheme {
        ShopTopBar(title = "신입 집사 환영", onBack = {}, cartItemCount = 2, onCartClick = {})
    }
}
