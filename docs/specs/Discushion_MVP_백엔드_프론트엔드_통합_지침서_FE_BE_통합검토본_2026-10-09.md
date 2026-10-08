# Discushion MVP 백엔드·프론트엔드 통합 지침서 — 통합검토본

작성일: 2026-10-09 KST. 적용 대상: 임시 integration/develop.

## 작성 기준과 원본 보존

- main: dc139f43ce9e7cbe09d660202ee0ccec033b0f5f
- front/develop: 108cb477322eaffdfb42c520a93a5e53794031ce
- back/develop: eb9a7d4a734a744731d7bb85564b32f3c8f3b39c

기존 문서는 첫 번째 BE 병합 상태로 유지했다. 각 분기의 전체 원문은 docs/integration/originals/front/docs/specs/ 및 docs/integration/originals/back/docs/specs/의 같은 파일명에 보존했다. main 원문도 같은 originals/main 구조에서 확인할 수 있다. 보존 manifest의 SHA-256은 source blob과 동일하다. 이 검토본은 기존 정본을 대체하지 않는다.

## 함께 적용할 유효 변경

- Frontend: Figma 기반 UI·Mock 구현 대상과 실제 서버 연결 완료 상태를 분리한다. 표시 모델은 HTTP 응답과 동일한 구조라고 가정하지 않는다.
- Backend: 현재 Controller·API 계약 검토표의 Method·Endpoint·Request·Response를 기준으로 연결한다. DB 권한·지역 자격·소유권은 서버에서 최종 검사한다.
- 공통: 로그인 후 원 상세로 복귀해도 참여·북마크 행동은 자동 실행하지 않는다. 공유 게스트는 지정된 상세와 허용된 댓글/답글만 이용한다.
- 사진: 현재 서버 구현은 예약→Spring RAW 전송→완료 확인을 사용한다. 직접 Storage 전송을 현재 연결 방식으로 임의 적용하지 않는다. 상태·취소·삭제 완료를 구분한다.
- 기관: FE의 표시 상태·담당 지역 배열과 BE의 대표 상태·단일 지역 응답은 adapter로 변환한다. 사람이 승인하거나 실 연결이 끝났다고 기록하지 않는다.
- 검증: UI/Mock 성공, 정적 계약 대조, 서버 직접 호출, 실제 FE/BE 사용자 여정을 각각 기록한다.

## 확인 필요

- FE가 복구한 알림·관심·추천·설정·이메일 변경·탈퇴 등 UI/Mock 범위와 BE MVP 제외 목록의 최종 정책 합의.
- 사진 합계 제한: FE 10,485,760 bytes와 BE 10,000,000 bytes의 차이.
- 실제 API/service adapter, 표시 모델↔응답의 ID·enum·nullable·pagination·error 변환.
- 실제 지도 viewport의 지역 ID 집합과 서버 조회 조건 연결.
- 실제 환경에서 CORS 또는 같은 origin 중계 방식, Backend test/build 및 사용자 여정 검증.

이번 사용자 지시에 따라 integration/develop만 검토용으로 업로드한다. main/front/develop/back/develop에는 변경을 반영하지 않는다. 이 업로드는 위 미확정 사항의 해결이나 연결 완료를 뜻하지 않는다.

## 원격 업로드 검토에 따른 범위 제한

새 통합 문서에는 운영 키 발급·설정 과정이나 실제 환경 식별값을 복사하지 않는다. 이는 업로드 자동 검토에서 거절된 운영 설정 서술을 제거한 안전한 별도 검토본이다. branch 원본 문서의 파일명·원문·이력을 변경하거나 삭제하지 않았다. 세부 검증과 분기별 충돌은 docs/integration/Discushion_FE_BE_통합검증보고서_2026-10-09.md 및 다른 새 검토본을 함께 확인한다.
