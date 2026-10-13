package com.dandi.nyummy.legal.presentation

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySpinner
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateAction
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyStateSurface
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.legal.entity.LegalDocument

/**
 * 이용약관, 개인정보처리방침을 앱 안에서 보여 준다. 정책 페이지(GitHub Pages)를 WebView로 연다.
 *
 * - 정책 페이지끼리의 링크(약관 ↔ 방침)는 이 화면 안에서 열고, 메일이나 외부 사이트 링크는 앱 밖에서 연다.
 * - 뒤로가기(상단 바, 시스템)는 다른 정책 페이지로 옮겨 갔으면 이전 페이지로, 아니면 화면을 닫는다.
 * - 읽는 동안 스피너를, 본문을 읽지 못하면 다시 시도 화면을 띄운다.
 */
@Composable
fun LegalPage(
    document: LegalDocument,
    modifier: Modifier = Modifier,
    viewModel: LegalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LegalScreen(document = document, uiState = uiState, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
private fun LegalScreen(
    document: LegalDocument,
    uiState: LegalUIState,
    onIntent: (LegalIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // WebView 가 직접 하는 일(이전 페이지, 다시 읽기)에 쓰려고 들고 있는다. 상태는 ViewModel 이 가진다.
    var webView by remember { mutableStateOf<WebView?>(null) }
    val currentOnIntent by rememberUpdatedState(onIntent)

    BackHandler(enabled = uiState.canGoBack) { webView?.goBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        NyummyTopBar(
            title = stringResource(uiState.shownPage?.titleRes ?: document.titleRes),
            onBackClick = {
                val view = webView
                if (view != null && view.canGoBack()) view.goBack() else onIntent(LegalIntent.ClickClose)
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        configurePolicySettings()
                        webViewClient = PolicyWebViewClient(onIntent = { currentOnIntent(it) })
                        loadUrl(document.url)
                        webView = this
                    }
                },
                onRelease = { it.destroy() },
                modifier = Modifier.fillMaxSize(),
            )
            when (uiState.loadState) {
                LegalLoadState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NyummyTheme.colors.bg.canvas),
                    contentAlignment = Alignment.Center,
                ) {
                    NyummySpinner(contentDescription = stringResource(R.string.legal_loading))
                }
                LegalLoadState.Failed -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NyummyTheme.colors.bg.canvas),
                    contentAlignment = Alignment.Center,
                ) {
                    NyummyStateSurface.Error(
                        voice = stringResource(R.string.legal_error_voice),
                        message = stringResource(R.string.legal_error_message),
                        retry = NyummyStateAction(
                            text = stringResource(R.string.legal_retry),
                            onClick = { webView?.let { reload(it, document) } },
                        ),
                    )
                }
                LegalLoadState.Loaded -> Unit
            }
        }
    }
}

private val LegalDocument.titleRes: Int
    get() = when (this) {
        LegalDocument.TERMS -> R.string.legal_terms_title
        LegalDocument.PRIVACY -> R.string.legal_privacy_title
    }

private val PolicyPage.titleRes: Int
    get() = when (this) {
        PolicyPage.TERMS -> R.string.legal_terms_title
        PolicyPage.PRIVACY -> R.string.legal_privacy_title
        PolicyPage.ACCOUNT_DELETION -> R.string.legal_account_deletion_title
    }

/**
 * 정책 페이지에 필요한 만큼만 연다. 상단 메뉴를 숨기는 `inapp=1` 처리가 JS라 JS만 켜고,
 * 저장소와 파일 접근은 끈다. 정책 페이지는 밝은 색으로 고정이라 강제 다크도 켜지 않는다(기본값 그대로).
 */
@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configurePolicySettings() {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = false
    settings.allowFileAccess = false
    settings.allowContentAccess = false
}

/** 보던 정책 페이지를 다시 읽는다. 처음부터 못 읽었으면 문서 주소를 연다. */
private fun reload(view: WebView, document: LegalDocument) {
    view.loadUrl(view.url?.takeIf(::isPolicyUrl) ?: document.url)
}

/**
 * WebView 에서 생기는 일을 화면 인텐트로 넘긴다.
 * 본문(메인 프레임)을 읽지 못했을 때만 실패로 본다. 이미지 같은 하위 리소스 실패로 화면 전체를 막지 않기 위해서다.
 */
private class PolicyWebViewClient(
    private val onIntent: (LegalIntent) -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        if (isPolicyUrl(url)) return false
        onIntent(LegalIntent.ClickExternalLink(url))
        return true
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        onIntent(LegalIntent.PageStarted)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        onIntent(LegalIntent.PageFinished)
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) onIntent(LegalIntent.PageFailed)
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        if (request.isForMainFrame) onIntent(LegalIntent.PageFailed)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        onIntent(LegalIntent.HistoryChanged(url = url, canGoBack = view.canGoBack()))
    }
}
