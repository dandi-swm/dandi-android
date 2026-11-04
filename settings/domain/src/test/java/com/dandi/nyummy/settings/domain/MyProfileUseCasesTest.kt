package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.settings.entity.MyProfileChange
import com.dandi.nyummy.settings.entity.MyProfileVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class MyProfileUseCasesTest {

    private val repository = FakeSettingsRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()
    private var retryCount = 0
    private var signOutCount = 0

    private val getMyProfile = GetMyProfileUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper,
    )

    private val updateMyProfile = UpdateMyProfileUseCase(
        repository = repository,
        resourceHelper = FakeResourceHelper,
        messageHelper = messageHelper,
        navigationHelper = navigationHelper,
        ttiHelper = FakeTTIHelper,
    )

    private suspend fun update(change: MyProfileChange) = updateMyProfile(change, onRetry = { retryCount++ })

    @Test
    fun `내 프로필을 읽어 그대로 돌려준다`() = runBlocking {
        repository.profile = MyProfileVO(nickname = "진우", gender = Gender.FEMALE, heightCm = 163)

        val result = getMyProfile(onRetry = { retryCount++ }, onSignOut = { signOutCount++ })

        assertEquals(repository.profile, result.getOrNull())
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `내 프로필을 읽지 못하면 다시 시도 스낵바를 띄우고 다시 시도는 화면에 맡긴다`() = runBlocking {
        repository.getMealTimesError = IOException("offline")

        val result = getMyProfile(onRetry = { retryCount++ }, onSignOut = { signOutCount++ })

        assertTrue(result.isFailure)
        val snackBar = messageHelper.snackBars.single()
        assertEquals("내 정보를 불러오지 못했어요", snackBar.messageText)
        snackBar.onClickCTA?.invoke()
        assertEquals(1, retryCount)
    }

    @Test
    fun `회원이 없으면 닫을 수 없는 다시 로그인 창을 띄우고 확인하면 로그아웃을 화면에 맡긴다`() = runBlocking {
        repository.getMealTimesError = httpException(404, "api.user.notFound")

        val result = getMyProfile(onRetry = { retryCount++ }, onSignOut = { signOutCount++ })

        assertTrue(result.isFailure)
        assertTrue(messageHelper.snackBars.isEmpty())
        val dialog = messageHelper.dialogs.single()
        assertEquals("회원 정보를 찾지 못했어요. 다시 로그인해 주세요.", dialog.descText)
        assertTrue(dialog.cantIgnore)
        assertEquals(0, signOutCount)
        dialog.onClickButton?.invoke()
        assertEquals(1, signOutCount)
        assertEquals(0, retryCount)
    }

    @Test
    fun `바꾼 항목만 보내고 집사 이름은 앞뒤 공백을 뺀다`() = runBlocking {
        val result = update(MyProfileChange(nickname = "  진우  "))

        assertEquals(MyProfileChange(nickname = "진우"), result.getOrNull())
        assertEquals(listOf(MyProfileChange(nickname = "진우")), repository.profileChanges)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `바꿀 항목이 없으면 서버에 보내지 않고 성공으로 본다`() = runBlocking {
        val result = update(MyProfileChange())

        assertTrue(result.isSuccess)
        assertTrue(repository.profileChanges.isEmpty())
    }

    @Test
    fun `저장하지 못하면 다시 시도 스낵바를 띄우고 다시 시도는 화면에 맡긴다`() = runBlocking {
        repository.updateMyProfileError = IOException("offline")

        val result = update(MyProfileChange(weightKg = 50))

        assertTrue(result.isFailure)
        val snackBar = messageHelper.snackBars.single()
        assertEquals("내 정보를 저장하지 못했어요", snackBar.messageText)
        assertEquals("다시 시도", snackBar.callToActionText)
        snackBar.onClickCTA?.invoke()
        assertEquals(1, retryCount)
    }

    @Test
    fun `서버가 입력값을 거절하면 식사 시각이 아닌 프로필 안내를 한다`() = runBlocking {
        repository.updateMyProfileError = httpException(400, "api.common.invalidInputValue")

        update(MyProfileChange(heightCm = 163))

        val snackBar = messageHelper.snackBars.single()
        assertEquals("입력한 값을 다시 확인해 주세요.", snackBar.messageText)
        assertNull(snackBar.callToActionText)
    }

    @Test
    fun `로그인이 만료됐으면 세션 만료 다이얼로그만 띄운다`() = runBlocking {
        repository.updateMyProfileError = httpException(401, "api.auth.tokenExpired")

        val result = update(MyProfileChange(heightCm = 163))

        assertTrue(result.isFailure)
        assertTrue(messageHelper.snackBars.isEmpty())
        messageHelper.dialogs.single().onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
    }

    @Test(expected = CancellationException::class)
    fun `취소는 삼키지 않고 그대로 던진다`(): Unit = runBlocking {
        repository.updateMyProfileError = CancellationException("left screen")
        update(MyProfileChange(heightCm = 163))
        Unit
    }
}
