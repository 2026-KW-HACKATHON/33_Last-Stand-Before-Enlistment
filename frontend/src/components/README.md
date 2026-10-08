# Issue #38 common UI

FE1 owns these shared components. Tokens live only in `src/app/globals.css`
using Tailwind v4 `@theme`. Import components directly from their files.
No package, route, provider, API or root page was added.

```tsx
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { TextArea } from "@/components/ui/TextArea";
import { Notice } from "@/components/ui/Notice";
import { Header, HeaderAction } from "@/components/layout/Header";
import { BottomNavigation } from "@/components/layout/BottomNavigation";
import { MobileLayout } from "@/components/layout/MobileLayout";
```

- Button: native button props/ref, primary or secondary, default `type="button"`.
  Pass `type="submit"` explicitly for submission. Native `disabled` is supported.
- Input/TextArea: required visible `label`, native input props/ref, generated or
  explicit `id`. `className` extends the outer field; native `style` applies to
  the control. Pass `aria-invalid` and `aria-describedby` for externally supplied
  validation messages. TextArea uses a 132px minimum and resizes vertically.
- Notice: caller-supplied children and optional info/warning/error tone. Native
  `role`/`aria-live` are available; callers decide whether a message is live.
  No timer, toast provider, feature message or business logic is included.
- Header: the existing default variant accepts string `title`, optional `onBack`
  and `rightAction`. `variant="brand"` renders the Figma logo/service name/tagline
  without a title/back button. Back navigation is supplied by the page; no
  implicit browser history behavior.
- HeaderAction: `action="settings"` (L01) or `action="notification"` (H01), an
  injected `destination` (Next Link) or `onAction` callback; no destination/callback
  or explicit `disabled` renders a disabled button. `variant="main"` uses the
  brand header's 32px action slot; default uses the general header's 56px slot.
  Settings/notification Pages and actual URLs are not implemented here.
- BottomNavigation: main/map/write/notification/my, in the order
  **메인 / 지도 / 글쓰기 / 알림 / 마이**. `activeItem` supplies selection.
  `destinations` supplies confirmed hrefs rendered as Next Link. Alternatively,
  `onNavigate` receives the menu id. Missing destinations without a callback are
  disabled buttons, never placeholder links. The empty `/` route is an environment
  check, not a main screen, so it is not assigned as a default destination.
- MobileLayout: optional `header` and `bottomNavigation` nodes, `children`, outer
  `className`, and `contentClassName`. Maximum width 396px, dynamic viewport height,
  independently scrolling main content, with non-overlapping header/navigation
  and safe-area padding. It is an application shell; do not nest it or add a
  second `<main>` inside it. Height is not fixed to the 852px Figma canvas.

Event handlers must be supplied from a Client Component. MobileLayout and Notice
can remain Server Components. Native refs use the existing React 19 prop pattern.

The brand Header composes the same action slot; consumers inject confirmed
destinations or callbacks rather than reproducing the Header:

```tsx
<Header variant="brand" rightAction={<>
  <HeaderAction action="settings" variant="main" onAction={openSettings} />
  <HeaderAction action="notification" variant="main" onAction={openNotifications} />
</>} />
```

Callbacks above are supplied by the consuming Client Component. Route/Guard,
origin/returnTo and persistent selection belong to #40 and the feature Page.

## Design sources and remaining integration

Figma file `Wobmrjd8xSUNKQISplV5aR`, final design page:
Header/Back `1155:1005`, Header/Main `1255:6584`, common UI `1155:950`,
navigation `1255:6597`; consuming reference Frames B01 `1159:1694` and
I01 `1160:4471`. Header/Main, Header/Back, navigation and both reference Frames
were read directly with screenshots during this #38 update. Existing Button,
Input, TextArea and Notice implementations are reused.
The existing four navigation SVGs are preserved; `notification.svg` is the
unchanged Figma asset from the notification slot `1255:6617`. General icons
use CSS masks to follow the selected text color; the map retains its original
24 × 22.7654 aspect ratio centered in a 24px slot. Write remains a 40px SVG.
Header assets `settings.svg`, `header-notification.svg` and `brand.png` are
unchanged downloads supplied by Figma Header/Main (settings `1255:6591`,
notification `1255:6594`, logo `1255:6587`). Header SVG sources use a 36px viewBox
and are displayed at the design's 32px size; the logo is displayed at 35 × 32.

The navigation uses five equal slots (72.8px at 396px width), 16px horizontal
padding, 76px height and an inside border. The write menu retains its 40px asset
and selected ring; its accessible name is "글쓰기", visually hidden as in Figma.
Confirm product routes in #40 and inject destinations/activeItem. Shared guest
Pages omit the optional `bottomNavigation` slot; no session logic is inside UI.
MobileLayout remains the sole owner of top/bottom safe-area padding.

No Chip/Modal/BottomSheet/Confirm/Loading/Empty/Error/Toast/Skeleton abstraction
was added: the current #38 difference is Header/navigation, and no product Page
consumer or agreed additional state props exists yet. Existing Notice handles
caller-supplied messages. These presentation components can be added when a
consumer establishes a concrete shared contract, without adding business logic.
Page consumption is #67, final Integration QA is #68, notification/settings
contents are #89/#91. Common UI is API-independent; callbacks/stub destinations
do not establish real API or complete-product Integration.

## Regression checks

`__tests__/layout.test.tsx` uses the existing TypeScript compiler, React server
renderer and Node built-in test runner. No dependency or npm `test` script was
added. From `frontend/` in PowerShell:

```powershell
$uiTestBuild = Join-Path ([System.IO.Path]::GetTempPath()) ('discushion-38-ui-tests-' + [guid]::NewGuid().ToString('N'))
& .\node_modules\.bin\tsc.cmd --target es2022 --lib esnext,dom,dom.iterable --module commonjs --moduleResolution node --jsx react-jsx --strict --skipLibCheck --esModuleInterop --types node --outDir $uiTestBuild src/components/__tests__/layout.test.tsx
if ($LASTEXITCODE -eq 0) {
  $previousNodePath = $env:NODE_PATH
  try {
    $env:NODE_PATH = Join-Path (Get-Location).Path 'node_modules'
    node --test (Join-Path $uiTestBuild '__tests__/layout.test.js')
  } finally { $env:NODE_PATH = $previousNodePath }
}
```

These checks verify rendered labels/anchors/ARIA, all five callbacks, active and
disabled states, Header back/actions/branding and optional Layout slots. Layout
checks cover rendered scroll/safe-area contracts, not browser geometry. Browser
keyboard/focus/scroll interaction, nonzero device safe-area and rendered pixel
comparison still require browser QA; Figma screenshots and source comparison
alone do not establish those results. Normal lint/typecheck/build remain separate.

Pretendard Variable is self-hosted in `public/fonts` with its OFL license.
Source: https://github.com/orioncactus/pretendard/blob/main/packages/pretendard/dist/web/variable/woff2/PretendardVariable.woff2
