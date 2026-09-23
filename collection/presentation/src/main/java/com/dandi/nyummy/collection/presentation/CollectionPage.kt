package com.dandi.nyummy.collection.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyBadge
import com.dandi.nyummy.common.presentation.component.NyummyMascot
import com.dandi.nyummy.common.presentation.component.NyummyMascotPose
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/** 컬렉션 화면. 기능 공개 전까지 준비 중 안내를 보여준다. */
@Composable
fun CollectionPage(
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    CollectionScreen()
}

@Composable
private fun CollectionScreen(
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgSurfaceIvory)
            .padding(horizontal = DesignSystemThemeImpl.designSystemLayout.mobileGutter),
    ) {
        Spacer(modifier = Modifier.height(spacing.space8))
        DandiText(
            text = stringResource(R.string.collection_title),
            color = colors.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularXL,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NyummyMascot(
                    pose = NyummyMascotPose.Welcome,
                    contentDescription = stringResource(R.string.collection_mascot_content_description),
                    modifier = Modifier.size(ComingSoonMascotSize),
                )
                Spacer(modifier = Modifier.height(spacing.space16))
                DandiText(
                    text = stringResource(R.string.collection_coming_title),
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.textStrongL,
                )
                Spacer(modifier = Modifier.height(spacing.space8))
                DandiText(
                    text = stringResource(R.string.collection_coming_body),
                    color = colors.contentDefaultLevel2,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    style = DesignSystemThemeImpl.typeScale.textRegularM,
                )
                Spacer(modifier = Modifier.height(spacing.space16))
                NyummyBadge(label = stringResource(R.string.collection_badge_coming_soon))
            }
        }
        Spacer(modifier = Modifier.height(spacing.space24))
    }
}

private val ComingSoonMascotSize = 160.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 700)
@Composable
private fun CollectionScreenPreview() {
    DesignSystemTheme {
        CollectionScreen()
    }
}
