# #94 현재 기기 로그아웃 UI·Mock

- FE1 / A-07 / FE-R15 / S-HDQTIR (F-SAOWVT).
- 시작 기준: origin/front/develop `983b8ccf21fa4b11bd4be782c1da8c9e79573e2a`. fetch 전후 develop 변경 없음. 원격 Feature 변경은 사용하지 않았다.
- L01의 기존 로그아웃 메뉴만 연결한다. 성공 시 기존 accountReturnDestinations.logout(`/`, A01)을 replace로 사용한다. 새 production URL, 서버 Endpoint/DTO, SDK 의존성은 추가하지 않는다.

## Client port / Session / Navigation

`CurrentDeviceLogoutService.endCurrentDevice(subjectKey, signal)`은 현재 기기 종료 성공 또는 실패를 반환하는 Client interface다. 실제 Privy Method나 Backend 구현 완료를 뜻하지 않는다. AppProviders는 선택적 logoutService를 주입받고, 미연결은 연결 대기로 표시한다. production에서 Mock을 기본 주입하지 않는다.

동일 subjectKey의 성공 결과만 Session을 guest로 바꾼다. 실패/예외/다른 계정 결과/취소/늦은 응답은 기존 Session과 Capabilities를 유지한다. 요청 중에는 중복 실행과 다른 설정 메뉴를 막고, 뒤로가기·화면 숨김·계정/Session 전환·Adapter 교체는 대기 요청을 취소한다. 성공한 계정은 Adapter 재생성만으로 member로 복구하지 않는다. 실제 재로그인은 upstream Session/계정 전환으로 새 scope를 시작해야 한다.

성공 때만 Navigation의 이전 계정 history/returnTo/snapshots를 비우고 A01로 이동한다. 실패·취소는 이 상태를 지우지 않는다. 기존 일반 게시물/공유 returnTo, 삭제 fallback, 가입 및 설정 진입 구조는 재작성하지 않았다. guest의 보호 화면 접근 제한과 공유 상세 허용은 기존 AccessGuard를 소비한다.

Mock Adapter 한 인스턴스는 현재 기기 하나를 나타낸다. 다른 인스턴스의 상태는 바꾸지 않는다. 실제 전역 로그아웃·토큰 무효화·계정 삭제·회원탈퇴는 수행하지 않는다. 실제 Adapter는 #74/#61에서 Privy와 로컬 세션 종료 순서, 실패·부분 종료 및 취소 뒤 서버 결과 재조정을 확인해야 한다. 성공 결과는 실제 필요한 현재 기기 종료가 확인된 뒤 반환해야 한다.

## 상태 / Preview

`npm run dev` → `/dev/logout-preview` (production은 notFound).

정상 / 첫 실패 후 Retry, 메인·마이 설정 진입, member/guest/signup-incomplete/loading/error, 계정 변경, Adapter 미연결을 제공한다. 성공은 기존 StartScreen을 표시한다. Preview의 로그인 화면은 연결 대기이며 실제 재인증을 구현하지 않는다.

- Pending / Disabled: 처리 안내, 중복 실행 방지.
- Failure / Retry / 취소: 로그인 유지, 명시적 재시도.
- Success: 현재 기기만 guest, A01 복귀.
- Loading / Error / 권한 제한: 기존 Session AccessGuard.
- Empty / 입력 Validation / 필터 / 탭: 단일 메뉴 동작이므로 해당 없음.
- 실패·취소 시 원 L01과 기존 scroll 보존. 성공은 이전 계정 Navigation 상태 제거.

## 디자인 / 공통 코드

Figma `Wobmrjd8xSUNKQISplV5aR` 최종디자인 L01 `1160:6401`, A01 `1157:905`를 직접 조회했다. 기존 Header/MobileLayout/SettingRow/Notice/Button과 디자인 토큰을 재사용한다. A01의 StartScreen과 기존 이미지도 그대로 사용한다. 웹에 OS 상태바를 추가하지 않는다. 브라우저 396×852 렌더 비교·클릭 인수는 미검증이다.

## 검증 / 대기

기존 Node 방식: `rg --files src -g '*.test.ts' -g '*.test.tsx'`로 테스트를 찾고, 기존 tsc의 CommonJS/React JSX/strict 컴파일 후 NODE_PATH=node_modules로 출력 테스트를 `node --test` 실행한다. npm test script는 없다.

2026-10-08: lint/typecheck/build 통과. 기존 Node 테스트 299개(신규 22개) 통과. 신규 테스트는 성공/A01/보호 접근/공유 guest/다른 기기 유지, 실패와 Session/Capabilities/Navigation 유지, Retry/중복/취소/계정 전환/늦은 응답/미연결/설정 메뉴 렌더를 검증한다. DOM 이벤트·브라우저 history 인수와 실제 Provider/API 종료는 미검증이다.

실 API 연동 대기. #74/#61 실제 Provider·로컬 세션 연결, #67/#68 후속 통합 검증이 필요하다. Backend/FE2 Domain, 전체 기기 로그아웃, 회원탈퇴, 기관/이웃 자격 서버 정본 변경은 범위 밖이다. commit/push/PR/merge는 수행하지 않는다.
