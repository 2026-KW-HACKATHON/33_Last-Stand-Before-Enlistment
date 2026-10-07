# #42 — 최초 가입·동의·후속 단계 (FE1 / A-02)

작업 시작 기준: `origin/front/develop b9d2acc` (2026-10-08 fetch).
#53 PostCard/PostDetail/Page/표시 모델/Service 및 #41 인증 구현을 포함한
최신 develop에서 시작했다. FE2 파일을 수정하거나 Feature를 직접 병합하지 않았다.

## 범위 / 연결

F-KZRSXU. `/signup`은 #40의 기존 Route다. #41 LoginProvider의 Provider 확인 +
signup-incomplete 결과 또는 #61이 주입하는 검증된 signup-incomplete Session만
가입 양식에 접근한다. guest/loading/error/member는 신규 가입 제출을 할 수 없다.
OTP UI·Service를 다시 만들지 않는다. 임의 proof/email/userId/token으로 가입하지 않는다.

필수 이용약관/개인정보 동의와 선택 마케팅 동의를 분리한다. 전체 동의는 세 항목을
집계한다. 동의 → 프로필 → 지역을 단일 in-memory SignupDraft로 조립하고 단계 back,
취소/재진입, known failure Retry에서 입력을 유지한다. 최대 10자 닉네임과 50자 소개,
지역 선택, 필수 동의는 최종 제출 시에도 검사한다. 서버 중복/지역/권한 오류는 Service가
표시용 failure로 매핑한다. Mock 중복 닉네임 '중복'은 개발 fixture일 뿐 실제 정책이 아니다.

`SignupService.complete`는 검증된 주체에 바인딩된 Adapter를 주입하는 FE port다.
실제 Method/Endpoint/Request/Response·중복 연결·세션 계약은 #74/#7/#61 대기이며
API SPEC의 접힌 과거 비밀번호 가입 DTO를 사용하지 않는다. API Client를 복제하지
않았고 현재 네트워크 호출은 없다. 실제 Adapter는 #39를 소비하고 성공 Session을
기존 SessionProvider에 동기화해야 한다.

가입 완료가 확인되면 복귀 조회만 별도로 실행한다. 조회 실패 재시도로 가입을
재실행하지 않는다. 결과 불명 예외/진행 중 취소는 unknown으로 표시하고 제출을
차단한다. 실제 회원 조회를 통한 reconciliation은 #61의 계약이 필요하다.
확정 실패만 사용자가 재시도하며 자동 재시도/중복 생성은 없다. Mock은 단일 주체에
최대 한 draft만 저장하며 이는 서버 idempotency 또는 영속 저장 증거가 아니다.

성공 후 계속 버튼은 #40 completeAuthentication으로 원 게시물 또는 메인에
복귀만 한다. 지역/기관 grants를 만들어 내지 않고 반응·투표·북마크 등을 실행하지
않는다. 삭제/이용 불가 원 대상은 기존 home fallback을 사용한다. returnTo는 기존
Navigation의 메모리 상태를 그대로 소비하며 완료/취소 전까지 유지한다.
새로고침 복원, 실제 Adapter의 권한/토큰 검증·저장/재조회는 #61/#64/#68 대기다.

## #43 편집기 경계

최신 develop에는 #43 편집기가 없다. `SignupEditors`의 Profile/Region slot은
controlled value/onChange/disabled props로 합성한다. #43에서 실제 공통 편집기를
이 interface에 주입한다. 새로운 프로필 수정/검색/위치/사진 기능을 #42에 만들지
않았다. production에서 미주입이면 연결 대기를 표시하고 단계를 진행할 수 없다.

`editor-stubs.tsx`는 개발 preview 전용이다. 최소 닉네임/소개/복수 속성·fixture 지역
선택으로 가입 단계만 검증한다. 실제 프로필 저장/중복 조회/사진 업로드/지역 검색/
위치 요청은 없으며 #43 UI 완료로 기록하지 않는다. 후보는 fixture이며 실제 지역 ID가
아니다. 사진 데이터/업로드 참조는 #43 합의 후 같은 props를 확장하며 별도 DTO로
만들지 않는다. `AppProviders`는 안정된 signupService/signupEditors를 받을 수 있다.

Root Provider가 draft를 보유하므로 Page unmount에서 입력을 잃지 않는다. OTP/token은
draft에 없다. localStorage/sessionStorage/로그에 저장하지 않는다. 계정 전환/로그아웃
시 #61이 store.clear 또는 Provider remount로 인증과 draft를 함께 초기화해야 한다.
인증 접근 상실은 진행 중 응답을 무효화한다. 확인된 member 이후 입력은 잠근다.

## 디자인 / 개발 검증

Figma KW 해커톤 디자인 / 최종디자인의 7개 Frame 직접 확인:
1157:1130, 1157:1262, 1157:1410, 1157:1558, 1157:1703, 1157:1801, 1157:1910.
https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1157-1130
이전 비밀번호/고정 코드·타이머/증빙 진입은 최신 정책으로 제외한다. 기존 Header58,
좌우16, 콘텐츠 상단20/하단32, section12, 내부8, 입력/Notice10, 버튼/카드14와
Pretendard 토큰을 재사용한다. 가짜 OS 상태바는 만들지 않는다.
동의 체크 UI를 비교하며 A08/A09 사진/검색/위치는 #43 stub이라 최종 Frame 일치가
아니다. 전체 디자인 시스템/공통 토큰/FE2 Photo를 수정하지 않았다.

`/dev/signup-preview`: NODE_ENV=development에서만 노출. 실제 이메일/코드 없이
검증된 상태 fixture 또는 기존 #41 OTP Mock으로 진입한다. 시나리오/인증 상태/
공유 returnTo를 선택하고 정상·필수 미동의·최소 편집·단계 왕복·실패·취소를 확인한다.
목적지는 개발 안내로만 표시하며 FE2 Page를 복제하지 않는다.

기존 npm lint/typecheck/build와 TypeScript + Node 내장 테스트 방식을 사용한다.
package에 npm test script는 없으며 추가하지 않았다.

```powershell
$signup42Tests = Join-Path $env:TEMP 'discushion-42-tests'
.\node_modules\.bin\tsc.cmd --target es2023 --lib esnext,dom,dom.iterable --module commonjs --moduleResolution node --jsx react-jsx --strict --skipLibCheck --esModuleInterop --types node --rootDir src --outDir $signup42Tests src/features/signup/__tests__/signup.test.ts src/features/auth/__tests__/login.test.ts src/lib/navigation/__tests__/navigation.test.tsx src/lib/api/__tests__/client.test.ts src/components/__tests__/layout.test.tsx src/features/post/__tests__/post.test.tsx
$env:NODE_PATH = (Resolve-Path node_modules).Path
node --test "$signup42Tests/features/signup/__tests__/signup.test.js" "$signup42Tests/features/auth/__tests__/login.test.js" "$signup42Tests/lib/navigation/__tests__/navigation.test.js" "$signup42Tests/lib/api/__tests__/client.test.js" "$signup42Tests/components/__tests__/layout.test.js" "$signup42Tests/features/post/__tests__/post.test.js"
```

Empty는 미입력/지역 미선택 차단으로 검증한다. 위치 후보의 빈 결과/권한 거부, 사진,
기존 회원 프로필 저장·수정은 #43 영역이어서 이 Issue에서는 N/A다. 가입 API의 unknown
결과, 실제 저장/중복 연결, 계정 전환과 새로고침 보존은 Integration에서 별도 검증한다.
