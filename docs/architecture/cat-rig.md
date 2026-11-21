# 냐미 리그 (파츠 + 본 + 착용 슬롯)

냐미를 스프라이트 시트 대신 파츠 그림과 본(뼈대)으로 움직인다. 고양이를 늘리거나 옷, 모자를 입힐 때 그림을 프레임마다 새로 그리지 않아도 된다.
원격 구성 `android_cat_rig_enabled`(기본 false)가 켜지면 홈과 마이룸의 냐미만 리그로 바뀌고, 동작 선택(`CatBrain`), 대사, 자리, 끝남 콜백은 스프라이트와 같다.

## 왜 기본값이 꺼져 있나

- 정면 앉은 원화 한 장으로는 눕기, 구르기, 배 보이기 같은 큰 동작을 그릴 수 없어 11종이 근사다(`CatClipLibrary.approximateMotions`).
- 그림체가 바뀐다(136px 픽셀풍 스프라이트 → 매끈한 원화).
- 실기기 프레임 성능을 아직 재지 않았다.

디자이너 자세 원화와 실기기 확인 뒤 원격 구성으로 일부부터 켠다. 리그 그림을 못 읽으면 화면이 스프라이트로 되돌아간다.

## 구조

| 모듈 | 내용 |
|---|---|
| `cat:entity` `rig/` | 데이터 모델. `CatBone`(본 계층), `AttachmentSlot`/`CatZOrder`(착용 슬롯, 그리는 순서), `CatCharacterSpec`/`CatSkinId`(고양이 스킨), `CatBodyRigVO`(체형 파츠 묶음), `RigClip`/`RigChannel`(동작 클립) |
| `cat:domain` `rig/` | 순수 Kotlin 엔진. `CatSkeleton`(체형별 휴지 자세), `CatPoseSolver`(채널 → 본 변환), `SkinnedGrid`/`CatWeightRules`(선형 블렌드 스키닝), `CatRigModel`(그릴 목록), `CatRigAnimator`(상태 머신, 관성 블렌딩, 스프링), `CatClipLibrary`(클립) |
| `cat:data` `rig/` | `cats/rig/` 에셋 해석(`CatRigParser`), 플래그(`CatRigRepositoryImpl`), debug 덮어쓰기(`CatRigDebugStore`) |
| `cat:presentation` | `CatRigView`(Compose), `CatRigRenderer`(drawBitmapMesh), 텍스처 읽기, 검증용 코드 도형 아이템 |
| `tools/cat-rig/extract_parts.py` | 공식 정면 일러스트 5체형 → 파츠 WebP + `rig.json` |

좌표는 136 x 136 논리 단위(u), 바닥 앵커 (64, 118), 시계 방향 회전이 +다(단디-디자이너 검수 기준서 v1.2).

## 한 프레임

`CatRigAnimator.advance(dt)` → 포즈(채널 값) → `CatRigModel.update` → `CatRigFrame`(그릴 목록, z 순서) → `CatRigRenderer.draw`.
시간은 1/240초 고정 하위 단계로만 진행해 주사율이 달라도 같은 시각의 포즈가 같다.

## 더하는 법

**새 고양이(스킨)**: `cat/data/src/main/assets/cats/rig/skins/<id>.json`. 파츠 묶음(`partSet`), 재질별 색 변환(`materials`), 본별 피벗 오프셋(`pivotOffsets`), 기본 반다나 여부를 적는다. 원화가 따로 있으면 `extract_parts.py`로 파츠 묶음을 하나 더 만든다. 코드는 고치지 않는다. 예: `mackerel.json`.

**착용 아이템**: `cats/rig/items.json`에 슬롯, 그림 키, 크기(u, NORMAL 기준), 소켓에 맞출 점(anchor)을 적는다. 모자는 `earLayerMode`(OVER_EARS는 귀를 챙 선 위에서 자름, BETWEEN_EARS는 뒤판 `backArt`를 귀 뒤에)를 정한다. 목 장식이나 `hidesDefaultBandana` 옷은 기본 반다나를 숨긴다.

**동작 클립**: `CatClipLibrary`에 `clip(...)`으로 더한다. 휴지 자세에서 시작해 휴지 자세로 끝나고, 되풀이 구간은 시작과 끝 값, 기울기가 같아야 한다(`RigClipContractTest`가 막는다). 계단형 보간은 없다.

## 품질 테스트

- `cat:domain` `RigClipContractTest`: 클립 시작/끝, 이음매 값과 기울기, Hermite, 최단 호
- `cat:data` `CatRigMatrixTest`: 스킨 2 x 체형 5 x 상황 9 x 착용 3에서 프레임당 이동, 1px 이상 6Hz 초과 떨림, 이음새, 접지, 모자 안전 영역, 아이템 추종 / 무작위 전환과 재인터럽트. 결과 표 `cat/data/build/cat-rig-metrics/`
- `cat:data` `CatRigSlotAndSkinTest`: 동작 전수(미재생 0), 프레임률 독립, z 순서, 귀 처리, 체형 비율, 스킨 데이터 교체
- `cat:presentation` androidTest `CatRigViewTest`(그리기, 재생, 끝남, 모자 픽셀), `CatRigFrameCaptureTest`(연속 프레임 PNG)

debug 확인: `adb shell am start -n com.dandi.nyummy/.debug.CatRigDebugActivity --es body PLUMP --es skin mackerel --es outfit full_over --ez apply true`
