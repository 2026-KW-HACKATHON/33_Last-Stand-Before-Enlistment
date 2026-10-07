# #88 관심 키워드 · D-09 / FE-R03 / F-OAPPBV

- Figma L07 `1160:6874`, `1164:6114`, `1164:9400` 직접 확인. 기존 Header/MobileLayout/Button/Notice와 선택 색상·간격·Typography 토큰 재사용. 칩 미선택 색상은 해당 Frame의 로컬 스타일이며 전역 Theme는 변경하지 않는다.
- FE2의 기존 `boardTopics` / `BoardTopic`을 읽어 동일한 7개 주제만 사용한다. 추천 소비자는 동일 `InterestKeywordService.load`의 저장값을 사용한다. 게시판 자동 필터·추천 산출·임의 가중치는 구현하지 않는다.
- 최대 4개 선택. 4개여도 선택된 항목 해제는 가능하다. 미설정/0개도 저장 가능하며 자유 입력은 없다.
- 실패 시 저장값과 화면 선택을 마지막 성공값으로 복원한다. Retry는 실패했던 요청을 다시 보낸다. 실패 후 새 선택을 하면 이전 Retry 요청은 폐기한다.
- Header 뒤로가기/취소는 요청을 abort하고 저장값으로 복원한다. 성공 시 L01 복귀. L01은 계속 mount되어 원 진입과 스크롤이 보존된다. L07 URL은 미합의라 추가하지 않는다.
- `AppProviders.interestKeywordService`와 인증된 `subjectKey`를 주입한다. Adapter는 동일 계정에 바인딩하고 AbortSignal을 존중해야 한다. 계정 변경 시 Store/화면을 분리한다. production Mock 기본 활성화 없음.
- 개발 전용 `/dev/interest-keywords-preview`: 정상/미설정/4개/조회 실패/저장 실패/Retry/Pending 취소/세션 제한. 실제 설정 메뉴 연결은 `/dev/settings-preview`.
- 실 API 연동 대기. Service는 FE port이며 Endpoint/DTO 정본이 아니다. #66/#68에서 실제 저장/재조회/본인 권한/오류 및 추천 입력 소비를 확인한다. Mock 성공은 영속 저장·Integration 완료가 아니다.
- Node Store/SSR 검증과 브라우저 인수는 구분한다. 실제 클릭·뒤로가기·스크롤 및 396×852 시각 비교는 별도 인수 항목이다.
