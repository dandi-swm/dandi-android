package com.dandi.nyummy.common.data.push

import com.dandi.nyummy.common.data.push.dto.DeviceTokenRequestDTO
import com.dandi.nyummy.common.data.token.TokenProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** 로그인한 상태에서만 보내고, 실패를 밖으로 내지 않고, 등록과 삭제가 겹치지 않는지 확인합니다. */
class PushTokenHelperImplTest {

    private val uncaught = mutableListOf<Throwable>()
    private val ioJob = SupervisorJob()
    private val ioScope = CoroutineScope(Dispatchers.IO + ioJob + CoroutineExceptionHandler { _, e -> uncaught += e })

    private val api = FakeDeviceTokenApiService()
    private val tokenProvider = FakeTokenProvider(refresh = "refresh")
    private var fetchToken: suspend () -> String = { FCM_TOKEN }

    private val helper = PushTokenHelperImpl(
        dataSource = DeviceTokenDataSource(api),
        tokenProvider = tokenProvider,
        ioScope = ioScope,
        fetchToken = { fetchToken() },
        deleteFcmToken = {},
    )

    @After
    fun tearDown() {
        ioScope.cancel()
    }

    @Test
    fun `로그인한 상태면 지금 기기의 토큰을 보낸다`() = runBlocking {
        helper.register()
        awaitLaunched()

        assertEquals(listOf(FCM_TOKEN), api.sentTokens)
    }

    @Test
    fun `로그아웃 상태면 보내지 않는다`() = runBlocking {
        tokenProvider.refreshToken = null
        helper.register()
        helper.register("given-token")
        awaitLaunched()

        assertEquals(emptyList<String>(), api.sentTokens)
    }

    @Test
    fun `받은 토큰이 있으면 FCM에 묻지 않고 그 값을 보낸다`() = runBlocking {
        var fetchCount = 0
        fetchToken = { fetchCount++; FCM_TOKEN }

        helper.register("given-token")
        awaitLaunched()

        assertEquals(listOf("given-token"), api.sentTokens)
        assertEquals(0, fetchCount)
    }

    @Test
    fun `전송이 실패해도 예외가 밖으로 나오지 않는다`() = runBlocking {
        api.fail = true
        helper.register()
        awaitLaunched()

        assertEquals(listOf(FCM_TOKEN), api.sentTokens)
        assertTrue(uncaught.isEmpty())
    }

    @Test
    fun `토큰 조회가 실패해도 예외가 밖으로 나오지 않는다`() = runBlocking {
        fetchToken = { error("FCM 실패") }
        helper.register()
        awaitLaunched()

        assertEquals(emptyList<String>(), api.sentTokens)
        assertTrue(uncaught.isEmpty())
    }

    @Test
    fun `진행 중인 등록이 끝난 뒤에 삭제한다`() = runBlocking {
        // 등록과 삭제를 모두 runBlocking 의 한 스레드에서 돌려 순서를 정한다. 다른 디스패처나 시간 제한에 기대지 않는다.
        val order = mutableListOf<String>()
        val fetchStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        api.onRegister = { order += "register" }
        val sameThreadHelper = PushTokenHelperImpl(
            dataSource = DeviceTokenDataSource(api),
            tokenProvider = tokenProvider,
            ioScope = this,
            fetchToken = {
                fetchStarted.complete(Unit)
                release.await()
                FCM_TOKEN
            },
            deleteFcmToken = { order += "delete" },
        )

        sameThreadHelper.register()
        fetchStarted.await() // 등록이 잠금을 쥔 채 토큰을 기다리는 중이다.
        // 바로 실행을 시작해, 잠금이 없다면 이 줄에서 곧장 삭제까지 끝난다.
        val deleting = async(start = CoroutineStart.UNDISPATCHED) { sameThreadHelper.deleteToken() }

        assertEquals(emptyList<String>(), order)
        assertFalse(deleting.isCompleted)

        release.complete(Unit)
        deleting.await()

        assertEquals(listOf("register", "delete"), order)
    }

    @Test
    fun `FCM 토큰 삭제가 실패해도 예외가 밖으로 나오지 않는다`() = runBlocking {
        val failingHelper = PushTokenHelperImpl(
            dataSource = DeviceTokenDataSource(api),
            tokenProvider = tokenProvider,
            ioScope = ioScope,
            fetchToken = { FCM_TOKEN },
            deleteFcmToken = { error("FCM 삭제 실패") },
        )

        failingHelper.deleteToken()
    }

    /** 등록은 [ioScope]에서 따로 돌므로 끝날 때까지 기다린다. */
    private suspend fun awaitLaunched() {
        ioJob.children.forEach { it.join() }
    }

    private class FakeDeviceTokenApiService : DeviceTokenApiService {
        val sentTokens = mutableListOf<String>()
        var fail = false
        var onRegister: () -> Unit = {}

        override suspend fun register(request: DeviceTokenRequestDTO): Response<Unit> {
            sentTokens += request.token
            onRegister()
            return if (fail) Response.error(500, "".toResponseBody()) else Response.success(Unit)
        }
    }

    private class FakeTokenProvider(refresh: String?) : TokenProvider {
        override val accessToken: String? = "access"
        override var refreshToken: String? = refresh

        override suspend fun update(access: String, refresh: String) {
            refreshToken = refresh
        }

        override suspend fun clear() {
            refreshToken = null
        }
    }

    private companion object {
        const val FCM_TOKEN = "fcm-token"
    }
}
