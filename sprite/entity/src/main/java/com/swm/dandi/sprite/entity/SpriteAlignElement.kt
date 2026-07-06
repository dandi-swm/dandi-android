package com.swm.dandi.sprite.entity

/**
 * 스프라이트 시트 안에서 프레임들이 배치된 방식.
 *
 * 같은 애니메이션이라도 생성 도구가 어떤 packing 전략을 썼는지에 따라 frame index 의
 * 좌표가 달라진다. [com.swm.dandi.sprite.domain.SpriteFrameCalculator] 는 이 값을
 * 기준으로 원본 시트 좌표를 계산한다.
 */
enum class SpriteAlignElement {
    /** 행과 열이 고정된 일반 grid. [SpriteVO.framesPerRow] 를 사용한다. */
    Grid,

    /** sprite-creator 의 binary tree packing 순서를 재현한 배치. */
    BinaryTree,

    /** 모든 프레임이 왼쪽에서 오른쪽으로 한 줄에 배치된 형태. */
    LeftRight,

    /** 모든 프레임이 위에서 아래로 한 줄에 배치된 형태. */
    TopDown,
}
