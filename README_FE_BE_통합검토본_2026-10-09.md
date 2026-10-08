# README — FE/BE 통합검토본

작성일: 2026-10-09 (KST). 적용 대상: 임시 integration/develop.

원본 `README.md`는 첫 BE 병합 상태를 그대로 유지한다. 이는 BE를 최신 제품 정본으로 선정한 것이 아니다. Front/Back/main의 정확한 원문은 `docs/integration/originals/`와 보존 manifest에서 확인한다. 원본 삭제·rename·추가 정책 결정은 하지 않는다.

- main: `dc139f43ce9e7cbe09d660202ee0ccec033b0f5f`
- front/develop: `108cb477322eaffdfb42c520a93a5e53794031ce`
- back/develop: `eb9a7d4a734a744731d7bb85564b32f3c8f3b39c`

## 통합 판단

FE 문서/ERD·유저플로우 링크와 BE 실행 골격·MVP 결정 변경 링크를 모두 보존한다. 통합 저장소에는 Next.js frontend와 Spring Boot backend가 모두 존재한다. 원문 초기화 미완료 문구는 출처 시점의 기록이며 현재 상태로 해석하지 않는다.

공통·비충돌 변경은 아래 검토 내용에 병합했다. `2`개 상충 구간은 Front/Back 원문을 병기하고 확인 필요로 남겼다. 상충 구간에 포함된 예전 확정/완료 표현은 그 branch의 주장이지 이번 통합의 승인 기록이 아니다. 상세 영향과 검증은 `docs/integration/Discushion_FE_BE_통합검증보고서_2026-10-09.md`를 따른다.

근거: #74(open), PR #84(FE 문서 동기화, merged), FE 후속 문서 commit 78c4d6c, PR #154(기관 계약, merged), Backend API 계약 검토표 §15~20 및 #18~20 결정. FE 범위 확대의 최종 양쪽 정책 합의 근거는 확인 필요.

## 병합 검토 내용

# Discushion

2026-10-07 현재 서비스 선택·MVP 변경은 [결정 변경 기록](docs/specs/Discushion_MVP_결정변경_2026-10-07.md)과 각 기준 문서의 같은 날짜 반영을 확인하세요. 계약/문서 후속은 #74, 시연용 계정 준비는 #75입니다. 기존 비밀번호/증빙 신청 예시는 현재 MVP 계약이 아닙니다. API 내부 버전은 v1.1.2이며 변경된 경로/DTO는 합의 대기입니다.

지역 주민이 지역 문제를 확인하고 의견을 나누며, 주민 참여와 기관의 지역 안건 확인·채택을 연결하는 지역 참여 서비스입니다.

## 저장소 구성

```text
frontend/  프론트엔드 애플리케이션 영역
backend/   백엔드 애플리케이션 영역
docs/      제품 기준·API·설계·협업 문서
```

현재 저장소에는 기준 문서와 개발 협업 자료, Spring Boot Backend 실행 골격이 있습니다. 실행·테스트·Supabase 연결·Vercel 배포 준비는 [Backend 안내](backend/README.md)를 확인하세요. Frontend 애플리케이션은 아직 초기화되지 않았습니다.

## 개발 기준

- 제품 요구사항과 MVP 범위는 `docs/specs/`의 PRD v10.2, 기능명세서 v10.2, MVP 통합 지침서를 따릅니다.

### 확인 필요 — 분기별 변경 1

이 부분은 양쪽 원문을 병기한다. 제품/API 정책을 확정한 내용이 아니다.

**Frontend 기준 원문**

````````text
- API 계약과 ERD 문서는 구현 전에 FE/BE가 검토·합의할 설계 자료입니다. 문서의 기술 제안이나 미확정 항목을 확정 정책으로 간주하지 않습니다.
````````

**Backend / 첫 병합 상태 원문**

````````text
- API 문서는 구현 전에 FE/BE가 검토·합의할 계약 초안입니다. 문서의 기술 제안이나 미확정 항목을 확정 정책으로 간주하지 않습니다.
````````

- 저장소 작업 지침은 루트의 [`AGENTS.md`](AGENTS.md)를 참고하세요.

## GitHub 협업 흐름

```text
main
├── front/develop
│   └── front/feature/<issue번호>-<기능명>
└── back/develop
    └── back/feature/<issue번호>-<기능명>
```

기능 작업은 GitHub Issue에서 시작하고, 담당 영역의 Feature 브랜치에서 작업한 뒤 해당 영역의 `develop`을 base로 Pull Request를 엽니다. Part A~D는 기능 분류이며 Part 이름의 장기 브랜치나 디렉터리를 만들지 않습니다. `main`과 두 `develop` 브랜치에는 직접 push하지 않습니다.

## 문서 안내

- [PRD v10.2](docs/specs/Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md) · [기능명세서 v10.2](docs/specs/Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md)
- [MVP FE/BE 통합 지침서](docs/specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md)

### 확인 필요 — 분기별 변경 2

이 부분은 양쪽 원문을 병기한다. 제품/API 정책을 확정한 내용이 아니다.

**Frontend 기준 원문**

````````text
- [와이어프레임 기반 유저플로우](docs/specs/Discushion_유저플로우_와이어프레임기반_2026-10-06.md)
- [MVP API 계약 초안](docs/api/Discushion_API_SPEC_v2.md) · [MVP ERD 상세명세](docs/specs/Discushion_MVP_ERD_상세명세.md)
````````

**Backend / 첫 병합 상태 원문**

````````text
- [MVP API 계약 초안](docs/api/Discushion_API_SPEC_v2.md)
- [현재 와이어프레임 기반 유저플로우](docs/specs/Discushion_유저플로우_와이어프레임기반_2026-10-06.md): 화면 흐름 참고 자료이며 제품 정책 정본을 대체하지 않습니다.
````````

- [통합 Git/GitHub 협업전략](docs/collaboration/Discushion_Git_GitHub_통합협업전략_2026-10-06.md) · [Frontend 협업전략](docs/collaboration/Discushion_프론트엔드_Git_GitHub_협업전략_2026-10-06_v2.md) · [Backend 협업전략](docs/collaboration/Discushion_백엔드_Git_GitHub_협업전략_2026-10-06.md)
- [문서 색인과 협업 기록 양식](docs/README.md)
