package com.dandi.nyummy.room.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

/** 마이룸(방 꾸미기). 홈 고양이방 메뉴의 집 버튼에서 들어온다. */
object MyRoomPage : Page {

    const val PATH = "/room"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
