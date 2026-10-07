# Issue #38 common UI

FE1 owns these shared components. Tokens live only in `src/app/globals.css`
using Tailwind v4 `@theme`. Import components directly from their files.
No package, route, provider, API or root page was added.

```tsx
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { TextArea } from "@/components/ui/TextArea";
import { Notice } from "@/components/ui/Notice";
import { Header } from "@/components/layout/Header";
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
- Header: string `title`, optional `onBack`, optional `rightAction` node. Back
  navigation is supplied by the page; no implicit browser history behavior.
- BottomNavigation: only main/map/write/my. `activeItem` supplies selection.
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

## Design sources and remaining integration

Figma file `Wobmrjd8xSUNKQISplV5aR`, final design page:
Header/Back `1155:1005`, common UI `1155:950`, navigation `1255:6597`.
Header, Button, Input, TextArea and Notice were read directly from Figma.
The four navigation SVGs are unchanged downloaded Figma assets. General icons
use CSS masks to follow the selected text color; the map retains its original
24 × 22.7654 aspect ratio centered in a 24px slot. Write remains a 40px SVG.
No notification assets are included.

TODO: Confirm the four-menu spacing with design. The Figma original has five
slots; this MVP uses four flexible slots without treating their widths as a new
design policy. The write menu has an accessible "작성" label, visually hidden
as in the original circular action. Confirm product routes in their own issues
and inject destinations/activeItem there. Shared guest pages omit navigation.
Toast/EmptyState/modal specifications remain outside this minimal foundation.

Pretendard Variable is self-hosted in `public/fonts` with its OFL license.
Source: https://github.com/orioncactus/pretendard/blob/main/packages/pretendard/dist/web/variable/woff2/PretendardVariable.woff2
