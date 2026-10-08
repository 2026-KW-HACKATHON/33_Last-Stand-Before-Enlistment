# #87 관심 지역 · D-08 / FE-R02 / F-MBJNPG

- Figma I08: `1160:5516`, `1164:11981`, `1164:12042`.
- I01 메뉴에서 논리 목적지 `interestRegions`를 열고 Header/돌아가기로 복귀한다. 미합의 production URL은 추가하지 않는다. 원 화면을 유지해 마이페이지 상태를 보존한다.
- 추가/제거 즉시 동일 Service의 `save`를 호출한다. 성공한 목록만 표시하고 실패·취소 시 마지막 성공 값을 유지한다. 0개 허용, 개수 제한 없음. 기본 활동 지역·이웃 권한·기관 담당 지역은 수정하지 않는다.
- `AppProviders.interestRegionService`와 인증된 `subjectKey`로 주입한다. 계정/세션 변경 시 화면·Store를 분리하고 이전 요청을 abort한다. Adapter는 해당 인증 계정에 바인딩해야 하며 AbortSignal을 존중해야 한다.
- Service는 기존 `SignupRegion` 표시 타입을 사용하는 FE port이다. Method/Endpoint/DTO 정본을 확정하지 않는다. 미주입은 오류 상태이며 production에서 Mock을 활성화하지 않는다.
- 개발 전용 `/dev/interest-regions-preview`: 정상·0개·조회 실패·검색 실패·저장 실패(각각 Retry 가능)·Pending 취소, 검색 결과 없음, 세션 제한. 실제 I01 연결은 `/dev/my-page-preview`의 관심 지역 메뉴에서 확인한다.
- Node 테스트: 검색/즉시 반영/재조회/0개/개수 무제한/실패·Retry/취소/중복 요청/늦은 응답/계정 격리/입력·스크롤 상태/권한/SSR UI. 실제 브라우저 클릭·396×852 시각 비교는 별도 인수 항목이다.
- 실 API 연동 대기: BE #9 공통 Region 원본과 본인 관심 지역 조회·영속 저장 계약. #66/#68에서 준비된 실제 접점의 저장·재조회·실패 복원·탐색/추천 입력 소비를 확인한다. Mock 성공은 실제 인증·영속 저장·Integration 완료가 아니다.
