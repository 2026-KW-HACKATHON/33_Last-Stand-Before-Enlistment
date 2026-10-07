# Issue #39: 공통 데이터 접근 기반

담당: FE1 (#39). 공통 계약 변경은 소비 FE·기능 BE와 검토하고 BE1이 정합성을 확인한다.
`src/app`, UI, 디자인 토큰, 패키지/lockfile 및 Backend는 변경하지 않는다.

## 확인한 계약과 보류 항목

2026-10-07 `origin/front/develop` **42e3061**에는 실행 환경만 있고 API Client/DTO/Provider/Mock이 없다.
GitHub #39·#2·#4·#30·#74 본문과 댓글, PRD/기능명세 v10.2, API SPEC v1.1.2 및
`origin/back/develop`의 계약 검토표와 실제 코드를 확인했다. #2는 closed이고 #4/#30/#74는 open이다.
#2 완료는 새 인증/파일 계약 합의나 서버 구현 완료를 뜻하지 않는다.

| 항목 | 근거와 구현 상태 |
| --- | --- |
| 응답 | 실제 Backend `HealthController`의 `/health`는 `{ data: { status: "UP" } }`. `ApiResponse<T>`/`decodeApiResponse`는 이처럼 data envelope가 확인된 endpoint에서만 선택 사용한다. Client가 모든 응답을 강제로 unwrap하지 않는다. |
| 목록 meta·ID·Enum·도메인 DTO | API SPEC §1·2와 검토표 C02/C05/C07/C09/C10은 설계 제안/기존안이다. FE/BE 합의 완료 증거가 없어 구체 타입·페이지 크기·nullable을 확정하지 않았다. TODO(BE #2). |
| Error | HTTP 상태는 FE 내부 `ApiError.kind`로 분류한다. wire 오류 DTO를 확정하지 않는다. 원본 `body: unknown`, status/headers를 보존하고 `mapError`로 합의된 code/details/traceId/안내 문구를 주입한다. 기본값은 서버 내부 오류 메시지를 표시하지 않는다. |
| Auth | Privy OTP 선택은 제품 확정. 직접 토큰/자체 세션·보관·갱신·로컬 회원/가입 완료·권한 사유 코드는 #74/BE #4 합의 대기. `prepareRequest` 경계만 제공한다. SDK·Bearer·세션 쿠키·자동 refresh/로그인 이동은 구현하지 않았다. |
| 권한 | F-TSOXGG/F-ATWJDJ/F-OPNIXL의 로그인·가입 완료·이웃 완료 지역·유효 기관/담당 지역은 독립적이다. 401/403을 구분하며 세부 reason은 원본/mapper로 전달한다. `isVerified`로 축약하거나 403만으로 가입 미완료를 추측하지 않는다. |
| 파일 | TODO(#74, BE #30): 업로드 endpoint·소유권·최종 파일 참조/nullable 계약 대기. 파일 DTO/업로드 서비스는 만들지 않았다. 일반 `BodyInit` 전달은 가능하다. |
| API 주소 | TODO(BE #30): 배포 주소/CORS 합의 대기. `NEXT_PUBLIC_API_BASE_URL` 또는 명시적 `baseUrl`로 전체 prefix를 주입한다. `/api/v1`, localhost나 배포 URL을 기본값으로 고정하지 않는다. |

`back/develop`에는 HealthController와 실행 환경만 있으며 공통 인증/기능 API 구현은 없다.
따라서 이 변경은 실 API 연동 완료나 Integration 검증 완료가 아니다. 제품 정본/기술 계약 문서는 수정하지 않았다.

## 사용 경계

```text
Page / Component → 도메인 Service(ApiClient 주입)
                              ↓
                  createApiClient / createMockApiClient
                              ↓
                 같은 요청·decoder·ApiError 경계
```

실 API Client는 `@/lib/api`에서, 개발용 Mock은 별도 `@/lib/api/mock`에서 가져온다.
Client Component에 필요하면 `@/lib/api/provider`의 `ApiClientProvider`/`useApiClient`를 사용한다.
Provider는 주입된 인스턴스만 전달하며 source를 선택하지 않는다. 도메인 Service에서 Client를 소비하고
화면은 Service의 DTO를 사용한다. 서버 Service는 React Context 없이 `ApiClient`를 직접 주입한다.
인스턴스는 조립 지점에서 안정적으로 유지하되 서버의 회원별 인증 상태를 전역 singleton에 저장하지 않는다.

현재 빈 루트 화면에는 Provider나 Mock을 연결하지 않았다. 후속 기능의 합의된 Service/DTO를 연결할 때
조립 지점에서만 구현체를 교체한다. 화면 안의 fixture와 Mock/API 조건문은 추가하지 않는다.

아래는 실제 Backend에 존재하는 Health 응답의 사용 예시이며 새 제품 API 계약이 아니다.

```ts
import { createApiClient, decodeApiResponse, type ApiClient } from "@/lib/api";

const decodeHealth = decodeApiResponse((value) => {
  if (typeof value !== "object" || value === null || !("status" in value) || value.status !== "UP") {
    throw new Error("Invalid health response");
  }
  return { status: value.status };
});

// 동일 Service를 API/Mock Client로 검증할 수 있다.
const createHealthService = (client: ApiClient) => ({
  getStatus: (signal?: AbortSignal) => client.request("health", { decode: decodeHealth, signal }),
});
const service = createHealthService(createApiClient()); // 합의한 base URL 필요
```

개발 검증에서는 `createMockApiClient({ routes: [{ method: "GET", path: "/health",
respond: () => Response.json({ data: { status: "UP" } }) }], delayMs: 200 })`를 동일 Service에 주입한다.
Mock은 기본 fixture·자동 활성화·실 fetch fallback을 제공하지 않는다. `NODE_ENV=production`에서는
생성 자체가 실패한다. Mock 모듈은 제품용 barrel이나 app에서 import하지 않는다.
내부 `.invalid` 주소는 Request 구성용이며 네트워크 요청이나 Backend 주소가 아니다.

`respond(request)`에서 요청/쿼리를 검사하고 새로운 `Response`를 반환한다. 성공/빈 목록/도메인 pending은
합의된 body로, Forbidden/Unavailable은 403/404로, Network Error는 reject로 표현할 수 있다.
`delayMs`와 비동기 handler로 Loading을 검증하며 AbortSignal로 지연/handler 대기를 취소한다.
가입/기관 접수 상태를 임의 fixture 계약으로 만들지 않는다. 테스트의 `test.*` 사유와 `fixtureState`는 검증 전용이다.

## 요청·응답·오류 규칙

- `request<T>`는 필수 `decode`의 반환 타입에서 T를 추론한다. JSON을 `as T`로 무검증 반환하지 않는다.
  성공 JSON shape는 endpoint decoder가 검증한다. 실패 시 `invalid-response`다.
- `json`은 JSON 직렬화를, `body`는 원본 BodyInit을 전달한다. 둘을 함께 사용하지 않는다.
  FormData의 Content-Type은 브라우저가 boundary와 함께 만든다. 취소 Signal과 요청 헤더를 전달할 수 있다.
- 무본문(204/205/HEAD/빈 body)은 `undefined`, JSON `null`은 `null`이다. `decodeNoContent`는 JSON null을 거부한다.
  `decodeApiResponse`는 data가 없을 때 실패하며 null data 허용 여부는 하위 decoder가 판단한다.
- nullable 필드는 합의에 따라 `field: T | null`, 생략 가능 필드는 `field?: T`로 작성한다.
  필수 nullable의 누락과 optional의 null을 혼동하지 않는다. 빈 배열/생략/null을 자동 치환하지 않는다.
  JSON 요청의 undefined 속성 생략은 JSON.stringify의 동작이다. 미정 DTO는 새로 정의하지 않는다.
- 기본 credentials는 omit, cache는 no-store, redirect는 error다. 인증/공유 헤더를 자동으로 만들지 않는다.
  `prepareRequest`는 매 요청 실행되고 headers/credentials만 주입한다. 전달받은 url/method/signal로 범위를 판단한다.
  공유 컨텍스트를 사용할 때는 합의된 대상 요청에만 주입한다. 요청 URL은 설정한 base origin/prefix를 벗어날 수 없다.
- `ApiError.kind`: configuration/network/cancelled/invalid-response, 400 validation, 401 unauthorized,
  403 forbidden, 404 unavailable, 409 conflict, 413 payload-too-large, 415 unsupported-media-type,
  422 domain-validation, 429 rate-limit, 5xx server, 나머지 http.
  깨진 JSON/HTML 오류 및 mapper 실패도 HTTP 분류를 유지한다. Retry-After 등은 error.headers에서 읽는다.
- `mapError`의 code/details/traceId는 클라이언트 내부 메타데이터이며 서버 필드/Enum을 선언하는 것이 아니다.
  실제 오류 shape와 표시 문구를 합의한 후 mapper를 작성한다. 원본 body/cause를 그대로 UI에 출력하지 않는다.
- 자동 재시도·token refresh·로그인 리다이렉트·권한 부여·캐시는 없다. POST가 실패해도 자동 반복하지 않는다.

## 검증

기존 package.json의 lint/typecheck/build를 실행한다. **test script/runner 의존성은 추가하지 않았다.**
새 데이터 경계의 회귀 테스트는 기존 TypeScript compiler와 Node 내장 test runner로 독립 실행할 수 있다.
PowerShell에서 frontend를 현재 디렉터리로 실행한다. 출력은 저장소 밖 임시 폴더에만 생성된다.

```powershell
$apiTestBuild = Join-Path ([System.IO.Path]::GetTempPath()) ('discushion-39-api-tests-' + [guid]::NewGuid().ToString('N'))
& .\node_modules\.bin\tsc.cmd --target es2022 --lib esnext,dom,dom.iterable --module commonjs --moduleResolution node --strict --skipLibCheck --esModuleInterop --types node --outDir $apiTestBuild src/lib/api/__tests__/client.test.ts
if ($LASTEXITCODE -eq 0) { node --test (Join-Path $apiTestBuild '__tests__/client.test.js') }
```

테스트는 주입한 transport/Mock을 사용한다. 실제 Spring API·Privy·업로드·CORS·전체 사용자 흐름을 검증하지 않는다.
UI/Figma 비교는 대상 화면이 없어 적용하지 않는다. 후속 #74/#4/#30 계약 반영과 실제 기능 Integration이 필요하다.
