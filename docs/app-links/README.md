# 앱 링크 (Android App Links)

`https://link.nyummy.co.kr/<path>` 를 누르면 선택 팝업 없이 냐미 앱의 해당 화면이 열린다.

## 구성

| 쪽 | 내용 |
|---|---|
| 앱 | `app/src/main/AndroidManifest.xml` 의 `autoVerify` intent-filter (`https://link.nyummy.co.kr`). path 는 `AppRouteRegistry.kt` 에 등록된 라우트와 동일 — 화면을 등록하면 링크가 함께 생긴다. 미등록 path 는 인트로로 폴백. |
| 도메인 | `link.nyummy.co.kr` → CloudFront → S3 (서울). 가비아 DNS: ACM 검증 CNAME + `link` CNAME → CloudFront. |
| 파일 | `.well-known/assetlinks.json` ([assetlinks.json](assetlinks.json)) — 패키지명 + 서명 키 지문. `index.html` ([index.html](index.html)) — 앱으로 못 열린 경우 보이는 폴백 페이지. |

랜딩 페이지는 `www.nyummy.co.kr` 에 따로 두고, `link` 호스트는 앱 링크 전용으로만 쓴다(경로 충돌 방지).

## 링크 예시

| 링크 | 화면 |
|---|---|
| `https://link.nyummy.co.kr/meal/record` | 식사 기록 (리마인드 푸시·위젯 기본값) |
| `https://link.nyummy.co.kr/home` · `/history` · `/collection` · `/shop` | 각 탭 |

## S3 / CloudFront 설정

- 버킷에 `.well-known/assetlinks.json`(Content-Type `application/json`)과 `index.html` 업로드.
- CloudFront **오류 페이지(Error responses)**: `403 → /index.html, 응답 코드 200`, `404 → /index.html, 응답 코드 200`.
  OAC 로 접근하는 S3 는 없는 키에 403 을 돌려주므로 둘 다 매핑해야 `/meal/record` 같은 경로가 폴백 페이지로 떨어진다.
  실제 존재하는 파일(`assetlinks.json`)은 이 매핑을 타지 않는다.
- `assetlinks.json` 을 바꾼 뒤에는 CloudFront 캐시 무효화(`/.well-known/*`)를 한 번 돌린다.

## 서명 키 지문 관리

`assetlinks.json` 의 `sha256_cert_fingerprints` 는 **앱에 서명한 키**의 지문 목록이다. 앱 버전과 무관하며 키가 바뀔 때만 수정한다.

- 디버그 빌드: PC 마다 `~/.android/debug.keystore` 가 달라 지문도 다르다. 자기 PC 빌드에서 검증을 보려면 아래로 뽑아 배열에 추가한다.
  ```bash
  keytool -exportcert -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -rfc \
    | openssl x509 -fingerprint -sha256 -noout
  ```
- 스토어 빌드: Play App Signing 이 다시 서명하므로 Play Console > 테스트 및 출시 > 설정 > 앱 서명의 **앱 서명 키 인증서 SHA-256** 을 추가한다(업로드 키 지문이 아님). 내부 테스트 트랙도 같은 키라 출시 전에 검증할 수 있다.
- 파일을 먼저 배포하고 앱을 설치/업데이트해야 한다 — 안드로이드는 설치 시점에 파일을 읽는다.

## 검증

```bash
# 1. 파일이 200 / application/json / 리다이렉트 없이 나오는지
curl -i https://link.nyummy.co.kr/.well-known/assetlinks.json

# 2. 구글이 해석한 결과
curl "https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://link.nyummy.co.kr&relation=delegate_permission/common.handle_all_urls"

# 3. 기기에서 재검증 후 상태 확인 (link.nyummy.co.kr: verified 여야 한다)
adb shell pm verify-app-links --re-verify com.dandi.nyummy
adb shell pm get-app-links com.dandi.nyummy

# 4. 실제 클릭과 같은 조건(패키지 지정 없음)으로 열기
adb shell am start -a android.intent.action.VIEW -d "https://link.nyummy.co.kr/meal/record"
```

Play Console > 성장 > 딥 링크 에서도 도메인별 검증 상태를 볼 수 있다.
