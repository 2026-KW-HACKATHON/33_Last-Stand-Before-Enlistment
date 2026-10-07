# Discushion 프로젝트 작업 지침

이 파일은 이 저장소에서 Codex와 개발자가 작업할 때 적용하는 루트 안내서다. 구체적인 제품 정책과 협업 절차는 아래 기준 문서를 확인한다.

## 기준 문서와 우선순위

제품 동작과 MVP 포함 여부는 다음 기준을 따른다.

1. PRD와 기능명세서의 확정 정책, 범위 대조, 기능 ID별 명세
2. [MVP 백엔드·프론트엔드 통합 지침서](docs/specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md)
3. [Git/GitHub 통합협업전략](docs/collaboration/Discushion_Git_GitHub_통합협업전략_2026-10-06.md)
4. Backend 작업이면 아래 [Backend 협업전략](docs/collaboration/Discushion_백엔드_Git_GitHub_협업전략_2026-10-06.md)을 공통 Git 전략과 함께 적용한다.

제품 기준 문서는 [PRD v10.2](docs/specs/Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md)과 [기능명세서 v10.2](docs/specs/Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md)이다. 문서 간 정책 충돌이나 모호함을 발견하면 임의로 결정하지 말고 영향을 받는 작업을 보류해 확인한다. 기술 권장안과 확인 필요 항목을 확정 제품 정책으로 취급하지 않는다.

## 작업 범위

- 요청된 세부 작업과 관련 기능명세 ID, MVP 포함 범위, API 계약을 먼저 확인한다.
- 승인된 범위의 파일만 바꾼다. 관련 없는 리팩터링, 전역 포맷팅, 타 Part 소유 파일 변경은 하지 않는다.
- 저장소의 현재 구조와 Git 상태를 확인한다. 보존해야 할 미커밋 변경이 있으면 덮어쓰거나 초기화하지 않는다.
- 기술 스택, 실행·테스트 명령, API 경로, DTO, DB 구조가 정해지지 않았다면 추측해 만들지 않는다.
- 기능 코드 변경은 작은 작업 단위로 검토하고, 실제 저장소에 구성된 검증만 실행한다. 실행하지 않은 검증은 통과로 기록하지 않는다.
- 사람이 `git status`, `git diff`, `git diff --cached`와 새 파일을 검토할 수 있게 변경 결과와 실행한 검증을 보고한다.

## 문서 수정 규칙

- 기존 문서를 수정할 때는 수정본을 새로운 문서로 만들지 않고, 기존 파일의 내용만 갱신한다.
- 문서를 수정하더라도 파일명과 경로를 유지한다. 개정일이나 버전 변경을 이유로 파일명을 변경하지 않는다.
- 문서 변경 이력은 Git commit과 PR로 관리한다.

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

# Frontend 추가 작업 지침

이 절은 기존 `AGENTS.md`의 공통 지침을 대체하지 않는다.

Frontend 코드, UI, Route, 상태, 공통 컴포넌트 또는 디자인 시스템을 작업할 때 기존 지침과 함께 적용한다.


## Frontend 필수 참조 문서

Frontend 작업을 시작하기 전에 현재 Issue와 관련된 다음 문서를 확인한다.


### 제품 정책 / MVP / 기능

- `docs/specs/Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md`
- `docs/specs/Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md`
- `docs/specs/Discushion_유저플로우_와이어프레임기반_2026-10-06.md`


### FE/BE 통합

- `docs/specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md`


### Frontend 전체 구현 기준

- `docs/frontend/Discushion_MVP_프론트엔드_개발_상세지침서.md`


### 디자인

- `docs/design/Discushion_최종디자인_디자인기준 v2.md`
- Figma `KW 해커톤 디자인`의 `최종디자인` 페이지
- `https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1136-6698`


### Frontend 2인 병렬 개발

- `docs/collaboration/Discushion_프론트엔드_2인_개발시작전_합의사항_병렬개발.md`
- `docs/collaboration/Discushion_프론트엔드_2인_담당분배_병렬개발순서.md`


### Frontend Git / GitHub

- `docs/collaboration/Discushion_프론트엔드_Git_GitHub_협업전략_2026-10-06_v2.md`
- `docs/collaboration/Discushion_Git_GitHub_통합협업전략_2026-10-06.md`


### API

- `docs/api/Discushion_API_SPEC_v2.md`


현재 Issue와 관련된 범위, 기능 ID, 화면, Contract, API, 공통 컴포넌트를 중심으로 필요한 내용을 확인한다.

문서에 예시로 적힌 Route, 파일명, DTO, 라이브러리 또는 명령을 실제 저장소 확인 없이 확정 구현하지 않는다.


---

## Frontend 판단 우선순위

판단 종류에 따라 다음 기준을 사용한다.

1. 기능 동작·권한·MVP 포함 여부: PRD + 기능명세서
2. FE/BE 책임·통합 기준: MVP 백엔드·프론트엔드 통합 지침서
3. Git·Issue·Branch·Commit·PR·Merge: Git/GitHub 통합협업전략
4. Frontend 구현 구조·상태·화면 개발 방식: MVP 프론트엔드 개발 상세지침서
5. 화면의 시각적 외형: Figma `최종디자인`
6. 색상·Typography·Spacing·Radius·Shadow·공통 UI 규격: 최종디자인 디자인기준 v2
7. 담당 분배·병렬 개발·Contract·Mock/Integration: Frontend 2인 병렬개발 문서
8. API: API SPEC v2 + 실제 FE/BE 합의 + 현재 구현
9. 실제 파일 구조·패키지·실행 명령: 현재 저장소


기능 정책과 디자인이 충돌하면 기능·권한·MVP 포함 여부는 PRD와 기능명세서를 따른다.

시각적 구현은 Figma `최종디자인`과 디자인기준 v2를 따른다.

Figma에 존재한다는 이유만으로 비-MVP 기능을 구현하지 않는다.

API SPEC에 존재한다는 이유만으로 실제 Backend에 이미 구현되어 있다고 가정하지 않는다.

같은 우선순위의 기준끼리 충돌해 판단할 수 없으면 임의 구현하지 않고 확인 필요 사항으로 보고한다.


---

## Frontend 세부 작업 흐름

1. Issue, 관련 기능명세 ID, 담당자, 완료 조건, 대상 Figma Frame과 현재 Git 상태를 확인한다.

2. 최신 `origin/front/develop`을 확인하고 그 기준에서 `front/feature/<issue번호>-<기능명>`을 사용한다.

3. 구현 전에 현재 Issue와 관련된 Frontend 기준 문서, Figma `최종디자인`, 기존 공통 컴포넌트, 디자인 토큰, Contract/API 및 파일 Owner를 확인한다.

4. 해당 Issue 범위의 Frontend 코드와 관련 검증만 변경한다. 다른 Issue·Part 기능, 무관한 리팩터링, 전역 포맷팅, Backend 코드 변경은 포함하지 않는다.

5. 상대 Frontend 또는 Backend 구현이 아직 준비되지 않았더라도 합의된 Contract가 있으면 Mock 또는 개발용 Stub으로 독립 구현할 수 있다. Mock 성공을 실 API 연동 완료로 기록하지 않는다.

6. 공통 UI 또는 공유 파일 변경이 필요한 경우 기존 Owner와 영향 범위를 확인한다. 상대 Feature Branch를 직접 merge하지 않고, 필요한 공통 변경은 별도 Issue/PR로 `front/develop`에 먼저 통합한 뒤 사용한다.

7. UI 구현 후 가능한 경우 대상 Figma Frame과 `396 × 852px` 기준으로 비교하여 구조, 정렬, 크기, 간격, Typography, 색상, Border, Radius, Shadow, Icon, Image, Header, BottomNavigation 및 Scroll 영역을 확인한다.

8. 변경 내용을 확인하고 저장소에 실제 구성된 Frontend lint, typecheck, test, build 중 관련 검증을 실행한다. 존재하지 않는 명령을 임의로 만들거나 실행하지 않은 검증을 통과로 기록하지 않는다.

9. 구현과 검증이 끝나면 Feature의 변경 내용과 PR에 포함해야 할 정보를 정리한다. 사용자가 명시적으로 요청한 경우에만 Feature를 push하거나 `front/develop`을 base로 PR을 생성한다. PR에는 Issue, 변경 요약, 기능명세 ID, 대상 Figma Frame, 공통 컴포넌트 영향, API/Contract 영향, Mock/실 API 연동 상태와 검증 결과를 기록한다.

10. PR 병합은 필수 리뷰와 자동 검증이 통과한 뒤 담당자가 수행한다. Codex는 사용자가 명시적으로 요청하지 않는 한 PR을 병합하지 않는다. 검증 실패, 해결되지 않은 충돌, 디자인·정책·API 계약 충돌이 있으면 병합하지 않고 관련 담당자에게 확인 필요 사항을 보고한다.


Codex는 별도 요청이 없는 한 commit, push, PR 생성, PR 병합, `front/develop` 또는 `main` 반영을 수행하지 않는다.

Git 변경 작업을 요청받은 경우에도 현재 Branch와 `git status`를 먼저 확인하고 기존 Git/GitHub 협업 규칙을 따른다.


Frontend 2인 담당 기준은 최신 병렬개발 문서를 따른다.

- FE1: Part A·D 중심 — Account / Authority / Personal / Institution
- FE2: Part B·C 중심 — Content / Post / Participation / Share


Part는 기능 분류이며 장기 Git Branch가 아니다.


---

## Frontend 구현 원칙

Frontend 구현은 가능한 한 다음 순서를 따른다.

`Design Token → Common UI → Domain Component → Screen → Page / Route`

- 기존 공통 컴포넌트와 디자인 토큰을 먼저 확인하고 재사용한다.
- 같은 역할의 UI를 Page마다 중복 구현하지 않는다.
- 공통 색상, Typography, Spacing, Radius, Shadow 값을 화면마다 반복해서 임의 하드코딩하지 않는다.
- 특정 화면의 예외값을 전역 디자인 규칙으로 임의 승격하지 않는다.
- 현재 Issue와 관계없는 기능이나 리팩터링을 함께 처리하지 않는다.


---

## Figma 구현 원칙

Frontend 화면의 시각적 Source of Truth는 Figma `최종디자인`이다.

가능한 경우 구현 전에 현재 Issue의 대상 Figma Frame을 직접 확인한다.

디자인을 임의로 개선하거나 재해석하지 않는다.

다음 항목을 Figma와 최대한 동일하게 구현한다.

- 화면 구조와 요소 순서
- 정렬
- width / height
- padding / margin / gap
- Typography
- font size / weight / line-height
- color
- border / radius / shadow
- icon 크기와 위치
- image 크기와 비율
- Header
- BottomNavigation
- 카드 / 입력 / 버튼 구조
- Scroll 영역


기준 모바일 비교 크기는 `396 × 852px`이다.

단, 실제 웹 Page 전체 높이를 `852px`로 고정하지 않는다.

구현 후 가능한 경우 Figma와 실제 화면을 비교하고 시각적 차이를 Styling/Layout 범위에서 수정한다.


### Figma 접근 실패 시

Figma에 접근할 수 없는 경우 비슷한 디자인을 임의로 추측하거나 새로 만들지 않는다.

다음 자료를 기준으로 구현한다.

- `docs/design/Discushion_최종디자인_디자인기준 v2.md`
- `docs/frontend/Discushion_MVP_프론트엔드_개발_상세지침서.md`
- 기존 공통 컴포넌트
- 현재 Issue에 첨부된 화면 자료


Figma를 직접 확인하지 못했다면 작업 결과에 명시한다.


---

## 병렬 개발 / Contract / Mock 원칙

상대 Frontend Feature 또는 Backend 구현 완료를 전체 개발의 선행조건으로 삼지 않는다.

합의된 Contract를 기준으로 독립 개발 가능한 작업은 병렬로 진행한다.

필요하면 동일 Contract를 따르는 Mock 또는 개발용 Stub을 사용할 수 있다.

단 다음을 지킨다.

- 실제 저장소에 기존 Contract가 있으면 그것을 우선한다.
- 화면 Component 내부에 임의 Fixture를 흩뿌리지 않는다.
- Mock용 타입과 실제 API 타입을 별도 정본으로 만들지 않는다.
- Mock 성공을 실제 API 연동 완료라고 기록하지 않는다.
- 상대 Feature Branch를 직접 merge하지 않는다.
- API 또는 공통 Contract를 현재 Feature에서 독자적으로 변경하지 않는다.


실제 API 또는 Provider가 준비되면 별도의 작은 Integration 작업으로 연결한다.


---

## 정본 문서와 공통 설정 변경 제한

일반 기능 Issue를 수행하는 과정에서 다음 파일 또는 설정을 편의상 임의로 수정하지 않는다.

- `AGENTS.md`
- PRD
- 기능명세서
- API SPEC
- MVP 통합 지침서
- Git/GitHub 협업전략
- Frontend 병렬개발 문서
- 디자인 기준 문서
- 공통 CI / Workflow
- 전역 Dependency / Lockfile
- 공통 Theme / Design Token


현재 Issue 완료를 위해 변경이 반드시 필요하면 영향 범위를 확인하고 별도 Issue/PR 또는 명시적인 요청을 기준으로 처리한다.

기능 구현 편의를 위해 정본 문서나 공통 계약을 현재 코드에 맞게 임의 수정하지 않는다.


---

## Frontend Issue 범위

Codex 작업은 기본적으로 한 번에 Issue 하나를 대상으로 한다.

현재 Issue와 관계없는 다음 작업을 임의로 포함하지 않는다.

- 다른 Issue 기능
- 다른 Part 기능
- 비-MVP 기능
- 무관한 Refactoring
- 전역 Formatting
- 불필요한 Dependency 추가
- API Contract 독자 변경
- Backend 코드 변경


현재 Issue를 구현하면서 발견한 별도 문제는 필요하면 작업 결과에 보고하고 후속 Issue 대상으로 남긴다.


---

## Frontend 검증

작업 완료 전에 현재 저장소에 실제 존재하는 검증 Script와 명령을 확인한다.

관련 Script가 실제로 존재하면 작업 범위에 따라 다음 검증을 수행한다.

- lint
- typecheck
- test
- build


존재하지 않는 명령을 추측해서 실행하지 않는다.

실행하지 않은 검증을 통과했다고 기록하지 않는다.

실패한 검증을 숨기지 않는다.

가능하면 기존 실패와 이번 변경으로 새로 발생한 실패를 구분한다.


---

## 작업 완료 보고

Frontend 작업 종료 후 최소한 다음 내용을 보고한다.

```text
작업 Issue:
현재 Branch:

변경 파일:
- ...

구현:
- ...

미구현 / Issue 범위 밖:
- ...

Figma:
- 대상 Frame:
- 직접 확인 여부:
- 비교 결과:

API / Contract:
- Mock 여부:
- 실 API 연결 여부:
- Contract 변경 여부:

완료 상태:
- UI 완료:
- Mock 연동 완료:
- 실 API 연동 완료:
- Integration 검증 완료:

실행한 검증:
- 명령:
- 결과:

남은 확인 사항:
- ...
```

실행하지 않은 검증을 실행했다고 기록하지 않는다.

Mock으로만 확인한 기능을 실 API 연동 완료라고 기록하지 않는다.

Figma를 직접 확인하지 않았다면 Figma 직접 비교 완료라고 기록하지 않는다.