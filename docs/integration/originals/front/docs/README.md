# Discushion 문서

프로젝트 제품 기준, API·데이터 설계, GitHub 협업 문서를 관리합니다.

## 디렉터리

- `specs/`: PRD·기능명세서·MVP 통합 지침서 및 ERD
- `api/`: FE/BE가 합의할 API 계약 자료
- `collaboration/`: 통합·Frontend·Backend GitHub 협업전략, 팀 작업 현황, 작업 카드
- `architecture/`: 시스템 구조, 데이터 흐름 및 권한 설계 자료

## 제품 기준 문서

- [PRD v10.2](specs/Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md)
- [기능명세서 v10.2](specs/Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md)
- [MVP 백엔드·프론트엔드 통합 지침서](specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md)
- [와이어프레임 기반 유저플로우](specs/Discushion_유저플로우_와이어프레임기반_2026-10-06.md)

제품 정책과 MVP 범위는 PRD·기능명세서의 확정 정책 및 범위 표기를 우선합니다. 통합 지침과 API·ERD 자료의 기술 권장안은 FE/BE가 검토·합의하기 전까지 확정 계약이 아닙니다.

## API·데이터 설계

- [MVP API 계약 초안](api/Discushion_API_SPEC_v2.md): 내부 버전 v1.1. 경로·필드·Enum은 FE/BE 합의와 실제 구현 대조가 필요합니다.
- [MVP ERD 상세명세](specs/Discushion_MVP_ERD_상세명세.md): 데이터 원본·관계·제약 설계안. DBMS 선택 후 실제 Schema/Migration으로 확정합니다.

## GitHub 협업전략

- [통합 협업전략](collaboration/Discushion_Git_GitHub_통합협업전략_2026-10-06.md): Issue → 영역별 Feature → PR → 해당 영역 develop
- [Frontend 협업전략 v2](collaboration/Discushion_프론트엔드_Git_GitHub_협업전략_2026-10-06_v2.md)
- [Backend 협업전략](collaboration/Discushion_백엔드_Git_GitHub_협업전략_2026-10-06.md)

Part A~D는 기능 분류이며 장기 Git 브랜치가 아닙니다. 브랜치 명명·PR·보호 규칙은 통합 협업전략을 기준으로 합니다.

## 작업 기록

- [팀 작업 현황](collaboration/team-workspace.md)
- [세부 작업 카드 양식](collaboration/task-card-template.md)
- [API 합의 기록 안내](api/README.md)
