package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import com.dandi.nyummy.common.domain.message.IconType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class RenameCatUseCaseTest {

    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()
    private val memory = FakeCompanionMemory()
    private val renamed = mutableListOf<String>()
    private var renameError: Exception? = null
    private var retryCount = 0

    private val useCase = RenameCatUseCase(
        repository = object : CatRepository {
            override suspend fun getAnimations(): CatAnimationSetVO = CatAnimationSetVO.empty
            override suspend fun refreshWeight(): Boolean = false
            override suspend fun rename(name: String) {
                renameError?.let { throw it }
                renamed += name
            }
        },
        companionMemory = memory,
        resourceHelper = FakeResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper,
    )

    private suspend fun rename(name: String) = useCase(name, onRetry = { retryCount++ })

    @Test
    fun `앞뒤 공백을 뺀 이름을 서버에 저장하고 기기에 기억한 이름도 바꾼다`() = runBlocking {
        val result = rename("  달봉이 ")

        assertEquals("달봉이", result.getOrNull())
        assertEquals(listOf("달봉이"), renamed)
        assertEquals("달봉이", memory.catName)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `이름 규칙에 맞지 않으면 서버에 보내지 않고 안내도 하지 않는다`() = runBlocking {
        val empty = rename("   ")
        val tooLong = rename("아홉글자인고양이름")

        assertTrue(empty.isFailure)
        assertTrue(tooLong.isFailure)
        assertTrue(renamed.isEmpty())
        assertNull(memory.catName)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `저장하지 못하면 기억한 이름은 그대로 두고 다시 시도를 화면에 맡긴다`() = runBlocking {
        renameError = IOException("offline")

        val result = rename("달봉이")

        assertTrue(result.isFailure)
        assertNull(memory.catName)
        assertEquals(IconType.ERROR to "고양이 이름을 바꾸지 못했어요", messageHelper.snackBars.single())
        messageHelper.snackBarActions.single()?.invoke()
        assertEquals(1, retryCount)
    }

    @Test
    fun `서버가 이름이 길다고 거절하면 다시 시도 없이 길이 안내만 한다`() = runBlocking {
        renameError = httpException(400, "api.cat.nameTooLong")

        val result = rename("달봉이")

        assertTrue(result.isFailure)
        assertNull(memory.catName)
        assertEquals(IconType.ERROR to "이름은 8자까지 지을 수 있어요", messageHelper.snackBars.single())
        assertNull(messageHelper.snackBarActions.single())
    }

    @Test
    fun `고양이가 없으면 준비 중 안내 대신 고양이를 찾지 못했다고 안내한다`() = runBlocking {
        renameError = httpException(404, "api.cat.notFound")

        rename("달봉이")

        assertEquals(IconType.ERROR to "고양이 정보를 찾지 못했어요", messageHelper.snackBars.single())
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `로그인이 만료됐으면 세션 만료 다이얼로그만 띄운다`() = runBlocking {
        renameError = httpException(401, "api.auth.tokenExpired")

        rename("달봉이")

        assertTrue(messageHelper.snackBars.isEmpty())
        messageHelper.dialogs.single().onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
    }

    @Test(expected = CancellationException::class)
    fun `취소는 삼키지 않고 그대로 던진다`(): Unit = runBlocking {
        renameError = CancellationException("left screen")
        rename("달봉이")
        Unit
    }

    private fun httpException(status: Int, code: String) = HttpResponseException(
        status = HttpResponseStatus.create(status),
        rawCode = status,
        errorRequestUrl = "https://test/api/v1/cats",
        msg = "Http Request Failed ($status)",
        cause = Throwable(code),
    )

    private class FakeCompanionMemory : CompanionMemoryHelper {
        var catName: String? = null

        override suspend fun lastSeenAtMillis(): Long? = null
        override suspend fun setLastSeenAtMillis(millis: Long) = Unit
        override suspend fun lastCatWeight(): String? = null
        override suspend fun setLastCatWeight(weight: String) = Unit
        override fun catName(): Flow<String?> = flowOf(catName)
        override suspend fun setCatName(name: String) {
            catName = name
        }
        override suspend fun clear() = Unit
    }
}
