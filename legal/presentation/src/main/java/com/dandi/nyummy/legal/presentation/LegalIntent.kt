package com.dandi.nyummy.legal.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 약관 화면(WebView)에서 생기는 일. */
sealed interface LegalIntent : MviIntent {

    /** 페이지를 읽기 시작했다(처음 열기, 다른 정책 페이지로 이동, 다시 시도). */
    data object PageStarted : LegalIntent

    /** 페이지를 다 읽었다. 본문을 읽지 못한 경우에도 불린다. */
    data object PageFinished : LegalIntent

    /** 본문(메인 프레임)을 읽지 못했다. */
    data object PageFailed : LegalIntent

    /** 보는 페이지가 바뀌었다(링크 이동, 뒤로 가기). 그 주소와 뒤로 갈 페이지가 있는지 넘긴다. */
    data class HistoryChanged(val url: String?, val canGoBack: Boolean) : LegalIntent

    /** 정책 페이지가 아닌 링크(메일, 외부 사이트)를 눌렀다. 앱 밖에서 연다. */
    data class ClickExternalLink(val url: String) : LegalIntent

    /** 돌아갈 페이지가 없을 때 상단 바 뒤로가기를 눌렀다. */
    data object ClickClose : LegalIntent
}
