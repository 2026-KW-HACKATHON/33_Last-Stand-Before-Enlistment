# Discushion Git & GitHub 협업 전략

> 작성일: 2026-10-06 · 팀 개발 실무 가이드
> 제품 기준: Discushion PRD 및 기능명세서 v10.2 — 2026-10-07 MVP 결정 반영 정리본 (협업전략의 Git 절차 개정 아님)
> 운영 방식: GitHub Issue → 영역별 Feature 브랜치 → Pull Request → 영역별 develop

## 1. 목적

팀원이 작업을 시작하고 검증한 코드를 공유·통합하는 순서를 정한다. 작업 관리는 GitHub Issue로, 코드 통합은 Feature Pull Request(PR)로 진행한다. Frontend와 Backend는 각각 독립된 통합 브랜치를 사용한다. 제품 동작과 MVP 범위는 PRD, 기능명세서 및 `Discushion_MVP_백엔드_프론트엔드_통합_지침서.md`를 따른다. 이 문서는 제품 정책을 변경하지 않는다.

## 2. 핵심 원칙

1. 일반 개발은 GitHub Issue에서 시작한다.
2. 기본 작업 단위는 **1 Issue → 1 Feature 브랜치 → 1 PR → 담당 영역 develop**이다.
3. Frontend는 `front/develop`, Backend는 `back/develop`을 사용한다.
4. Feature는 항상 해당 영역의 최신 develop에서 만든다.
5. `front/develop`, `back/develop`, `main`에 직접 push/merge하지 않고 PR만 사용한다.
6. `release` 브랜치와 기존 Part 장기 브랜치를 사용하지 않는다.
7. FE/BE Feature는 각자 영역의 develop으로만 통합한다. 다른 영역 Feature를 직접 merge하지 않는다.
8. FE/BE 연동은 Git merge 대신 합의한 API 계약으로 검증한다.
9. PR 전 담당자가 변경 범위와 test/build를 확인한다. 필수 자동 검증과 대상 브랜치·변경 영향에 따라 필요한 리뷰를 통과한 PR만 병합한다.

```text
Issue → 최신 영역 develop에서 Feature 생성 → 구현/test/build → push
→ 영역 develop 대상 PR → 필요한 리뷰/CI → merge
→ 최종 검증 후 front/develop 및 back/develop을 각각 main으로 PR
```

## 3. 브랜치 구조

```text
main
├── front/develop
│   └── front/feature/<issue번호>-<기능명>
└── back/develop
    └── back/feature/<issue번호>-<기능명>
```

- 단일 `develop`은 사용하지 않는다. FE 통합 브랜치는 `front/develop`, BE 통합 브랜치는 `back/develop`이다.
- 브랜치 이름 형식은 `<영역>/feature/<issue번호>-<기능명>`이며 영역은 `front` 또는 `back`이다. 예: `front/feature/31-comment`, `back/feature/32-comment`.
- `feature/fe-*`, `feature/be-*`, `frontend-part-*`, `backend-part-*`, `release`는 사용하지 않는다.
- `main`, `front/develop`, `back/develop`은 팀 공용 보호 브랜치다. 저장소 관리자가 `main`에서 두 develop을 최초 생성하고 보호 규칙을 설정한다.

## 4. Part A~D 제품 영역과 담당

같은 Part 문자는 FE/BE에서 같은 제품 영역을 뜻한다. Part는 기능 분류와 협업 추적 단위이며 장기 Git 브랜치가 아니다. 기존 범위와 기능명세 ID는 그대로 사용한다.

| Part | 제품 영역 | 주요 범위 | 연결 기능명세 ID |
| --- | --- | --- | --- |
| A — Account & Authority | 계정·프로필·지역·인증 | 회원가입, 로그인, 시작/공통 메뉴, 프로필, 활동 지역, 이웃 인증, 기관 인증 및 인증 상태·권한·파란 배지 | `F-WLXSSC`, `F-TSOXGG`, `F-KZRSXU`, `F-RBVFZX`, `F-QQKYLC`, `F-ATWJDJ`, `F-OPNIXL`, `S-YLSPHQ`, `S-JRMYIV`, `F-GDASNA`, `F-MUBDJD` |
| B — Content & Post | 콘텐츠 탐색·게시물 관리 | 메인, 통합 게시판, 공통 상세, 지역 안건, 지역 활동 정보, 투표 게시물 작성·관리, 사진, AI 안건 요약, 이슈 지도 | `F-UPRLMN`, `F-EAJPVC`, `F-PUDHYO`, `F-UCDVNA`, `F-FTLHCX`, `S-NVXXYQ`, `S-TBFIHO`, `F-GSMCLD`, `F-WSCKDN`, `F-QIGKAK` |
| C — Participation | 주민 참여·공유·게스트 | 세 게시물 반응, 댓글·답글, 댓글 평가·정렬, 실제 투표 참여·선택 변경, 공유 링크, 게스트 접근 | `F-GOMLGG`, `S-HNVDPO`, `F-EDNVWZ`, `S-JCEZAP`, `S-YYDGUS`, `F-CDIBRF`, `S-OXTKEP`, `F-FCPVIS`, `S-CMGJIG`, `F-OWFYWE`, `S-NYUECP` |
| D — Personal & Institution | 개인 기록·기관 안건 업무 | 북마크, 마이페이지, 개인 활동 횟수, 내가 만든/참여한 게시물, 참여한 투표, 기관 담당자 안건 목록, 채택·취소 | `F-FYQJPT`, `F-WYMXXP`, `F-SSHXAA`, `F-NZTUYE`, `F-QPGNCF`, `F-CNNPYL`, `S-PCCNUU`, `F-TUGMEP`, `S-AQOBIE` |

### Backend 인원별 책임

| 담당자 | 기능 구현 | 추가 전역 책임 |
| --- | --- | --- |
| BE1 | Part A, Part C, Part D | 공통 인증·자격 조회, API/ERD/Schema/Migration 전체 정합성 검토 |
| BE2 | Part B | 배포, 실행환경, Docker, 서버/환경설정 |

- BE1과 BE2 모두 기능 개발자이며 자신의 Part 기능과 API를 직접 구현한다.
- 각 담당자가 자기 영역 API 명세·ERD·Migration을 작성하고 BE1은 전체 정합성을 검토한다. 공통·타 영역·합의된 계약 변경은 사전 공동 검토한다.
- BE2는 배포·실행환경 전역 Owner이며 Part B 기능도 직접 구현한다. Docker, 서버 설정, 환경변수, CORS, 배포 연동 등은 BE2와 협의한다.

### Part 경계와 선행 작업

- 인증·지역 참여 자격 및 기관 인증: BE1 / Part A.
- 게시물 원본·탐색: BE2 / Part B.
- 주민 참여·공유·게스트: BE1 / Part C.
- 개인 기록·기관 안건 목록 및 채택: BE1 / Part D.
- FE/BE 선행 작업과 API 계약은 Issue와 협업 문서에서 서로 연결한다.

## 5. 브랜치 역할과 PR 대상

| 브랜치 | 담당 | 역할 | 직접 push |
| --- | --- | --- | --- |
| `main` | 팀 공용 | 최종 검증된 FE/BE 통합 상태 | 금지, PR만 허용 |
| `front/develop` | Frontend 팀 | 검증된 Frontend Feature 통합 | 금지, PR만 허용 |
| `back/develop` | Backend 팀 | 검증된 Backend Feature 통합 | 금지, PR만 허용 |
| `front/feature/*` | Frontend Issue 담당자 | 해당 Issue의 FE 구현 | 허용 |
| `back/feature/*` | Backend Issue 담당자 | 해당 Issue의 BE 구현 | 허용 |

Feature PR base는 FE `front/develop`, BE `back/develop`이다. 두 develop의 최종 PR base는 `main`이다. Feature는 PR 병합 뒤 삭제한다.

## 6. Issue와 작업 관리

모든 Feature 작업은 Issue로 등록한다. 기본 규칙은 Issue 하나에 기능 범위와 완료 조건 하나, 그에 대응하는 Feature 브랜치 하나와 PR 하나를 둔다.

Issue에는 영역/요약, 세부 범위, 완료 조건, 관련 기능명세 ID, API 계약·FE/BE 연동 여부, 테스트 방법, 담당자와 의존 작업을 기록한다. 예: `#32 [BE] 댓글/답글 작성` → `back/feature/32-comment` → PR base `back/develop`. PR은 Issue에 연결하고 병합 후 Issue를 닫는다.

별도 협업 문서는 API 계약과 FE/BE 연동, 검증 이력을 보충 기록할 수 있으며 Issue와 PR을 대체하지 않는다.

## 7. 개발 시작 절차

Issue, FE/BE 영역, 완료 조건, 선행 API 계약을 확인한다. 작업 폴더에 보존해야 할 미커밋 변경이 없는지 확인한 후 올바른 영역 develop을 최신화하고 Feature를 만든다.

Frontend 예시:

```bash
git status
git fetch origin
git switch front/develop
git pull --ff-only origin front/develop
git switch -c front/feature/31-comment
git push -u origin front/feature/31-comment
```

Backend 예시:

```bash
git status
git fetch origin
git switch back/develop
git pull --ff-only origin back/develop
# 실제 Backend test/build로 기준 상태를 검증한 뒤 다음 단계 진행
git switch -c back/feature/32-comment
git push -u origin back/feature/32-comment
```

Backend는 pull 직후 실제 구성된 test/build로 기준 상태를 검증하고 통과한 뒤 Feature를 만든다. 이력 분기·실패·검증 환경 누락은 보고하고 임의 성공 처리하지 않는다.

기존 Feature는 `git switch <브랜치명>`으로 연다. FE Feature는 `front/develop`, BE Feature는 `back/develop`에서만 시작한다.

## 8. Feature 브랜치 개발

Issue의 작업을 구현하고 의미 있는 단위로 commit한다. 개발 중 해당 영역 develop이 변경되면 Feature에 최신 통합 변경을 반영하고 다시 검증한다. FE/BE Feature끼리는 직접 merge하지 않는다. API가 아직 준비되지 않은 경우 계약을 먼저 합의하고 그 계약을 기준으로 병렬 개발한다.

최신 영역 develop 반영 예시:

```bash
git fetch origin
git switch front/feature/31-comment
git merge origin/front/develop
```

Backend에서는 Feature와 `origin/back/develop`을 사용한다. 충돌 해결은 15절을 따른다.

## 9. Commit 규칙

형식은 `<type>(<scope>): <작업 내용>`이다. type은 `feat`, `fix`, `refactor`, `test`, `docs`, `chore`; scope는 `fe`, `be`, `common`을 사용한다.

```text
feat(fe): 댓글 UI 구현
feat(be): 댓글 및 답글 작성 구현
fix(fe): 투표 선택 상태 오류 수정
refactor(be): 게시물 조회 로직 정리
test(be): 투표 참여 테스트 추가
docs(common): 협업 문서 수정
chore(be): 공통 설정 수정
```

`update`, `수정`, `작업함`처럼 내용을 알 수 없는 메시지는 금지한다. 원격에 공유한 commit은 amend/rebase로 다시 쓰지 않고 후속 commit을 추가한다.

## 10. Codex 사용 규칙

Codex 요청은 Issue 한 개의 작업 범위로 제한한다. Issue 번호, FE/BE 담당자, 현재 Feature 브랜치, base develop, 기능명세/API, 수정 허용·금지 범위, 완료 조건을 적는다.

Backend 예시:

```text
담당자: BE1
현재 Issue: #32 [BE] 댓글/답글 작성 구현
현재 브랜치: back/feature/32-comment
Base Branch: back/develop
관련 기능명세/API: Issue에 연결된 ID와 계약
수정 범위: Issue 완료 조건에 필요한 Backend 코드와 테스트
수정 금지: 다른 Issue/Part, 무관한 리팩터링, 임의 브랜치 이동, develop/main merge·push
완료 조건: 인수 기준, 테스트/build, 변경 파일과 결과 보고
```

Frontend 예시는 `#33 [FE] 댓글 UI 구현`, `front/feature/33-comment`, base `front/develop`을 사용한다. 작업 후 사람이 `git status`, `git diff`, `git diff --cached`를 검토한다. Codex는 develop/main에 직접 push하지 않는다. PR 생성 요청만으로 병합하지 않으며, 사용자가 병합까지 명시적으로 요청한 경우에만 대상 브랜치의 최종 검토·CI·필요한 승인 조건을 확인해 GitHub PR에서 병합한다.

## 11. PR 생성과 병합

1. Issue 범위, 기능명세, 완료 조건을 확인한다.
2. Feature가 올바른 최신 영역 develop에서 시작했는지 확인한다.
3. Feature에서 관련 test/build를 실행하고 변경 파일·diff를 검토한다.
4. Feature를 push한다.
5. FE는 `front/develop`, BE는 `back/develop`을 base로 PR을 연다.
6. PR에 Issue 연결, 변경 요약, 기능명세 ID, API/DB 영향, 테스트 결과를 기록한다.
7. 필수 자동 검증과 §13의 대상 브랜치·변경 영향별 리뷰 조건을 통과하면 GitHub PR에서 병합한다.
8. 병합 결과를 확인하고 Issue를 닫은 뒤 Feature 브랜치를 삭제한다.

PR 충돌·검증 실패가 있으면 병합하지 않는다. develop에서 로컬 merge 후 직접 push하는 절차는 사용하지 않는다.

## 12. 테스트와 빌드

- PR을 열기 전에 Feature에서 관련 테스트와 build를 실행하고 결과를 기록한다.
- develop 반영 전에 저장소의 필수 자동 검증이 통과해야 한다.
- develop에 병합된 뒤 영역 통합 상태와 FE/BE API 연동을 확인한다.
- 실제 저장소의 검증 명령은 협업 문서/README에 기록하며 확인되지 않은 명령을 임의로 만들지 않는다.
- main 반영 전 FE build, BE build, 핵심 연동 흐름, 배포 환경을 검증한다.

## 13. PR 리뷰와 보호 규칙

`main`, `front/develop`, `back/develop`에는 보호 규칙을 적용하고 직접 push를 제한하며 PR과 필수 CI로 통합한다. FE와 `main` 대상 PR의 기존 리뷰 정책은 유지한다.

2026-10-07 Backend 변경: `back/develop` 대상 일반 PR은 PR 생성 후 작성자·Codex의 최종 diff 재검토와 결과 기록·관련 test/build·필수 CI 통과 후 작성자가 병합할 수 있다. 공통 인터페이스·API 계약·DB 구조/Migration·인증/권한·타 영역 영향 변경은 상대 Backend 담당자 1명의 리뷰·승인을 받는다. 문서만 바꾸더라도 계약이나 협업 규칙을 바꾸면 상대 리뷰 대상이며 이 정책 변경 PR도 포함한다. 상세 기준과 PR 분류는 [AGENTS.md의 Backend PR 리뷰 기준](../../AGENTS.md#backend-pr-리뷰-기준-2026-10-07-변경)과 [Backend 전략 §5.1](Discushion_백엔드_Git_GitHub_협업전략_2026-10-06.md#51-변경-영향별-리뷰와-병합-조건)을 따른다.

`back/develop`의 일괄 필수 승인 수는 0으로 조정하며 필수 CI 등 다른 보호 조건은 유지한다. 상대 리뷰가 필요한 변경은 PR 운영 규칙으로 확인하고 GitHub가 변경별 승인을 자동 강제한다고 기록하지 않는다. 작성자는 자기 PR을 승인할 수 없으며 필요한 상대 승인을 Codex 검사로 대체하지 않는다. 동일 파일이나 API 계약에 영향이 겹치는 작업은 Issue/PR에서 선행 순서를 조율한다.

## 14. 동시 개발

각 영역은 여러 Feature PR을 병렬로 진행할 수 있다. develop의 변경은 모두 PR을 통해 들어오며, base branch와 충돌 상태를 GitHub에서 확인한다. 같은 파일/API 계약을 수정하는 PR이 겹치면 담당자끼리 순서와 적용 범위를 합의한다. develop이나 main에 반영을 위해 직접 push하는 별도 잠금 절차는 두지 않는다.

## 15. 충돌 해결

- 전부 `ours`/`theirs`를 자동 선택하지 않는다.
- Issue 요구사항, 기능명세, API 계약을 기준으로 양쪽 변경의 목적을 확인한다.
- 다른 팀원의 변경을 확인 없이 지우거나 덮어쓰지 않는다.
- Feature에서 충돌을 해결하고 관련 테스트/build를 다시 실행한 뒤 push한다.
- base branch가 잘못되었으면 임의 merge하지 말고 PR base를 바로잡는다.

## 16. FE/BE API·DB 협업

구현 전에 Method, Endpoint, Request, Response, Status Code, Error Response, 인증 여부, 권한 조건을 합의한다. 각 Part 담당자는 자신의 API/화면을 직접 구현한다.

- 각 담당자가 자기 영역 API 정본 해당 절을 갱신하고 BE1이 전체 정합성을 검토한다. 공통·타 영역·합의된 계약 변경은 사전 협의한다. FE 담당자에게 전달하고 Issue/PR에 기록한다.
- 각 담당자가 자기 영역 Entity/Column/Relation·ERD·Migration을 작성하고 BE1이 전체 정합성을 검토한다. 공통·타 영역 변경은 사전 공동 검토하고 적용된 Migration은 보존한다.
- 배포·실행환경·Docker·환경변수·CORS·CI/CD는 BE2와 협의한다. 운영 DB 연결은 BE2, 데이터 모델은 BE1 책임이다.
- FE와 BE develop은 서로의 Feature 통합 브랜치가 아니다. FE/BE 연동은 합의한 API 계약과 실행 환경에서 검증한다.

## 17. develop 브랜치에서 main 반영

최종 시연·배포 단계에서 `front/develop`과 `back/develop`을 각각 `main`으로 PR한다. `main` 직접 push/merge는 금지한다.

1. 두 develop에서 각 영역 전체 검증과 FE/BE 연동을 완료한다.
2. 먼저 반영할 develop에서 `main`으로 PR을 열고 필요한 리뷰/CI를 통과시켜 병합한다.
3. 두 번째 PR 전, 두 번째 develop과 최신 `main`의 차이·충돌·누락을 다시 확인한다.
4. 첫 PR에서 들어온 main 변경을 두 번째 develop에 반영해야 하면 `main → 해당 develop` 동기화 PR을 열고 검토·검증한다. 직접 merge/push하지 않는다.
5. 두 번째 develop에서 최신 main을 확인한 PR을 갱신/생성하고 양 영역 변경의 포함 여부를 확인한 뒤 병합한다.
6. 최종 main에서 FE/BE build, 연동, 핵심 사용자 흐름, 배포 환경을 다시 검증한다.

```text
front/develop → PR → main
main → 동기화 PR → back/develop (필요한 경우)
back/develop → 최신 main을 확인한 PR → main
main 전체 검증
```

첫 번째 PR 병합 전 상태로 두 번째 PR을 검토하지 않는다. 실패나 충돌이 있으면 main 반영을 멈추고 담당 영역의 Feature/PR에서 해결한다.

## 18. 금지 사항

- `main`, `front/develop`, `back/develop` 직접 push/merge
- 단일 `develop`, `release`, Part 장기 브랜치 사용
- `feature/fe-*`, `feature/be-*` 이름 사용
- FE Feature를 `back/develop`에, BE Feature를 `front/develop`에 PR
- Issue 없는 일반 Feature 개발 또는 PR 없는 develop/main 반영
- 미검증 PR 또는 필요한 상대 담당자 승인이 없는 PR 병합
- FE/BE Feature 간 직접 merge
- 타인의 변경 임의 삭제, 관련 없는 대규모 리팩터링, 전역 포맷팅
- API 계약 변경 무통보
- secret/API key/password commit
- force push 또는 공유 이력을 덮는 reset/rebase

## 19. 작업 시작 전 확인

- [ ] Issue, 담당 영역, 완료 조건을 확인했다.
- [ ] FE는 `front/develop`, BE는 `back/develop`을 기준으로 선택했다.
- [ ] 작업 폴더 상태와 관련 기능명세/API 계약을 확인했다.
- [ ] 브랜치 이름이 `<영역>/feature/<issue번호>-<기능명>` 형식이다.
- [ ] 필요한 테스트/build 명령을 확인했다.

## 20. PR 병합 직전 확인

- [ ] base가 FE `front/develop` 또는 BE `back/develop`으로 정확하다.
- [ ] Issue 완료 조건, 기능명세 범위, 변경 diff를 검토했다.
- [ ] 관련 테스트/build 결과가 기록됐고 필수 CI와 대상 브랜치·변경 영향에 따라 필요한 리뷰가 통과했다.
- [ ] API/DB 영향과 FE/BE 연동 통보를 확인했다.
- [ ] GitHub PR을 통해 병합한다. develop/main 직접 push하지 않는다.

**기억할 흐름: Issue → 최신 영역 develop → Feature → 구현/test/build → push → 영역 develop 대상 PR → 필요한 리뷰/CI → merge. 최종 단계에서 두 develop을 각각 PR로 main에 반영한다.**

2026-10-07 팀 합의: Backend 공통 기반 1~5번과 독립 개발·Migration 영역별 작성은 [AGENTS.md](../../AGENTS.md)의 같은 날짜 합의를 따른다. 이전 담당/전담 작성 예시보다 현재 분담을 우선하며 GitHub 이슈 담당·선행은 후속 동기화 대상이다.
