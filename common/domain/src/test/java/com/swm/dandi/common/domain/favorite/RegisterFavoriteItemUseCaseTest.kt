package com.swm.dandi.common.domain.favorite

import com.swm.dandi.common.domain.helper.MessageHelper
import com.swm.dandi.common.domain.helper.ResourceHelper
import com.swm.dandi.common.domain.helper.StringResource
import com.swm.dandi.common.entity.favorite.FavoriteItemVO
import com.swm.dandi.common.entity.media.MediaType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegisterFavoriteItemUseCaseTest {

    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var messageHelper: MessageHelper
    private lateinit var resourceHelper: ResourceHelper
    private lateinit var useCase: RegisterFavoriteItemUseCase

    @Before
    fun setUp() {
        favoriteRepository = mockk(relaxed = true)
        messageHelper = mockk(relaxed = true)
        resourceHelper = mockk(relaxed = true)
        useCase = RegisterFavoriteItemUseCase(
            favoriteRepository,
            resourceHelper,
            messageHelper,
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
    }

    @Test
    fun `invoke - on success - returns success and does not show message`() = runTest {
        val item = FavoriteItemVO(type = MediaType.IMAGE, title = "title", urlKey = "url")
        coEvery { favoriteRepository.createFavoriteItem(item) } returns Unit

        val result = useCase(item)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { favoriteRepository.createFavoriteItem(item) }
        verify(exactly = 0) { resourceHelper.getString(any()) }
    }

    @Test
    fun `invoke - on repository error - returns failure and shows message`() = runTest {
        val item = FavoriteItemVO(type = MediaType.IMAGE, title = "title", urlKey = "url")
        val expected = RuntimeException("create failed")
        coEvery { favoriteRepository.createFavoriteItem(item) } throws expected

        val result = useCase(item)

        assertTrue(result.isFailure)
        assertEquals(expected, result.exceptionOrNull())
        verify(exactly = 1) { resourceHelper.getString(StringResource.FAVORITE_REGISTER_FAILED) }
    }
}
