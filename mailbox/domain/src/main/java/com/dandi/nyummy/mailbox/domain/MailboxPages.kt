package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

/** 우편함(받은 보상, 코인, 아이템). 홈 우편함 버튼에서 들어온다. */
object MailboxPage : Page {

    const val PATH = "/mailbox"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
