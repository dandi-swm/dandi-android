package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.Page

/** 설정. 홈 상단 설정 버튼에서 들어온다. */
object SettingsPage : Page {

    const val PATH = "/settings"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/** 평소 식사 시간(아침, 점심, 저녁)을 드롭다운으로 고친다. */
object MealTimeSettingPage : Page {

    const val PATH = "/settings/meal-time"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/** 내가 보낸 문의 목록. 설정의 문의하기에서 들어온다. */
object InquiryListPage : Page {

    const val PATH = "/settings/inquiry"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/** 문의하기(유형 고르기 + 내용 쓰기). 서버 푸시 링크와 맞춰 둔 경로라 설정으로 옮긴 뒤에도 우편함 아래 경로를 그대로 쓴다. */
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
