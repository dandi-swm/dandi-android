package com.dandi.nyummy.common.domain.helper

/**
 * 그림을 화면에 나오기 전에 받아 앱 공용 이미지 디스크 캐시에 넣어 두는 통로.
 * 이미지 로더는 presentation 에만 있으므로 domain 은 이 인터페이스로 부른다.
 */
interface ImagePrefetchHelper {

    /**
     * 냐미 스프라이트 시트 [urls]를 디스크 캐시에 모두 받을 때까지 기다린다. 메모리에는 올리지 않는다.
     * 시트를 그리는 컴포넌트와 같은 주소로 받아, 화면이 그릴 때 네트워크 대신 디스크에서 읽게 한다.
     * 받지 못한 시트는 건너뛰고 알리지 않는다(화면이 그릴 때 다시 받는다).
     */
    suspend fun prefetchSpriteSheets(urls: List<String>)
}
