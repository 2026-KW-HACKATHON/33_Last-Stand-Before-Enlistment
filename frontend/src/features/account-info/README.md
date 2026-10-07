# #92 계정 정보 조회 (A-05 / FE-R14 / F-JRAFSD)

- L02 Figma 1160:6480, 1164:5414, 1164:9610, 1164:10991 직접 확인. Header/MobileLayout/Notice/SettingRow와 기존 토큰 재사용. 비밀번호 변경은 현행 정책에서 제외, OS 상태바는 기존 웹 safe-area 사용.
- 기존 논리 account를 SettingsNavigation에서 소비한다. L01 또는 I01을 마운트 상태로 보존하고 뒤로가기는 원 진입으로 복귀한다. production URL을 추가하지 않는다.
- AccountInfoService.load는 registeredEmail 표시를 위한 Client port다. null만 미설정이며 오류는 별도 Error/Retry다. 오류를 탈퇴 상태로 해석하지 않는다. Service 미주입은 조회 오류로 표시하며 production Mock은 주입하지 않는다.
- renderEmailChange는 #93 Owner가 소비할 emailChange 논리 목적지, AbortSignal, onChanged/onCancel/onFailure를 받는다. 성공 결과는 이메일값을 직접 덮지 않고 같은 Service를 재조회한다. 실패/취소에는 기존 값 유지. 재조회 실패 시 새 값이나 완료를 추정하지 않고 Retry한다.
- 이메일 변경 입력·Privy·비밀번호·OTP·실 저장은 구현하지 않았다. 개발 stub은 결과 콜백과 재조회만 검증한다. 중복 진입과 취소/계정 변경 이전 늦은 응답은 무효화한다.
- /dev/account-info-preview는 development 전용. 실제 L01/I01 메뉴를 재사용하며 지연/미설정/조회 실패/재조회 실패/변경 결과/취소/실패/미연결 및 Session 전환을 제공한다.
- 입력/Validation/저장 Pending은 조회 전용 #92에 N/A. 조회 중 변경 버튼 Disabled, Empty는 null일 때만 표시. 스크롤은 L02 store와 기존 L01/I01 저장 구조로 유지한다.
- 실 API 연동 대기: #74/#2 계약과 #8 계정/세션 연결을 확인한 후 #61/#67/#68에서 실제 Adapter/Privy/로컬 회원 이메일 일치 검증. Backend 변경 없음.
- 브라우저 클릭/스크롤/native history/396×852 Figma 픽셀 비교는 별도 인수 필요. Node/SSR는 브라우저 인수를 대체하지 않는다.
