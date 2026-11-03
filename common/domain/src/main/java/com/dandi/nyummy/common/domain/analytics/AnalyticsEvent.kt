package com.dandi.nyummy.common.domain.analytics

/**
 * 분석 이벤트 하나. 이름과 매개변수를 이 파일에서만 정해 화면마다 철자가 달라지지 않게 한다.
 *
 * GA4 규칙: 이름과 매개변수 키는 영문 소문자와 밑줄로 40자 안, 값은 100자 안이다. 개인 정보(이메일, 닉네임 등)는 넣지 않는다.
 * 로그인과 회원가입은 GA4 권장 이벤트 이름(login, sign_up, method)을 그대로 써서 기본 보고서에 잡히게 한다.
 *
 * 단계 이벤트(가입 단계, 온보딩 장면)는 그 단계에 도착할 때 남긴다. GA4 퍼널 탐색에서 단계를 차례로 놓으면
 * 어느 단계에서 다음 단계로 못 넘어갔는지(이탈)가 보인다.
 */
sealed class AnalyticsEvent(
    val name: String,
    val params: Map<String, String> = emptyMap(),
    /** 숫자 매개변수. GA4 맞춤 측정항목(평균, 합계)으로 쓰려면 숫자로 보내야 한다. */
    val numberParams: Map<String, Long> = emptyMap(),
) {

    /** 로그인 성공. [method]는 email, kakao, google 같은 로그인 수단이다. 소셜 신규 회원은 가입이 끝날 때 [SignUp]으로 남는다. */
    class Login(method: String) : AnalyticsEvent("login", mapOf(PARAM_METHOD to method))

    /**
     * 회원가입 단계에 도착했다. [step]은 account(이메일과 비밀번호), code(이메일 인증 코드), profile(닉네임과 신체 정보)이고,
     * [method]는 email 또는 social이다. 소셜 가입은 profile부터 시작한다.
     */
    class SignUpStep(step: String, method: String) : AnalyticsEvent(
        "sign_up_step",
        mapOf(PARAM_STEP to step, PARAM_METHOD to method),
    )

    /** 회원가입 성공. [method]는 email 또는 social이다. */
    class SignUp(method: String) : AnalyticsEvent("sign_up", mapOf(PARAM_METHOD to method))

    /**
     * 온보딩 장면에 도착했다. [scene]은 몇 번째 장면인지(1부터), [kind]는 장면 종류(story, choice, name_input, start, meal_time)다.
     * 장면 번호는 온보딩 대본 순서라, 대본을 바꾸면 번호가 가리키는 장면도 바뀐다.
     */
    class OnboardingStep(scene: Int, kind: String) : AnalyticsEvent(
        "onboarding_step",
        mapOf("scene" to scene.toString(), "kind" to kind),
    )

    /** 온보딩을 마치고 고양이를 만들었다. */
    data object OnboardingComplete : AnalyticsEvent("onboarding_complete")

    /** 식사 기록 화면(카메라)에 들어왔다. 기록 퍼널의 시작이다. */
    data object MealRecordOpen : AnalyticsEvent("meal_record_open")

    /** 기록할 식사 사진이 준비됐다(촬영했거나 갤러리에서 가져왔다). [source]는 camera 또는 gallery. */
    class MealPhotoReady(source: String) : AnalyticsEvent("meal_photo_ready", mapOf(PARAM_SOURCE to source))

    /** 확인 단계에서 다시 찍기를 눌렀다. */
    class MealRetake(source: String) : AnalyticsEvent("meal_retake", mapOf(PARAM_SOURCE to source))

    /** 사진을 냈다(업로드 시작). */
    class MealSubmit(source: String) : AnalyticsEvent("meal_submit", mapOf(PARAM_SOURCE to source))

    /** 서버가 기록을 받았다(저장 완료). [durationSec]는 식사 기록 화면에 들어와서 저장될 때까지 걸린 초다. */
    class MealSubmitSuccess(source: String, durationSec: Long) : AnalyticsEvent(
        "meal_submit_success",
        params = mapOf(PARAM_SOURCE to source),
        numberParams = mapOf("duration_sec" to durationSec),
    )

    /**
     * 기록이 막혔다. [stage]는 gallery(갤러리 사진 가져오기) 또는 submit(제출)이고, [reason]은 막힌 사유다.
     * gallery: gallery_not_today, gallery_load_failed, gallery_import_failed
     * submit: daily_limit, stale_image, duplicate_image, unsupported_type, file_too_large, invalid_photo, server_error, network_or_unknown
     */
    class MealRecordBlocked(stage: String, reason: String) : AnalyticsEvent(
        "meal_record_blocked",
        mapOf(PARAM_STAGE to stage, "reason" to reason),
    )

    /** 저장하지 않고 식사 기록 화면을 떠났다. [stage]는 떠난 단계(camera, confirm, uploading, failed)다. */
    class MealRecordExit(stage: String) : AnalyticsEvent("meal_record_exit", mapOf(PARAM_STAGE to stage))

    // ---- 코인 충전 이벤트(가짜 결제 A/B 테스트, docs/remote-config-coin-charge-event.md) ----
    // 모든 단계에 이벤트 구분값(event_id)과 A/B 그룹(ab_group)을 붙인다. 그룹은 사용자 속성 coin_charge_group으로도 남는다.
    // 퍼널: notice_view → notice_click → view → package_click → pay_click → granted. 핵심 지표는 view 대비 pay_click 비율이다.

    /** 홈에서 코인 충전 이벤트 공지가 떴다. */
    class CoinChargeNoticeView(eventId: String, group: String) : AnalyticsEvent(
        "coin_charge_notice_view",
        coinCharge(eventId, group),
    )

    /** 이벤트 공지의 "코인 충전하기"를 눌렀다. */
    class CoinChargeNoticeClick(eventId: String, group: String) : AnalyticsEvent(
        "coin_charge_notice_click",
        coinCharge(eventId, group),
    )

    /** 이벤트 공지를 버튼을 누르지 않고 닫았다(닫기, 바깥 누름, 뒤로 가기, 다른 장의 버튼). [dontShowAgain]은 "다시 보지 않기"를 체크했는지. */
    class CoinChargeNoticeClose(eventId: String, group: String, dontShowAgain: Boolean) : AnalyticsEvent(
        "coin_charge_notice_close",
        coinCharge(eventId, group) + mapOf("dont_show_again" to dontShowAgain.toString()),
    )

    /**
     * 코인 충전 화면이 보였다(화면에 들어올 때 한 번). [source]는 notice(홈 공지 버튼) 또는 link(그 밖의 링크)이고,
     * [claimed]는 이미 이벤트 코인을 받은 사람인지다.
     */
    class CoinChargeView(eventId: String, group: String, source: String, claimed: Boolean) : AnalyticsEvent(
        "coin_charge_view",
        coinCharge(eventId, group) + mapOf(PARAM_SOURCE to source, "claimed" to claimed.toString()),
    )

    /** 묶음 하나를 눌렀다(충전 확인 시트가 뜬다). [packageId]는 묶음 번호(1~3), [price]는 원, [coins]는 묶음의 코인 수다. */
    class CoinChargePackageClick(eventId: String, group: String, packageId: String, price: Int, coins: Int) : AnalyticsEvent(
        "coin_charge_package_click",
        coinCharge(eventId, group) + mapOf(PARAM_PACKAGE_ID to packageId),
        mapOf(PARAM_PRICE to price.toLong(), PARAM_COINS to coins.toLong()),
    )

    /** 충전 확인 시트를 결제하지 않고 닫았다(취소, 바깥 누름, 뒤로 가기). */
    class CoinChargeSheetClose(eventId: String, group: String, packageId: String) : AnalyticsEvent(
        "coin_charge_sheet_close",
        coinCharge(eventId, group) + mapOf(PARAM_PACKAGE_ID to packageId),
    )

    /** 시트의 "n원 결제하기"를 눌렀다. 이 실험의 핵심 지표(돈을 내고 사려는 사람)다. 지급 성공 여부와 상관없이 누르면 남는다. */
    class CoinChargePayClick(eventId: String, group: String, packageId: String, price: Int, coins: Int) : AnalyticsEvent(
        "coin_charge_pay_click",
        coinCharge(eventId, group) + mapOf(PARAM_PACKAGE_ID to packageId),
        mapOf(PARAM_PRICE to price.toLong(), PARAM_COINS to coins.toLong()),
    )

    /** 선물 코인을 받았다(결제 대신 무료 지급). [coins]는 서버가 넣어 준 코인 수다(그룹·묶음과 상관없이 같다). */
    class CoinChargeGranted(eventId: String, group: String, packageId: String, coins: Int) : AnalyticsEvent(
        "coin_charge_granted",
        coinCharge(eventId, group) + mapOf(PARAM_PACKAGE_ID to packageId),
        mapOf(PARAM_COINS to coins.toLong()),
    )

    /**
     * 선물 코인을 받지 못했다. [reason]은 already_claimed(기기에 받음 기록, 서버에 묻지 않음), package_not_found(404),
     * invalid_request(그 밖의 4xx), server_error(로그인 만료·5xx), network_or_unknown이다.
     */
    class CoinChargeGrantFailed(eventId: String, group: String, packageId: String, reason: String) : AnalyticsEvent(
        "coin_charge_grant_failed",
        coinCharge(eventId, group) + mapOf(PARAM_PACKAGE_ID to packageId, "reason" to reason),
    )

    /** 이미 받은 사람이 묶음을 다시 눌렀다(시트 없이 안내만 뜬다). 더 사고 싶은 사람을 센다. */
    class CoinChargeRepeatClick(eventId: String, group: String, packageId: String, price: Int) : AnalyticsEvent(
        "coin_charge_repeat_click",
        coinCharge(eventId, group) + mapOf(PARAM_PACKAGE_ID to packageId),
        mapOf(PARAM_PRICE to price.toLong()),
    )

    /** 받은 뒤 화면에서 고른 행동. [action]은 shop(상점 둘러보기), home(홈으로), back(뒤로 가기)이다. */
    class CoinChargeResultAction(eventId: String, group: String, action: String) : AnalyticsEvent(
        "coin_charge_result_action",
        coinCharge(eventId, group) + mapOf("action" to action),
    )

    /** 코인 충전 화면을 떠났다. [stage]는 떠날 때 보던 곳(list 묶음 목록, sheet 확인 시트, result 받은 뒤 화면)이다. */
    class CoinChargeExit(eventId: String, group: String, stage: String) : AnalyticsEvent(
        "coin_charge_exit",
        coinCharge(eventId, group) + mapOf(PARAM_STAGE to stage),
    )

    private companion object {
        const val PARAM_METHOD = "method"
        const val PARAM_STEP = "step"
        const val PARAM_SOURCE = "source"
        const val PARAM_STAGE = "stage"
        const val PARAM_PACKAGE_ID = "package_id"
        const val PARAM_PRICE = "price"
        const val PARAM_COINS = "coins"

        /** 코인 충전 이벤트 단계마다 붙는 매개변수. */
        fun coinCharge(eventId: String, group: String): Map<String, String> = mapOf("event_id" to eventId, "ab_group" to group)
    }
}
