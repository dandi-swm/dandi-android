package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.home.domain.HomePage
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
     * 이름을 등록한다. 이미 고양이가 있는 회원(409)은 온보딩을 끝낸 것으로 보고 홈으로 보낸다.
     * 실패는 안내 후 [Result.failure] 로 돌려준다.
     */
    suspend operator fun invoke(name: String): Result<CatVO> = try {
        Result.success(repository.registerCat(name.trim()))
    } catch (e: HttpResponseException) {
        handleError(e)
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        messageHelper.showOneButtonDialog(descText = TEMPORARY_ERROR_MESSAGE)
        Result.failure(e)
    }

    private fun handleError(e: HttpResponseException) {
        when (e.handlingErrorOnUseCase<OnboardingErrorType>()) {
            OnboardingErrorType.CAT_ALREADY_EXISTS -> {
                navigationHelper.navigateToAsRoot(HomePage)
                return
            }
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
