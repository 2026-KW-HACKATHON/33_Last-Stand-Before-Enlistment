# Discushion Backend

Issue #1의 실행 골격이다. Java 17, Spring Boot 4.0.8, Gradle Wrapper 9.7.1을 사용한다. DB는 사용자 선택에 따라 Supabase PostgreSQL로 연결한다. 버전은 초기화에 사용한 기술 기준이고, 파일 저장·이메일·AI 서비스는 미정이다. [결정 기록](../docs/collaboration/backend-environment-decisions.md)을 함께 확인한다.

## 로컬 실행

JDK 17을 설치하고 `JAVA_HOME`을 해당 JDK 디렉터리로 설정한다. 시스템 Gradle 설치는 필요하지 않다. 아래 명령은 `backend/` 안에서 실행한다.

```powershell
# Windows PowerShell: 경로는 실제 설치 위치에 맞춘다.
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.19'
.\gradlew.bat bootRun
```

```bash
# macOS / Linux: JAVA_HOME은 설치한 JDK 17 경로로 설정한다.
sh ./gradlew bootRun
```

기본 `local` 프로필은 DB나 외부 서비스 없이 시작한다. 기본 포트는 8080이며 `PORT` 또는 Spring의 `SERVER_PORT` 환경변수로 변경할 수 있다. `backend/.env`가 있으면 Spring이 properties 형식으로 읽는다. 값 앞에 `export`를 붙이지 않는다.

```powershell
Invoke-RestMethod -Uri http://localhost:8080/health
```

`GET /health`는 인증 없이 HTTP 200과 `{"data":{"status":"UP"}}`를 반환한다. API 초안 4.1절을 구현한 서버 생존 확인이며, DB·파일 저장·이메일·AI 연결 성공을 뜻하지 않는다. BE1의 계약 정합성 확인은 아직 필요하다. `local`은 향후 DB 의존 기능의 검증 환경으로 사용하지 않는다.

## Supabase 연결

1. `.env.example`을 `backend/.env`로 복사한다.
2. `SPRING_PROFILES_ACTIVE=supabase`로 바꾼다.
3. Supabase 프로젝트의 **Connect**에서 실제 호스트와 계정명을 확인하고 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 로컬 파일 또는 배포 환경에 설정한다. DB URL에는 비밀번호를 넣지 않는다.
4. `bootRun` 또는 아래 JAR 명령으로 시작한다.

서버리스 실행 후보와 IPv4 환경을 고려해 transaction pooler(6543)를 사용한다. `prepareThreshold=0`, `sslmode=verify-full`, 공식 Supabase 루트 CA 파일을 지정하는 `sslrootcert`를 연결 예시에 설정했다. 초기 Java 기본 신뢰 저장소 방식은 실제 인증서 체인을 신뢰하지 못해 실패했으므로 프로젝트 연결 전용 CA 파일로 보완했다. 시스템/Java 전역 인증서는 바꾸지 않고 인증서·호스트 검증을 유지한다. 연결 10초/소켓 30초는 초기 기술 기준이다. direct/session으로 바꾸면 실제 URL과 이유를 기록한다. [Supabase 연결 안내](https://supabase.com/docs/guides/database/connecting-to-postgres), [결정 기록](../docs/collaboration/backend-db-connection-decisions.md)

`DB_POOL_SIZE` 기본값은 인스턴스당 1이다. 트래픽과 Supabase 연결 한도를 확인한 뒤 조정한다. SQL 초기화는 비활성화돼 있으며 Schema/Migration은 BE1의 Issue #3에서 관리한다. ORM·도메인 Entity·테이블 생성은 포함하지 않는다. Supabase Auth와 Storage는 DB 선택만으로 채택하지 않는다. 2026-10-07 사용자 지정 개발 DB에 실제 Spring JDBC SELECT-only 검증 3개가 통과했다. 초기 검증용 postgres 계정이며 최종 앱 최소 권한 역할은 BE2/#4와 확인해야 한다.

원격 연결 회귀 검사는 `backend/`에서 `DISCUSHION_VERIFY_SUPABASE=true`를 설정하고 `./gradlew.bat test --tests com.discushion.SupabaseJdbcSmokeTests --rerun-tasks`로 명시적으로 실행한다. 지정 개발 프로젝트만 검사하며 비밀값은 `.env`에서 읽고 쿼리는 SELECT만 수행한다. 기본 실행에서는 이 3개 검사가 skip된다. 현재 PC의 CA 파일은 Git 제외 `.local-db/supabase-prod-ca-2021.crt`다. 다른 PC/배포 환경에서는 공식 대시보드의 SSL Certificate를 다운로드하고 실제 파일 경로를 지정한다. 이 원격 검사는 fixture를 사용하는 localhost 전용 Schema 시험과 구분한다.

## 테스트와 build

```powershell
.\gradlew.bat test
.\gradlew.bat build
# 실패 후 재검증은 기존 결과를 재사용하지 않는다.
.\gradlew.bat test --rerun-tasks
java -jar build/libs/discushion.jar
```

```bash
sh ./gradlew test
sh ./gradlew build
sh ./gradlew test --rerun-tasks
java -jar build/libs/discushion.jar
```

테스트는 실제 임시 HTTP 서버에서 익명 Health 응답, 지원하지 않는 HTTP Method, 없는 경로를 확인한다. 별도 설정 테스트는 `supabase` 프로필을 가짜 접속값으로 확인하며 DB에 연결하지 않는다. 테스트는 `backend/.env`를 읽지 않는다. 결과는 `build/reports/tests/test/index.html`, JAR는 `build/libs/discushion.jar`에 생성된다.

## Vercel 배포 준비

사용자가 선택한 배포 도구는 Vercel 플러그인이다. Spring Boot는 `Dockerfile.vercel`로 컨테이너 빌드하도록 준비했다. Vercel Container Images는 베타이며, 서버는 `$PORT`에 HTTP 요청을 받는다. [Vercel 공식 문서](https://vercel.com/docs/functions/container-images)

```powershell
# Docker가 실행 중인 backend/에서 검증한다.
docker build -f Dockerfile.vercel -t discushion-backend:local .
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=local discushion-backend:local
```

실제 배포는 Issue #30에서 프로젝트·요금제·리전·이미지 실행·DB 연결과 FE 연동을 검증한다. Backend를 별도 Vercel 프로젝트로 배포한다면 Root Directory는 `backend/`, `PORT=8080`을 프로젝트 환경변수로 설정한다. FE와 같은 프로젝트에 배포할 경우에는 Services와 `/health`, `/api/v1/*` 라우팅을 FE 담당자와 합의한다. 현재 루트 `vercel.json`, 외부 프로젝트 생성, 배포는 포함하지 않는다.

Vercel의 요청 본문 제한(4.5MB)과 제품의 사진·기관 증빙 한도를 함께 만족할 파일 전송 경로는 Issue #13·#12·#30에서 합의한다. 파일 저장 서비스가 미정인 상태에서 제품 한도를 줄이거나 임의 업로드 계약을 만들지 않는다. [Vercel 요청 제한](https://vercel.com/docs/functions/limitations#request-body-size)

FE/BE 실제 연동은 실제 FE가 실행 중인 Backend를 호출해야 한다. Health API 직접 호출이나 Backend 테스트만으로 FE/BE 연동을 완료 처리하지 않는다. API 오류·인증·권한 계약은 BE1의 Issue #2·#4와 맞춘다.

## 협업

Issue #1은 `back/feature/1-env`에서 작업하며 PR base는 `back/develop`이다. BE1은 API 정본과 DB Schema/Migration, BE2는 실행환경을 관리한다. `back/develop`과 `main`에는 직접 push하지 않는다. 실제 비밀번호·키·토큰·증빙 자료는 저장소에 넣지 않는다.
