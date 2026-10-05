

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
