package com.dandi.nyummy.collection.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyComingSoonPage
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** 컬렉션 화면. 기능 공개 전까지 준비 중 안내를 보여준다. */
@Composable
fun CollectionPage(
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    CollectionScreen()
}

@Composable
private fun CollectionScreen() {
    NyummyComingSoonPage(
        title = stringResource(R.string.collection_title),
        message = stringResource(R.string.collection_coming_title),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 767)
@Composable
private fun CollectionScreenPreview() {
    NyummyTheme {
        CollectionScreen()
    }
}
