package com.dandi.nyummy.shop.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyComingSoonPage
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** 상점 화면. 기능 공개 전까지 준비 중 안내를 보여준다. */
@Composable
fun ShopPage(
    viewModel: ShopViewModel = hiltViewModel(),
) {
    ShopScreen()
}

@Composable
private fun ShopScreen() {
    NyummyComingSoonPage(
        title = stringResource(R.string.shop_title),
        message = stringResource(R.string.shop_coming_title),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 767)
@Composable
private fun ShopScreenPreview() {
    NyummyTheme {
        ShopScreen()
    }
}
