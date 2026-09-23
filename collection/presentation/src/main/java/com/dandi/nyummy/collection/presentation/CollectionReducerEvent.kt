package com.dandi.nyummy.collection.presentation

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

/** 컬렉션 화면 상태를 변이시키는 내부 이벤트. 준비 중 화면이라 아직 항목이 없다. */
// TODO: 컬렉션 기능 구현 시 실제 이벤트를 추가한다.
sealed interface CollectionReducerEvent : ReducerEvent
