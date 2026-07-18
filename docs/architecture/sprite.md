# Sprite Architecture

이 문서는 에이전트가 `sprite` 모듈을 수정할 때 지켜야 하는 경계와 판단
기준을 정의한다. 개발자가 사용법을 빠르게 확인할 때는
[`sprite/README.md`](../../sprite/README.md)를 먼저 본다.

## 핵심 원칙

`sprite` 모듈은 범용 스프라이트 엔진과 sample/usage screen을 함께 가진다.
엔진은 `Documents/sprite-creator`의 Align element 개념 중 `Grid`, `BinaryTree`,
`LeftRight`, `TopDown` 배치를 다룰 수 있어야 한다. `Grid`는 기본값이며
`framesPerRow`를 쓰는 grid로 계산한다. `BinaryTree`는 `sprite-creator`의
binary tree packed layout과 같은 grow/split node 배치다. `LeftRight`와
`TopDown`은 각각 가로 한 줄, 세로 한 줄 배치다. 현재 기준 입력은 같은 크기의
프레임을 가진 시트 스펙이고, source rectangle은 `sprite:domain` 계산기가 align
방식에 맞춰 계산한다.

범용 엔진은 캐릭터를 모른다. 캐릭터의 기분, 체력, 성장 단계, 배고픔,
서비스 정책에 따른 행동 판단은 `sprite`의 책임이 아니다. feature가 캐릭터
상태를 계산하고, 그 상태를 스프라이트 애니메이션 key 또는 clip으로 매핑한
뒤 `sprite`에 전달한다.

현재 `SpritePage`, `SpriteViewModel`은 `sprite` 모듈 사용법을 보여주는
sample/usage shell이다. 현재 프로젝트에는 실제 앱 시작 화면이 아직 없으므로
임시 시작 route로 등록해 둔다. 이 shell은 캐릭터 도메인 상태를 소유하지 않는다.

## 불변 규칙

1. `sprite` 모듈은 캐릭터 도메인 상태를 소유하지 않는다.
2. `sprite:entity`에는 sheet, clip, frame, playback 같은 범용 값 객체만 둔다.
3. `sprite:domain`에는 프레임 계산과 범용 애니메이션 계산만 둔다.
4. `sprite:presentation`의 엔진 영역은 Canvas 렌더링과 프레임 진행만 담당한다.
5. `DandiMood`, `GrowthStage`, `Hungry` 같은 서비스 의미 상태는 `sprite`에 추가하지 않는다.
6. Android 리소스 id가 포함된 VO는 domain에서 생성하지 않는다. 보통 feature presentation 또는 화면 근처에서 만든다.
7. `SpriteAnimationKey`는 전역 key가 아니다. 캐릭터별 `CharacterSpriteSpec` 안에서 해석한다.
8. sample/usage shell에 `Page`, `ViewModel`, `Intent`, `ReducerEvent`를 둘 수 있지만, 이 shell은 sample/usage screen이어야 한다.
9. sample/usage shell은 animation 선택, layout 선택, frame speed, 재생/정지, loop 옵션 같은 범용 사용 예시만 다룬다.
10. 실제 앱 시작 화면이 생기면 sample route를 `main`의 route registry와 기본 back stack에서 제거한다.

## 책임 분리

| 책임 | 위치 |
|---|---|
| 캐릭터의 상태 계산 | 캐릭터를 소유한 feature `domain` |
| 캐릭터 상태를 animation key 또는 clip으로 매핑 | feature `domain` 또는 `presentation` |
| 스프라이트 시트/클립/프레임 값 모델 | `sprite:entity` |
| 프레임 좌표 및 다음 프레임 계산 | `sprite:domain` |
| 이미지 로드, Canvas 렌더링, 프레임 타이머 | `sprite:presentation` |
| 임시 sample route 및 사용 예제 화면 | `sprite` sample/usage shell |

## 현재 shell의 성격

현재 `sprite`에는 다음 sample shell 요소가 있다.

- `sprite:presentation/SpritePage`
- `sprite:presentation/SpriteViewModel`
- `sprite:presentation/SpriteIntent`
- `sprite:presentation/SpriteReducerEvent`
- `sprite:domain/SpritePage`
- `main`의 임시 `/sprite` route 등록

이 요소들은 초기 개발 단계에서 `SpriteView` 사용법과 프레임 계산 결과를 바로
확인하기 위한 sample/usage 진입점이다. 캐릭터 상태를 담는 곳이 아니다.

허용:

```kotlin
data class SpriteUIState(
    val isLoading: Boolean = true,
    val isPlaying: Boolean = true,
    val loop: Boolean = true,
    val selectedAnimation: SpriteSampleAnimation = SpriteSampleAnimation.Hello,
    val selectedAlignElement: SpriteAlignElement = SpriteAlignElement.Grid,
    val frameDurationMillis: Long = 140L,
)
```

예제 화면에서 허용되는 사용법:

```kotlin
val spec = uiState.selectedAnimation
    .spec(uiState.selectedAlignElement)
    .copy(
        frameDurationMillis = uiState.frameDurationMillis,
        loop = uiState.loop,
    )

SpriteView(
    spec = spec,
    playing = uiState.isPlaying,
)
```

금지:

```kotlin
data class SpriteUIState(
    val mood: DandiMood,
    val hunger: Int,
    val growthStage: Int,
)
```

두 번째 예시는 demo shell이 캐릭터 도메인을 소유하게 만들기 때문에 금지한다.
캐릭터 상태는 실제 캐릭터를 소유한 feature에 둔다.

## 여러 캐릭터 기준

캐릭터가 여러 명이어도 `sprite`는 캐릭터 목록을 소유하지 않는다. feature가
캐릭터별 상태와 sprite spec을 관리하고, `sprite`에는 선택된 sheet와 clip만
전달한다.

```kotlin
enum class CharacterType {
    Dandi,
    Bori,
}

data class CharacterRenderState(
    val id: String,
    val type: CharacterType,
    val animationKey: SpriteAnimationKey,
    val playing: Boolean = true,
)
```

```kotlin
data class CharacterSpriteSpec(
    val animations: Map<SpriteAnimationKey, SpriteVO>,
)
```

```kotlin
val characterSpriteSpecs = mapOf(
    CharacterType.Dandi to CharacterSpriteSpec(
        animations = mapOf(
            SpriteAnimationKey("hello") to dandiHelloSprite,
            SpriteAnimationKey("sleep") to dandiSleepSprite,
        ),
    ),
    CharacterType.Bori to CharacterSpriteSpec(
        animations = mapOf(
            SpriteAnimationKey("hello") to boriHelloSprite,
            SpriteAnimationKey("walk") to boriWalkSprite,
        ),
    ),
)
```

`SpriteAnimationKey("idle")`은 전역에서 하나의 clip을 가리키지 않는다.
각 캐릭터의 `CharacterSpriteSpec.clips` 안에서만 해석한다.

```kotlin
characters.forEach { character ->
    val spriteSpec = characterSpriteSpecs.getValue(character.type)
    val animation = spriteSpec.animations.getValue(character.animationKey)

    SpriteView(
        spec = animation,
        playing = character.playing,
    )
}
```

`CharacterType`, `CharacterRenderState`, `CharacterSpriteSpec`,
`CharacterSpriteCatalog`는 feature-local 타입이다. `sprite`에 둘 수 있는
타입은 sheet, clip, frame, playback처럼 캐릭터 의미를 모르는 값뿐이다.

## `sprite:entity`

허용하는 타입은 스프라이트 렌더링에 필요한 순수 값 모델이다.

```kotlin
data class SpriteVO(
    val spriteSheetRes: Int,
    val totalFrames: Int,
    val framesPerRow: Int,
    val frameWidthPx: Int,
    val frameHeightPx: Int,
    val alignElement: SpriteAlignElement,
    val gapPx: Int,
    val paddingPx: Int,
    val frameDurationMillis: Long,
    val startFrame: Int,
    val loop: Boolean,
)
```

```kotlin
enum class SpriteAlignElement {
    Grid,
    BinaryTree,
    LeftRight,
    TopDown,
}
```

```kotlin
data class SpriteFrameVO(
    val index: Int,
    val srcX: Int,
    val srcY: Int,
    val widthPx: Int,
    val heightPx: Int,
)
```

`SpriteAlignElement`는 `sprite-creator`의 Align element와 같은 의미다.
`Grid`는 기본 배치이며 `framesPerRow`를 사용한다. `BinaryTree`는
`sprite-creator`의 packed binary tree 알고리즘을 따른다. `LeftRight`와
`TopDown`은 한 줄짜리 배치라 추가 layout mode를 갖지 않는다. `SpriteFrameVO`는
저장 모델이 아니라 `SpriteFrameCalculator.frameAt(...)`의 계산 결과로 사용한다.
현재 `SpriteVO`는 모든 프레임이 같은 크기라는 전제를 가지므로, 가변 크기 frame을
binary tree atlas로 쓰려면 frame별 rect 모델을 먼저 추가한다.

애니메이션 이름이 필요하면 문자열을 직접 흘리지 않고 key 타입으로 감싼다.

```kotlin
@JvmInline
value class SpriteAnimationKey(val value: String)
```

금지하는 타입은 특정 캐릭터나 서비스 정책을 아는 값이다.

```kotlin
enum class DandiMood {
    Normal,
    Happy,
    Tired,
}

data class DandiCharacterState(
    val mood: DandiMood,
    val hunger: Int,
    val growthStage: Int,
)
```

## `sprite:domain`

허용하는 로직은 범용 계산이다.

- frame index 검증
- `startFrame`이 있는 spec의 sheet frame index 계산
- frame index에 해당하는 source rectangle 계산
- `Grid`, `BinaryTree`, `LeftRight`, `TopDown` align별 좌표 계산
- loop 여부에 따른 다음 frame index 계산
- loop 시 마지막 프레임 다음 값을 `startFrame`으로 되돌리는 계산
- 캐릭터 의미를 모르는 playback 계산

예시:

```kotlin
fun sheetFrameIndex(
    spec: SpriteVO,
    frameIndex: Int,
): Int {
    require(frameIndex in 0 until spec.totalFrames)
    return spec.startFrame + frameIndex
}
```

금지하는 로직은 캐릭터 상태 판단이다.

```kotlin
// sprite:domain에 두지 않는다.
fun DandiCharacterState.nextAnimationKey(): SpriteAnimationKey =
    when {
        hunger > 80 -> SpriteAnimationKey("hungry")
        mood == DandiMood.Tired -> SpriteAnimationKey("sleepy")
        else -> SpriteAnimationKey("idle")
    }
```

위 로직은 `DandiCharacterState`, `hunger`, `DandiMood`를 알기 때문에
캐릭터를 소유한 feature에 둔다.

## `sprite:presentation`

엔진 영역에서 허용하는 책임은 렌더링이다.

- `ImageBitmap.imageResource(...)`
- `Canvas.drawImage(...)`
- `LaunchedEffect` 기반 프레임 진행
- `FilterQuality` 같은 렌더링 옵션
- `SpriteVO` 변경 시 frame index 초기화

현재 API 예시:

```kotlin
@Composable
fun SpriteView(
    spec: SpriteVO,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    filterQuality: FilterQuality = FilterQuality.None,
)
```

`SpriteView`는 캐릭터 상태를 해석하지 않는다. 다음과 같은 파라미터를 받는
방향은 피한다.

```kotlin
// sprite:presentation에 만들지 않는다.
@Composable
fun DandiSpriteView(
    state: DandiCharacterState,
)
```

sample/usage shell 영역에서는 `SpritePage`, `SpriteViewModel`, `SpriteIntent`,
`SpriteReducerEvent`를 둘 수 있다. 단, 이 타입들은 animation 선택, layout 선택,
frame speed, 재생/정지, loop 같은 사용 예제 상태만 다뤄야 한다. 캐릭터의 의미
상태를 다루기 시작하면 feature로 분리한다.

## feature에서의 매핑

캐릭터 상태와 animation key 사이의 매핑은 feature에 둔다.

```kotlin
// main:domain 또는 캐릭터를 소유한 feature domain
enum class DandiMood {
    Normal,
    Happy,
    Tired,
}
```

```kotlin
// main:presentation 또는 feature mapper
fun DandiMood.toSpriteAnimationKey(): SpriteAnimationKey =
    when (this) {
        DandiMood.Normal -> SpriteAnimationKey("idle")
        DandiMood.Happy -> SpriteAnimationKey("happy")
        DandiMood.Tired -> SpriteAnimationKey("sleepy")
    }
```

호출부는 이미 결정된 `SpriteVO`만 `SpriteView`에 전달한다.

```kotlin
val animationKey = uiState.mood.toSpriteAnimationKey()
val animation = dandiAnimations.getValue(animationKey)

SpriteView(
    spec = animation,
)
```

## 새 타입 추가 판단 기준

새 타입을 `sprite`에 추가하기 전에 다음 질문에 답한다.

1. 특정 캐릭터 이름이 들어가는가?
   - 예: `DandiMood`, `DandiCharacterState`, `CatState`
   - 그렇다면 `sprite`에 두지 않는다.
2. 서비스 정책이나 사용자 상태를 알고 있는가?
   - 예: 배고픔, 성장 단계, 친밀도, 날씨 기반 행동
   - 그렇다면 feature domain에 둔다.
3. sheet, align element, frame, playback 같은 범용 애니메이션 개념인가?
   - 그렇다면 `sprite`에 둘 수 있다.
4. Android 리소스 id 또는 Compose 타입이 필요한가?
   - 그렇다면 `sprite:presentation` 또는 feature presentation에서 생성한다.
   - `sprite:domain`에는 Android/Compose 의존을 추가하지 않는다.
5. `Page`/`ViewModel`/`Intent`처럼 feature shell 타입인가?
   - 렌더러 확인용 demo shell이면 임시로 `sprite`에 둘 수 있다.
   - 캐릭터 도메인 상태를 포함하면 해당 캐릭터 feature로 옮긴다.

## 허용 가능한 확장

범용 애니메이션 상태 머신은 `sprite` 내부에 둘 수 있다.

```kotlin
data class SpritePlaybackState(
    val currentKey: SpriteAnimationKey,
    val clipFrameIndex: Int,
    val playing: Boolean,
)
```

```kotlin
data class SpriteAnimationGraphVO(
    val defaultKey: SpriteAnimationKey,
    val animations: Map<SpriteAnimationKey, SpriteVO>,
    val fallbackKey: SpriteAnimationKey = defaultKey,
)
```

이 확장은 캐릭터 의미를 몰라야 한다. one-shot clip 종료 후 fallback으로
돌아가는 규칙은 허용되지만, 캐릭터의 기분이나 배고픔을 보고 key를 고르는
규칙은 허용하지 않는다.

## 테스트 기준

`sprite:domain`을 수정하면 JVM 단위 테스트를 추가하거나 갱신한다.

검증해야 하는 예:

- 첫 프레임 source rectangle 계산
- `Grid`, `LeftRight`, `TopDown`, `BinaryTree` align source rectangle 계산
- loop clip의 마지막 프레임 다음 값
- non-loop clip의 마지막 프레임 정지
- 잘못된 frame index, frame count, duration 검증

`sprite:presentation`을 수정하면 최소한 빌드 검증을 수행한다. 렌더링 동작이
크게 바뀌면 emulator 또는 screenshot 기반 확인을 추가로 수행한다.

## PR 메모

PR에는 현재 구조의 의도와 열린 결정을 명시한다.

```md
현재 `sprite` 모듈은 범용 스프라이트 엔진과 sample/usage screen을 함께 포함합니다.
초기 단계에서는 `SpriteView` 사용법과 렌더러 동작을 바로 확인할 수 있도록 이 구조를 유지합니다.

다만 `SpritePage`, `SpriteViewModel`, 임시 start route는 sample shell 성격을 가지므로,
`sprite`를 순수 라이브러리로 보는 데에는 한계가 있습니다. 이 shell은 sample/usage screen이며,
캐릭터의 기분/체력/행동/성장 단계 같은 도메인 상태는 실제 캐릭터를 소유한 feature에 둡니다.

향후 실제 캐릭터 feature가 생기고 `SpriteView` 사용처가 명확해지면,
`SpritePage`/`SpriteViewModel`/임시 route 등록을 제거하거나 별도 demo/sample feature로 분리하는 것을 검토합니다.
```
