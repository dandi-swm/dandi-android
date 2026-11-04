package com.dandi.nyummy.intro.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

sealed interface IntroIntent : MviIntent {
    /** 권한 안내 바텀시트에서 "확인"을 눌렀다. 물을 권한이 있으면 이어서 시스템 권한 팝업을 띄운다. */
    data object ConfirmPermissionNotice : IntroIntent

    /** 권한 요청이 끝났다. 허용 여부와 상관없이 시작 흐름을 잇는다. */
    data object PermissionRequestFinished : IntroIntent
}
