

# 카카오 SDK — 오류 원인(ClientErrorCause 등)과 응답 모델을 리플렉션으로 읽는다.
# 없으면 release 빌드에서 로그인 취소·실패 시 크래시가 난다.
-keep class com.kakao.sdk.**.model.* { <fields>; }
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.**

# Credential Manager(구글 로그인) — Play 서비스 구현체를 리플렉션으로 찾는다.
# 없으면 release 빌드에서 구글 로그인이 "제공자 없음"으로 실패한다.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# Crashlytics: 난독화된 크래시 스택에 파일 이름과 줄 번호를 남긴다.
-keepattributes SourceFile,LineNumberTable
# 앱이 정의한 예외는 이름만 그대로 둔다(콘솔 이슈 제목). 쓰지 않는 예외는 지워지도록 -keep 대신 -keepnames, 범위는 앱 패키지로 한정한다.
-keepnames class com.dandi.nyummy.** extends java.lang.Exception
