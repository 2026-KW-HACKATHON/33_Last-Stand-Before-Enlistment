# #95 Privy 본인 확인·회원 탈퇴 UI·Mock

FE1 / A-08 / FE-R17 / S-FIUQIE (F-SAOWVT).

## 기준과 범위

최신 origin/front/develop `98f92487065c1ccb1ba52533ee416955f8cd4a6d`에서 front/feature/95-privy-withdrawal을 생성했다. 이전 `86786bf` 이후 #94가 통합되었으며 신규 FE2 merge는 없다. 기존 FE2 #57/#58, 공통 UI, Session, Navigation 및 #94 로그아웃 연결을 유지한다. 다른 Feature merge/cherry-pick은 수행하지 않았다.

L01 회원 탈퇴 메뉴 → L09 주의 → L10 Privy 확인 상태 → L11 최종 확인 → Pending → L12 완료 → 사용자가 버튼을 누르면 기존 A02 `/login`으로 replace 이동한다. 새 production URL은 만들지 않는다. 기존 SettingsNavigation의 논리 withdrawal intent와 공통 accountReturnDestinations.withdrawal을 소비한다. L01과 원 Page는 유지해 취소 시 원 설정과 scroll로 돌아간다.

## Client interface와 Session

WithdrawalService.verify(subjectKey, signal)는 Adapter가 소유하는 불투명 메모리 confirmation을 반환한다. withdraw(confirmation, signal)는 해당 로컬 회원 탈퇴/필요한 세션 처리가 확인된 경우에만 withdrawn과 동일 subjectKey를 반환해야 한다. release는 메모리 확인 흐름 종료 포트다. 이는 Privy SDK Method·HTTP Endpoint·DTO·DB 삭제 정책이 아니며 실제 Provider 지원은 #74 확인 대기다. AppProviders는 선택적 withdrawalService를 주입받는다. 미연결 시 연결 대기이며 production에 Mock을 기본 주입하지 않는다.

주의사항 다음 확인과 최종 확인은 별도 사용자 행동이다. 확인 실패/만료/취소는 L10, 탈퇴 실패는 L11에서 명시적으로 재시도한다. 탈퇴 처리 중 만료/취소는 ticket을 폐기하고 L10 재확인을 요구한다. 중복 요청을 막고 back/cancel/계정·upstream Session·Adapter 변경 시 AbortSignal과 generation으로 늦은 응답을 무효화한다. ticket은 표시·로그·영속 저장하지 않는다.

동일 subjectKey의 성공만 guest로 표시하고 이전 계정 Navigation history/returnTo/snapshots를 비운다. 실패/취소는 동일 Session/Capabilities와 Navigation을 유지한다. 성공 화면은 public L12로 유지되며 완료 버튼 전에 자동 로그인 이동/자동 행동 실행을 하지 않는다. Adapter 재생성만으로 이전 member를 복구하지 않고 upstream 재인증 상태 전환을 기다린다. 실제 서버에서 이미 처리된 뒤 취소된 결과의 재조정과 일부 종료/실패는 Integration에서 확인해야 한다.

공개 게시물·댓글·답글/참여 집계 보존, 공개 작성자 ‘회원 탈퇴한 사용자’, 기존 익명 표시 유지, 식별 연결·북마크·개인 설정·활동 문의 이메일 정리 정책을 L09에 안내한다. 독립 Mock fixture에서 관계 정리와 집계 보존을 검증한다. FE2 원본/실 DB/실 Provider 계정은 변경하지 않으며 전체 기기 로그아웃·임의 Provider 사용자 삭제·회원 데이터 삭제 로직은 추가하지 않는다.

## 디자인과 상태

Figma 최종디자인을 직접 조회했다: L09 1160:6991, L10 1160:7035, L11 1160:7084, L12 1160:7128, 확인 실패 1164:9352. 공통 Header/MobileLayout/Button/Notice 및 page16/top20/bottom32/section12, text-section/body/caption 토큰을 재사용한다. 과거 비밀번호 입력·불일치는 최신 제품 정책대로 Privy 확인 안내·실패/재시도로 대체한다. 제품 화면에 OS 상태바를 복제하지 않는다. 새 정적 이미지/아이콘 자산은 없다.

Pending/Disabled, Failure/Error/Retry, 확인 만료·취소, 미연결, Success, Session Loading/Error/guest/signup-incomplete 제한을 처리한다. Empty/입력 Validation/필터/탭은 단일 확인 흐름이므로 N/A다. 확인 뒤 back은 ticket을 폐기하여 재확인이 필요하다. 실패는 현재 단계와 원 L01을 유지하고 취소는 L01로 복귀한다.

## 개발 Preview와 검증

npm run dev → /dev/withdrawal-preview (production은 notFound). 정상, 확인 오류, 만료, Provider 취소, 탈퇴 실패 후 Retry, Session/계정 전환·Adapter 미연결을 제공한다. 성공 다음 A02는 기존 LoginScreen을 사용하며 실제 재로그인 Adapter 연결은 대기다. Mock임을 Preview 상단에 명시한다.

기존 Node 방식: 실제 *.test.ts/*.test.tsx를 기존 tsc CommonJS/React JSX/strict로 컴파일하고 NODE_PATH=node_modules로 출력 테스트를 node --test 실행한다. npm test script는 없다. 모델과 SSR 테스트에서 선행 단계·최종 동의, 성공/L12/A02/Session·보호 접근·공유 접근, 공개/익명/집계 보존, 관계 정리, 확인 실패·만료·취소·탈퇴 실패/Retry, 중복·취소·back·계정 변경·늦은 응답·미연결·오류·기존 논리 설정 intent를 검증한다.

브라우저 DOM 클릭/history·scroll 인수 및 396×852 실제 렌더 비교는 미검증이다. 실 API 연동 대기. #74/#8/#61 실제 Privy·로컬 회원/세션 계약과 #68의 실제 본인 확인·탈퇴·데이터 처리·권한·실패 복구 검증이 필요하다. 별도 Integration Issue, dependency/lockfile, Backend/FE2 Domain 변경을 추가하지 않는다. commit/push/PR/merge는 수행하지 않았다.

최종 검증(2026-10-08): npm run lint/typecheck/build 통과, 기존 Node 테스트 전체 327개(신규 28개) 통과, git diff --check와 새 파일 whitespace 검사 통과. 초기 lint의 useMemo 형식 오류는 수정 후 재검증했다.
