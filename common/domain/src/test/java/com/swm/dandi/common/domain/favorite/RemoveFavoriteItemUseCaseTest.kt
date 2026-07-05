package com.swm.dandi.common.domain.favorite

import com.swm.dandi.common.domain.helper.MessageHelper
import com.swm.dandi.common.domain.helper.ResourceHelper
import com.swm.dandi.common.domain.helper.StringResource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoveFavoriteItemUseCaseTest {

    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var messageHelper: MessageHelper
    private lateinit var resourceHelper: ResourceHelper
    private lateinit var useCase: RemoveFavoriteItemUseCase

    @Before
    fun setUp() {
        favoriteRepository = mockk(relaxed = true)
        messageHelper = mockk(relaxed = true)
        resourceHelper = mockk(relaxed = true)
        useCase = RemoveFavoriteItemUseCase(
            favoriteRepository,
            resourceHelper,
            messageHelper,
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
    }

    @Test
    fun `invoke - on success - returns success and does not show message`() = runTest {
        val url = "url"
        coEvery { favoriteRepository.deleteFavoriteItem(url) } returns Unit

        val result = useCase(url)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { favoriteRepository.deleteFavoriteItem(url) }
        verify(exactly = 0) { resourceHelper.getString(any()) }
    }

    @Test
    fun `invoke - on repository error - returns failure and shows message`() = runTest {
        val url = "url"
        val expected = RuntimeException("delete failed")
        coEvery { favoriteRepository.deleteFavoriteItem(url) } throws expected

        val result = useCase(url)

        assertTrue(result.isFailure)
        assertEquals(expected, result.exceptionOrNull())
        verify(exactly = 1) { resourceHelper.getString(StringResource.FAVORITE_REMOVE_FAILED) }
    }
}
