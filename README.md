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

PRD v10.1, 기능명세서 v10.1, MVP 통합 지침서는 [docs/specs](docs/specs/)에서 확인한다. 문서의 기술 권장안은 확정된 기술 스택이나 제품 정책으로 취급하지 않는다.

## 개발 전 협업 준비

- [Git/GitHub 협업 전략](docs/collaboration/Discushion_Git_GitHub_통합협업전략_2026-10-06.md): Part 장기 브랜치, 검토·검증·통합 절차
- [팀 작업 현황](docs/collaboration/team-workspace.md): 담당자, 작업 기록, 검증 명령, develop 반영자
- [작업 카드](docs/collaboration/task-card-template.md): 세부 작업 하나의 범위·계약·검증 기록

일반 기능 개발은 GitHub Issue와 PR 대신 협업 문서와 로컬 검토·병합으로 관리한다. Part 간 변경 전달은 `develop`을 거친다. 기술 스택·실제 API 계약·담당자를 합의한 뒤 해당 Part 브랜치에서 개발을 시작한다.
