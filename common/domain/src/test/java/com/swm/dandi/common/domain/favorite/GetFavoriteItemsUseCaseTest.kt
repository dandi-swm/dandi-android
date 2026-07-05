package com.swm.dandi.common.domain.favorite

import com.swm.dandi.common.entity.favorite.FavoriteItemVO
import com.swm.dandi.common.entity.media.MediaType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetFavoriteItemsUseCaseTest {

    private lateinit var favoriteRepository: FavoriteRepository
    private lateinit var useCase: GetFavoriteItemsUseCase

    @Before
    fun setUp() {
        favoriteRepository = mockk(relaxed = true)
        useCase = GetFavoriteItemsUseCase(
            favoriteRepository,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
    }

    @Test
    fun `invoke - emits the list from repository`() = runTest {
        val items = listOf(
            FavoriteItemVO(type = MediaType.IMAGE, title = "title1", urlKey = "url1"),
            FavoriteItemVO(type = MediaType.VIDEO, title = "title2", urlKey = "url2"),
        )
        coEvery { favoriteRepository.getFavoriteItemsFlow() } returns flowOf(items)

        val result = useCase().toList()

        assertEquals(1, result.size)
        assertEquals(items, result.first())
    }
}
