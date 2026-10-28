# Remote Config 홈 새 소식 공지

앱을 열고 홈에 처음 들어왔을 때 띄우는 새 소식 다이얼로그를, 서버 개발이나 앱 배포 없이 Firebase Remote Config의 JSON 하나로 띄우고 내린다.
화면은 피그마 `v2 · 20 Home & My Room`의 `Notice · 01~03`, 동작은 `openspec/changes/add-home-notice`(아카이브 뒤에는 `openspec/specs/home/notice`)를 본다.

## 키

| 빌드 | 읽는 키 | 타입 |
|---|---|---|
| release | `android_home_notice` | 문자열(JSON) |
| debug, benchmark | `android_home_notice_debug` | 문자열(JSON) |

- 키가 없거나, 비었거나, JSON이 깨졌으면 아무것도 띄우지 않는다. 앱은 그대로 동작한다.
- 앱은 인트로가 앱 시작 때 받아 둔 값을 읽는다. 콘솔에서 바꾼 값은 앱을 완전히 종료하고 다시 열 때(받아 오기 주기가 지난 뒤) 반영된다.
- 구현: `RemoteConfigKey.HomeNotice`, `home/data/.../dto/HomeNoticeDTO.kt`, `GetHomeNoticesUseCase`, `CloseHomeNoticeUseCase`, `NyummyNoticeDialog`

## JSON 형식

```json
{
  "notices": [
    {
      "id": "2026-10-renewal",
      "badge": "업데이트",
      "startAt": "2026-10-28T00:00:00+09:00",
      "endAt": "2026-11-30T23:59:59+09:00",
      "minVersionCode": 6,
      "imageUrl": "https://link.nyummy.co.kr/notice/2026-10-renewal.webp",
      "title": "냐미가 새단장을 했어요!",
      "body": "화면을 더 보기 편하게 새로 꾸몄어요.\n새로 들어온 기능도 확인해 보세요."
    },
    {
      "id": "2026-autumn-event",
      "badge": "이벤트",
      "meta": "10월 28일 ~ 11월 10일",
      "imageUrl": "https://link.nyummy.co.kr/notice/2026-autumn-event.webp",
      "title": "가을 한 상 이벤트가 열렸어요",
      "body": "이벤트 페이지에서 자세히 알아보세요.",
      "cta": { "text": "자세히 보기", "link": "/shop" }
    }
  ]
}
```

| 필드 | 필수 | 설명 |
|---|---|---|
| `id` | | 운영자가 공지를 알아보는 이름. 화면에는 쓰지 않는다. |
| `badge` | | 사진 위 왼쪽 배지 글자. 비우면 배지가 없다. |
| `startAt` / `endAt` | | 보이는 기간. ISO 8601에 **오프셋을 꼭 붙인다**(`+09:00`, `Z`). 오프셋이 없으면 UTC로 읽는다. 적지 않으면 그쪽 끝이 열려 있다. 읽을 수 없는 값을 적으면 그 공지는 띄우지 않는다. |
| `minVersionCode` | | 이 versionCode 이상인 앱에만 보인다. 새 기능 소개는 그 기능이 들어간 versionCode를 적는다. |
| `imageUrl` | | 10:9 사진 주소(`https://link.nyummy.co.kr/notice/…`). 없거나 받지 못하면 연초록 바탕만 보인다. |
| `meta` | | 제목 위 작은 회색 글(이벤트 기간 등). |
| `title` | 필수 | 제목. 없으면 그 공지는 띄우지 않는다. |
| `body` | | 설명. 줄을 바꾸려면 `\n`. |
| `cta` | | 버튼. `text`와 `link`가 모두 있어야 보인다. 없으면 버튼 없는 공지다. |

- 공지는 `notices` 순서대로 넘겨 본다. 기간과 버전 조건에 맞는 것만 남는다.
- 그 밖의 필드(예: 예전 메모의 `type`)는 무시한다.

## 버튼 링크

`/`로 시작하는 **앱 경로**만 연다. 웹 주소(`https://…`)를 넣으면 버튼을 눌러도 공지만 닫힌다. 앱에 없는 경로도 공지만 닫힌다.

쓸 수 있는 경로(`AppRouteRegistry.kt` 기준, 인자 없이 열리는 화면):

| 경로 | 화면 |
|---|---|
| `/meal/record` | 식사 기록(카메라) |
| `/history` | 기록 탭 |
| `/shop` | 상점 탭 |
| `/room` | 마이룸 탭 |
| `/mailbox` | 우편함 |
| `/settings` | 설정 |
| `/settings/meal-time` | 식사 시간 설정 |
| `/settings/inquiry` | 문의하기 |

- 버튼은 기능 플래그를 보지 않고 그 화면을 바로 연다. 상점·마이룸·우편함처럼 플래그로 막아 둔 기능은 플래그를 켠 뒤에만 링크로 건다.
- `/history/meals/{mealId}`처럼 경로에 값이 들어가는 화면은 지금 지원하지 않는다.

## 사진 만들기

1. 피그마 공지 카드 안의 `Notice image (...)` 레이어를 고른다. 이 프레임만 사진에 들어가고, 배지는 앱이 위에 얹는다.
2. 오른쪽 아래 Export(PNG 3x)로 내보낸다. 1002×900(10:9)이다.
3. WebP로 압축한다: `cwebp -q 85 -m 6 2026-10-renewal.png -o 2026-10-renewal.webp`(1MB 남짓 PNG가 100KB 아래로 준다).
4. 앱 링크 CDN(`link.nyummy.co.kr`, CloudFront → S3) 버킷의 `notice/` 폴더에 올리고, `https://link.nyummy.co.kr/notice/<파일명>`을 `imageUrl`에 넣는다.

- 파일명에 공지 id를 붙여 공지마다 새 이름으로 올린다(예: `2026-10-renewal.webp`). 같은 이름으로 덮어쓰면 CloudFront와 앱의 이미지 캐시에 옛 사진이 남는다. 꼭 덮어써야 하면 CloudFront 캐시 무효화(`/notice/*`)를 돌린다.
- 이 버킷은 없는 키에 폴백 페이지(`index.html`, 200)를 준다. 주소를 틀리면 사진 대신 연초록 바탕만 보이니, 게시 전에 브라우저(PC)에서 주소가 사진으로 열리는지 본다.
- `link` 호스트는 앱 링크라, 안드로이드 폰 브라우저에서 사진 주소를 누르면 앱이 열린다. 앱이 사진을 받아 오는 데는 영향이 없다.

- 사진은 반드시 **10:9**로 만든다. 앱은 사진 칸을 10:9로 고정하고 화면 폭에 맞춰 줄였다 늘린다. 비율이 다르면 가운데를 기준으로 가장자리가 잘린다.
- 사진 맨 위 왼쪽(약 64 높이)에는 배지가 얹힌다. 중요한 내용은 그 아래에 둔다.
- 세로가 짧은 화면에서는 사진을 자르지 않고 다이얼로그 전체를 작게 줄인다. 작은 글씨는 사진 안에 넣지 않는다.

## 띄우는 규칙

- 앱을 열고 홈에 처음 들어올 때 한 번. 같은 실행에서 다시 홈에 들어와도 띄우지 않는다.
- 첫 사용자 튜토리얼이 떠 있는 실행에는 띄우지 않는다(다음 실행에 보인다).
- "닫기"(바깥 탭, 뒤로 가기 포함): 이번 실행만 닫는다. 앱을 다시 열면 또 뜬다.
- "오늘 하루 보지 않기"를 체크하고 닫거나 버튼을 누르면 그 기기 날짜로 오늘은 띄우지 않는다. 계정이 아니라 기기 단위다.

## 게시 순서

1. `android_home_notice_debug`에 JSON을 넣고 게시한다.
2. debug 빌드를 완전히 종료했다가 다시 열어 홈에서 확인한다: 넘기기, 사진, 버튼 이동, 닫기, 오늘 하루 보지 않기.
   - 서버 없이 다이얼로그만 보려면 디버그 화면을 쓴다:
     `adb shell am start -n com.dandi.nyummy/com.dandi.nyummy.common.presentation.debug.TutorialDebugActivity --es scene notice --es images "<사진1>,<사진2>,<사진3>"`
3. 같은 JSON을 `android_home_notice`에 넣고 게시한다.
4. 내릴 때는 운영 키 값을 비우거나 `endAt`을 지난 시각으로 바꾼다.
