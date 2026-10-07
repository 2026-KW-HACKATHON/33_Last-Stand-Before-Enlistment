# #93 Privy 이메일 변경·확인 UI/Mock

- FE1 / A-06 / FE-R16 / S-YXAQIT. F-JRAFSD 계정 메뉴의 L02→L03→L04→L02 접점.
- 최신 `origin/front/develop` `5b577c5156d7bae64933dbc0b58da94de28e0d7b`에서 시작. 이전 `8e9097d` 이후 #92 계정 관리만 추가 통합됨. 새 FE2 변경·충돌 없음.
- #92 `AccountInfoService`, `EmailChangeRequest`, `renderEmailChange`와 성공 재조회·취소 콜백을 재사용한다. 원 L01/I01 및 L02를 유지한 채 이메일 변경 화면을 표시한다. 취소는 L02, L04 뒤로가기는 입력 보존 L03, L02 뒤로가기는 원 L01/I01로 돌아간다.
- AppProviders의 `emailChangeService`는 Integration 주입 포트다. 미연결 시 연결 대기이며 production에서 Mock 성공을 만들지 않는다. 기존 `renderEmailChange` override를 유지한다. URL/Endpoint/DTO는 추가 확정하지 않는다.

## Client Service와 상태

`begin`은 본인·새 이메일 확인을 시작하고 Adapter 소유의 메모리 handle을 반환한다. `confirm`이 확인 성공을 반환한 경우에만 `change`를 호출한다. 저장 결과의 동일 subjectKey/이메일을 확인한 후에만 #92 결과 재조회를 호출한다. handle은 표시·로그·영속 저장하지 않으며 수정/재시작/취소/세션 전환/완료 시 해제한다. AbortSignal은 각 호출에 적용된다. `release`는 해당 확인 흐름을 종료하는 Adapter 포트다.

이는 Client interface이며 Privy SDK Method나 서버 계약이 아니다. 실제 Adapter는 Privy의 지원되는 본인/이메일 확인, 중복 판정, 동일 로컬 회원 동기화가 성공한 경우에만 changed를 반환해야 한다. 취소 시 이미 서버에서 완료된 변경의 결과 재조정도 실제 Integration에서 검증해야 한다.

- 이메일 형식/현재 이메일 동일 여부, 중복 이메일 거절 및 수정.
- 현재 이메일 Loading / Empty(미설정) / Error / Retry.
- 확인 시작 Pending, 확인 Pending, 변경 Pending, 성공 재조회.
- 발송 시작 실패 / 확인 실패 / 만료 / Provider 취소 / 변경 실패와 명시적 재시도.
- 실패 시 기존 이메일 유지. 만료/취소 후 기존 handle 재사용 금지.
- 로그인/가입 필요·세션 Loading/Error는 기존 AccessGuard 재사용. Session/Capabilities를 쓰지 않으므로 이웃 지역/기관 상태를 부여하거나 초기화하지 않는다.
- 별도 리스트/필터/탭은 N/A. L02 scroll과 원 설정/마이 화면은 기존 Host가 보존한다. 새로 열거나 계정 변경 시 새 입력 흐름을 사용한다.

## 개발 Preview

`npm run dev` → `/dev/email-change-preview` (production은 notFound).

메인→설정→계정 / 마이→계정 양쪽에서 확인할 수 있다. 정상·미설정·조회 실패·확인 시작 실패·확인 불일치·만료·취소·중복·처리 실패 시나리오, 세션/계정 변경, 미연결을 제공한다. 오류 시나리오는 첫 시도 실패 후 Retry 성공이다. `other@example.com`은 계속 중복 거절된다. L02 조회와 L03/L04 변경은 같은 회원 fixture를 공유한다. 실제 발송/인증/저장은 수행하지 않는다.

## 디자인 근거

Figma `Wobmrjd8xSUNKQISplV5aR` 최종디자인의 #93 지정 15 Frame을 직접 조회했다.
- L02: `1160:6480`, `1164:5414`, `1164:9610`, `1164:10991`
- L03: `1160:6527`, `1164:5461`, `1164:8954`, `1164:9007`, `1164:9799`
- L04: `1160:6577`, `1164:5514`, `1164:5570`, `1164:5626`, `1164:10199`, `1164:11180`

기존 Header/MobileLayout/Notice/Input/Button의 디자인 토큰을 재사용한다. 396px 모바일 폭, 콘텐츠 top20px/page16px/section12px/bottom32px 규격이다. 최신 정책에 따라 과거 비밀번호 입력은 Privy 확인 안내로, 고정 인증번호 길이/타이머는 Provider 확인 상태로 대체했다. OS 상태바는 웹 화면에 복제하지 않는다. 브라우저 396×852 실제 렌더/클릭 인수는 미검증이다.

## 검증과 남은 사항

기존 Node 테스트 방식: `rg --files src -g '*.test.ts' -g '*.test.tsx'`의 파일을 기존 tsc로 CommonJS/React JSX/strict 컴파일하고 `NODE_PATH=node_modules`로 출력의 모든 `*.test.js`를 `node --test` 실행한다. npm test script는 없다. 새 테스트는 Validation/성공/실패/Retry/중복/취소/계정 전환/늦은 응답/handle 재사용 거절/L02 재조회·scroll/양쪽 origin/권한 제한/공통 UI 렌더링을 포함한다.

실 API 연동 대기. #74 Provider 지원·동일 회원 매핑과 로컬 동기화, #61/#64/#68 실제 인증/저장/재조회·권한·연락 이메일 동기화·실패 및 최종 Integration을 확인해야 한다. 활동 연락 이메일·기관 정본·이웃 자격의 실제 서버 동기화는 구현하지 않았으며 FE2 파일은 수정하지 않는다. SDK 의존성, 자체 OTP 서버, 계정 복구, 비밀번호 기능은 추가하지 않는다.

최종 실행 결과(2026-10-08): `npm run lint`, `npm run typecheck`, 기존 tsc→`node --test` 전체 277개(신규 31개), `npm run build`, `git diff --check` 및 새 파일 whitespace 검사 모두 통과. 브라우저 인수·실 Provider/API/Integration은 미검증이다. commit/push/PR/merge는 수행하지 않았다.
