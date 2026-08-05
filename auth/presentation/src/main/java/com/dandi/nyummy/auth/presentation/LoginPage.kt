package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 로그인 화면 스텁. Figma `73:53`(소셜 로그인 랜딩) 기반 실제 UI 는 후속 PR 에서 구현한다.
 */
@Composable
fun LoginPage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignSystemThemeImpl.designSystemColor.bgSurfaceIvory),
        contentAlignment = Alignment.Center,
    ) {
        DandiText(
            text = "Login",
            style = DesignSystemThemeImpl.typeScale.textStrongM,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
        )
    }
}
