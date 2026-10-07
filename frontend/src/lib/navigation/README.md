# #40 Router / Navigation 계약

이 디렉터리는 FE1 소유 공통 기반이다. 제품 Page, Privy SDK, Backend DTO·권한 판정, 실제 인증 연동은 포함하지 않는다. Route는 FE 상세지침서 §8의 현재 URL을 사용하며, Registry 등록은 Page 구현 완료를 의미하지 않는다.

## 소비 경계

- `routes.ts`: 내부 Destination, 공개/회원/공유 접근 정책, 필수 capability, fallback, Header와 BottomNavigation 목적지. URL 확인 필요 항목은 `path: null`이다.
- `guard.ts`: FE 내부 Session/Capabilities 상태와 순수 Guard 판정. 서버가 최종 권한자다.
- `session.tsx`: 제어형 Session 주입과 AccessGuard. Adapter 없는 Root는 `loading`을 유지한다.
- `state.ts`: Provider마다 분리된 메모리 Store. 이동, origin, returnTo, snapshot 참조만 관리한다.
- `context.tsx`: Store의 React 소비 경계. 실제 이동은 주입한 Next App Router 함수로 전달한다.
- `app/providers.tsx`: Root 조립 지점. 실제 client가 주입될 때만 기존 #39 ApiClientProvider를 사용한다. API URL/토큰/Mock client를 생성하지 않는다.

## Route와 권한

`resolveDestination({ id: "post", params: { postId } })`는 내부 동적 경로를 생성한다. postId는 URL 한 segment를 위한 불투명 문자열이며 서버 ID 형식을 새로 정하지 않는다. 외부 URL 문자열, 미등록 identity, 경로 주입 parameter는 거부한다. `destinationFromPathname`은 등록된 pathname만 인식하며 query/returnTo 직렬화 계약은 추가하지 않는다.

검색·추천·새 추천·알림·관심 지역·관심 키워드·설정·계정·이메일 변경·알림 수신 설정·탈퇴·탈퇴 완료는 논리 목적지를 제공한다. URL 미확정 목적지 이동은 `unresolved`를 반환하고 현재 화면을 유지한다. 소비자는 이를 누락·접근 불가로 바꾸지 말고 기능 Owner의 Route 확정 후 중앙 Registry에 연결한다. 로그아웃 복귀는 start, 탈퇴 후 복귀는 login이다. 증빙 접수 Route는 없다.

Session은 `loading / guest / signup-incomplete / member / error`를 구분한다. Privy 인증 성공만으로 member로 바꾸지 않는다. 공개 화면은 Session 없이 열 수 있지만, 회원 화면은 Session loading/error 중 콘텐츠를 노출하지 않는다.

필수 권한은 다음처럼 분리한다.

- 새 글: 선택 지역의 `neighbor-region`.
- 수정: 해당 게시물의 `edit-post` (소유권·지역 등 실제 판정은 Adapter/Backend 책임).
- 기관 목록/상세 읽기: `institution`. 다른 담당 지역이라는 이유만으로 읽기를 차단하지 않는다.
- 담당 지역 행동: 소비자가 해당 지역의 `responsible-region`을 requirements로 추가한다.

`Capability`는 서버 DTO/Enum이 아닌 FE Adapter의 scoped assertion이다. role/badge/isVerified를 조합해 만들지 않는다. 필요한 grant가 없거나 중복되면 재조회 오류, 명시적으로 false이면 forbidden이다. 필요하지 않은 권한 조회를 일반 회원 화면의 선행조건으로 두지 않는다.

공유 Guard는 Session 확인과 특정 postId의 검증된 SharedAccess assertion을 요구한다. guest의 일반 상세·참여 권한은 부여하지 않는다. 공유 검증은 #60/#64/#61에서 제공하며, raw share token은 NavigationContext에 넣지 않는다. Session 오류를 공유 guest로 자동 강등하지 않는다.

```tsx
const navigation = useNavigation();
<AccessGuard destination={destination} fallback={(result, retry) => {
  // 상태 UI는 각 Page Owner가 구현한다. 렌더 중 이동하지 않는다.
  // login-required: 사용자 이벤트에서 navigation.beginAuthentication({ destination, origin })
  // signup-required: 동일 계약의 두 번째 인자로 "signup" 지정
  // error: 사용자 Retry 이벤트에서 주입된 retry 호출
  return renderAccessState(result, retry);
}}>
  {children}
</AccessGuard>
```

Guard 자체는 자동 redirect하지 않는다. 로그인/가입 진입이 필요하면 fallback의 이벤트 또는 Page의 명시적 전환 처리에서 `beginAuthentication`을 호출한다. 로그인/가입 목적지만 직접 navigate하면 원 목적지 보존을 대신 수행하지 않는다.

## returnTo와 origin

`beginAuthentication(entry?, step?)`는 최초 원 목적지를 한 번 저장한다. 재시도, 로그인↔가입 이동, 인증 오류는 이를 덮어쓰거나 소비하지 않는다. `completeAuthentication(session, availability)`는 가입 완료 및 원본 접근 가능 여부를 소비자가 확인한 뒤 호출한다. loading/error이면 대기하며 incomplete이면 signup으로 이동한다. 완료 후 이동한 Page는 Guard와 실제 API 권한을 다시 확인해야 한다.

공유 상세 target은 같은 postId의 일반 상세로 변환한다. 취소는 원 공유 entry와 비밀이 아닌 context 참조로 복귀한다. 일반 로그인 취소는 원 화면으로 복귀한다. 성공/취소는 인증 이동 이력을 정리한다. unavailable이면 `{ notice: "unavailable", destination: home }`을 반환하고 returnTo/이력을 제거한다. 소비자가 안내 UI를 표시한다. 자동 북마크·반응·투표·신고를 실행할 callback/pending action은 저장하지 않는다.

`navigate({ destination: { id: "bookmarks" }, origin: { id: "settings" } })`처럼 진입 Owner가 origin을 전달한다. I02/I03/I04의 `backDestination`/`back()`은 me에서 진입하면 me, settings에서 진입하면 settings를 반환한다. settings의 URL은 아직 미확정이므로 `back()`은 unresolved 의도를 반환하며 가짜 URL로 이동하지 않는다. 이력에 동일 목적지가 있으면 해당 entry를 복구한다. 직접 진입/새로고침으로 origin이 없으면 개인 목록 기본 복귀는 me다.

## 화면 상태 참조

`registerSnapshot({ destination, kind, ref })`와 `snapshotReference(state, destination, kind)`를 사용한다. kind는 list/search/map/form이며, ref는 feature 소유 저장소의 비밀이 아닌 불투명 키다. filter/topic/tab/cursor/scroll, 검색어·조건·결과 컨텍스트, 지도 중심·zoom·선택 동·말풍선, 작성/가입 draft **값**은 Page Owner가 관리한다. 화면 unmount 이후에도 필요한 값은 해당 feature의 상위 Provider 등에 보관해야 한다.

Destination와 kind별로 참조를 분리하고 없는 참조는 undefined로 반환한다. 완료/폐기 시 removeSnapshot, 로그아웃/계정 전환 시 clear를 호출한다. feature 저장소 값의 삭제도 각 Owner 책임이다. Navigation은 게시물/댓글/알림 데이터, 검색 결과, 폼 DTO, API cache, 토큰, 권한 정본을 저장하지 않는다. 현재 계약은 메모리만 사용하며 새로고침·탭 종료 후 복구나 URL 직렬화를 보장하지 않는다.

## 후속 작업

#41/#42는 인증·가입 UI/draft, #47/#49는 개인 목록과 snapshot 값, #60은 공유 화면, #61은 실제 Session/Capability Adapter·Privy·401/403, #64는 실제 인증 복귀, #67은 전체 Page의 공통 계약 소비, #68은 최종 Integration을 담당한다. 필요한 계약만 소비하여 병렬 착수할 수 있다. 실제 API URL/DTO/Provider 인증은 #74의 합의와 실제 Backend 구현을 확인하여 연결한다.

## 검증

새 dependency나 npm test script를 추가하지 않는다. 기존 TypeScript compiler + Node runner로 테스트한다 (frontend 폴더, PowerShell).

```powershell
$navigationTestBuild = Join-Path ([IO.Path]::GetTempPath()) ('discushion-40-tests-' + [guid]::NewGuid())
& .\node_modules\.bin\tsc.cmd --target es2023 --lib esnext,dom,dom.iterable --module commonjs --moduleResolution node --jsx react-jsx --strict --skipLibCheck --esModuleInterop --types node --outDir $navigationTestBuild src/lib/navigation/__tests__/navigation.test.tsx
if ($LASTEXITCODE -ne 0) { throw 'Navigation test compilation failed' }
$previousNodePath = $env:NODE_PATH
try {
  $env:NODE_PATH = Join-Path (Get-Location) 'node_modules'
  node --test (Join-Path $navigationTestBuild '__tests__/navigation.test.js')
} finally { $env:NODE_PATH = $previousNodePath }
```

검증 대상은 순수 계약 전이와 React 서버 렌더링이다. 실제 브라우저 이력·Privy·Backend·제품 Page 간 E2E 검증은 포함하지 않는다.
