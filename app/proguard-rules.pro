

# 카카오 SDK — 오류 원인(ClientErrorCause 등)과 응답 모델을 리플렉션으로 읽는다.
# 없으면 release 빌드에서 로그인 취소·실패 시 크래시가 난다.
-keep class com.kakao.sdk.**.model.* { <fields>; }
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.**
