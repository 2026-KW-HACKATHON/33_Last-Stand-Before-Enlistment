# Discushion

2026-10-07 현재 서비스 선택·MVP 변경은 [결정 변경 기록](docs/specs/Discushion_MVP_결정변경_2026-10-07.md)과 각 기준 문서의 같은 날짜 반영을 확인하세요. 계약/문서 후속은 #74, 시연용 계정 준비는 #75입니다. 기존 비밀번호/증빙 신청 예시는 현재 MVP 계약이 아닙니다. API 내부 버전은 v1.1.2이며 변경된 경로/DTO는 합의 대기입니다.

지역 주민이 지역 문제를 확인하고 의견을 나누며, 주민 참여와 기관의 지역 안건 확인·채택을 연결하는 지역 참여 서비스입니다.

## 저장소 구성

```text
frontend/  프론트엔드 애플리케이션 영역
backend/   백엔드 애플리케이션 영역
docs/      제품 기준·API·설계·협업 문서
```

현재 저장소에는 Next.js 프론트엔드와 Spring Boot 백엔드의 통합 구현이 있습니다. 설치부터 화면 열기, 실제 API·로그인 연결까지는 [로컬 실행 안내](docs/LOCAL_SETUP.md)를 확인하세요. 백엔드 상세 설정은 [Backend 안내](backend/README.md)를 참고하세요.

## 개발 기준

- 제품 요구사항과 MVP 범위는 `docs/specs/`의 PRD v10.2, 기능명세서 v10.2, MVP 통합 지침서를 따릅니다.
- API 문서는 구현 전에 FE/BE가 검토·합의할 계약 초안입니다. 문서의 기술 제안이나 미확정 항목을 확정 정책으로 간주하지 않습니다.
- 저장소 작업 지침은 루트의 [`AGENTS.md`](AGENTS.md)를 참고하세요.

## GitHub 협업 흐름

```text
main
├── front/develop
│   └── front/feature/<issue번호>-<기능명>
└── back/develop
    └── back/feature/<issue번호>-<기능명>
```

기능 작업은 GitHub Issue에서 시작하고, 담당 영역의 Feature 브랜치에서 작업한 뒤 해당 영역의 `develop`을 base로 Pull Request를 엽니다. Part A~D는 기능 분류이며 Part 이름의 장기 브랜치나 디렉터리를 만들지 않습니다. `main`과 두 `develop` 브랜치에는 직접 push하지 않습니다.

## 문서 안내

- [PRD v10.2](docs/specs/Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md) · [기능명세서 v10.2](docs/specs/Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md)
- [MVP FE/BE 통합 지침서](docs/specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md)
- [MVP API 계약 초안](docs/api/Discushion_API_SPEC_v2.md)
- [현재 와이어프레임 기반 유저플로우](docs/specs/Discushion_유저플로우_와이어프레임기반_2026-10-06.md): 화면 흐름 참고 자료이며 제품 정책 정본을 대체하지 않습니다.
- [통합 Git/GitHub 협업전략](docs/collaboration/Discushion_Git_GitHub_통합협업전략_2026-10-06.md) · [Frontend 협업전략](docs/collaboration/Discushion_프론트엔드_Git_GitHub_협업전략_2026-10-06_v2.md) · [Backend 협업전략](docs/collaboration/Discushion_백엔드_Git_GitHub_협업전략_2026-10-06.md)
- [문서 색인과 협업 기록 양식](docs/README.md)
