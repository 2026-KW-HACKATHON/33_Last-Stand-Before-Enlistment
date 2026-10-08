# #44 이웃 완료 지역·참여 자격

F-ATWJDJ. 독립 제품 Page/신청 Route를 추가하지 않고 기존 화면 Owner가 NeighborProvider + NeighborStatus를 합성합니다. 대상/활동 지역과 돌아가기 콜백을 주입합니다. 돌아가기는 원 화면의 필터/입력/탭/스크롤 snapshot을 변경하지 않는 콜백으로 연결하고, 인증 콜백은 기존 Navigation.beginAuthentication(origin/returnTo)을 소비합니다. 인증 완료 뒤 원 행동을 자동 재실행하지 않습니다.

NeighborService는 조회 전용 FE 표시 port이며 wire DTO/endpoint가 아닙니다. 실제 #11/#74 adapter는 본인의 완료 지역만 반환하고 최대3/중복/유효 식별자를 검증해야 합니다. NeighborProvider에 실제 인증 주체의 안정적인 subjectKey를 전달합니다. 로그아웃/계정 변경/Service 변경은 조회를 취소하고 이전 결과를 폐기합니다. Capabilities는 기존 SessionProvider가 관리하며 이 provider는 실제 권한을 생성/변경하지 않습니다. 완료 목록과 지역 grant가 불일치하면 재조회 안내를 표시합니다. retrySession은 Capabilities 재조회 담당입니다.

완료/미완료/타 지역/최대3/loading/error→retry Mock은 /dev/neighbor-preview에서 확인할 수 있습니다. production은 notFound이며 Mock은 제품 Provider에 등록되지 않습니다. 원 화면 복귀/기본 활동 지역 복귀/필터 보존/인증 취소는 개발 harness로 확인 가능합니다. 로그인/가입/활동 지역/기관 자격은 이웃 권한을 대체하지 않습니다. 신청/증빙/제출/승인 UI, 저장 API는 없습니다.

Figma 최종디자인 WF-F20 (1162:5991) 직접 확인: Notice 문구, 52px secondary 버튼, 20px 안내 패널 radius와 38/36px padding을 적용합니다. 공통 Header/BottomNavigation/Button/Notice와 토큰을 재사용합니다. 완료 목록은 독립 Frame이 없는 #44 상태 계약에 따른 합성 UI입니다. FE2 Page/PostCard/PostDetail/참여 파일은 변경하지 않았습니다.

실 API 연동 대기: #11/#4/#9/#74/#75. #62/#68에서 실제 계정, 3지역 상한, 타 지역 서버 거부, 자격 갱신, returnTo와 각 참여 동작을 검증합니다. 제품 접점은 해당 Page Owner의 Integration에서 연결해야 합니다. 읽기 기능이므로 입력 validation/쓰기 success/failure/pending은 N/A; 조회 성공/실패/loading/retry와 잘못된 완료 목록은 검증 대상입니다.
