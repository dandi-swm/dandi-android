package com.dandi.nyummy.cat.data

import com.dandi.nyummy.cat.domain.CatRepository
import com.dandi.nyummy.cat.entity.CatAnimationSetVO
import com.dandi.nyummy.common.data.token.TokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlin.coroutines.cancellation.CancellationException

/**
 * 미리 받은 애니메이션은 조회 한 번만 넘겨준다. 그 뒤 조회는 매번 새로 받아, 체형이 바뀌거나 다른 계정으로 로그인해도 지난 값을 쓰지 않는다.
 * 미리 받기는 [ioScope]에서 돌려, 기다리던 화면(인트로, 방 준비)이 먼저 닫혀도 끊기지 않는다.
 */
class CatRepositoryImpl(
    private val dataSource: CatDataSource,
    private val tokenProvider: TokenProvider,
    private val ioScope: CoroutineScope,
) : CatRepository {

    /**
     * 미리 받는 중이거나 받아 둔 결과. 로그인 전이라 받지 않았으면 결과가 null이다. 조회 한 번이 가져가면 비운다.
     * 화면(ViewModel)이 메인 스레드에서 차례로 읽고 쓰고, 받는 코루틴은 이 값을 건드리지 않아 잠그지 않는다.
     * 엇갈리더라도 한 번 더 받을 뿐이다.
     */
    private var prefetched: Deferred<CatAnimationSetVO?>? = null

    override suspend fun getAnimations(): CatAnimationSetVO {
        val pending = prefetched ?: return fetch()
        prefetched = null
        return try {
            pending.await() ?: fetch()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 미리 받기가 실패했으면 지금 다시 받는다. 다시 받다가 실패하면 그 실패를 부른 쪽이 처리한다.
            fetch()
        }
    }

    override suspend fun prefetchAnimations(): CatAnimationSetVO? {
        // 받는 중인 요청만 같이 기다린다. 이미 끝난 미리 받기(로그인 전에 인트로가 부른 것 등)는 새로 받는다.
        val pending = prefetched?.takeIf { it.isActive } ?: ioScope.async {
            // 토큰 저장소는 첫 읽기 때 디스크를 막고 읽으므로 IO 스코프 안에서 확인한다.
            if (tokenProvider.refreshToken.isNullOrBlank()) null else fetch()
        }.also { prefetched = it }
        return try {
            pending.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetch(): CatAnimationSetVO = dataSource.getAnimations().toVO()
}
