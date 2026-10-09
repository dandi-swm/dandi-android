package com.dandi.nyummy.settings.domain

import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

/** 배경음 켜기, 끄기. 기기 저장소 쓰기가 실패하면 화면이 되돌린다. */
class SetBgmEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> = try {
        Result.success(repository.setBgmEnabled(enabled))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
