package com.dandi.nyummy.settings.presentation.profile

import com.dandi.nyummy.auth.domain.LogoutUseCase
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.auth.entity.UserProfileRange
import com.dandi.nyummy.cat.domain.CatRepository
import com.dandi.nyummy.cat.domain.RenameCatUseCase
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import com.dandi.nyummy.common.entity.time.DeviceTime
import com.dandi.nyummy.settings.domain.GetMyProfileUseCase
import com.dandi.nyummy.settings.domain.UpdateMyProfileUseCase
import com.dandi.nyummy.settings.entity.MyProfileChange
import com.dandi.nyummy.settings.entity.MyProfileVO
import com.dandi.nyummy.settings.presentation.FakeAuthRepository
import com.dandi.nyummy.settings.presentation.FakeCompanionMemoryHelper
import com.dandi.nyummy.settings.presentation.FakePushTokenHelper
import com.dandi.nyummy.settings.presentation.FakeResourceHelper
import com.dandi.nyummy.settings.presentation.FakeSettingsRepository
import com.dandi.nyummy.settings.presentation.FakeTTIHelper
import com.dandi.nyummy.settings.presentation.FakeTutorialHelper
import com.dandi.nyummy.settings.presentation.NoOpWidgetHelper
import com.dandi.nyummy.settings.presentation.RecordingMessageHelper
import com.dandi.nyummy.settings.presentation.RecordingNavigationHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MyProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = FakeSettingsRepository()
    private val messageHelper = RecordingMessageHelper()
    private val navigationHelper = RecordingNavigationHelper()
    private val catRepository = FakeCatRepository()
    private val memory = FakeCompanionMemory()

    private val viewModel by lazy {
        MyProfileViewModel(
            getMyProfile = GetMyProfileUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            updateMyProfile = UpdateMyProfileUseCase(repository, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            renameCat = RenameCatUseCase(catRepository, memory, FakeResourceHelper, messageHelper, navigationHelper, FakeTTIHelper),
            logout = LogoutUseCase(FakeAuthRepository, FakePushTokenHelper, FakeCompanionMemoryHelper, FakeTutorialHelper, NoOpWidgetHelper, navigationHelper),
            navigationHelper = navigationHelper,
        )
    }

    private val state get() = viewModel.uiState.value

    private val profile = MyProfileVO(
        nickname = "진우",
        gender = Gender.FEMALE,
        birth = LocalDate.of(2001, 3, 14),
        heightCm = 163,
        weightKg = 52,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository.profile = profile
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun kotlinx.coroutines.test.TestScope.enter() {
        viewModel.onIntent(MyProfileIntent.Enter)
        advanceUntilIdle()
    }

    @Test
    fun `들어오면 내 프로필을 읽어 보여준다`() = runTest(testDispatcher) {
        enter()

        assertTrue(state.isLoaded)
        assertEquals(profile, state.profile.copy(mealTimes = profile.mealTimes))
    }

    @Test
    fun `읽기 전에는 집사 정보 시트를 열지 않고 다시 시도하면 다시 읽는다`() = runTest(testDispatcher) {
        repository.getMealTimesError = IOException("offline")
        enter()

        viewModel.onIntent(MyProfileIntent.ClickField(MyProfileField.WEIGHT))
        assertNull(state.editing)

        repository.getMealTimesError = null
        messageHelper.snackBars.single().onClickCTA?.invoke()
        advanceUntilIdle()
        assertTrue(state.isLoaded)
    }

    @Test
    fun `시트를 열면 지금 값으로 채운다`() = runTest(testDispatcher) {
        enter()

        viewModel.onIntent(MyProfileIntent.ClickField(MyProfileField.BIRTH))

        assertEquals(MyProfileField.BIRTH, state.editing)
        assertEquals(LocalDate.of(2001, 3, 14), state.draft.birth)
        assertEquals(163, state.draft.heightCm)
        assertEquals(Gender.FEMALE, state.draft.gender)
    }

    @Test
    fun `서버가 비워 둔 값은 회원가입 첫 값을 가리킨다`() = runTest(testDispatcher) {
        repository.profile = MyProfileVO(nickname = "진우")
        enter()

        viewModel.onIntent(MyProfileIntent.ClickField(MyProfileField.HEIGHT))

        assertEquals(MyProfileDraft.DEFAULT_HEIGHT_CM, state.draft.heightCm)
        assertEquals(MyProfileDraft.DEFAULT_BIRTH, state.draft.birth)
        assertNull(state.draft.gender)
    }

    @Test
    fun `몸무게를 바꿔 저장하면 바꾼 항목만 보내고 화면에 반영한 뒤 시트를 닫는다`() = runTest(testDispatcher) {
        enter()
        viewModel.onIntent(MyProfileIntent.ClickField(MyProfileField.WEIGHT))
        viewModel.onIntent(MyProfileIntent.SelectWeight(50))

        viewModel.onIntent(MyProfileIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(listOf(MyProfileChange(weightKg = 50)), repository.profileChanges)
        assertEquals(50, state.profile.weightKg)
        assertNull(state.editing)
        assertFalse(state.isSaving)
    }

    @Test
    fun `바꾸지 않고 저장하면 서버에 보내지 않고 시트만 닫는다`() = runTest(testDispatcher) {
        enter()
        viewModel.onIntent(MyProfileIntent.ClickField(MyProfileField.HEIGHT))

        viewModel.onIntent(MyProfileIntent.ClickSave)
        advanceUntilIdle()

        assertTrue(repository.profileChanges.isEmpty())
        assertNull(state.editing)
    }

    @Test
    fun `저장하지 못하면 값은 그대로 두고 시트를 닫으며 다시 시도는 같은 값을 다시 보낸다`() = runTest(testDispatcher) {
        enter()
        repository.profileUpdateResponses.addLast { throw IOException("offline") }
        viewModel.onIntent(MyProfileIntent.ClickField(MyProfileField.HEIGHT))
        viewModel.onIntent(MyProfileIntent.SelectHeight(170))

        viewModel.onIntent(MyProfileIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(163, state.profile.heightCm)
        assertNull(state.editing)
        messageHelper.snackBars.single().onClickCTA?.invoke()
        advanceUntilIdle()
        assertEquals(listOf(MyProfileChange(heightCm = 170)), repository.profileChanges)
        assertEquals(170, state.profile.heightCm)
    }

    @Test
    fun `저장 중에는 시트를 닫지 않고 다시 저장하지 않는다`() = runTest(testDispatcher) {
        enter()
        val gate = CompletableDeferred<Unit>()
        repository.profileUpdateResponses.addLast { gate.await() }
        viewModel.onIntent(MyProfileIntent.ClickField(MyProfileField.WEIGHT))
        viewModel.onIntent(MyProfileIntent.SelectWeight(50))
        viewModel.onIntent(MyProfileIntent.ClickSave)
        advanceUntilIdle()

        viewModel.onIntent(MyProfileIntent.DismissSheet)
        viewModel.onIntent(MyProfileIntent.ClickSave)
        assertEquals(MyProfileField.WEIGHT, state.editing)
        assertTrue(state.isSaving)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repository.profileChanges.size)
    }

    @Test
    fun `집사 이름이 비면 보내지 않고 오류를 보여 주며 고치면 오류를 지운다`() = runTest(testDispatcher) {
        enter()
        viewModel.onIntent(MyProfileIntent.ClickField(MyProfileField.NICKNAME))
        assertEquals("진우", state.draft.text)

        viewModel.onIntent(MyProfileIntent.InputName("  "))
        viewModel.onIntent(MyProfileIntent.ClickSave)
        assertEquals(NameInputError.EMPTY, state.nameError)

        viewModel.onIntent(MyProfileIntent.InputName(" 진우우 "))
        assertNull(state.nameError)
        viewModel.onIntent(MyProfileIntent.ClickSave)
        advanceUntilIdle()
        assertEquals(listOf(MyProfileChange(nickname = "진우우")), repository.profileChanges)
        assertEquals("진우우", state.profile.nickname)
    }

    @Test
    fun `성별을 모르면 고르기 전까지 저장을 막는다`() = runTest(testDispatcher) {
        repository.profile = profile.copy(gender = Gender.OTHER)
        enter()
        viewModel.onIntent(MyProfileIntent.ClickField(MyProfileField.GENDER))
        assertFalse(state.isSaveEnabled)

        viewModel.onIntent(MyProfileIntent.SelectGender(Gender.MALE))
        assertTrue(state.isSaveEnabled)
        viewModel.onIntent(MyProfileIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(listOf(MyProfileChange(gender = Gender.MALE)), repository.profileChanges)
    }

    @Test
    fun `고른 일이 그달에 없으면 그달 마지막 날로 저장한다`() = runTest(testDispatcher) {
        enter()
        viewModel.onIntent(MyProfileIntent.ClickField(MyProfileField.BIRTH))
        viewModel.onIntent(MyProfileIntent.SelectBirthDay(31))
        viewModel.onIntent(MyProfileIntent.SelectBirthMonth(2))

        viewModel.onIntent(MyProfileIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(listOf(MyProfileChange(birth = LocalDate.of(2001, 2, 28))), repository.profileChanges)
    }

    @Test
    fun `고양이 이름은 규칙에 맞지 않으면 오류를 보여 주고 서버에 보내지 않는다`() = runTest(testDispatcher) {
        viewModel.onIntent(MyProfileIntent.ClickCatName(currentName = "냐미"))
        viewModel.onIntent(MyProfileIntent.InputName("아홉글자인고양이름"))

        viewModel.onIntent(MyProfileIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(NameInputError.CAT_NAME_TOO_LONG, state.nameError)
        assertTrue(catRepository.renamed.isEmpty())
    }

    @Test
    fun `고양이 이름은 프로필을 읽기 전에도 바꾸고 저장되면 기기에 기억한 이름도 바뀐다`() = runTest(testDispatcher) {
        viewModel.onIntent(MyProfileIntent.ClickCatName(currentName = "냐미"))
        assertEquals("냐미", state.draft.text)
        viewModel.onIntent(MyProfileIntent.InputName(" 달봉이 "))

        viewModel.onIntent(MyProfileIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(listOf("달봉이"), catRepository.renamed)
        assertEquals("달봉이", memory.catName)
        assertNull(state.editing)
    }

    @Test
    fun `고양이 이름을 그대로 저장하면 서버에 보내지 않는다`() = runTest(testDispatcher) {
        viewModel.onIntent(MyProfileIntent.ClickCatName(currentName = "냐미"))

        viewModel.onIntent(MyProfileIntent.ClickSave)
        advanceUntilIdle()

        assertTrue(catRepository.renamed.isEmpty())
        assertNull(state.editing)
    }

    private class FakeCatRepository : CatRepository {
        val renamed = mutableListOf<String>()

        override suspend fun getAnimations(): CatAnimationSetVO = CatAnimationSetVO.empty
        override suspend fun refreshWeight(): Boolean = false
        override suspend fun rename(name: String) {
            renamed += name
        }
    }

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

    // region 입력 제한

    @Test
    fun `집사 이름이 8자를 넘으면 보내지 않고 글자 수 오류를 보여 준다`() = runTest(testDispatcher) {
        viewModel.onIntent(MyProfileIntent.Enter)
        advanceUntilIdle()
        viewModel.onIntent(MyProfileIntent.ClickField(MyProfileField.NICKNAME))
        viewModel.onIntent(MyProfileIntent.InputName("가나다라마바사아자"))

        viewModel.onIntent(MyProfileIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(NameInputError.NICKNAME_TOO_LONG, state.nameError)
        assertEquals(MyProfileField.NICKNAME, state.editing)
        assertTrue(repository.profileChanges.isEmpty())

        viewModel.onIntent(MyProfileIntent.InputName("가나다라마바사아"))
        viewModel.onIntent(MyProfileIntent.ClickSave)
        advanceUntilIdle()

        assertEquals(listOf(MyProfileChange(nickname = "가나다라마바사아")), repository.profileChanges)
    }

    @Test
    fun `생년월일은 오늘 뒤로 고르면 오늘로 맞춘다`() = runTest(testDispatcher) {
        val today = DeviceTime.today()
        viewModel.onIntent(MyProfileIntent.Enter)
        advanceUntilIdle()
        viewModel.onIntent(MyProfileIntent.ClickField(MyProfileField.BIRTH))

        viewModel.onIntent(MyProfileIntent.SelectBirthYear(today.year))
        viewModel.onIntent(MyProfileIntent.SelectBirthMonth(12))
        viewModel.onIntent(MyProfileIntent.SelectBirthDay(31))

        val picked = LocalDate.of(today.year, 12, 31)
        assertEquals(if (picked.isAfter(today)) today else picked, state.draft.birth)
        assertTrue(!state.draft.birth.isAfter(today))
    }

    @Test
    fun `몸무게는 200kg까지 고를 수 있다`() {
        assertEquals(200, UserProfileRange.WEIGHT_KG.last)
    }

    // endregion

    // region 회원 없음

    @Test
    fun `회원이 없으면 다시 로그인 창을 띄우고 확인하면 로그아웃해 처음 화면으로 간다`() = runTest(testDispatcher) {
        repository.getMealTimesError = httpException(404, "api.user.notFound")

        viewModel.onIntent(MyProfileIntent.Enter)
        advanceUntilIdle()

        assertTrue(!state.isLoaded)
        val click = messageHelper.oneButtonDialogClicks.single()
        assertEquals(0, navigationHelper.initialCount)
        click?.invoke()
        advanceUntilIdle()
        assertEquals(1, navigationHelper.initialCount)
    }

    // endregion
}

private fun httpException(code: Int, errorCode: String): HttpResponseException = HttpResponseException(
    status = HttpResponseStatus.create(code),
    rawCode = code,
    errorRequestUrl = "https://test/api/v1/users/me",
    msg = "Http Request Failed ($code)",
    cause = Throwable(errorCode),
)
