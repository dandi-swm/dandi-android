package com.dandi.nyummy.common.domain.base

import com.dandi.nyummy.common.domain.error.CommonErrorPolicy
import com.dandi.nyummy.common.domain.error.TestErrorType
import com.dandi.nyummy.common.domain.error.httpException
import com.dandi.nyummy.common.domain.fake.CallLog
import com.dandi.nyummy.common.domain.fake.FakeMessageHelper
import com.dandi.nyummy.common.domain.fake.FakeNavigationHelper
import com.dandi.nyummy.common.domain.fake.StringNameResourceHelper
import com.dandi.nyummy.common.domain.fake.noOpFake
import com.dandi.nyummy.common.domain.fake.recordingFake
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/** [BaseUseCase.request]가 처리 순서(서버 code → 공통 → 네트워크 → 기본 안내)와 취소 규칙을 지키는지. */
class RequestTest {

    private val messageHelper = FakeMessageHelper()
    private val navigationHelper = FakeNavigationHelper()
    private val useCase = RequestUseCase(messageHelper, navigationHelper)

    @Test
    fun `성공하면 블록의 값을 돌려주고 아무것도 안내하지 않는다`() = runBlocking {
        val result = useCase.run { "ok" }

        assertEquals("ok", result.getOrNull())
        assertTrue(useCase.handled.isEmpty())
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `비즈니스 에러는 공통 404 안내보다 먼저 onBusinessError 가 결과를 고른다`() = runBlocking {
        val result = useCase.run { throw httpException(404, TestErrorType.DOMAIN.type) }

        assertEquals("recovered", result.getOrNull())
        assertEquals(listOf("business:DOMAIN"), useCase.handled)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `화면이 처리하는 code 는 안내 없이 원래 예외로 실패한다`() = runBlocking {
        val e = httpException(409, TestErrorType.PRESENTATION_ONLY.type)

        val result = useCase.run { throw e }

        assertSame(e, result.exceptionOrNull())
        assertTrue(useCase.handled.isEmpty())
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `등록하지 않은 서버 오류는 공통 안내를 하고 화면 기본 안내는 하지 않는다`() = runBlocking {
        val e = httpException(500)

        val result = useCase.run { throw e }

        assertSame(e, result.exceptionOrNull())
        assertEquals(StringResource.TRY_AGAIN_LATER.name, messageHelper.dialogs.single().descText)
        assertTrue(useCase.handled.isEmpty())
    }

    @Test
    fun `네트워크 오류는 onNetworkError 로 안내하고 원래 예외로 실패한다`() = runBlocking {
        val e = IOException("offline")

        val result = useCase.run { throw e }

        assertSame(e, result.exceptionOrNull())
        assertEquals(listOf("network"), useCase.handled)
    }

    @Test
    fun `onNetworkError 가 없으면 네트워크 오류도 onUnknownError 로 안내한다`() = runBlocking {
        val result = useCase.run(withNetworkHandler = false) { throw IOException("offline") }

        assertTrue(result.isFailure)
        assertEquals(listOf("unknown:IOException"), useCase.handled)
    }

    @Test
    fun `예상 못 한 일반 예외도 앱을 죽이지 않고 onUnknownError 로 안내한 뒤 실패한다`() = runBlocking {
        val e = IllegalStateException("Successful response with null body")

        val result = useCase.run { throw e }

        assertSame(e, result.exceptionOrNull())
        assertEquals(listOf("unknown:IllegalStateException"), useCase.handled)
    }

    @Test
    fun `등록하지 않은 code 의 4xx 는 onUnknownError 로 안내한다`() = runBlocking {
        useCase.run { throw httpException(400, "api.unknown") }

        assertEquals(listOf("unknown:HttpResponseException"), useCase.handled)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `코루틴 취소는 실패로 바꾸지 않고 그대로 다시 던진다`() = runBlocking {
        val cancel = CancellationException("screen closed")
        try {
            useCase.run { throw cancel }
            fail("CancellationException 을 삼켰다")
        } catch (e: CancellationException) {
            assertSame(cancel, e)
        }
        assertTrue(useCase.handled.isEmpty())
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `로그인 만료 401 은 SessionExpiryHandler 가 세션을 정리하고 확인하면 로그인 화면으로 보낸다`() = runBlocking {
        val log = CallLog()
        val handler = sessionExpiryHandler(log)
        val useCase = RequestUseCase(messageHelper, navigationHelper, handler)

        useCase.run { throw httpException(401) }
        messageHelper.dialogs.single().onClickButton?.invoke()

        assertEquals(StringResource.SESSION_EXPIRED_MESSAGE.name, messageHelper.dialogs.single().descText)
        assertEquals(1, log.count("SessionTokenHelper.clearTokens"))
        assertEquals(1, navigationHelper.initialCount)
        assertTrue(useCase.handled.isEmpty())
    }

    @Test
    fun `SIGNED_OUT 정책의 401 은 로그인 만료로 처리하지 않는다`() = runBlocking {
        val log = CallLog()
        val useCase = RequestUseCase(messageHelper, navigationHelper, sessionExpiryHandler(log))

        useCase.run(policy = CommonErrorPolicy.SIGNED_OUT) { throw httpException(401) }

        assertEquals(listOf("unknown:HttpResponseException"), useCase.handled)
        assertTrue(messageHelper.dialogs.isEmpty())
        assertEquals(0, log.count("SessionTokenHelper.clearTokens"))
        assertEquals(0, navigationHelper.initialCount)
    }

    @Test
    fun `로그인 만료를 안내한 뒤 다시 로그인하기 전에는 다른 공통 다이얼로그로 덮지 않는다`() = runBlocking {
        val useCase = RequestUseCase(messageHelper, navigationHelper, sessionExpiryHandler(CallLog()))

        useCase.run { throw httpException(401) }
        useCase.run { throw httpException(500) }
        useCase.run { throw httpException(404) }

        assertEquals(StringResource.SESSION_EXPIRED_MESSAGE.name, messageHelper.dialogs.single().descText)
    }

    @Test
    fun `SessionExpiryHandler 없이 만든 UseCase 는 다이얼로그와 이동만 한다`() = runBlocking {
        useCase.run { throw httpException(401) }
        messageHelper.dialogs.single().onClickButton?.invoke()

        assertEquals(StringResource.SESSION_EXPIRED_MESSAGE.name, messageHelper.dialogs.single().descText)
        assertEquals(1, navigationHelper.initialCount)
    }

    private fun sessionExpiryHandler(log: CallLog) = SessionExpiryHandler(
        resourceHelper = StringNameResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        sessionTokenHelper = recordingFake(log),
        pushTokenHelper = recordingFake(log),
        companionMemory = recordingFake(log),
        tutorialHelper = recordingFake(log),
        widgetHelper = recordingFake(log),
        scope = CoroutineScope(Dispatchers.Unconfined),
    )
}

/** 각 갈래에서 어떤 처리가 불렸는지 [handled]에 남기는 UseCase. */
private class RequestUseCase(
    messageHelper: FakeMessageHelper,
    navigationHelper: FakeNavigationHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(StringNameResourceHelper, messageHelper, navigationHelper, noOpFake(), sessionExpiryHandler) {

    val handled = mutableListOf<String>()

    suspend fun run(
        policy: CommonErrorPolicy = CommonErrorPolicy.ALL,
        withNetworkHandler: Boolean = true,
        block: suspend () -> String,
    ): Result<String> = request(
        TestErrorType.entries,
        commonErrors = policy,
        onBusinessError = { type, _ ->
            handled += "business:${type.name}"
            Result.success("recovered")
        },
        onNetworkError = if (withNetworkHandler) ({ handled += "network" }) else null,
        onUnknownError = { cause -> handled += "unknown:${cause::class.simpleName}" },
        block = block,
    )
}
