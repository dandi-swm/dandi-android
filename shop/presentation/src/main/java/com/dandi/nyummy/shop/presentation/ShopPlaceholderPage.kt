package com.dandi.nyummy.shop.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyComingSoonPage

/** 상점 하위 화면(프로모션·장바구니·코인 내역)이 만들어지기 전까지 라우트에 그리는 준비 중 화면. */
@Composable
fun ShopPlaceholderPage() {
    NyummyComingSoonPage(
        title = stringResource(R.string.shop_title),
        message = stringResource(R.string.shop_coming_title),
    )
}
