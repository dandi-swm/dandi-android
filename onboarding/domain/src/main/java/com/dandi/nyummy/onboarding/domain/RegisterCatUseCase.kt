package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.onboarding.entity.CatVO
import com.dandi.nyummy.tti.TTIHelper
import java.util.concurrent.CancellationException
import javax.inject.Inject

/** 고양이 이름 등록. 이름 검증은 화면에서 먼저 하고, 여기서는 등록과 이후 이동만 담당한다. */
class RegisterCatUseCase @Inject constructor(
    private val repository: OnboardingRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /**
     * 이름을 등록한다. 온보딩 완료 기록은 남은 장면(평소 식사 시각)까지 마친 뒤 [FinishOnboardingUseCase]가 한다.
     *
     * 이미 고양이가 있는 회원(409)은 등록된 것으로 보고 남은 장면을 이어간다. 이름 등록 뒤 식사 시각 장면에서
     * 앱이 꺼지면 다음 실행 때 온보딩을 처음부터 다시 하게 되는데, 그때 이름을 다시 지으면 409가 오기 때문이다.
     * 서버에 있는 이름은 알 수 없어 방금 입력한 이름으로 이어간다.
     * 그 밖의 실패는 안내 후 [Result.failure]로 돌려준다.
     */
    suspend operator fun invoke(name: String): Result<CatVO> = try {
        Result.success(repository.registerCat(name.trim()))
    } catch (e: HttpResponseException) {
        if (e.handlingErrorOnUseCase<OnboardingErrorType>() == OnboardingErrorType.CAT_ALREADY_EXISTS) {
            Result.success(CatVO(name = name.trim()))
        } else {
            handleError(e)
            Result.failure(e)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        messageHelper.showOneButtonDialog(descText = TEMPORARY_ERROR_MESSAGE)
        Result.failure(e)
    }

    private suspend fun handleError(e: HttpResponseException) {
        when (e.handlingErrorOnUseCase<OnboardingErrorType>()) {
            // invoke에서 먼저 이어가기로 처리한다.
            OnboardingErrorType.CAT_ALREADY_EXISTS -> return
            OnboardingErrorType.CAT_NAME_INVALID -> {
                messageHelper.showOneButtonDialog(descText = OnboardingErrorType.CAT_NAME_INVALID.errorMsg)
                return
            }
            null -> Unit
        }
        if (e.isCommonErrorHandling()) {
            executeCommonErrorHanding(e)
        } else {
            messageHelper.showOneButtonDialog(descText = TEMPORARY_ERROR_MESSAGE)
        }
    }

    private companion object {
        const val TEMPORARY_ERROR_MESSAGE = "일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요."
    }
}
