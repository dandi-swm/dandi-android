package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.mailbox.entity.MailPageVO
import com.dandi.nyummy.mailbox.entity.MailVO
import com.dandi.nyummy.mailbox.entity.RewardCoinVO
import com.dandi.nyummy.mailbox.entity.RewardVO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class MailUseCasesTest {

    private val repository = FakeMailboxRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()

    @Test
    fun `우편 목록은 커서와 크기를 넘기고, 크기는 서버 최대치로 자른다`() = runBlocking {
        repository.mailPage = MailPageVO(mails = listOf(MailVO(mailId = 1)), nextCursor = 9)

        val result = getMails()(cursor = 5, size = 999)

        assertEquals(listOf<Pair<Long?, Int>>(5L to GetMailsUseCase.MAX_PAGE_SIZE), repository.mailRequests)
        assertEquals(9L, result.getOrNull()?.nextCursor)
    }

    @Test
    fun `우편 목록 중 네트워크 오류면 안내 없이 실패를 돌려준다`() = runBlocking {
        repository.error = IOException("offline")

        val result = getMails()()

        assertTrue(result.isFailure)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `받기에 성공하면 지급된 보상을 돌려준다`() = runBlocking {
        val reward = RewardVO(coin = RewardCoinVO(quantity = 100))
        repository.reward = reward

        val result = claimMail()(7L)

        assertEquals(reward, result.getOrNull())
        assertEquals(listOf(7L), repository.claimedMailIds)
    }

    @Test
    fun `이미 받았거나 없거나 기한이 지난 우편이면 실패가 아니라 null 을 돌려준다`() = runBlocking {
        for (code in listOf("api.mail.alreadyClaimed", "api.mail.notFound", "api.mail.expired")) {
            repository.error = httpException(if (code == "api.mail.notFound") 404 else 409, code)

            val result = claimMail()(7L)

            assertTrue("$code 는 성공이어야 한다", result.isSuccess)
            assertNull(result.getOrNull())
        }
        // 우편 code가 먼저 처리되어 404여도 공통 "준비 중" 안내가 뜨지 않는다.
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `받기 중 서버 오류면 공통 안내를 한 번 띄우고 실패를 돌려준다`() = runBlocking {
        repository.error = httpException(500)

        val result = claimMail()(7L)

        assertTrue(result.isFailure)
        assertEquals(1, messageHelper.dialogs.size)
    }

    @Test
    fun `받기 중 네트워크 오류면 앱이 죽지 않고 실패를 돌려준다`() = runBlocking {
        repository.error = IOException("offline")

        assertTrue(claimMail()(7L).isFailure)
    }

    @Test
    fun `모두 받기에 성공하면 보상 합계를 돌려준다`() = runBlocking {
        val reward = RewardVO(coin = RewardCoinVO(quantity = 410))
        repository.reward = reward

        val result = claimAllMails()()

        assertEquals(reward, result.getOrNull())
        assertEquals(1, repository.claimAllCount)
    }

    @Test
    fun `이미 없는 우편을 지우면 지운 것과 같으니 성공이다`() = runBlocking {
        repository.error = httpException(404, "api.mail.notFound")

        assertTrue(deleteMail()(3L).isSuccess)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `삭제 중 서버 오류면 실패를 돌려준다`() = runBlocking {
        repository.error = httpException(400)

        assertTrue(deleteMail()(3L).isFailure)
    }

    @Test
    fun `복구에 실패하면 안내 없이 실패를 돌려준다`() = runBlocking {
        repository.error = httpException(404, "api.mail.notFound")

        assertTrue(restoreMail()(3L).isFailure)
        assertTrue(messageHelper.dialogs.isEmpty())
    }

    @Test
    fun `우편 요청이 취소되면 실패로 바꾸지 않고 그대로 던진다`() = runBlocking {
        repository.error = CancellationException("left mailbox")

        val calls: List<suspend () -> Result<*>> = listOf(
            { getMails()() },
            { claimMail()(1L) },
            { claimAllMails()() },
            { deleteMail()(1L) },
            { restoreMail()(1L) },
        )
        for (call in calls) {
            val thrown = runCatching { call() }.exceptionOrNull()
            assertTrue("취소가 전파되지 않았다: $thrown", thrown is CancellationException)
        }
    }

    private fun getMails() = GetMailsUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
    private fun claimMail() = ClaimMailUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
    private fun claimAllMails() = ClaimAllMailsUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
    private fun deleteMail() = DeleteMailUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
    private fun restoreMail() = RestoreMailUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper)
}
