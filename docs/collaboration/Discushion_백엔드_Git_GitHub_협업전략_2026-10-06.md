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
git switch -c back/feature/32-comment
git push -u origin back/feature/32-comment
```

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
5. 지정 리뷰와 필수 CI를 통과한 뒤 PR에서 병합한다. Issue를 닫고 Feature를 삭제한다.

FE Feature를 Backend develop에 직접 merge하지 않는다. FE/BE 연동은 합의된 API 계약과 실행 환경으로 검증한다.

## 6. API 및 DB 변경 협의

2026-10-07 팀 합의로 각 담당자는 자기 영역 API 정본 해당 절·ERD·Migration까지 작성하고 BE1이 전체 정합성을 검토한다. BE1은 Part A·C·D, BE2는 Part B를 맡는다. 공통·타 영역 또는 이미 합의된 API/DB 계약 변경은 사전 공동 검토하며 FE 데이터 계약은 구현 전에 확인한다. 코드·명세·Migration을 해당 기능 Feature PR에 함께 포함한다.

공통 파일 테이블은 프로필/게시물 등 용도별 작성 범위와 공통 제약을 구분한다. 투표 질문/선택지는 BE2, 실제 투표 참여/집계는 BE1이며 종료/삭제와 참여의 트랜잭션 경합은 함께 정한다. 적용된 Migration은 수정하지 않는다. 공유 환경에는 각자 임의 적용하지 않고 로컬 검증·PR 통합 후 BE2가 조율해 지정 담당자가 적용한다.

공통 기반 1~5번과 독립 개발의 정확한 경계는 [루트 AGENTS.md](../../AGENTS.md)의 같은 날짜 합의를 따른다. 인증은 Privy Bearer 직접 검증, 공통 자격 조회는 BE1, 최소 게시물 내부 조회는 BE2다. 필수 계약/DB 구조가 기준 develop에 준비되면 상대 기능 전체 완료 전에도 합의된 대체 구현으로 개별 검증할 수 있다. 실제 연결 전에는 완료로 표시하지 않으며 각 기능의 실제 연결 및 #30/#31 인수 조건을 유지한다. 인터페이스·DTO·테스트 fixture·코드 위치는 구현 전 해당 이슈에서 구체화한다.

GitHub 이슈 담당/선행 표기는 후속 동기화 대상이며 문서 합의만으로 실제 이슈 수정·Schema 적용·서비스 연동이 완료된 것은 아니다.
## 7. 배포·실행환경

BE2가 Backend 배포, 실행환경, 서버 설정, Docker, 운영 환경변수, CORS, FE/BE 배포 연동, 필요 시 CI/CD, Health Check, 배포 후 서버 정상 동작·운영 로그 확인, 배포 방법 문서화를 맡는다. 다른 개발자는 이 범위의 변경을 BE2와 협의한다.

운영 DB 연결·실행과 공유 환경 적용은 BE2가 조율한다. 영역별 데이터 모델·ERD·Migration은 각 담당자가 작성하고 BE1이 전체 정합성을 검토한다.

## 8. Codex 요청과 main 최종 반영

Codex 요청에는 BE 담당자, Issue, `back/feature/<issue>-<기능명>`, base `back/develop`, 기능명세/API 계약, 허용·금지 범위와 완료 조건을 적는다. 결과 후 사람이 `git status`, `git diff`, `git diff --cached`를 확인한다. Codex는 develop/main에 push하거나 PR을 병합하지 않는다.

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

공통 port와 고정 Clock/합성 fixture는 상대 기능 미완성 시 단위 검증에 사용한다. 운영 대체 bean을 등록하지 않고 DB 잠금/실제 인증 검증을 단위 시험으로 대신하지 않는다. 실제 adapter 연결·경합·FE 연동은 별도로 기록한다. GitHub 핸들이 확정되면 위 담당에 맞게 CODEOWNERS/리뷰 강제 설정을 검토한다.

Backend CI는 격리 PostgreSQL Schema/모의 역할 검증 후 Java 17 테스트/build를 실행한다. workflow를 원격에 반영한 뒤 실제 실행 결과를 확인하고 저장소 관리자가 필수 검사 `Backend tests and build`를 지정한다. CD는 #30에서 실제 Vercel 프로젝트와 환경별 구성·실패 복구를 확인한 뒤 연결한다. 준비 파일 작성은 CI 실행·배포 성공을 뜻하지 않는다.

## 9. 금지 사항

- `back/develop` 또는 `main` 직접 push/merge
- `backend-part-a`~`backend-part-d`, `feature/be-*` 브랜치 사용
- Issue 없는 일반 Feature 작업 또는 PR 없는 통합
- 잘못된 영역 develop을 대상으로 PR
- 미검증·미승인 PR 병합, FE/BE Feature 간 직접 merge
- BE1이 모든 API를 직접 구현하거나 BE2가 배포만 담당한다고 해석
- API 계약·DB 구조·배포 설정 변경 무통보
- secret/API key/password commit, force push, 공유 이력 덮어쓰기
