# #90 알림 수신 설정 UI / Mock

- S-EJZFTB / D-11 / FE-R06. Figma 최종디자인 L08 `1160:6943`의 화면·토글·설명문을 직접 확인했다. 정상 화면은 단일 토글을 조작하면 즉시 저장하며 별도 저장 버튼이나 추가 수신 종류를 만들지 않는다.
- 기존 Header/MobileLayout/Button/Notice와 디자인 토큰을 사용한다. L08 토글은 108×44, 트랙은 44×26이며 ON SVG는 Figma 원본을 로컬에 보관했다. OS 상태바는 웹 앱에서 가짜 시간·통신 상태로 그리지 않는다.
- L01 알림 설정 메뉴와 기존 `notificationSettings` 논리 intent로 진입하고 뒤로/취소하면 같은 L01과 scroll·origin으로 돌아간다. URL은 미확정이며 production Route를 추가하지 않았다.
- `AppProviders.pushPreferenceService`에 조회/저장 어댑터를 주입한다. 미주입 상태는 조회 실패/Retry로 표시한다. API SPEC 후순위 설정 경로를 실제 구현으로 가정하지 않았으며 Endpoint·DTO·Provider 구독 계약은 만들지 않았다.
- null 조회값은 미설정이다. 임의 ON/OFF 기본값을 정하지 않고 사용자가 ON 또는 OFF를 명시적으로 선택한다. 개발 Mock의 on/off 시나리오는 테스트 fixture이며 제품 기본값이 아니다.
- 쓰기 Pending은 중복 입력을 막는다. 실패 시 기존 저장값으로 복구하고 Retry는 실패한 값을 다시 저장한다. 취소·계정 변경은 요청을 중단하며 늦은 결과를 반영하지 않는다. Mock 성공은 실제 저장이나 OS 권한/푸시 발송 성공이 아니다.
- PushPreferenceService는 NotificationService 및 목록 store와 독립이다. OFF로 저장해도 앱 내 알림/활동·읽음 상태를 지우거나 차단하지 않는다. 실제 Push ON 1회 시도/실패 자동 재전송 없음은 설명만 유지하며 발송 로직을 구현하지 않는다.
- `/dev/notification-settings-preview`에서 L01→L08→L01 및 앱 내 알림 목록을 같은 기존 host로 연결해 확인한다. on/off/unset/load-error/save-error/pending 및 세션 제한을 선택할 수 있다. production에서는 notFound 처리한다.
- 실 API 연동 대기. 설정 조회/저장·재조회·기본값·권한·오류 계약과 OS/Provider/실 발송은 #66/#67/#68에서 확인한다. 실제 Backend 설정 전용 Issue는 현재 정본에서 확인되지 않았다. API 정본/계약 조율은 #2를 참고한다.
- Node 테스트는 기존 테스트 TS 컴파일 후 `node --test` 방식이다. 브라우저 실제 클릭·스크롤 복귀·Figma 픽셀 비교는 별도로 확인해야 한다.
