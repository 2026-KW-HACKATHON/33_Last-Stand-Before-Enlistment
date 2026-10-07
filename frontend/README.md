# Discushion Frontend

Issue #37 initializes the execution environment only. The root route is deliberately empty. Product screens, design tokens, providers and permission guards belong to later issues.

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

```sh
npm run lint
npm run typecheck
npm run build
npm run start
```

`typecheck` generates route types before TypeScript, including on a clean checkout. Next.js 16 build does not run ESLint; lint is a separate check. No test runner/script or product tests exist yet; report test as not configured, not passed.

## Environment Variables

No variables or backend connection are required. `.env.example` documents this baseline. Future agreed variables go in ignored `frontend/.env.local`; restart the server after changes. Never commit secrets. `NEXT_PUBLIC_` variables are browser-visible and must not contain secrets. API URLs, authentication and Mock contracts remain in their owning issues.

## Initial Structure

- `src/app/layout.tsx`: mandatory HTML shell and stylesheet import.
- `src/app/page.tsx`: empty root route for execution verification only.
- `src/app/globals.css`: Tailwind import without product tokens.
- Next.js, TypeScript, PostCSS and ESLint configs: toolchain configuration.
- `next-env.d.ts`, `.next/`, `*.tsbuildinfo`: generated and ignored.

Do not pre-create future feature directories or implement later issues here.

## CI and Collaboration

Known tooling limitations: ESLint 9 is deprecated upstream, but the React/import/accessibility plugins bundled by eslint-config-next 16.3.8 do not yet declare ESLint 10 compatibility. Keep 9.39.5 until that combination supports 10. npm audit currently reports five high-severity entries from one braces stack-exhaustion advisory in the development-only Next.js lint dependency chain. No compatible fixed braces release is available; do not force-downgrade Next.js lint configuration to 14 or hide the advisory. Recheck before dependency upgrades. Production dependencies are audited separately.

`.github/workflows/frontend.yml` runs `npm ci`, lint, typecheck and build for frontend PRs targeting `front/develop` or manual dispatch, with the same Node/npm versions. No deployment is configured. Remote workflow execution and required branch checks must be verified on GitHub separately; local success is not remote CI success.

Use latest `origin/front/develop` and `front/feature/<issue-number>-<short-name>`. Do not directly commit/push protected branches. FE1 owns this environment; coordinate dependency/lockfile/CI changes with FE2 and execution impacts with BE2. Their feature code is outside this issue.

## Issue #37 Revalidation

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
