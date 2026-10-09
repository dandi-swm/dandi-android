package com.dandi.nyummy.common.domain.base

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.SESSION_UNAUTHORIZED_CODE
import com.dandi.nyummy.common.domain.error.TestErrorType
import com.dandi.nyummy.common.domain.error.httpException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.tti.TTIHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class HandleHttpErrorTest {

    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()
    private val useCase = TestUseCase(messageHelper, navigationHelper)

    @Test
    fun `등록된 code 는 공통 안내보다 먼저 domain 처리로 간다`() {
        val result = useCase.handle(httpException(404, TestErrorType.DOMAIN.type))

        assertEquals("domain:DOMAIN", result)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `presentation 이 처리할 code 는 아무 안내도 하지 않는다`() {
        val result = useCase.handle(httpException(401, TestErrorType.PRESENTATION_ONLY.type))

        assertEquals("none", result)
        assertTrue(messageHelper.dialogs.isEmpty())
        assertEquals(0, navigationHelper.initialCount)
    }

    @Test
    fun `등록하지 않은 code 의 401 은 로그인 만료가 아니라 화면 기본 안내로 간다`() {
        val result = useCase.handle(httpException(401, "api.auth.invalidVerifiedToken"))

        assertEquals("unknown", result)
        assertTrue(messageHelper.dialogs.isEmpty())
        assertEquals(0, navigationHelper.initialCount)
    }

    @Test
    fun `세션 401 은 로그인 만료를 안내하고 초기 화면으로 보낸다`() {
        listOf(httpException(401), httpException(401, SESSION_UNAUTHORIZED_CODE)).forEach { e ->
            messageHelper.dialogs.clear()
            useCase.handle(e)

            val dialog = messageHelper.dialogs.single()
            assertEquals("로그인이 만료됐어요. 다시 로그인해 주세요.", dialog.descText)
            assertTrue(dialog.cantIgnore)
            dialog.onClickButton?.invoke()
        }
        assertEquals(2, navigationHelper.initialCount)
    }

    @Test
    fun `등록하지 않은 code 의 404 는 준비 중인 기능으로 안내한다`() {
        useCase.handle(httpException(404, "api.unknown.notFound"))

        assertEquals("준비 중인 기능이에요.", messageHelper.dialogs.single().descText)
    }

    @Test
    fun `공통 처리를 직접 불러도 다른 code 의 401 은 로그인 만료로 안내하지 않는다`() {
        useCase.executeCommonErrorHanding(httpException(401, "api.auth.invalidEmailChallengeToken"))

        assertEquals("잠시 후 다시 시도해 주세요.", messageHelper.dialogs.single().descText)
        assertEquals(0, navigationHelper.initialCount)
    }
}

private class TestUseCase(
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
) : BaseUseCase(noOp(), messageHelper, navigationHelper, noOp()) {

    /** 어느 갈래로 처리됐는지 돌려준다. */
    fun handle(e: HttpResponseException): String {
        var handled = "none"
        handleHttpError<TestErrorType>(
            e,
            onDomainError = { handled = "domain:${it.name}" },
            onUnknownError = { handled = "unknown" },
        )
        return handled
    }
}

/** 테스트에서 쓰지 않는 의존성(ResourceHelper, TTIHelper)의 빈 구현. */
private inline fun <reified T : Any> noOp(): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, _, _ -> null } as T

private class RecordingNavigationHelper : NavigationHelper {
    var initialCount = 0

    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) = Unit
    override fun navigateDeepLink(route: NavRoute) = Unit
    override fun navigateToBack() = Unit
    override fun navigateToAsRoot(page: Page) = Unit
    override fun navigateToInitial() {
        initialCount++
    }

    override fun navigateToExternalLink(url: String) = Unit
}

private class RecordingMessageHelper : MessageHelper {
    data class DialogCall(val descText: String, val cantIgnore: Boolean, val onClickButton: (() -> Unit)?)

    val dialogs = mutableListOf<DialogCall>()

    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) = Unit

    override fun showSnackBar(
        iconType: IconType,
        messageRes: Int,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) = Unit

    override fun showOneButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        buttonText: String,
        onClickButton: (() -> Unit)?,
    ) {
        dialogs += DialogCall(descText, cantIgnore, onClickButton)
    }

    override fun showTwoButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        leftButtonText: String,
        onClickLeftButton: (() -> Unit)?,
        rightButtonText: String,
        onClickRightButton: (() -> Unit)?,
    ) = Unit
}
