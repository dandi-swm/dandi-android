# 냐미(Nyummy) Android

단디 팀의 AI 식사 기록 앱 냐미(Nyummy)의 안드로이드 저장소. Codex 스킬과 서브에이전트 운영 규칙을 사용한다.

**작업을 시작하기 전에 [CLAUDE.md](CLAUDE.md)를 먼저 읽는다.** 프로젝트 소개, 현재 구현 현황, 빌드/테스트, 불변 규칙, 네이밍, 상세 문서, 스킬 인덱스는 모두 그 파일에 있고 Codex도 똑같이 따른다. 두 파일에 같은 내용을 따로 적으면 한쪽이 낡으므로, 이 파일에는 Codex에만 해당하는 내용만 적는다.

## Subagent 운영

- Kotlin/Gradle 기능 코드를 수정한 뒤 최종 보고 또는 커밋 전: `architecture-guardian`으로 변경 파일을 리뷰한다.
- `architecture-guardian`이 `HIGH` 또는 `MEDIUM`으로 보고한 항목은 수정한 뒤 `gradle-build-check`를 다시 실행한다. 심각도 기준은 `.codex/agents/architecture-guardian.toml`에 정의되어 있다.
- subagent를 사용할 수 없으면 같은 검토를 메인 에이전트가 직접 수행하고, 최종 보고에 미사용 사유를 적는다.
