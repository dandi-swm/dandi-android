package com.dandi.nyummy.common.presentation.helper

import android.content.Context
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.presentation.R

class ResourceHelperImpl(private val context: Context) : ResourceHelper {
    override fun getString(resource: StringResource): String =
        context.getString(resource.toResId())

    override fun getString(resource: StringResource, vararg formatArgs: Any): String =
        context.getString(resource.toResId(), *formatArgs)

    private fun StringResource.toResId(): Int = when (this) {
        StringResource.APP_NAME -> R.string.app_name
        StringResource.NAV_BACK_BUTTON -> R.string.nav_back_button
        StringResource.MEAL_GALLERY_NOT_TAKEN_TODAY -> R.string.meal_gallery_not_taken_today
        StringResource.MEAL_GALLERY_LOAD_FAILED -> R.string.meal_gallery_load_failed
        StringResource.MEAL_GALLERY_IMPORT_FAILED -> R.string.meal_gallery_import_failed
        StringResource.CAT_DEFAULT_NAME -> R.string.nyummy_cat_default_name
        StringResource.CONFIRM -> R.string.msg_confirm
        StringResource.CANCEL -> R.string.msg_cancel
        StringResource.RETRY -> R.string.msg_retry
        StringResource.SIGN_IN_AGAIN -> R.string.msg_sign_in_again
        StringResource.GO_BACK -> R.string.msg_go_back
        StringResource.SESSION_EXPIRED_MESSAGE -> R.string.msg_session_expired_message
        StringResource.NOT_READY_MESSAGE -> R.string.msg_not_ready_message
        StringResource.TEMPORARY_ERROR_TITLE -> R.string.msg_temporary_error_title
        StringResource.TRY_AGAIN_LATER -> R.string.msg_try_again_later
        StringResource.NETWORK_ERROR -> R.string.msg_network_error
        StringResource.TEMPORARY_ERROR -> R.string.msg_temporary_error
        StringResource.DUPLICATE_MEAL_TIMES -> R.string.msg_duplicate_meal_times
        StringResource.AUTH_INVALID_CREDENTIALS -> R.string.msg_auth_invalid_credentials
        StringResource.AUTH_EMAIL_SEND_RATE_LIMITED -> R.string.msg_auth_email_send_rate_limited
        StringResource.AUTH_EMAIL_SEND_FAILED -> R.string.msg_auth_email_send_failed
        StringResource.AUTH_INCORRECT_EMAIL -> R.string.msg_auth_incorrect_email
        StringResource.AUTH_EMAIL_CODE_ATTEMPT_EXCEEDED -> R.string.msg_auth_email_code_attempt_exceeded
        StringResource.AUTH_EMAIL_CODE_MISMATCH -> R.string.msg_auth_email_code_mismatch
        StringResource.AUTH_EMAIL_CODE_EXPIRED -> R.string.msg_auth_email_code_expired
        StringResource.AUTH_VERIFICATION_RESTART -> R.string.msg_auth_verification_restart
        StringResource.AUTH_INVALID_EMAIL_CHALLENGE -> R.string.msg_auth_invalid_email_challenge
        StringResource.AUTH_EMAIL_ALREADY_EXISTS -> R.string.msg_auth_email_already_exists
        StringResource.AUTH_EMAIL_NOT_REGISTERED -> R.string.msg_auth_email_not_registered
        StringResource.AUTH_ACCOUNT_ALREADY_EXISTS -> R.string.msg_auth_account_already_exists
        StringResource.AUTH_SOCIAL_LOGIN_FAILED -> R.string.msg_auth_social_login_failed
        StringResource.AUTH_SOCIAL_SERVER_UNAVAILABLE -> R.string.msg_auth_social_server_unavailable
        StringResource.AUTH_CODE_CONFIRM_FAILED -> R.string.msg_auth_code_confirm_failed
        StringResource.AUTH_TEMP_PASSWORD_MAIL_FAILED -> R.string.msg_auth_temp_password_mail_failed
        StringResource.AUTH_PASSWORD_RESET_FAILED -> R.string.msg_auth_password_reset_failed
        StringResource.AUTH_SOCIAL_SESSION_MISSING -> R.string.msg_auth_social_session_missing
        StringResource.AUTH_SOCIAL_SIGN_UP_EXPIRED -> R.string.msg_auth_social_sign_up_expired
        StringResource.AUTH_INVALID_PROFILE -> R.string.msg_auth_invalid_profile
        StringResource.AUTH_SIGN_UP_FAILED -> R.string.msg_auth_sign_up_failed
        StringResource.AUTH_LOGIN_FAILED -> R.string.msg_auth_login_failed
        StringResource.AUTH_BAD_REQUEST -> R.string.msg_auth_bad_request
        StringResource.AUTH_PROVIDER_NOT_READY -> R.string.msg_auth_provider_not_ready
        StringResource.AUTH_PROVIDER_UNAVAILABLE -> R.string.msg_auth_provider_unavailable
        StringResource.AUTH_PROVIDER_FAILED -> R.string.msg_auth_provider_failed
        StringResource.AUTH_PROVIDER_KAKAO -> R.string.msg_auth_provider_kakao
        StringResource.AUTH_PROVIDER_GOOGLE -> R.string.msg_auth_provider_google
        StringResource.AUTH_PROVIDER_NAVER -> R.string.msg_auth_provider_naver
        StringResource.INTRO_FORCE_UPDATE -> R.string.msg_intro_force_update
        StringResource.INTRO_GO_TO_STORE -> R.string.msg_intro_go_to_store
        StringResource.INTRO_ERROR -> R.string.msg_intro_error
        StringResource.ONBOARDING_INVALID_INPUT -> R.string.msg_onboarding_invalid_input
        StringResource.ONBOARDING_CAT_ALREADY_EXISTS -> R.string.msg_onboarding_cat_already_exists
        StringResource.CAT_NAME_TOO_LONG -> R.string.msg_cat_name_too_long
        StringResource.CAT_INVALID_NAME -> R.string.msg_cat_invalid_name
        StringResource.CAT_NOT_FOUND -> R.string.msg_cat_not_found
        StringResource.CAT_RENAME_FAILED -> R.string.msg_cat_rename_failed
        StringResource.HOME_LOAD_FAILED -> R.string.msg_home_load_failed
        StringResource.MEAL_SUBMIT_FAILED -> R.string.msg_meal_submit_failed
        StringResource.MEAL_TUTORIAL_RETRY -> R.string.msg_meal_tutorial_retry
        StringResource.MEAL_STALE_IMAGE -> R.string.msg_meal_stale_image
        StringResource.MEAL_DUPLICATE_IMAGE -> R.string.msg_meal_duplicate_image
        StringResource.MEAL_DAILY_LIMIT -> R.string.msg_meal_daily_limit
        StringResource.MEAL_UNSUPPORTED_TYPE -> R.string.msg_meal_unsupported_type
        StringResource.MEAL_FILE_TOO_LARGE -> R.string.msg_meal_file_too_large
        StringResource.MEAL_TUTORIAL_DONE -> R.string.msg_meal_tutorial_done
        StringResource.MEAL_PHOTO_NOT_FOUND -> R.string.msg_meal_photo_not_found
        StringResource.MEAL_PHOTO_INFO_SAVE_FAILED -> R.string.msg_meal_photo_info_save_failed
        StringResource.MEAL_PHOTO_READ_FAILED -> R.string.msg_meal_photo_read_failed
        StringResource.MEAL_PHOTO_COMPRESS_FAILED -> R.string.msg_meal_photo_compress_failed
        StringResource.HISTORY_MEAL_ALREADY_DELETED -> R.string.msg_history_meal_already_deleted
        StringResource.HISTORY_ALREADY_REANALYZED -> R.string.msg_history_already_reanalyzed
        StringResource.HISTORY_INVALID_RATING -> R.string.msg_history_invalid_rating
        StringResource.HISTORY_ALREADY_RATED -> R.string.msg_history_already_rated
        StringResource.HISTORY_RATING_NOT_READY -> R.string.msg_history_rating_not_ready
        StringResource.HISTORY_FEEDBACK_SUBMIT_FAILED -> R.string.msg_history_feedback_submit_failed
        StringResource.HISTORY_FEEDBACK_THANKS -> R.string.msg_history_feedback_thanks
        StringResource.HISTORY_REANALYZE_FAILED -> R.string.msg_history_reanalyze_failed
        StringResource.HISTORY_RENAME_FAILED -> R.string.msg_history_rename_failed
        StringResource.HISTORY_DELETE_FAILED -> R.string.msg_history_delete_failed
        StringResource.SHOP_ADDED_TO_CART -> R.string.msg_shop_added_to_cart
        StringResource.SHOP_CANNOT_ADD_MORE -> R.string.msg_shop_cannot_add_more
        StringResource.SHOP_CART_FULL -> R.string.msg_shop_cart_full
        StringResource.SHOP_ADD_FAILED -> R.string.msg_shop_add_failed
        StringResource.SHOP_COIN_CHARGE_FAILED -> R.string.msg_shop_coin_charge_failed
        StringResource.SHOP_PRICE_CHANGED_TITLE -> R.string.msg_shop_price_changed_title
        StringResource.SHOP_PRICE_CHANGED_BODY -> R.string.msg_shop_price_changed_body
        StringResource.SHOP_NOT_FOR_SALE_TITLE -> R.string.msg_shop_not_for_sale_title
        StringResource.SHOP_NOT_FOR_SALE_BODY -> R.string.msg_shop_not_for_sale_body
        StringResource.SHOP_ALREADY_OWNED_TITLE -> R.string.msg_shop_already_owned_title
        StringResource.SHOP_ALREADY_OWNED_BODY -> R.string.msg_shop_already_owned_body
        StringResource.SHOP_PURCHASE_LIMIT_TITLE -> R.string.msg_shop_purchase_limit_title
        StringResource.SHOP_PURCHASE_LIMIT_BODY -> R.string.msg_shop_purchase_limit_body
        StringResource.SHOP_RELOAD_CART -> R.string.msg_shop_reload_cart
        StringResource.SETTINGS_USER_NOT_FOUND -> R.string.msg_settings_user_not_found
        StringResource.SETTINGS_PROFILE_INVALID_INPUT -> R.string.msg_settings_profile_invalid_input
        StringResource.SETTINGS_WITHDRAWN -> R.string.msg_settings_withdrawn
        StringResource.SETTINGS_WITHDRAW_FAILED -> R.string.msg_settings_withdraw_failed
        StringResource.SETTINGS_PROFILE_SAVE_FAILED -> R.string.msg_settings_profile_save_failed
        StringResource.SETTINGS_PROFILE_LOAD_FAILED -> R.string.msg_settings_profile_load_failed
        StringResource.SETTINGS_MEAL_TIMES_SAVE_FAILED -> R.string.msg_settings_meal_times_save_failed
        StringResource.SETTINGS_MEAL_TIMES_LOAD_FAILED -> R.string.msg_settings_meal_times_load_failed
        StringResource.SETTINGS_LOAD_FAILED -> R.string.msg_settings_load_failed
    }
}
