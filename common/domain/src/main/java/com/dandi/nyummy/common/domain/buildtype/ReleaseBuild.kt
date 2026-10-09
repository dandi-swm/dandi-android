package com.dandi.nyummy.common.domain.buildtype

import javax.inject.Qualifier

/**
 * 스토어에 올리는 release 빌드인지(Boolean). 앱 모듈이 빌드 타입 이름으로 정해 주입한다.
 *
 * debug 뿐 아니라 benchmark, 베이스라인 프로파일 수집용 빌드도 false 다. 이 빌드들은 debuggable 이 아니어서
 * `BuildConfig.DEBUG`(라이브러리 모듈)나 `FLAG_DEBUGGABLE` 로는 release 와 구분되지 않는다.
 * 실사용자 지표(TTI, 버벅임, 이미지 로딩)를 외부로 보낼지는 이 값으로만 정한다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ReleaseBuild
