package com.dandi.nyummy.main.presentation.deeplink

import com.dandi.nyummy.common.domain.navigation.LinkArgs
import com.dandi.nyummy.common.domain.navigation.NavRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** 알림에서 열린 링크의 라우트에 알림 식별자가 실리는지 확인합니다. */
class PushAppLinkTest {

    private val mealLink = NavRoute("/history", mapOf("mealId" to "7"))

    @Test
    fun `알림 식별자를 링크 인자와 함께 싣는다`() {
        val route = mealLink.withNotificationId("n-1")

        assertEquals(mapOf("mealId" to "7", LinkArgs.NOTIFICATION_ID to "n-1"), route.args)
    }

    @Test
    fun `같은 링크라도 알림이 다르면 다른 라우트가 된다`() {
        assertNotEquals(mealLink.withNotificationId("n-1"), mealLink.withNotificationId("n-2"))
    }

    @Test
    fun `알림에서 열린 게 아니면 라우트를 그대로 둔다`() {
        assertEquals(mealLink, mealLink.withNotificationId(null))
    }
}
