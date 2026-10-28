# Remote Config 강제 업데이트 설정

2026-10-14 작업 내용을 복원한 문서. 아래 값은 당시 확인한 초안 기준이며, 현재 Firebase 게시 상태를 다시 확인한 것은 아니다.

## 가장 중요한 규칙

**운영 중인 서비스가 읽는 기존 키 이름과 값은 함부로 바꾸지 않는다.**

다음 기존 키 5개는 이름과 값을 유지하고 그룹으로만 옮겼다.

- `android_latest_version_update_link` → release 그룹
- `android_latest_version_update_link_qa` → qa 그룹
- `android_minimum_version_code_qa` → qa 그룹
- `android_minimum_version_qa` → qa 그룹
- `android_minimum_version_release_note_qa` → qa 그룹

그룹은 콘솔에서 보기 좋게 묶는 용도다. 앱은 그룹 경로가 아니라 키 이름으로 읽는다. 그룹 간에도 키 이름은 프로젝트 전체에서 고유해야 한다.

주의: Admin SDK/REST로 템플릿을 직접 읽는 서비스는 그룹 이동 후 `parameters` 대신 `parameterGroups` 아래에서 찾아야 할 수 있다. 일반 Android 클라이언트 SDK의 키 조회와는 다르다.

## 당시 구성한 초안

| 그룹 | 최소 versionCode | 최소 버전 이름 | 키 접미사 |
|---|---:|---|---|
| release | 0 (강제 업데이트 꺼둠) | 2.0.0 | 없음 |
| qa | 1 (기존 값 유지) | 1.1.0 (기존 값 유지) | `_qa` |
| debug | 1 (qa 값 복사) | 1.1.0 (qa 값 복사) | `_debug` |

각 그룹의 키는 다음 4개다. qa/debug는 각 이름 끝에 해당 접미사를 붙인다.

- `android_minimum_version_code`: 숫자, 강제 업데이트 판단 기준
- `android_minimum_version`: 문자열, 버전 이름. 당시 앱 코드에서는 강제 업데이트 판단에 사용하지 않음
- `android_latest_version_update_link`: 문자열, 업데이트 버튼의 이동 링크
- `android_minimum_version_release_note`: 문자열, 강제 업데이트 다이얼로그 문구

모든 그룹의 링크: `https://velog.io/@cjw4458/posts`

플레이스토어 미공개 상태라 블로그 링크를 임시로 유지했다. 스토어 공개 및 업데이트 가능 여부를 확인한 뒤 교체한다.

release 문구:

> 현재 앱이 최소 요구 버전을 만족하지 않습니다. 최신 버전으로 업데이트 해주세요.

qa/debug 문구:

> 테스트 강제 업데이트입니다.

당시 마지막 상태는 **게시하지 않은 초안**이었다.

## versionCode 0의 의미

당시 `GetIntroUseCase.isForceUpdateRequired`의 조건은 다음과 같다.

```kotlin
version.minimumVersionCode > 0L && deviceHelper.appVersionCode < version.minimumVersionCode
```

- release 최소 versionCode를 4에서 **0으로 변경**해 업데이트 체크를 껐다.
- 0이면 기존 버전도 강제 업데이트 창을 보지 않는다.
- 강제 업데이트가 필요할 때만 값을 올린다. 사용자가 실제로 설치할 수 있는 업데이트 버전의 versionCode를 초과하지 않게 한다.
- 예: 최소 code 4라면 현재 code 3 이하는 차단, code 4 이상은 허용.
- 당시 앱 빌드 설정은 versionName 2.0.0, versionCode 4였다.

## 앱이 읽는 키 (2026-10-18 반영)

qa 환경은 따로 두지 않기로 했다. 앱은 빌드 타입이 release인지(`@ReleaseBuild`)만 보고 두 갈래로 키를 고른다.

| 빌드 | 환경(`RemoteConfigEnv`) | 읽는 키 |
|---|---|---|
| release | RELEASE | 접미사 없음 |
| debug, benchmark, 베이스라인 프로파일 수집용 | DEBUG | `_debug` |

- 구현: `common/domain/.../remoteconfig/RemoteConfigKey.kt`(키 정의), `common/data/.../remoteconfig/FirebaseRemoteConfigHelper.kt`, `RemoteConfigEnv.kt`, `common/data/.../di/RemoteConfigModule.kt`
- 원격 구성은 `intro`에서 `common`으로 옮겼다. 인트로가 앱 시작 때 한 번 받아 두고, 강제 업데이트와 기능 플래그가 같은 값을 읽는다.
- 키는 `RemoteConfigKey<T>`(이름, 직렬화기, 기본값)로 정의한다. 원시 타입은 Firebase 타입별 조회로, 목록과 VO는 문자열을 JSON으로 읽는다. 콘솔에 값이 없거나 형식이 맞지 않으면 기본값을 쓴다. 새 키는 `data object`를 하나 추가한다.
- 콘솔의 기존 `_qa` 키는 앱이 더 읽지 않는다. 운영 키 이름과 값은 그대로 두는 규칙에 따라 지우지 않았다.
- debug 빌드는 이제 `_debug` 키를 읽는다. debug 그룹을 게시하지 않으면 debug 빌드는 기본값(최소 versionCode 0, 기능 플래그 꺼짐)으로 동작해 강제 업데이트가 걸리지 않는다.
- 이전에는 받아 오기 전 기본값으로 현재 앱 버전을 깔았다. 지금은 키마다 정적 기본값을 두고, 최소 versionCode 기본값 0이 같은 역할(강제 업데이트 안 함)을 한다.

### 기능 플래그 키

| 키 | `RemoteConfigKey` | 타입 | 기본값 | release | debug |
|---|---|---|---|---|---|
| `android_mail_item_tab_enabled` | `MailItemTabEnabled` | Boolean | false | false(우편함 아이템 탭 숨김) | `_debug`: true |
| `android_my_room_enabled` | `MyRoomEnabled` | Boolean | false | false(하단 내비 마이룸 탭을 누르면 "준비 중" 안내) | `_debug`: true |
| `android_shop_enabled` | `ShopEnabled` | Boolean | false | false(하단 내비 상점 탭을 누르면 "준비 중" 안내) | `_debug`: 미정 |

키가 없거나 값을 받지 못하면 꺼진 것으로 본다.

### 홈 새 소식 공지 키

`android_home_notice`(debug는 `_debug`)에 공지 JSON을 넣는다. 형식과 게시 순서는 [remote-config-home-notice.md](remote-config-home-notice.md)를 본다.

### 남은 일

- [x] qa와 debug를 구분하는 방식 결정: qa 없이 release / 그 밖의 빌드 두 갈래
- [x] 환경값을 DI로 전달(`@ReleaseBuild` → `RemoteConfigEnv`)
- [x] 키 선택을 환경별로 변경하되 기존 release 키 이름 유지
- [x] benchmark 빌드는 `_debug` 키를 읽음
- [x] 환경별 키 선택 테스트(`RemoteConfigEnvTest`)
- [x] code 0이면 차단 없음, 최소 code 미만이면 차단, 같으면 허용하는 경계 테스트(`GetIntroUseCaseTest`)
- [x] 오프라인, 값 누락에서 앱이 불필요하게 차단되지 않는지 테스트(기본값으로 만든 버전 확인 값, `DecodeRemoteConfigValueTest`의 형식 오류)
- [ ] 콘솔 debug 그룹에 `_debug` 키 4개와 기능 플래그 `_debug` 키 게시

## 나중에 게시할 때

- [ ] 콘솔 초안이 여전히 있는지 확인
- [ ] 기존 키 5개의 이름과 값 유지 여부 확인
- [ ] 기능 플래그 운영 키(`android_mail_item_tab_enabled`, `android_my_room_enabled`, `android_shop_enabled`)가 false인지 확인
- [ ] release 최소 versionCode가 0인지 확인
- [ ] 업데이트 링크와 문구 확인
- [ ] 사용자 검토 및 승인 후 변경사항 게시
