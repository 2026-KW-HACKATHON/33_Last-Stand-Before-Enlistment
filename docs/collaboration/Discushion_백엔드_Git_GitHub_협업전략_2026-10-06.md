# Discushion 백엔드 Git & GitHub 협업 전략

> 작성일: 2026-10-06 · 백엔드 팀 실무 가이드
> 공통 규칙: `../collaboration/Discushion_Git_GitHub_통합협업전략_2026-10-06.md`
> Backend 흐름: GitHub Issue → `back/feature/*` → PR → `back/develop`

## 1. Backend 작업 원칙

Backend 작업도 공통 Git 전략을 따른다. 단일 `develop`, Part 장기 브랜치, `release` 브랜치는 사용하지 않는다. Frontend 통합 브랜치는 `front/develop`, Backend 통합 브랜치는 `back/develop`이다. Backend Feature 형식은 `back/feature/<issue번호>-<기능명>`이며 Issue 하나에 Feature 하나와 PR 하나를 연결하고 PR base는 `back/develop`으로 한다. 두 develop과 `main`에는 직접 push하지 않는다.

## 2. 담당자 및 전역 책임

| 담당자 | 기능 구현 | 추가 전역 책임 |
| --- | --- | --- |
| BE1 | Part A, Part D | API 명세 정본, 계약 정합성, ERD, DB Schema/Migration |
| BE2 | Part B, Part C | 배포, 실행환경, 서버 설정, Docker, 환경변수, CORS, FE/BE 배포 연동, 필요 시 CI/CD, Health Check, 배포 후 서버·로그 확인, 배포 방법 문서화 |

- BE1과 BE2 모두 기능 개발자이며 자기 Part 기능과 API를 직접 구현한다.
- BE1은 API/DB의 전역 Owner다. 모든 API를 BE1이 구현한다는 뜻은 아니다. 계약/API 변경은 BE1과 협의한다.
- 타 Part의 Entity/Column/Relation 변경은 BE1이 ERD·기존 DB 영향을 검토하고 Schema/Migration 정합성을 관리한다.
- BE2는 배포/실행환경의 전역 Owner이며 배포만 담당하지 않는다. Part B/C를 구현한다.
- Docker, 서버 실행, 운영 환경변수, CORS, 배포 서버, 운영 DB 연결은 BE2와 협의한다. 데이터 모델·ERD·Schema·Migration은 BE1 책임이다.

## 3. Part 담당 매핑

| Part | 담당자 | 제품 영역 | 주요 백엔드 범위 |
| --- | --- | --- | --- |
| A | BE1 | Account & Authority | 회원가입·로그인, 프로필, 활동 지역, 이웃 인증, 지역 참여 권한, 기관 인증·권한·배지 |
| B | BE2 | Content & Post | 메인·통합 게시판·공통 상세, 지역 안건·활동 정보, 투표 게시물 작성/관리, 사진, AI 요약, 이슈 지도 |
| C | BE2 | Participation | 게시물 반응, 댓글·답글, 댓글 평가·정렬, 실제 투표 참여·변경·결과, 공유·게스트 |
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

각 Part 담당자가 자기 기능 API를 구현한다. BE2가 Part C API 필드를 추가 제안하면 변경 필요사항을 정리해 BE1과 협의하고, BE1이 전체 API 명세 정합성을 확인해 정본을 갱신한다. BE2는 합의된 계약으로 구현하고 FE 담당자에게 알리며 Issue/PR에 기록한다.

BE2의 Part B/C 구현에서 새 Entity/Column/Relation이 필요하면 BE2가 제안하고 BE1이 전체 ERD 및 기존 DB 영향을 확인한다. 합의한 Schema/Migration은 BE1이 관리하며 BE2는 확정된 구조를 사용해 구현한다. BE1의 Part A/D에서도 같은 협의·기록 원칙을 따른다.

## 7. 배포·실행환경

BE2가 Backend 배포, 실행환경, 서버 설정, Docker, 운영 환경변수, CORS, FE/BE 배포 연동, 필요 시 CI/CD, Health Check, 배포 후 서버 정상 동작·운영 로그 확인, 배포 방법 문서화를 맡는다. 다른 개발자는 이 범위의 변경을 BE2와 협의한다.

운영 DB 연결·실행은 BE2, DB 데이터 모델·ERD·Schema·Migration은 BE1이 담당한다.

## 8. Codex 요청과 main 최종 반영

Codex 요청에는 BE 담당자, Issue, `back/feature/<issue>-<기능명>`, base `back/develop`, 기능명세/API 계약, 허용·금지 범위와 완료 조건을 적는다. 결과 후 사람이 `git status`, `git diff`, `git diff --cached`를 확인한다. Codex는 develop/main에 push하거나 PR을 병합하지 않는다.

Backend 전체 검증과 FE/BE 연동 후 `back/develop → main` PR로 최종 반영한다. `main` 직접 push/merge는 금지한다. Frontend가 먼저 main에 반영됐다면 최신 main과 차이·충돌·누락을 확인하고 필요한 동기화도 PR로 처리한다. main에서 Backend build와 핵심 FE/BE 흐름을 다시 확인한다.

공통 commit, 리뷰, 충돌, PR 종료 절차는 통합 전략을 따른다.

## 9. 금지 사항

- `back/develop` 또는 `main` 직접 push/merge
- `backend-part-a`~`backend-part-d`, `feature/be-*` 브랜치 사용
- Issue 없는 일반 Feature 작업 또는 PR 없는 통합
- 잘못된 영역 develop을 대상으로 PR
- 미검증·미승인 PR 병합, FE/BE Feature 간 직접 merge
- BE1이 모든 API를 직접 구현하거나 BE2가 배포만 담당한다고 해석
- API 계약·DB 구조·배포 설정 변경 무통보
- secret/API key/password commit, force push, 공유 이력 덮어쓰기
