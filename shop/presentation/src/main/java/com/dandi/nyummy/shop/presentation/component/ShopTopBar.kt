package com.dandi.nyummy.shop.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.shop.presentation.R
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 상점 하위 화면 상단 바(피그마 `v2 / Shop / Top Bar`). 뒤로, 왼쪽 정렬 제목, 장바구니 버튼(배지).
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TopBarHeight)
            .background(NyummyTheme.colors.bg.surface)
            .padding(start = NyummyTheme.spacing.s4, end = NyummyTheme.spacing.s12),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(BackSize)
                .nyummyClickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_ic_chevron_left),
                contentDescription = stringResource(R.string.shop_back),
                tint = NyummyTheme.colors.content.primary,
                modifier = Modifier.size(NyummyTheme.size.iconL),
            )
        }
        NyummyText(
            text = title,
            style = NyummyTheme.typography.titleM,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (onCartClick != null) {
            ShopCartButton(count = cartItemCount, onClick = onCartClick)
        }
    }
}

private val TopBarHeight = 56.dp
private val BackSize = 44.dp

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ShopTopBarPreview() {
    NyummyTheme {
        ShopTopBar(title = "신입 집사 환영", onBack = {}, cartItemCount = 2, onCartClick = {})
    }
}
