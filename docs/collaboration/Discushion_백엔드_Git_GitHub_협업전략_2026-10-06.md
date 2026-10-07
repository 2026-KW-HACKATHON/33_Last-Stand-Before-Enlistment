# Discushion 백엔드 Git & GitHub 협업 전략

> 작성일: 2026-10-06 · 백엔드 팀 실무 가이드
> 공통 규칙: `../collaboration/Discushion_Git_GitHub_통합협업전략_2026-10-06.md`
> Backend 흐름: GitHub Issue → `back/feature/*` → PR → `back/develop`

## 1. Backend 작업 원칙

Backend 작업도 공통 Git 전략을 따른다. 단일 `develop`, Part 장기 브랜치, `release` 브랜치는 사용하지 않는다. Frontend 통합 브랜치는 `front/develop`, Backend 통합 브랜치는 `back/develop`이다. Backend Feature 형식은 `back/feature/<issue번호>-<기능명>`이며 Issue 하나에 Feature 하나와 PR 하나를 연결하고 PR base는 `back/develop`으로 한다. 두 develop과 `main`에는 직접 push하지 않는다.

## 2. 담당자 및 전역 책임

| 담당자 | 기능 구현 | 추가 전역 책임 |
| --- | --- | --- |
| BE1 | Part A, Part C, Part D | 공통 인증·자격 조회, API/ERD/Schema/Migration 전체 정합성 검토 |
| BE2 | Part B | 배포, 실행환경, 서버 설정, Docker, 환경변수, CORS, FE/BE 배포 연동, 필요 시 CI/CD, Health Check, 배포 후 서버·로그 확인, 배포 방법 문서화 |

- BE1과 BE2 모두 기능 개발자이며 자기 Part 기능과 API를 직접 구현한다.
- 각 담당자는 자기 영역 API 명세·ERD·Migration까지 작성한다. BE1은 전체 API/DB 정합성을 검토하며 모든 변경을 대신 작성하지 않는다. 공통·타 영역·합의된 계약 변경은 사전 공동 검토한다.
- 타 Part·공통 Entity/Column/Relation 변경은 해당 담당자와 BE1이 ERD·기존 DB 영향을 사전 검토하고 작성 담당자를 정한다. 자기 영역 Migration은 각 담당자의 Feature PR에 포함한다.
- BE2는 배포/실행환경의 전역 Owner이며 배포만 담당하지 않는다. Part B를 구현한다.
- Docker, 서버 실행, 운영 환경변수, CORS, 배포 서버, 운영 DB 연결은 BE2와 협의한다. 영역별 데이터 모델·ERD·Migration은 각 담당자가 작성하고 전체 정합성은 BE1이 검토한다.

## 3. Part 담당 매핑

| Part | 담당자 | 제품 영역 | 주요 백엔드 범위 |
| --- | --- | --- | --- |
| A | BE1 | Account & Authority | 회원가입·로그인, 프로필, 활동 지역, 이웃 인증, 지역 참여 권한, 기관 인증·권한·배지 |
| B | BE2 | Content & Post | 메인·통합 게시판·공통 상세, 지역 안건·활동 정보, 투표 게시물 작성/관리, 사진, AI 요약, 이슈 지도 |
| C | BE1 | Participation | 게시물 반응, 댓글·답글, 댓글 평가·정렬, 실제 투표 참여·변경·결과, 공유·게스트 |
| D | BE1 | Personal & Institution | 북마크, 마이페이지, 개인 기록·활동 횟수, 기관 안건 목록·채택/취소 |

기능명세 ID는 통합 전략 및 현재 기준 기능명세서의 기존 값을 쓴다. Part는 기능 분류이며 브랜치 단위가 아니다.

## 4. Issue에서 Backend Feature 시작

Issue `#32 [BE] 댓글/답글 작성 구현`의 예:

```bash
git status
git fetch origin
git switch back/develop
git pull --ff-only origin back/develop
# 실제 Backend test/build로 기준 상태를 검증한 뒤 다음 단계 진행
git switch -c back/feature/32-comment
git push -u origin back/feature/32-comment
```

pull 직후 실제 구성된 Backend test/build로 기준 상태를 검증하고 통과한 뒤 Feature를 생성한다. 이력 분기·실패·검증 환경 누락이 있으면 이를 보고하고 임의로 성공 처리하지 않는다.

기존 Feature는 `git switch back/feature/32-comment`로 이동한다. 작업 중 최신 Backend 통합을 반영할 때:

```bash
git fetch origin
git switch back/feature/32-comment
git merge origin/back/develop
```

충돌을 해결하면 테스트/build 후 Feature에 push한다. `back/develop`에는 직접 push/merge하지 않는다.

## 5. Backend PR과 검증

1. Issue 완료 조건, 기능명세 ID, API 계약을 확인한다.
2. 해당 Issue 범위만 구현하고 의미 있는 단위로 commit한다.
3. 변경 파일/diff를 검토하고 실제 저장소의 Backend test/build를 수행한다.
4. Feature를 push하고 `back/develop`을 base로 PR을 연다. Issue, 테스트 결과, API/DB/FE 영향 및 관련 담당자를 적는다.
5. 필수 CI와 §5.1에서 요구하는 상대 담당자 리뷰를 통과한 뒤 PR에서 병합한다. Issue를 닫고 Feature를 삭제한다.

FE Feature를 Backend develop에 직접 merge하지 않는다. FE/BE 연동은 합의된 API 계약과 실행 환경으로 검증한다.

## 5.1 변경 영향별 리뷰와 병합 조건

2026-10-07 사용자 결정으로 모든 Backend PR의 승인 1명 강제를 해제하는 정책으로 변경한다. `back/develop` 대상 PR은 다음 기준을 적용한다. FE와 `main` 대상 정책은 변경하지 않는다.

| 분류 | 변경 예 | 병합 조건 |
| --- | --- | --- |
| 일반 | 확정 계약을 유지하는 자기 영역 구현·테스트, 오탈자·설명 보완 | PR 생성 후 작성자·Codex 최종 diff 재검토 및 결과 기록 + 관련 test/build + 필수 CI. 상대 승인 없이 작성자 병합 가능 |
| 상대 리뷰 필요 | 공통 port/DTO·오류·트랜잭션 규약, API 계약, DB 구조/Migration·RLS/권한, 인증/제품 권한, 타 영역 영향 | 일반 조건 + 상대 Backend 담당자 1명의 최신 변경 리뷰·승인 |

문서 수정도 계약·정책을 바꾸면 상대 리뷰가 필요하며 이 리뷰 정책 변경 PR 자체도 포함한다. API/DB 정합성은 BE1, 실행환경과 BE2 소비 계약은 BE2가 확인하고 작성자가 해당하면 상대 담당자의 승인을 받는다. 혼합 PR은 상대 리뷰 기준을 적용하고 불명확한 분류는 병합 전 확인한다. PR에는 리뷰 분류·근거·영향 영역·검증 결과와 PR 생성 후 재검토 결과를 적는다. 재검토에서는 이슈 완료 조건·불필요한 변경·API/DB/권한 영향·검증 누락을 확인한다. 추가 수정이나 base 갱신이 있으면 최종 diff를 다시 검토하고 영향을 받는 검증을 재실행한다. 실패한 CI·충돌·미해결 리뷰 지적이 있으면 병합하지 않는다.

GitHub `back/develop` 보호 설정의 목표는 일괄 필수 승인 수 0이며, 필수 검사 `Backend tests and build`와 최신 base 검증을 유지한다. PR 필수·대화 해결·관리자 적용·force push/삭제 금지도 유지한다. 보호 설정 변경은 저장소 관리자의 별도 승인·적용과 실제 조회 확인 후 완료로 기록한다. 2026-10-07 실제 설정 조회로 일괄 필수 승인 수 0 적용을 확인했다. 필수 CI·최신 base·관리자 적용·대화 해결·force push/삭제 금지는 유지됐다. 변경별 상대 승인은 운영 규칙으로 확인하며 CODEOWNERS나 자동 분류 검사는 추가하지 않는다. 이후 작업자의 develop pull·검증 또는 Codex 검사만으로 상대 승인 완료를 기록하지 않는다. “PR해줘”는 commit·push·PR 생성과 검증 확인까지이며 병합하지 않는다. “PR하고 조건 통과하면 merge까지 해줘”처럼 병합까지 요청받으면 Codex가 최종 재검토·CI·필요한 상대 승인 1명·최신 base·충돌/미해결 지적 없음을 확인한 뒤 GitHub PR에서 병합한다. 부족한 조건은 우회하지 않고 대기로 보고한다.

## 6. API 및 DB 변경 협의

2026-10-07 팀 합의로 각 담당자는 자기 영역 API 정본 해당 절·ERD·Migration까지 작성하고 BE1이 전체 정합성을 검토한다. BE1은 Part A·C·D, BE2는 Part B를 맡는다. 공통·타 영역 또는 이미 합의된 API/DB 계약 변경은 사전 공동 검토하며 FE 데이터 계약은 구현 전에 확인한다. 코드·명세·Migration을 해당 기능 Feature PR에 함께 포함한다.

공통 파일 테이블은 프로필/게시물 등 용도별 작성 범위와 공통 제약을 구분한다. 투표 질문/선택지는 BE2, 실제 투표 참여/집계는 BE1이며 종료/삭제와 참여의 트랜잭션 경합은 함께 정한다. 적용된 Migration은 수정하지 않는다. 공유 환경에는 각자 임의 적용하지 않고 로컬 검증·PR 통합 후 BE2가 조율해 지정 담당자가 적용한다.

공통 기반 1~5번과 독립 개발의 정확한 경계는 [루트 AGENTS.md](../../AGENTS.md)의 같은 날짜 합의를 따른다. 인증은 Privy Bearer 직접 검증, 공통 자격 조회는 BE1, 최소 게시물 내부 조회는 BE2다. 필수 계약/DB 구조가 기준 develop에 준비되면 상대 기능 전체 완료 전에도 합의된 대체 구현으로 개별 검증할 수 있다. 실제 연결 전에는 완료로 표시하지 않으며 각 기능의 실제 연결 및 #30/#31 인수 조건을 유지한다. 인터페이스·DTO·테스트 fixture·코드 위치는 구현 전 해당 이슈에서 구체화한다.

GitHub 이슈 담당/선행은 작업 시작 때마다 최신 상태를 확인한다. 문서 합의만으로 Schema 적용·서비스 연동이 완료된 것은 아니다.

## 6.1 BE1·BE2 독립 업무표와 다음 작업 선택

각 담당자는 아래 자기 업무표를 기준으로 다음 열린 Issue를 선택한다. 매번 상대 담당자에게 작업을 요청하거나 상대 기능 전체 완료를 기다리지 않는다. 표는 우선순위이며 모든 앞선 Issue의 종료를 일괄 착수 조건으로 삼지 않는다. 실제 Issue의 완료 조건·확정 API·필수 Schema를 먼저 대조한다. 공통 업무표의 정본은 이 절이며 다른 문서는 이 절을 참조한다.

### BE1 업무표 — A·C·D

| 순서 | Issue·작업 | 착수·완료 시 확인 |
| --- | --- | --- |
| 1 | #74 인증·로컬 회원 연결·가입 상태 계약 | 확정된 공통 규약은 유지하고 인증 관련 미정 항목을 정리. 사진 계약은 BE2 범위 |
| 2 | #4 실제 인증·자격·오류/권한 기반 | 필요한 #74 계약·회원 Schema 통합 후 구현. 기본 DB 권한표는 #30 계정 적용 전 공동 확인 |
| 3 | #9 → #5 → #6 → #7 → #8 → #10 → #11 → #12 | 지역 → 활동 기록 → Privy 인증 → 최초 가입 → 로그인 → 프로필 → 완료 지역/자격 → 기관 유효 상태. 필요한 계약과 Schema가 준비된 항목부터 진행 |
| 4 | #21 → #22 → #23 → #24 → #25 | 공유 → 댓글/답글 → 세 반응 → 댓글 평가 → 투표 참여/집계. PostContextReader 대체 구현으로 개별 검증하고 실제 adapter·삭제/종료 경합을 완료 전 확인 |
| 5 | #26 → #28 → #29 → #27 | 북마크 → 기관 목록 → 채택/취소 → 개인 기록. PostSummaryReader·참여 집계 규약을 사용하며 실제 원본/삭제 이력 연결을 완료 전 확인 |
| 6 | #75 → #31 | 회원·지역·기관 자격·참여 시연 데이터를 준비하고 공동 인수. 필요한 상태 기능 이후 #75를 병렬 준비 가능 |

### BE2 업무표 — B·사진·탐색·AI·실행환경

| 순서 | Issue·작업 | 착수·완료 시 확인 |
| --- | --- | --- |
| 1 | #74 사진 계약·DB 보완 검토 | 파일 참조·업로드/삭제·24시간 정리·경합 계약과 추가 Migration을 대조. 인증 계약은 BE1 범위 |
| 2 | #13 사진 처리 | 필수 wire 계약·Schema 통합 후 구현. 실제 Storage 삭제/재시도·늦은 전송/정리 경합을 완료 전 검증 |
| 3 | #14 → #15 → #16 → #17 | 게시물 생성 → 상세 → 수정/삭제 → 목록. 인증/자격·참여 집계는 합의된 대체 구현으로 개별 검증하고 실제 adapter 연결은 완료 전 수행 |
| 4 | #18 → #19 → #20 | 메인 → 지도 → AI. 원본·집계 규약과 유형/지역/공개 상태를 확인하며 실제 연결·오류/fallback 검증 유지 |
| 병렬 | #30 DB 실행 계정·환경 준비 → 배포·FE 연결·CD | #4와 기본 권한표를 확인하고 실제 서버 역할 검증 전에 계정 준비. 전체 기능 완료를 기다리지 않으며 최종 환경 연결은 #31 전 완료 |
| 마지막 | #75 → #31 | 게시물·사진·투표 원본 시연 데이터 준비와 공동 인수. 참여 데이터와 합성 시연 데이터의 실제 재현을 확인 |

### 공통 진행 기준

1. 시작 전 최신 GitHub Issue/PR과 `origin/back/develop`을 조회한다. 미커밋 변경을 보존하고 기준 브랜치를 갱신한 뒤 해당 Issue의 새 Feature에서 진행한다. #1/#2/#3 기반도 Issue 제목/종료 여부만으로 준비됐다고 판단하지 않고 필요한 변경의 통합 상태를 확인한다.
2. 착수 조건은 필요한 확정 계약·Schema·공통 인터페이스/테스트 기반의 back/develop 반영이다. 상대 기능 전체가 미완성이면 test-only 대체 구현으로 독립 개발한다. 미정 계약이나 미반영 필수 Schema를 임의 생성해 우회하지 않는다.
3. 완료 조건은 실제 의존 adapter 연결·권한·오류·데이터·동일 transaction/경합 검증이다. 대체 구현 통과는 개별 검증으로 기록하고 실제 연동 대기는 유지한다. FE 연결 이관은 #30/#31에서 해소한다.
4. 자기 Issue의 코드·테스트·API 문서·추가 Migration은 각자 작성한다. 상대 문서 영역이나 확정 공통 규약을 임의 수정하지 않는다. 공통 계약 변경·타 영역 영향·불명확한 의존성·실제 연결이 막힌 경우에만 상대에게 구체적인 확인을 요청한다. 일반 Backend PR은 Codex 변경 검토와 필수 CI 통과로 병합할 수 있다. 상대 담당자 리뷰는 §5.1의 영향 기준에 따라 요청한다.
5. 업무표는 순서 안내의 근거이며 최신 Issue 변경이 있으면 영향을 받는 표/참조를 기존 파일에서 갱신한다. 기본 안내는 하나의 시간선에 역할을 표시하고, 역할별 안내를 요청받으면 해당 표를 사용한다. 상대에게 실제 필요한 요청이 있을 때만 안내 끝에 요청 사항을 붙인다.
6. CODEOWNERS는 사용자 결정으로 생략한다. AGENTS.md의 담당 규칙과 직접 리뷰 요청, 필수 CI와 변경 영향별 리뷰 기준을 유지한다.

Codex에는 다음처럼 시작을 요청할 수 있다. 역할은 실제 담당에 맞게 선택한다.

```text
역할: BE1 / BE2
최신 back/develop의 AGENTS.md와 Backend 협업전략 §6.1 업무표,
관련 API/DB 계약과 최신 GitHub Issue/PR을 확인해 다음 작업을 선정해줘.
확정된 규약과 상대 담당 영역은 유지하고, 필요한 계약/Schema가 준비된
Issue 하나의 범위에서 진행해줘. 변경 전 범위·착수 조건·검증 기준을 알려줘.
공통/타 영역 변경이 필요하면 이유와 영향을 먼저 정리해줘.
미커밋 변경을 보존하고 commit/push/PR·병합·배포는 사용자 허용 범위를 확인해줘.
```

2026-10-07 #4 착수 확인: 공통 준비 PR #101·사진 보완 PR #85·인증 문서 PR #104는 병합되어 back/develop `6a84ffc`에 반영됐다. #4는 BE1의 `back/feature/4-auth`에서 구현/로컬 검증 중이고 #74는 열려 있다. BE2 사진 잔여 계약 PR #107은 Draft/미병합이다. 실제 Privy 앱·공개 검증키 공급과 서버 역할은 #30에서 필요한 기반부터 준비하고 BE1 #4가 연결·권한 검증한다. 문서 병합이나 합성 키/localhost 시험을 전체 기능/실제 연동 완료로 표시하지 않으며 다음 착수 때 최신 Issue/PR 상태를 다시 확인한다.

## 7. 배포·실행환경

BE2가 Backend 배포, 실행환경, 서버 설정, Docker, 운영 환경변수, CORS, FE/BE 배포 연동, 필요 시 CI/CD, Health Check, 배포 후 서버 정상 동작·운영 로그 확인, 배포 방법 문서화를 맡는다. 다른 개발자는 이 범위의 변경을 BE2와 협의한다.

운영 DB 연결·실행과 공유 환경 적용은 BE2가 조율한다. 영역별 데이터 모델·ERD·Migration은 각 담당자가 작성하고 BE1이 전체 정합성을 검토한다.

## 8. Codex 요청과 main 최종 반영

Codex 요청에는 BE 담당자, Issue, `back/feature/<issue>-<기능명>`, base `back/develop`, 기능명세/API 계약, 허용·금지 범위와 완료 조건을 적는다. 결과 후 사람이 `git status`, `git diff`, `git diff --cached`를 확인한다. Codex는 develop/main에 직접 push하지 않는다. PR 생성 요청만으로 병합하지 않으며 병합까지 명시적으로 요청받은 경우 §5.1의 조건을 확인해 GitHub PR에서 병합한다.

Backend 전체 검증과 FE/BE 연동 후 `back/develop → main` PR로 최종 반영한다. `main` 직접 push/merge는 금지한다. Frontend가 먼저 main에 반영됐다면 최신 main과 차이·충돌·누락을 확인하고 필요한 동기화도 PR로 처리한다. main에서 Backend build와 핵심 FE/BE 흐름을 다시 확인한다.

공통 commit, 리뷰, 충돌, PR 종료 절차는 통합 전략을 따른다.

## 8.1 공통 파일 담당과 테스트/CI 운영

| 파일/영역 | 구현 담당 | 검토 기준 |
| --- | --- | --- |
| identity 실제 adapter, A·C·D 기능과 해당 테스트 | BE1 | BE2 소비 계약 영향 시 공동 리뷰 |
| post 실제 adapter, B 기능과 해당 테스트 | BE2 | BE1 소비 계약 영향 시 공동 리뷰 |
| backend/src/main/java/com/discushion/contracts, src/test/java/com/discushion/support, 공통 오류/트랜잭션 규약 | 변경 제안자 | BE1·BE2 공동 리뷰 |
| API/ERD 문서와 각 영역 추가 Migration | 해당 영역 담당자 | 전역 정합성 BE1, 교차 변경 공동 리뷰 |
| 실행환경, .github/workflows/backend-ci.yml, scripts/ci | BE2 | DB/공통 계약 영향 시 BE1 리뷰 |

공통 port와 고정 Clock/합성 fixture는 상대 기능 미완성 시 단위 검증에 사용한다. 운영 대체 bean을 등록하지 않고 DB 잠금/실제 인증 검증을 단위 시험으로 대신하지 않는다. 실제 adapter 연결·경합·FE 연동은 별도로 기록한다. CODEOWNERS는 생략하고 PR 작성자가 해당 담당자에게 직접 리뷰를 요청한다. 필수 CI와 변경 영향별 리뷰 조건은 유지하며 실제 적용 상태를 확인한다.

Backend CI는 격리 PostgreSQL Schema/모의 역할 검증 후 Java 17 테스트/build를 실행한다. workflow를 원격에 반영한 뒤 실제 실행 결과를 확인하고 저장소 관리자가 필수 검사 `Backend tests and build`를 지정한다. CD는 #30에서 실제 Vercel 프로젝트와 환경별 구성·실패 복구를 확인한 뒤 연결한다. 준비 파일 작성은 CI 실행·배포 성공을 뜻하지 않는다.

## 9. 금지 사항

- `back/develop` 또는 `main` 직접 push/merge
- `backend-part-a`~`backend-part-d`, `feature/be-*` 브랜치 사용
- Issue 없는 일반 Feature 작업 또는 PR 없는 통합
- 잘못된 영역 develop을 대상으로 PR
- 미검증 PR 또는 필요한 상대 담당자 승인이 없는 PR 병합, FE/BE Feature 간 직접 merge
- BE1이 모든 API를 직접 구현하거나 BE2가 배포만 담당한다고 해석
- API 계약·DB 구조·배포 설정 변경 무통보
- secret/API key/password commit, force push, 공유 이력 덮어쓰기
