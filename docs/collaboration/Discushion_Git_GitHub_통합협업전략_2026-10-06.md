# Discushion Git & GitHub 협업 전략

> 작성일: 2026-10-06 · 팀 개발 실무 가이드
> 제품 기준: Discushion PRD 및 기능명세서 v10.1 — 2026-10-06 MVP 반영 정리본
> 운영 방식: Part 장기 브랜치 + 협업 문서 + 사람이 검토하는 로컬 병합

## 1. 목적

팀원이 **어느 브랜치에서 무엇을 개발하고, 어떤 순서로 검증·공유·통합하는지**를 정한다. GitHub Issue와 일반 기능 개발 PR을 사용하지 않고도 작업 범위, API 계약, 통합 이력을 추적한다.

제품 동작과 MVP 포함 여부는 아래 두 기준 문서를 따른다. 이 가이드는 제품 정책이나 실제 코드 폴더·API 경로를 새로 정의하지 않는다.

- `Discushion_PRD_2026-10-06_MVP반영_정리본_v10` — 문서 내부 버전 v10.1
- `Discushion_기능명세서_2026-10-06_MVP반영_정리본_v10` — 문서 내부 버전 v10.1

명령은 해당 저장소의 작업 폴더에서 한 줄씩 실행한다. 예시의 `backend-part-c`는 자신의 Part 브랜치로 바꾼다. `<팀_저장소_URL>`처럼 꺾쇠로 표시한 값은 실제 값으로 바꾼 뒤 실행한다.

## 2. 핵심 원칙

1. 일반 개발은 자신이 맡은 Part 브랜치에서만 한다.
2. 기능별 feature 브랜치를 추가로 만들지 않는다. 같은 Part에서 세부 작업을 순차적으로 구현한다.
3. 작업 시작과 통합 준비 시 최신 `origin/develop`을 로컬 `develop`에 받고, 그 `develop`을 Part에 병합한다.
4. 세부 작업마다 검토·확인·commit한다. Part 전체를 구현한 뒤 한 번만 commit하지 않는다.
5. Part 간 코드 전달은 `develop`을 거친다. 다른 Part를 직접 merge하거나 그 코드를 cherry-pick해 우회하지 않는다.
6. Part와 develop에서 각각 test/build한다. 실패한 결과를 develop에 push하지 않는다.
7. develop 반영은 한 번에 한 사람만 한다. 시작·완료·보류 상태를 팀 채팅에 공지한다.
8. Issue 대신 협업 문서로 작업을 관리한다. 일반 기능 개발은 PR 없이 로컬에서 검토·병합한다.
9. Codex에는 세부 작업 하나를 요청한다. 사람이 변경을 검토하고 commit·통합·push를 판단한다.
10. `main`은 최종 시연·배포·제출이 가능한 상태로 유지한다.

**매일 따라갈 흐름**

```text
origin/develop → 로컬 develop 최신화 → develop을 내 Part에 merge
→ 세부 작업 구현·확인·commit → Part 원격 공유
→ Part 완료 시 12장의 develop 통합 절차
```

## 3. 브랜치 구조

```text
main
└── develop
    ├── frontend-part-a
    ├── frontend-part-b
    ├── frontend-part-c
    ├── frontend-part-d
    ├── backend-part-a
    ├── backend-part-b
    ├── backend-part-c
    └── backend-part-d
```

- 브랜치명은 위 이름 그대로 소문자를 사용한다.
- Part 브랜치는 장기 사용한다. develop 반영 후에도 삭제·재생성하지 않고 다음 작업 전에 develop을 다시 받는다.
- 이 그림은 브랜치의 역할과 시작 기준을 나타낸다. Git이 브랜치를 폴더처럼 부모·자식으로 관리한다는 뜻은 아니다.
- FE/BE가 한 저장소면 위 구조를 함께 사용한다. 별도 저장소면 각 저장소에 `main`, `develop`, 해당 파트의 A~D를 두고 같은 규칙을 적용한다. 저장소 구성은 실제 팀 저장소를 따른다.

### 최초 준비 — 담당자가 한 번만 수행

`origin`이 팀 GitHub 저장소인지, `main`·`develop`이 준비되어 있는지 확인한다. develop은 안정된 main에서 시작한다. 기존 브랜치를 덮어쓰지 않는다.

```bash
git remote -v
git fetch origin
git branch -a
```

새 PC에서 원격 develop과 기존 Part를 처음 사용하는 경우:

```bash
git switch --track origin/develop
git switch --track origin/backend-part-c
```

로컬에 이미 같은 이름이 있으면 `--track`으로 다시 만들지 않고 `git switch develop`, `git switch backend-part-c`를 사용한다.

팀 원격에도 Part가 없는 경우에만, 담당자가 최신 develop에서 생성한다.

```bash
git switch develop
git pull --ff-only origin develop
git switch -c backend-part-c
git push -u origin backend-part-c
```

### GitHub 설정 확인

- 팀원에게 자신이 작업·공유할 저장소의 쓰기 권한을 제공한다.
- main 반영 담당자를 정하고 협업 문서에 기록한다. develop 반영자는 14장의 공지 절차를 따른다.
- `main`·`develop`에는 가능한 보호 설정에서 force push와 브랜치 삭제를 제한한다.
- 이 운영 방식은 로컬 병합 후 직접 push하므로 **필수 PR/리뷰 설정이나 merge commit을 금지하는 선형 이력 설정을 적용하면 절차와 충돌한다.** 저장소 관리자가 보호 규칙·ruleset을 이 가이드에 맞게 확인한다. 지원되는 접근 제한은 저장소 종류·플랜에 따라 확인한다. [GitHub 브랜치 보호 문서](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- 보호 설정 때문에 push가 거절되면 개인이 우회하거나 force push하지 않고 저장소 관리 담당자에게 확인한다.

## 4. Part A~D 구성

Frontend와 Backend의 **같은 Part 문자는 같은 제품 영역**이다. FE는 화면·상태·API 연결을, BE는 API·데이터·로직·권한을 맡는다. 기존 기능 ID는 작업 추적에 그대로 사용한다.

| Part | 제품 영역 | 주요 범위 | 연결 기능명세 ID |
| --- | --- | --- | --- |
| A — Account & Authority | 계정·프로필·지역·인증 | 회원가입, 로그인, 시작/공통 메뉴, 프로필, 활동 지역, 이웃 인증, 기관 인증 및 인증 상태·권한·파란 배지 | `F-WLXSSC`, `F-TSOXGG`, `F-KZRSXU`, `F-RBVFZX`, `F-QQKYLC`, `F-ATWJDJ`, `F-OPNIXL`, `S-YLSPHQ`, `S-JRMYIV`, `F-GDASNA`, `F-MUBDJD` |
| B — Content & Post | 콘텐츠 탐색·게시물 관리 | 메인, 통합 게시판, 공통 상세, 지역 안건, 지역 활동 정보, 투표 게시물 작성·관리, 사진, AI 안건 요약, 이슈 지도 | `F-UPRLMN`, `F-EAJPVC`, `F-PUDHYO`, `F-UCDVNA`, `F-FTLHCX`, `S-NVXXYQ`, `S-TBFIHO`, `F-GSMCLD`, `F-WSCKDN`, `F-QIGKAK` |
| C — Participation | 주민 참여·공유·게스트 | 세 게시물 반응, 댓글·답글, 댓글 평가·정렬, 실제 투표 참여·선택 변경, 공유 링크, 게스트 접근 | `F-GOMLGG`, `S-HNVDPO`, `F-EDNVWZ`, `S-JCEZAP`, `S-YYDGUS`, `F-CDIBRF`, `S-OXTKEP`, `F-FCPVIS`, `S-CMGJIG`, `F-OWFYWE`, `S-NYUECP` |
| D — Personal & Institution | 개인 기록·기관 안건 업무 | 북마크, 마이페이지, 개인 활동 횟수, 내가 만든/참여한 게시물, 참여한 투표, 기관 담당자 안건 목록, 채택·취소 | `F-FYQJPT`, `F-WYMXXP`, `F-SSHXAA`, `F-NZTUYE`, `F-QPGNCF`, `F-CNNPYL`, `S-PCCNUU`, `F-TUGMEP`, `S-AQOBIE` |

### Part 경계와 선행 작업

| 함께 쓰는 대상 | 책임과 연결 방법 |
| --- | --- |
| 인증·지역 참여 자격 | A가 인증 상태·지역·권한 판정 계약을 제공한다. B/C는 게시·참여에, D는 기관 화면·채택에 필요한 조건을 사용한다. 운영자 심사는 v10.1의 비-MVP 범위를 유지한다. |
| 공통 게시물 상세 | B가 게시물 원본과 상세의 공통 구조를 담당한다. C의 참여 영역, D의 북마크·채택 표시는 계약을 맞춰 연결한다. |
| 투표 | B는 투표 게시물의 작성·수정·삭제 및 작성 제약, C는 표 제출·선택 변경·집계, D는 개인 참여 기록 조회를 맡는다. 투표 원본을 중복 생성하지 않는다. |
| 기관 | A는 인증 상태·담당 지역·기관 권한·배지, D는 전체 안건 조회와 담당 지역 채택·취소를 맡는다. |
| 활동 기록 | 각 Part가 만드는 행동과 D의 집계 방식을 계약에 기록한다. D가 목록·횟수 조회를 담당하고 원본 참여 데이터를 중복하지 않는다. |
| 공유·로그인 복귀 | C가 공유/게스트 흐름을, A가 로그인·가입 및 복귀 정보를, D가 게스트 북마크 유도를 담당한다. 자동 행동 실행 없이 기존 복귀 정책을 연결한다. |

공통 DTO·인증 처리·DB 변경·공통 화면 구조를 함께 만져야 하면 먼저 담당자와 변경 범위를 협업 문서에 적는다. 담당자가 구현·검증한 변경을 develop에 올린 뒤 다른 Part가 받는다. API·공통 구조가 아직 없으면 계약을 먼저 맞추고, 의존 작업은 `대기`로 표시한다.

Part 분류는 작업 소유권이다. 기존 ID 안에 비-MVP 동작이 섞여 있어도 이를 전부 구현하는 요청으로 해석하지 않는다. AI 추천·알림·신고·계정 복구/탈퇴·운영자 백오피스 등은 v10.1의 범위 표기를 따른다.

## 5. 브랜치별 역할

| 브랜치 | 해야 하는 일 | 하지 않는 일 |
| --- | --- | --- |
| `main` | 최종 검증된 develop을 받아 시연·배포·제출 상태로 유지 | 일반 기능 개발, 미검증 Part 직접 병합 |
| `develop` | 모든 Part의 검증된 작업을 통합하고 통합 test/build | 기능을 직접 구현하거나 Part 간 전달을 우회 |
| `frontend-part-a` ~ `frontend-part-d` | 해당 FE Part의 연관 기능을 세부 작업별로 순차 구현 | 기능마다 브랜치 추가 생성, 다른 담당자의 Part에서 임의 개발 |
| `backend-part-a` ~ `backend-part-d` | 해당 BE Part의 연관 기능을 세부 작업별로 순차 구현 | 미완료·미검증 코드를 develop으로 전달 |

한 Part 브랜치의 개발 담당자를 협업 문서에 명시한다. 공동 작업이 필요하면 담당자와 순서·파일 범위를 먼저 합의한다. 다른 Part의 선행 코드는 그 담당자가 develop에 반영한 뒤 받는다.

## 6. 협업 문서 작업 관리 방식

GitHub Issue를 작업 관리 수단으로 사용하지 않는다. 일반 기능 개발에서 PR도 사용하지 않는다. GitHub은 **원격 저장소·브랜치 공유·코드 이력**을 담당하고, 작업 계획과 검토·검증 기록은 별도 협업 문서에 남긴다.

### 작업 표 — 세부 작업마다 한 행

| Part | 담당자 | 세부 작업 | 상태 | 관련 기능명세 ID | 관련 API | FE/BE 연동 필요 | 완료 여부 | 테스트 여부 | 특이사항 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| BE C | 담당자 기입 | 게시물 반응 등록/취소 | 대기 | F-GOMLGG, S-HNVDPO | 실제 API 명세의 Method·Endpoint | 예 | 아니오 | 미실행 | A의 권한 계약·B의 게시물 원본 필요 |
| FE C | 담당자 기입 | 게시물 반응 UI·API 연결 | 대기 | F-GOMLGG, S-HNVDPO | BE C와 같은 계약 | 예 | 아니오 | 미실행 | 버튼별 상태·집계 연결 |

- 상태는 `대기 → 진행 중 → 검증 중 → 완료 → develop 반영 완료`로 기록한다. 진행 불가 시 `보류`와 이유를 남긴다.
- `완료`는 세부 작업 구현과 해당 검증이 끝났다는 뜻이다. develop 반영까지 끝난 상태와 구분한다.
- 테스트 여부에는 통과/실패/미실행뿐 아니라 **실행 명령·검증 커밋·결과·확인자**를 적는다.
- API는 실제 명세를 연결한다. 이 문서의 예시를 근거로 존재하지 않는 Endpoint를 만들지 않는다.
- commit SHA와 develop 반영 SHA를 기록해 변경 이력을 찾을 수 있게 한다.
- 일반 기능 PR 대신 사람의 변경 검토 결과·확인자도 이 문서에 기록한다.

### 복사해서 사용하는 Part 작업 카드

```markdown
[Backend Part C]

브랜치: backend-part-c
담당자:
상태: 대기
통합 대상 작업:

작업 목록:
- [ ] 게시물 반응 등록/취소
- [ ] 댓글/답글 작성
- [ ] 댓글/답글 좋아요·싫어요
- [ ] 댓글 좋아요순/최신순 정렬
- [ ] 투표 참여
- [ ] 투표 선택 변경
- [ ] 게시물 공유/게스트 권한 처리

관련 기능명세:
- F-GOMLGG / S-HNVDPO
- F-EDNVWZ / S-JCEZAP / S-YYDGUS / S-OXTKEP
- F-CDIBRF
- F-FCPVIS / S-CMGJIG
- F-OWFYWE / S-NYUECP

관련 API:
- 실제 API 명세 링크:
- 작업별 Method / Endpoint:
- Request / Response / Error / 권한 계약:

FE/BE 연동:
- 상대 담당자:
- 연결할 화면/API:
- 계약 변경 통보 기록:

검증:
- [ ] 기능 단위 테스트
- [ ] build
- [ ] FE/BE 연동 확인
- [ ] develop 통합 테스트
- 실행 명령 / 결과:
- 검증 커밋 SHA / 확인자:

완료 및 반영:
- 세부 작업 완료 여부:
- 사람의 diff 검토 결과:
- Part commit SHA:
- origin/develop 반영 SHA:

비고:
- 의존 작업 / 막힌 이유 / MVP 범위 / 특이사항:
```

작업 카드는 Part 전체를 한 번에 구현하는 지시가 아니다. Codex 요청과 commit의 기본 단위는 **카드 안의 세부 작업 하나**다.

## 7. 개발 시작 절차

### 시작 전에 작업 폴더를 비운다

```bash
git branch --show-current
git status
```

`working tree clean`을 확인한다. 미커밋 변경·충돌·진행 중 merge가 있으면 브랜치 전환을 멈춘다. 본인의 변경을 원래 Part에서 검토·commit하거나 담당자와 보관 방법을 확인한다. 정리를 위해 `reset --hard`나 파일 삭제를 사용하지 않는다.

### 매번 수행하는 6단계

| 순서 | 실행 위치·작업 |
| --- | --- |
| 1 | 로컬 `develop`으로 이동 |
| 2 | GitHub `origin/develop`의 최신 내용을 로컬 `develop`에 pull |
| 3 | 자신의 Part 브랜치로 이동 |
| 4 | 최신 로컬 `develop`을 Part에 merge |
| 5 | 충돌이 있으면 15장에 따라 직접 확인·해결하고 test/build |
| 6 | 협업 문서의 세부 작업 하나를 선택해 개발 시작 |

```bash
git fetch origin

git switch develop
git pull --ff-only origin develop

git switch backend-part-c
git merge develop

git status
```

같은 Part를 다른 PC에서 이어서 작업한다면 **아직 새 로컬 작업이 없는 상태**에서 Part로 이동한 다음 `git pull --ff-only origin backend-part-c`로 본인 원격 Part도 최신화하고 `git merge develop`을 실행한다. 로컬과 원격이 갈라져 pull이 실패하면 강제로 덮지 않고 담당자와 확인한다.

### 이름과 방향 구분

| 이름·명령 | 의미 |
| --- | --- |
| `origin` | 팀 GitHub 저장소를 가리키는 원격 이름 |
| `origin/develop` | 마지막 fetch/pull로 갱신한, GitHub develop을 추적하는 로컬 참조 |
| GitHub의 `develop` | 다른 팀원의 push가 실제로 반영되는 원격 브랜치 |
| `develop` | 내 PC의 로컬 통합 브랜치 |
| `backend-part-c` | 내 PC에서 실제 BE Part C 작업을 하는 브랜치 |
| `git fetch origin` | 원격 최신 정보를 받는다. 로컬 develop이나 Part의 코드를 자동 병합하지 않는다. |
| develop에서 `git pull --ff-only origin develop` | 원격 내용을 받아 **현재 로컬 develop**을 최신화한다. |
| Part에서 `git merge develop` | **이미 최신화한 로컬 develop**을 현재 Part에 반영한다. 원격을 새로 조회하지 않는다. |

`git pull origin develop`과 `git merge develop`은 같은 작업이 아니다. pull은 원격을 가져와 **현재 체크아웃한 브랜치**에 통합한다. 따라서 Part에 있는 채로 `git pull origin develop`을 실행하면 로컬 develop 최신화가 되지 않는다. 우리 팀은 `git switch develop` 후 `git pull --ff-only origin develop`, 이후 Part에서 `git merge develop` 순서로 고정한다. `--ff-only`가 실패하면 이력 분기를 확인하고 중단한다. [Git pull 공식 문서](https://git-scm.com/docs/git-pull)

## 8. Part 브랜치 개발 규칙

```text
backend-part-c
  댓글/답글 작성 구현 → 확인·diff 검토 → commit
  게시물 반응 구현   → 확인·diff 검토 → commit
  투표 참여 구현     → 확인·diff 검토 → commit
```

1. 협업 문서에서 세부 작업과 기능명세 ID·API 계약을 확인한다.
2. 상태를 `진행 중`으로 바꾸고 허용 파일·수정 금지 범위를 정한다.
3. 하나의 세부 작업만 구현한다.
4. 기능 확인·관련 테스트와 사람이 하는 diff 검토를 마친다.
5. 관련 파일만 stage하고 의미 있는 메시지로 commit한다.
6. Part 원격에 정상 push한 뒤 협업 문서에 commit SHA·검증 결과를 적는다.
7. 다음 세부 작업으로 넘어간다.

```bash
git status
git diff
git add -- <확인한_변경_파일_경로>
git diff --cached
git commit -m "feat(be): 댓글 및 답글 작성 구현"
git push origin backend-part-c
```

`git diff`는 stage하지 않은 변경, `git diff --cached`는 commit에 포함될 변경을 확인한다. 새 파일도 `git status`와 실제 파일 내용으로 검토한다. 파일 경로를 실제 값으로 바꾸며, 무조건 `git add .`로 관련 없는 파일까지 넣지 않는다.

Part 브랜치 push는 자신의 코드 공유다. develop 통합이나 팀 전체 검증 완료를 의미하지 않는다. develop 반영 대기 중에도 자신의 Part 개발·push는 가능하다.

## 9. Commit 규칙

형식은 다음과 같다.

```text
<type>(<scope>): <작업 내용>
```

| type | 사용 상황 |
| --- | --- |
| `feat` | 기능 추가 |
| `fix` | 오류 수정 |
| `refactor` | 기능 동작을 유지하는 내부 구조 정리 |
| `test` | 테스트 추가·수정 |
| `docs` | 명세·API·협업 문서 수정 |
| `chore` | 공통 설정·개발 환경 작업 |

scope는 `fe`, `be`, `common`만 사용한다.

```text
feat(fe): 게시물 반응 UI 구현
feat(be): 게시물 반응 등록 및 취소 구현
feat(be): 댓글 및 답글 작성 구현
fix(fe): 투표 선택 상태 오류 수정
refactor(be): 게시물 조회 로직 정리
test(be): 투표 참여 테스트 추가
docs(common): 협업 문서 수정
chore(be): 공통 설정 수정
```

- `update`, `수정`, `작업함`, `코드 변경` 같은 메시지는 금지한다.
- 하나의 commit에는 의미가 연결된 변경을 담는다. 관련 없는 기능·전역 포맷 변경을 섞지 않는다.
- 작업을 잘게 나누되 테스트가 실패하거나 기능이 임의로 비활성화된 상태를 완료 commit으로 표시하지 않는다.
- 팀 통합 merge commit도 `chore(common): backend-part-c를 develop에 통합`처럼 방향을 기록한다.
- 원격에 공유한 commit을 amend/rebase로 다시 써서 force push가 필요해지는 방식은 사용하지 않는다. 후속 수정 commit으로 남긴다.

## 10. Codex 사용 규칙

Part 전체 구현을 한 번에 요청하지 않는다. 세부 작업 하나와 **현재 브랜치·관련 명세·API 계약·허용/금지 범위·완료 조건**을 함께 전달한다.

### 요청 예시

```text
현재 브랜치: backend-part-c
현재 작업: 게시물 반응 등록/취소
제품 기준: Discushion PRD/기능명세서 v10.1의 MVP 범위
관련 기능명세: F-GOMLGG, S-HNVDPO
관련 API: 협업 문서에 합의한 실제 API 계약을 첨부

구현 범위:
- 공감해요 / 필요해요 / 궁금해요
- 각 반응 독립 등록·취소 및 집계
- 동일 회원·게시물·반응 유형의 중복 처리
- 로그인 및 해당 지역 이웃 인증 권한 검증

수정 허용:
- Reaction 관련 코드
- 필요한 DTO / Service / Repository / Controller / Test
- 실제 저장소에서 위 범위에 해당하는 파일

수정 금지:
- 댓글 / 투표 / 기관 기능
- 관련 없는 리팩토링 / 전역 포맷 변경
- 새로운 API·제품 정책의 임의 결정
- 다른 Part 수정 / 브랜치 생성·전환
- develop/main merge·push
- 별도 요청 없는 commit·Part push

완료 조건:
- 등록 정상 / 취소 정상 / 집계 정상
- 중복 처리 정상 / 권한 검증 정상
- 관련 테스트 통과 / build 통과
- 실행 명령·결과·변경 파일·남은 제약 보고
```

Codex가 끝나면 사람이 다음을 실행한다.

```bash
git branch --show-current
git status
git diff
git diff --cached
```

요청 범위, 실제 변경 파일, 명세/API 일치 여부, 테스트 결과를 확인한다. 범위 밖 변경이 있으면 그대로 commit하지 않는다. 작성자와 확인해 해당 변경을 분리하거나 요청 범위로 수정한 뒤 다시 검토한다. 다른 사람의 변경을 자동으로 되돌리지 않는다.

**기본 원칙: Codex는 요청된 구현과 검증을 수행하고, develop/main 반영·push는 사람이 한다.**

## 11. 최신 develop 반영 방법

다른 팀원의 develop 반영 완료 공지를 받으면 자신의 작업을 commit하고 최신 develop을 Part에 반영한다.

```bash
git status
git fetch origin

git switch develop
git pull --ff-only origin develop

git switch backend-part-c
git merge develop

# 협업 문서에 등록된 관련 테스트와 build 실행
git status
```

다른 사람의 코드 때문에 test/build가 실패하면 기능을 꺼서 통과시키지 않는다. 해당 변경 담당자에게 재현·실패 결과를 공유하고 Part에서 해결·재검증한다.

```text
허용: Part A → develop → Part B가 develop 반영
금지: backend-part-a → backend-part-b 직접 merge
금지: frontend-part-b → frontend-part-c 직접 merge
```

## 12. Part 완료 → develop 통합 절차

**통합 대상은 Part 브랜치 전체 변경이다.** 협업 문서의 완료 체크만으로 merge할 commit이 선택되지는 않는다. 미완료 작업·실험 코드가 같은 브랜치에 섞여 있으면 통합을 보류한다.

### 두 방향 merge의 목적

| 방향 | 목적 | 검증할 질문 |
| --- | --- | --- |
| `develop → Part` | 최신 팀 통합 코드를 내 Part에 반영 | 내 Part가 최신 팀 코드와 함께 정상 동작하는가? |
| `Part → develop` | 검증을 끝낸 Part 작업을 팀 통합 브랜치에 반영 | 실제 통합 브랜치 전체가 정상 동작하는가? |

```text
origin/develop
    ↓ pull
로컬 develop 최신화
    ↓ develop → Part merge
Part test/build
    ↓ Part → develop merge
develop test/build + FE/BE 연동 확인
    ↓ push
origin/develop
```

### 12단계 실행표

| 순서 | 해야 할 일 | 다음 단계 진행 조건 |
| --- | --- | --- |
| 1 | origin/develop을 로컬 develop에 pull | 작업 폴더가 clean이고 최신화 성공 |
| 2 | 최신 develop을 Part에 merge | 충돌 해결·merge 종료 |
| 3 | Part에서 test/build, 변경 검토 및 Part 원격 push | 통과한 커밋이 원격 Part에 보관됨 |
| 4 | 팀 채팅에 develop 반영 시작 공지 | 다른 반영자가 없고 자신이 통합 순서를 확보함 |
| 5 | 로컬 develop으로 이동 | Part에 미커밋 변경 없음 |
| 6 | origin/develop을 다시 pull해 추가 변경 확인 | 추가 변경이 있으면 Part 반영·검증을 다시 완료 |
| 7 | Part를 develop에 merge | 통합할 전체 diff 검토·충돌 해결 |
| 8 | develop에서 전체 test/build | 모두 통과 |
| 9 | 필요한 FE/BE 연동 검증 | 계약·핵심 연결 정상 |
| 10 | 정상 결과를 origin/develop에 push | 통합 commit 완료·push 성공 확인 |
| 11 | develop 반영 완료 공지·협업 문서 갱신 | 반영 SHA·검증 결과 기록 |
| 12 | 다른 팀원이 최신 origin/develop을 자신의 Part에 반영 | 11장 절차 수행 |

### 실제 명령 — BE Part C 예시

**① Part를 최신 develop 기준으로 검증한다.**

```bash
git status
git fetch origin
git switch develop
git pull --ff-only origin develop

git switch backend-part-c
git merge develop

# Part에서 등록된 test/build 실행 — 실패하면 여기서 중단
git status
git log -1 --oneline
git push origin backend-part-c
```

테스트 결과를 협업 문서에 기록하고 14장의 **반영 시작 공지**를 보낸다. 자신의 차례를 확인한 뒤 다음 단계로 간다.

**② 공지 후 develop이 더 바뀌었는지 확인한다.**

```bash
git switch develop
git log -1 --oneline
git pull --ff-only origin develop
git log -1 --oneline
```

두 SHA가 다르면 다른 변경이 들어온 것이다. Part로 돌아가 `git merge develop` → Part test/build → Part push를 다시 수행한다. 그 뒤 develop으로 돌아와 원격 최신 여부를 재확인한다. **오래된 develop에서 검증한 결과를 그대로 통합하지 않는다.** 반영 대기·보류가 길어지면 팀에 상태를 알린다.

**③ develop에서 병합 결과를 검토하고 검증한다.**

```bash
git branch --show-current
git status
git log --oneline develop..backend-part-c
git diff --stat develop...backend-part-c

git merge --no-ff --no-commit backend-part-c
git status
git diff --cached --stat
git diff --cached

# develop에서 전체 test/build 실행
# 필요한 FE/BE 연동 확인
git diff --check
git diff --cached --check
```

`--no-ff`는 Part 통합 지점을 merge commit으로 남긴다. 여기에 `--no-commit`을 붙여 commit 직전에 멈추므로 사람이 통합 결과를 검토·검증할 수 있다. `git merge --no-ff backend-part-c`는 성공하면 merge commit까지 바로 생성한다. 이 가이드는 검증 후 commit하기 위해 `--no-ff --no-commit`을 사용한다. [Git merge 공식 문서](https://git-scm.com/docs/git-merge)

merge가 `Already up to date`라면 새로 통합할 변경이 없는 것이다. 불필요한 commit을 만들지 않고 협업 문서의 반영 상태를 확인한다. 충돌이나 실패가 있으면 아래 실패 절차로 간다.

**④ 모두 통과했을 때 통합 commit과 push를 한다.**

```bash
git commit -m "chore(common): backend-part-c를 develop에 통합"
git status
git log -1 --oneline

git fetch origin
git log --oneline develop..origin/develop
```

마지막 log에 원격의 새 commit이 표시되면 push를 멈춘다. 표시가 없으면 정상 push한다. 이 재확인 뒤에도 동시에 원격이 바뀔 수 있으므로 **push 성공 결과까지 확인**한다.

```bash
git push origin develop
git fetch origin
git rev-parse develop
git rev-parse origin/develop
```

통합 반영 중에는 두 SHA가 같아야 한다. 다르거나 push가 거절되면 완료 공지를 하지 않고 아래 절차를 따른다. 성공한 SHA를 협업 문서에 적고 **반영 완료 공지**를 보낸다.

### 충돌·검증 실패·push 거절 시

| 상황 | 처리 |
| --- | --- |
| Part에 develop merge 중 충돌 | 15장대로 당사자가 해결한다. test/build하고 Part를 push한 뒤 다시 통합 준비한다. |
| develop에 Part merge 중 충돌 | 통합을 멈추고 관련 담당자와 확인한다. 진행 중 merge를 취소한 뒤 최신 develop을 Part에 반영·해결·검증하고 통합을 재시작한다. |
| develop test/build·연동 실패 | develop에서 기능 수정 commit을 만들지 않는다. 실패 결과를 기록하고 진행 중 merge를 취소한다. Part에서 수정·검증·push 후 다시 시작한다. |
| 로컬 develop pull의 `--ff-only` 실패 | 로컬·원격 이력이 갈라졌거나 미푸시 통합이 남았는지 확인한다. force push/rebase/reset으로 밀어붙이지 않는다. |
| push가 non-fast-forward로 거절되거나 원격 새 commit 발견 | 원격에 추가 변경이 들어온 것이다. 반영권 상태를 확인하고 최신 코드 기준으로 Part·develop 검증을 다시 수행한다. |

merge 취소는 **작업 폴더가 clean인 상태에서 시작했고, 아직 merge commit을 만들지 않은 진행 중 merge**에만 사용한다. 보존할 변경이 있으면 먼저 담당자와 확인한다.

```bash
git status
git merge --abort
```

`git merge --abort`는 진행 중 병합을 취소한다. merge를 시작할 때 미커밋 변경이 있으면 원래 상태 복원이 어려울 수 있으므로 시작 전 clean 확인이 필요하다. [Git merge 취소 설명](https://git-scm.com/docs/git-merge)

이미 통합 commit을 만든 뒤 push가 거절되어 로컬 develop이 갈라진 경우, 초보 팀원은 기존 폴더를 보존하고 **새 클론에서 12장 절차를 다시 진행**한다. ①에서 검증된 Part를 원격에 push했으므로 그 작업을 다시 받을 수 있다.

```bash
# 다른 새 폴더에서 실행; 기존 작업 폴더는 보존
git clone <팀_저장소_URL> <새_작업_폴더>
cd <새_작업_폴더>
git fetch origin
git switch --track origin/develop
git switch --track origin/backend-part-c
```

clone이 이미 해당 브랜치를 체크아웃했거나 로컬 브랜치가 존재하면 `--track` 대신 `git switch 브랜치명`을 사용한다. 반영 순서를 다시 확보하고 12장의 최신화·두 번 검증을 모두 반복한다. 기존 로컬 develop을 원격에 강제로 밀지 않는다. 일반 push는 이력 손실을 막기 위해 non-fast-forward 갱신을 거절한다. [Git push 공식 문서](https://git-scm.com/docs/git-push)

이미 origin/develop에 올라간 통합에서 문제가 발견되면 팀에 즉시 알리고 main 반영을 보류한다. 담당 Part에서 수정한 commit을 같은 절차로 통합한다. 공유 이력을 임의로 삭제하지 않는다.

## 13. 테스트/빌드 규칙

### 실행 명령을 먼저 등록한다

기술 스택·실제 코드 경로가 이 문서의 입력에 없으므로 npm·Gradle 등 특정 명령을 팀 표준이라고 가정하지 않는다. 코드 저장소 준비 시 담당자가 아래 값을 **실제 설정·스크립트에 맞춰 협업 문서에 등록**한다. 빈 항목으로는 검증 완료를 표시하지 않는다.

| 검증 | 실행 위치 | 등록할 실제 명령·확인 방법 |
| --- | --- | --- |
| FE 기능 테스트 | FE 프로젝트의 실제 경로 | 기능 테스트를 실행하고 종료 결과를 확인할 명령 |
| FE build | FE 프로젝트의 실제 경로 | 배포용 build 명령 |
| BE 기능·통합 테스트 | BE 프로젝트의 실제 경로 | 관련 테스트와 전체 테스트 명령 |
| BE build | BE 프로젝트의 실제 경로 | build 명령 |
| FE/BE 연동 | 실제 개발 환경 | BE 실행·FE 실행 명령, 테스트 계정·인증 지역·확인 시나리오 |

### 두 번 검증한다

| 검증 위치 | 목적 | 완료 기록 |
| --- | --- | --- |
| 최신 develop을 반영한 Part | 내 코드가 최신 팀 코드와 함께 정상 동작하는지 확인 | Part SHA, 명령, 기능·test/build 결과 |
| Part를 병합한 develop | 팀 통합 브랜치 전체와 연결된 FE/BE가 정상인지 확인 | merge 후 전체 test/build·연동 결과, 최종 통합 SHA |

- Part 통합 준비 시 해당 파트의 테스트와 build를 모두 수행한다.
- 단일 저장소의 develop에서는 FE/BE 전체 test/build를 수행한다. 분리 저장소면 각 develop의 전체 검증과 합의한 FE/BE 연동 환경 검증을 수행한다.
- UI 변경은 정상 상태뿐 아니라 오류·로딩·권한·빈 데이터 상태도 관련 명세대로 확인한다.
- API 변경은 정상 요청, 입력 오류, 인증/권한 거부, 중복·상태 제약을 작업 범위에 맞춰 확인한다.
- 테스트를 건너뛰거나 build만 통과한 상태를 `test/build 통과`로 기록하지 않는다.
- 충돌 해결·추가 코드 변경 후에는 검증을 다시 실행한다. 실패를 고친 뒤에는 실패한 테스트가 실제로 다시 실행됐는지 확인한다.
- 검증 후 코드가 바뀌면 이전 통과 결과를 사용하지 않는다. 명령·시간·대상 commit·결과를 협업 문서에 남긴다.

FE/BE 연동이 필요한 작업인데 상대 API/UI가 없어 확인하지 못했다면 `연동 미확인`과 이유를 기록하고 통합 완료 조건으로 넘기지 않는다. 선행 작업을 먼저 합의해 순서대로 통합한다.

## 14. develop 동시 반영 방지

PR을 사용하지 않으므로 **원격 develop 반영은 한 번에 한 사람만** 한다. 반영권은 팀 채팅의 시작·완료 공지와 협업 문서의 현재 반영자 기록으로 관리한다.

1. 기존 반영 시작 공지가 진행 중인지 확인한다.
2. 진행 중이면 대기한다. 자신의 Part 작업·push는 계속할 수 있다.
3. 비어 있으면 시작 공지를 보내고 다른 요청과 겹치지 않았는지 확인한다. 동시에 요청했으면 순서를 합의한 한 사람만 시작한다.
4. 반영자가 작업을 끝내거나 보류·중단 공지로 반영권을 해제할 때까지 다른 사람은 origin/develop에 push하지 않는다.

```text
[develop 반영 시작]
BE Part C develop 병합 및 검증 시작합니다.
완료 공지 전까지 develop push를 잠시 보류해주세요.
반영 대상: backend-part-c / 검증한 commit SHA
```

```text
[develop 반영 완료]
BE Part C develop 반영 완료했습니다.
반영 SHA: 실제 develop commit SHA
검증: test/build 및 필요한 FE/BE 연동 통과
최신 origin/develop을 받아 자신의 Part에 반영해주세요.
```

```text
[develop 반영 보류 — 반영권 해제]
BE Part C 통합 검증에서 오류를 확인했습니다.
origin/develop에는 push하지 않았습니다.
Part에서 수정 후 다시 반영 시작 공지하겠습니다.
```

공지 후 오래 중단되면 현재 상태와 반영권 유지/해제를 명확히 알린다. 팀 채팅 공지는 실제 push 제한을 대신하는 기술적 잠금은 아니므로 마지막 원격 재확인과 push 결과 확인도 수행한다.

## 15. 충돌 해결

충돌이 나오면 다음 순서로 진행한다.

1. `git status`로 진행 중 merge와 충돌 파일을 확인한다.
2. `git diff --name-only --diff-filter=U`로 해결해야 할 파일을 모은다.
3. 어떤 변경이 최신 develop의 변경이고, 어떤 변경이 자신의 Part 작업인지 확인한다.
4. 기능명세·API 계약과 파일 담당자를 기준으로 최종 코드를 판단한다. 판단이 안 되면 해당 담당자와 확인하고 보류한다.
5. 충돌 표시를 제거하되 양쪽의 필요한 동작을 보존한다. 다른 사람의 기능·검증을 임의로 삭제하지 않는다.
6. 해결한 파일만 stage하고 충돌 파일이 남지 않았는지 확인한다.
7. test/build를 실행한다. 필요한 연동 검증도 수행한다.
8. Part에서 진행한 merge라면 검증 후 merge commit을 완료하고 협업 문서에 해결 내용을 기록한다. develop에서 발생한 경우는 12장의 취소·Part 재검증 절차를 따른다.

```bash
git status
git diff --name-only --diff-filter=U

# 담당자와 확인하여 충돌 파일 수정
git add -- <해결한_파일_경로>
git diff --name-only --diff-filter=U
git diff --cached

# test/build 실행 — 모두 통과하고 진행 중 Part merge인 경우
git commit -m "chore(be): backend-part-c에 develop 변경 병합"
```

**자동으로 전부 ours/theirs를 선택하지 않는다.** 현재 브랜치가 Part인지 develop인지에 따라 양쪽의 의미도 달라진다. Codex에게 전체 충돌을 무조건 해결하게 맡기지 않는다. 특정 충돌의 원인·대안 분석을 요청할 수 있지만 최종 채택과 검증은 담당자가 한다.

## 16. FE/BE API 연동

같은 Part의 FE/BE 담당자가 구현 전에 다음을 합의하고 협업 문서에 기록한다.

| 확인 항목 | 기록 내용 |
| --- | --- |
| Method / Endpoint | 실제 API 명세의 메서드·경로 |
| Request | path/query/body, 필수·선택, 형식·제약 |
| Response | 필드명·타입·nullable·목록/집계 형식 |
| Status Code | 정상·오류별 상태 코드 |
| Error Response | 오류 코드·메시지·응답 구조 |
| 인증 여부 | 회원/게스트 접근과 실제 인증 전달 방식 |
| 권한 조건 | 해당 지역 이웃 인증, 기관 인증 유효 상태·담당 지역 등 관련 명세 조건 |

연동 순서는 **계약 합의 → FE/BE 각 Part 구현 → 각자 검증 → 연동 확인 → develop 통합**이다. 다른 Part의 API가 필요하면 상대 담당자와 선행 통합 순서를 맞춘다.

- FE mock은 합의한 Request/Response를 따른다. mock 확인만으로 실제 FE/BE 연동 완료를 표시하지 않는다.
- API를 바꿀 때 협업 문서·API 명세를 즉시 갱신하고 반대 파트 담당자에게 알린다. 개발 도구로 자동 메시지를 보내지 않고 담당자가 팀 채팅에 공지한다.
- 호환되지 않는 변경은 적용 시점과 양쪽 수정 순서를 맞춘다. 공통 계약을 각 Part에서 서로 다르게 재정의하지 않는다.
- API 변경의 관련 commit·연동 결과를 작업 카드에 연결한다.

```text
[API 계약 변경]
Part: C
관련 기능: F-GOMLGG / S-HNVDPO
API: 실제 Method / Endpoint
변경 전 → 변경 후:
영향받는 FE/BE 작업:
반영 commit / 적용 순서:
상대 담당자 확인:
```

## 17. main 반영

main은 개발 중 자주 갱신하지 않는다. 지정한 main 반영 담당자가 **develop 전체 안정화와 최종 검증 후**에만 반영한다.

### 반영 전 확인

- develop의 FE/BE 전체 test/build 통과
- 실제 FE/BE 연동 확인
- 가입·로그인·원 상세 복귀, 게시물 작성/열람, 참여·투표, 개인 기록, 인증 상태 기반 권한·기관 채택 등 MVP 핵심 사용자 플로우 확인
- 해커톤 시연 시나리오와 사용하는 데이터·계정 확인
- 미완료·실험 코드 및 secret 없음
- 검증한 develop SHA와 담당자·결과 기록

검증을 시작하면 팀에 최종 반영 중임을 공지하고 develop 추가 반영도 잠시 보류한다. 이후 develop이 바뀌면 새 SHA를 기준으로 최종 검증을 다시 한다.

```text
develop → 전체 검증 → main merge → main test/build → origin/main push
```

```bash
git status
git fetch origin
git switch develop
git pull --ff-only origin develop
git log -1 --oneline

# 위 develop SHA로 전체 test/build·연동·시연 검증

git switch main
git pull --ff-only origin main
git merge --no-ff --no-commit develop
git diff --cached

# main에서 전체 test/build·필요한 핵심 플로우 재확인
git diff --check
git diff --cached --check

# 모두 통과한 경우
git commit -m "chore(common): 최종 검증된 develop을 main에 반영"
git status
git fetch origin
git log --oneline main..origin/main
```

원격 main에 새 commit이 없을 때만 다음을 수행한다.

```bash
git push origin main
git fetch origin
git rev-parse main
git rev-parse origin/main
```

push 성공과 동일 SHA를 확인한 뒤 팀에 main 반영 완료를 알린다. 이미 반영되어 `Already up to date`면 불필요한 merge commit을 만들지 않는다.

충돌·검증 실패 시 push하지 않는다. 아직 commit 전인 진행 중 merge는 12장 기준으로 취소한다. 수정은 소유 Part에서 수행하고 develop 통합·최종 검증을 거쳐 다시 main에 반영한다. main push 거절이나 이력 분기가 발생해도 force push·임의 reset하지 않고 반영 담당자와 확인한 뒤 최신 원격 기준으로 재검증한다.

## 18. 금지 사항

- main에서 직접 기능 개발 금지
- develop에서 직접 기능 개발 또는 통합 실패를 숨기는 기능 수정 금지
- 기능별 feature 브랜치 추가 생성 금지
- Part 브랜치끼리 직접 merge 또는 cherry-pick으로 통합 허브 우회 금지
- 다른 사람의 Part 브랜치에서 임의 개발 금지
- 미완료·검증하지 않은 Part의 develop 반영 금지
- develop 동시 push 및 시작 공지 없이 통합 금지
- test/build가 실패한 상태로 develop/main push 금지
- 다른 팀원의 변경 임의 삭제 또는 자동 전체 ours/theirs 선택 금지
- Codex 변경사항을 사람이 검토하지 않은 상태로 commit 금지
- 관련 없는 대규모 리팩토링·전역 포맷팅 금지
- API 변경 무통보 금지
- secret / API key / password / 실제 인증 증빙 commit 금지
- force push는 원칙적으로 금지. 이 가이드의 정상 작업·통합 절차에서는 사용하지 않는다.
- 공유한 commit 이력을 다시 쓰는 rebase/amend와 임의 `reset --hard`, 정리를 위한 강제 파일 삭제 금지

민감 정보가 이미 원격에 올라갔다면 즉시 팀에 알리고 해당 키·자격 정보를 폐기/교체한다. 단순 삭제 commit만으로 과거 이력의 노출이 제거됐다고 판단하지 않는다. 공유 이력 정리는 저장소 관리 담당자와 별도로 처리한다.

## 19. 팀원이 작업 시작 전 확인할 체크리스트

- [ ] 팀 저장소·origin 주소·자신의 Part 브랜치를 확인했다.
- [ ] 협업 문서에 내 담당자·세부 작업·상태를 기록했다.
- [ ] 기준 문서 v10.1의 기능 ID와 해당 MVP 범위를 읽었다.
- [ ] 수정 허용 파일과 다른 Part의 소유 범위를 확인했다.
- [ ] 필요한 API 계약과 FE/BE 상대 담당자를 확인했다.
- [ ] 선행 API·공통 코드가 develop에 반영됐거나 대기 이유를 기록했다.
- [ ] 현재 작업 폴더가 clean이고 진행 중 충돌/merge가 없다.
- [ ] origin/develop을 로컬 develop에 `pull --ff-only`했다.
- [ ] 최신 로컬 develop을 내 Part에 merge했다.
- [ ] 충돌 해결·필요한 test/build를 마쳤다.
- [ ] 실제 테스트·빌드·연동 명령과 실행 위치가 협업 문서에 등록돼 있다.
- [ ] Codex에는 이번 세부 작업 하나와 범위·완료 조건만 요청한다.

## 20. develop 반영 직전 체크리스트

- [ ] Part의 통합 대상 작업이 모두 완료됐고 미완료·실험 코드가 섞이지 않았다.
- [ ] 사람이 `git status`, `git diff`, `git diff --cached`와 새 파일을 검토했다.
- [ ] 관련 명세/API·MVP 범위와 구현이 일치한다.
- [ ] 최신 develop을 Part에 merge하고 충돌을 해결했다.
- [ ] 최신 코드 기준 Part test/build가 실제 실행돼 통과했다.
- [ ] 검증한 Part commit과 원격 Part push를 확인했다.
- [ ] 기능명세 ID·API·검토자·검증 결과·SHA를 협업 문서에 기록했다.
- [ ] 다른 develop 반영자가 없고 시작 공지로 순서를 확보했다.
- [ ] 통합 직전 origin/develop을 다시 확인했다. 추가 변경이 있으면 Part 검증부터 반복했다.
- [ ] Part → develop merge 결과 전체를 사람이 검토했다.
- [ ] develop 전체 test/build와 필요한 FE/BE 연동을 통과했다.
- [ ] secret·불필요한 파일·전역 포맷 변경·충돌 표시가 없다.
- [ ] 정상 통합 commit을 만들었고 원격에 추가 변경이 없는지 재확인했다.
- [ ] origin/develop push 성공과 반영 SHA를 확인했다.
- [ ] 완료 공지와 협업 문서 갱신을 마쳤고 다른 팀원에게 Part 최신화를 안내했다.

**기억할 흐름: origin/develop pull → develop을 Part에 merge → Part 검증 → Part를 develop에 merge → develop 검증 → origin/develop push.**
