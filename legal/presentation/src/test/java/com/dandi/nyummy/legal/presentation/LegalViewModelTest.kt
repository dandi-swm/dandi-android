package com.dandi.nyummy.legal.presentation

import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalViewModelTest {

    private val navigationHelper = RecordingNavigationHelper()
    private val viewModel = LegalViewModel(navigationHelper)
    private val state get() = viewModel.uiState.value

    @Test
    fun `읽는 동안은 로딩이고 다 읽으면 본문을 보여 준다`() {
        assertEquals(LegalLoadState.Loading, state.loadState)

        viewModel.onIntent(LegalIntent.PageStarted)
        viewModel.onIntent(LegalIntent.PageFinished)

        assertEquals(LegalLoadState.Loaded, state.loadState)
    }

    @Test
    fun `본문을 못 읽으면 다 읽었다는 알림이 와도 실패 화면을 둔다`() {
        viewModel.onIntent(LegalIntent.PageStarted)
        viewModel.onIntent(LegalIntent.PageFailed)
        viewModel.onIntent(LegalIntent.PageFinished)

        assertEquals(LegalLoadState.Failed, state.loadState)
    }

    @Test
    fun `다시 시도하면 로딩부터 다시 시작한다`() {
        viewModel.onIntent(LegalIntent.PageFailed)

        viewModel.onIntent(LegalIntent.PageStarted)
        assertEquals(LegalLoadState.Loading, state.loadState)
        viewModel.onIntent(LegalIntent.PageFinished)
        assertEquals(LegalLoadState.Loaded, state.loadState)
    }

    @Test
    fun `다른 정책 페이지로 옮기면 제목을 그 페이지로 바꾸고 뒤로 갈 수 있다`() {
        viewModel.onIntent(
            LegalIntent.HistoryChanged(url = "https://dandi-swm.github.io/nyummy-policy/privacy.html?inapp=1", canGoBack = true),
        )

        assertEquals(PolicyPage.PRIVACY, state.shownPage)
        assertTrue(state.canGoBack)

        viewModel.onIntent(
            LegalIntent.HistoryChanged(url = "https://dandi-swm.github.io/nyummy-policy/terms.html?inapp=1", canGoBack = false),
        )
        assertEquals(PolicyPage.TERMS, state.shownPage)
        assertFalse(state.canGoBack)
    }

    @Test
    fun `모르는 페이지면 처음 연 문서 제목을 둔다`() {
        viewModel.onIntent(LegalIntent.HistoryChanged(url = "https://dandi-swm.github.io/nyummy-policy/", canGoBack = true))

        assertNull(state.shownPage)
    }

    @Test
    fun `정책 페이지가 아닌 링크는 앱 밖에서 연다`() {
        viewModel.onIntent(LegalIntent.ClickExternalLink("mailto:help@nyummy.co.kr"))

        assertEquals(listOf("mailto:help@nyummy.co.kr"), navigationHelper.externalLinks)
    }

    @Test
    fun `돌아갈 페이지가 없을 때 뒤로가기는 화면을 닫는다`() {
        viewModel.onIntent(LegalIntent.ClickClose)

        assertEquals(1, navigationHelper.backCount)
    }

    private class RecordingNavigationHelper : NavigationHelper {
        val externalLinks = mutableListOf<String>()
        var backCount = 0

        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) = Unit
        override fun navigateDeepLink(route: NavRoute) = Unit
        override fun navigateToBack() {
            backCount++
        }

        override fun navigateToAsRoot(page: Page) = Unit
        override fun navigateToInitial() = Unit
        override fun navigateToExternalLink(url: String) {
            externalLinks += url
        }
    }
}
