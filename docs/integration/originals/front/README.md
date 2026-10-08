# Discushion

지역 주민이 지역 문제를 확인하고 의견을 나누며, 주민 참여와 기관의 지역 안건 확인·채택을 연결하는 지역 참여 서비스입니다.

## 저장소 구성

```text
frontend/  프론트엔드 애플리케이션 영역
backend/   백엔드 애플리케이션 영역
docs/      제품 기준·API·설계·협업 문서
```

현재 저장소에는 기준 문서와 개발 협업 자료가 있습니다. Frontend/Backend 프레임워크와 애플리케이션 코드는 아직 초기화되지 않았습니다.

## 개발 기준

- 제품 요구사항과 MVP 범위는 `docs/specs/`의 PRD v10.2, 기능명세서 v10.2, MVP 통합 지침서를 따릅니다.
- API 계약과 ERD 문서는 구현 전에 FE/BE가 검토·합의할 설계 자료입니다. 문서의 기술 제안이나 미확정 항목을 확정 정책으로 간주하지 않습니다.
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
- [와이어프레임 기반 유저플로우](docs/specs/Discushion_유저플로우_와이어프레임기반_2026-10-06.md)
- [MVP API 계약 초안](docs/api/Discushion_API_SPEC_v2.md) · [MVP ERD 상세명세](docs/specs/Discushion_MVP_ERD_상세명세.md)
- [통합 Git/GitHub 협업전략](docs/collaboration/Discushion_Git_GitHub_통합협업전략_2026-10-06.md) · [Frontend 협업전략](docs/collaboration/Discushion_프론트엔드_Git_GitHub_협업전략_2026-10-06_v2.md) · [Backend 협업전략](docs/collaboration/Discushion_백엔드_Git_GitHub_협업전략_2026-10-06.md)
- [문서 색인과 협업 기록 양식](docs/README.md)
