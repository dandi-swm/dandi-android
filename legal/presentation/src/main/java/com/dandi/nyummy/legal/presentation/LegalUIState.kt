package com.dandi.nyummy.legal.presentation

import com.dandi.nyummy.common.presentation.mvi.UiState

/** 정책 페이지 본문을 읽는 상태. */
enum class LegalLoadState { Loading, Loaded, Failed }

/**
 * 약관 화면 상태.
 *
 * @property loadState 본문(메인 프레임)을 읽는 상태. 읽는 동안 스피너를, 읽지 못하면 다시 시도 화면을 띄운다.
 * @property canGoBack 페이지 안에서 다른 정책 페이지로 옮겨 갔다. 뒤로가기가 화면을 닫지 않고 이전 페이지로 간다.
 * @property shownPage 지금 보고 있는 정책 페이지. 상단 제목을 이 페이지에 맞춘다. 모르면 null이고 처음 연 문서 제목을 둔다.
 */
data class LegalUIState(
    val loadState: LegalLoadState = LegalLoadState.Loading,
    val canGoBack: Boolean = false,
    val shownPage: PolicyPage? = null,
) : UiState {

    companion object {
        val empty = LegalUIState()
    }
}
