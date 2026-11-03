# Remote Config 코인 충전 이벤트 (가짜 결제 A/B 테스트)

결제를 붙이기 전에, 가격을 본 사람 중 돈을 내고 코인을 사려는 사람이 얼마나 되는지 두 가격대(A/B)로 잰다.
홈 공지 "코인 충전하기" → 코인 충전 화면 → 묶음 → 충전 확인 시트 → "n원 결제하기"를 누르면 **실제 결제 없이** 이번 한 번만
이벤트 코인(그룹·묶음과 상관없이 3,000)을 넣어 주고 "돈은 나가지 않았어요"라고 알린다.

화면은 피그마 `v2 · 40 Shop`의 섹션 `Shop · 60 코인 충전 이벤트`(`Charge · 01~15`, 명세 메모)를 본다.
구현: `RemoteConfigKey.CoinChargeEvent`, `shop/data/.../dto/CoinChargeEventDTO.kt`, `CoinChargeRepositoryImpl`,
`CoinChargeUseCases.kt`, `CoinChargeTracker`, `ShopCoinChargePage`·`ShopCoinChargeViewModel`, 홈 `HomeViewModel.showNoticeIfNeeded`.

## 키

| 빌드 | 읽는 키 | 타입 |
|---|---|---|
| release | `android_coin_charge_event` | 문자열(JSON) |
| debug, benchmark | `android_coin_charge_event_debug` | 문자열(JSON) |

- 키가 없거나, 비었거나, JSON이 깨졌거나, 필수 값(`eventId`, `group`, `grantCoins`, 맞는 묶음 하나 이상)이 빠졌으면 이벤트가 없다.
  홈 공지도 뜨지 않고, 충전 링크로 들어오면 "충전 이벤트가 끝났어요"(`Charge · 15`)가 보인다.
- 앱은 인트로가 앱 시작 때 받아 둔 값을 읽는다. release는 받아 오기 주기가 1시간이라, 실험을 켜거나 바꾸면 앱을 다시 열 때(1시간이 지난 뒤) 반영된다.

## JSON 형식

A 그룹:

```json
{
  "eventId": "2026-11-coin-charge",
  "group": "A",
  "grantCoins": 3000,
  "notice": {
    "badge": "상점 오픈",
    "imageUrl": "https://cdn.nyummy.co.kr/notices/2026-11-shop-open.webp",
    "title": "냐미 상점이 문을 열었어요!",
    "body": "코인을 충전하고 냐미 방을 꾸밀\n가구와 배경을 골라 보세요.",
    "cta": "코인 충전하기"
  },
  "packages": [
    { "id": "coin_300", "coins": 300, "price": 1000 },
    { "id": "coin_1500", "coins": 1500, "price": 5000 },
    { "id": "coin_3000", "coins": 3000, "price": 10000 }
  ]
}
```

B 그룹은 `"group": "B"`이고 `price`만 1500 / 7500 / 15000이다. 나머지(eventId, grantCoins, 공지, 묶음 id·코인 수·순서)는 A와 똑같이 둔다 —
가격 말고 다른 차이가 생기면 비교가 흐려진다.

| 필드 | 필수 | 설명 |
|---|---|---|
| `eventId` | O | 회차 구분값. 분석 이벤트의 `event_id`, "다시 보지 않기", 받음 기록이 이 값으로 회차를 나눈다. 다음 회차는 새 값으로 바꾼다. |
| `group` | O | A/B 그룹 이름. 모든 분석 이벤트의 `ab_group`과 사용자 속성 `coin_charge_group`에 그대로 남는다. |
| `grantCoins` | O | "결제하기"를 누르면 넣어 주는 코인 수(1 이상). |
| `notice` | - | 홈 공지. 없거나 `title`·`cta`가 비었으면 공지 없이 충전 화면만 있다. 다른 홈 공지가 있으면 맨 앞 장에 뜬다. |
| `notice.imageUrl` | - | 10:9 사진(홈 새 소식 공지와 같은 규격, [remote-config-home-notice.md](remote-config-home-notice.md)). 인트로·방 준비 화면이 미리 받는다. |
| `packages[].id` | O | 묶음 구분값. 분석 이벤트의 `package_id`다. |
| `packages[].coins`, `price` | O | 화면에 보일 코인 수와 원 단위 가격(1 이상). 형식이 틀린 묶음은 그 묶음만 빠진다. |
| `packages[].imageId` | - | 묶음 그림 CDN 번호(`coins/{imageId}.png`, 1 동전 · 2 주머니 · 3 바구니). 없으면 순서대로 1, 2, 3. |

## A/B 실험 만들기(Firebase 콘솔)

1. **A/B Testing → 실험 만들기 → Remote Config.**
2. **타겟팅:** 앱 `com.dandi.nyummy`, 이 기능이 들어간 versionCode 이상, 노출 비율은 그룹당 목표 인원(각 100명)에 맞게 정한다.
   실험은 인원이 아니라 비율로 나누므로, 결과 화면의 참여자 수를 보며 목표에 닿으면 멈춘다.
3. **활성화 이벤트:** `coin_charge_notice_view`. 홈에서 이벤트 공지를 실제로 본 사람만 실험 결과에 넣는다.
4. **목표:** 주 측정항목은 `coin_charge_pay_click`. 함께 볼 측정항목으로 `coin_charge_view`, `coin_charge_package_click`, 유지율을 더한다.
5. **변형:** 매개변수 `android_coin_charge_event`에 기준(Baseline) = A 그룹 JSON, 변형 B = B 그룹 JSON을 넣는다. 실험에 들지 않은 사람은
   Remote Config 기본값(비어 있음)을 받아 이벤트가 없다.
6. **시작 전 확인:** 지급 API 연결(아래 "남은 일")과 상점 플래그(`android_shop_enabled`, 받은 뒤 "상점 둘러보기")를 켰는지 본다.

개발 빌드는 분석 수집이 꺼져 있어(매니페스트 `firebase_analytics_collection_enabled`) 실험 결과에 잡히지 않는다. 화면 확인은 `_debug` 키
**기본값**에 A 또는 B JSON을 직접 넣고 한다(데이터 유형 JSON, 운영 키 기본값은 `{}`). release 빌드로 특정 그룹을 확인하려면 실험의 테스트 기기(Firebase 설치 ID)를 쓴다.

분석 이벤트를 개발 빌드에서 GA4 DebugView로 보려면 빌드 옵션으로 수집을 켜고 디버그 모드를 켠다. 이 이벤트도 운영과 같은 GA4 속성에 쌓이므로
관리 → 데이터 필터의 "개발자 트래픽" 필터를 활성으로 둔다.

```bash
./gradlew :app:assembleDebug -Pnyummy.analyticsInDebug=true
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell setprop debug.firebase.analytics.app com.dandi.nyummy   # 끌 때는 .none.
```

## 측정(GA4)

모든 단계 이벤트에 `event_id`, `ab_group`이 붙는다. 이벤트 이름과 매개변수는 `AnalyticsEvent`(common/domain)에서만 정한다.

| 단계 | 이벤트 | 추가 매개변수 |
|---|---|---|
| 홈 공지가 떴다 | `coin_charge_notice_view` | |
| 공지 "코인 충전하기" | `coin_charge_notice_click` | |
| 공지를 버튼 없이 닫음 | `coin_charge_notice_close` | `dont_show_again` |
| 충전 화면에 들어옴 | `coin_charge_view` | `source`(notice·link), `claimed` |
| 묶음 누름(시트) | `coin_charge_package_click` | `package_id`, 숫자 `price`·`coins` |
| 시트를 결제 없이 닫음 | `coin_charge_sheet_close` | `package_id` |
| **"n원 결제하기" 누름** | **`coin_charge_pay_click`** | `package_id`, 숫자 `price`·`coins` |
| 이벤트 코인 받음 | `coin_charge_granted` | `package_id`, 숫자 `coins` |
| 받지 못함 | `coin_charge_grant_failed` | `package_id`, `reason`(already_claimed·server_error·network_or_unknown) |
| 받은 사람이 다시 누름 | `coin_charge_repeat_click` | `package_id`, 숫자 `price` |
| 받은 뒤 행동 | `coin_charge_result_action` | `action`(shop·home·back) |
| 충전 화면을 떠남 | `coin_charge_exit` | `stage`(list·sheet·result) |

- **그룹 알아보기:** 이벤트를 읽을 때마다(인트로, 홈, 충전 화면) 사용자 속성 `coin_charge_group`에 그룹을 남긴다. 실험이 끝난 뒤에도
  이 속성으로 식사 기록·구매 같은 다른 이벤트를 그룹별로 나눠 볼 수 있다. Firebase A/B Testing도 참여자에게 자체 사용자 속성(`firebase_exp_*`)을 붙인다.
- **GA4 맞춤 정의 등록(관리 → 맞춤 정의):** 이벤트 범위 측정기준 `event_id`, `ab_group`, `package_id`, `source`, `claimed`, `reason`,
  `action`, `stage`, `dont_show_again`, 사용자 범위 측정기준 `coin_charge_group`, 맞춤 측정항목 `price`, `coins`. 등록하지 않으면 보고서와 탐색에서 보이지 않는다.
- **핵심 지표:** 그룹별로 `coin_charge_view` 사용자 중 `coin_charge_pay_click` 사용자 비율. 함께 볼 것은 고른 묶음(`package_id`) 분포와
  퍼널 탐색(notice_view → notice_click → view → package_click → pay_click, 분류 `coin_charge_group`).

## 동작 요약

- 공지는 기존 새 소식 공지 다이얼로그를 그대로 쓴다. "다시 보지 않기"는 `eventId` 단위로 숨긴다. 이미 코인을 받은 기기에는 띄우지 않는다.
- 충전 화면 경로는 `/shop/coins/charge`(앱 링크 `https://link.nyummy.co.kr/shop/coins/charge`). 공지 버튼은 `?source=notice`를 붙여 연다.
  링크로 바로 열면 뒤로 가기가 홈이다.
- 받음 기록은 기기(app_prefs DataStore, `coin_charge_claimed_event_ids`)에 남는다. 계정이 아니라 기기 단위다. 두 번째 지급은 서버가 막는다.
- 받은 사람이 다시 들어오면 묶음은 그대로 보이고, 누르면 시트 없이 "결제는 아직 준비 중이에요" 안내만 뜬다.

## 남은 일

- [ ] **지급 API 연결(백엔드):** 지금은 `PendingCoinChargeGrantDataSource`가 대신한다. 개발 빌드는 받은 것처럼 응답하고(실제 코인은 늘지 않음),
  release 빌드는 실패해 "코인을 넣지 못했어요" 안내를 띄운다. API가 생기면 ApiService 구현으로 바꾸고, 응답 필드(`CoinChargeGrantDTO`)와
  이미 받음 code(`ShopErrorType.COIN_CHARGE_ALREADY_CLAIMED`, 지금은 가짜 값)를 맞춘다. **연결 전에는 release 실험을 시작하지 않는다.**
- [ ] 코인 내역의 새 종류 "코인 충전 이벤트"(서버).
- [ ] 공지 사진 `notices/2026-11-shop-open.webp` 올리기(피그마 `Charge · 01`의 사진은 상점 예고 2안을 임시로 썼다).
