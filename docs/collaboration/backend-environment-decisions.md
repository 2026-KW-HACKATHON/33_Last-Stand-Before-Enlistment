# Issue #1 백엔드 실행환경 결정 기록

기준일: 2026-10-07 (Asia/Seoul). 작업 역할은 BE2이며, 작업 브랜치는 `back/feature/1-env`, PR base는 `back/develop`이다. 사용자 답변으로 정해진 선택과 팀 협의가 남은 항목을 구분한다. 이 기록은 API 정본과 제품 정책을 대체하지 않는다.

## 확인된 선택

| 항목 | 선택 | 근거·상태 |
| --- | --- | --- |
| Frontend | Next.js, TypeScript, Tailwind CSS | 사용자 제공. 버전·App Router 여부는 실제 package.json에서 확인한다. |
| Backend | Java, Spring Boot, Gradle | 사용자 답변으로 선택. |
| DB | Supabase PostgreSQL | 사용자 답변으로 선택. 프로젝트 접속정보와 연결 검증은 아직 없다. |
| 배포 도구·서비스 | Vercel 플러그인 | 사용자 답변으로 선택. 실제 프로젝트·리전·배포는 Issue #30에서 확인한다. |

Frontend의 TanStack Query, React Hook Form, Zod, Zustand, Axios는 선택 후보다. Backend 초기화의 선행조건으로 설치하지 않는다. 지도 provider와 실시간 투표 방식도 확정하지 않는다.

## 구현 전에 확인할 항목

| 항목 | 현재 상태 | 담당·후속 작업 |
| --- | --- | --- |
| JDK·Spring Boot·Gradle 버전 | 기존 문서의 확정 버전은 미정. 실행 골격은 Java 17, Spring Boot 4.0.8, 공식 Initializr의 Gradle 9.7.1로 구성 | BE1·BE2가 초기화 버전 검토, Issue #1 |
| Health Check | API 초안 4.1절: 인증 없는 `GET /health`, HTTP 200, `{"data":{"status":"UP"}}`를 구현. 계약 합의 완료로 표시하지 않음 | BE1 계약 정합성 확인, BE2 구현 |
| 파일 저장 서비스 | 미정. 기존 md 문서에 서비스 선택 없음 | BE2, Issue #13 |
| 이메일 발송 서비스 | 미정. 기존 md 문서에 서비스 선택 없음 | BE1·BE2, Issue #6 |
| AI 연동 서비스 | 미정. 기존 md 문서에 서비스 선택 없음 | BE2, Issue #20 |
| 서버 배포 환경 | Vercel Container Images(베타)를 실행 후보로 문서화. 실제 프로젝트·설정 미정 | BE2, Issue #30 |
| Supabase 연결 방식 | JDBC transaction pooler 예시, TLS·prepared statement 비활성화·기본 pool 1. 실제 주소·계정은 프로젝트의 Connect 화면에서 확인 | BE2 실행환경, BE1 DB 모델 |

Supabase DB 선택만으로 Supabase Auth 또는 Storage를 선택한 것으로 간주하지 않는다. 실제 비밀번호·키·접속정보는 저장소와 이 문서에 기록하지 않는다.

## Issue #1 범위와 검증

- 확정된 스택의 애플리케이션, 실행·테스트·build 설정, 환경변수 예시와 Health Check를 준비한다.
- Schema/Migration·도메인 Entity는 Issue #3, 공통 인증·권한은 Issue #4에서 각 담당자가 구현한다.
- 실제 테스트·build 결과는 아래 검증 기록에 남긴다. Supabase DB 연결·FE/BE 실제 연동·실제 배포는 미검증이다.

버전 선택 근거: 기존 애플리케이션 의존성이 없고 로컬에는 JDK 17.0.19가 있다. Java 17과 호환되는 Spring Boot 4.0.8 안정 버전으로 초기화했고, Gradle 9.7.1은 공식 Spring Initializr에서 생성한 Wrapper 버전이다. Snapshot/Milestone은 사용하지 않았다. Wrapper JAR와 배포 ZIP의 공식 SHA-256을 확인했다. 이는 이번 골격의 기술 선택 기록이며 외부 서비스의 미정 상태나 API 초안의 상태를 바꾸지 않는다.

Supabase 플러그인은 설치·활성화 상태지만 현재 채팅에 조회 도구가 제공되지 않아 실제 프로젝트를 조회하지 못했다. Vercel 플러그인으로 공식 컨테이너 지원 문서와 이름에 discushion이 포함된 프로젝트 목록을 조회했으며 검색 결과는 없었다. 프로젝트·배포·DB·인증 설정은 변경하지 않았다.

Issue #1 완료 조건 중 외부 서비스 선택·FE/BE 합의는 남아 있다. 초기화 코드가 통과해도 Issue 전체를 완료 처리하거나 닫지 않는다.

## 검증 기록

검증일: 2026-10-07 (Asia/Seoul), JAR 확인 완료 02:23. 확인자: Codex. 대상은 기준 SHA `d1114f4`에서 작성한 `back/feature/1-env`의 미커밋 작업 트리이며, 새 변경을 이미 커밋한 SHA로 기록하지 않는다.

| 검사 | 실제 결과 |
| --- | --- |
| `backend/`에서 `gradlew.bat --no-daemon test build --console=plain` | 성공. 테스트 4개, 실패·오류 0개. JAR 생성 완료 |
| 생성 JAR 직접 실행 + `GET /health` | 임시 포트의 실제 HTTP 서버에서 200 및 계약 JSON 확인, 실행 프로세스 종료 |
| Supabase 설정 테스트 | 가짜 접속값으로 DataSource와 lazy pool 설정 확인. 실제 원격 DB 연결 검증 아님 |
| Gradle Wrapper | JAR SHA-256 일치, 배포 ZIP checksum을 Wrapper 설정에 기록 |
| 실제 Docker build/run | 미실행. Docker Linux Engine이 실행되지 않아 `docker info`가 연결 실패 |
| 실제 Supabase DB 연결 | 미실행. 플러그인 조회 도구·실제 연결 설정 미확보 |
| FE/BE 실제 연동·Vercel 배포 | 미실행. FE 실행환경·배포 프로젝트 미준비 |

Vercel Functions의 요청 본문 제한은 4.5MB다. 제품 명세의 게시물 사진 합계 10MB와 기관 증빙 한도를 그대로 유지하려면 Issue #13·#12·#30에서 파일 저장소 직접 업로드 등 전송 경로를 FE/BE와 합의해야 한다. 요청 제한에 맞추려고 제품의 파일 한도를 임의로 줄이지 않는다. 파일 저장 서비스는 아직 미정이다. [Vercel 요청 제한](https://vercel.com/docs/functions/limitations#request-body-size)

## 참고

- [Issue #1](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/issues/1)
- [Backend 협업전략](Discushion_백엔드_Git_GitHub_협업전략_2026-10-06.md)
- [API 계약 초안](../api/Discushion_API_SPEC_v2.md)
- [Supabase PostgreSQL 연결 안내](https://supabase.com/docs/guides/database/connecting-to-postgres)
- [Spring Boot 4.0 시스템 요구사항](https://docs.spring.io/spring-boot/4.0/system-requirements.html)
- [Vercel Container Images](https://vercel.com/docs/functions/container-images)
