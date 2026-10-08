# #47 개인 게시물 목록

- 기준 origin/front/develop: `1b6a060f9c3e2017b1ed263985bc0562446cdaba`. fetch 후 추가 FE merge 없음. 기존 FE2 #53 PostCard/PostDisplayModel/PostDetail, #59 투표 원본 표시 계약 재사용. FE2 파일 수정 없음.
- 범위: F-SSHXAA / F-NZTUYE, D-03/D-04. 작성자 관계(익명 포함), 현재 본인 참여 관계를 postId당 하나로 묶고 전체/안건/활동/투표 유형만 제공한다. 댓글·답글은 댓글 작성, 평가는 댓글 좋아요·싫어요로 묶는다. 작성/북마크만으로 참여를 만들지 않는다.
- PersonalListsService는 FE 표시 관계 계약이다. API SPEC의 myParticipation 제안과 실제 wire DTO를 독자 확정하지 않는다. API Client는 기존 lib/api를 사용할 Real Adapter에서 주입한다. production은 Mock을 기본 제공하지 않고 연결 대기를 표시한다.
- I01/L01에서 같은 provider/store와 I03/I04를 사용한다. 목록별 filter/scroll을 보존하고 기존 /me/posts, /me/participations Route만 등록한다. settings 복귀는 기존 논리 intent를 소비한다. 목록 카드는 기존 /posts/[postId]로 전달하며 상세는 FE2 Owner 구현을 재사용한다.
- renderPersonalDetail 포트는 FE2 상세 소비/Mock 왕복 검증용이다. 개발 preview는 동일 fixture의 PostDetail을 조립하고 취소/삭제·비노출 후 재조회한다. 실제 상세 참여 Service 동기화는 #66/#67에서 검증한다.
- Session/subject/service 변경 시 provider scope를 분리하고 guest/incomplete는 AccessGuard로 차단한다. 계정 간 기록 이관 및 로컬 영속 저장 없음. 취소/재조회/dispose 후 지연 응답은 무효화한다.
- 마지막 표시 원본이 삭제/비노출이면 제목/본문/이미지를 노출하지 않는다. 현재 참여 취소는 해당 관계만 제거하고 다른 행동이 남으면 카드 유지.
- Loading/Empty/Error/Retry/unavailable/권한/뒤로가기/상세 왕복을 구현한다. 읽기 목록이므로 제출 Validation/저장 Success/Pending mutation은 N/A. 투표 진행/종료는 카드에 표시하고 별도 상태·주제·반응 필터 없음.

## 디자인

Figma 최종디자인 I03 `1160:4807`, I04 `1160:4930`의 design context와 screenshot 직접 확인. Frame은 396×852, 16px 좌우, 20/32px 상하, 12px gap, 116px chip 기준. Header/BottomNavigation/MobileLayout/Button/Notice 및 FE2 PostCard 재사용. OS status bar는 웹 UI 대상에서 제외한다. I04 명칭/참여 평가 라벨 및 전체 필터는 최신 PRD/명세 정책을 적용한다. 공유 PostCard의 기존 디자인 차이는 FE2 파일을 수정하지 않고 유지한다. 브라우저 실행 환경(apps/browsers)이 없어 실제 렌더 396×852 비교·스크롤/native history 인수는 미실행.

관련 Frame: I03 1164:8407/8708/8831, I04 1164:9468/9657/10057/10255/10451/10849/11038. 변형 Frame 전체의 직접 화면 비교 완료를 주장하지 않는다.

## 검증 / 대기

기존 Node 방식: rg로 *.test.ts/tsx 수집 → 로컬 tsc를 이용해 임시 폴더로 CommonJS 컴파일 → NODE_PATH=node_modules → node --test 생성된 *.test.js. npm test script는 없다.
새 model/SSR 테스트는 익명 작성, 3유형, 계정 분리, 중복/취소, 타인/작성만 제외, 삭제, 투표 선택, 별도 filter/scroll/origin, 실패 Retry, 지연 응답 무효화와 guest/가입 미완료 차단을 검증한다.

실 API 연동 대기: Backend #27/#15/#16/#5는 계약 참고이며 실제 구현 완료로 간주하지 않는다. Backend 파일 수정 없음. #66 원본/현재 참여/집계 동기화, #67 Page/Provider 등록, #68 실제 사용자 및 브라우저 인수 필요. #48 참여 투표 전용 목록, #49 북마크, 실 Privy/Backend Integration과 다음 Issue는 구현하지 않는다.