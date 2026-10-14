package com.dandi.nyummy.intro.presentation

import com.dandi.nyummy.common.presentation.mvi.MviIntent

sealed interface IntroIntent : MviIntent {
    /**
     * 권한 안내 바텀시트에서 "확인"을 눌렀다. 여기서는 시스템 권한 팝업을 띄우지 않는다.
     * 권한은 카메라(식사 기록), 알림(온보딩, 설정)처럼 기능을 쓰는 순간에 그 화면이 묻는다.
     */
    data object ConfirmPermissionNotice : IntroIntent
}
