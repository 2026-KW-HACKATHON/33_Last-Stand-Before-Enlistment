# #49 북마크 · F-FYQJPT (D-02 / SH-13, FE1)

기준 develop: `df7b4003454f385d62d9d953f38561ade5a0f599`.

- I02 `/me/bookmarks`: 유형 4개 / 주제 8개, 각 3열. I01과 L01의 기존 메뉴에서 진입하고 원 화면으로 돌아간다. 설정은 기존 논리 목적지를 사용하며 URL을 새로 만들지 않는다.
- `BookmarkService`는 FE1의 조회/등록/해제 port다. FE2 `PostDisplayModel`과 기존 `BookmarkBinding`을 소비한다. API DTO/Endpoint를 새로 확정하지 않았다.
- `BookmarksProvider`는 회원/계정별로 store를 분리하고 로그아웃·계정 변경 시 요청을 폐기한다. `AppProviders`의 `bookmarkService`와 `renderBookmarkDetail`로 주입한다. production에 Mock을 자동 등록하지 않는다.
- FE2는 `useBookmarkBinding(postId, originalNavigationEntry)`로 기존 상세 단일 버튼에 상태를 연결할 수 있다. 게스트는 저장 없이 인증으로 이동하며 일반/공유 상세 returnTo를 기존 Navigation에 전달한다. 인증 후 자동 저장하지 않는다.
- 목록 카드에는 저장/해제 버튼이 없다. 원 상세 renderer에 `postId`, `bookmark`, `onReturn`을 전달한다. `BookmarkDetailBridge`는 조회 Loading/Error/Retry/삭제 안내와 저장 성공 팝업을 조립한다. 동일 Service로 상세 변경 후 목록을 재조회한다.
- 삭제/접근 불가 원본은 목록에 남기지 않는다. Adapter도 접근 가능한 원본만 list로 전달해야 한다. 목록 조회 실패 시 오래된 본문을 노출하지 않는다. 변경 실패 시 기존 북마크 상태를 유지한다.
- 유형·주제와 scroll은 Provider에서 유지한다. 상세에서 복귀하거나 재시도해도 보존하고 필터 변경 때만 scroll을 초기화한다.

## 개발 확인

`/dev/bookmarks-preview`는 development에서만 접근 가능하다. 기존 FE2 `PostDetail`의 단일 버튼을 그대로 사용하며 목록·상세가 하나의 Mock Service/원본 registry를 공유한다.

- `member`: 이웃/기관 grant 없이 북마크 가능. `other`: 별도 계정의 빈 목록. `guest` / `incomplete`: 회원 전용 목록 제한.
- 조회/변경 상태를 `error`, `mutation-error`, `empty`로 바꿔 Loading, 실패, Retry, 빈 목록을 확인한다. 정상으로 바꾸고 재시도하거나 상세에서 복귀한다.
- 상세에서 북마크 해제 → 재저장 → 확인: 단일 버튼 상태, 성공 팝업, 목록 동기화. 원본 삭제 후 복귀: 목록에서 제거.
- I01 진입 → 필터 선택 → scroll → 상세 → 복귀 → 뒤로가기. L01의 `스크랩한 글`에서 같은 흐름을 확인한다.

## 근거와 대기

Figma `최종디자인`의 `1160:4653`(I02), `1164:6588`(유형), `1164:7050`(주제)을 직접 확인했다. 다른 유형/주제 Frame은 같은 필터 구조를 사용한다. 기존 Header/BottomNavigation/Notice/Button/PostCard를 재사용하며 FE2 파일이나 전역 토큰을 변경하지 않는다.

자동 검증: 기존 TypeScript → CommonJS 임시 출력 → `node --test` 방식의 store/SSR 테스트. 396×852px 브라우저 시각 비교 및 클릭/scroll 인수는 별도 대기다.

**실 API 연동 대기**: Backend #26/#15/#16/#5, 상세 production 연결 #53/#63, 실제 목록/원본 정합성 #66. Mock 성공은 실제 저장이나 Integration 완료가 아니다. 실제 Privy Adapter, 활동 집계, 다른 개인 목록·투표 기능은 이 Issue에서 구현하지 않는다.
