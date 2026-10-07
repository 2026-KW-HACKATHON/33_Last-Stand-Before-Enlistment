# Discushion Frontend

Issue #37 owns the execution environment only (SH-14 / Phase 0). The root route is deliberately empty. The current repository also contains common UI/design tokens from #38 and API Client/Mock/Provider foundations from #39; reuse those in their owning issues. Product screens and Router/permission guards are outside #37.

## Fixed Toolchain

| Tool | Version |
| --- | --- |
| Node.js (24 LTS) / npm | 24.21.0 / 11.19.0 |
| Next.js (App Router) | 16.3.8 |
| React / React DOM | 19.3.0 |
| TypeScript | 5.9.3 |
| Tailwind CSS / PostCSS plugin | 4.3.3 |
| PostCSS | 8.5.29 |
| ESLint / Next.js config | 9.39.5 / 16.3.8 |

Dependencies use exact versions. Keep `package-lock.json` with dependency changes; use npm only. `.nvmrc`, `engines`, `packageManager` and `.npmrc` define the same runtime baseline. Install Node with the official installer or your existing version manager.

Next.js supports Node >=20.9 and TypeScript >=5.1. Its React peer range supports React 19, and React DOM matches React exactly. TypeScript 5.9.3 satisfies typescript-eslint's supported range (>=4.8.4 <6.1.0), unlike TypeScript 7. Only stable published packages are declared; no experimental Next.js options are enabled. App Router internally bundles framework-managed React canary code as documented by Next.js; no canary/RC React package is installed directly.

Official references checked 2026-10-07:
- [Next.js installation and compatibility](https://nextjs.org/docs/app/getting-started/installation)
- [Next.js ESLint configuration](https://nextjs.org/docs/app/api-reference/config/eslint)
- [Tailwind CSS with Next.js](https://tailwindcss.com/docs/installation/framework-guides/nextjs)
- [Node.js release lifecycle](https://nodejs.org/en/about/previous-releases)
- [Selected Node.js release](https://nodejs.org/dist/v24.21.0/)
- [Published typescript-eslint peer requirements](https://registry.npmjs.org/typescript-eslint/latest)

## Local Execution

Run from `frontend/` using the fixed Node/npm versions:

```sh
npm install
npm run dev
```

Open http://localhost:3000. The empty document is intentional, not a completed product UI. If that port is occupied, use `npm run dev -- --port 3001`. Use `npm ci` for a clean lockfile-based installation.

On Windows PowerShell, if execution policy blocks `npm.ps1`, use `npm.cmd` for these commands without changing the policy.

```sh
npm run lint
npm run typecheck
npm run build
npm run start
```

`typecheck` generates route types before TypeScript, including on a clean checkout. Next.js 16 build does not run ESLint; lint is a separate check. There is no npm `test` script or additional test framework dependency. API regression tests exist in `src/lib/api/__tests__/client.test.ts`; run the existing TypeScript compiler + Node built-in test runner command in [the API README](src/lib/api/README.md#검증). These tests use injected transport/Mock and do not verify actual Backend integration or product UI flows.

## Environment Variables

The empty root route needs no variables or Backend connection. The current `.env.example` includes optional `NEXT_PUBLIC_API_BASE_URL` for #39's API Client; an explicitly supplied `baseUrl` takes precedence. Configure the full agreed API prefix only when using the real Client. The actual API address/CORS remain #30 and related Integration work; do not guess a URL or prefix. Agreed local values go in ignored `frontend/.env.local`; restart the server after changes. Never commit secrets. `NEXT_PUBLIC_` variables are browser-visible and must not contain secrets. Authentication and domain Mock contracts remain in their owning issues.

## Current Structure

- `src/app/layout.tsx`: mandatory HTML shell and stylesheet import.
- `src/app/page.tsx`: empty root route for execution verification only.
- `src/app/globals.css`: Tailwind import, design tokens and Pretendard font setup owned by #38; font/license assets are in `public/fonts/`.
- `src/components/ui/` and `src/components/layout/`: existing #38 common UI/layout foundations; see [the component README](src/components/README.md). Product screen integration and remaining UI changes belong to #38 and feature issues.
- `src/lib/api/`: existing #39 Client, error/decoder/types, Mock, Provider and regression tests; see [the API README](src/lib/api/README.md). The empty root route does not mount the Provider or connect to an API.
- Next.js, TypeScript, PostCSS and ESLint configs: toolchain configuration.
- `next-env.d.ts`, `.next/`, `*.tsbuildinfo`: generated and ignored.

Do not pre-create future feature directories or implement later issues here.

## CI and Collaboration

Known tooling limitations: ESLint 9 is deprecated upstream, but the React/import/accessibility plugins bundled by eslint-config-next 16.3.8 do not yet declare ESLint 10 compatibility. Keep 9.39.5 until that combination supports 10. npm audit currently reports five high-severity entries from one braces stack-exhaustion advisory in the development-only Next.js lint dependency chain. No compatible fixed braces release is available; do not force-downgrade Next.js lint configuration to 14 or hide the advisory. Recheck before dependency upgrades. Production dependencies are audited separately.

`.github/workflows/frontend.yml` runs `npm ci`, lint, typecheck and build for frontend PRs targeting `front/develop` or manual dispatch, with the same Node/npm versions. It does not run the API regression tests. No deployment is configured. Remote workflow execution and required branch checks must be verified on GitHub separately; local success is not remote CI success.

Use latest `origin/front/develop` and `front/feature/<issue-number>-<short-name>`. Do not directly commit/push protected branches. FE1 owns this environment; coordinate dependency/lockfile/CI changes with FE2 and execution impacts with BE2. Their feature code is outside this issue.

## Current Issue #37 Revalidation

Revalidated on Windows on 2026-10-07 using Node.js 24.21.0/npm 11.19.0, after fast-forwarding the existing `front/feature/37-project-environment` branch to the fetched `origin/front/develop` (`53aee05` at validation time). Only this README required updates; #38/#39 code, environment example, dependencies, lockfile and CI configuration were preserved.

| Check | Result |
| --- | --- |
| `npm.cmd ci` | Passed; existing ESLint deprecation, five High development-dependency audit entries and unrs-resolver install-script approval warning reported |
| `npm.cmd run lint` / `npm.cmd run typecheck` / `npm.cmd run build` | Passed |
| API README's TypeScript compiler + `node --test` command | Passed, 30 tests; output generated outside the repository |
| `npm.cmd run dev -- --hostname 127.0.0.1 --port 3001` | Ready; root HTML and stylesheet returned HTTP 200; server stopped after verification |
| Toolchain declarations vs lockfile | Matched; no dependency or version changes |

There is still no npm `test` script. Product UI/Figma checks are N/A for this environment issue. Actual Backend/API/Provider integration and remote CI were not executed. No new commit, push, PR, Issue close or deployment was performed.

## Previous Issue #37 Revalidation Record

The following record describes the earlier environment-only validation, before the #38/#39 foundations were integrated. Its machine path, checks and exclusions are historical, not instructions for the current checkout; use the current sections above for execution and test guidance.

Rechecked on Windows on 2026-10-07 from `front/feature/37-project-environment`, after rereading the root AGENTS.md and the entire issue. Existing files were reviewed and preserved. The branch starts at the fetched `origin/front/develop` commit. The repository is in `C:\Users\허예은\Documents\New project`, not the empty `discushion` project directory.

Validation used a separate official Node.js 24.21.0 runtime with npm 11.19.0; the system-wide Node installation was not changed. Select these versions before running the commands yourself; `engine-strict` intentionally rejects a different runtime.

| Check | Result |
| --- | --- |
| `npm install` | Passed; known development-tool audit warnings remain |
| `npm run lint` | Passed with zero lint warnings |
| `npm run typecheck` | Passed |
| `npm run build` | Passed |
| `npm run dev -- --hostname 127.0.0.1 --port 3001` | Ready; root HTML and stylesheet both returned HTTP 200 |
| Exact direct dependency versions vs lockfile | Matched; no prerelease direct versions |
| `npm audit --omit=dev` | Zero vulnerabilities |

Product tests, UI/Figma comparison, Mock/API integration and remote GitHub CI were not performed. They are not implied by the environment checks. No commit, push, PR, merge or deployment was performed.
