# #48 참여한 투표 (D-05 / F-QPGNCF)

## 기준과 범위

- 시작 origin/front/develop: `77fb9c15e93bccfca19b2bb0b2caeb4034f3f377`. 이전 `1b6a060` 이후 #47 및 FE2 #60 공유·게스트가 통합됨. 다른 Feature merge/cherry-pick 없음.
- 기존 `/me/votes` identity를 Page로 등록하고 I01의 참여한 투표 메뉴를 연결한다. 별도 L01 메뉴를 추가하지 않는다.
- FE2 PostCard / VotePostDisplay / VoteSnapshot / PostDetail / VotePanel 및 기존 개인 목록 상세 renderer 포트를 소비한다. FE2 파일은 수정하지 않는다.
- 개인 목록은 읽기 전용이다. 제출·변경 로직은 상세의 기존 VotePanel에만 있다. 개발 preview에서 같은 VoteService를 상세와 목록에 사용해 선택 변경 후 복귀·재조회한다.
- subject/session 변경 시 Provider scope 분리, close/dispose/후속 조회 시 이전 응답 무효화. production은 Adapter 주입 대기를 표시하며 Mock을 기본 등록하지 않는다.

## 표시 계약과 미합의 항목

MyVotesService는 FE 읽기 표시 포트이며 서버 DTO/Endpoint/Enum 정본이 아니다. Real Adapter는 합의한 계약과 기존 API Client를 소비해야 한다. 본인 선택은 VoteSnapshot.submittedOptionId로만 표시한다. 미조회/알 수 없는 선택을 최다 득표로 채우지 않는다. 동률은 함께 표시하며 0표는 집계 없음이다.

공개 가능한 레코드는 기존 Post 원본과 VoteSnapshot을 참조한다. 상태는 원본의 OPEN/ENDED를 사용하며 FE 시계로 종료를 확정하지 않는다. timingLabel/reminderLabel은 Adapter가 제공하는 표시값이다. Mock의 남은 기간/종료 시각은 시연용이고 실제 알림 예약/발송을 주장하지 않는다.

삭제/접근 불가 레코드는 postId/subject/참여시각/availability와 알려진 상태만 보존한다. 제목·본문·사진·선택지·본인 선택 텍스트·집계는 담지 않는다. 전체 탭에는 유지하며 현재 접근 불가 안내로 연결한다. API SPEC §8.2와 FE 지침서가 삭제 기록의 상태 필터를 확인 필요로 명시하므로, 알려진 상태만 해당 탭에 분류하고 상태 미조회는 전체에만 표시하는 FE Mock 규칙을 사용한다. 실제 서버 필터 계약 확정은 #27/#66에서 필요하다.

## Figma와 검증

최종디자인 `1160:5075`, `1164:11290`, `1164:11601`의 design context와 screenshot을 직접 확인했다. 396×852, 콘텐츠 364px, 3열 칩 116px/8px gap, Notice와 기존 Header/BottomNavigation/MobileLayout을 재사용한다. 정적 아이콘은 기존 public/icons의 동일 공통 자산을 사용한다. OS 상태바는 웹 구현 대상이 아니다. FE2 PostCard의 기존 본문/메타 구조 차이는 이번 Issue에서 재작성하지 않는다.

브라우저 도구의 apps/browsers 목록이 비어 있어 실제 모바일 화면·클릭·native history·스크롤 비교는 미실행이다. 모델/SSR Node 테스트로 필터, 실제 선택/최다·동률·0표·조회 대기, 계정 분리, 삭제 기록 비노출, 변경 후 동일 원본 재조회, Retry·조건/scroll 보존, stale 응답 및 권한 차단을 검증한다.

실행 방식: 기존 테스트처럼 `rg`로 `.test.ts/.tsx` 수집 → 로컬 tsc로 임시 CommonJS 출력 → NODE_PATH=node_modules → `node --test`. npm test script는 없다. 읽기 화면의 제출 Validation/저장 Pending/Success는 N/A이며 상세 mutation은 FE2를 재사용한다.

## 남은 사항

실 API 연동 대기: #27 개인 목록 / #25 실제 한 표·종료·집계 / #15 원본 상세 / #16 삭제 계약. #59 도메인 UI를 소비하며 #66 원본·본인 선택·삭제 필터·집계 실제 연결, #67 Provider/Page/왕복, #68 396×852 브라우저 인수와 전체 여정 검증이 남는다. Mock 성공을 실제 저장/인증/Integration 완료로 기록하지 않는다. #49 및 다음 Issue는 구현하지 않는다.
