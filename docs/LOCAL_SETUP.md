# Discushion 로컬 실행 안내

현재 `main`의 통합 구현을 PC에서 설치하고 실행하는 방법이다. VS Code 등의 편집기로 저장소 폴더를 열고, 아래 명령을 터미널에서 실행한다.

## 1. 실행 방식 선택

| 목적 | 실행할 항목 | 필요한 설정 |
| --- | --- | --- |
| 화면 디자인·상태 확인 | 프론트엔드 개발 서버 + `/dev/` 미리보기 | Node.js, npm |
| 실제 계정으로 서비스 사용 | 프론트엔드 + 배포 API | 실제 Privy App ID, 이메일 OTP, 배포 서버의 localhost CORS 허용 |
| 백엔드 시작 확인 | 백엔드 `local` 프로필 | JDK 17, DB 불필요 |
| FE·BE 기능을 모두 로컬 서버로 연결 | 프론트엔드 + 백엔드 `supabase` 프로필 | 실제 개발 DB·권한·Privy 설정, 기능별 외부 서비스 설정 |

프론트엔드 실행 자체는 Privy 키 없이 가능하다. 다만 회원 기능은 로그인 설정이 필요하다. 기본 백엔드 `local` 프로필은 Health 확인용이며 게시물·회원 API를 제공하는 전체 기능 환경이 아니다.

## 2. 준비물

| 도구 | 버전·용도 |
| --- | --- |
| Git | 저장소 다운로드 |
| Node.js | **24.21.0** (`frontend/.nvmrc`) |
| npm | **11.19.0** (`frontend/package.json`) |
| JDK | **17** — 백엔드를 실행할 경우 |
| 편집기 | VS Code 또는 IntelliJ IDEA 등 |

Gradle은 저장소의 Wrapper가 **9.7.1**을 다운로드하므로 따로 설치할 필요 없다. 최초 설치 시 npm·Gradle 의존성을 내려받을 인터넷 연결이 필요하다.

Node.js는 [공식 배포 페이지](https://nodejs.org/dist/v24.21.0/) 또는 기존 버전 관리 도구로 설치한다. macOS/Linux에서 nvm을 이미 사용한다면 `frontend/`에서 `nvm install`과 `nvm use`를 실행하면 된다. npm 버전이 다르면 다음 명령으로 맞춘다.

```sh
npm install --global npm@11.19.0
node --version
npm --version
```

기대 버전은 각각 `v24.21.0`, `11.19.0`이다. 백엔드를 실행하려면 `java -version`도 확인하고 `JAVA_HOME`을 설치한 JDK 17 폴더로 설정한다.

## 3. 저장소 다운로드 및 열기

```sh
git clone --branch main https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment.git
cd 33_Last-Stand-Before-Enlistment
```

VS Code에서 **파일 → 폴더 열기**로 이 폴더를 선택한다. `code` 명령이 설치돼 있다면 `code .`로 열어도 된다. 백엔드를 IntelliJ에서 따로 열 때는 `backend/`를 Gradle 프로젝트로 불러오고 Gradle JVM을 JDK 17로 선택한다.

이미 복제한 저장소는 미커밋 변경을 확인·보존한 뒤 `main`에서 `git pull --ff-only origin main`으로 갱신한다.

## 4. 프론트엔드 설치 및 실행

아래 작업은 **`frontend/` 안에서** 한다.

```sh
cd frontend
npm ci
```

환경 예시를 복사한다. 기존 `.env.local`이 있다면 덮어쓰지 말고 필요한 값만 수정한다.

**macOS / Linux**

```sh
cp .env.example .env.local
```

**Windows PowerShell**

```powershell
Copy-Item .env.example .env.local
```

`frontend/.env.local`을 편집한다.

```dotenv
NEXT_PUBLIC_API_BASE_URL=https://discushion-api.onrender.com/api/v1
NEXT_PUBLIC_PRIVY_APP_ID=
```

API 주소에는 **`/api/v1`까지 포함**한다. 배포 API 주소는 사용자가 확인한 주소다. 실제 로그인 시 `NEXT_PUBLIC_PRIVY_APP_ID`에 같은 백엔드에서 사용하는 Privy 애플리케이션의 공개 App ID를 입력한다. 키가 비어 있으면 게스트 상태와 로그인 설정 안내가 표시된다.

```sh
npm run dev
```

브라우저에서 **http://localhost:3000**을 연다. 시작 화면이 표시된다. 터미널을 닫으면 개발 서버도 종료되며, 직접 종료할 때는 `Ctrl+C`를 누른다.

Windows에서 `npm.ps1` 실행 정책 오류가 나면 명령의 `npm`을 `npm.cmd`로 바꾼다. 예: `npm.cmd ci`, `npm.cmd run dev`.

### 외부 서비스 없이 화면 보기

`npm run dev`가 실행 중일 때 다음 주소로 접근한다. 미리보기는 Mock 데이터를 사용하므로 실제 회원·서버 저장 결과를 확인하는 방법은 아니다. 미리보기의 모든 기능이 현재 MVP에 포함된다는 의미도 아니다.

| 화면 | 주소 |
| --- | --- |
| 공통 UI | http://localhost:3000/dev/ui-preview |
| 홈·게시판 화면 | http://localhost:3000/dev/explore-preview |
| 게시물 상세 | http://localhost:3000/dev/post-preview |
| 게시물 작성·수정 | http://localhost:3000/dev/post-editor-preview |
| 로그인·가입 UI | http://localhost:3000/dev/login-preview · http://localhost:3000/dev/signup-preview |
| 프로필 | http://localhost:3000/dev/profile-preview |

`/dev/integration-preview`는 별도의 로컬 HTTP fixture 서버(`127.0.0.1:4199`)를 사용하는 검증 페이지다. `npm run dev`만으로 실제 API 검증을 할 수 있는 페이지가 아니다.

### 실제 로그인·배포 API 연결

1. Privy 대시보드에서 이메일 OTP 로그인을 활성화한다.
2. Privy 허용 주소에 **`http://localhost:3000`**을 등록한다.
3. FE의 `NEXT_PUBLIC_PRIVY_APP_ID`와 BE의 `PRIVY_APP_ID`가 동일한 앱인지 확인한다.
4. 배포 백엔드의 `CORS_ALLOWED_ORIGINS`에 **`http://localhost:3000`**을 등록하고 적용한다. 로컬 FE 파일 변경만으로 원격 서버의 CORS 설정이 바뀌지는 않는다.
5. 프론트엔드를 재시작한 뒤 본인이 사용할 수 있는 이메일로 OTP 로그인을 진행한다. 신규 회원은 가입 절차를 마친다.

실제 서비스 화면은 `/home`, `/board`, `/map`, `/posts/new`, `/me` 등에 연결돼 있다. 회원 여부와 서버가 반환한 지역·기관 권한에 따라 접근할 수 있는 기능이 달라진다. 로그인·가입만으로 지역 참여나 기관 채택 권한이 생기지 않는다. 시연 권한 계정은 별도 준비된 계정을 사용한다.

지도는 Leaflet·OSM 타일과 저장소에 포함된 행정동 GeoJSON을 사용하므로 별도 지도 SDK 키가 필요 없다. 배경 타일에는 인터넷 연결이 필요하다. 서버 지역 ID는 지도 코드와 같은 값으로 가정하지 않고 API의 `mapFeatureKey` 또는 유일한 전체 행정동 이름을 기준으로 연결한다.

## 5. 백엔드 시작만 확인하기 — DB 불필요

**새 터미널**을 열어 저장소의 `backend/`로 이동한다. 프론트엔드 터미널은 계속 실행해 둔다.

```sh
cd backend
```

환경 예시를 복사한다. 기존 `.env`가 있다면 먼저 내용을 확인한다.

**macOS / Linux**

```sh
cp .env.example .env
sh ./gradlew bootRun
```

**Windows PowerShell**

```powershell
Copy-Item .env.example .env
.\gradlew.bat bootRun
```

`backend/.env`의 `SPRING_PROFILES_ACTIVE=local`, `PORT=8080`을 유지한다. Spring은 **`backend/`를 작업 디렉터리로 실행할 때** 이 `.env`를 properties 형식으로 읽는다. 값 앞에 `export`를 붙이지 않는다.

브라우저에서 **http://localhost:8080/health**을 열거나 다음 명령으로 확인한다.

```sh
curl http://localhost:8080/health
```

PowerShell에서는 `Invoke-RestMethod http://localhost:8080/health`을 사용할 수 있다. 기대 응답은 `{"data":{"status":"UP"}}`이다. 로컬 코드의 Health 경로는 **`/health`**이며 `/api/v1/health`이 아니다. 성공해도 DB·OTP·사진·AI 연결이 검증된 것은 아니다.

## 6. 프론트엔드를 로컬 백엔드의 실제 기능에 연결하기

전체 기능을 로컬 BE에서 사용하려면 프로젝트의 **개발용 Supabase DB와 실제 서비스 설정**이 필요하다. 기본 `local` 프로필을 그대로 두고 FE 주소만 변경하면 전체 API가 연결되지 않는다.

### 백엔드 설정

`backend/.env`의 예시값을 실제 개발 환경 값으로 바꾼다.

| 변수 | 설정 내용 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `supabase` |
| `PORT` | `8080` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` |
| `DB_URL` | 예시의 JDBC 형식에 실제 pooler 호스트·공식 CA 파일 경로 입력 |
| `DB_USERNAME`, `DB_PASSWORD` | 준비된 서버용 최소 권한 계정 |
| `PRIVY_APP_ID`, `PRIVY_APP_SECRET` | FE와 동일한 앱의 ID 및 서버 전용 Secret |
| `PUBLIC_WEB_BASE_URL` | 공유 기능에 사용할 웹 주소. 로컬 공유 검증은 `http://localhost:3000` |
| `SHARE_TOKEN_SIGNING_KEY` | 공유 기능용 키. Base64로 인코딩한 32바이트 이상의 랜덤 값 |
| `SUPABASE_URL`, `SUPABASE_SECRET_KEY`, `SUPABASE_STORAGE_BUCKET` | 사진 기능에 사용할 실제 프로젝트·서버 키·버킷 |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | AI 기능용 서버 키 및 예시의 `gemini-3.5-flash-lite` |

DB 연결은 `.env.example`의 `sslmode=verify-full`, 공식 CA 경로, transaction pooler의 `prepareThreshold=0`을 유지한다. CA 파일 경로는 PC마다 다르며 Windows properties 파일에서는 `C:/...`처럼 슬래시 경로를 쓰면 역슬래시 이스케이프 문제를 피할 수 있다.

`PHOTO_UPLOADS_ENABLED`, `PHOTO_STORAGE_WIRE_VERIFIED`, `PHOTO_CLEANUP_ENABLED`, `AI_SUMMARIES_ENABLED`는 예시에서 비활성 상태다. 필요한 Migration·서버 DB 권한·Storage 또는 AI 연결을 확인한 기능만 활성화한다. 사진 업로드·정리나 AI 생성을 확인하려면 해당 기능의 실제 구성이 필요하다.

앱 시작은 Migration을 자동 적용하거나 회원·시연 권한 데이터를 생성하지 않는다. 기존 개발 DB를 쓸 경우 담당자가 준비한 Schema·계정·권한을 사용한다. 새 DB 준비는 [Supabase 안내](../supabase/README.md)와 [DB 연결 결정 기록](collaboration/backend-db-connection-decisions.md)을 따른다. 공유 DB에 임의로 Migration을 적용하지 않는다.

### 프론트엔드 설정 및 재시작

`frontend/.env.local`을 수정한다.

```dotenv
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_PRIVY_APP_ID=<실제 공개 Privy App ID>
```

Privy 허용 주소에 `http://localhost:3000`을 등록한다. 두 서버를 각각 `Ctrl+C`로 종료한 뒤 해당 폴더에서 다시 실행한다.

| 터미널 | 위치 | macOS/Linux 명령 | Windows PowerShell 명령 |
| --- | --- | --- | --- |
| FE | `frontend/` | `npm run dev` | `npm.cmd run dev` |
| BE | `backend/` | `sh ./gradlew bootRun` | `.\gradlew.bat bootRun` |

브라우저에서 `http://localhost:3000`으로 들어간 뒤 개발자 도구의 Network 탭에서 요청이 `http://localhost:8080/api/v1`로 가는지 확인한다. Health 성공 → OTP 로그인 → 가입 상태 → 목록 조회 순으로 확인하면 문제가 생긴 구간을 구분하기 쉽다.

모든 `NEXT_PUBLIC_` 변수는 브라우저에 공개된다. 여기에 Privy Secret, DB 비밀번호, Supabase Secret, Gemini 키를 넣지 않는다. `.env`·`.env.local`은 Git 제외 대상이며 실제 값은 커밋하지 않는다.

## 7. 빌드 및 검사

프론트엔드 `frontend/`에서:

```sh
npm run lint
npm run typecheck
npm run build
npm run start
```

`npm run start`는 빌드된 앱을 실행한다. 같은 3000 포트의 개발 서버는 먼저 종료한다. 공개 환경변수 변경은 다시 빌드해야 반영된다. 개발 전용 미리보기 페이지는 운영 빌드에서 사용할 수 없다. 이 프로젝트에는 `npm test` 스크립트가 없다.

백엔드 `backend/`에서:

```sh
sh ./gradlew test bootJar
java -jar build/libs/discushion.jar
```

Windows에서는 첫 명령을 `.\gradlew.bat test bootJar`로 바꾼다. 기존 `bootRun` 서버는 먼저 종료한다. DB·외부 서비스 조건이 없는 테스트는 일부 skip될 수 있으므로 테스트 성공을 실제 DB·배포 인수 성공과 구분한다.

## 8. 자주 생기는 문제

| 증상 | 확인 및 해결 |
| --- | --- |
| `EBADENGINE` | Node 24.21.0 / npm 11.19.0인지 확인한다. 저장소의 버전 검사를 끄지 않는다. |
| Windows에서 `npm.ps1` 실행 차단 | `npm.cmd`를 사용한다. |
| 3000 포트 사용 중 | 다른 개발 서버를 종료하거나 `npm run dev -- --port 3001`로 실행한다. 실제 로그인/API 연결에는 Privy·CORS에도 `http://localhost:3001`을 추가한다. |
| 로그인 설정 준비 안내 | FE의 공개 Privy App ID가 비어 있는지 확인하고 재시작한다. |
| OTP 수신·검증 실패 | Privy 이메일 로그인 활성화, 허용 주소, App ID 일치와 해당 이메일 수신함을 확인한다. |
| 브라우저 CORS 오류 | 현재 FE origin을 해당 백엔드에 정확히 등록한다. `localhost`와 `127.0.0.1`은 서로 다른 origin이다. |
| API 404 | FE 주소의 `/api/v1`, BE 프로필, 실행·배포된 코드 버전을 확인한다. |
| API 401 / 403 | 로그인·가입 완료 여부와 실제 회원의 지역·기관 권한을 확인한다. |
| DB 연결·인증서 실패 | 실제 호스트·계정·CA 경로·권한을 확인한다. 인증서 검증을 끄지 않는다. |
| 백엔드가 `.env` 값을 못 읽음 | `backend/`에서 실행했는지, IDE의 작업 디렉터리와 기존 OS 환경변수가 값을 덮는지 확인한다. |
| 지도 배경 타일이 비어 있음 | 인터넷 연결과 `tile.openstreetmap.org` 접근을 확인한다. 지역 데이터 연결 문제는 서버 응답의 이름·`mapFeatureKey`를 확인한다. |

## 관련 문서

- [프론트엔드 안내](../frontend/README.md) — 초기 환경 구축 기록 포함
- [백엔드 안내](../backend/README.md)
- [현재 통합 구현·검증 기록](integration/Discushion_FE_BE_통합검증보고서_2026-10-09.md)

이 안내는 저장소의 실행 설정을 기준으로 작성했다. 실제 배포 서버의 CORS·Privy·DB 구성 상태는 별도로 확인해야 한다.
