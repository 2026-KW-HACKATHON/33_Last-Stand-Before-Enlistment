# Discushion 프론트엔드 Git·GitHub 협업 전략 v2

> 작성일: 2026-10-06 (Asia/Seoul)  
> 적용 기준: 새 통합 Git·GitHub 협업전략의 Issue·영역별 develop·Feature PR 규칙  
> 대상: 프론트엔드 개발자와 리뷰 담당자  
> 운영 흐름: **Issue → 최신 front/develop → front/feature/<issue번호>-<기능명> → 구현·검증 → PR → 리뷰·CI → front/develop**

## 1. 목적과 기준 문서

프론트엔드 팀이 작업을 나누고, 서로의 변경을 검토하며, 백엔드 API와 연결하는 절차를 정한다. 이 문서는 프론트엔드 협업용 별도 문서이며 원본 제품 정책을 변경하지 않는다.

| 판단 대상 | 기준 문서 |
| --- | --- |
| 브랜치·Issue·commit·PR·병합 | `Discushion_Git_GitHub_통합협업전략_2026-10-06.md` |
| 제품 동작·MVP 범위·권한 | `Discushion_PRD_2026-10-06_MVP반영_v10.md`, `Discushion_기능명세서_2026-10-06_MVP반영_v10.md` — 내부 버전 v10.1 |
| FE/BE 책임·상태·연동 인수 | `Discushion_MVP_백엔드_프론트엔드_통합_지침서 (1).md` |
| API 계약 초안 | `Discushion_API_SPEC_v2.md` — 본문 제목/내부 버전 v1.1 |

제품 해석은 두 정본의 확정 정책 보완·최신 MVP 범위 → 기능 ID별 본문 → PRD 사용자 흐름 순으로 적용한다. API 명세의 경로·DTO·Enum·인증 전달 방식은 설계 제안이므로 FE/BE 합의 후 고정한다.

다른 자료에 단일 `develop`, Part 장기 브랜치 또는 별도 작업 카드만으로 개발하는 설명이 남아 있더라도 Git 운영은 새 통합 협업전략을 따른다. 협업 문서와 작업 카드는 Issue·PR을 보충하며 대체하지 않는다.

**확정 규칙**은 새 통합 문서에서 가져왔다. 아래 FE 2인 담당 분배·공통 파일 Owner·Issue 분할 예시는 **권장안**이다. 제공 소스에는 FE 인원별 확정 배정과 실제 코드 구조가 없으므로 실명·폴더·프레임워크·검증 명령을 임의로 확정하지 않는다.

## 2. 반드시 지킬 Git 운영 규칙

1. 일반 기능 개발 전에 GitHub Issue를 생성하고 범위·담당자·완료 조건을 적는다.
2. 기본 작업 단위는 **1 Issue → 1 Feature 브랜치 → 1 PR**이다.
3. 프론트엔드 Feature는 항상 최신 `front/develop`에서 만든다.
4. 이름은 `front/feature/<issue번호>-<기능명>`으로 통일한다.
5. 일반 FE Feature PR의 base는 반드시 `front/develop`이다.
6. `front/develop`, `back/develop`, `main`에는 직접 push하거나 로컬 merge 결과를 직접 반영하지 않는다. 통합은 GitHub PR로 진행한다.
7. 단일 `develop`, `release`, `frontend-part-*`, `feature/fe-*` 브랜치는 사용하지 않는다.
8. 다른 FE/BE Feature를 직접 merge하지 않는다. 필요한 FE 공통 변경은 먼저 PR로 `front/develop`에 통합한 뒤 받는다.
9. 리뷰와 필수 CI를 통과한 PR만 병합한다. 작성자가 자기 PR을 단독 승인·병합하지 않는다.
10. 공유한 commit의 amend/rebase, force push, 공유 이력을 덮는 reset을 하지 않는다. 수정은 후속 commit으로 남긴다.

| 브랜치 | 역할 | FE 작업자의 사용 방식 |
| --- | --- | --- |
| `front/feature/*` | Issue 한 개 구현 | 코드 수정·commit·push 가능 |
| `front/develop` | 검증된 FE 기능 통합 | 최신 기준 읽기, Feature PR의 base |
| `back/develop` | 검증된 BE 기능 통합 | FE 코드 통합 대상으로 사용하지 않음 |
| `main` | 최종 FE/BE 통합 결과 | 최종 검증 후 develop별 PR로 반영 |

저장소 관리자가 `main`에서 두 develop을 최초 생성하고 보호 규칙을 설정한다. 일반 FE 개발자가 초기 설정을 이유로 보호 브랜치에 직접 push하지 않는다.

## 3. 프론트엔드 2인 담당 분배 — 권장안

Part A~D는 FE/BE에서 같은 제품 영역을 뜻한다. **담당 구분이며 장기 Git 브랜치가 아니다.** 두 명이면 FE1이 A·D, FE2가 B·C를 맡는 구성을 권장한다. 실명과 최종 배정은 팀 합의로 기록한다.

| 담당자 | Part | 주요 구현 책임 | 기능 API 협의 대상 |
| --- | --- | --- | --- |
| FE1 | A — Account & Authority | 시작·로그인·이메일 인증 가입·프로필·활동 지역·이웃 인증·기관 인증·권한/배지 기반 | BE1 |
| FE1 | D — Personal & Institution | 북마크·마이페이지·나의 활동·개인 목록·참여 투표·기관 안건 목록·채택/취소 | BE1 |
| FE2 | B — Content & Post | 메인·통합 게시판·공통 상세·세 유형 게시물 관리·사진·지도·AI 안건 요약 | BE2 |
| FE2 | C — Participation | 반응·댓글/1단계 답글·평가/정렬·실제 투표·공유 링크·게스트 접근 | BE2 |

새 통합 문서에서 **BE1=A·D + API 정본·ERD·DB Schema/Migration**, **BE2=B·C + 실행환경·배포·Docker·환경변수·CORS** 책임은 확정되어 있다. API 계약 변경은 기능 담당 BE와 조율하더라도 BE1의 정합성 확인·정본 갱신을 거친다.

### Part별 기능명세 연결

| Part | 연결 기능명세 ID |
| --- | --- |
| A | `F-WLXSSC`, `F-TSOXGG`, `F-KZRSXU`, `F-RBVFZX`, `F-QQKYLC`, `F-ATWJDJ`, `F-OPNIXL`, `S-YLSPHQ`, `S-JRMYIV`, `F-GDASNA`, `F-MUBDJD` |
| B | `F-UPRLMN`, `F-EAJPVC`, `F-PUDHYO`, `F-UCDVNA`, `F-FTLHCX`, `S-NVXXYQ`, `S-TBFIHO`, `F-GSMCLD`, `F-WSCKDN`, `F-QIGKAK` |
| C | `F-GOMLGG`, `S-HNVDPO`, `F-EDNVWZ`, `S-JCEZAP`, `S-YYDGUS`, `F-CDIBRF`, `S-OXTKEP`, `F-FCPVIS`, `S-CMGJIG`, `F-OWFYWE`, `S-NYUECP` |
| D | `F-FYQJPT`, `F-WYMXXP`, `F-SSHXAA`, `F-NZTUYE`, `F-QPGNCF`, `F-CNNPYL`, `S-PCCNUU`, `F-TUGMEP`, `S-AQOBIE` |

하나의 기능 ID가 여러 세부 Issue에 연결될 수 있다. Part 전체를 Issue 하나로 묶지 않고 독립적으로 검증할 수 있는 동작 단위로 나눈다.

## 4. 공통 코드와 화면 조립 책임 — 권장안

한 화면에 여러 Part가 들어가므로 화면 전체를 두 사람이 각자 수정하지 않도록 조립 책임을 정한다.

| 공통 변경/접점 | 제안 Owner | 협업 방식 |
| --- | --- | --- |
| 라우트 등록·공통 앱 구조·로그인 복귀·권한 가드 | FE1 | 변경할 경로와 입력/복귀 계약을 Issue에 기록, FE2 리뷰 |
| 디자인 토큰·기본 UI·Header·BottomNavigation | FE1 | 공통 기반 Issue로 먼저 통합, 변경 영향 화면은 양쪽 확인 |
| 공통 API 클라이언트·응답/오류 처리 | FE1 | FE2와 인터페이스 합의, API 변경은 BE1 협의 |
| 게시물 카드·공통 상세의 페이지 조립 | FE2 | C 참여와 D 북마크/채택을 같은 게시물 식별자로 연결 |
| 댓글·반응·투표 참여 컴포넌트 | FE2 | 공통 상세에 연결할 입력·콜백·갱신 계약 명시 |
| 북마크·기관 채택·개인 목록 컴포넌트 | FE1 | 상세 삽입용 인터페이스를 FE2와 먼저 합의 |
| 의존성·lockfile·빌드/CI 설정 | FE 두 명 사전 조율 | 한 Issue 담당자가 변경, 실행/배포 영향은 BE2 협의 |

Owner는 모든 코드를 혼자 작성하는 사람이 아니라 최종 인터페이스와 통합 영향을 조율하는 사람이다. 실제 파일 경로는 저장소 확인 후 Issue에 적는다.

예를 들어 공통 상세에 북마크를 붙일 때 FE1은 북마크 동작을 구현하고, FE2는 상세 조립을 담당한다. 같은 파일 수정이 필요하면 선행 PR과 후속 PR 순서를 정한다. 공통 변경이 `front/develop`에 들어오기 전에 상대 Feature를 직접 merge해 받지 않는다.

투표 **작성·원본·수정 제약은 B**, **제출·선택 변경·집계는 C**, **참여한 투표 조회는 D**다. 기관 **인증 상태·배지는 A**, **안건 목록·채택/취소는 D**다. 화면이 겹쳐도 이 책임을 유지한다.

## 5. 개발 시작 전 합의할 항목

| 합의 항목 | 기록할 내용 | 주 담당 |
| --- | --- | --- |
| 담당 배정 | FE1/FE2 실명·Part·리뷰 상대 | FE 두 명 |
| 실제 저장소 구조 | FE 코드 위치·공통 파일·실행 위치 | FE 두 명 |
| 개발 환경 | 프레임워크·런타임·패키지 매니저·lockfile·설치 방법 | FE 두 명 |
| 디자인 기준 | 적용할 디자인 버전·토큰·공통 컴포넌트·레이아웃 | FE 두 명 |
| 라우트·상태 | 경로·상세 복귀·필터 보존·회원/게스트 내비게이션 | FE1 중심 |
| API 계약 | 실제 URL·인증 전달·ID·DTO·nullable·오류·mock | 기능 FE/BE + BE1 |
| 실행 연동 | API 서버 접근·환경변수·CORS·시연 계정 | BE2와 협의 |
| 자동 검증 | 실제 lint/typecheck/test/build 명령·필수 CI | FE 두 명 |
| PR 운영 | 리뷰 담당·병합 방식·보호 규칙·main 반영 순서 | 저장소 관리자/팀 |

합의 내용은 README 또는 협업 문서에 남기고, 영향을 받는 Issue에서 연결한다. 미정 항목 때문에 독립 UI 작업까지 멈추지 않되, 미확정 정책을 임의로 확정하거나 실 API 연동 완료로 표시하지 않는다.

## 6. Issue 작성과 분할

### GitHub에서 등록하는 순서

1. 저장소의 **Issues**에서 새 Issue를 만든다.
2. 제목을 `[FE][Part C] 댓글·답글 작성 UI 구현`처럼 적는다.
3. 아래 양식에 작업 범위·기능 ID·완료 조건·API·검증·의존 작업을 적는다.
4. Assignee에 실제 작업자 한 명을 지정한다. label은 저장소에 있는 것을 쓰거나 팀 합의로 만든다.
5. 생성된 Issue 번호로 Feature 이름을 만든다. 번호는 임의로 정하지 않는다.
6. 선행 Issue가 있으면 연결하고, 공통 파일이 겹치면 구현/병합 순서를 기록한다.

`frontend`, `part-a`~`part-d`, `blocked` 등의 label과 Project 보드는 선택 사항이다. 필수인 것은 Issue의 범위·담당·완료 조건과 대응 Feature/PR이다.

### Issue 양식

```markdown
## 목표
사용자가 수행할 수 있게 되는 동작 한 가지 또는 독립 검증 가능한 작업 묶음

## 담당/영역
- 담당자: FE1 또는 FE2 / 실제 계정
- Part:
- 리뷰 담당:

## 근거
- 기능명세 ID:
- PRD/통합 지침 위치:
- 디자인 화면/노드: 해당하는 경우

## 구현 범위
- 포함:
- 제외:
- 수정 예상 파일: 실제 저장소에서 확인한 경로
- 공통 코드/API 영향:

## API 계약/연동
- 연결 BE 담당자 / BE Issue:
- Method / Endpoint:
- Request / Response / nullable:
- 상태 코드 / 오류 / 인증 / 권한:
- 계약 상태: 미합의 / 합의 / 변경 협의 중
- mock과 실제 API의 차이:

## 의존 작업
- 선행 Issue/PR:
- 기다려야 하는 인터페이스/병합:

## 완료 조건
- [ ] 정상 동작 및 명세상 제한 구현
- [ ] 관련 로딩·빈 상태·권한·오류·복귀 처리
- [ ] 관련 검증 및 build 결과 기록
- [ ] mock 여부/실 API 연동 여부 구분
- [ ] PR 리뷰·필수 CI 통과 및 front/develop 통합

## 검증 방법
재현 순서, 계정/지역/상태, 실제 실행할 명령
```

### 분할 예시

| Issue 작업 단위 | Part | 연결 기능 ID | 선행 작업 |
| --- | --- | --- | --- |
| 로그인과 원 상세 복귀 | A | F-TSOXGG, S-NYUECP | 인증/복귀 계약 |
| 목록의 지역·유형·주제 필터와 상세 진입 | B | F-EAJPVC, F-PUDHYO | Region·게시물 조회 계약 |
| 댓글·1단계 답글 작성/표시 | C | F-EDNVWZ, S-JCEZAP, S-YYDGUS | 상세·권한·댓글 계약 |
| 댓글 평가와 부모 댓글 정렬 | C | F-CDIBRF, S-OXTKEP | 댓글 조회 |
| 투표 제출·확인 변경·종료 제한 | C | F-FCPVIS, S-CMGJIG | 투표 원본·조회 계약 |
| 북마크 등록/해제·복귀·저장 안내 | D | F-FYQJPT | 상세·로그인 복귀 |
| 기관 안건 채택/취소 | D | F-TUGMEP, S-AQOBIE | 유효 기관 상태·담당 지역·안건 조회 |

한 Issue가 너무 커지면 구현 전에 독립 완료 조건으로 나눈다. 작업 도중 추가 기능이 생기면 현재 PR에 섞지 않고 후속 Issue로 등록한다.

## 7. 작업 시작·commit·push

아래 `#31`과 브랜치 이름은 설명용 예시다. 실제 Issue 번호와 기능명으로 바꿔 사용한다.

### 새 Feature 만들기

```bash
git status
git fetch origin
git switch front/develop
git pull --ff-only origin front/develop
git switch -c front/feature/31-comment
git push -u origin front/feature/31-comment
```

- 첫 `git status`에서 미커밋 변경이 있으면 보존·정리한 뒤 이동한다. 변경을 임의로 삭제하거나 덮어쓰지 않는다.
- `front/develop` 로컬 브랜치가 없고 원격에는 있다면 `git switch --track origin/front/develop`으로 추적 브랜치를 만든다.
- `pull --ff-only`가 실패하면 원인을 확인한다. 강제 reset이나 임의 merge로 정리하지 않는다.
- 보호 브랜치의 최신 상태를 로컬로 받는 것은 가능하다. 보호 브랜치에 구현 commit을 추가하거나 직접 push하는 것은 금지다.

### 구현 후 commit·push

```bash
git status
git diff
git add <검토한-변경파일-경로>
git diff --cached
git commit -m "feat(fe): 댓글 및 답글 작성 UI 구현"
git push origin front/feature/31-comment
```

`<검토한-변경파일-경로>`는 실제 파일 경로로 바꾼다. 검증 명령은 저장소에서 확인한 것을 실행하고 PR에 결과를 기록한다. 무관한 파일이나 환경 비밀값이 stage에 들어가지 않도록 확인한다.

### commit 규칙

형식은 **`<type>(<scope>): <작업 내용>`**이다. type은 `feat`, `fix`, `refactor`, `test`, `docs`, `chore`; scope는 `fe`, `be`, `common`이다.

```text
feat(fe): 지역 유형 주제 필터 UI 구현
fix(fe): 로그인 후 원 게시물 복귀 수정
refactor(fe): 상세의 투표 표시 컴포넌트 정리
test(fe): 투표 변경 취소 시 기존 선택 유지 검증
docs(common): 프론트엔드 API 계약 변경 기록
chore(fe): 프론트엔드 빌드 설정 추가
```

`update`, `수정`, `작업함`처럼 변경 내용을 알 수 없는 메시지는 사용하지 않는다. 원격 공유 commit의 수정은 새 commit으로 남긴다.

## 8. 최신 변경 반영과 충돌 해결

자기 Feature 작업 중 `front/develop`이 갱신되면 아래처럼 받는다.

```bash
git status
git fetch origin
git switch front/feature/31-comment
git merge origin/front/develop
```

이 merge는 **Feature 안에서 통합 변경을 받는 작업**이다. develop에서 Feature를 로컬 merge하고 직접 push하는 절차가 아니다.

충돌이 나면 다음 순서를 따른다.

1. 충돌 파일과 양쪽 변경 목적을 확인한다.
2. Issue 완료 조건·기능명세·API 계약을 기준으로 상대 담당자와 결정한다.
3. 한쪽 전체를 `ours`/`theirs`로 자동 선택하지 않는다.
4. Feature에서 파일을 수정하고 충돌 해결 commit을 남긴다.
5. 관련 검증/build를 다시 실행하고 자기 Feature를 push한다.
6. 기존 PR에 해결 내용과 검증 결과를 갱신한다.

같은 라우트·공통 상세·공통 타입·lockfile을 두 PR이 수정하면 선행 PR을 먼저 병합하고 후속 Feature가 최신 `front/develop`을 받는다. 별도 잠금이나 보호 브랜치 직접 push로 통합하지 않는다.

## 9. PR 생성·리뷰·병합

### PR 생성

1. Issue 범위·변경 diff·검증 결과를 확인한다.
2. Feature를 원격에 push한다.
3. **base: `front/develop`**, **compare: 자기 `front/feature/...`**로 PR을 만든다.
4. 제목은 `[FE][Part C] 댓글·답글 작성 UI 구현`처럼 쓴다.
5. 연결 Issue·기능 ID·API 영향·검증 결과·화면 자료를 기록한다.
6. 상대 FE 개발자를 reviewer로 지정한다. 공통/API 영향은 관련 담당자 확인도 받는다.

### PR 양식

```markdown
## 연결 작업
- Issue: #실제번호
- Part / 기능명세 ID:
- Base: front/develop

## 변경 결과
사용자가 할 수 있게 된 동작과 핵심 변경

## 화면/상태
- 정상:
- 로딩·빈 상태:
- 권한·오류:
- 복귀·입력 보존:
- 화면 캡처: UI 변경 시

## 영향
- 공통 컴포넌트/라우트/상태:
- API/DB 계약 영향: 없음 또는 구체적 내용
- 관련 FE/BE 담당자 확인:
- 선행/후속 Issue:

## 검증
- 실행 위치와 실제 명령:
- 결과:
- 수동 확인 시나리오:
- mock 검증:
- 실제 API 연동:
- 남은 제한:
```

### 리뷰와 병합 기준

| 리뷰 대상 | 확인 내용 |
| --- | --- |
| 범위 | Issue 완료 조건·기능 ID·MVP 범위를 지켰는가 |
| 화면 | 합의한 디자인·공통 컴포넌트를 사용했는가 |
| 동작 | 권한·실패·로딩·빈 상태·복귀가 있는가 |
| 데이터 | 상세·목록·마이에서 같은 원본과 실제 본인 선택을 쓰는가 |
| 공통 영향 | 다른 Part의 라우트/타입/기능을 깨뜨리지 않는가 |
| 검증 | 담당자 검증 결과·필수 CI·리뷰가 통과했는가 |

리뷰와 CI가 통과하면 GitHub PR에서 병합한다. 작성자 단독 승인·병합은 금지다. 충돌이나 검증 실패가 있으면 Feature에서 해결한다.

병합 후에는 `front/develop` 통합 상태를 확인하고 Issue를 닫은 뒤 Feature를 삭제한다. **`front/develop` 대상 PR의 Issue 자동 종료를 전제로 하지 않는다.** Issue 링크와 병합 결과를 확인해 수동 종료가 필요한 경우 직접 닫는다.

## 10. FE/BE API 협업

FE/BE 코드는 각자의 develop으로 통합한다. **백엔드 코드를 FE Feature로 merge하지 않고, 합의한 API 계약과 실행 환경으로 연결한다.**

| 계약 항목 | 사전에 확인할 내용 |
| --- | --- |
| 요청 | Method·Endpoint·필수/선택 필드·인증/공유 컨텍스트 |
| 응답 | DTO·ID 타입·nullable·본인 상태·권한·집계 |
| 실패 | HTTP 상태·오류 구조·권한 사유·재시도 가능 여부 |
| 목록 | 지역/유형/주제·정렬·페이지/커서 |
| 첨부 | multipart·개수/용량·업로드와 최종 제출 구분 |
| 갱신 | 반응·투표·북마크·채택 변경 후 영향을 받는 조회 |
| 환경 | API 주소·환경변수·CORS·시연 계정·접근 방법 |

API가 준비되지 않았으면 합의한 계약으로 mock 구현을 진행할 수 있다. 계약 초안에서 확정되지 않은 부분은 미합의로 기록한다. mock 성공은 실제 연동 완료가 아니다.

API 변경 순서는 **변경 제안 → 기능 BE/FE 협의 → BE1의 정본 정합성 확인·갱신 → 영향 Issue/PR 기록 → 담당자 통보 → 실제 연동 검증**이다. 실행환경·CORS·환경변수·배포는 BE2와 협의한다. DB 모델 변경 요구는 FE가 독자적으로 반영하지 않고 BE1과 협의한다.

변경 기록에는 변경 전/후, 영향 Part, 적용 순서, Issue/PR/commit, 상대 확인 여부와 연동 결과를 적는다. 문서만 갱신하고 상대에게 통보한 것으로 표시하지 않는다.

## 11. 구현·병합 순서 — 권장안

아래는 의존성 기준이다. 각 단계는 여러 작은 Issue/Feature/PR로 진행한다.

| 단계 | FE1 | FE2 | 다음 작업이 받을 결과 |
| --- | --- | --- | --- |
| 1. 공통 기반 | 라우트·세션·오류·권한/복귀 계약, 기본 UI | 공통 카드·상세 조립 인터페이스 합의 | 검증된 공통 기반을 front/develop에 통합 |
| 2. 계정과 읽기 | 가입·로그인·프로필·지역·인증 상태 | 메인·목록·공통 상세 조회 | 동일 Region/게시물/세션 계약 |
| 3. 쓰기와 참여 | 인증 완료 지역/기관 상태 연결, 북마크 | 세 유형 작성·사진, 댓글/반응/평가/투표 | 동작 후 상세·목록 상태 갱신 |
| 4. 개인/기관과 탐색 확장 | 마이·개인 기록·기관 안건·채택 | 공유 게스트·지도·AI 요약 | 삭제/복귀/권한까지 연결된 흐름 |
| 5. 실제 연동과 인수 | A·D API와 교차 흐름 검증 | B·C API와 교차 흐름 검증 | 각 영역 통합 및 main 반영 준비 |

독립 UI는 계약 합의 후 병렬 진행할 수 있다. 이 순서를 Part 전체가 끝나야 다른 개발자가 시작할 수 있는 잠금으로 사용하지 않는다. 의존 공통 코드가 필요하면 해당 PR이 `front/develop`에 들어간 뒤 가져온다.

## 12. 프론트엔드 검증과 MVP 경계

실제 저장소가 제공되지 않았으므로 `npm run ...` 등의 명령이나 실행 성공을 선언하지 않는다. 개발 시작 시 실제 scripts/README/CI에서 명령을 확인해 기록한다.

PR 전에는 변경 관련 검증과 build를 실행한다. develop 병합 뒤 영역 통합과 API 연동을 확인하고, main 반영 전 전체 흐름·FE/BE build·배포 환경을 검증한다.

| 핵심 흐름 | FE 인수 기준 |
| --- | --- |
| 가입·로그인·복귀 | 이메일 인증 가입, 일반 로그인, returnTo 보존, 복귀 후 원 행동 자동 실행 금지 |
| 지역 권한 | 임시 탐색 지역과 기본 활동 지역 분리, 이웃 완료 지역에서만 주민 참여, 접수만으로 권한 부여 금지 |
| 공유 게스트 | 받은 특정 공개 상세·댓글/답글만, 일반 내비게이션 없음, 회원 기능은 로그인 안내 |
| 세 유형 게시물 | 같은 원본, 게시 후 상세 이동, 활동 필수 입력, 투표 질문/선택지/종료와 수정 제한 |
| 사진 | JPG/PNG 최대 10장·게시물 전체 10MB, 선택 취소/왕복/실패 입력 유지, 사진 없으면 이미지 영역 생략 |
| 반응·댓글 평가 | 세 반응은 독립 복수 선택, 좋아요/싫어요는 상호배타 전환/취소 |
| 댓글·답글 | 1단계, 기존 답글에 답하면 원 부모/대상명 유지, 부모 기준 좋아요순/최신순, 실패 시 입력 유지 |
| 투표 | 선택만으로 저장 안 됨, 제출·변경 확인, 취소 시 기존 표, 종료 제한, 실제 본인 선택 표시 |
| 북마크·개인 기록 | 상세 한 버튼, 게스트 자동 저장 금지, 북마크만 참여 목록에 포함 금지, 삭제 투표 기록의 콘텐츠 접근 제한 |
| 기관 | 유효 완료 상태 기반 역할/배지, 전체 공개 안건·주민 의견 열람, 담당 지역 공개 안건만 기관별 채택/취소 |
| 지도·AI | 지도 대표는 안건/투표 반응 기준·GPS 불필요, 요약은 원문 기반 3문장·상태/실패 시 원문 |
| 화면 공통 | 명세에 맞는 로딩/빈 상태/오류/재시도, 원 목록 필터·정렬·진입 맥락 보존 |

서버는 요청 시점 권한과 상태를 재검증한다. FE 버튼 비활성이나 낙관 UI만으로 권한/저장 성공을 확정하지 않는다. 실패 시 임시 상태를 되돌리거나 재조회한다.

이번 필수 구현에서 **추천·관심 정보·알림/예약·신고·계정 복구/탈퇴·임시저장·익명 작성·AI 이미지·운영 심사·행정 연동**을 제외한다. 후순위 기능을 연결되지 않는 필수 메뉴/버튼으로 추가하지 않는다. 설정·로그아웃 UI도 API 명세상 이번 필수 범위에서 제외되어 있다.

금칙어 실제 목록, 이웃 증빙/프로필 사진 제한, 미명시 입력 길이, 페이지 기본값, 공유 토큰 방식, AI 재시도 등은 확인 필요로 기록한다. 기관 증빙 제한을 이웃 증빙에 그대로 적용하거나 제품 미정값을 임의로 고정하지 않는다.

## 13. Codex 요청 규칙과 복사 양식

Codex에는 한 번에 **Issue 한 개**만 요청한다. Part 전체·담당자 전체 기능을 일괄 구현하게 하지 않는다.

```text
Discushion 프론트엔드 Issue 한 개를 구현해줘.

담당자: FE2
현재 Issue: #31 [FE][Part C] 댓글·답글 작성 UI 구현
현재 브랜치: front/feature/31-comment
Base Branch: front/develop

근거:
- 관련 기능명세 ID: F-EDNVWZ, S-JCEZAP, S-YYDGUS
- API 계약: Issue에 연결된 합의본
- 디자인 화면: Issue에 연결된 대상

수정 허용:
- Issue 완료 조건에 필요한 프론트엔드 코드와 관련 검증
- 합의된 공통 인터페이스 범위

수정 금지:
- 다른 Issue/Part 기능의 임의 구현
- 백엔드 코드와 API 계약의 독자 변경
- 무관한 리팩터링/전역 포맷팅
- 임의 브랜치 이동/다른 Feature merge
- develop/main에 push·merge
- PR 병합

완료 조건:
- Issue 인수 기준 구현
- 실제 저장소의 관련 검증과 build 실행
- 변경 파일, 명령/결과, mock 여부, 실 API 연동 여부,
  미해결 사항을 보고

먼저 현재 브랜치와 git status를 확인해줘.
브랜치가 다르거나 보존해야 할 미커밋 변경이 있으면
임의로 바꾸거나 덮어쓰지 말고 상태를 알려줘.
```

번호/담당자/Part/기능 ID/브랜치는 실제 Issue에 맞게 바꾼다. 작업 후 사람이 `git status`, `git diff`, `git diff --cached`를 검토한다. Codex가 만든 코드도 같은 PR·리뷰·CI 절차를 따른다.

## 14. 최종 main 반영

최종 시연·배포 단계에서 `front/develop`과 `back/develop`을 각각 `main`으로 PR한다. `release`는 만들지 않는다.

1. 각 develop의 전체 검증과 FE/BE 연동을 완료한다.
2. 팀이 먼저 반영할 영역을 정하고 해당 develop → main PR을 리뷰·CI 후 병합한다.
3. 두 번째 영역 PR은 **첫 번째 병합 이후 최신 main** 기준으로 차이·충돌·누락을 다시 확인한다.
4. 최신 main 변경이 두 번째 develop에 필요하면 **main → 해당 develop 동기화 PR**을 열어 검토·검증한다. 직접 merge/push하지 않는다.
5. 두 번째 develop → main PR을 갱신/생성하고 양 영역 변경이 포함되는지 확인해 병합한다.
6. 최종 main에서 FE/BE build·핵심 사용자 흐름·API 연동·배포 환경을 다시 확인한다.

BE가 먼저 반영되면 필요에 따라 main → front/develop 동기화 PR을 사용한다. FE가 먼저면 필요에 따라 main → back/develop 동기화 PR을 사용한다. 최종 PR이나 동기화 PR의 병합도 담당자가 수행하며 Codex는 병합하지 않는다.

## 15. 매 작업 체크리스트

### 시작 전

- [ ] Issue에 담당자·범위·기능 ID·API·완료 조건이 있다.
- [ ] 의존 PR과 공통 파일 변경 순서를 확인했다.
- [ ] 미커밋 변경을 확인하고 보존했다.
- [ ] 최신 front/develop에서 front/feature/<issue번호>-<기능명>을 만들었다.
- [ ] 실제 개발/검증 명령과 계약 미정 항목을 확인했다.

### PR 전·병합 전

- [ ] Issue 범위와 diff를 검토하고 비밀값·무관한 변경을 제외했다.
- [ ] 관련 검증/build 결과와 mock/실 API 연동 상태를 기록했다.
- [ ] PR base가 front/develop이며 Issue가 연결되어 있다.
- [ ] API/공통 변경과 상대 담당자 확인을 기록했다.
- [ ] 필수 CI·리뷰가 통과했고 충돌이 없다.
- [ ] GitHub PR로 병합하며 작성자 단독 승인·병합이 아니다.

### 병합 후

- [ ] front/develop 통합 상태와 관련 연동을 확인했다.
- [ ] Issue 종료 여부를 확인하고 Feature를 삭제했다.
- [ ] 후속 개발자는 최신 front/develop에서 변경을 받는다.
