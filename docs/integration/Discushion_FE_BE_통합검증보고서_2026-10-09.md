# Discushion FE/BE 통합 검증 보고서 — 2026-10-09

## 안전한 업로드 문서 처리

새 FE/BE 통합 지침 검토본의 운영 설정 서술이 자동 승인 검토에서 두 차례 거절됐다. 실제 secret/JWT 형식은 발견되지 않았지만 거절된 긴 본문은 업로드하지 않는다. 해당 새 검토본을 운영 설정/식별값 없는 짧은 통합 판단·확인 필요 문서로 대체했다. 원본 branch 문서47개 보존은 유지한다. 로컬 자체 commit 이력은 유지하되 원격에는 안전하게 정리한 최종 tree를 BE→FE 두-parent merge 구조로 올리므로 로컬 자체 commit SHA와 원격 통합 SHA는 다르다. 기존 FE/BE 이력과 코드 tree는 동일하다.

## 원격 업로드 후속 지시 — 2026-10-09 02:19 KST

사용자가 이전 검증 결과를 보고 `integration/develop`을 원격에 올리도록 명시적으로 요청했다. 이에 기존 통합 결과를 검토용 임시 branch로 push한다. 이 지시는 이전 push 보류 결정을 대체하지만 검증 실패/미검증·정책·Contract 차이를 해결한 것으로 해석하지 않는다. main과 두 develop에는 commit/merge/push하지 않는다.

일반 HTTPS git push는 실행환경의 GitHub 쓰기 credential 부재로 실패했다. 연결된 GitHub Git Data 기능으로 로컬에서 직접 병합한 것과 동일한 tree·parent 관계를 업로드한다. 인증된 작성자 메타데이터가 달라 자체 통합 commit SHA는 달라질 수 있지만 원본 FE/BE commit SHA와 이력은 그대로 보존한다. 로컬 통합 이력은 reset/rebase하지 않고 유지한다.

업로드 직전 fetch에서 origin/back/develop은 `6ff69300e4257f5d49dcd13fc8cf7096b32e25ff`(#17 통합 게시판 목록·필터·커서 조회)로 전진했고 origin/front/develop과 origin/main은 이전과 동일했다. 이번 업로드는 기존 BE 기준 `eb9a7d4`와 FE 기준 `108cb47`의 통합 결과다. 새 BE #17 commit은 아직 integration에 merge하지 않았다. 아래 endpoint 누락·Issue 상태 및 원격 integration 부재/push 미실행 설명은 최초 검증 시점의 기록이며, 최신 BE 상태나 업로드 이후 원격 상태를 뜻하지 않는다. 최신 BE 반영과 그 이후 계약/빌드 재검증은 별도 후속 작업이다.

---

Repository: `2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment`

**최종 판정: C. Conflict/정책 확인 필요 — 연결 시작 금지.** Git 병합은 성공했으나 MVP 범위 정책, FE/BE adapter와 wire 차이, 누락 endpoint가 남아 있다. Backend build/test는 도구 다운로드 차단으로 실행하지 못했다. 사용자 push 조건을 충족하지 않아 원격에 올리지 않았다.

## 통합 기준

| 기준 | SHA |
| --- | --- |
| main | `dc139f43ce9e7cbe09d660202ee0ccec033b0f5f` |
| front/develop | `108cb477322eaffdfb42c520a93a5e53794031ce` |
| back/develop | `eb9a7d4a734a744731d7bb85564b32f3c8f3b39c` |
| integration/develop 시작 | `dc139f43ce9e7cbe09d660202ee0ccec033b0f5f` |
| 첫 merge (BE) | `3ceb5c6a00cc2a7e18ba265f29b2b579c0d1ac55` |
| 두 번째 merge (FE) | `c8e6f8c88d2c38de84c49079c5c532524f42a670` |

보고서 추가 commit 전 검증 기준은 두 번째 merge SHA다. 보고서 commit은 문서만 추가하며 FE/BE 코드는 동일하다. 최종 branch HEAD는 `git rev-parse integration/develop`으로 확인한다.

| merge-base | SHA |
| --- | --- |
| front/develop ↔ back/develop | `eeec7eff45f206947e63eb25f21b413a099dad4e` |
| main ↔ front/develop | `eeec7eff45f206947e63eb25f21b413a099dad4e` |
| main ↔ back/develop | `fa3a21d42878cf49d18ddbb6ed3fd48633630139` |

최초 workspace에는 사용자 Git checkout이 없었다. `repository/`에 no-checkout clone을 만들고 status/worktree/fetch/remote branches를 확인했다. no-checkout의 파일 부재는 기존 사용자 변경이 아니며 해당 worktree를 수정하지 않았다. 별도 `integration/` worktree에서 최신 origin/main으로 새 local integration/develop을 생성했다. local/remote integration 기존 이력·미완료 merge는 없었다. 최종 원격 재조회에서도 위 세 SHA는 동일하고 origin/integration/develop은 존재하지 않는다.

## 병합 순서

1. `origin/back/develop → integration/develop`
2. `origin/front/develop → integration/develop`

선택 이유: main 기준 FE 343경로·BE 358경로 변경, 양쪽 공통 변경 경로 11개를 비교했다. root/.github/docs/config/API를 확인하고 merge-tree로 예상 충돌을 비교했다. main→BE는 충돌0, main→FE는 README/ERD/유저플로우3개 충돌이다. Backend가 실제 wire/API·DB/실행환경을 제공하고 FE가 소비하는 관계이므로 BE를 먼저 보존하면 불필요한 초기 문서 충돌을 줄일 수 있다. 두 번째 충돌8개는 모두 문서이며 코드 충돌은 없다.

## Merge 결과

| 순서 | branch | 명령 | 결과 | conflict |
| --- | --- | --- | --- | --- |
| 1 | back/develop | `git merge --no-ff origin/back/develop` | merge commit 성공 | 없음 |
| 2 | front/develop | `git merge --no-ff --no-commit origin/front/develop` 후 문서 처리·commit | merge commit 성공 | 문서8개, 모두 index 해결 |

각 merge commit은 기존 integration HEAD와 해당 develop HEAD를 두 parent로 보존한다. rebase/squash/force/reset을 사용하지 않았다. 첫 병합 후 status/log를 확인한 뒤 두 번째를 진행했다.

## Conflict 해결

| 파일 | Front 의도 | Back 의도 | 해결 내용 |
| --- | --- | --- | --- |
| `AGENTS.md` | FE 세부 지침·Figma/UI/Mock 범위·FE 담당 | BE1 A/C/D 및 #14~17 이관·승인0 정책 | first BE merge 원본 유지, 새 검토본에 FE 지침·BE 책임과 사용자 예외 A/B를 기록 |
| `README.md` | FE 기준 문서·ERD·유저플로우 링크 | BE 실행 안내·결정 기록 링크 | 양쪽 안내를 새 검토본에 보존. 초기화 상태 서술은 현재 FE/BE 존재 사실로 보완 |
| `docs/api/Discushion_API_SPEC_v2.md` | FE UI/Mock 범위와 서버 계약의 분리 | 실제 auth/지역/기관/공유/참여/사진/AI wire | 비충돌 변경 통합, 서로 다른 범위 문구는 병기. 실제 API 변경 없음 |
| `docs/collaboration/Discushion_프론트엔드_Git_GitHub_협업전략_2026-10-06_v2.md` | 복구된 FE UI/Mock 범위 | 기존 MVP 필수 범위 제외 목록 | 상충 정책 확인 필요. 실제 (1) 없는 통합 지침 경로를 새 검토본에서 설명 |
| `docs/specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md` | Figma 기반 UI/Mock·확장 FE 흐름 | 최신 계약·사진 lifecycle·BE 권한/인수 | 공통 부분 통합. 역할/범위 상충 병기, port/wire를 같다고 가정하지 않음 |
| `docs/specs/Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md` | FE 범위 복구, API 부재와 제품 제외 구분 | MVP 제외 유지·사진/AI 추가 결정 | 확인 필요 — 최종 MVP 및 FE/BE 분리 범위를 임의 확정하지 않음 |
| `docs/specs/Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md` | 기능 ID별 FE/UI/Mock 대상 확대 | 기존 BE 제외 목록·최신 파일 처리 | 기능 ID/공통 권한 보존, 범위 충돌 병기 |
| `docs/specs/Discushion_유저플로우_와이어프레임기반_2026-10-06.md` | 확장 UI/Mock 화면 흐름 | 변경 후 인증·사진·MVP 경계 | main/FE/BE 원본 모두 보존, 정본 정책 대체 금지. 상충 흐름 확인 필요 |

문서 원본 경로는 첫 번째 BE 병합 직후 상태를 유지했다. 이는 BE 내용을 최신 정본으로 자동 선택한 것이 아니다. Front와 Back의 정확한 내용은 각각 다른 보존 경로에 있으며, 양쪽 유효·비충돌 변경은 새 검토본으로 옮겼다. 상충 구간은 분기별 원문을 병기하고 확인 필요로 남겼다. 별도 main 원문도 보존했다. 새로운 정책·제품 기능·endpoint·Schema·UI 변경은 없다.

새 검토본의 상충 구간은 문서 비교 자료다. 그 안의 이전 '확정/완료' 표현을 이번 통합의 승인으로 해석하지 않는다. 옛 README 초기화 문구와 경과 기록은 원문 시점 자료다. 최신 구현 상태는 이 보고서가 기록한다.

## 문서 처리

A=원본 유지, B=단순 merge, C=새 통합본 생성, D=확인 필요. 각 원본은 아래 한 가지 처리로 기록하고 C 내 정책 미확정은 이유에 별도 표시한다.

| 기존 문서 | 처리 | 새 통합본 | 이유 |
| --- | --- | --- | --- |
| `AGENTS.md` | 새 통합본 생성 | `AGENTS_FE_BE_통합검토본_2026-10-09.md` | 양쪽 변경 보존; first BE merge 원본 유지, 새 검토본에 FE 지침·BE 책임과 사용자 예외 A/B를 기록 |
| `README.md` | 새 통합본 생성 | `README_FE_BE_통합검토본_2026-10-09.md` | 양쪽 변경 보존; 양쪽 안내를 새 검토본에 보존. 초기화 상태 서술은 현재 FE/BE 존재 사실로 보완 |
| `backend/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/README.md` | 단순 merge | `—` | 비충돌 양쪽 변경 병합; 분기 원문도 보존 |
| `docs/api/Discushion_API_CONTRACT_검토표_2026-10-07.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/api/Discushion_API_SPEC_v2.md` | 새 통합본 생성 | `docs/api/Discushion_API_SPEC_v2_FE_BE_통합검토본_2026-10-09.md` | 양쪽 변경 보존; 비충돌 변경 통합, 서로 다른 범위 문구는 병기. 실제 API 변경 없음 |
| `docs/api/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/architecture/Discushion_Issue3_DB_구현검증_2026-10-07.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/architecture/Discushion_Issue3_MVP_v10.2_반영.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/architecture/Discushion_MVP_DB_SCHEMA_준비명세_2026-10-07.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/architecture/Discushion_MVP_DB_검증계획_2026-10-07.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/collaboration/Discushion_Git_GitHub_통합협업전략_2026-10-06.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/collaboration/Discushion_백엔드_Git_GitHub_협업전략_2026-10-06.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/collaboration/Discushion_프론트엔드_2인_개발시작전_합의사항_병렬개발.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/collaboration/Discushion_프론트엔드_2인_담당분배_병렬개발순서.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/collaboration/Discushion_프론트엔드_Git_GitHub_협업전략_2026-10-06_v2.md` | 새 통합본 생성 | `docs/collaboration/Discushion_프론트엔드_Git_GitHub_협업전략_2026-10-06_v2_FE_BE_통합검토본_2026-10-09.md` | 양쪽 변경 보존; 상충 정책 확인 필요. 실제 (1) 없는 통합 지침 경로를 새 검토본에서 설명 |
| `docs/collaboration/backend-db-connection-decisions.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/collaboration/backend-environment-decisions.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/collaboration/task-card-template.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/collaboration/team-workspace.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/design/Discushion_최종디자인_디자인기준 v2.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/frontend/Discushion_MVP_프론트엔드_개발_상세지침서.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/specs/Discushion_MVP_ERD.mmd` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/specs/Discushion_MVP_ERD_상세명세.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/specs/Discushion_MVP_결정변경_2026-10-07.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `docs/specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md` | 새 통합본 생성 | `docs/specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서_FE_BE_통합검토본_2026-10-09.md` | 양쪽 변경 보존; 공통 부분 통합. 역할/범위 상충 병기, port/wire를 같다고 가정하지 않음 |
| `docs/specs/Discushion_PRD_2026-10-06_MVP반영_정리본_v10.1.md` | 원본 유지 | `—` | main의 기존 v10.1 파일을 복원해 경로/원문 유지; develop의 v10.2도 보존 |
| `docs/specs/Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md` | 새 통합본 생성 | `docs/specs/Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2_FE_BE_통합검토본_2026-10-09.md` | 양쪽 변경 보존; 확인 필요 — 최종 MVP 및 FE/BE 분리 범위를 임의 확정하지 않음 |
| `docs/specs/Discushion_기능명세서_2026-10-06_MVP반영_정리본_v10.1.md` | 원본 유지 | `—` | main의 기존 v10.1 파일을 복원해 경로/원문 유지; develop의 v10.2도 보존 |
| `docs/specs/Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md` | 새 통합본 생성 | `docs/specs/Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2_FE_BE_통합검토본_2026-10-09.md` | 양쪽 변경 보존; 기능 ID/공통 권한 보존, 범위 충돌 병기 |
| `docs/specs/Discushion_유저플로우_와이어프레임기반_2026-10-06.md` | 새 통합본 생성 | `docs/specs/Discushion_유저플로우_와이어프레임기반_2026-10-06_FE_BE_통합검토본_2026-10-09.md` | 양쪽 변경 보존; main/FE/BE 원본 모두 보존, 정본 정책 대체 금지. 상충 흐름 확인 필요 |
| `frontend/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/components/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/account-info/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/auth/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/bookmarks/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/email-change/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/institution/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/interest-keywords/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/interest-regions/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/logout/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/my-page/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/my-votes/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/neighbor/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/notification-settings/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/notifications/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/officer-agendas/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/personal-lists/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/profile/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/settings/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/signup/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/features/withdrawal/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/lib/api/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `frontend/src/lib/navigation/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |
| `supabase/README.md` | 원본 유지 | `—` | 기존 내용 그대로 유지 |

`docs/integration/originals/{main,front,back}/`에 47개 원문 버전을 보존했다. `preservation-manifest.json`은 source branch/SHA, 원래 경로, 보존 경로와 SHA-256을 기록한다. 47개 모두 source blob ↔ working file ↔ committed blob이 byte 단위로 동일함을 확인했다. 기존 root/backend/frontend README, AGENTS, API, 정책, 협업, DB/ERD 문서가 대상에 포함된다. 양쪽에서 동일하거나 해당 branch 원문이 그대로 남은 문서는 불필요하게 복제하지 않았다. 원본의 의도적 Markdown 두 칸 공백 등을 그대로 보존해 일부 inherited whitespace 경고가 남는다. 새 검토본8개는 diff-check 통과다. 원본 whitespace를 임의 변경하지 않았다.

## 포함 검증

- `git merge-base --is-ancestor origin/front/develop HEAD`: **0**.
- `git merge-base --is-ancestor origin/back/develop HEAD`: **0**.
- 그래프/log/status 확인: 두 merge commit, merge 미완료 없음, unresolved conflict0.
- origin/main22파일, origin/front/develop352파일, origin/back/develop365파일의 모든 경로가 통합 worktree에 존재한다. 소스 파일 삭제/누락0.
- `frontend/` tree = front/develop tree `f5b1a749ddcff21864b050b8aabec3c9276a003e`와 정확히 동일.
- `backend/` tree = back/develop tree `c80cd0ca3f3b36ce4f5cf3bb11f40f6bf3bfade8`와 정확히 동일.
- frontend workflow는 FE 원본, backend/supabase/scripts/render.yaml/backend workflow는 BE 원본과 diff0.
- main 대비 최종 삭제0. root config·Docker/env examples·CI 양쪽 유지.
- 새 검토본8개의 Git conflict marker0. 제품 정책 미확정은 별도 문서 상태로만 남아 있으며 Git conflict와 구분한다.

## Frontend 검증

`frontend/package.json` 실제 scripts만 실행했다. Windows npm.cmd 대신 Linux npm을 사용했다.

| 항목 | 명령 | 결과 |
| --- | --- | --- |
| dependencies | `npm ci --ignore-scripts --no-audit --no-fund` | 성공 |
| typecheck | `npm run typecheck` | 통과, exit0 |
| lint | `npm run lint` | 통과, exit0 |
| test | — | 미실행 — test script 없음. 테스트 소스 파일은 있으나 runner/script가 설정돼 있지 않음 |
| build | `npm run build` | 통과, exit0 |

환경 기본 Node24.19.0/npm11.9.0은 engine-strict에 막혔다. 저장소 engines에 맞는 Node24.21.0/npm11.19.0을 저장소 밖 임시 toolchain에 설치하고 다시 실행했다. package.json/lockfile/.npmrc는 수정하지 않았다.

## Backend 검증

Java17 / Spring Boot4.0.8 / Gradle Wrapper9.7.1을 실제 build.gradle과 wrapper에서 확인했다.

| 항목 | 명령/조건 | 결과 |
| --- | --- | --- |
| test | `sh ./gradlew --no-daemon test build --console=plain` | 실행 시도, Wrapper 배포 다운로드 실패로 테스트 미시작 |
| build | 위 명령의 build task | 미검증 — Wrapper 다운로드 전 중단, 소스 컴파일 실패로 판정하지 않음 |
| DB/Provider opt-in | 실제 credentials 미제공 | 실행하지 않음. 임의 secret/API key/원격 DB 설정을 만들지 않음 |

services.gradle.org 배포 ZIP 다운로드는 Java Network is unreachable, 같은 URL의 curl 및 공식 downloads.gradle.org 대체 경로는 proxy CONNECT timeout이었다. 로컬 검증 도구 문제이며 branch 기존 CI 성공을 이번 통합의 test/build 성공으로 대체하지 않았다. 현재 환경에서 실행한 test 수/skip 수는 없으므로 0통과나 일부 skip으로 꾸미지 않는다.

## FE/BE Contract

아래는 실제 소스의 정적 대조다. 실제 HTTP wire 사용자 흐름 검증을 대신하지 않는다.

| 항목 | Frontend | Backend | 판정 |
| --- | --- | --- | --- |
| Base URL | NEXT_PUBLIC_API_BASE_URL 빈 예시, createApiClient explicit injection | PORT 기본8080 | 확인 필요 — 실제 주소·주입 없음 |
| Prefix | client가 prefix 자동 추가 안 함 | 제품 `/api/v1`, health `/health` | adapter/base에 prefix 포함 필요. health는 제품 prefix 밖임 |
| 일반 게시판 목록 | BoardPage/toBoardQuery UI/Mock 존재 | GET `/api/v1/posts` Controller 없음 | **Blocker, BE1 #17 open** |
| 개인 활동 횟수 | MyPage service port | GET `/api/v1/users/me/activity` 없음 | 해당 기능 Blocker, BE1 #5 open; 다른 count로 대체 금지 |
| Method/endpoint | 대부분 화면 service port, 운영 wire adapter 없음 | 가입POST·프로필GET/PATCH·투표PUT·반응PUT/DELETE·공유GET·사진POST/PUT/GET/DELETE | 구현 API는 보존했지만 FE가 실제 소비하는 endpoint/method는 미연결 |
| ID type | 주로 string, region Mock ID도 slug | JSON positive safe integer ≤9007199254740991 | 숫자↔검증된10진string 변환과 region/subject 정본 연결 필요 |
| post/reaction enums | LOCAL_AGENDA/LOCAL_ACTIVITY/VOTE, EMPATHY/NEEDED/CURIOUS | 동일 | enum 집합 일치(정적), wire shape 동일 의미 아님 |
| 사진 size | 10×1024×1024 =10,485,760 bytes | PhotoContent.MAX_BYTES=10,000,000 | **심각한 제한 차이**. 10,000,001~10,485,760을 FE는 허용/BE는 거부. 계약 임의 수정 안 함 |
| 사진 전송 | PhotoSelection/File·상태 port/Mock | reserve→PUT RAW content→complete, 소유권 검사; Storage 서버 중계 | 실제 adapter와 예약/소유자 fileId·nullable/status·취소200/202 연결 필요 |
| 기관 상태 | none/unknown/completed/expired, subjectId·기관stringID·responsibleRegions 배열 | NOT_SUBMITTED/COMPLETED/EXPIRED, data.id 회원number·단일 responsibleRegion|null | §15/PR154 변환 계약 있음, FE 구현/사람 승인·실제 흐름은 별도 |
| 지도 Request | MapViewport centerRegionId·zoom | regionIds 필수CSV, centerRegionId 선택 | viewport→실제 visible region ID 집합/geometry 연결 미구현 |
| 목록 Response | pageInfo.nextCursor?:string | meta.nextCursor:string|null, hasNext:boolean | adapter의 nullable/paging 변환 필요 |
| 요약 enum | SUCCEEDED/SOURCE_TOO_SHORT/FAILED | 해당 상태 + PENDING | 중복 요청 등의 PENDING을 FE 상태로 변환 필요 |
| 투표 Request | string option ID·선택/변경 UI | number optionId, confirmChange boolean 모두 필수 | adapter에서 입력/서버 결과 변환 필요. 임시 선택 자동 제출 금지 |
| success envelope | decodeApiResponse(data) opt-in | data 또는 data+meta | 공통 data 원칙 일치; 도메인 decode/추가 meta 처리는 미주입 |
| error envelope | ApiError는 kind/status와 optional mapError | 최상위 code/message/details/traceId | custom mapError 미주입 시 도메인 code/details가 연결 안 됨. 403 USER_REGISTRATION_REQUIRED를 일반 금지와 구분 필요 |
| Authorization | prepareRequest hook 존재, auth/session adapter 미제공 | Privy access token Bearer 직접 검증 | 전달 구조 지원, 실 토큰/동일 앱/갱신·회원조회 연결 미완 |
| Privy | AppProviders가 sessionAdapter 없이 loading. package에 Privy SDK 없음 | 앱ID/JWKS·server app secret verified email | 실제 앱·SDK·OTP·subject/member 연결 필요, 합성 인증을 등록하지 않음 |
| 공유 | shared route/token 구조·Mock port | X-Post-Share-Token·post-bound 서명/7일 | 공유 data source/wire adapter·header 연결 확인 필요 |
| CORS | 다른 origin API를 fetch할 수 있는 generic client | CORS mapping/annotation/config 소스 없음 | 직접 cross-origin 방식은 **Blocker**. OPTIONS 예외는 허용 header 발급과 다름. proxy/CORS 방식을 #30에서 결정 |
| Storage | 앱 client에 secret 없음 | Supabase 서버 key, PHOTO flags 기본false | 키·버킷·DB permissions·실제10MB·relay/cleanup 검증 미준비 |
| 환경변수 | API base만 빈 예시; 실제 .env 없음 | DB/Privy/Storage/Gemini placeholder 예시; 실제 .env 없음 | 정상값 임의 생성 안 함, 서버 secret FE 공개 금지 유지 |
| AI Provider | summary display port/fallback | Gemini3.5 Flash-Lite, flags 기본false·revision 저장 | 개념 일치, 키/실 호출/adapter 흐름 미검증 |

공유 상세의 `frontend/src/features/share/SharedPostScreen.tsx`는 summary/comment/evaluation/vote Mock service를 내부에서 생성한다. 일부 composition port만 교체해도 공유 기능 전체가 실 API로 바뀌지는 않는다. Mock은 요청 범위대로 유지했으며 교체·guest credential·토큰 전달은 후속 Integration의 명시 작업이다.

**확인 필요 — 제품 범위:** FE PRD/기능명세·FE Git·통합 지침은 관심 지역/키워드, 추천, 알림/설정/예약, 신고, 참고 링크/익명/임시저장, 설정/로그아웃, Privy 이메일 변경/탈퇴 등 UI/Mock을 복구했다. BE 문서 및 열린 #74/#30/#31은 기존 MVP 제외 정책을 유지한다. 이는 'FE Mock은 가능 / BE 후순위'의 두 축으로 정리 가능한 부분과 실제 제품 정책 상충이 섞여 있다. 최신 사용자 합의가 양쪽 문서에 일치한다고 판단할 근거가 없어 이번 작업에서 어느 쪽도 최종 정책으로 확정하지 않았다. API 추가·FE 삭제로 해결하지 않았다.

## 실제 연동

| 항목 | 결과 |
| --- | --- |
| Backend 실행 | 불가 — Gradle/운영 JAR 미준비. 제품 실행에는 실제 DB/Privy/환경 준비도 필요 |
| Frontend 실행 | `npm run start -- --hostname 127.0.0.1 --port 3100` 시작 확인, 검사 후 종료 |
| Frontend HTTP | `/`, `/home`, `/dev/shared-preview`, `/shared/posts/1?token=review-only` 모두200(text/html). 화면 shell 응답만 확인한 것이며 권한/공유 token/업무 성공이 아님 |
| Health | 미검증 — Backend 실행 못 함. 소스 계약은 GET /health→data.status UP |
| FE→BE | 미검증 — BE 미실행·실 adapter 미주입·credentials 없음 |
| 미검증 사유 | 환경 미준비로 실행 검증 불가(Backend/실제 연동). FE 자체 실행만 검증 |

## Git

- local `integration/develop`: 두 develop 전체 이력을 포함하며 위 두 merge 이후 보고서만 추가.
- push: **미실행**. 사용자 §21 조건상 심각한 Contract 차이와 Backend build 미검증·정책 확인 필요가 남아 원격에 올리지 않는다.
- origin/integration/develop: 존재하지 않음. origin/main/front/develop/back/develop의 SHA는 최초/최종 조회 동일.
- integration worktree: clean, 진행 중 merge 없음. branch upstream이 origin/main으로 잘못 남지 않도록 해제했다.
- main/front/develop/back/develop에 commit·merge·push 없음. main의 파일을 작업용으로 수정하지 않음.
- CI: 기존 workflow2개 보존. 둘 다 integration/develop push를 자동 검증 대상으로 포함하지 않는다. frontend workflow는 front/develop PR만, backend workflow는 back/develop/main PR 및 BE feature/develop push다. workflow_dispatch는 있으나 이번 작업에서 원격 실행/정책 변경 안 함.
- Docker/config: BE Dockerfile.vercel·render.yaml와 root/영역별 env 예시 보존. 현재 CI/배포 경로의 integration branch 지원은 후속 BE2 확인 필요.

## 발견된 후속 문제

| 내용 | FE/BE 담당 | 파일/근거 | Blocker 여부 |
| --- | --- | --- | --- |
| FE 복구 범위↔BE MVP 제외 충돌 최종 정책 확인 | FE1/FE2·BE1/BE2, 제품 결정자 | 새 PRD/기능/통합/Git 검토본, #74/#30/#31 | **Yes — C 판정 근거** |
| 10MB byte 수치 불일치 | FE2·BE2/API Owner BE1 | frontend/src/features/post-editor/model.ts; backend/src/main/java/com/discushion/photos/PhotoContent.java | **Yes — 사진 연동/push** |
| 통합 게시판 목록 endpoint 미구현 | BE1·FE2 | backend/src/main/java/com/discushion/posts/; #17 open | **Yes — 게시판 연결** |
| 활동 횟수 API/실제 등록 이벤트 미구현 | BE1·FE1 | #5 open, my-page service, API 계약 검토표 §18/20 | **Yes — 해당 마이 활동 기능** |
| 실 API/Auth/service adapter 미주입 | FE1/FE2·BE1/BE2 | frontend/src/app/providers.tsx; auth/session-adapter.tsx; 표시 models | **Yes — 모든 실 연결** |
| CORS 또는 same-origin proxy 방식 미구성 | BE2·FE | backend Controllers/config, #30 | **Yes — 직접 cross-origin 연결** |
| 숫자ID/string·기관 단일↔배열/상태/nullable·map regionIds·PENDING·paging/error 매핑 | FE1/FE2·각 BE 담당, 공통 BE1 | frontend/src/features/**/model.ts, features/share/SharedPostScreen.tsx, API 검토표 §15~20, controllers | **Yes — 각 도메인 실 adapter** |
| Gradle 다운로드 차단·BE test/build 검증 불가 | 검증 환경·BE2 | backend/gradle/wrapper/gradle-wrapper.properties | **Yes — push 기본 검증 미충족** |
| 실제 Privy/DB/Storage/Gemini 및 photo flags 준비 | BE2·BE1·FE | backend/.env.example, #30/#75 | **Yes — 실제 실행** |
| integration branch CI 자동 검증 미설정 | BE2·FE 공통 CI 담당 | .github/workflows/backend-ci.yml; frontend.yml | 후속 필요 — 미검증 상태를 CI통과로 표시 금지 |

## Issue/PR 근거

2026-10-09 확인: [#74](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/issues/74), [#17](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/issues/17), [#5](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/issues/5), [#30](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/issues/30), [#31](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/issues/31)는 open. [PR#84](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/pull/84)는 FE 문서 동기화로 merged(body 없음). FE 문서 확대의 후속 직접 commit78c4d6c도 확인했다. [PR#154](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/pull/154)는 기관 조회 계약·FE port 변환을 기록하고 재승인 후 merged이며, 실제 FE adapter 완료로 보지 않는다. 첨부 v10 문서는 이전 기준이며 저장소의 각 branch v10.2·추가 계약/결정과 구분했다. Issue/PR 내용은 참조만 했으며 외부 댓글·리뷰·메시지 전송/상태 변경은 하지 않았다.

## 최종 판정

**C. Conflict/정책 확인 필요 — 연결 시작 금지.**

Git 이력·파일·문서 보존은 성공했다. 실제 연결 가능/완료 판정은 아니다. 정책 충돌을 확인하고 FE/BE wire adapter·사진 byte 제한·미구현 endpoint·실제 환경·BE build/test를 각 담당 Feature→develop 흐름으로 해결한 뒤 최신 develop을 다시 integration에 직접 merge해 재검증해야 한다. main은 건드리지 않는다. 조건 통과 전 push하지 않는다.

## 로컬 결과 복구

원격 push 대신 검토용 `Discushion_integration_develop_2026-10-09.bundle`을 제공한다. 이 bundle은 이미 원격에 있는 기준 main/FE/BE 전체를 중복하지 않는 증분이며 위 세 기준 commit이 필요하다.

1. 대상 저장소에서 `git status`, `git worktree list`, `git fetch origin --prune`를 확인한다.
2. `git bundle verify <다운로드한 bundle 경로>`로 전제 commit을 확인한다.
3. local integration/develop이 없는 경우 `git fetch <bundle 경로> integration/develop:integration/develop` 후 별도 worktree에 연결한다.
4. 기존 local integration이 있으면 덮거나 force하지 말고 HEAD·이력 차이를 먼저 확인한다. 복구해도 원격 push 조건은 그대로 유지한다.
