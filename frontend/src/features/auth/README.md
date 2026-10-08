# #41 — Privy email OTP / local membership / returnTo

FE1 owns this feature and its start/login Pages. It reuses #38 UI and #40
Navigation/Session. It does not implement #42 consent/signup, #43 profile/region,
FE2 home/share/detail Pages, a password form, an OTP server or a Privy SDK adapter.

## Service and state boundary

`OtpLoginService` is a frontend port, not an Endpoint/DTO declaration. Inject one
stable service via `AppProviders.loginService`. Without it, production login
reports connection unavailable and never reports authentication success.
`sendCode` and `verifyCode` project the Provider result; `resolveSession` separately
confirms local membership and return target availability. Provider success alone
never means local signup or region/institution qualification is complete.
The member Mock has no capability grants. The feature never creates API requests
or chooses a base URL, Bearer header, cookie, token format or session lifetime.
The real adapter must validate/map actual responses through #39 where applicable.

Official Privy reference checked 2026-10-07:
https://docs.privy.io/authentication/user-authentication/login-methods/email
The documented `useLoginWithEmail` operations are `sendCode` and `loginWithCode`.
Its `isNewUser` callback describes Privy, not Discushion local signup completion.
No fixed OTP length, TTL, resend interval or attempt limit is inferred from this
page. The input accepts a nonempty Provider code; expiration/rate-limit/retryable
states come from the adapter. Mock delay and arbitrary input are test mechanics,
not Provider policies. #74/#61 must confirm actual app settings and limitations.

Email survives navigation/cancellation/retry in per-root memory. OTP is only in
the mounted input state and is not saved, logged or copied into snapshots. No
credentials are written to localStorage/sessionStorage or fixtures. Cancellation
aborts operations and invalidates late results; real SDK cancellation/session
cleanup semantics must be checked in #61. Unexpected errors use generic copy
without account-existence/provider diagnostics.

## Navigation and incomplete destinations

Use existing `useNavigation().beginAuthentication(entry)` before navigating from
shared/member-only actions. #40 owns target/origin/sharedContextRef, and #41 reads
its existing in-memory returnTo. No URL/query/returnTo serialization is added.
Mock/full reload persistence is not claimed; #64 validates the real journey.
Success hands off through `completeAuthentication` only: member → original post
or `/home`; signup-incomplete → `/signup` while retaining returnTo. Deleted target
uses #40's unavailable/home fallback. There is no reaction/vote/bookmark replay.
Root SessionProvider remains controlled by #61; this feature does not overwrite
real session data with a Mock. #42 can consume Provider verification/local state
at this same boundary; no signup Page or feature implementation is added here.

Registered home/signup/post paths do not imply those Pages exist. Production has
no configured adapter so it cannot claim those journeys are integrated. The
development harness displays intended destinations without reimplementing other
owners' screens.

## Development UI / fixtures

`/dev/login-preview` is gated with the existing NODE_ENV=development pattern.
Mock is imported only there and in tests. The page consumes fixtures from
`mock.ts`, never embeds account/OTP data. Choose a scenario and optional shared
origin. Enter an example email and any nonempty fake code. It explicitly states
no email is sent and no real authentication runs. Scenario reset aborts the prior
store. Existing-member/new-member, send/verify/session failure with retry,
expiration with resend, Provider cancellation/rate limit, target error/deletion
are available. This is not Mock API/real server registration.

## Design

Directly read Figma final frames `1157:905`, `1157:962`, `1157:1044`, `1162:7519`.
https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1157-905
The obsolete password/find-password controls are replaced per current product
policy with email/code/provider feedback. This is not a claim that Figma already
contains an OTP-specific screen. Common Header/Input/Button/Notice spacing is
reused. No fabricated system clock/status bar or notification navigation is shown.
The A01 image is downloaded unchanged as `public/images/start.png`, 336 × 224
design slot, scaled only for narrow screens. `start.module.css` holds screen-only
geometry, including the documented Inter 16px bold slogan exception. The local
Inter font/license came from https://github.com/rsms/inter/tree/master/docs/font-files
and https://github.com/rsms/inter/blob/master/LICENSE.txt. Global tokens are intact.

## Checks

Run existing npm lint/typecheck/build scripts. There is no npm test script.
Use the existing TypeScript compiler + Node built-in test pattern:

```powershell
$loginTestBuild = Join-Path $env:TEMP 'discushion-41-tests'
& .\node_modules\.bin\tsc.cmd --target es2023 --lib esnext,dom,dom.iterable --module commonjs --moduleResolution node --jsx react-jsx --strict --skipLibCheck --esModuleInterop --types node --rootDir src --outDir $loginTestBuild src/features/auth/__tests__/login.test.ts src/lib/navigation/__tests__/navigation.test.tsx src/lib/api/__tests__/client.test.ts
$env:NODE_PATH = (Resolve-Path node_modules).Path
node --test "$loginTestBuild/features/auth/__tests__/login.test.js" "$loginTestBuild/lib/navigation/__tests__/navigation.test.js" "$loginTestBuild/lib/api/__tests__/client.test.js"
```

Real OTP delivery, Privy verification, local DB membership, token refresh/CORS and
production target availability remain waiting for #74/#8/#4/#7/#21 and #61/#64.
#68 aggregates actual integration evidence; Mock success is not that evidence.
