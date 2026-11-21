package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.cat.entity.CatLineVO
import com.dandi.nyummy.common.domain.error.runSuspendCatching
import javax.inject.Inject

/** 냐미 대사 은행을 읽는다. 읽지 못하면 빈 목록이라 화면은 기본 대사를 쓴다. */
class GetCatLinesUseCase @Inject constructor(
    private val repository: CatLineRepository,
) {
    suspend operator fun invoke(): List<CatLineVO> = runSuspendCatching { repository.getLines() }.getOrDefault(emptyList())
}
