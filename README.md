# Discushion

지역 주민이 지역 문제를 확인하고 의견을 나누며, 주민 참여와 기관의 지역 안건 확인/채택을 연결하는 지역 참여 서비스.

## Repository 구조

```text
frontend/  Frontend 애플리케이션 개발 영역
backend/   Backend 애플리케이션 개발 영역
docs/      개발 기준 문서와 협업 문서 관리 영역
```

현재는 프로젝트 초기 골격만 구성하며, 실제 소스 구조는 각 기술 스택 확정 후 구성한다.

## 개발 브랜치 전략

```text
main
└── develop
    ├── frontend-part-a ~ d
    └── backend-part-a ~ d
```

Part 브랜치는 기능 구현 단위이며, Part 이름의 디렉터리는 생성하지 않는다. 모든 Part 브랜치는 공통 `frontend/`, `backend/` 폴더에서 개발한다.

제품 요구사항과 MVP 범위는 docs/specs의 기준 문서를 우선한다.

기준 문서 원본은 아직 이 저장소에 배치되지 않았다.
