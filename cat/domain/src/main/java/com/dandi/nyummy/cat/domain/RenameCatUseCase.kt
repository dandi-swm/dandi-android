package com.dandi.nyummy.cat.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.entity.cat.CatNameValidator
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 고양이 이름을 바꾼다. 앞뒤 공백을 뺀 이름을 서버에 저장하고, 이 기기에 기억한 이름도 바꾼다.
 * 앱 화면 문구의 이름 자리는 기기에 기억한 이름을 쓰므로 저장하자마자 모든 화면에 새 이름이 보인다.
 *
 * 이름 규칙([CatNameValidator])에 맞지 않으면 서버에 보내지 않고 [Result.failure]를 돌려준다. 안내는 화면이 시트 안에서 먼저 한다.
 * 저장에 실패하면 안내하고 [Result.failure]를 돌려준다. 서버가 이름을 거절한 경우([CatErrorType])는 안내만 하고,
 * 그 밖의 실패는 다시 시도를 [onRetry]로 화면에 맡긴다.
 */
class RenameCatUseCase @Inject constructor(
    private val repository: CatRepository,
    private val companionMemory: CompanionMemoryHelper,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(name: String, onRetry: () -> Unit): Result<String> {
        val trimmed = name.trim()
        CatNameValidator.validate(trimmed)?.let { error ->
            return Result.failure(IllegalArgumentException("고양이 이름 규칙 위반: $error"))
        }
        return try {
            repository.rename(trimmed)
            companionMemory.setCatName(trimmed)
            Result.success(trimmed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpResponseException) {
            handleHttpError<CatErrorType>(
                e,
                onDomainError = { messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = resourceHelper.getString(it.errorMsg, CatNameValidator.MAX_LENGTH)) },
                onUnknownError = { showRenameFailed(onRetry) },
            )
            Result.failure(e)
        } catch (e: Exception) {
            // 네트워크 끊김 등. 던지면 화면 코루틴이 앱을 죽인다.
            showRenameFailed(onRetry)
            Result.failure(e)
        }
    }

    private fun showRenameFailed(onRetry: () -> Unit) {
        messageHelper.showSnackBar(
            iconType = IconType.ERROR,
            messageText = resourceHelper.getString(StringResource.CAT_RENAME_FAILED),
            callToActionText = resourceHelper.getString(StringResource.RETRY),
            onClickCTA = onRetry,
        )
    }
}
