# Remote Config 상점 추천 탭 배치 A/B

상점 추천 탭(서버 SDUI)의 칸 순서를 두 그룹으로 나눠, 어느 배치에서 더 많이 담고 사는지 잰다.
코인 충전 이벤트([remote-config-coin-charge-event.md](remote-config-coin-charge-event.md))와는 **다른 실험**으로 따로 나눈다 —
가격 A/B와 배치 A/B의 효과를 따로 읽기 위해서다. Firebase A/B Testing은 실험마다 따로 무작위로 나누므로 한 사람이 두 실험에 함께 들 수 있다.

구현: `RemoteConfigKey.ShopLayoutVariant`, `GetShopRecommendPageUseCase`, `ShopApiService.getRecommendPage`(헤더), `ShopTracker`,
`ShopRecommendViewModel`, `AddToCartUseCase`·`PurchaseUseCase`.

## 키와 서버 규격

| 빌드 | 읽는 키 | 타입 | 값 |
|---|---|---|---|
| release | `android_shop_layout_variant` | 문자열 | `A` 또는 `B` |
| debug, benchmark | `android_shop_layout_variant_debug` | 문자열 | `A` 또는 `B` |

- 앱은 추천 탭을 읽을 때(`GET /api/v1/shop/pages/recommend`) 값을 `X-Shop-Layout-Variant` 헤더로 보낸다. 비었거나 모르는 값이면 `A`로 보낸다(서버도 헤더가 없거나 모르는 값이면 A).
- 서버 배치(dev Swagger 기준): A는 배너 → 오늘의 할인 → 인기, B는 배너 → 인기 → 오늘의 할인.
- 콘솔에 있는 `store_sdui_layout`(값 `default`)은 앱이 읽지 않는다.

## A/B 실험 만들기(Firebase 콘솔)

1. A/B Testing → 실험 만들기 → Remote Config. 타겟팅은 이 기능이 들어간 versionCode 이상.
2. 매개변수 `android_shop_layout_variant`: 기준 = `A`, 변형 B = `B`. Remote Config 기본값은 `A`(또는 비움).
3. 활성화 이벤트: `shop_recommend_view`(추천 탭을 실제로 본 사람만).
4. 주 측정항목: `shop_purchase`. 함께 볼 것: `shop_add_to_cart`, `shop_recommend_item_click`.

## 측정(GA4)

| 단계 | 이벤트 | 매개변수 |
|---|---|---|
| 추천 탭 칸을 받아 그림 | `shop_recommend_view` | `layout_group`, `sections`(받은 칸 종류를 위에서부터, 예 `hero_banner,item_list,ranking`) |
| 추천 탭 상품·배너 누름 | `shop_recommend_item_click` | `layout_group`, `section`(hero_banner·item_grid·ranking·item_list), 숫자 `section_order`(제목 줄을 뺀 칸 중 몇 번째), `position`(칸 안 몇 번째), `product_id` 또는 `banner_id` |
| 장바구니 담기 성공 | `shop_add_to_cart` | `source`(recommend·interior·background·promotion·cart), 숫자 `product_id` |
| 구매 완료 | `shop_purchase` | 숫자 `item_count`, `coins` |

- 추천 탭을 읽을 때마다 사용자 속성 `shop_layout_group`(A·B)을 남긴다. 담기·구매는 이 속성으로 그룹을 나눠 본다.
- GA4 맞춤 정의 등록: 이벤트 범위 `layout_group`, `sections`, `section`, `source`, 측정항목 `section_order`, `position`, `item_count`, `coins`,
  사용자 범위 `shop_layout_group`. 개발 빌드에서 DebugView로 보는 법은 코인 충전 이벤트 문서와 같다(`-Pnyummy.analyticsInDebug=true`).
