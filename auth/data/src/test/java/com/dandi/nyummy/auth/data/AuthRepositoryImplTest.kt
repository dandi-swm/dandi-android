package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.data.dto.AuthTokenDTO
import com.dandi.nyummy.auth.data.dto.EmailVerificationConfirmRequestDTO
import com.dandi.nyummy.auth.data.dto.LoginRequestDTO
import com.dandi.nyummy.auth.data.dto.PasswordChangeRequestDTO
import com.dandi.nyummy.auth.data.dto.SignupRequestDTO
import com.dandi.nyummy.auth.entity.SignupFormVO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private lateinit var dataSource: AuthDataSource
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        dataSource = mockk()
        repository = AuthRepositoryImpl(dataSource)
    }

    @Test
    fun `로그인은 입력값을 요청 DTO로 전달하고 응답을 VO로 변환한다`() = runTest {
        val requestSlot = slot<LoginRequestDTO>()
        coEvery { dataSource.login(capture(requestSlot)) } returns AuthTokenDTO(
            accessToken = "access",
            refreshToken = "refresh",
            redirectUrl = "dandi://",
        )

        val vo = repository.login(email = "cat@dandi.com", password = "pw1234")

        assertEquals("cat@dandi.com", requestSlot.captured.email)
        assertEquals("pw1234", requestSlot.captured.password)
        assertEquals("access", vo.accessToken)
        assertEquals("refresh", vo.refreshToken)
        assertEquals("dandi://", vo.redirectUrl)
    }

    @Test
    fun `회원가입은 폼 전체 필드를 요청 DTO에 매핑한다`() = runTest {
        val requestSlot = slot<SignupRequestDTO>()
        coEvery { dataSource.signup(capture(requestSlot)) } returns AuthTokenDTO(
            accessToken = "access",
            refreshToken = "refresh",
        )

        val form = SignupFormVO(
            email = "cat@dandi.com",
            password = "pw1234",
            nickname = "진우 집사",
            gender = "남성",
            birth = "2000-07-12",
            height = 175,
            weight = 80,
        )
        val vo = repository.signup(form)

        with(requestSlot.captured) {
            assertEquals("cat@dandi.com", email)
            assertEquals("pw1234", password)
            assertEquals("진우 집사", nickname)
            assertEquals("남성", gender)
            assertEquals("2000-07-12", birth)
            assertEquals(175, height)
            assertEquals(80, weight)
        }
        assertEquals("access", vo.accessToken)
    }

    @Test
    fun `이메일 인증 확인은 이메일과 코드를 그대로 전달한다`() = runTest {
        val requestSlot = slot<EmailVerificationConfirmRequestDTO>()
        coEvery { dataSource.confirmEmailVerification(capture(requestSlot)) } returns Unit

        repository.confirmEmailVerification(email = "cat@dandi.com", verificationCode = 123456)

        assertEquals("cat@dandi.com", requestSlot.captured.email)
        assertEquals(123456, requestSlot.captured.verificationCode)
    }

    @Test
    fun `비밀번호 변경은 userId 헤더 값과 요청 DTO를 전달한다`() = runTest {
        val requestSlot = slot<PasswordChangeRequestDTO>()
        coEvery { dataSource.changePassword(any(), capture(requestSlot)) } returns Unit

        repository.changePassword(userId = 1L, password = "old", newPassword = "new")

        coVerify(exactly = 1) { dataSource.changePassword(1L, any()) }
        assertEquals("old", requestSlot.captured.password)
        assertEquals("new", requestSlot.captured.newPassword)
    }
}
