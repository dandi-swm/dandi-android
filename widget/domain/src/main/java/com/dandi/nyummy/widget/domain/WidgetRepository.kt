package com.dandi.nyummy.widget.domain

import com.dandi.nyummy.widget.entity.WidgetSnapshotVO

/** 위젯이 기기에 남긴 값과 로그인 여부. 서버를 부르지 않는다. */
interface WidgetRepository {
    /** 남겨 둔 값. 없거나 읽지 못하면 빈 값이다. */
    suspend fun snapshot(): WidgetSnapshotVO

    /** 남겨 둔 값을 [transform] 결과로 바꾼다. 읽기와 쓰기는 한 번에 일어난다. */
    suspend fun update(transform: (WidgetSnapshotVO) -> WidgetSnapshotVO)

    /** 남겨 둔 값을 모두 지운다. */
    suspend fun clear()

    /** 로그인 토큰이 남아 있는지. 디스크를 읽을 수 있어 메인 스레드에서 부르지 않는다. */
    suspend fun isLoggedIn(): Boolean
}

/** 위젯을 지금 다시 그려 달라고 요청한다. 구현은 위젯 receiver를 아는 presentation에 있다. */
interface WidgetRefresher {
    fun requestRefresh()
}
