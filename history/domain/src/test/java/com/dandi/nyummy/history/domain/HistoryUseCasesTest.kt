package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.history.entity.AnalysisFeedbackReason
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.HistoryMonthWithDayVO
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class HistoryUseCasesTest {

    private val navigationHelper = RecordingNavigationHelper()
    private val messageHelper = RecordingMessageHelper()

    @Test
    fun `월간 조회 성공 시 캘린더 VO 를 돌려준다`() = runBlocking {
        val calendar = HistoryCalendarVO(year = 2026, month = 8)
        val useCase = GetMonthlyMealsUseCase(
            repository = FakeHistoryRepository(monthly = calendar),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(2026, 8)

        assertEquals(Result.success(calendar), result)
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
    }

    @Test
    fun `월간 조회 중 401 이면 세션 만료 다이얼로그를 띄우고 실패를 돌려준다`() = runBlocking {
        val useCase = GetMonthlyMealsUseCase(
            repository = ThrowingHistoryRepository(httpException(401)),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(2026, 8)

        val dialog = messageHelper.oneButtonDialogs.single()
        assertTrue(dialog.cantIgnore)
        dialog.onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
        assertTrue(result.isFailure)
    }

    @Test
    fun `월간 조회 중 액세스 토큰 만료 code 의 401 도 세션 만료 다이얼로그를 띄운다`() = runBlocking {
        // 이 401은 토큰 재발급을 거친 뒤에 올라온다. 재발급까지 실패했다는 뜻이다.
        val useCase = GetMonthlyMealsUseCase(
            repository = ThrowingHistoryRepository(httpException(401, "api.auth.tokenExpired")),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(2026, 8)

        val dialog = messageHelper.oneButtonDialogs.single()
        assertEquals("로그인이 만료됐어요. 다시 로그인해 주세요.", dialog.descText)
        dialog.onClickButton?.invoke()
        assertEquals(1, navigationHelper.initialCount)
        assertTrue(result.isFailure)
    }

    @Test
    fun `월간 조회 중 비공통 에러면 아무것도 띄우지 않고 실패를 돌려준다`() = runBlocking {
        val useCase = GetMonthlyMealsUseCase(
            repository = ThrowingHistoryRepository(httpException(400)),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(2026, 8)

        // 화면이 오류를 보여 주고 다시 불러오게 하므로 스낵바를 띄우지 않는다.
        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
        assertTrue(result.isFailure)
    }

    @Test
    fun `월간 조회 중 네트워크 오류면 아무것도 띄우지 않고 실패를 돌려준다`() = runBlocking {
        val useCase = GetMonthlyMealsUseCase(
            repository = ThrowingHistoryRepository(IOException("offline")),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(2026, 8)

        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
        assertEquals(0, navigationHelper.initialCount)
        assertTrue(result.isFailure)
    }

    @Test
    fun `일일 조회 성공 시 상세 VO 를 돌려준다`() = runBlocking {
        val daily = DailyMealHistoryVO.empty
        val useCase = GetDailyMealsUseCase(
            repository = FakeHistoryRepository(daily = daily),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(2026, 8, 23)

        assertEquals(Result.success(daily), result)
    }

    @Test
    fun `일일 조회 중 네트워크 오류면 아무것도 띄우지 않고 실패를 돌려준다`() = runBlocking {
        val useCase = GetDailyMealsUseCase(
            repository = ThrowingHistoryRepository(IOException("offline")),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(2026, 8, 23)

        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
        assertTrue(result.isFailure)
    }

    @Test
    fun `이름 수정 성공 시 수정된 식사를 돌려준다`() = runBlocking {
        val renamed = MealHistoryVO(id = "1", name = "연어 포케")
        val useCase = UpdateMealNameUseCase(
            repository = FakeHistoryRepository(meal = renamed),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(1L, "연어 포케")

        assertEquals(Result.success(renamed), result)
    }

    @Test
    fun `삭제 중 5xx 이면 일시 오류 다이얼로그를 띄우고 실패를 돌려준다`() = runBlocking {
        val useCase = DeleteMealUseCase(
            repository = ThrowingHistoryRepository(httpException(500)),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(1L)

        assertEquals("잠시 문제가 생겼어요", messageHelper.oneButtonDialogs.single().titleText)
        assertTrue(result.isFailure)
    }

    @Test
    fun `재분석 성공 시 서버가 준 분석 상태를 돌려준다`() = runBlocking {
        val useCase = ReanalyzeMealUseCase(
            repository = FakeHistoryRepository(reanalyzed = MealAnalysisStatus.ANALYZING),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(7L)

        assertEquals(Result.success(MealAnalysisStatus.ANALYZING), result)
        assertTrue(messageHelper.snackBars.isEmpty())
    }

    @Test
    fun `재분석 중 일반 오류면 안내 스낵바를 띄우고 실패를 돌려준다`() = runBlocking {
        val useCase = ReanalyzeMealUseCase(
            repository = ThrowingHistoryRepository(httpException(400)),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(7L)

        val snackBar = messageHelper.snackBars.single()
        assertEquals(IconType.ERROR, snackBar.iconType)
        assertEquals("다시 분석하지 못했어요", snackBar.messageText)
        assertTrue(result.isFailure)
        assertFalse(result.exceptionOrNull() is OutdatedMealException)
    }

    @Test
    fun `지운 기록의 404 는 공통 404 대신 지워진 기록으로 안내한다`() = runBlocking {
        val useCase = GetMealDetailUseCase(
            repository = ThrowingHistoryRepository(httpException(404, HistoryErrorType.MEAL_NOT_FOUND.type)),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(1L)

        assertEquals("이미 지워진 기록이에요", messageHelper.snackBars.single().messageText)
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
        assertEquals(0, navigationHelper.backCount)
        assertTrue(result.isFailure)
    }

    @Test
    fun `code 없는 404 는 준비 중인 기능으로 안내하고 뒤로 간다`() = runBlocking {
        val useCase = GetMealDetailUseCase(
            repository = ThrowingHistoryRepository(httpException(404)),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        useCase(1L)

        val dialog = messageHelper.oneButtonDialogs.single()
        assertEquals("준비 중인 기능이에요.", dialog.descText)
        dialog.onClickButton?.invoke()
        assertEquals(1, navigationHelper.backCount)
    }

    @Test
    fun `이미 지워진 기록을 지우면 성공으로 돌려준다`() = runBlocking {
        val useCase = DeleteMealUseCase(
            repository = ThrowingHistoryRepository(httpException(404, HistoryErrorType.MEAL_NOT_FOUND.type)),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(1L)

        assertTrue(result.isSuccess)
        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
    }

    @Test
    fun `이미 다시 분석한 기록이면 안내하고 화면의 식사가 낡았다고 알린다`() = runBlocking {
        val useCase = ReanalyzeMealUseCase(
            repository = ThrowingHistoryRepository(httpException(409, HistoryErrorType.ANALYSIS_NOT_RETRYABLE.type)),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(1L)

        assertEquals("이미 다시 분석한 기록이에요", messageHelper.snackBars.single().messageText)
        assertTrue(result.exceptionOrNull() is OutdatedMealException)
    }

    @Test
    fun `지운 기록을 다시 분석하면 지워진 기록으로 안내하고 화면의 식사가 낡았다고 알린다`() = runBlocking {
        val useCase = ReanalyzeMealUseCase(
            repository = ThrowingHistoryRepository(httpException(404, HistoryErrorType.MEAL_NOT_FOUND.type)),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(1L)

        assertEquals("이미 지워진 기록이에요", messageHelper.snackBars.single().messageText)
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
        assertTrue(result.exceptionOrNull() is OutdatedMealException)
    }

    @Test
    fun `상세 조회 중 네트워크 오류면 죽지 않고 아무것도 띄우지 않는다`() = runBlocking {
        val useCase = GetMealDetailUseCase(
            repository = ThrowingHistoryRepository(IOException("offline")),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(7L)

        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
        assertTrue(result.isFailure)
    }

    @Test
    fun `이름 수정 중 네트워크 오류면 죽지 않고 수정 실패를 안내한다`() = runBlocking {
        val useCase = UpdateMealNameUseCase(
            repository = ThrowingHistoryRepository(IOException("offline")),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(7L, "김치찌개")

        assertEquals("이름 수정에 실패했어요", messageHelper.snackBars.single().messageText)
        assertTrue(result.isFailure)
    }

    @Test
    fun `재분석 중 네트워크 오류면 죽지 않고 재분석 실패를 안내한다`() = runBlocking {
        val useCase = ReanalyzeMealUseCase(
            repository = ThrowingHistoryRepository(IOException("offline")),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(7L)

        assertEquals("다시 분석하지 못했어요", messageHelper.snackBars.single().messageText)
        assertTrue(result.isFailure)
    }

    @Test
    fun `삭제 중 네트워크 오류면 죽지 않고 삭제 실패를 안내한다`() = runBlocking {
        val useCase = DeleteMealUseCase(
            repository = ThrowingHistoryRepository(IOException("offline")),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        val result = useCase(7L)

        assertEquals("삭제에 실패했어요", messageHelper.snackBars.single().messageText)
        assertTrue(result.isFailure)
    }

    @Test
    fun `상세 조회가 취소되면 실패로 바꾸지 않고 그대로 던진다`() = runBlocking {
        val useCase = GetMealDetailUseCase(
            repository = ThrowingHistoryRepository(CancellationException("left history")),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        assertCancelledSilently { useCase(7L) }
    }

    @Test
    fun `이름 수정이 취소되면 실패로 바꾸지 않고 그대로 던진다`() = runBlocking {
        val useCase = UpdateMealNameUseCase(
            repository = ThrowingHistoryRepository(CancellationException("left history")),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        assertCancelledSilently { useCase(7L, "김치찌개") }
    }

    @Test
    fun `재분석이 취소되면 실패로 바꾸지 않고 그대로 던진다`() = runBlocking {
        val useCase = ReanalyzeMealUseCase(
            repository = ThrowingHistoryRepository(CancellationException("left history")),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        assertCancelledSilently { useCase(7L) }
    }

    @Test
    fun `삭제가 취소되면 실패로 바꾸지 않고 그대로 던진다`() = runBlocking {
        val useCase = DeleteMealUseCase(
            repository = ThrowingHistoryRepository(CancellationException("left history")),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        assertCancelledSilently { useCase(7L) }
    }

    @Test
    fun `5xx 공통 오류는 서버 상태 문자열을 보여 주지 않는다`() = runBlocking {
        val useCase = GetMealDetailUseCase(
            repository = ThrowingHistoryRepository(httpException(500, "api.common.internalServerError")),
            resourceHelper = FakeResourceHelper(),
            messageHelper = messageHelper,
            navigationHelper = navigationHelper,
            ttiHelper = FakeTTIHelper(),
        )

        useCase(1L)

        val dialog = messageHelper.oneButtonDialogs.single()
        assertEquals("잠시 후 다시 시도해 주세요.", dialog.descText)
        assertTrue("500" !in dialog.descText && "api." !in dialog.descText)
    }

    @Test
    fun `평가를 보내면 반 칸 수와 합친 이유를 넘기고 고맙다는 스낵바를 띄운다`() = runBlocking {
        val repository = FakeHistoryRepository()
        val useCase = submitFeedbackUseCase(repository)

        val result = useCase(7L, ratingHalfSteps = 7, reason = AnalysisFeedbackReason.WRONG_PORTION, comment = "  밥이 더 많았어요 ")

        assertTrue(result.isSuccess)
        assertEquals(Triple(7L, 7, "양을 다르게 봤어요\n밥이 더 많았어요"), repository.feedbackRequests.single())
        val snackBar = messageHelper.snackBars.single()
        assertEquals(IconType.SUCCESS, snackBar.iconType)
        assertEquals("고마워요! 냐미가 더 잘 알아볼게요", snackBar.messageText)
    }

    @Test
    fun `다른 기기에서 이미 평가한 식사면 성공으로 보고 고맙다는 스낵바를 띄운다`() = runBlocking {
        val useCase = submitFeedbackUseCase(
            ThrowingHistoryRepository(httpException(409, "api.meal.analysisFeedbackAlreadyExists")),
        )

        val result = useCase(7L, ratingHalfSteps = 10, reason = null, comment = "")

        assertTrue(result.isSuccess)
        assertEquals(IconType.SUCCESS, messageHelper.snackBars.single().iconType)
    }

    @Test
    fun `분석이 끝나지 않은 식사를 평가하면 안내하고 실패를 돌려준다`() = runBlocking {
        val useCase = submitFeedbackUseCase(ThrowingHistoryRepository(httpException(409, "api.meal.analysisNotCompleted")))

        val result = useCase(7L, ratingHalfSteps = 6, reason = null, comment = "")

        assertTrue(result.isFailure)
        val snackBar = messageHelper.snackBars.single()
        assertEquals(IconType.ERROR, snackBar.iconType)
        assertEquals("분석이 끝난 식사만 평가할 수 있어요", snackBar.messageText)
    }

    @Test
    fun `평가 중 네트워크 오류면 죽지 않고 보내지 못했다고 안내한다`() = runBlocking {
        val useCase = submitFeedbackUseCase(ThrowingHistoryRepository(IOException("offline")))

        val result = useCase(7L, ratingHalfSteps = 6, reason = null, comment = "")

        assertTrue(result.isFailure)
        assertEquals("평가를 보내지 못했어요. 잠시 후 다시 시도해 주세요.", messageHelper.snackBars.single().messageText)
    }

    @Test
    fun `범위를 벗어난 별점은 죽지 않고 보내지 않은 채 다시 고르라고 안내한다`() = runBlocking {
        val repository = FakeHistoryRepository()
        val useCase = submitFeedbackUseCase(repository)

        listOf(0, 11).forEach { halfSteps ->
            val result = useCase(7L, ratingHalfSteps = halfSteps, reason = null, comment = "")
            assertTrue(result.isFailure)
        }

        assertTrue(repository.feedbackRequests.isEmpty())
        assertEquals(listOf("별점을 다시 골라 주세요", "별점을 다시 골라 주세요"), messageHelper.snackBars.map { it.messageText })
    }

    @Test
    fun `평가 이유는 아쉬운 점과 직접 쓴 글을 줄을 바꿔 합치고 둘 다 없으면 보내지 않는다`() {
        assertNull(feedbackReasonOf(null, "   "))
        assertEquals("빠진 음식이 있어요", feedbackReasonOf(AnalysisFeedbackReason.MISSING_FOOD, ""))
        assertEquals("국이 빠졌어요", feedbackReasonOf(null, "  국이 빠졌어요\n"))
        assertEquals(
            "음식을 잘못 알아봤어요\n떡볶이가 아니라 라볶이예요",
            feedbackReasonOf(AnalysisFeedbackReason.WRONG_FOOD, "떡볶이가 아니라 라볶이예요"),
        )
    }

    @Test
    fun `평가 이유는 서버 한도 500 자까지만 보낸다`() {
        val reason = feedbackReasonOf(AnalysisFeedbackReason.WRONG_NUTRITION, "가".repeat(600))

        assertEquals(SubmitAnalysisFeedbackUseCase.REASON_MAX_LENGTH, reason?.length)
    }

    @Test
    fun `월간과 일간을 동시에 요청하고 함께 돌려준다`() = runBlocking {
        val calendar = HistoryCalendarVO(year = 2026, month = 8)
        val daily = DailyMealHistoryVO(date = HistoryDateVO(2026, 8, 3))
        // 두 요청이 모두 시작돼야 응답한다. 차례로 부르면 첫 요청이 끝나지 않아 시간 초과로 실패한다.
        val bothStarted = CompletableDeferred<Unit>()
        var started = 0
        val markStarted = {
            started++
            if (started == 2) bothStarted.complete(Unit)
        }
        val repository = SuspendingHistoryRepository(
            onMonthly = { markStarted(); bothStarted.await(); calendar },
            onDaily = { markStarted(); bothStarted.await(); daily },
        )

        val result = withTimeout(1_000) { monthWithDayUseCase(repository)(2026, 8, HistoryDateVO(2026, 8, 3)) }

        assertEquals(Result.success(HistoryMonthWithDayVO(calendar = calendar, dailyDetail = daily)), result)
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
    }

    @Test
    fun `두 요청이 함께 로그인 만료로 실패해도 세션 만료 안내는 한 번만 띄운다`() = runBlocking {
        val bothStarted = CompletableDeferred<Unit>()
        var started = 0
        val failAfterBothStarted: suspend () -> Nothing = {
            started++
            if (started == 2) bothStarted.complete(Unit)
            bothStarted.await()
            throw httpException(401)
        }
        val repository = SuspendingHistoryRepository(onMonthly = failAfterBothStarted, onDaily = failAfterBothStarted)

        val result = withTimeout(1_000) { monthWithDayUseCase(repository)(2026, 8, HistoryDateVO(2026, 8, 3)) }

        assertEquals(2, started)
        assertEquals(1, messageHelper.oneButtonDialogs.size)
        assertTrue(result.isFailure)
    }

    @Test
    fun `한쪽이 실패하면 다른 쪽 요청은 취소하고 실패를 돌려준다`() = runBlocking {
        // 일간 요청이 진행 중일 때 월간이 실패하게 한다.
        val dailyStarted = CompletableDeferred<Unit>()
        var dailyCancelled = false
        val repository = SuspendingHistoryRepository(
            onMonthly = {
                dailyStarted.await()
                throw httpException(401)
            },
            onDaily = {
                dailyStarted.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    dailyCancelled = true
                }
            },
        )

        val result = withTimeout(1_000) { monthWithDayUseCase(repository)(2026, 8, HistoryDateVO(2026, 8, 3)) }

        assertTrue(dailyCancelled)
        assertEquals(1, messageHelper.oneButtonDialogs.size)
        assertTrue(result.isFailure)
    }

    @Test
    fun `월간과 일간 조회 중 네트워크 오류면 아무것도 띄우지 않고 실패를 돌려준다`() = runBlocking {
        val result = monthWithDayUseCase(ThrowingHistoryRepository(IOException("offline")))(
            2026, 8, HistoryDateVO(2026, 8, 3),
        )

        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
        assertTrue(result.isFailure)
    }

    @Test
    fun `월간과 일간 조회가 취소되면 실패로 바꾸지 않고 그대로 던진다`() = runBlocking {
        val useCase = monthWithDayUseCase(ThrowingHistoryRepository(CancellationException("left history")))

        assertCancelledSilently { useCase(2026, 8, HistoryDateVO(2026, 8, 3)) }
    }

    private fun monthWithDayUseCase(repository: HistoryRepository) = GetMonthWithDayMealsUseCase(
        repository, FakeResourceHelper(), messageHelper, navigationHelper, FakeTTIHelper(),
    )

    private fun submitFeedbackUseCase(repository: HistoryRepository) = SubmitAnalysisFeedbackUseCase(
        repository, FakeResourceHelper(), messageHelper, navigationHelper, FakeTTIHelper(),
    )

    /** 취소는 `Result.failure`로 바꾸지 않고 그대로 던져야 하고, 실패 안내도 띄우지 않아야 한다. */
    private suspend fun assertCancelledSilently(block: suspend () -> Result<*>) {
        val thrown = runCatching { block() }.exceptionOrNull()

        assertTrue("취소가 전파되지 않았다: $thrown", thrown is CancellationException)
        assertTrue(messageHelper.snackBars.isEmpty())
        assertTrue(messageHelper.oneButtonDialogs.isEmpty())
    }

    /** 서버 에러 바디의 `code` 를 cause 로 담는 실제 변환(BaseRemoteDataSource)과 같은 형태로 만든다. */
    private fun httpException(code: Int, errorCode: String? = null): HttpResponseException = HttpResponseException(
        status = HttpResponseStatus.create(code),
        rawCode = code,
        errorRequestUrl = "https://test/meals",
        msg = "Http Request Failed ($code)",
        cause = errorCode?.let(::Throwable),
    )

    private class FakeHistoryRepository(
        private val monthly: HistoryCalendarVO = HistoryCalendarVO.empty,
        private val daily: DailyMealHistoryVO = DailyMealHistoryVO.empty,
        private val meal: MealHistoryVO = MealHistoryVO.empty,
        private val reanalyzed: MealAnalysisStatus = MealAnalysisStatus.ANALYZING,
    ) : HistoryRepository {
        override suspend fun getMonthlyCalendar(year: Int, month: Int) = monthly
        override suspend fun getDailyMeals(year: Int, month: Int, day: Int) = daily
        override suspend fun getMeal(mealId: Long) = meal
        override suspend fun updateMealName(mealId: Long, name: String) = meal
        override suspend fun reanalyzeMeal(mealId: Long) = reanalyzed
        override suspend fun deleteMeal(mealId: Long) = Unit

        val feedbackRequests = mutableListOf<Triple<Long, Int, String?>>()

        override suspend fun submitAnalysisFeedback(mealId: Long, ratingHalfSteps: Int, reason: String?) {
            feedbackRequests += Triple(mealId, ratingHalfSteps, reason)
        }
    }

    private class ThrowingHistoryRepository(
        private val exception: Exception,
    ) : HistoryRepository {
        override suspend fun getMonthlyCalendar(year: Int, month: Int) = throw exception
        override suspend fun getDailyMeals(year: Int, month: Int, day: Int) = throw exception
        override suspend fun getMeal(mealId: Long) = throw exception
        override suspend fun updateMealName(mealId: Long, name: String) = throw exception
        override suspend fun reanalyzeMeal(mealId: Long): MealAnalysisStatus = throw exception
        override suspend fun deleteMeal(mealId: Long) = throw exception
        override suspend fun submitAnalysisFeedback(mealId: Long, ratingHalfSteps: Int, reason: String?) = throw exception
    }

    /** 월간과 일간 응답을 테스트가 정한 suspend 블록으로 돌려준다. 나머지는 쓰지 않는다. */
    private class SuspendingHistoryRepository(
        private val onMonthly: suspend () -> HistoryCalendarVO,
        private val onDaily: suspend () -> DailyMealHistoryVO,
    ) : HistoryRepository {
        override suspend fun getMonthlyCalendar(year: Int, month: Int) = onMonthly()
        override suspend fun getDailyMeals(year: Int, month: Int, day: Int) = onDaily()
        override suspend fun getMeal(mealId: Long): MealHistoryVO = error("not used")
        override suspend fun updateMealName(mealId: Long, name: String): MealHistoryVO = error("not used")
        override suspend fun reanalyzeMeal(mealId: Long): MealAnalysisStatus = error("not used")
        override suspend fun deleteMeal(mealId: Long) = error("not used")
        override suspend fun submitAnalysisFeedback(mealId: Long, ratingHalfSteps: Int, reason: String?) = error("not used")
    }

    private class RecordingNavigationHelper : NavigationHelper {
        var initialCount = 0
        var backCount = 0

        override val navigationFlow: Flow<NavSignal> = emptyFlow()
        override fun navigateByRoute(route: NavRoute) = Unit
        override fun navigateTo(page: Page) = Unit
        override fun navigateDeepLink(route: NavRoute) = Unit
        override fun navigateToBack() {
            backCount++
        }

        override fun navigateToAsRoot(page: Page) = Unit
        override fun navigateToInitial() {
            initialCount++
        }

        override fun navigateToExternalLink(url: String) = Unit
    }

    private class FakeResourceHelper : ResourceHelper {
        override fun getString(resource: StringResource): String = ""
    }

    private class RecordingMessageHelper : MessageHelper {
        data class OneButtonDialogCall(
            val titleText: String?,
            val descText: String,
            val cantIgnore: Boolean,
            val buttonText: String,
            val onClickButton: (() -> Unit)?,
        )

        data class SnackBarCall(val iconType: IconType, val messageText: String)

        val oneButtonDialogs = mutableListOf<OneButtonDialogCall>()
        val snackBars = mutableListOf<SnackBarCall>()

        override val effect: Flow<MessageEffect> = emptyFlow()
        override fun showToast(toastMsg: String) = Unit
        override fun showToast(messageRes: Int) = Unit
        override fun showSnackBar(
            iconType: IconType,
            messageText: String,
            callToActionText: String?,
            onClickCTA: (() -> Unit)?,
        ) {
            snackBars += SnackBarCall(iconType, messageText)
        }

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
            oneButtonDialogs += OneButtonDialogCall(titleText, descText, cantIgnore, buttonText, onClickButton)
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

    private class FakeTTIHelper : TTIHelper {
        override fun startTTITracking(page: TTIPage) = Unit
        override fun startTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITimeline(category: TimelineCategory) = Unit
        override fun endTTITracking() = Unit
        override fun shotTTILogging() = Unit
        override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
    }
}
