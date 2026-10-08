# #50 기관 안건 업무

기능 ID: F-CNNPYL, S-PCCNUU, F-TUGMEP, S-AQOBIE. FE1 D-06/07.

최신 기준 `origin/front/develop`: `49a894f` (#49 병합 포함). 새 브랜치는 `front/feature/50-institution-agendas`이며 기존 `globals.css` 미커밋 변경은 보존했다.

## 화면과 계약

- 기존 합의 Route `/officer/agendas`, `/officer/agendas/[postId]`만 사용한다. 목록 → 검토 → 채택 기록은 같은 상세 내 Client State다.
- 기존 InstitutionProvider의 자격/유효기간과 Session Capabilities를 함께 확인한다. 전체 공개 지역 안건을 반응 합계 내림차순으로 열람하고 지역/본 기관 현재 채택으로 필터링한다.
- 채택 및 본 기관 취소는 유효 기관·담당 지역 안건으로 제한한다. 기관 ID로 관계를 구분하며 기관명으로 소유권을 추측하지 않는다. 게시물 상태/반응을 변경하지 않는다.
- AdoptionService의 list/get/setAdopted는 FE 표시 port이다. API SPEC의 미합의 wire DTO를 확정한 것이 아니다. 프로덕션에는 Mock을 등록하지 않는다.
- FE2 PostCard/PostDetail과 summary/reaction/comment slot을 소비한다. 주민 의견/답글은 읽기 전용이며 주민 참여 권한을 부여하지 않는다.
- publicAdoptions/publicPost는 기관명·채택 시각만 반환한다. 취소는 현재 표시/본 기관 목록에서만 관계를 제거하고 다른 기관은 유지한다. Mock 취소 이력은 별도로 보존한다.
- 계정 변경 시 store를 새로 생성하고 자격/권한 변경 및 dispose 시 요청을 무효화한다. 실패/Retry 시 관계와 목록 조건/scroll은 보존한다. 채택 후 자동 주민 참여/알림/후속 행정 처리를 실행하지 않는다.

## 디자인 / 확인

Figma 최종디자인의 `1160:5995`, `1160:6063`, `1160:6134`, `1160:6189`, `1164:6182`, `1164:6520`의 design context와 screenshot을 직접 확인했다. 웹 화면은 기존 MobileLayout/Header/PostCard/PostDetail/Button/Notice와 디자인 토큰을 사용하며 Figma의 기기 OS 상태 바는 웹에 중복 구현하지 않는다. 새 전역 토큰/의존성/FE2 파일 변경은 없다.

개발 전용 `/dev/officer-agendas-preview`에서 정상/빈/지연/조회 실패/저장 실패/삭제/만료/미자격을 재현한다. `다음 재시도 성공`은 같은 Mock 서비스의 다음 호출을 정상으로 바꾼다. Production에서는 notFound 처리한다.

기존 Node 테스트 방식으로 권한·본 기관 취소·타 기관 유지·주민 의견·공개 projection·실패/Retry·중복 클릭·늦은 응답·필터/scroll 값을 검증한다. 브라우저 클릭/네이티브 뒤로가기/396px 시각 비교는 자동 테스트만으로 완료 처리하지 않는다.

## 남은 실제 연결

실 API 연동 대기: #12/#28/#29 및 #15/#22/#23의 실제 기관 자격/원본/댓글·반응과 #75 계정. #65에서 실제 저장/재조회/동시성/권한 실패와 #53 공개 상세의 동일 관계 갱신을 검증한다. 일반 공개 상세의 root 조립은 FE2 Owner가 이 port를 소비해야 하며 이번 작업에서 해당 Page를 재작성하지 않았다. 실제 cursor/동률 보조 정렬·기관/관계 wire ID·오류 매핑은 합의된 Adapter에서 처리한다.

FE UI/Mock 코드는 구현했으며 실제 Provider/API/Integration 완료를 주장하지 않는다. 실제 사용한 lint/typecheck/Node test/build/diff 검사 결과와 브라우저 검증 여부는 작업 보고에 기록한다.
