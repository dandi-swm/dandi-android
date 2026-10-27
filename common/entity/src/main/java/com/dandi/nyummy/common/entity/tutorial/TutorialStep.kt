package com.dandi.nyummy.common.entity.tutorial

/**
 * 첫 사용자 튜토리얼의 단계. 화면마다 따로 띄우고, 단계마다 마쳤는지를 기기에 남긴다.
 * 이름은 기기에 저장하는 값이므로 바꾸지 않는다.
 */
enum class TutorialStep {
    /** 홈 첫 진입 소개. 마지막에 카메라 버튼을 가리켜 첫 식사 기록으로 이끈다. */
    HOME_INTRO,

    /** 첫 식사를 기록하고 홈에 왔을 때 우편 버튼을 가리킨다. */
    HOME_MAIL_POINTER,

    /** 우편함 안내를 마치고 홈에 왔을 때 마이룸 탭을 가리킨다. */
    HOME_ROOM_POINTER,

    /** 우편함 첫 진입. 받지 않은 상자를 열어 보게 하거나, 없으면 우편함을 소개한다. */
    MAILBOX,

    /** 마이룸 첫 방문. 아이템을 놓고 저장해 보게 하거나, 없으면 꾸미기를 소개한다. */
    ROOM,
}
