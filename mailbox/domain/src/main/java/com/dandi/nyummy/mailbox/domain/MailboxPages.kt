package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

/** 우편함(1차는 내가 보낸 문의 목록만). 홈 우편함 버튼에서 들어온다. */
object MailboxPage : Page {

    const val PATH = "/mailbox"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/** 문의하기(유형 고르기 + 내용 쓰기). */
object InquiryWritePage : Page {

    const val PATH = "/mailbox/inquiry/write"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/** 문의 상세. 답장이 왔으면 답장과 내 문의를, 아직이면 내 문의만 보여 준다. */
data class InquiryDetailPage(val inquiryId: Long) : Page {

    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_INQUIRY_ID to inquiryId.toString()))

    companion object {
        const val ARG_INQUIRY_ID = "inquiryId"
        const val PATH = "/mailbox/inquiry/{$ARG_INQUIRY_ID}"
    }
}
