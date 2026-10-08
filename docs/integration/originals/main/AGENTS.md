# Discushion 프로젝트 작업 지침

이 파일은 이 저장소에서 Codex와 개발자가 작업할 때 적용하는 루트 안내서다. 구체적인 제품 정책과 협업 절차는 아래 기준 문서를 확인한다.

## 기준 문서와 우선순위

제품 동작과 MVP 포함 여부는 다음 기준을 따른다.

1. PRD와 기능명세서의 확정 정책, 범위 대조, 기능 ID별 명세
2. [MVP 백엔드·프론트엔드 통합 지침서](docs/specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md)
3. [Git/GitHub 통합협업전략](docs/collaboration/Discushion_Git_GitHub_통합협업전략_2026-10-06.md)
4. Backend 작업이면 아래 [Backend 협업전략](docs/collaboration/Discushion_백엔드_Git_GitHub_협업전략_2026-10-06.md)을 공통 Git 전략과 함께 적용한다.

제품 기준 문서는 [PRD v10.1](docs/specs/Discushion_PRD_2026-10-06_MVP반영_정리본_v10.1.md)과 [기능명세서 v10.1](docs/specs/Discushion_기능명세서_2026-10-06_MVP반영_정리본_v10.1.md)이다. 문서 간 정책 충돌이나 모호함을 발견하면 임의로 결정하지 말고 영향을 받는 작업을 보류해 확인한다. 기술 권장안과 확인 필요 항목을 확정 제품 정책으로 취급하지 않는다.

## 작업 범위

- 요청된 세부 작업과 관련 기능명세 ID, MVP 포함 범위, API 계약을 먼저 확인한다.
- 승인된 범위의 파일만 바꾼다. 관련 없는 리팩터링, 전역 포맷팅, 타 Part 소유 파일 변경은 하지 않는다.
- 저장소의 현재 구조와 Git 상태를 확인한다. 보존해야 할 미커밋 변경이 있으면 덮어쓰거나 초기화하지 않는다.
- 기술 스택, 실행·테스트 명령, API 경로, DTO, DB 구조가 정해지지 않았다면 추측해 만들지 않는다.
- 기능 코드 변경은 작은 작업 단위로 검토하고, 실제 저장소에 구성된 검증만 실행한다. 실행하지 않은 검증은 통과로 기록하지 않는다.
- 사람이 `git status`, `git diff`, `git diff --cached`와 새 파일을 검토할 수 있게 변경 결과와 실행한 검증을 보고한다.

## 제품·API 경계

- MVP에는 명세에서 포함으로 표시한 범위만 구현한다. 비-MVP 기능을 추측해 추가하지 않는다.
- 이웃 인증 완료 지역, 유효한 기관 인증과 담당 지역, 게스트 공유 상세 접근 등 권한은 명세대로 구분한다. 인증 접수와 완료를 혼동하지 않는다.
- 실제 API 명세를 먼저 확인하고 FE/BE가 Method, Endpoint, Request, Response, 오류, 인증·권한을 합의한다. 명세가 없으면 존재하지 않는 계약을 만들어 내지 않는다.
- API 계약 변경은 제안자가 BE1과 협의한다. BE1이 API 정본의 정합성을 확인하고 명세를 갱신한 뒤 FE 담당자에게 공유한다.
- Entity, Column, Relation 변경은 BE1이 ERD와 기존 DB 영향을 확인한다. BE1이 Schema/Migration을 관리한다.
- 배포, Docker, 실행환경, 서버 설정, 환경변수, CORS 및 FE/BE 배포 연동은 BE2와 협의한다. 운영 DB 연결·실행은 BE2, 데이터 모델은 BE1 책임이다.
- 실제 secret, password, API key, 인증 증빙 자료를 저장소에 추가하거나 commit하지 않는다.

## Part 담당 경계

같은 Part 문자는 FE/BE에서 같은 제품 영역을 뜻한다. Part는 기능 분류이며 Git 브랜치나 디렉터리가 아니다.

| Part | 제품 영역 | Backend 담당 |
| --- | --- | --- |
| A | 계정·프로필·지역·인증 | BE1 |
| B | 콘텐츠 탐색·게시물 관리 | BE2 |
| C | 주민 참여·공유·게스트 | BE2 |
| D | 개인 기록·기관 안건 업무 | BE1 |

BE1과 BE2는 자기 Part의 기능과 API를 구현한다. BE1은 API 명세 정본·계약 정합성·ERD·DB Schema/Migration의 전역 Owner다. BE2는 배포·실행환경의 전역 Owner이며 배포 전담자가 아니다. 세부 운영은 [Backend 협업전략](docs/collaboration/Discushion_백엔드_Git_GitHub_협업전략_2026-10-06.md)을 따른다.

## GitHub 협업 방식

통합 및 Backend 기준 브랜치 구조:

```text
main
├── front/develop
│   └── front/feature/<issue번호>-<기능명>
└── back/develop
    └── back/feature/<issue번호>-<기능명>
```

- 일반 기능 작업은 GitHub Issue에서 시작한다. 기본 단위는 Issue 하나, Feature 브랜치 하나, PR 하나다.
- FE Feature는 최신 `front/develop`, BE Feature는 최신 `back/develop`에서 시작하고 자기 영역 develop을 PR base로 사용한다.
- `main`, `front/develop`, `back/develop`에 직접 push하거나 로컬 병합 후 push하지 않는다. 검증과 필수 리뷰·CI를 거쳐 PR로 통합한다.
- FE와 BE Feature 브랜치를 직접 병합하지 않는다. 실제 API 계약과 합의된 실행 환경으로 연동한다.
- `develop` 단일 브랜치, `release`, Part 장기 브랜치, `feature/fe-*`, `feature/be-*`, `frontend-part-*`, `backend-part-*` 브랜치 이름을 만들지 않는다.
- 브랜치 생성·전환·commit·push·PR 병합 요청은 현재 저장소 상태와 사용자의 허용 범위를 확인하고, 협업전략의 순서를 따른다. 공유 이력을 force push, reset, rebase로 덮어쓰지 않는다.
- Codex는 요청된 작업 범위에서 변경안을 준비하고 검증 결과를 보고한다. 별도 요청이 없다면 develop/main 통합, PR 병합, 배포를 수행하지 않는다.

각 영역의 Feature에서 실제 설정에 맞는 테스트와 build를 실행하고 결과를 기록한다. FE/BE 연동, 영역 전체 검증 및 배포 준비가 끝난 뒤 `front/develop`과 `back/develop`을 각각 검토·CI를 거쳐 `main`에 반영한다. 두 번째 PR은 첫 번째 반영으로 바뀐 최신 `main`을 기준으로 차이와 충돌을 다시 확인한다.

## Backend 세부 작업 흐름

1. Issue, 기능명세 ID, API/DB 계약, 담당자, 완료 조건과 현재 Git 상태를 확인한다.
2. 최신 `origin/back/develop`을 확인하고 그 기준에서 `back/feature/<issue번호>-<기능명>`을 사용한다.
3. 해당 Issue 범위의 코드와 테스트만 변경하고, API·DB·실행환경에 영향이 있으면 전역 Owner와 합의한다.
4. 변경 내용을 확인하고 저장소에 실제 구성된 Backend 테스트와 build를 실행한다.
5. Feature를 push하고 `back/develop`을 base로 PR을 준비한다. Issue, 변경 요약, 기능명세 ID, API/DB/FE 영향, 검증 결과를 연결한다.
6. 필수 리뷰와 자동 검증이 통과한 경우 GitHub PR에서 병합한다. 검증 실패나 해결되지 않은 충돌이 있으면 병합을 멈추고 담당자와 확인한다.

BE1은 Part A·D, BE2는 Part B·C와 배포·실행환경을 담당한다. API를 각 Part 담당자가 구현하더라도 계약 정본은 BE1과 협의한다. DB 변경은 제안자와 BE1이 검토하고 Schema/Migration 정합성을 유지한다.

## 안전한 작업과 보고

- 충돌을 무조건 `ours` 또는 `theirs`로 선택하지 않는다. 명세, 계약, 파일 담당자 기준으로 판단하고 판단할 수 없으면 작업을 보류한다.
- 미커밋 변경, 실패한 테스트, 원격 이력 분기 또는 예기치 않은 원격 브랜치를 숨기거나 강제로 덮지 않는다.
- 테스트·build 명령이 저장소에서 확인되지 않으면 임의로 성공을 주장하지 말고 미검증 사유를 보고한다.
- 작업 결과에는 변경 파일, 구현·미구현 범위, 실제 실행한 검사 결과, 남은 의존성이나 확인 필요 사항을 간단히 적는다.
