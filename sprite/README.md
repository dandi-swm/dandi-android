# Sprite Module

`sprite` 모듈은 범용 스프라이트 엔진과 이를 확인하기 위한 sample/usage
screen을 함께 가진다.

범용 엔진은 캐릭터를 모른다. 캐릭터의 기분, 체력, 성장 단계, 행동 판단은
`sprite`가 아니라 해당 캐릭터를 소유한 feature가 담당한다. `sprite`의 엔진
계층은 이미 선택된 스프라이트 시트와 애니메이션 클립을 받아 프레임을
계산하고 그리는 책임만 가진다.

현재 모듈 안에는 `SpritePage`, `SpriteViewModel`도 있다. 이 화면은 `sprite`
모듈을 어떻게 쓰는지 보여주는 sample/usage screen이다. 현재 프로젝트에는 실제
앱 시작 화면이 아직 없으므로 임시 시작 route로 등록해 둔다. 캐릭터 도메인
상태를 관리하기 위한 위치가 아니다.

이 문서의 코드 예시는 `sprite` 모듈이 지향하는 목표 구조다. 현재 구현이
단일 `SpriteVO` 중심이라면, 이후 리팩터링은 이 경계를 기준으로 진행한다.

## 왜 이렇게 나누는가

캐릭터 상태를 `sprite`에 넣기 시작하면 이 모듈은 렌더러가 아니라 캐릭터
도메인 모듈이 된다.

예를 들어 `DandiMood`, `Hungry`, `GrowthStage` 같은 값은 스프라이트를
그리는 데 필요한 범용 개념이 아니다. 이런 값은 서비스 정책, 사용자 상태,
캐릭터 기획에 따라 바뀌므로 feature의 domain 또는 presentation에 둔다.

`sprite`는 다음 질문에만 답한다.

- 시트의 몇 번째 프레임을 잘라야 하는가?
- 현재 클립에서 다음 프레임은 무엇인가?
- 반복 재생이면 어디로 돌아가야 하는가?
- 선택된 프레임을 Canvas에 어떻게 그릴 것인가?

sample/usage screen은 다음 질문에만 답한다.

- 예제 화면에서 어떤 animation sample을 선택할 것인가?
- 예제 화면에서 어떤 layout mode로 시트를 해석할 것인가?
- 예제 화면에서 frame speed를 어떻게 조절할 것인가?
- 예제 화면에서 재생/정지와 loop 옵션을 어떻게 바꿀 것인가?

sample/usage screen도 캐릭터의 기분, 성장, 행동 의미를 판단하지 않는다.

## 모듈별 책임

| 모듈 | 책임 |
|---|---|
| `sprite:entity` | 시트, 클립, 프레임 같은 순수 값 모델 |
| `sprite:domain` | 프레임 좌표 계산, 다음 프레임 계산, 범용 재생 규칙 |
| `sprite:presentation` | `SpriteView` 렌더러, 프레임 타이머, 렌더링 옵션, sample/usage screen |

사용하는 feature는 다음 책임을 가진다.

| 위치 | 책임 |
|---|---|
| feature `domain` | 캐릭터의 상태 계산 |
| feature `presentation` | 캐릭터 상태를 sprite animation key 또는 clip으로 매핑 |

현재 shell의 책임은 별도로 본다.

| 위치 | 책임 |
|---|---|
| `sprite:domain/SpritePage` | 임시 sample route 메타데이터 |
| `sprite:presentation/SpritePage` | animation 선택, layout 선택, frame speed, 재생/정지, loop 옵션을 보여주는 예제 화면 |
| `sprite:presentation/SpriteViewModel` | 예제 화면의 최소 MVI 상태 |
| `sprite:presentation/res/drawable-nodpi` | demo/preview용 스프라이트 시트 리소스 |

이 shell과 route 등록은 나중에 실제 앱 시작 화면 또는 실제 캐릭터 feature가
생기면 제거하거나 별도 demo/sample feature로 분리할 수 있다.

현재 `SpritePage`는 다음 사용 흐름을 보여준다.

```kotlin
val spec = uiState.selectedSpec.copy(loop = uiState.loop)

SpriteView(
    spec = spec,
    playing = uiState.isPlaying,
)
```

즉 sample screen은 `SpriteView`에 `SpriteVO`와 재생 상태를 넘기는 방법을
보여준다. 실제 앱 시작 화면이 생기면 이 route는 제거하고, 캐릭터를 소유한
feature가 자기 상태를 `SpriteVO` 또는 animation key/clip으로 바꿔 `SpriteView`에
전달한다.

## 현재 demo 리소스 위치

현재 추가된 `hello`, `eat`, `sleep` 애니메이션 이미지는 앱 리소스로
패키징되어야 하므로 `sprite:presentation`에 둔다. 각 애니메이션은 `Grid`,
`BinaryTree`, `LeftRight`, `TopDown` 배치별 시트를 가진다.

```text
sprite/presentation/src/main/res/drawable-nodpi/sample_cat_grid_hello_sprite_sheet.png
sprite/presentation/src/main/res/drawable-nodpi/sample_cat_binary_hello_sprite_sheet.png
sprite/presentation/src/main/res/drawable-nodpi/sample_cat_left_right_hello_sprite_sheet.png
sprite/presentation/src/main/res/drawable-nodpi/sample_cat_top_down_hello_sprite_sheet.png
```

스프라이트 시트는 원본 픽셀 좌표를 기준으로 잘라 그리기 때문에 `drawable`이
아니라 `drawable-nodpi`를 사용한다. Android density scaling이 리소스에
적용되면 `SpriteFrameVO.srcX`, `srcY`, `widthPx`, `heightPx`가 이미지 원본
좌표와 어긋날 수 있다.

현재 sample 이미지는 모두 같은 크기의 `136px` 프레임을 사용한다. `Grid` 시트는
4열 grid이고, `LeftRight`는 가로 한 줄, `TopDown`은 세로 한 줄, `BinaryTree`는
`sprite-creator`의 packed binary tree 결과물이다. sample spec은 프레임별 rect
목록을 직접 들고 있지 않고, `SpriteAlignElement`에 따라 source rectangle을
계산한다.

```kotlin
val spec = SpriteSampleAnimation.Hello.spec(SpriteAlignElement.Grid)
val binarySpec = SpriteSampleAnimation.Hello.spec(SpriteAlignElement.BinaryTree)
```

`SpritePage` 예제 화면은 animation selector와 layout dropdown을 통해 이 매핑을
확인한다. 이 sample spec은 `SpritePage` 예제 화면과 Compose Preview에서 렌더러를
확인하기 위한 데이터다. 특정 캐릭터의 기분, 배고픔, 성장 단계 같은 상태를
판단하는 위치는 아니다. 실제 feature에서 캐릭터 상태가 생기면 feature
presentation의 mapper/catalog가 상태를 `SpriteVO` 또는 이후의 animation
key/clip으로 변환한다.

## 기본 사용 흐름

feature는 캐릭터 상태를 먼저 계산한다.

```kotlin
// main:domain 또는 캐릭터를 소유한 feature domain
enum class DandiMood {
    Normal,
    Happy,
    Tired,
}
```

그 다음 화면 또는 mapper에서 캐릭터 상태를 스프라이트 애니메이션 키로
변환한다.

```kotlin
// main:presentation 또는 feature presentation mapper
fun DandiMood.toSpriteAnimationKey(): SpriteAnimationKey =
    when (this) {
        DandiMood.Normal -> SpriteAnimationKey("idle")
        DandiMood.Happy -> SpriteAnimationKey("happy")
        DandiMood.Tired -> SpriteAnimationKey("sleepy")
    }
```

`sprite` 모듈은 `DandiMood`를 모른다. `sprite`는 `"idle"`, `"happy"`,
`"sleepy"` 같은 범용 key 또는 이미 선택된 clip만 받는다.

## 목표 값 모델 예시

스프라이트 정보는 시트 리소스, 프레임 크기, 프레임 수, align 방식, 재생 규칙만
표현한다. `sprite-creator`처럼 같은 크기의 프레임들을 다른 align 방식으로
배치한 시트라면 `SpriteFrameCalculator`가 align 방식에 맞는 source rectangle을
계산한다.

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

`alignElement`는 `sprite-creator`의 Align element와 같은 의미다.

```kotlin
enum class SpriteAlignElement {
    Grid,
    BinaryTree,
    LeftRight,
    TopDown,
}
```

`Grid`는 기본값이며 `framesPerRow`를 기준으로 grid source rectangle을 계산한다.
`BinaryTree`는 `sprite-creator`의 binary tree packed layout과 같은 grow/split
node 배치다. `LeftRight`는 모든 프레임이 가로 한 줄로 놓인 시트, `TopDown`은
모든 프레임이 세로 한 줄로 놓인 시트를 뜻한다. 그래서 별도의 layout mode 타입은
두지 않는다.

현재 구현에서는 `SpriteVO` 하나가 하나의 animation spec 역할을 한다.
나중에 한 시트 안에서 여러 clip을 잘라 써야 하면 clip 모델을 별도로 분리한다.


애니메이션 이름이 필요하면 문자열을 직접 흘리지 않고 key 타입으로 감싼다.

```kotlin
@JvmInline
value class SpriteAnimationKey(val value: String)
```

프레임 계산 결과는 실제 Canvas에서 잘라낼 source rectangle로 사용한다.

```kotlin
data class SpriteFrameVO(
    val index: Int,
    val srcX: Int,
    val srcY: Int,
    val widthPx: Int,
    val heightPx: Int,
)
```

`srcX`, `srcY`, `widthPx`, `heightPx`는 atlas 이미지에서 잘라낼 영역이다. 현재
구현은 같은 크기의 프레임을 align 방식에 따라 배치한 시트를 대상으로 한다.

## 캐릭터별 clip 구성 예시

이미지 리소스 id는 Android 리소스이므로 보통 feature presentation 또는
화면 근처에서 구성한다. domain에 `R.drawable.*`를 넘기지 않는다.

grid 시트라면 기본값인 `Grid`를 사용한다.

```kotlin
val dandiSprite = SpriteVO(
    spriteSheetRes = R.drawable.dandi_sprite_sheet,
    totalFrames = 32,
    framesPerRow = 8,
    frameWidthPx = 64,
    frameHeightPx = 64,
    frameDurationMillis = 120L,
    loop = true,
)
```

가로 한 줄로 이어진 시트라면 `alignElement`만 `LeftRight`로 바꾼다.

```kotlin
val dandiSprite = SpriteVO(
    spriteSheetRes = R.drawable.dandi_sprite_sheet,
    totalFrames = 8,
    frameWidthPx = 64,
    frameHeightPx = 64,
    alignElement = SpriteAlignElement.LeftRight,
    frameDurationMillis = 120L,
    loop = true,
)
```

`sprite-creator`에서 `Binary tree` 배치로 생성한 시트라면 `BinaryTree`를
명시한다. 현재 `SpriteVO`는 모든 프레임이 같은 크기라는 전제를 가지므로,
가변 크기 프레임의 binary tree atlas까지 지원하려면 프레임별 rect 모델을
별도로 추가해야 한다.

```kotlin
val dandiSprite = SpriteVO(
    spriteSheetRes = R.drawable.dandi_binary_tree_sprite_sheet,
    totalFrames = 17,
    frameWidthPx = 136,
    frameHeightPx = 136,
    alignElement = SpriteAlignElement.BinaryTree,
    frameDurationMillis = 120L,
    loop = true,
)
```

각 캐릭터가 어떤 animation spec을 가지는지는 sprite 외부에서 정의한다.

```kotlin
val dandiAnimations = mapOf(
    SpriteAnimationKey("idle") to SpriteVO(
        spriteSheetRes = R.drawable.dandi_idle_sprite_sheet,
        totalFrames = 6,
        framesPerRow = 6,
        frameWidthPx = 64,
        frameHeightPx = 64,
        frameDurationMillis = 120L,
        loop = true,
    ),
    SpriteAnimationKey("sleepy") to SpriteVO(
        spriteSheetRes = R.drawable.dandi_sleep_sprite_sheet,
        totalFrames = 17,
        framesPerRow = 5,
        frameWidthPx = 136,
        frameHeightPx = 136,
        frameDurationMillis = 160L,
        loop = true,
    ),
)
```

캐릭터 상태에서 key를 고르고, key로 `SpriteVO`를 찾은 뒤 `SpriteView`에 넘긴다.

```kotlin
val animationKey = uiState.mood.toSpriteAnimationKey()
val animation = dandiAnimations.getValue(animationKey)

SpriteView(
    spec = animation,
    playing = true,
)
```

## 여러 캐릭터를 그릴 때

캐릭터가 여러 명이어도 `sprite` 모듈의 역할은 바뀌지 않는다. feature가
캐릭터별 sprite spec을 들고 있고, `SpriteView`는 캐릭터마다 같은 방식으로
재사용한다.

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

캐릭터별 animation spec 목록은 `CharacterSpriteSpec` 같은 feature-local
타입으로 묶는다.

```kotlin
data class CharacterSpriteSpec(
    val animations: Map<SpriteAnimationKey, SpriteVO>,
)
```

```kotlin
val characterSpriteSpecs = mapOf(
    CharacterType.Dandi to CharacterSpriteSpec(
        animations = mapOf(
            SpriteAnimationKey("idle") to dandiIdleSprite,
            SpriteAnimationKey("sleepy") to dandiSleepSprite,
        ),
    ),
    CharacterType.Bori to CharacterSpriteSpec(
        animations = mapOf(
            SpriteAnimationKey("idle") to boriIdleSprite,
            SpriteAnimationKey("walk") to boriWalkSprite,
        ),
    ),
)
```

`SpriteAnimationKey`는 전역으로 유일할 필요가 없다. 같은 `"idle"` key라도
`Dandi`의 spec 안에서는 단디의 idle clip으로, `Bori`의 spec 안에서는 보리의
idle clip으로 해석된다.

```kotlin
characters.forEach { character ->
    val spec = characterSpriteSpecs
        .getValue(character.type)
        .animations
        .getValue(character.animationKey)

    SpriteView(
        spec = spec,
        playing = character.playing,
    )
}
```

캐릭터 수가 늘어나면 map을 화면에 직접 두지 않고 `CharacterSpriteCatalog`
같은 mapper/catalog로 분리한다. 이 catalog도 `DandiMood` 같은 캐릭터 의미를
해석하는 위치라면 feature에 둔다.

## 프레임 계산 방식

`SpriteView` 내부의 현재 프레임은 `0 until spec.totalFrames` 범위의 index로
관리한다. 실제 source rectangle은 domain 계산기가 `alignElement`를 보고
계산한다.
`loop`가 켜져 있으면 마지막 프레임 다음 값은 `spec.startFrame`이다.

```kotlin
val frame = SpriteFrameCalculator.frameAt(
    spec = spec,
    frameIndex = currentFrame,
)
```

`Grid` align은 `framesPerRow`를 기준으로 grid 좌표를 계산한다. 예를 들어
136px 프레임 17개가 4열 grid로 배치되어 있으면 index `16`은 5번째 row의
1번째 column이므로 `x = 0`, `y = 544`가 된다.

```kotlin
val spec = SpriteVO(
    totalFrames = 17,
    framesPerRow = 4,
    frameWidthPx = 136,
    frameHeightPx = 136,
)

val frame = SpriteFrameCalculator.frameAt(spec, frameIndex = 16)
// frame.srcX == 0
// frame.srcY == 544
```

`BinaryTree` align은 `sprite-creator`의 packed binary tree 알고리즘을 사용한다.
같은 136px 프레임 17개라도 `BinaryTree`에서는 index `16`이 오른쪽으로 grow된
영역에 들어가므로 `x = 544`, `y = 0`이 된다.

```kotlin
val spec = SpriteVO(
    totalFrames = 17,
    frameWidthPx = 136,
    frameHeightPx = 136,
    alignElement = SpriteAlignElement.BinaryTree,
)

val frame = SpriteFrameCalculator.frameAt(spec, frameIndex = 16)
// frame.srcX == 544
// frame.srcY == 0
```

## sprite 모듈에 두지 않는 것

다음 타입은 sprite 모듈에 두지 않는다.

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

이 값들은 특정 캐릭터와 서비스 정책을 알고 있다. 이런 타입이 `sprite`에
들어가면 `sprite`는 범용 렌더러가 아니라 특정 캐릭터 도메인 모듈이 된다.

## sprite 모듈에 둘 수 있는 확장

캐릭터 의미를 모르는 범용 재생 상태는 `sprite`에 둘 수 있다.

```kotlin
data class SpritePlaybackState(
    val currentKey: SpriteAnimationKey,
    val clipFrameIndex: Int,
    val playing: Boolean,
)
```

one-shot clip이 끝난 뒤 fallback clip으로 돌아가는 규칙처럼 범용 애니메이션
규칙도 `sprite:domain`에 둘 수 있다.

```kotlin
data class SpriteAnimationGraphVO(
    val defaultKey: SpriteAnimationKey,
    val animations: Map<SpriteAnimationKey, SpriteVO>,
    val fallbackKey: SpriteAnimationKey = defaultKey,
)
```

단, 이런 타입도 `DandiMood`, `Hungry`, `GrowthStage` 같은 캐릭터 의미를
알면 안 된다.

## PR에 남길 설계 고민

현재 `sprite` 모듈은 범용 스프라이트 엔진과 sample/usage screen을 함께 포함한다.
초기 단계에서는 Preview와 예제 화면에서 `SpriteView` 사용법을 바로 확인할 수 있어
이 구조를 유지한다.

다만 이 구조가 `sprite`를 순수 라이브러리처럼 보이게 하지는 않는다.
`SpritePage`, `SpriteViewModel`, 임시 start route는 sample shell 성격을 가진다.
이 shell은 캐릭터 도메인 상태를 소유하지 않고, sample/usage screen으로만 사용한다.

향후 실제 캐릭터 feature가 생기고 `SpriteView` 사용처가 명확해지면,
`SpritePage`와 `SpriteViewModel` 및 route 등록을 제거하거나 별도 demo/sample
feature로 분리하는 것을 검토한다.
