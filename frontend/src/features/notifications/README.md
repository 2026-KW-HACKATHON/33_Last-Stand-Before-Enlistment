# #89 알림·활동 UI / Mock

- 기능명세: F-ITKTQN / S-KIPYBM, D-10 / FE-R05.
- Figma 최종디자인: H01 `1160:3940`, 알림 `1164:11788`, 활동 `1170:7447`, H02 `1160:4069`, H03 `1160:4427` 직접 확인.
- `notifications`는 기존 논리 목적지다. URL은 아직 미확정이므로 production Route를 추가하지 않았다. Header/BottomNavigation과 마이 메뉴의 intent는 기존 중앙 Navigation host에서 소비한다.
- `AppProviders.notificationService`로 본인 목록·읽음·원본 가용성 어댑터를 주입한다. API SPEC의 예정 Endpoint를 구현 완료로 간주하지 않으며 API Client, DTO, 이벤트 Enum을 추가하지 않았다. 어댑터가 없으면 인증 확인 또는 조회 실패/Retry가 표시된다.
- 원본 선택은 읽음 처리 성공 → 삭제/접근 권한 확인 → 같은 `post` 목적지 순서다. 실패 시 기존 읽음 상태를 보존하며 원본 확인 실패 재시도는 완료된 읽음을 다시 저장하지 않는다.
- 기관 채택/취소는 동일 안건의 기존 공개 채택 표시로 이동한다. #50의 K04 연결은 `onNotificationTarget(target, entry, onReturn, signal)` 포트로 소비할 수 있다. 별도의 미확정 기록 URL이나 담당자 개인정보를 추가하지 않는다.
- H01은 원본 이동 중에도 유지하며 탭·목록·읽음·스크롤을 저장한다. 원본의 공통 뒤로가기는 `origin: notifications`로 돌아온다. 계정 scope 변경 시 store와 요청을 폐기한다. 상세 어댑터는 AbortSignal을 준수하고 복귀 시 onReturn을 호출해야 한다.
- `/dev/notifications-preview`는 development에서만 열린다. 기존 `createMockPostService`와 FE2 `PostDetail`을 그대로 소비한다. 목록/빈 목록/조회 오류/읽음 오류/원본 오류/삭제/접근 불가/Pending 취소 및 게스트·가입 미완료 상태를 선택할 수 있다.
- Mock은 이벤트를 생성하거나 Push를 발송하지 않는다. 관심 정보 저장으로 새 글 알림을 생성하지 않으며, 활동 탭은 개발용 본인 참여 기록 fixture로 검증한다. 채택/취소 fixture도 실제 서버 이벤트가 아니다.
- 실 API 연동 대기. 수신자·중복 방지·자기 행동 수신 제외·서버 읽음 저장·실 원본 권한/삭제 일관성은 #65/#66/#67/#68에서 확인한다. 알림 수신 설정 #90과 다른 기능 Issue는 구현하지 않았다.
- Node 테스트는 기존 방식(테스트 TS 컴파일 후 `node --test`)을 따른다. 브라우저 픽셀 비교·클릭·native history 인수는 별도로 필요하다.
