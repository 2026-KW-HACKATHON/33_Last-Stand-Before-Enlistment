# Discushion 현재 와이어프레임 기반 유저플로우

## 2026-10-07 MVP 적용 주석

이 문서의 화면 번호·노드·Prototype 연결은 2026-10-06 디자인 관찰 기록으로 보존한다. Figma가 수정됐거나 코드가 구현됐다는 뜻은 아니다. [MVP 결정 변경 기록](./Discushion_MVP_결정변경_2026-10-07.md)에 따라 구현 범위는 다음처럼 달라졌다.

- 로그인/가입: 자체 비밀번호·가입 코드 폼 대신 Privy OTP 인증과 로컬 동의·프로필·지역 완료/returnTo를 적용한다. 기존 코드 타이머·제한은 provider 정책 확인/FE 합의가 필요하다.
- 이웃/기관 인증: 증빙 입력·첨부·신청 제출·접수 UI는 제외한다. 시연 계정의 완료 지역·기관 유효 상태·권한/배지 조회는 유지한다. 기존 신청 진입의 비노출/대체 안내는 #74에서 맞춘다.
- 사진: Supabase 공개 열람, 저장 파일 삭제, 미완료 업로드 24시간 정리. 직접 업로드/실패/재시도·최종 연결은 #74/#13/#54 계약을 따른다. 게시물 초안 임시저장은 추가하지 않는다.
- AI: 선택 모델은 Gemini 3.5 Flash-Lite이며 기존 3문장·원문 fallback UI를 유지한다. 시연 계정과 권한 검증은 #75를 사용한다.

아래 변경 전 화면·주석을 현재 MVP 필수 완료 조건이나 서버 구현 완료 근거로 사용하지 않는다.

디자인 관찰 기준일: 2026년 10월 6일
문서 개정일: 2026년 10월 7일 / 구현 범위 기준: 제품 v10.2
대상: Figma **KW 해커톤 디자인 → 와이어프레임**  
용도: 팀 디자인·개발용 화면 흐름과 조건 확인  
원본: [와이어프레임 페이지](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=689-905)

## 1 문서의 범위와 읽는 방법

Discushion의 현재 화면은 시작·가입·로그인, 지역 탐색, 게시판과 지도, 게시물 작성·참여, 공유 게스트 이용, 개인 기록, 인증, 기관 담당자, 설정·탈퇴로 구성된다. 이 문서는 A~L의 모바일 프레임 160개와 현재 연결을 기록한다. 같은 번호의 인증·검색·필터·오류 상태도 별도 프레임으로 포함한다.

화면 이름과 번호는 Figma에 있는 그대로 사용한다. 새 번호를 부여하거나 빠진 번호를 채우지 않는다. 이름이 같은 두 A02 로그인은 Figma 노드 ID로 구분한다.

- **연결 확인**: 클릭·조건 분기·변수 변경 등 현재 Prototype 설정으로 확인한 동작.
- **화면 표시**: UI에 버튼·입력·문구는 있지만 해당 기능의 연결이나 상태 변경이 확인되지 않은 경우. 기능 구현 완료를 뜻하지 않는다.
- **주석 참고**: 화면 밖 설명과 관련 상태에 적힌 정보. 실제 제품 UI와 구분하며, 문서나 PRD로 재검증한 확정 정책으로 취급하지 않는다.
- **확인 필요**: 현재 연결, UI, 기존 주석 사이의 차이 또는 목적지가 없는 동작.

화살표는 실제 화면 이동 순서를 뜻한다. “제자리 상태 변경”은 화면 이동 없이 변수를 바꾸는 동작이다. “이전 화면”은 Figma의 뒤로 가기이며 고정된 목적지가 아니다. 실제 서버 저장, 메일 발송, 권한 판정, 클립보드 복사까지 검증한 문서는 아니다.

## 2 전체 흐름

### 2.1 일반 회원의 이용

A01 시작 → A02 로그인 → B01 메인

B01에서 다음 경로로 갈라진다.

| 사용자 선택 | 연결된 경로 |
| --- | --- |
| 통합 게시판 | C01 → 지역 안건 F01, 지역 활동 F03, 진행 투표 F04 |
| 검색 | C02 → 결과 있음 또는 없음 → 결과 게시물 상세 |
| 지역 변경 | B02 → 하계2동 선택 → B03 |
| 이슈 지도 | D01 → 동별 말풍선에 대응하는 상세 |
| 글쓰기 | E01 → E02 지역 안건 / E03 지역 활동 정보 / E04 투표 |
| 알림 | H01 → 관련 상세 또는 H03 삭제 안내 |
| 마이 | I01 → 개인 기록·프로필·지역·인증·계정 |
| 설정 | L01 → 계정·키워드·알림·로그아웃·탈퇴 |

현재 B01에는 별도 온보딩 화면을 거치는 연결이 없다. A01에 서비스 소개와 로그인·회원가입 진입이 함께 있다. 게시 완료 화면 이후의 이동은 현재 연결되어 있지 않으므로 전체 흐름을 임의로 상세까지 완성하지 않는다.

### 2.2 공유 링크 이용

공유 게시물 게스트 시작점 G01 또는 공유 투표 게스트 시작점 G08 → 공개 내용 확인.

게스트 댓글·답글은 로그인 없이 상태 시연이 가능하다. 회원 기능을 선택하면 G02 로그인으로 이동한다. 로그인·가입 완료 뒤에는 원 게시물 유형에 따라 G05 또는 G09로 복귀한다. 복귀 자체로 반응·북마크·투표를 자동 실행하지 않는다.

### 2.3 기관 담당자 이용

K01 담당자 목록 → 담당 지역 안건 K02 → 채택 K04 → 본 기관 채택 취소 K02.

타지역 안건은 K03 열람 전용으로 연결된다. K01에는 독립된 Prototype 시작점이 있다. J05 기관 인증 접수에서 K01로 승인 완료를 시연하는 연결은 없다.

## 3 현재 화면에서 구분되는 사용자와 지역

| 구분 | 열람 | 참여와 관리 | 확인 근거 |
| --- | --- | --- | --- |
| 일반 서비스의 회원 화면 | 메인·게시판·지도·상세 | 기능별 버튼과 연결 상태가 다름 | B01, C01, F01~F04, I01 |
| 하계2동 이웃 미인증 회원 | 게시물·댓글·투표 결과·AI 요약 | 반응·댓글·투표·글쓰기는 B09 제한 안내로 연결 | B03, B05~B10 |
| 다른 동의 열람 전용 상세 | 월계2동·월계3동 안건, 하계1동 활동 | 지역 참여는 제한 안내; 북마크·공유·신고는 별도 경로 | F16~F22 |
| 공유 링크 게스트 | 특정 공유 게시물·요약·공개 투표 집계 | 게스트 댓글·답글; 회원 기능은 로그인 진입 | G01, G06~G08 |
| 공유 로그인 복귀 회원 | 원 게시물·원 투표 | 북마크를 다시 선택; 지역 참여는 이웃 인증 진입 | G05, G09 |
| 유효 기관 인증 담당자 상태 | 전체 지역 공개 안건 | 담당 지역 안건 채택·본 기관 채택 취소 | K01~K04 |

기본 활동 지역과 현재 탐색 지역은 다르다. B03~B14는 기본 활동 지역이 월계1동인 회원이 하계2동을 탐색하는 상태다. 탐색 지역 변경은 거주 지역 변경이나 참여 권한 부여가 아니다. B05의 추천은 화면 문구대로 월계1동과 관심 키워드를 기준으로 유지된다.

현재 UI의 인증 안내와 연결을 기록한 것이며, 일반 서비스의 모든 버튼에 인증 검사가 구현되어 있다고 해석하지 않는다.

## 4 A 시작 로그인 가입 비밀번호 복구

### 4.1 시작과 로그인

A01의 로그인 → A02 원본 로그인, 회원가입 → A06. 두 진입 모두 `returnTo 있음`과 `returnTo 투표`를 false로 초기화한다.

A02 원본의 로그인 클릭은 다음 조건으로 연결된다.

| 복귀 정보 | 목적지 |
| --- | --- |
| 원 게시물 있음 + 투표 | G09 로그인복귀 미인증회원투표 |
| 원 게시물 있음 + 일반 게시물 | G05 로그인복귀 미인증회원상세 |
| 원 게시물 없음 | B01 메인 |

A02의 비밀번호 찾기 → A03, 회원가입 → A06. 이메일과 비밀번호 입력 UI는 있으나 실제 인증 성공·실패 판정을 시연하는 연결은 확인되지 않는다.

같은 이름의 A02 복제본 노드 `812:5765`도 존재한다. 복제본은 공유 투표 조건 외에는 B01로 이동하며 원본과 분기가 다르다. 부록에서 두 화면을 구분한다.

### 4.2 회원가입

A06 계정정보·약관 → 인증번호 발송 → A07 발송.

A07 발송에서 인증번호 확인 → A07 완료, 재발송 → A07 재발송. 재발송 상태의 인증번호 확인도 A07 완료로 연결된다.

A07 완료 → 가입하고 프로필 설정 → A08 프로필 설정 → 활동 지역 설정 → A09 → 이 지역으로 설정하기 → A08 활동지역설정완료 → 설정 완료하고 시작하기.

마지막 버튼은 일반 진입이면 B01, 공유 복귀 정보가 있으면 G05/G09로 이어진다.

화면에 표시된 입력·선택 사항은 이메일, 비밀번호, 비밀번호 확인, 이메일 인증번호, 필수·선택 약관, 프로필 사진, 닉네임, 복수 이웃 속성이다. 닉네임은 중복 없이 최대 10자로 표시된다. A09에는 지역명 검색·현재 위치로 찾기·지역 목록이 있지만 현재 연결은 설정 버튼의 복귀를 중심으로 구성된다.

A08과 활동지역설정완료 상태의 이웃 인증하기 → J01. 다만 J02 접수의 프로필로는 가입 중 A08이 아니라 I06으로 이동한다. 가입 중 인증 후의 복귀 맥락은 확인 필요다.

### 4.3 비밀번호 복구

A02 또는 가입 화면의 비밀번호 찾기 → A03 등록 이메일 → 인증번호 발송 → A04 본인 확인 → 확인 → A05 새 비밀번호 → 재설정 완료 → A02 원본 로그인.

A04 재발송 → A04 재발송 상태 → 확인 → A05. 화면에는 6자리 인증번호, 04:59, 재발송 대기 60초가 표시된다. 타이머의 실시간 진행이나 대기 중 버튼 활성화 판정은 별도 연결로 확인되지 않는다.

복구 완료 자체는 로그인이 아니다. A02에서 로그인할 때 남아 있는 공유 복귀 정보에 따라 G05/G09로 이동한다.

## 5 B 메인과 탐색 지역 변경

### 5.1 기본 활동 지역 메인

B01은 월계1동 메인이다. 검색 → C02, 통합 게시판 → C01, 진행 중 투표 → F04, 지금 뜨는 이야기 → F01, 전체 지도 및 지도 미리보기 말풍선 → D01로 연결된다. 메인의 지도 말풍선은 즉시 게시물 상세로 가지 않고 전체 지도에 먼저 진입한다.

B01 지역 변경 → B02. 월계1동 또는 닫기는 이전 화면, 하계2동 → B03. 월계2동·월계3동·하계1동은 “시연 범위 외”로 표시된다.

### 5.2 하계2동 탐색

B03에서 검색 → B12, 지역 변경 → B04, 통합 게시판 → B05, 안건 카드 → B06, 투표 결과 보기 → B07, 지도 → B08, 글쓰기 → B09.

B04에서 월계1동 → B01, 현재 하계2동 또는 닫기 → 이전 화면.

B05에서 하계2동 안건 → B06, 투표 → B07, AI 요약 → B10. 월계1동 추천 카드 → F01로 연결된다. 추천과 탐색 게시물은 서로 다른 기준으로 배치되어 있다.

B06/B07의 지역 반응, 댓글 등록, 투표 선택·참여 등 → B09 제한 안내. 북마크는 제자리 상태 변수 변경, 공유 → B11 일반 회원 기능 안내, 신고 → F08이다. B11은 실제 링크 공유 화면이 아니라 로그인 회원 기능을 설명하는 화면이다.

B09 돌아가기 → 이전 화면, 기본 활동 지역 월계1동으로 돌아가기 → B01. B09에서 하계2동 인증을 신청하는 연결은 없다.

B10 원문 보기 → B06, 닫기 → 이전 화면. B08의 하계2동 말풍선·대표 게시물 → B06.

### 5.3 하계2동 검색

B12 검색 → B13 결과 있음 또는 B14 결과 없음. 검색어 예시는 보행로와 백화점을 클릭으로 전환한다. 결과 카드 → B06. 검색의 지역 선택·메인·지도·글쓰기 연결은 하계2동 맥락을 유지한다.

이는 자유 텍스트 검색 엔진이 아니라 두 검색어를 사용하는 Prototype 시연이다. 필터 항목의 실제 선택 상태 변경은 화면별 연결 부록에서 구분한다.

## 6 C 통합 게시판 검색 추천

### 6.1 게시판 열람

C01에서 월계1동 지역 변경 → B02, 검색 → C02, 추천 카드 → F01, 지역 활동 카드 → F03, 투표 카드 → F04.

현재 게시판 하단 지역 게시물 영역에는 AI 요약 본문과 원문 보기 버튼이 포함되어 있다. 별도 C04 요약 팝업으로 이동하는 연결은 현재 목록에 없다. C01 원문 보기 → F01.

게시물 유형과 주제 항목은 화면에 표시되지만 C01에서 이 항목들의 필터 동작은 연결되지 않았다.

### 6.2 새 추천

C01 새 추천 → C01 새추천성공. 추천 제목은 “광운대역 야간 조명 개선”으로 바뀐다. 새 추천 성공 상태의 추천 카드 → F19, F19 AI 요약 → F23, F23 원문 보기 → F19.

새 추천 성공 상태에서 새 추천을 다시 누르면 후보 없음 안내 변수를 true로 바꾸고 같은 화면을 유지한다. 메시지 표시 레이어의 현재 구성은 부록에 기록한다.

새 추천 성공 화면 하단 요약은 보행로 개선을 설명하지만 원문 보기 목적지는 F19 야간 조명 개선이다. 문구와 목적지의 차이는 확인 필요다.

### 6.3 검색 결과 분기

C02 검색어 입력 클릭은 보행로 ↔ 백화점 예시를 전환한다. 검색 시 백화점이면 결과 없음, 그 외에는 결과 있음으로 연결된다.

결과 있음 화면의 카드 → F01. 결과 없음에서 입력 영역을 클릭하면 보행로로 바꾸고 C02 입력 화면으로 돌아갈 수 있다. 현재 화면의 정적 텍스트와 검색어 변수 표시가 다를 수 있으므로 부록에서 바인딩을 구분한다.

## 7 D 이슈 지도

D01은 동 경계와 대표 의제 말풍선으로 탐색하는 전체 지도다.

| 지도 말풍선 | 연결 상세 |
| --- | --- |
| 월계1동 광운대역 보행로 개선 | F01 지역안건상세 사진있음 |
| 월계2동 월계도서관 공사 현황 | F16 월계2동안건 열람전용 |
| 월계3동 스타필드 입점 | F17 월계3동안건 열람전용 |
| 하계1동 과기대 독서 모임 | F18 하계1동 독서모임 열람전용 |

월계2동·월계3동 상세의 AI 요약은 각각 F21/F22이며 원문 보기는 같은 상세로 돌아간다. 타지역 참여 제한은 F20과 일부 B09 연결로 표현되어 있으므로 지역명이 맞는지 추가 확인이 필요하다.

D03은 지도 로딩 실패 상태다. 다시 시도 → D01. 일반 모바일 화면에서 D03으로 실패 분기가 연결되어 있지는 않으며, 화면 밖 작업용 예외 시연 시작점에서 접근할 수 있다.

## 8 E 작성 사진 수정 삭제

### 8.1 작성 유형

E01 → 지역 안건 E02 / 지역 활동 정보 E03 / 투표 E04. 화면에는 게시 지역의 이웃 인증이 필요하다고 표시되지만, 유형 버튼은 조건 검사 없이 해당 작성 프레임으로 연결된다.

| 유형 | 화면에 표시된 주요 입력 |
| --- | --- |
| 지역 안건 E02 | 지역, 주제, 제목, 본문, 사진, 참고 링크, 익명 게시 |
| 지역 활동 정보 E03 | 지역, 주제, 제목, 본문, 출처, 일정, 장소, 활동 상태, 외부 참여 링크, 참고 링크, 사진 |
| 투표 E04 | 지역, 주제, 제목, 본문, 질문, 선택지, 종료 일시, 선택적 종료 전 알림·알림 시점, 참고 링크, 사진 |

E03 활동 상태는 예정·진행·종료·취소이며 기본값 없음으로 표시된다. E04는 선택지 2~10개, 10개에서 추가 비활성 문구가 있다. 추가·삭제 버튼의 상태 변경 연결은 현재 확인되지 않는다.

### 8.2 임시 저장과 게시

E02/E03/E04 임시 저장 → 각각 메시지 표시 변수 true, 화면 이동 없음. 실제 저장된 초안을 다시 여는 화면·경로는 현재 확인되지 않는다.

게시하기의 현재 목적지는 각각 E02 게시완료, E03 게시완료, E04 게시완료다. 완료 화면에는 등록 완료 문구만 있고 후속 버튼이나 자동 이동 연결이 없다. 기존 외부 주석의 F01/F03/F04 상세 이동 설명과 다르다.

### 8.3 사진 첨부

E02/E03/E04/E06/E09 사진 버튼 → E05. E05 작성 화면으로 및 뒤로 → 이전 화면. 고정된 한 작성 화면으로 돌아가는 것이 아니다.

화면에는 카메라·갤러리, JPG·PNG, 최대 10장, 전체 합계 10MB, 미리보기·교체·제거가 있다. 카메라·갤러리 실행과 파일 선택, 교체·제거 동작은 연결되지 않았다. 현재 E05에는 AI 썸네일 제작 선택지나 별도 제작 프레임이 없다. 과거 대화의 기능을 이 문서에 추가하지 않는다.

### 8.4 내가 만든 게시물 수정

현재 연결된 수정 순서는 다음과 같다.

- 지역 안건: I03 목록 → F24 내게시물상세 지역안건 → 화면 아래 게시물 수정 → E09 내지역안건 수정 → 변경 저장 → F24.
- 진행 중 투표: I03 목록 → F25 내게시물상세 진행중투표 → 화면 아래 게시물 수정 → E06 진행중투표 수정 → 변경 저장 → F25.

I03의 지역 안건·투표 필터 상태에서도 같은 상세로 이동한다. 목록에서 바로 수정 화면으로 이동하지 않는다. 현재 내가 만든 지역 활동 정보 필터에는 수정으로 이어지는 본인 활동 상세 경로가 확인되지 않는다.

E09는 기존 제목·본문을 보여주고 저장 시 F24로 복귀한다. 입력한 모든 필드가 상세에 동적으로 반영되는 저장 시연은 확인되지 않는다.

E06은 지역·주제·질문·선택지 수정 불가를 표시한다. 제목·본문·사진·링크·종료 일시·종료 전 알림은 수정 항목으로 표시된다. 변경 저장은 투표 제목 변수를 “보행 환경 우선순위”로 바꾸고 F25로 이동한다. 종료 일시 변경 시 기존 알림 예약 취소 문구가 있으나 실제 예약 처리는 시연하지 않는다.

### 8.5 투표 삭제

E06 삭제 → E08 삭제 확인.

확인 → 본인 투표 표시 false, 삭제 메시지 표시 변수 true → E04 투표 작성. 취소 → E06. 현재 삭제 확인 후 I03 목록이나 이전 상세로 복귀하는 연결은 아니다. 삭제 메시지 변수는 변경되지만 목적지 E04에서 어떤 메시지가 보이는지는 별도 확인이 필요하다.

## 9 F 상세 참여 결과

### 9.1 상세 종류

F01 사진 있는 지역 안건, F02 사진 없는 지역 안건, F03 지역 활동 정보, F04 진행 중 투표가 기본 상세다. F19는 새 추천 상세, F24/F25는 본인 게시물 관리 진입점을 가진 상세다.

현재 F01/F02와 F19의 일부 요약은 상세 본문 안에 표시된다. 지도 안건 F16/F17은 별도 F21/F22로 이동한다. 모든 상세의 AI 요약이 같은 팝업 방식이라고 일반화하지 않는다.

사진 유무와 하단 메뉴 표시도 화면마다 다르다. 특히 F01의 하단 회원 내비게이션은 현재 숨김 상태다. 숨겨진 레이어에 이동 연결이 남아 있어도 사용자에게 보이는 버튼으로 취급하지 않는다.

### 9.2 북마크 댓글 공유 신고

상세의 북마크·댓글 등록은 주로 메시지 표시 변수를 변경한다. 메시지 레이어가 있으면 제자리 안내를 의도한 구조이며, 바인딩이 없는 화면에서는 변수 변경만 확인된다. 메시지 출력과 데이터 저장 완료는 서로 다르다.

공유는 주로 G04 링크 복사로 이동한다. 하계2동·월계2동·월계3동 일부 상세의 공유는 B11 일반 회원 기능 안내로 이동한다.

신고 → 원문 화면 변수를 설정하고 F08 → 신고 제출 시 접수 메시지 내용·표시 변수 변경. 제출 후 별도 접수 완료 화면으로 이동하지 않는다. 실패 시연 시작점은 F08에 실패 메시지를 먼저 표시하고, 같은 제출 버튼이 성공 메시지로 바꾼다.

기본 상세의 공감해요·필요해요·궁금해요, 댓글 정렬·좋아요·싫어요·답글은 표시와 실제 연결을 구분한다. 현재 일반 상세에는 이 항목 중 연결되지 않은 것들이 있다. 부록의 클릭 기록이 없는 항목을 구현 완료로 보지 않는다.

### 9.3 진행 중 투표

F04에서 선택지 클릭 → 선택 초안과 선택 표시 변수 변경.

투표 제출:

- 선택 초안 없음 → 미선택 안내 변수 true, 화면 유지.
- 최초 제출 또는 기존 제출과 같은 선택 → 제출 선택·내 선택 표시 변수 변경 → F11 제출후.
- 기존 제출과 다른 선택 → 현재 F04에서는 목적지·상태 갱신을 하는 분기가 확인되지 않는다.

F11 다른 선택 제출은 현재 F04를 교체 표시하는 연결이다. 기존 주석의 F05 선택 변경 확인 화면은 현재 프레임 목록에 없다. F25에는 다른 선택일 때 목적지 null인 연결이 남아 있다. 투표 변경·확인 흐름은 확인 필요다.

F11에는 내 선택과 결과 표시, F09에는 종료 투표 집계가 있다. 52표·28표·80명 등은 시연 값이며 실제 집계 갱신은 아니다.

### 9.4 종료 활동과 타지역 상세

F12는 종료된 지역 활동 정보의 상태 예시다. 외부 참여 비활성·문의 정보 없음 상태를 표시한다. 일반 서비스 상세와 별도로 연결된 상태 예시 진입이 있으며, 취소 활동의 독립 프레임은 없다.

F16/F17/F18은 타지역 열람 전용 상세다. 반응·댓글 등록은 F20 제한 안내로 연결된다. 다만 F16/F17 댓글 평가·답글 영역은 B09 하계2동 제한 안내로 연결되어 지역명이 달라진다. 이 연결은 문서에서 정상 정책으로 재해석하지 않는다.

F07 북마크 저장 성공 안내는 G05/G09에서 사용한다. 확인·뒤로 → 이전 화면. A~F의 제자리 상태 표시와 별도 화면 방식이 함께 존재한다.

## 10 G 공유 게스트 로그인 복귀

### 10.1 공유 게시물 게스트

G01 → 공개 본문·사진·댓글 확인. AI 요약 → G06 → 공유받은 원문 보기 → G01; 닫기 → 이전 화면.

게스트 댓글 등록은 G01의 등록 안내 변수를 true로 변경한다. 입력 영역 클릭은 안내를 숨긴다. 답글 → 원문이 일반 게시물임을 설정 → G07. 게스트로 등록 → 원문별 안내 변수 변경 → 이전 화면.

공감·필요·궁금, 북마크, 신고, 댓글 좋아요·싫어요는 복귀 정보를 일반 게시물로 설정하고 G02로 이동한다. 공유 → 복사 안내를 초기화한 뒤 G04.

### 10.2 공유 투표 게스트

G08에서 공개 집계·댓글 확인. 게스트 댓글 등록과 답글은 로그인 없이 안내 상태를 시연한다.

투표 참여 또는 북마크 → 원문 있음 true, 원문 투표 true → G02. 로그인 완료 → G09. 이 과정은 투표를 자동 제출하지 않는다.

### 10.3 로그인과 가입을 거치는 복귀

G02 로그인은 A02 원본과 같은 복귀 조건을 사용한다. 회원가입 → A06 → 인증·프로필·활동 지역 설정 → 마지막 버튼에서 G05/G09. 비밀번호 복구 → A02 원본 로그인 → 같은 복귀 조건.

G05 북마크 → F07; 반응·이웃 인증하기 → J01. G09 북마크 → F07; 투표 참여·이웃 인증하기 → J01. 두 복귀 화면은 지역 미인증 회원 상태이며 게스트 댓글 예외와 다르다.

G05/G09의 뒤로는 로그인 화면으로 가지 않고 복귀 변수를 초기화한 뒤 B01로 이동한다.

G04 링크 복사는 안내 변수 변경이며 실제 클립보드 복사 동작은 아니다. 닫기·뒤로는 이전 화면으로 돌아간다.

## 11 H 알림과 활동

H01 전체 → 알림 탭 H01 알림 / 활동 탭 H01 활동. 현재 알림·활동 상태에서 전체·다른 탭으로 돌아가는 탭 연결은 확인되지 않는다.

연결된 카드 경로:

| 카드 | 목적지 |
| --- | --- |
| 새 댓글 | F01 |
| 투표 종료 전 알림 | F04 |
| 기관 채택 취소 | K04 채택 기록 |
| 게시물 삭제 | H03 |

새 반응, 관심 지역의 새 의제, 투표 결과 카드도 표시되지만 클릭 이동은 확인되지 않는다. H01 활동 프레임에도 같은 알림성 카드 문구가 있어 활동 기록의 구분은 추가 확인이 필요하다.

H02는 빈 상태다. 일반 모바일 화면에서 진입하는 연결은 없고 화면 밖 상태 예시에서 확인할 수 있다. H03 알림으로 → H01. 삭제된 콘텐츠를 다시 여는 경로는 없다.

## 12 I 마이페이지와 개인 기록

### 12.1 마이페이지 진입

I01에서 북마크 I02, 내가 만든 게시물 I03, 반응한 게시물 I04, 참여한 투표 I05, 프로필 I06, 활동 지역 I07, 관심 지역 I08, 이웃 인증 J01, 기관 인증 J03, 계정 L02 마이페이지진입, 알림 H01, 설정 L01로 이동한다.

화면에는 작성·북마크·반응·댓글·평가·투표 숫자가 있다. 집계 취소·재등록·전환 규칙은 외부 주석의 참고 정책이며 현재 동적 계산 시연은 아니다.

### 12.2 개인 기록 목록

I02는 전체 목록 외에 유형 3개와 주제 7개의 필터 프레임이 있다. 선택한 필터 상태로 이동하고 카드에서 해당 상세로 연결된다. 복합 필터 저장 상태나 상세 복귀 후 조건 유지까지는 Prototype 실행 검증을 하지 않았다.

I03는 전체 및 지역 안건·지역 활동 정보·투표 상태가 있다. 지역 안건 → F24, 진행 중 투표 → F25. 본인 수정 흐름은 8.4절과 같다.

I04는 유형별 상태와 공감해요·필요해요·댓글 작성·댓글 좋아요싫어요 상태가 있다. 전체 화면에는 궁금해요도 표시되지만 독립된 궁금해요 필터 프레임과 연결은 없다. 게시물별 현재 참여 표시를 보여주며 카드에서 상세로 이동한다.

I05는 전체·진행 중·종료 상태. 진행 중 투표 → F04, 종료 기록 → F09. 화면에는 실제 내 선택과 최다 선택을 구분하는 표시가 있다.

### 12.3 프로필과 지역 설정

I06 저장 또는 취소 → I01, 이웃 인증 → J01. 프로필 사진·닉네임·한 줄 소개·복수 속성의 입력 UI가 있다.

I07 지역 검색을 포함한 큰 콘텐츠 영역 또는 현재 위치로 찾기 → I07 지역선택 상태 → 설정하기 → I01. 지역선택 상태에는 월계1동과 월계2동에 “선택됨”이 동시에 표시되며 복귀한 마이의 활동 지역은 월계1동으로 남는다. 실제 선택·저장 결과는 확인 필요다.

I08 콘텐츠 영역 클릭 → 추가됨 → 콘텐츠 영역 클릭 → 제거됨 → 다시 추가됨. 돌아가기 → I01. 개별 지역 행 대신 큰 콘텐츠 영역에 상태 전환이 연결되어 있다. “즉시 반영, 0개 가능” 문구는 화면 표시이며 실제 저장을 확인한 것은 아니다.

관심 지역의 권한·알림 영향, 빈 목록 처리 규칙은 화면 밖 참고 주석에 따로 보존한다.

## 13 J 이웃 인증과 기관 인증

### 13.1 이웃 인증

A08, I01, I06, G05/G09 등의 인증 진입 → J01 거주 지역·증빙 제출 → J02 접수 → 프로필로 → I06.

J02는 “최종 제품에서는 제출 후 1주일 이내 승인 여부 안내, 접수만으로 인증 완료 아님”을 표시한다. 현재 접수에서 권한이 부여되거나 승인 화면으로 이동하는 연결은 없다.

증빙 종류·인증 최대 지역 수 등은 외부 주석의 참고사항이며 실제 UI에서 확정한 정책으로 확대하지 않는다.

### 13.2 기관 인증

I01 기관 인증 → J03 기관명·부서·직책·이름·업무 이메일·전화번호·담당 지역 → 재직증명서 첨부 → J04 → 자료 제출 → J05 접수 → 마이페이지 I01.

J04에는 PDF·JPG·PNG, 파일당 10MB, 요청 합계 50MB, 개수 제한 없음이 표시된다. J05는 1주일 이내 자격 검토와 제출·완료의 차이를 설명한다. 실제 운영자 승인·반려·보완 요청, 기관 배지·K 권한 부여는 연결되지 않는다.

기관 인증 1년 유효 등의 내용은 외부 주석에만 있으므로 참고 정책으로 분리한다.

## 14 K 기관 담당자 안건 채택

K01 전체안건 → 채택한 게시물 상태 또는 월계1동 필터 상태. 지역 필터 클릭은 월계1동 상태로 이동한다.

담당 지역 안건 카드 → K02 → 이 안건 채택 → K04. K04 본 기관 채택 취소 → K02, 담당자 목록으로 → K01 전체안건.

타지역 카드 → K03. 화면은 “채택 불가 · 담당 지역 밖”을 표시하며 전체 안건 목록 → K01. 채택은 실제 문제 해결 완료를 뜻하지 않는다는 설명은 외부 주석의 참고 정책이다.

K04에는 기관명·채택 시각과 다른 기관 기록이 표시된다. 본 기관 관계만 취소하는 UI가 있다. 채택 목록 필터의 서버 저장·자동 갱신은 확인되지 않는다.

## 15 L 설정 계정 알림 탈퇴

### 15.1 설정에서 시작

L01 → 계정 관리 L02, 내가 쓴 글 I03, 댓글 남긴 글 I04, 스크랩한 글 I02, 관심 키워드 L07, 알림 설정 L08, 로그아웃 A01, 탈퇴 L09.

“댓글 남긴 글” 메뉴가 댓글 작성만의 필터가 아니라 I04 전체 반응한 게시물로 연결되는 점은 현재 연결 그대로 기록한다.

### 15.2 이메일 변경

설정 경로: L02 → L03 현재 비밀번호·새 이메일 → 인증번호 발송 → L04 → 인증 후 이메일 변경 → L02 이메일변경완료.

L04 재발송 → L04 재발송 상태 → 인증 후 이메일 변경 → 같은 완료 상태. 완료 화면 뒤로 → L01.

마이 경로: I01 계정 → L02 마이페이지진입 → L03 마이페이지진입 → L04 마이페이지진입 → L02 이메일변경완료 마이페이지진입 → 뒤로 I01. 재발송도 마이 경로의 전용 상태를 사용한다.

발송 실패, 인증 불일치·만료, 현재 비밀번호 불일치, 이미 사용 중인 이메일 상태 프레임은 존재하지만 일반 모바일 흐름에서 이 오류로 진입하는 연결은 없다. 다수 오류 프레임에서 재시도·재발송 버튼의 복구 연결도 없다.

### 15.3 비밀번호 변경

설정 경로: L02 → L05 인증 발송 → L05 발송완료 → 확인 → L06 → 비밀번호 변경 → L01.

마이 경로: I01 → L02 마이페이지진입 → L05 마이페이지진입 → 발송완료 마이페이지진입 → 확인 → L06 마이페이지진입 → 변경 → I01.

발송완료에서 재발송 → 재발송 상태, 확인 → 해당 경로의 L06. 형식 오류·확인 불일치·인증 불일치·만료·발송 실패 프레임은 존재한다. 오류를 자동 판단하는 연결이나 복구 클릭은 별도로 확인해야 한다.

### 15.4 관심 키워드와 알림

L07은 최대 4개 선택 안내와 현재 2개 선택 예시다. 저장 → L01. 4개선택 및 저장실패 프레임은 존재하지만 일반 흐름으로 연결되지 않는다.

L08의 ON 클릭은 OFF 컴포넌트로 상태 변경된다. 화면에는 OFF여도 서비스 내 알림·활동 기록 유지, ON에서 푸시 1회 시도·실패 자동 재전송 없음이 표시된다. 실제 기기 푸시 설정 변경은 아니다.

### 15.5 회원 탈퇴

L09 유지·삭제 정보 확인 → 현재 비밀번호 확인 → L10 → 다음 → L11 최종 확인 → L12 회원이용종료 → 로그인 화면 A02 원본.

L09/L10/L11 취소 → L01. 현재 비밀번호 불일치 프레임은 별도 존재하지만 정상 경로에서 오류 판정으로 진입하지 않는다.

L09 화면에 표시된 처리 내용:

- 게시물·댓글·답글 유지, 작성자를 회원 탈퇴한 사용자로 표시. 기존 익명 게시물은 익명 유지.
- 북마크·개인 설정 삭제, 활동 게시물 문의 이메일 제거.
- 반응·평가·투표 집계 유지, 회원 식별 연결 제거.

위 내용은 현재 안내 UI이며 실제 데이터 처리 실행을 검증한 것은 아니다.

## 16 상태 표시와 작업용 주석

### 16.1 제자리 상태와 별도 상태 프레임

임시 저장, 일반 상세의 북마크·댓글 등록, 신고 접수·실패, 게스트 댓글·답글 등록, 링크 복사 등은 변수 변경으로 같은 화면에 안내를 표시하도록 설정된 동작이 있다.

변수를 바꾸는 버튼이 있다고 해서 안내가 실제로 출력된다고 단정하지 않는다. 연결 부록에 표시 바인딩을 함께 기록한다. 현재 일부 상태 메시지 인스턴스는 자식 텍스트가 없어 문구 출력 확인이 필요하다.

작성 게시 완료 3개, G05/G09에서 쓰는 F07 북마크 성공, 인증·검색·필터·오류 상태는 별도 모바일 프레임으로 존재한다. 제품에서 반드시 별도 페이지여야 한다는 뜻은 아니다.

### 16.2 스크롤

다수 화면은 내부 “세로 스크롤 뷰포트”에 VERTICAL 스크롤과 넘친 콘텐츠 숨기기를 적용한다. 화면 번호와 외부 주석은 스크롤 UI에 포함하지 않는다. 세로 스크롤 설정은 긴 콘텐츠가 반드시 있다는 뜻과 다르며, 각 프레임의 실제 설정은 부록에 적는다.

### 16.3 디자인에서 제외할 메타정보

“작업용 주석 · 관련 상태 · 디자인 제외”는 화면 설명용이며 실제 디자인·구현에 포함하지 않는다. 기존 화면 설명, UF 참고, 예외 시연 시작점도 제품 메뉴로 재현하지 않는다.

관련 상태의 정보는 보존하되, 입력 필드·사진 영역·카드·실제 상태 메시지 같은 회색 UI와 색상만으로 구분하지 않는다. 전체 주석 원문은 별도 부록에 기록한다.

화면 밖 A~F 예외 시연 시작점은 지도 로딩 실패, 댓글 차단, 신고 실패, 새 추천 후보 없음, 검색 결과 없음을 보여주는 테스트용 진입이다.

## 17 확인 필요 항목

아래는 현재 구조에서 확인된 차이이며 이번 문서 작성 중 Figma를 수정하지 않았다.

| 항목 | 현재 확인한 내용 | 팀에서 확인할 사항 |
| --- | --- | --- |
| 게시 후 이동 | E02/E03/E04 → 각 게시완료 프레임; 이후 연결 없음 | 완료 화면이 종착점인지, 상세·목록으로 이어지는지 |
| 기존 게시 주석 | E02/E03/E04 주석은 F01/F03/F04 이동을 설명 | 실제 연결에 맞춰 주석을 갱신할지 |
| 투표 변경 | F11 다른 선택 제출은 F04 교체 표시; F04에는 다른 기존 선택 제출 분기 없음 | 변경·확인·취소 방식과 동작 |
| 본인 투표 변경 | F25 투표 제출의 다른 선택 분기에 목적지 null | 제거된 목적지 대신 어떤 동작을 사용할지 |
| 타지역 댓글 평가 | F16/F17 댓글 평가·답글 클릭 → 하계2동 B09 | 월계2·3동 대상에서도 하계2동 안내가 맞는지 |
| 추천 요약 원문 | C01 새추천성공의 보행로 요약 원문 보기 → 야간 조명 F19 | 요약 대상과 이동 대상을 일치시킬지 |
| 로그인 중복 | A02 원본과 복제본 이름 같고 로그인 조건 다름 | 복제본의 용도와 유지 여부 |
| 가입 중 이웃 인증 | A08 → J01 → J02 → I06 | 가입 중 프로필로 돌아올지, 마이 프로필로 갈지 |
| 상태 메시지 | F01의 북마크·댓글 버튼은 변수 변경만; 표시 바인딩 없음. 일부 다른 화면의 메시지 인스턴스는 자식 텍스트 없음 | 안내 문구가 실제로 보이는지 |
| 활동 지역 선택 | I07 선택 상태에 월계1동·월계2동 모두 선택됨; 복귀 I01은 월계1동 | 단일 선택 및 변경 결과 표현 |
| 관심 지역 조작 | I08의 큰 콘텐츠 영역 클릭으로 추가·제거 상태 전환 | 각 지역 행에 대응하는 조작 방식 |
| 알림 탭 복귀 | H01 알림·활동 상태에서 탭간 복귀 연결 없음 | 전체·알림·활동 왕복 경로 |
| 추가 알림 카드 | 반응·관심 지역·투표 결과 카드에는 클릭 연결 없음 | 각 카드의 목적지 |
| 오류 복구 | L 오류 상태는 정상 화면에서 오류 진입 연결 없고 재시도 연결도 다수 없음 | 상태 예시만 둘지, 실패·복구를 시연할지 |
| 기본 상세 참여 | F01~F04 반응·댓글 평가·정렬 등의 일부 UI에 동작 연결 없음 | 표시만의 와이어프레임인지 동작 시연 대상인지 |
| 본인 활동 수정 | I03 지역 활동 정보 상태에 본인 활동 상세·수정 경로 없음 | 안건·투표 외 수정도 필요한지 |
| 그룹 설명 | A 헤더 13개이나 A02 중복 포함 14개; H 헤더 4개이나 실제 5개 | 그룹 포함 범위와 화면 수 |
| 화면과 주석의 차이 | 일부 H 주석 이름·설명이 실제 빈 상태·삭제 안내와 다름 | 현재 화면 역할에 맞는 주석 구분 |
| 설정 메뉴 의미 | L01 댓글 남긴 글 → I04 전체 반응한 게시물 | 메뉴 문구와 목적 목록 범위 |
| 작성 권한 | E01은 이웃 인증 필요 문구를 표시하지만 작성 화면 진입에 조건 없음 | 안내만 표현한 범위와 권한 검증 범위 |


## 부록 A 전체 화면 목록

이 목록의 화면 수는 이름의 A~L 문자로 분류한 실제 모바일 프레임 수다. 헤더에 적힌 개수를 재사용하지 않았다. 동일한 번호의 상태 프레임도 한 개씩 센다.

| 섹션 | 역할 | 실제 프레임 수 |
| --- | --- | ---: |
| A | 시작 로그인 가입 복구 | 14 |
| B | 메인 지역 탐색 | 14 |
| C | 통합 게시판 검색 추천 | 5 |
| D | 이슈 지도 | 2 |
| E | 작성 사진 수정 삭제 | 11 |
| F | 상세 참여 결과 | 19 |
| G | 공유 게스트 로그인 복귀 | 8 |
| H | 알림 활동 | 5 |
| I | 마이 개인 기록 | 33 |
| J | 이웃 기관 인증 | 5 |
| K | 기관 담당자 | 6 |
| L | 설정 계정 탈퇴 | 38 |
| 합계 | A~L 전체 | 160 |

### A 시작 로그인 가입 복구

| 현재 화면 이름 | Figma 노드 ID |
| --- | --- |
| [WF-A01-시작](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=689-1077) | 689:1077 |
| [WF-A02-로그인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-923) | 692:923 |
| [WF-A02-로그인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=812-5765) | 812:5765 |
| [WF-A03-비밀번호찾기](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1098) | 692:1098 |
| [WF-A04-비밀번호복구-본인확인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1123) | 692:1123 |
| [WF-A04-비밀번호복구-본인확인-재발송](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=734-3947) | 734:3947 |
| [WF-A05-비밀번호재설정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1148) | 692:1148 |
| [WF-A06-회원가입-계정정보·약관](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-948) | 692:948 |
| [WF-A07-회원가입-이메일인증-발송](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=715-3556) | 715:3556 |
| [WF-A07-회원가입-이메일인증-완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=734-4072) | 734:4072 |
| [WF-A07-회원가입-이메일인증-재발송](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=734-3993) | 734:3993 |
| [WF-A08-회원가입-프로필설정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1048) | 692:1048 |
| [WF-A08-회원가입-프로필설정-활동지역설정완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=741-4230) | 741:4230 |
| [WF-A09-회원가입-활동지역설정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1073) | 692:1073 |

### B 메인 지역 탐색

| 현재 화면 이름 | Figma 노드 ID |
| --- | --- |
| [WF-B01-메인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1173) | 692:1173 |
| [WF-B02-탐색지역선택](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1198) | 692:1198 |
| [WF-B03-메인-하계2동-이웃미인증](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=726-3724) | 726:3724 |
| [WF-B04-탐색지역선택-하계2동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-5837) | 761:5837 |
| [WF-B05-통합게시판-하계2동-열람전용](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-5903) | 761:5903 |
| [WF-B06-지역안건상세-하계2동-열람전용](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-6013) | 761:6013 |
| [WF-B07-투표결과-하계2동-이웃미인증](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-6110) | 761:6110 |
| [WF-B08-이슈지도-하계2동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-6214) | 761:6214 |
| [WF-B09-하계2동-지역참여제한안내](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-6291) | 761:6291 |
| [WF-B10-AI요약-하계2동안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-6357) | 761:6357 |
| [WF-B11-하계2동-일반회원기능안내](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-6395) | 761:6395 |
| [WF-B12-검색-하계2동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4085) | 789:4085 |
| [WF-B13-검색결과있음-하계2동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4166) | 789:4166 |
| [WF-B14-검색결과없음-하계2동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4253) | 789:4253 |

### C 통합 게시판 검색 추천

| 현재 화면 이름 | Figma 노드 ID |
| --- | --- |
| [WF-C01-새추천성공](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=743-4650) | 743:4650 |
| [WF-C01-통합게시판](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1248) | 692:1248 |
| [WF-C02-검색-필터](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1273) | 692:1273 |
| [WF-C02-검색결과없음](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=743-4385) | 743:4385 |
| [WF-C02-검색결과있음](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=743-4291) | 743:4291 |

### D 이슈 지도

| 현재 화면 이름 | Figma 노드 ID |
| --- | --- |
| [WF-D01-전체지도](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1373) | 692:1373 |
| [WF-D03-지도-로딩실패](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1423) | 692:1423 |

### E 작성 사진 수정 삭제

| 현재 화면 이름 | Figma 노드 ID |
| --- | --- |
| [WF-E01-글쓰기-유형선택](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1448) | 692:1448 |
| [WF-E02-글쓰기-지역안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1473) | 692:1473 |
| [WF-E02-글쓰기-지역안건-게시완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1116-6196) | 1116:6196 |
| [WF-E03-글쓰기-지역활동정보](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1498) | 692:1498 |
| [WF-E03-글쓰기-지역활동정보-게시완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1116-6244) | 1116:6244 |
| [WF-E04-글쓰기-투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1523) | 692:1523 |
| [WF-E04-글쓰기-투표-게시완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1116-6303) | 1116:6303 |
| [WF-E05-사진-첨부미리보기](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1548) | 692:1548 |
| [WF-E06-진행중투표-수정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1573) | 692:1573 |
| [WF-E08-본인투표-삭제확인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=791-4640) | 791:4640 |
| [WF-E09-내지역안건-수정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=895-4619) | 895:4619 |

### F 상세 참여 결과

| 현재 화면 이름 | Figma 노드 ID |
| --- | --- |
| [WF-F01-지역안건상세-사진있음](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1598) | 692:1598 |
| [WF-F02-지역안건상세-사진없음](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1623) | 692:1623 |
| [WF-F03-지역활동정보상세](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1648) | 692:1648 |
| [WF-F04-투표상세-진행중](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1673) | 692:1673 |
| [WF-F07-북마크-저장성공](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1748) | 692:1748 |
| [WF-F08-신고-입력](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1773) | 692:1773 |
| [WF-F09-투표-종료결과](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1798) | 692:1798 |
| [WF-F11-투표-제출후](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=695-3309) | 695:3309 |
| [WF-F12-지역활동정보상세-종료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=695-3334) | 695:3334 |
| [WF-F16-월계2동안건-열람전용](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4412) | 789:4412 |
| [WF-F17-월계3동안건-열람전용](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4562) | 789:4562 |
| [WF-F18-하계1동-독서모임-열람전용](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4712) | 789:4712 |
| [WF-F19-지역안건-새추천상세](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4831) | 789:4831 |
| [WF-F20-탐색지역-참여제한안내](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4356) | 789:4356 |
| [WF-F21-AI요약-월계2동-지도안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4516) | 789:4516 |
| [WF-F22-AI요약-월계3동-지도안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4666) | 789:4666 |
| [WF-F23-AI요약-월계1동-새추천](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4926) | 789:4926 |
| [WF-F24-내게시물상세-지역안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=895-4421) | 895:4421 |
| [WF-F25-내게시물상세-진행중투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=895-4511) | 895:4511 |

### G 공유 게스트 로그인 복귀

| 현재 화면 이름 | Figma 노드 ID |
| --- | --- |
| [WF-G01-공유-게스트상세](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1848) | 692:1848 |
| [WF-G02-로그인-원게시물복귀](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=812-6012) | 812:6012 |
| [WF-G04-공유-링크복사](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1923) | 692:1923 |
| [WF-G05-로그인복귀-미인증회원상세](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1948) | 692:1948 |
| [WF-G06-게스트-AI요약](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=695-3384) | 695:3384 |
| [WF-G07-게스트-답글작성](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=695-3409) | 695:3409 |
| [WF-G08-게스트-공유투표결과](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=695-3434) | 695:3434 |
| [WF-G09-로그인복귀-미인증회원투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=699-3496) | 699:3496 |

### H 알림 활동

| 현재 화면 이름 | Figma 노드 ID |
| --- | --- |
| [WF-H01-알림및활동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1973) | 692:1973 |
| [WF-H01-알림및활동-알림](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1034-6049) | 1034:6049 |
| [WF-H01-알림및활동-활동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1034-6101) | 1034:6101 |
| [WF-H02-알림및활동-빈상태](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1998) | 692:1998 |
| [WF-H03-삭제된게시물안내](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2023) | 692:2023 |

### I 마이 개인 기록

| 현재 화면 이름 | Figma 노드 ID |
| --- | --- |
| [WF-I01-마이페이지](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2048) | 692:2048 |
| [WF-I02-북마크-유형-지역안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-4825) | 947:4825 |
| [WF-I02-북마크-유형-지역활동정보](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-4868) | 947:4868 |
| [WF-I02-북마크-유형-투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-4911) | 947:4911 |
| [WF-I02-북마크-주제-교통](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-4954) | 947:4954 |
| [WF-I02-북마크-주제-기타](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-5212) | 947:5212 |
| [WF-I02-북마크-주제-복지](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-5083) | 947:5083 |
| [WF-I02-북마크-주제-생활정보](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-5126) | 947:5126 |
| [WF-I02-북마크-주제-안전](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-5040) | 947:5040 |
| [WF-I02-북마크-주제-주거](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-4997) | 947:4997 |
| [WF-I02-북마크-주제-환경](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-5169) | 947:5169 |
| [WF-I02-북마크목록](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2073) | 692:2073 |
| [WF-I03-내가만든게시물](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2098) | 692:2098 |
| [WF-I03-내가만든게시물-지역안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=953-5241) | 953:5241 |
| [WF-I03-내가만든게시물-지역활동정보](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=953-5271) | 953:5271 |
| [WF-I03-내가만든게시물-투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=953-5301) | 953:5301 |
| [WF-I04-반응한게시물](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2123) | 692:2123 |
| [WF-I04-반응한게시물-공감해요](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5607) | 959:5607 |
| [WF-I04-반응한게시물-댓글작성](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5685) | 959:5685 |
| [WF-I04-반응한게시물-댓글좋아요싫어요](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5724) | 959:5724 |
| [WF-I04-반응한게시물-지역안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5490) | 959:5490 |
| [WF-I04-반응한게시물-지역활동정보](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5529) | 959:5529 |
| [WF-I04-반응한게시물-투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5568) | 959:5568 |
| [WF-I04-반응한게시물-필요해요](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5646) | 959:5646 |
| [WF-I05-참여한투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2148) | 692:2148 |
| [WF-I05-참여한투표-종료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=961-5978) | 961:5978 |
| [WF-I05-참여한투표-진행중](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=961-5948) | 961:5948 |
| [WF-I06-프로필수정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2173) | 692:2173 |
| [WF-I07-기본활동지역](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2198) | 692:2198 |
| [WF-I07-기본활동지역-지역선택](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1034-6155) | 1034:6155 |
| [WF-I08-관심지역](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2223) | 692:2223 |
| [WF-I08-관심지역-제거됨](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1034-6225) | 1034:6225 |
| [WF-I08-관심지역-추가됨](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1034-6206) | 1034:6206 |

### J 이웃 기관 인증

| 현재 화면 이름 | Figma 노드 ID |
| --- | --- |
| [WF-J01-이웃인증-제출](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2248) | 692:2248 |
| [WF-J02-이웃인증-접수](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2273) | 692:2273 |
| [WF-J03-기관인증-정보](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2298) | 692:2298 |
| [WF-J04-기관인증-증빙](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2323) | 692:2323 |
| [WF-J05-기관인증-접수](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2348) | 692:2348 |

### K 기관 담당자

| 현재 화면 이름 | Figma 노드 ID |
| --- | --- |
| [WF-K01-담당자-월계1동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=938-4793) | 938:4793 |
| [WF-K01-담당자-전체안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2373) | 692:2373 |
| [WF-K01-담당자-채택게시물](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=938-4771) | 938:4771 |
| [WF-K02-담당지역-채택상세](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2398) | 692:2398 |
| [WF-K03-타지역-열람전용](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2423) | 692:2423 |
| [WF-K04-지역안건-채택기록](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2448) | 692:2448 |

### L 설정 계정 탈퇴

| 현재 화면 이름 | Figma 노드 ID |
| --- | --- |
| [WF-L01-설정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2473) | 692:2473 |
| [WF-L02-계정관리](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2498) | 692:2498 |
| [WF-L02-계정관리-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-5655) | 958:5655 |
| [WF-L02-계정관리-이메일변경완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4553) | 897:4553 |
| [WF-L02-계정관리-이메일변경완료-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-5744) | 958:5744 |
| [WF-L03-이메일변경](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2523) | 692:2523 |
| [WF-L03-이메일변경-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-5671) | 958:5671 |
| [WF-L03-이메일변경-발송실패](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4569) | 897:4569 |
| [WF-L03-이메일변경-이미사용중인이메일](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=955-5540) | 955:5540 |
| [WF-L03-이메일변경-현재비밀번호불일치](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=955-5497) | 955:5497 |
| [WF-L04-이메일변경-인증](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2548) | 692:2548 |
| [WF-L04-이메일변경-인증-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-5688) | 958:5688 |
| [WF-L04-이메일변경-인증-만료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4605) | 897:4605 |
| [WF-L04-이메일변경-인증-불일치](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4586) | 897:4586 |
| [WF-L04-이메일변경-인증-재발송](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4624) | 897:4624 |
| [WF-L04-이메일변경-인증-재발송-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-6234) | 958:6234 |
| [WF-L05-비밀번호변경-발송실패](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4662) | 897:4662 |
| [WF-L05-비밀번호변경-인증](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2573) | 692:2573 |
| [WF-L05-비밀번호변경-인증-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-5707) | 958:5707 |
| [WF-L05-비밀번호변경-인증-만료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4700) | 897:4700 |
| [WF-L05-비밀번호변경-인증-발송완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4643) | 897:4643 |
| [WF-L05-비밀번호변경-인증-발송완료-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-6282) | 958:6282 |
| [WF-L05-비밀번호변경-인증-불일치](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4681) | 897:4681 |
| [WF-L05-비밀번호변경-인증-재발송](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4719) | 897:4719 |
| [WF-L05-비밀번호변경-인증-재발송-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=978-6023) | 978:6023 |
| [WF-L06-비밀번호변경](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2598) | 692:2598 |
| [WF-L06-비밀번호변경-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-5726) | 958:5726 |
| [WF-L06-비밀번호변경-형식오류](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=955-5583) | 955:5583 |
| [WF-L06-비밀번호변경-확인불일치](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=955-5626) | 955:5626 |
| [WF-L07-관심키워드](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2623) | 692:2623 |
| [WF-L07-관심키워드-4개선택](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=955-5709) | 955:5709 |
| [WF-L07-관심키워드-저장실패](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4738) | 897:4738 |
| [WF-L08-알림설정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2648) | 692:2648 |
| [WF-L09-탈퇴-주의사항](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2673) | 692:2673 |
| [WF-L10-탈퇴-본인확인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2698) | 692:2698 |
| [WF-L10-탈퇴-본인확인-비밀번호불일치](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=955-5669) | 955:5669 |
| [WF-L11-탈퇴-최종확인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2723) | 692:2723 |
| [WF-L12-탈퇴완료-회원이용종료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2748) | 692:2748 |

## 부록 B 화면별 상세 연결

현재 숨김 상태인 요소는 사용자 조작 목록과 분리한다. 조건부 연결은 각 조건 블록의 순서대로 기록한다. 변수만 변경하는 동작은 서버 저장이나 이동이 아니다. 표시 바인딩은 레이어 구조의 존재를 뜻하며 실제 출력 문구·배치를 보장하지 않는다.

### A 화면별 연결

#### [WF-A01-시작](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=689-1077)

노드 ID: 689:1077. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: Discushion · 우리 동네의 이야기가, / 변화를 만듭니다. · 로그인 · 회원가입

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| 로그인 | ON_CLICK | 변수 returnTo 있음 ← false → 변수 returnTo 투표 ← false → 이동 → WF-A02-로그인 |
| 회원가입 | ON_CLICK | 변수 returnTo 있음 ← false → 변수 returnTo 투표 ← false → 이동 → WF-A06-회원가입-계정정보·약관 |

#### [WF-A02-로그인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-923)

노드 ID: 692:923. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 로그인 · 등록 이메일 · 이메일 주소 · 비밀번호 · 영문과 숫자를 포함한 8자 이상으로 입력해 주세요. · 비밀번호 찾기 · 로그인 · 회원가입

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 비밀번호 찾기 | ON_CLICK | 이동 → WF-A03-비밀번호찾기 |
| 로그인 | ON_CLICK | ((returnTo 있음 = true) 그리고 (returnTo 투표 = true))인 경우: 이동 → WF-G09-로그인복귀-미인증회원투표 → ((returnTo 있음 = true) 그리고 (returnTo 투표 = false))인 경우: 이동 → WF-G05-로그인복귀-미인증회원상세 → (returnTo 있음 = false)인 경우: 이동 → WF-B01-메인 |
| 회원가입 | ON_CLICK | 이동 → WF-A06-회원가입-계정정보·약관 |

#### [WF-A02-로그인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=812-5765)

노드 ID: 812:5765. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 로그인 · 등록 이메일 · 이메일 주소 · 비밀번호 · 영문과 숫자를 포함한 8자 이상으로 입력해 주세요. · 비밀번호 찾기 · 로그인 · 회원가입

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 비밀번호 찾기 | ON_CLICK | 이동 → WF-A03-비밀번호찾기 |
| 로그인 | ON_CLICK | ((returnTo 있음 = true) 그리고 (returnTo 투표 = true))인 경우: 이동 → WF-G09-로그인복귀-미인증회원투표; 그 외: 이동 → WF-B01-메인 |
| 회원가입 | ON_CLICK | 이동 → WF-A06-회원가입-계정정보·약관 |

#### [WF-A03-비밀번호찾기](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1098)

노드 ID: 692:1098. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 비밀번호 찾기 · 등록 이메일 · 이메일 주소 · 인증번호 발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 인증번호 발송 | ON_CLICK | 이동 → WF-A04-비밀번호복구-본인확인 |

#### [WF-A04-비밀번호복구-본인확인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1123)

노드 ID: 692:1123. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 본인 확인 · 등록 이메일 · abc123@example.com · 6자리 인증번호 · 123456 · 04:59 · 재발송 대기 60초 · 확인 · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 확인 | ON_CLICK | 이동 → WF-A05-비밀번호재설정 |
| 재발송 | ON_CLICK | 이동 → WF-A04-비밀번호복구-본인확인-재발송 |

#### [WF-A04-비밀번호복구-본인확인-재발송](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=734-3947)

노드 ID: 734:3947. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 본인 확인 · 등록 이메일 · abc123@example.com · 6자리 인증번호 · 123456 · 인증번호가 재발송되었습니다. · 04:59 · 재발송 대기 60초 · 확인 · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 확인 | ON_CLICK | 이동 → WF-A05-비밀번호재설정 |

#### [WF-A05-비밀번호재설정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1148)

노드 ID: 692:1148. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 새 비밀번호 · 새 비밀번호 · 8~64자 · 새 비밀번호 확인 · 다시 입력 · 영문과 숫자를 포함한 8자 이상으로 입력해 주세요. · 재설정 완료

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 재설정 완료 | ON_CLICK | 이동 → WF-A02-로그인 |

#### [WF-A06-회원가입-계정정보·약관](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-948)

노드 ID: 692:948. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 회원가입 · 이메일 · abc123@example.com · 비밀번호 · •••••••• · 비밀번호 확인 · •••••••• · 비밀번호 찾기 · 인증번호 발송 · 6자리 인증번호 · 123456 · 기관 인증 · 로그인 후 신청 · 진입 세부 미정 · 약관 동의 · □ 전체 동의 · □ 이용약관 동의 (필수) · □ 개인정보 수집·이용 동의 (필수) · □ 소식·이벤트/마케팅 수신 (선택) · 가입하고 프로필 설정

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 비밀번호 찾기 | ON_CLICK | 이동 → WF-A03-비밀번호찾기 |
| 인증번호 발송 | ON_CLICK | 이동 → WF-A07-회원가입-이메일인증-발송 |

#### [WF-A07-회원가입-이메일인증-발송](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=715-3556)

노드 ID: 715:3556. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 회원가입 · 이메일 · abc123@example.com · 비밀번호 · •••••••• · 비밀번호 확인 · •••••••• · 비밀번호 찾기 · 인증번호 발송 · 6자리 인증번호 · 123456 · 인증번호를 발송했습니다. · 남은 시간 04:59 · 재발송까지 60초 · 인증번호 확인 · 재발송 · 기관 인증 · 로그인 후 신청 · 진입 세부 미정 · 약관 동의 · □ 전체 동의 · □ 이용약관 동의 (필수) · □ 개인정보 수집·이용 동의 (필수) · □ 소식·이벤트/마케팅 수신 (선택) · 가입하고 프로필 설정

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 비밀번호 찾기 | ON_CLICK | 이동 → WF-A03-비밀번호찾기 |
| 인증번호 확인 | ON_CLICK | 이동 → WF-A07-회원가입-이메일인증-완료 |
| 재발송 | ON_CLICK | 이동 → WF-A07-회원가입-이메일인증-재발송 |

#### [WF-A07-회원가입-이메일인증-완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=734-4072)

노드 ID: 734:4072. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 회원가입 · 이메일 · abc123@example.com · 비밀번호 · •••••••• · 비밀번호 확인 · •••••••• · 비밀번호 찾기 · 인증번호 발송 · 6자리 인증번호 · 123456 · 인증번호가 확인되었습니다. · 인증번호 확인 · 재발송 · 기관 인증 · 로그인 후 신청 · 진입 세부 미정 · 약관 동의 · □ 전체 동의 · □ 이용약관 동의 (필수) · □ 개인정보 수집·이용 동의 (필수) · □ 소식·이벤트/마케팅 수신 (선택) · 가입하고 프로필 설정

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 비밀번호 찾기 | ON_CLICK | 이동 → WF-A03-비밀번호찾기 |
| 가입하고 프로필 설정 | ON_CLICK | 이동 → WF-A08-회원가입-프로필설정 |

#### [WF-A07-회원가입-이메일인증-재발송](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=734-3993)

노드 ID: 734:3993. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 회원가입 · 이메일 · abc123@example.com · 비밀번호 · •••••••• · 비밀번호 확인 · •••••••• · 비밀번호 찾기 · 인증번호 발송 · 6자리 인증번호 · 123456 · 인증번호가 재발송되었습니다. · 남은 시간 04:59 · 재발송까지 60초 · 인증번호 확인 · 재발송 · 기관 인증 · 로그인 후 신청 · 진입 세부 미정 · 약관 동의 · □ 전체 동의 · □ 이용약관 동의 (필수) · □ 개인정보 수집·이용 동의 (필수) · □ 소식·이벤트/마케팅 수신 (선택) · 가입하고 프로필 설정

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 비밀번호 찾기 | ON_CLICK | 이동 → WF-A03-비밀번호찾기 |
| 인증번호 확인 | ON_CLICK | 이동 → WF-A07-회원가입-이메일인증-완료 |

#### [WF-A08-회원가입-프로필설정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1048)

노드 ID: 692:1048. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 프로필 설정 · 프로필 사진 (선택) · 닉네임 · 동네이웃 · 중복 없이 최대 10자 · 이웃 속성 · 복수 선택 · ☑ 거주자 · □ 학생 · □ 직장인 · □ 상인 · 활동 지역 설정 · 이웃 인증하기 

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 활동 지역 설정 | ON_CLICK | 이동 → WF-A09-회원가입-활동지역설정 |
| 이웃 인증하기  | ON_CLICK | 이동 → WF-J01-이웃인증-제출 |

#### [WF-A08-회원가입-프로필설정-활동지역설정완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=741-4230)

노드 ID: 741:4230. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 프로필 설정 · 프로필 사진 (선택) · 닉네임 · 동네이웃 · 중복 없이 최대 10자 · 이웃 속성 · 복수 선택 · ☑ 거주자 · □ 학생 · □ 직장인 · □ 상인 · 서울 노원구 월계1동 · 선택됨   · 활동 지역 설정 · 이웃 인증하기  · 설정 완료하고 시작하기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 활동 지역 설정 | ON_CLICK | 이동 → WF-A09-회원가입-활동지역설정 |
| 이웃 인증하기  | ON_CLICK | 이동 → WF-J01-이웃인증-제출 |
| 설정 완료하고 시작하기 | ON_CLICK | ((returnTo 있음 = true) 그리고 (returnTo 투표 = true))인 경우: 이동 → WF-G09-로그인복귀-미인증회원투표 → ((returnTo 있음 = true) 그리고 (returnTo 투표 = false))인 경우: 이동 → WF-G05-로그인복귀-미인증회원상세 → (returnTo 있음 = false)인 경우: 이동 → WF-B01-메인 |

#### [WF-A09-회원가입-활동지역설정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1073)

노드 ID: 692:1073. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 활동 지역 설정 · 지역명 검색 · 월계 · 현재 위치로 찾기 · 서울 노원구 월계1동 · 선택됨   · 서울 노원구 월계2동 · 서울 노원구 월계3동 · 이 지역으로 설정하기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 이 지역으로 설정하기 | ON_CLICK | 이동 → WF-A08-회원가입-프로필설정-활동지역설정완료 |

### B 화면별 연결

#### [WF-B01-메인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1173)

노드 ID: 692:1173. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 메인 · 설정 · 게시물 검색 · 어떤 이야기를 찾으세요? · 서울 노원구 월계1동 · 지역 변경  › · 통합 게시판 · 이슈 지도 · 월계1동 · 지역 안건 · 광운대역 보행로 개선 · 월계2동 · 지역 안건 · 월계 도서관 공사 현황 · 전체 지도 보기 · 진행 중 투표 · 무엇이 먼저 필요할까요? · 진행 중 · 80명 · 남은 2일 · ○ 보행로 정비    52표 · 65% · ○ 조명 개선    28표 · 35% · 지금 투표하기 · 지금 뜨는 이야기 · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 첫 번째 사용자 첨부 사진 · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 설정 | ON_CLICK | 이동 → WF-L01-설정 |
| 게시물 검색 / 어떤 이야기를 찾으세요? | ON_CLICK | 변수 검색어 예시 ← 보행로 → 이동 → WF-C02-검색-필터 |
| 서울 노원구 월계1동 / 지역 변경  › | ON_CLICK | 이동 → WF-B02-탐색지역선택 |
| 통합 게시판 | ON_CLICK | 이동 → WF-C01-통합게시판 |
| 지역 안건 / 광운대역 보행로 개선 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 지역 안건 / 월계 도서관 공사 현황 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 전체 지도 보기 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 지금 투표하기 | ON_CLICK | 이동 → WF-F04-투표상세-진행중 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 주변 보행로 개선 / 첫 번째 사용자 첨부 사진 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-B02-탐색지역선택](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1198)

노드 ID: 692:1198. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 메인 · 지역 변경 · 현재 탐색 지역만 변경합니다. · 월계1동 · 현재  › · 월계2동 · 시연 범위 외 · 월계3동 · 시연 범위 외 · 하계1동 · 시연 범위 외 · 하계2동 ·   › · 닫기 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 월계1동 / 현재  › | ON_CLICK | 이전 화면으로 |
| 하계2동 /   › | ON_CLICK | 이동 → WF-B03-메인-하계2동-이웃미인증 |
| 닫기 | ON_CLICK | 이전 화면으로 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-B03-메인-하계2동-이웃미인증](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=726-3724)

노드 ID: 726:3724. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 메인 · 설정 · 탐색 지역 · 하계2동 / 이웃 인증 없음 / 열람은 가능하며 반응·댓글·투표·글쓰기는 제한됩니다. · 게시물 검색 · 어떤 이야기를 찾으세요? · 서울 노원구 하계2동 · 지역 변경  › · 통합 게시판 · 이슈 지도 · 하계1동 · 지역 안건 · 하계역 계단 노후화 개선 · 하계2동 · 지역 안건 · 하계2동 보행로 개선 · 전체 지도 보기 · 진행 중 투표 · 무엇이 먼저 필요할까요? · 진행 중 · 80명 · 남은 2일 · 보행로 정비    52표 · 65% · 조명 개선    28표 · 35% · 투표 결과 보기 · 지금 뜨는 이야기 · 지역 안건 · 하계2동 · 교통 · 하계2동 보행로 개선 · 첫 번째 사용자 첨부 사진 · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 설정 | ON_CLICK | 이동 → WF-L01-설정 |
| 게시물 검색 / 어떤 이야기를 찾으세요? | ON_CLICK | 변수 검색어 예시 ← 보행로 → 이동 → WF-B12-검색-하계2동 |
| 서울 노원구 하계2동 / 지역 변경  › | ON_CLICK | 이동 → WF-B04-탐색지역선택-하계2동 |
| 통합 게시판 | ON_CLICK | 이동 → WF-B05-통합게시판-하계2동-열람전용 |
| 지역 안건 / 하계역 계단 노후화 개선 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |
| 지역 안건 / 하계2동 보행로 개선 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |
| 전체 지도 보기 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |
| 투표 결과 보기 | ON_CLICK | 이동 → WF-B07-투표결과-하계2동-이웃미인증 |
| 지역 안건 · 하계2동 · 교통 / 하계2동 보행로 개선 / 첫 번째 사용자 첨부 사진 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-B06-지역안건상세-하계2동-열람전용 |
| 지도 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |
| 글쓰기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-B04-탐색지역선택-하계2동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-5837)

노드 ID: 761:5837. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 메인 · 탐색 지역 변경 · 기본 활동 지역: 월계1동 / 현재 탐색 지역: 하계2동 · 월계1동 · 기본 활동 지역  › · 월계2동 · 시연 범위 외 · 월계3동 · 시연 범위 외 · 하계1동 · 시연 범위 외 · 하계2동 · 현재 탐색 지역  › · 닫기 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 월계1동 / 기본 활동 지역  › | ON_CLICK | 이동 → WF-B01-메인 |
| 하계2동 / 현재 탐색 지역  › | ON_CLICK | 이전 화면으로 |
| 닫기 | ON_CLICK | 이전 화면으로 |
| 메인 | ON_CLICK | 이동 → WF-B03-메인-하계2동-이웃미인증 |
| 지도 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |
| 글쓰기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |

#### [WF-B05-통합게시판-하계2동-열람전용](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-5903)

노드 ID: 761:5903. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 통합 게시판 · 탐색 지역 · 하계2동 / 이웃 인증 없음 / 열람은 가능하며 반응·댓글·투표·글쓰기는 제한됩니다. · 하계2동 · 지역  › · 게시물 검색 · 어떤 이야기를 찾으세요? · 게시물 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 이런 의제는 어떠세요? · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 반응 32 · 댓글 8 · 활동 지역 월계1동과 관심 키워드 교통에 맞는 안건입니다. · 새 추천 · 지역 게시물 · 지역 안건 · 하계2동 · 교통 · 하계2동 보행로 개선 · 첫 번째 사용자 첨부 사진 · 반응 32 · 댓글 8 · AI 요약 · 투표 · 하계2동 · 교통 · 보행로 개선, 무엇이 먼저일까요? · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 하계2동 / 지역  › | ON_CLICK | 이동 → WF-B04-탐색지역선택-하계2동 |
| 게시물 검색 / 어떤 이야기를 찾으세요? | ON_CLICK | 변수 검색어 예시 ← 보행로 → 이동 → WF-B12-검색-하계2동 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 주변 보행로 개선 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| 지역 안건 · 하계2동 · 교통 / 하계2동 보행로 개선 / 첫 번째 사용자 첨부 사진 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-B06-지역안건상세-하계2동-열람전용 |
| AI 요약 | ON_CLICK | 이동 → WF-B10-AI요약-하계2동안건 |
| 투표 · 하계2동 · 교통 / 보행로 개선, 무엇이 먼저일까요? / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-B07-투표결과-하계2동-이웃미인증 |
| 메인 | ON_CLICK | 이동 → WF-B03-메인-하계2동-이웃미인증 |
| 지도 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |
| 글쓰기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |

상태 표시 바인딩:

- PostCard / 보행로 개선, 무엇이 먼저일까요? (761:5944): visible ← 본인 투표 표시.

#### [WF-B06-지역안건상세-하계2동-열람전용](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-6013)

노드 ID: 761:6013. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 안건 · 탐색 지역 · 하계2동 / 이웃 인증 없음 / 열람은 가능하며 반응·댓글·투표·글쓰기는 제한됩니다. · 지역 안건 · 교통 · 하계2동 · 하계2동 이웃 · 10월 5일  › · 하계2동 보행로 개선 · 하계2동 주민이 출퇴근 시간 보행자가 몰리는 구간의 보행로 개선을 제안했습니다. 먼저 혼잡한 구간을 살펴보자는 내용입니다. 주민 의견을 함께 확인할 수 있습니다. · 참고 자료 · 출처 링크 · AI 요약 · 사용자 첨부 사진 · 1/2 · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 보행로 정비가 먼저 필요합니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 야간 조명도 함께 살펴보면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 하계2동 이웃 인증 후 작성할 수 있습니다. · 댓글 등록 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| AI 요약 | ON_CLICK | 이동 → WF-B10-AI요약-하계2동안건 |
| 공감해요 20 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 필요해요 10 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 궁금해요 2 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 북마크 | ON_CLICK | 변수 WF-B06-지역안건상세-하계2동-열람전용/761:6037/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-B11-하계2동-일반회원기능안내 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-B06-지역안건상세-하계2동-열람전용 → 이동 → WF-F08-신고-입력 |
| 좋아요 3   싫어요 0   답글 달기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 좋아요 3   싫어요 0   답글 달기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 댓글 등록 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 메인 | ON_CLICK | 이동 → WF-B03-메인-하계2동-이웃미인증 |
| 지도 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |
| 글쓰기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 북마크에 저장되었습니다. (804:4741): visible ← WF-B06-지역안건상세-하계2동-열람전용/761:6037/메시지 표시.

#### [WF-B07-투표결과-하계2동-이웃미인증](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-6110)

노드 ID: 761:6110. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 투표 · 탐색 지역 · 하계2동 / 이웃 인증 없음 / 열람은 가능하며 반응·댓글·투표·글쓰기는 제한됩니다. · 투표 · 교통 · 하계2동 · 진행 중 · 하계2동 이웃 ·   › · 보행로 개선, 무엇이 먼저일까요? · 하계2동 주민의 공개 투표 결과입니다. 이 지역의 이웃 인증이 없어 투표에는 참여할 수 없습니다. · 무엇이 먼저 필요할까요? · 진행 중 · 80명 · 남은 2일 · 보행로 정비    52표 · 65% · 조명 개선    28표 · 35% · 투표 참여 제한 안내 · 참고 링크 · 참고 자료 · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 보행로 정비가 먼저 필요합니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 야간 조명도 함께 살펴보면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 하계2동 이웃 인증 후 작성할 수 있습니다. · 댓글 등록 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 보행로 정비    52표 · 65% | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 조명 개선    28표 · 35% | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 투표 참여 제한 안내 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 공감해요 20 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 필요해요 10 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 궁금해요 2 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 북마크 | ON_CLICK | 변수 WF-B07-투표결과-하계2동-이웃미인증/761:6142/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-B11-하계2동-일반회원기능안내 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-B07-투표결과-하계2동-이웃미인증 → 이동 → WF-F08-신고-입력 |
| 좋아요 3   싫어요 0   답글 달기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 좋아요 3   싫어요 0   답글 달기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 댓글 등록 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 메인 | ON_CLICK | 이동 → WF-B03-메인-하계2동-이웃미인증 |
| 지도 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |
| 글쓰기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 북마크에 저장되었습니다. (804:4746): visible ← WF-B07-투표결과-하계2동-이웃미인증/761:6142/메시지 표시.

#### [WF-B08-이슈지도-하계2동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-6214)

노드 ID: 761:6214. 세로 스크롤 설정 없음.

화면에 포함된 텍스트: ‹ · 이슈 지도 · 하계2동 · 지역 안건 · 하계2동 보행로 개선 · 월계2동 · 월계3동 · 하계1동 · 탐색 지역 · 하계2동 / 이웃 인증 없음 / 열람은 가능하며 반응·댓글·투표·글쓰기는 제한됩니다. · 기본 활동 지역: 월계1동 / 탐색 지역: 하계2동 · ＋ · － · 동별 대표 게시물 · 지역 안건 · 하계2동 · 첫 번째 첨부 사진 · 하계2동 보행로 개선 · 첫 번째 사용자 첨부 사진 · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 / 하계2동 보행로 개선 | ON_CLICK | 이동 → WF-B06-지역안건상세-하계2동-열람전용 |
| 지역 안건 · 하계2동 · 첫 번째 첨부 사진 / 하계2동 보행로 개선 / 첫 번째 사용자 첨부 사진 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-B06-지역안건상세-하계2동-열람전용 |
| 메인 | ON_CLICK | 이동 → WF-B03-메인-하계2동-이웃미인증 |
| 글쓰기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |

#### [WF-B09-하계2동-지역참여제한안내](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-6291)

노드 ID: 761:6291. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 참여 안내 · 하계2동 이웃 인증이 없어 지역 참여를 할 수 없습니다. / 게시물·댓글·투표 결과는 열람할 수 있습니다. 탐색 지역 변경은 참여 권한을 부여하지 않습니다. · 돌아가기 · 기본 활동 지역 월계1동으로 돌아가기 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 돌아가기 | ON_CLICK | 이전 화면으로 |
| 기본 활동 지역 월계1동으로 돌아가기 | ON_CLICK | 이동 → WF-B01-메인 |
| 메인 | ON_CLICK | 이동 → WF-B03-메인-하계2동-이웃미인증 |
| 지도 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |

#### [WF-B10-AI요약-하계2동안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-6357)

노드 ID: 761:6357. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · AI 요약 · 하계2동 보행로 개선 · 하계2동 주민이 출퇴근 시간 보행자가 몰리는 구간의 보행로 개선을 제안했습니다. 먼저 혼잡한 구간을 살펴보자는 내용입니다. 원문에서 주민 의견을 함께 확인할 수 있습니다. · 원문 보기 · 닫기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 원문 보기 | ON_CLICK | 이동 → WF-B06-지역안건상세-하계2동-열람전용 |
| 닫기 | ON_CLICK | 이전 화면으로 |

#### [WF-B11-하계2동-일반회원기능안내](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=761-6395)

노드 ID: 761:6395. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 일반 회원 기능 안내 · 로그인 회원은 이웃 인증 없이 북마크·공유·신고를 이용할 수 있습니다. 지역 참여에는 해당 지역의 이웃 인증이 필요합니다. · 돌아가기 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 돌아가기 | ON_CLICK | 이전 화면으로 |
| 메인 | ON_CLICK | 이동 → WF-B03-메인-하계2동-이웃미인증 |
| 지도 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |
| 글쓰기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |

#### [WF-B12-검색-하계2동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4085)

노드 ID: 789:4085. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 게시물 검색 · 하계2동 · 1. 지역  › · 검색어 · 보행로 · 2. 게시물 유형 · 지역 안건 · 지역 활동 정보 · 투표 · 3. 주제 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 검색 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 하계2동 / 1. 지역  › | ON_CLICK | 이동 → WF-B02-탐색지역선택 |
| 검색어 / 보행로 | ON_CLICK | (검색어 예시 = 백화점)인 경우: 변수 검색어 예시 ← 보행로; 그 외: 변수 검색어 예시 ← 백화점 |
| 검색 | ON_CLICK | (검색어 예시 = 백화점)인 경우: 이동 → WF-B14-검색결과없음-하계2동; 그 외: 이동 → WF-B13-검색결과있음-하계2동 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- Value (I789:4098;693:2265): characters ← 검색어 예시; 텍스트 보행로.

#### [WF-B13-검색결과있음-하계2동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4166)

노드 ID: 789:4166. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 게시물 검색 · 하계2동 · 1. 지역  › · 검색어 · 보행로 · 2. 게시물 유형 · 지역 안건 · 지역 활동 정보 · 투표 · 3. 주제 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 하계2동 · 지역 안건 · 교통 · 검색 · 지역 안건 · 하계2동 · 교통 · 하계2동 보행로 개선 · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 하계2동 / 1. 지역  › | ON_CLICK | 이동 → WF-B02-탐색지역선택 |
| 검색어 / 보행로 | ON_CLICK | 변수 검색어 예시 ← 백화점 → 이동 → WF-B12-검색-하계2동 |
| 검색 | ON_CLICK | 이동 → WF-B12-검색-하계2동 |
| 지역 안건 · 하계2동 · 교통 / 하계2동 보행로 개선 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-B06-지역안건상세-하계2동-열람전용 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-B14-검색결과없음-하계2동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4253)

노드 ID: 789:4253. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 게시물 검색 · 하계2동 · 1. 지역  › · 검색어 · 백화점 · 2. 게시물 유형 · 지역 안건 · 지역 활동 정보 · 투표 · 3. 주제 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 하계2동 · 지역 안건 · 생활정보 · 검색 · 검색 결과가 없습니다. · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 하계2동 / 1. 지역  › | ON_CLICK | 이동 → WF-B02-탐색지역선택 |
| 검색어 / 백화점 | ON_CLICK | 변수 검색어 예시 ← 보행로 → 이동 → WF-B12-검색-하계2동 |
| 검색 | ON_CLICK | 이동 → WF-B12-검색-하계2동 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

### C 화면별 연결

#### [WF-C01-새추천성공](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=743-4650)

노드 ID: 743:4650. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 통합 게시판 · 월계1동 · 지역  › · 게시물 검색 · 어떤 이야기를 찾으세요? · 게시물 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 이런 의제는 어떠세요? · 지역 안건 · 월계1동 · 교통 · 광운대역 야간 조명 개선 · 반응 32 · 댓글 8 · 활동 지역 월계1동과 관심 키워드 교통에 맞는 안건입니다. · 새 추천 · 지역 게시물 · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 첫 번째 사용자 첨부 사진 · 반응 32 · 댓글 8 · AI 요약 · 주민이 광운대역 주변 보행로 개선을 제안했습니다. 출퇴근 시간에 보행자가 몰리는 구간을 먼저 살펴보자는 내용입니다. 게시물에서 주민 의견을 함께 확인할 수 있습니다. · 원문 보기 · 지역 활동 정보 · 월계1동 · 생활정보 · 진행 · 월계 주민 걷기 모임 · 반응 32 · 댓글 8 · 투표 · 월계1동 · 교통 · 보행로 개선, 무엇이 먼저일까요? · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 월계1동 / 지역  › | ON_CLICK | 이동 → WF-B02-탐색지역선택 |
| 게시물 검색 / 어떤 이야기를 찾으세요? | ON_CLICK | 이동 → WF-C02-검색-필터 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 야간 조명 개선 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F19-지역안건-새추천상세 |
| 새 추천 | ON_CLICK | 변수 C01 새 추천 / 후보 없음 메시지 표시 ← true |
| 원문 보기 | ON_CLICK | 이동 → WF-F19-지역안건-새추천상세 |
| 지역 활동 정보 · 월계1동 · 생활정보 · 진행 / 월계 주민 걷기 모임 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F03-지역활동정보상세 |
| 투표 · 월계1동 · 교통 / 보행로 개선, 무엇이 먼저일까요? / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F04-투표상세-진행중 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 새로운 추천 후보 게시물이 없습니다. (805:4650): visible ← C01 새 추천 / 후보 없음 메시지 표시.
- PostCard / 보행로 개선, 무엇이 먼저일까요? (743:4692): visible ← 본인 투표 표시.

#### [WF-C01-통합게시판](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1248)

노드 ID: 692:1248. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 통합 게시판 · 월계1동 · 지역  › · 게시물 검색 · 어떤 이야기를 찾으세요? · 게시물 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 이런 의제는 어떠세요? · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 반응 32 · 댓글 8 · 활동 지역 월계1동과 관심 키워드 교통에 맞는 안건입니다. · 새 추천 · 지역 게시물 · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 첫 번째 사용자 첨부 사진 · 반응 32 · 댓글 8 · AI 요약 · 주민이 광운대역 주변 보행로 개선을 제안했습니다. 출퇴근 시간에 보행자가 몰리는 구간을 먼저 살펴보자는 내용입니다. 게시물에서 주민 의견을 함께 확인할 수 있습니다. · 원문 보기 · 지역 활동 정보 · 월계1동 · 생활정보 · 진행 · 월계 주민 걷기 모임 · 반응 32 · 댓글 8 · 투표 · 월계1동 · 교통 · 보행로 개선, 무엇이 먼저일까요? · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 월계1동 / 지역  › | ON_CLICK | 이동 → WF-B02-탐색지역선택 |
| 게시물 검색 / 어떤 이야기를 찾으세요? | ON_CLICK | 이동 → WF-C02-검색-필터 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 주변 보행로 개선 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| 새 추천 | ON_CLICK | 이동 → WF-C01-새추천성공 |
| 원문 보기 | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| 지역 활동 정보 · 월계1동 · 생활정보 · 진행 / 월계 주민 걷기 모임 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F03-지역활동정보상세 |
| 투표 · 월계1동 · 교통 / 보행로 개선, 무엇이 먼저일까요? / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F04-투표상세-진행중 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- PostCard / 보행로 개선, 무엇이 먼저일까요? (694:2739): visible ← 본인 투표 표시.

#### [WF-C02-검색-필터](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1273)

노드 ID: 692:1273. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 게시물 검색 · 월계1동 · 1. 지역  › · 검색어 · 보행로 · 2. 게시물 유형 · 지역 안건 · 지역 활동 정보 · 투표 · 3. 주제 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 검색 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 월계1동 / 1. 지역  › | ON_CLICK | 이동 → WF-B02-탐색지역선택 |
| 검색어 / 보행로 | ON_CLICK | (검색어 예시 = 보행로)인 경우: 변수 검색어 예시 ← 백화점; 그 외: 변수 검색어 예시 ← 보행로 |
| 검색 | ON_CLICK | (검색어 예시 = 백화점)인 경우: 이동 → WF-C02-검색결과없음; 그 외: 이동 → WF-C02-검색결과있음 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- Value (I694:2771;693:2265): characters ← 검색어 예시; 텍스트 보행로.

#### [WF-C02-검색결과없음](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=743-4385)

노드 ID: 743:4385. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 게시물 검색 · 월계1동 · 1. 지역  › · 검색어 · 보행로 · 2. 게시물 유형 · 지역 안건 · 지역 활동 정보 · 투표 · 3. 주제 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 월계1동 · 지역 안건 · 교통 · 검색 · 검색 결과가 없습니다. · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 월계1동 / 1. 지역  › | ON_CLICK | 이동 → WF-B02-탐색지역선택 |
| 검색어 / 보행로 | ON_CLICK | 변수 검색어 예시 ← 보행로 → 이동 → WF-C02-검색-필터 |
| 검색 | ON_CLICK | (검색어 예시 = 보행로)인 경우: 이동 → WF-C02-검색결과있음 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- Value (I743:4398;693:2265): characters ← 검색어 예시; 텍스트 보행로.

#### [WF-C02-검색결과있음](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=743-4291)

노드 ID: 743:4291. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 게시물 검색 · 월계1동 · 1. 지역  › · 검색어 · 보행로 · 2. 게시물 유형 · 지역 안건 · 지역 활동 정보 · 투표 · 3. 주제 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 월계1동 · 지역 안건 · 교통 · 검색 · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 월계1동 / 1. 지역  › | ON_CLICK | 이동 → WF-B02-탐색지역선택 |
| 검색어 / 보행로 | ON_CLICK | 변수 검색어 예시 ← 백화점 → 이동 → WF-C02-검색-필터 |
| 검색 | ON_CLICK | (검색어 예시 = 백화점)인 경우: 이동 → WF-C02-검색결과없음 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 주변 보행로 개선 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- Value (I743:4304;693:2265): characters ← 검색어 예시; 텍스트 보행로.

### D 화면별 연결

#### [WF-D01-전체지도](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1373)

노드 ID: 692:1373. 세로 스크롤 설정 없음.

화면에 포함된 텍스트: ‹ · 이슈 지도 · 월계1동 · 지역 안건 · 광운대역 보행로 개선 · 월계2동 · 지역 안건 · 월계도서관 공사 현황 · 월계3동 · 지역 안건 · 스타필드 입점 · 하계1동 · 지역 활동 · 과기대 독서 모임 · 기본 활동 지역 · 월계1동 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 / 광운대역 보행로 개선 | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| 지역 안건 / 월계도서관 공사 현황 | ON_CLICK | 이동 → WF-F16-월계2동안건-열람전용 |
| 지역 안건 / 스타필드 입점 | ON_CLICK | 이동 → WF-F17-월계3동안건-열람전용 |
| 지역 활동 / 과기대 독서 모임 | ON_CLICK | 이동 → WF-F18-하계1동-독서모임-열람전용 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-D03-지도-로딩실패](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1423)

노드 ID: 692:1423. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 이슈 지도 · 지도를 불러오지 못했습니다. · 다시 시도 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 다시 시도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

### E 화면별 연결

#### [WF-E01-글쓰기-유형선택](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1448)

노드 ID: 692:1448. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 글쓰기 · 어떤 게시물을 작성할까요? · 지역 안건 · 의견과 지역 문제  › · 지역 활동 정보 · 일정과 참여 정보  › · 투표 · 질문과 선택지  › · 게시하려는 지역의 이웃 인증이 필요합니다.

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 / 의견과 지역 문제  › | ON_CLICK | 이동 → WF-E02-글쓰기-지역안건 |
| 지역 활동 정보 / 일정과 참여 정보  › | ON_CLICK | 이동 → WF-E03-글쓰기-지역활동정보 |
| 투표 / 질문과 선택지  › | ON_CLICK | 이동 → WF-E04-글쓰기-투표 |

#### [WF-E02-글쓰기-지역안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1473)

노드 ID: 692:1473. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 안건 작성 · 지역 * · 월계1동 · 주제 * · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 제목 * · 제목 입력 · 본문 * · 우리 동네 이야기를 적어 주세요. · 사진 추가 (선택) · 참고 링크 (선택) · https:// · □ 익명으로 게시 · 임시 저장 · 게시하기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 사진 추가 (선택) | ON_CLICK | 이동 → WF-E05-사진-첨부미리보기 |
| 임시 저장 | ON_CLICK | 변수 WF-E02-글쓰기-지역안건/755:5185/메시지 표시 ← true |
| 게시하기 | ON_CLICK | 이동 → WF-E02-글쓰기-지역안건-게시완료 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 임시 저장되었습니다. (804:4651): visible ← WF-E02-글쓰기-지역안건/755:5185/메시지 표시.

#### [WF-E02-글쓰기-지역안건-게시완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1116-6196)

노드 ID: 1116:6196. 세로 스크롤 설정 없음.

화면에 포함된 텍스트: 게시 완료 · 게시가 완료되었습니다 · 게시물이 정상적으로 등록되었습니다.

현재 연결된 클릭 또는 자동 이동 동작 없음.

#### [WF-E03-글쓰기-지역활동정보](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1498)

노드 ID: 692:1498. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 활동 정보 작성 · 지역 * · 월계1동 · 주제 * · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 제목 * · 제목 입력 · 본문 * · 우리 동네 이야기를 적어 주세요. · 출처 * · 출처 입력 · 일정 * · 날짜와 시간 · 장소 * · 활동 장소 · 활동 상태 * · 기본값 없음 · ○ 예정 · ○ 진행 · ○ 종료 · ○ 취소 · 외부 참여 링크 (선택) · https:// · 참고 링크 (선택) · 출처와 별개 · 사진 추가 (선택) · 주최자 문의는 현재 등록 이메일을 사용합니다. · 임시 저장 · 게시하기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 사진 추가 (선택) | ON_CLICK | 이동 → WF-E05-사진-첨부미리보기 |
| 임시 저장 | ON_CLICK | 변수 WF-E03-글쓰기-지역활동정보/755:5173/메시지 표시 ← true |
| 게시하기 | ON_CLICK | 이동 → WF-E03-글쓰기-지역활동정보-게시완료 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 임시 저장되었습니다. (804:4656): visible ← WF-E03-글쓰기-지역활동정보/755:5173/메시지 표시.

#### [WF-E03-글쓰기-지역활동정보-게시완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1116-6244)

노드 ID: 1116:6244. 세로 스크롤 설정 없음.

화면에 포함된 텍스트: 게시 완료 · 게시가 완료되었습니다 · 게시물이 정상적으로 등록되었습니다.

현재 연결된 클릭 또는 자동 이동 동작 없음.

#### [WF-E04-글쓰기-투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1523)

노드 ID: 692:1523. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 투표 작성 · 지역 * · 월계1동 · 주제 * · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 제목 * · 제목 입력 · 본문 * · 우리 동네 이야기를 적어 주세요. · 질문 * · 무엇이 먼저 필요할까요? · 선택지 1 * · 보행로 정비 · 선택지 2 * · 조명 개선 · 선택지 추가 · 선택지 삭제 · 선택지 2~10개 · 10개에서 추가 비활성 · 종료 일시 * · 날짜와 시간 · □ 종료 전 알림 설정 (선택) · 알림 시점 (설정 시) · 종료 이전 날짜와 시간 · 참고 링크 (선택) · https:// · 사진 추가 (선택) · 임시 저장 · 게시하기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 사진 추가 (선택) | ON_CLICK | 이동 → WF-E05-사진-첨부미리보기 |
| 임시 저장 | ON_CLICK | 변수 WF-E04-글쓰기-투표/755:5179/메시지 표시 ← true |
| 게시하기 | ON_CLICK | 변수 본인 투표 표시 ← true → 변수 현재 투표 제목 ← 보행로 개선, 무엇이 먼저일까요? → 이동 → WF-E04-글쓰기-투표-게시완료 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 임시 저장되었습니다. (804:4661): visible ← WF-E04-글쓰기-투표/755:5179/메시지 표시.

#### [WF-E04-글쓰기-투표-게시완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1116-6303)

노드 ID: 1116:6303. 세로 스크롤 설정 없음.

화면에 포함된 텍스트: 게시 완료 · 게시가 완료되었습니다 · 게시물이 정상적으로 등록되었습니다.

현재 연결된 클릭 또는 자동 이동 동작 없음.

#### [WF-E05-사진-첨부미리보기](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1548)

노드 ID: 692:1548. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 사진 첨부 · 카메라 · 갤러리 · JPG·PNG · 최대 10장 · 전체 합계 10MB · 선택한 사진 미리보기 · 교체 · 제거 · 작성 화면으로

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 작성 화면으로 | ON_CLICK | 이전 화면으로 |

#### [WF-E06-진행중투표-수정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1573)

노드 ID: 692:1573. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 진행 중 투표 수정 · 지역·주제·질문·선택지는 수정할 수 없습니다. · 제목 · 보행 환경 우선순위 · 본문 · 변경할 설명 · 사진 관리 · 참고 링크 · https:// · 종료 일시 · 변경할 날짜와 시간 · 종료 일시 변경 시 기존 알림 예약이 취소됩니다. · 새 종료 전 알림 (선택) · 새 시점 직접 설정 · 변경 저장 · 삭제

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 사진 관리 | ON_CLICK | 이동 → WF-E05-사진-첨부미리보기 |
| 변경 저장 | ON_CLICK | 변수 현재 투표 제목 ← 보행 환경 우선순위 → 이동 → WF-F25-내게시물상세-진행중투표 |
| 삭제 | ON_CLICK | 이동 → WF-E08-본인투표-삭제확인 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 변경 사항이 저장되었습니다. (804:4665): visible ← WF-E06-본인투표-수정/694:3315/메시지 표시.
- 실제 UI 상태 메시지 / 투표가 삭제되었습니다. (804:4669): visible ← WF-E06-본인투표-수정/694:3317/메시지 표시.

#### [WF-E08-본인투표-삭제확인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=791-4640)

노드 ID: 791:4640. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 투표 삭제 · 이 투표를 삭제하시겠습니까? · 삭제한 게시물은 목록에서 더 이상 표시되지 않습니다. · 확인 · 취소 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 확인 | ON_CLICK | 변수 본인 투표 표시 ← false → 변수 WF-E06-본인투표-수정/694:3317/메시지 표시 ← true → 이동 → WF-E04-글쓰기-투표 |
| 취소 | ON_CLICK | 이동 → WF-E06-진행중투표-수정 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-E09-내지역안건-수정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=895-4619)

노드 ID: 895:4619. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 게시물 수정 · 지역 * · 월계1동 · 주제 * · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 제목 * · 광운대역 주변 보행로 개선 · 본문 * · 출퇴근 시간에 사람이 많이 모이는 구간을 살펴보고 보행로를 개선하면 좋겠습니다. 우리 동네에서 필요한 부분을 함께 이야기해 주세요. · 사진 추가 (선택) · 참고 링크 (선택) · https:// · □ 익명으로 게시 · 변경 저장

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 사진 추가 (선택) | ON_CLICK | 이동 → WF-E05-사진-첨부미리보기 |
| 변경 저장 | ON_CLICK | 이동 → WF-F24-내게시물상세-지역안건 |

### F 화면별 연결

#### [WF-F01-지역안건상세-사진있음](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1598)

노드 ID: 692:1598. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 안건 · 지역 안건 · 월계1동 · 교통 · 동네이웃 · 기관 인증 배지 자리 · 10월 5일  › · 광운대역 주변 보행로 개선 · 출퇴근 시간에 사람이 많이 모이는 구간을 살펴보고 보행로를 개선하면 좋겠습니다. 우리 동네에서 필요한 부분을 함께 이야기해 주세요. · 참고 자료 · 출처 링크 · 사용자 첨부 사진 · 1/2 · AI 요약 · 주민이 광운대역 주변 보행로 개선을 제안했습니다. 출퇴근 시간에 보행자가 몰리는 구간을 먼저 살펴보자는 내용입니다. 게시물에서 주민 의견을 함께 확인할 수 있습니다. · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 보행로 정비가 먼저 필요합니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 야간 조명도 함께 살펴보면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 의견을 남겨 주세요. · 댓글 등록

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 북마크 | ON_CLICK | 변수 WF-F01-지역안건상세-사진있음/694:3350/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-G04-공유-링크복사 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-F01-지역안건상세-사진있음 → 이동 → WF-F08-신고-입력 |
| 댓글 입력 / 의견을 남겨 주세요. | ON_CLICK | 변수 WF-F01-지역안건상세-사진있음/694:3376/메시지 표시 ← false |
| 댓글 등록 | ON_CLICK | 변수 WF-F01-지역안건상세-사진있음/694:3376/메시지 내용 ← 댓글이 등록되었습니다. → 변수 WF-F01-지역안건상세-사진있음/694:3376/메시지 표시 ← true |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- NavItem / 메인 (694:3379): 이동 → WF-B01-메인.
- NavItem / 지도 (694:3382): 이동 → WF-D01-전체지도.
- NavItem / 글쓰기 (694:3385): 이동 → WF-E01-글쓰기-유형선택.
- NavItem / 알림 (694:3388): 이동 → WF-H01-알림및활동.
- NavItem / 마이 (694:3391): 이동 → WF-I01-마이페이지.

#### [WF-F02-지역안건상세-사진없음](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1623)

노드 ID: 692:1623. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 안건 · 지역 안건 · 교통 · 월계1동 · 동네이웃 · 기관 인증 배지 자리 · 10월 5일  › · 광운대역 주변 보행로 개선 · 출퇴근 시간에 사람이 많이 모이는 구간을 살펴보고 보행로를 개선하면 좋겠습니다. 우리 동네에서 필요한 부분을 함께 이야기해 주세요. · 참고 자료 · 출처 링크 · AI 요약 · 주민이 광운대역 주변 보행로 개선을 제안했습니다. 출퇴근 시간에 보행자가 몰리는 구간을 먼저 살펴보자는 내용입니다. 게시물에서 주민 의견을 함께 확인할 수 있습니다. · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 보행로 정비가 먼저 필요합니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 야간 조명도 함께 살펴보면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 의견을 남겨 주세요. · 댓글 등록 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 북마크 | ON_CLICK | 변수 WF-F02-지역안건상세-사진없음/694:3423/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-G04-공유-링크복사 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-F02-지역안건상세-사진없음 → 이동 → WF-F08-신고-입력 |
| 댓글 등록 | ON_CLICK | 변수 WF-F02-지역안건상세-사진없음/694:3449/메시지 내용 ← 댓글이 등록되었습니다. → 변수 WF-F02-지역안건상세-사진없음/694:3449/메시지 표시 ← true |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 북마크에 저장되었습니다. (804:4683): visible ← WF-F02-지역안건상세-사진없음/694:3423/메시지 표시.
- 실제 UI 상태 메시지 / 댓글이 등록되었습니다. (804:4688): visible ← WF-F02-지역안건상세-사진없음/694:3449/메시지 표시.

#### [WF-F03-지역활동정보상세](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1648)

노드 ID: 692:1648. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 활동 정보 · 지역 활동 정보 · 생활정보 · 월계1동 · 동네이웃 ·   › · 월계 주민 걷기 모임 · 이웃과 동네를 걸으며 함께 이야기를 나누는 모임입니다. · 출처  주민 모임 안내 · 일정  10월 10일 10:00 · 장소  월계동 공원 입구 · 상태  진행 · 외부 참여 경로 열기 · 주최자 문의  neighbor@example.com · 참고 링크 (선택) · 별도 참고 자료 · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 보행로 정비가 먼저 필요합니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 야간 조명도 함께 살펴보면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 의견을 남겨 주세요. · 댓글 등록 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 북마크 | ON_CLICK | 변수 WF-F03-지역활동정보상세/694:3510/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-G04-공유-링크복사 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-F03-지역활동정보상세 → 이동 → WF-F08-신고-입력 |
| 댓글 등록 | ON_CLICK | 변수 WF-F03-지역활동정보상세/694:3536/메시지 내용 ← 댓글이 등록되었습니다. → 변수 WF-F03-지역활동정보상세/694:3536/메시지 표시 ← true |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 북마크에 저장되었습니다. (804:4693): visible ← WF-F03-지역활동정보상세/694:3510/메시지 표시.
- 실제 UI 상태 메시지 / 댓글이 등록되었습니다. (804:4698): visible ← WF-F03-지역활동정보상세/694:3536/메시지 표시.

#### [WF-F04-투표상세-진행중](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1673)

노드 ID: 692:1673. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 투표 · 투표 · 교통 · 월계1동 · 진행 중 · 동네이웃 ·   › · 보행로 개선, 무엇이 먼저일까요? · 우선 개선할 항목을 선택해 의견을 보태 주세요. · 무엇이 먼저 필요할까요? · 진행 중 · 80명 · 남은 2일 · ○ 보행로 정비    52표 · 65% · ○ 조명 개선    28표 · 35% · 투표 제출 · 참고 링크 · 참고 자료 · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 보행로 정비가 먼저 필요합니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 야간 조명도 함께 살펴보면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 의견을 남겨 주세요. · 댓글 등록 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| ○ 보행로 정비    52표 · 65% | ON_CLICK | 변수 투표 선택 초안 ← 보행로 정비 → 변수 투표 선택지 1 표시 ← ● 보행로 정비    52표 · 65% → 변수 투표 선택지 2 표시 ← ○ 조명 개선    28표 · 35% |
| ○ 조명 개선    28표 · 35% | ON_CLICK | 변수 투표 선택 초안 ← 조명 개선 → 변수 투표 선택지 1 표시 ← ○ 보행로 정비    52표 · 65% → 변수 투표 선택지 2 표시 ← ● 조명 개선    28표 · 35% |
| 투표 제출 | ON_CLICK | 변수 WF-F04-투표상세-진행중/694:3579/메시지 표시 ← false → (투표 선택 초안 = "" )인 경우: 변수 WF-F04-투표상세-진행중/694:3579/메시지 표시 ← true → ((투표 선택 초안 = 보행로 정비) 그리고 ((투표 제출 선택 = "" ) 또는 (투표 제출 선택 = 보행로 정비)))인 경우: 변수 투표 제출 선택 ← 보행로 정비 → 변수 투표 내 선택 표시 ← 내 선택: 보행로 정비  \|  최다: 보행로 정비 → 변수 투표 선택 초안 ← 보행로 정비 → 변수 투표 선택지 1 표시 ← ● 보행로 정비    52표 · 65% → 변수 투표 선택지 2 표시 ← ○ 조명 개선    28표 · 35% → 이동 → WF-F11-투표-제출후 → ((투표 선택 초안 = 조명 개선) 그리고 ((투표 제출 선택 = "" ) 또는 (투표 제출 선택 = 조명 개선)))인 경우: 변수 투표 제출 선택 ← 조명 개선 → 변수 투표 내 선택 표시 ← 내 선택: 조명 개선  \|  최다: 보행로 정비 → 변수 투표 선택 초안 ← 조명 개선 → 변수 투표 선택지 1 표시 ← ○ 보행로 정비    52표 · 65% → 변수 투표 선택지 2 표시 ← ● 조명 개선    28표 · 35% → 이동 → WF-F11-투표-제출후 |
| 북마크 | ON_CLICK | 변수 WF-F04-투표상세-진행중/694:3592/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-G04-공유-링크복사 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-F04-투표상세-진행중 → 이동 → WF-F08-신고-입력 |
| 댓글 등록 | ON_CLICK | 변수 WF-F04-투표상세-진행중/694:3618/메시지 내용 ← 댓글이 등록되었습니다. → 변수 WF-F04-투표상세-진행중/694:3618/메시지 표시 ← true |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- 섹션 제목 (694:3568): characters ← 현재 투표 제목; 텍스트 보행로 개선, 무엇이 먼저일까요?.
- Option (694:3574): characters ← 투표 선택지 1 표시; 텍스트 ○ 보행로 정비    52표 · 65%.
- Option (694:3577): characters ← 투표 선택지 2 표시; 텍스트 ○ 조명 개선    28표 · 35%.
- 실제 UI 상태 메시지 / 투표할 항목을 선택해 주세요. (804:4702): visible ← WF-F04-투표상세-진행중/694:3579/메시지 표시.
- 실제 UI 상태 메시지 / 북마크에 저장되었습니다. (804:4707): visible ← WF-F04-투표상세-진행중/694:3592/메시지 표시.
- 실제 UI 상태 메시지 / 댓글이 등록되었습니다. (804:4712): visible ← WF-F04-투표상세-진행중/694:3618/메시지 표시.

#### [WF-F07-북마크-저장성공](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1748)

노드 ID: 692:1748. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 북마크 · 저장되었습니다 · 확인

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 확인 | ON_CLICK | 이전 화면으로 |

#### [WF-F08-신고-입력](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1773)

노드 ID: 692:1773. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 게시물 신고 · 신고 사유 · 신고 사유 * · 어떤 문제가 있는지 문장으로 알려 주세요. · 신고 제출

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 신고 사유 * / 어떤 문제가 있는지 문장으로 알려 주세요. | ON_CLICK | 변수 WF-F08-신고-입력/694:3750/메시지 표시 ← false |
| 신고 제출 | ON_CLICK | 변수 WF-F08-신고-입력/694:3750/메시지 내용 ← 신고가 접수되었습니다. → 변수 WF-F08-신고-입력/694:3750/메시지 표시 ← true |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 신고가 접수되었습니다. (804:4722): visible ← WF-F08-신고-입력/694:3750/메시지 표시.

#### [WF-F09-투표-종료결과](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1798)

노드 ID: 692:1798. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 투표 결과 · 투표 · 교통 · 월계1동 · 종료 · 보행로 개선 우선순위 · 무엇이 먼저 필요할까요? · 종료 · 최종 결과 · 80명 · 보행로 정비    52표 · 65% · 조명 개선    28표 · 35% · 내 선택: 조명 개선  |  최다: 보행로 정비 · 투표가 종료되었습니다. · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 보행로 정비가 먼저 필요합니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 야간 조명도 함께 살펴보면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 의견을 남겨 주세요. · 댓글 등록 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 북마크 | ON_CLICK | 변수 WF-F09-투표-종료결과/694:3784/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-G04-공유-링크복사 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-F09-투표-종료결과 → 이동 → WF-F08-신고-입력 |
| 댓글 등록 | ON_CLICK | 변수 WF-F09-투표-종료결과/694:3810/메시지 내용 ← 댓글이 등록되었습니다. → 변수 WF-F09-투표-종료결과/694:3810/메시지 표시 ← true |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 북마크에 저장되었습니다. (804:4727): visible ← WF-F09-투표-종료결과/694:3784/메시지 표시.
- 실제 UI 상태 메시지 / 댓글이 등록되었습니다. (804:4732): visible ← WF-F09-투표-종료결과/694:3810/메시지 표시.

#### [WF-F11-투표-제출후](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=695-3309)

노드 ID: 695:3309. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 투표 · 참여 완료 · 투표 · 교통 · 월계1동 · 진행 중 · 보행로 개선, 무엇이 먼저일까요? · 무엇이 먼저 필요할까요? · 진행 중 · 80명 · 남은 2일 · ○ 보행로 정비    52표 · 65% · ○ 조명 개선    28표 · 35% · 내 선택: 없음  |  최다: 보행로 정비 · 다른 선택 제출 · 새로운 선택을 고른 뒤 투표 제출을 눌러 변경할 수 있습니다. · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-F04-투표상세-진행중 |
| ○ 보행로 정비    52표 · 65% | ON_CLICK | 변수 투표 선택 초안 ← 보행로 정비 → 변수 투표 선택지 1 표시 ← ● 보행로 정비    52표 · 65% → 변수 투표 선택지 2 표시 ← ○ 조명 개선    28표 · 35% |
| ○ 조명 개선    28표 · 35% | ON_CLICK | 변수 투표 선택 초안 ← 조명 개선 → 변수 투표 선택지 1 표시 ← ○ 보행로 정비    52표 · 65% → 변수 투표 선택지 2 표시 ← ● 조명 개선    28표 · 35% |
| 다른 선택 제출 | ON_CLICK | 교체 표시 → WF-F04-투표상세-진행중 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- 섹션 제목 (695:3466): characters ← 현재 투표 제목; 텍스트 보행로 개선, 무엇이 먼저일까요?.
- Option (695:3471): characters ← 투표 선택지 1 표시; 텍스트 ○ 보행로 정비    52표 · 65%.
- Option (695:3474): characters ← 투표 선택지 2 표시; 텍스트 ○ 조명 개선    28표 · 35%.
- 본인 선택 (695:3476): characters ← 투표 내 선택 표시; 텍스트 내 선택: 없음  |  최다: 보행로 정비.
- 실제 UI 상태 메시지 / 투표할 항목을 선택해 주세요. (804:4736): visible ← WF-F11-투표-제출후/695:3477/메시지 표시.

#### [WF-F12-지역활동정보상세-종료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=695-3334)

노드 ID: 695:3334. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 활동 정보 · 지역 활동 정보 · 생활정보 · 월계1동 · 월계 주민 걷기 모임 · 출처  주민 모임 안내 · 일정  10월 10일 10:00 · 장소  월계동 공원 입구 · 상태  종료 · 외부 참여 경로 · 비활성 · 주최자 문의 정보를 확인할 수 없습니다. · 취소 상태도 외부 참여 경로가 비활성화됩니다. · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-F16-월계2동안건-열람전용](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4412)

노드 ID: 789:4412. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 안건 · 탐색 지역 · 월계2동 / 이웃 인증 없음 / 열람은 가능하며 반응·댓글·투표·글쓰기는 제한됩니다. · 지역 안건 · 교통 · 월계2동 · 월계2동 이웃 · 10월 5일  › · 월계도서관 공사 현황 · 월계도서관 공사 진행 상황과 이용 안내를 함께 확인하고 주민 의견을 나눕니다. · 참고 자료 · 출처 링크 · AI 요약 · 사용자 첨부 사진 · 1/2 · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 지역 소식을 잘 확인했습니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 관련 안내도 함께 살펴보면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 월계2동 이웃 인증 후 작성할 수 있습니다. · 댓글 등록 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| AI 요약 | ON_CLICK | 이동 → WF-F21-AI요약-월계2동-지도안건 |
| 공감해요 20 | ON_CLICK | 이동 → WF-F20-탐색지역-참여제한안내 |
| 필요해요 10 | ON_CLICK | 이동 → WF-F20-탐색지역-참여제한안내 |
| 궁금해요 2 | ON_CLICK | 이동 → WF-F20-탐색지역-참여제한안내 |
| 북마크 | ON_CLICK | 변수 WF-F16-월계2동안건-열람전용/789:4437/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-B11-하계2동-일반회원기능안내 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-F16-월계2동안건-열람전용 → 이동 → WF-F08-신고-입력 |
| 좋아요 3   싫어요 0   답글 달기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 좋아요 3   싫어요 0   답글 달기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 댓글 등록 | ON_CLICK | 이동 → WF-F20-탐색지역-참여제한안내 |
| 메인 | ON_CLICK | 이동 → WF-B03-메인-하계2동-이웃미인증 |
| 지도 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |
| 글쓰기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 북마크에 저장되었습니다. (804:4751): visible ← WF-F16-월계2동안건-열람전용/789:4437/메시지 표시.

#### [WF-F17-월계3동안건-열람전용](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4562)

노드 ID: 789:4562. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 안건 · 탐색 지역 · 월계3동 / 이웃 인증 없음 / 열람은 가능하며 반응·댓글·투표·글쓰기는 제한됩니다. · 지역 안건 · 교통 · 월계3동 · 월계3동 이웃 · 10월 5일  › · 스타필드 입점 · 스타필드 입점과 관련한 지역 변화와 생활 환경에 대한 주민 의견을 확인합니다. · 참고 자료 · 출처 링크 · AI 요약 · 사용자 첨부 사진 · 1/2 · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 지역 소식을 잘 확인했습니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 관련 안내도 함께 살펴보면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 월계3동 이웃 인증 후 작성할 수 있습니다. · 댓글 등록 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| AI 요약 | ON_CLICK | 이동 → WF-F22-AI요약-월계3동-지도안건 |
| 공감해요 20 | ON_CLICK | 이동 → WF-F20-탐색지역-참여제한안내 |
| 필요해요 10 | ON_CLICK | 이동 → WF-F20-탐색지역-참여제한안내 |
| 궁금해요 2 | ON_CLICK | 이동 → WF-F20-탐색지역-참여제한안내 |
| 북마크 | ON_CLICK | 변수 WF-F17-월계3동안건-열람전용/789:4587/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-B11-하계2동-일반회원기능안내 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-F17-월계3동안건-열람전용 → 이동 → WF-F08-신고-입력 |
| 좋아요 3   싫어요 0   답글 달기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 좋아요 3   싫어요 0   답글 달기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |
| 댓글 등록 | ON_CLICK | 이동 → WF-F20-탐색지역-참여제한안내 |
| 메인 | ON_CLICK | 이동 → WF-B03-메인-하계2동-이웃미인증 |
| 지도 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |
| 글쓰기 | ON_CLICK | 이동 → WF-B09-하계2동-지역참여제한안내 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 북마크에 저장되었습니다. (804:4756): visible ← WF-F17-월계3동안건-열람전용/789:4587/메시지 표시.

#### [WF-F18-하계1동-독서모임-열람전용](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4712)

노드 ID: 789:4712. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 활동 정보 · 탐색 지역 · 하계1동 / 이웃 인증 없음 / 열람은 가능하며 지역 참여는 제한됩니다. · 지역 활동 정보 · 생활정보 · 하계1동 · 동네이웃 ·   › · 과기대 독서 모임 · 이웃과 함께 책을 읽고 이야기를 나누는 과기대 독서 모임 안내입니다. · 출처  주민 모임 안내 · 일정  10월 10일 10:00 · 장소  과기대 모임 장소 · 상태  진행 · 외부 참여 경로 열기 · 주최자 문의  neighbor@example.com · 참고 링크 (선택) · 별도 참고 자료 · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 모임 안내를 잘 확인했습니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 함께 책 이야기를 나누면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 하계1동 이웃 인증 후 작성할 수 있습니다. · 댓글 등록 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 공감해요 20 | ON_CLICK | 이동 → WF-F20-탐색지역-참여제한안내 |
| 필요해요 10 | ON_CLICK | 이동 → WF-F20-탐색지역-참여제한안내 |
| 궁금해요 2 | ON_CLICK | 이동 → WF-F20-탐색지역-참여제한안내 |
| 북마크 | ON_CLICK | 변수 WF-F18-하계1동-독서모임-열람전용/789:4740/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-G04-공유-링크복사 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-F18-하계1동-독서모임-열람전용 → 이동 → WF-F08-신고-입력 |
| 댓글 등록 | ON_CLICK | 이동 → WF-F20-탐색지역-참여제한안내 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 북마크에 저장되었습니다. (804:4761): visible ← WF-F18-하계1동-독서모임-열람전용/789:4740/메시지 표시.

#### [WF-F19-지역안건-새추천상세](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4831)

노드 ID: 789:4831. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 안건 · 지역 안건 · 교통 · 월계1동 · 동네이웃 · 기관 인증 배지 자리 · 10월 5일  › · 광운대역 야간 조명 개선 · 야간 보행이 안전하도록 광운대역 주변의 어두운 구간을 살펴보고 조명 개선을 제안합니다. · 참고 자료 · 출처 링크 · AI 요약 · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 보행로 정비가 먼저 필요합니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 야간 조명도 함께 살펴보면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 의견을 남겨 주세요. · 댓글 등록 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| AI 요약 | ON_CLICK | 이동 → WF-F23-AI요약-월계1동-새추천 |
| 북마크 | ON_CLICK | 변수 WF-F19-지역안건-새추천상세/789:4854/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-G04-공유-링크복사 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-F19-지역안건-새추천상세 → 이동 → WF-F08-신고-입력 |
| 댓글 등록 | ON_CLICK | 변수 WF-F19-지역안건-새추천상세/789:4867/메시지 내용 ← 댓글이 등록되었습니다. → 변수 WF-F19-지역안건-새추천상세/789:4867/메시지 표시 ← true |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- 실제 UI 상태 메시지 / 북마크에 저장되었습니다. (804:4766): visible ← WF-F19-지역안건-새추천상세/789:4854/메시지 표시.
- 실제 UI 상태 메시지 / 댓글이 등록되었습니다. (804:4771): visible ← WF-F19-지역안건-새추천상세/789:4867/메시지 표시.

#### [WF-F20-탐색지역-참여제한안내](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4356)

노드 ID: 789:4356. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 참여 안내 · 이 지역 이웃 인증이 없어 지역 참여를 할 수 없습니다. / 게시물·댓글·투표 결과는 열람할 수 있습니다. 탐색 지역 변경은 참여 권한을 부여하지 않습니다. · 돌아가기 · 기본 활동 지역 월계1동으로 돌아가기 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 돌아가기 | ON_CLICK | 이전 화면으로 |
| 기본 활동 지역 월계1동으로 돌아가기 | ON_CLICK | 이동 → WF-B01-메인 |
| 메인 | ON_CLICK | 이동 → WF-B03-메인-하계2동-이웃미인증 |
| 지도 | ON_CLICK | 이동 → WF-B08-이슈지도-하계2동 |

#### [WF-F21-AI요약-월계2동-지도안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4516)

노드 ID: 789:4516. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · AI 요약 · 월계도서관 공사 현황 · 월계도서관 공사 진행 상황과 이용 안내를 함께 확인하고 주민 의견을 나눕니다. · 원문 보기 · 닫기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 원문 보기 | ON_CLICK | 이동 → WF-F16-월계2동안건-열람전용 |
| 닫기 | ON_CLICK | 이전 화면으로 |

#### [WF-F22-AI요약-월계3동-지도안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4666)

노드 ID: 789:4666. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · AI 요약 · 스타필드 입점 · 스타필드 입점과 관련한 지역 변화와 생활 환경에 대한 주민 의견을 확인합니다. · 원문 보기 · 닫기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 원문 보기 | ON_CLICK | 이동 → WF-F17-월계3동안건-열람전용 |
| 닫기 | ON_CLICK | 이전 화면으로 |

#### [WF-F23-AI요약-월계1동-새추천](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=789-4926)

노드 ID: 789:4926. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · AI 요약 · 광운대역 야간 조명 개선 · 야간 보행이 안전하도록 광운대역 주변의 어두운 구간을 살펴보고 조명 개선을 제안합니다. · 원문 보기 · 닫기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 원문 보기 | ON_CLICK | 이동 → WF-F19-지역안건-새추천상세 |
| 닫기 | ON_CLICK | 이전 화면으로 |

#### [WF-F24-내게시물상세-지역안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=895-4421)

노드 ID: 895:4421. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 안건 · 지역 안건 · 월계1동 · 교통 · 동네이웃 · 기관 인증 배지 자리 · 10월 5일  › · 광운대역 주변 보행로 개선 · 출퇴근 시간에 사람이 많이 모이는 구간을 살펴보고 보행로를 개선하면 좋겠습니다. 우리 동네에서 필요한 부분을 함께 이야기해 주세요. · 참고 자료 · 출처 링크 · 사용자 첨부 사진 · 1/2 · AI 요약 · 주민이 광운대역 주변 보행로 개선을 제안했습니다. 출퇴근 시간에 보행자가 몰리는 구간을 먼저 살펴보자는 내용입니다. 게시물에서 주민 의견을 함께 확인할 수 있습니다. · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 보행로 정비가 먼저 필요합니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 야간 조명도 함께 살펴보면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 의견을 남겨 주세요. · 댓글 등록 · 게시물 수정

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-I03-내가만든게시물 |
| 북마크 | ON_CLICK | 변수 WF-F01-지역안건상세-사진있음/694:3350/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-G04-공유-링크복사 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-F01-지역안건상세-사진있음 → 이동 → WF-F08-신고-입력 |
| 댓글 입력 / 의견을 남겨 주세요. | ON_CLICK | 변수 WF-F01-지역안건상세-사진있음/694:3376/메시지 표시 ← false |
| 댓글 등록 | ON_CLICK | 변수 WF-F01-지역안건상세-사진있음/694:3376/메시지 내용 ← 댓글이 등록되었습니다. → 변수 WF-F01-지역안건상세-사진있음/694:3376/메시지 표시 ← true |
| 게시물 수정 | ON_CLICK | 이동 → WF-E09-내지역안건-수정 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- NavItem / 메인 (895:4463): 이동 → WF-B01-메인.
- NavItem / 지도 (895:4464): 이동 → WF-D01-전체지도.
- NavItem / 글쓰기 (895:4465): 이동 → WF-E01-글쓰기-유형선택.
- NavItem / 알림 (895:4466): 이동 → WF-H01-알림및활동.
- NavItem / 마이 (895:4467): 이동 → WF-I01-마이페이지.

#### [WF-F25-내게시물상세-진행중투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=895-4511)

노드 ID: 895:4511. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 투표 · 투표 · 교통 · 월계1동 · 진행 중 · 동네이웃 ·   › · 보행로 개선, 무엇이 먼저일까요? · 우선 개선할 항목을 선택해 의견을 보태 주세요. · 무엇이 먼저 필요할까요? · 진행 중 · 80명 · 남은 2일 · ○ 보행로 정비    52표 · 65% · ○ 조명 개선    28표 · 35% · 투표 제출 · 참고 링크 · 참고 자료 · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 8 · 정렬 · 좋아요순 · 최신순 · 민수 · 방금 · 보행로 정비가 먼저 필요합니다. · 좋아요 3   싫어요 0   답글 달기 · 동네이웃 · 방금 · @민수 야간 조명도 함께 살펴보면 좋겠어요. · 좋아요 3   싫어요 0   답글 달기 · 댓글 입력 · 의견을 남겨 주세요. · 댓글 등록 · 게시물 수정 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-I03-내가만든게시물 |
| ○ 보행로 정비    52표 · 65% | ON_CLICK | 변수 투표 선택 초안 ← 보행로 정비 → 변수 투표 선택지 1 표시 ← ● 보행로 정비    52표 · 65% → 변수 투표 선택지 2 표시 ← ○ 조명 개선    28표 · 35% |
| ○ 조명 개선    28표 · 35% | ON_CLICK | 변수 투표 선택 초안 ← 조명 개선 → 변수 투표 선택지 1 표시 ← ○ 보행로 정비    52표 · 65% → 변수 투표 선택지 2 표시 ← ● 조명 개선    28표 · 35% |
| 투표 제출 | ON_CLICK | 변수 WF-F04-투표상세-진행중/694:3579/메시지 표시 ← false → (투표 선택 초안 = "" )인 경우: 변수 WF-F04-투표상세-진행중/694:3579/메시지 표시 ← true → ((투표 제출 선택 ≠ ) 그리고 ((투표 선택 초안 ≠ ) 그리고 (투표 제출 선택 ≠ 투표 선택 초안)))인 경우: 이동 → 목적지 없음 · 확인 필요 → ((투표 선택 초안 = 보행로 정비) 그리고 ((투표 제출 선택 = "" ) 또는 (투표 제출 선택 = 보행로 정비)))인 경우: 변수 투표 제출 선택 ← 보행로 정비 → 변수 투표 내 선택 표시 ← 내 선택: 보행로 정비  \|  최다: 보행로 정비 → 변수 투표 선택 초안 ← 보행로 정비 → 변수 투표 선택지 1 표시 ← ● 보행로 정비    52표 · 65% → 변수 투표 선택지 2 표시 ← ○ 조명 개선    28표 · 35% → 이동 → WF-F11-투표-제출후 → ((투표 선택 초안 = 조명 개선) 그리고 ((투표 제출 선택 = "" ) 또는 (투표 제출 선택 = 조명 개선)))인 경우: 변수 투표 제출 선택 ← 조명 개선 → 변수 투표 내 선택 표시 ← 내 선택: 조명 개선  \|  최다: 보행로 정비 → 변수 투표 선택 초안 ← 조명 개선 → 변수 투표 선택지 1 표시 ← ○ 보행로 정비    52표 · 65% → 변수 투표 선택지 2 표시 ← ● 조명 개선    28표 · 35% → 이동 → WF-F11-투표-제출후 |
| 북마크 | ON_CLICK | 변수 WF-F04-투표상세-진행중/694:3592/메시지 표시 ← true |
| 공유 | ON_CLICK | 이동 → WF-G04-공유-링크복사 |
| 신고 | ON_CLICK | 변수 신고 원문 화면 ← WF-F04-투표상세-진행중 → 이동 → WF-F08-신고-입력 |
| 댓글 등록 | ON_CLICK | 변수 WF-F04-투표상세-진행중/694:3618/메시지 내용 ← 댓글이 등록되었습니다. → 변수 WF-F04-투표상세-진행중/694:3618/메시지 표시 ← true |
| 게시물 수정 | ON_CLICK | 이동 → WF-E06-진행중투표-수정 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

상태 표시 바인딩:

- 섹션 제목 (895:4525): characters ← 현재 투표 제목; 텍스트 보행로 개선, 무엇이 먼저일까요?.
- Option (895:4531): characters ← 투표 선택지 1 표시; 텍스트 ○ 보행로 정비    52표 · 65%.
- Option (895:4534): characters ← 투표 선택지 2 표시; 텍스트 ○ 조명 개선    28표 · 35%.
- 실제 UI 상태 메시지 / 투표할 항목을 선택해 주세요. (895:4537): visible ← WF-F04-투표상세-진행중/694:3579/메시지 표시.
- 실제 UI 상태 메시지 / 북마크에 저장되었습니다. (895:4546): visible ← WF-F04-투표상세-진행중/694:3592/메시지 표시.
- 실제 UI 상태 메시지 / 댓글이 등록되었습니다. (895:4560): visible ← WF-F04-투표상세-진행중/694:3618/메시지 표시.

### G 화면별 연결

#### [WF-G01-공유-게스트상세](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1848)

노드 ID: 692:1848. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 공유 게시물 · 공유 링크로 바로 열람 · 게스트 · 지역 안건 · 교통 · 월계1동 · 동네이웃 · 기관 인증 배지 자리 · 10월 5일  › · 광운대역 주변 보행로 개선 · 출퇴근 시간에 사람이 많이 모이는 구간을 살펴보고 보행로를 개선하면 좋겠습니다. 우리 동네에서 필요한 부분을 함께 이야기해 주세요. · 참고 자료 · 출처 링크 · AI 요약 · 공유 게시물 첨부 사진 · 공감해요 20 · 필요해요 10 · 궁금해요 2 · 북마크 · 공유 · 신고 · 댓글 · 게스트 · 방금 · @민수 보행 환경이 개선되면 좋겠습니다. · 공개 댓글 · 게스트 · 좋아요 · 싫어요 · 답글 · 게스트 댓글 · 의견을 남겨 주세요. · 댓글 등록

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| AI 요약 | ON_CLICK | 이동 → WF-G06-게스트-AI요약 |
| 공감해요 20 | ON_CLICK | 변수 returnTo 있음 ← true → 변수 returnTo 투표 ← false → 이동 → WF-G02-로그인-원게시물복귀 |
| 필요해요 10 | ON_CLICK | 변수 returnTo 있음 ← true → 변수 returnTo 투표 ← false → 이동 → WF-G02-로그인-원게시물복귀 |
| 궁금해요 2 | ON_CLICK | 변수 returnTo 있음 ← true → 변수 returnTo 투표 ← false → 이동 → WF-G02-로그인-원게시물복귀 |
| 북마크 | ON_CLICK | 변수 returnTo 있음 ← true → 변수 returnTo 투표 ← false → 이동 → WF-G02-로그인-원게시물복귀 |
| 공유 | ON_CLICK | 변수 G04 링크 복사 안내 ← false → 이동 → WF-G04-공유-링크복사 |
| 신고 | ON_CLICK | 변수 returnTo 있음 ← true → 변수 returnTo 투표 ← false → 이동 → WF-G02-로그인-원게시물복귀 |
| 좋아요 | ON_CLICK | 변수 returnTo 있음 ← true → 변수 returnTo 투표 ← false → 이동 → WF-G02-로그인-원게시물복귀 |
| 싫어요 | ON_CLICK | 변수 returnTo 있음 ← true → 변수 returnTo 투표 ← false → 이동 → WF-G02-로그인-원게시물복귀 |
| 답글 | ON_CLICK | 변수 G07 답글 원문은 투표 ← false → 이동 → WF-G07-게스트-답글작성 |
| 게스트 댓글 / 의견을 남겨 주세요. | ON_CLICK | 변수 G01 댓글·답글 등록 안내 ← false |
| 댓글 등록 | ON_CLICK | 변수 G01 댓글·답글 등록 안내 ← true |

상태 표시 바인딩:

- Notice / 게스트 의견 등록 완료 (856:4425): visible ← G01 댓글·답글 등록 안내.

#### [WF-G02-로그인-원게시물복귀](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=812-6012)

노드 ID: 812:6012. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 로그인 · 로그인이 필요한 기능입니다. · 등록 이메일 · 이메일 주소 · 비밀번호 · 영문과 숫자를 포함한 8자 이상으로 입력해 주세요. · 비밀번호 찾기 · 로그인 · 회원가입

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 변수 returnTo 있음 ← false → 변수 returnTo 투표 ← false → 이전 화면으로 |
| 비밀번호 찾기 | ON_CLICK | 이동 → WF-A03-비밀번호찾기 |
| 로그인 | ON_CLICK | ((returnTo 있음 = true) 그리고 (returnTo 투표 = true))인 경우: 이동 → WF-G09-로그인복귀-미인증회원투표 → ((returnTo 있음 = true) 그리고 (returnTo 투표 = false))인 경우: 이동 → WF-G05-로그인복귀-미인증회원상세 → (returnTo 있음 = false)인 경우: 이동 → WF-B01-메인 |
| 회원가입 | ON_CLICK | 이동 → WF-A06-회원가입-계정정보·약관 |

#### [WF-G04-공유-링크복사](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1923)

노드 ID: 692:1923. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 공유 · 공개 게시물 링크 · 공유 링크 · 이 게시물의 공개 링크 · 링크 복사 · 닫기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 링크 복사 | ON_CLICK | 변수 G04 링크 복사 안내 ← true |
| 닫기 | ON_CLICK | 이전 화면으로 |

상태 표시 바인딩:

- Notice / 링크 복사 완료 (856:4438): visible ← G04 링크 복사 안내.

#### [WF-G05-로그인복귀-미인증회원상세](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1948)

노드 ID: 692:1948. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 원 게시물 · 로그인 완료 · 원 게시물 복귀 · 지역 안건 · 교통 · 월계1동 · 동네이웃 · 기관 인증 배지 자리 · 10월 5일  › · 광운대역 주변 보행로 개선 · 출퇴근 시간에 사람이 많이 모이는 구간을 살펴보고 보행로를 개선하면 좋겠습니다. 우리 동네에서 필요한 부분을 함께 이야기해 주세요. · 참고 자료 · 출처 링크 · AI 요약 · 주민이 광운대역 주변 보행로 개선을 제안했습니다. 출퇴근 시간에 보행자가 몰리는 구간을 먼저 살펴보자는 내용입니다. 게시물에서 주민 의견을 함께 확인할 수 있습니다. · 북마크 · 반응 · 이 지역 이웃 인증이 필요합니다. · 이웃 인증하기 · 복귀만으로 북마크·반응·투표는 저장되지 않습니다. · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 변수 returnTo 있음 ← false → 변수 returnTo 투표 ← false → 이동 → WF-B01-메인 |
| 북마크 | ON_CLICK | 이동 → WF-F07-북마크-저장성공 |
| 반응 | ON_CLICK | 이동 → WF-J01-이웃인증-제출 |
| 이웃 인증하기 | ON_CLICK | 이동 → WF-J01-이웃인증-제출 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-G06-게스트-AI요약](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=695-3384)

노드 ID: 695:3384. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 공유 게시물 · AI 요약 · 광운대역 주변 보행로 개선 · 주민이 광운대역 주변 보행로 개선을 제안했습니다. 출퇴근 시간에 보행자가 몰리는 구간을 먼저 살펴보자는 내용입니다. 게시물에서 주민 의견을 함께 확인할 수 있습니다. · 공유받은 원문 보기 · 닫기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 공유받은 원문 보기 | ON_CLICK | 이동 → WF-G01-공유-게스트상세 |
| 닫기 | ON_CLICK | 이전 화면으로 |

#### [WF-G07-게스트-답글작성](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=695-3409)

노드 ID: 695:3409. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 답글 작성 · 민수 · 방금 · 보행로 정비가 먼저 필요합니다. · 좋아요 3   싫어요 0   답글 달기 · 게스트 · @민수에게 답글 · 답글 내용 · 우리 동네 의견을 남겨 주세요. · 게스트로 등록

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 게스트로 등록 | ON_CLICK | (G07 답글 원문은 투표 = false)인 경우: 변수 G01 댓글·답글 등록 안내 ← true → (G07 답글 원문은 투표 = true)인 경우: 변수 G08 댓글·답글 등록 안내 ← true → 이전 화면으로 |

#### [WF-G08-게스트-공유투표결과](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=695-3434)

노드 ID: 695:3434. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 공유 투표 · 공유 링크로 직접 열람 · 게스트 · 투표 · 교통 · 월계1동 · 진행 중 · 보행로 개선 우선순위 · 질문  무엇이 먼저 필요할까요? · 보행로 정비 52표 · 65% · 조명 개선 28표 · 35% · 전체 참여 인원 80명 · 남은 2일 · 투표 참여 · 북마크 · 게스트 · 방금 · 의견을 남겨 주세요. · 좋아요 3   싫어요 0   답글 달기 · 게스트 댓글 · 가입 없이 댓글 작성 · 댓글 등록 · 답글 작성

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 투표 참여 | ON_CLICK | 변수 returnTo 있음 ← true → 변수 returnTo 투표 ← true → 이동 → WF-G02-로그인-원게시물복귀 |
| 북마크 | ON_CLICK | 변수 returnTo 있음 ← true → 변수 returnTo 투표 ← true → 이동 → WF-G02-로그인-원게시물복귀 |
| 게스트 댓글 / 가입 없이 댓글 작성 | ON_CLICK | 변수 G08 댓글·답글 등록 안내 ← false |
| 댓글 등록 | ON_CLICK | 변수 G08 댓글·답글 등록 안내 ← true |
| 답글 작성 | ON_CLICK | 변수 G07 답글 원문은 투표 ← true → 이동 → WF-G07-게스트-답글작성 |

상태 표시 바인딩:

- Notice / 게스트 의견 등록 완료 (856:4431): visible ← G08 댓글·답글 등록 안내.

#### [WF-G09-로그인복귀-미인증회원투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=699-3496)

노드 ID: 699:3496. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 원 투표 · 로그인 완료 · 원 투표 복귀 · 투표 · 교통 · 월계1동 · 진행 중 · 보행로 개선 우선순위 · 질문  무엇이 먼저 필요할까요? · 보행로 정비 52표 · 65% · 조명 개선 28표 · 35% · 전체 참여 인원 80명 · 남은 2일 · 투표 참여 · 북마크 · 로그인 완료 · 투표는 자동 제출되지 않습니다. · 이 지역 이웃 인증 후 투표·댓글 참여가 가능합니다. · 이웃 인증하기 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 변수 returnTo 있음 ← false → 변수 returnTo 투표 ← false → 이동 → WF-B01-메인 |
| 투표 참여 | ON_CLICK | 이동 → WF-J01-이웃인증-제출 |
| 북마크 | ON_CLICK | 이동 → WF-F07-북마크-저장성공 |
| 이웃 인증하기 | ON_CLICK | 이동 → WF-J01-이웃인증-제출 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

### H 화면별 연결

#### [WF-H01-알림및활동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1973)

노드 ID: 692:1973. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 알림 및 활동 · 보기 · ● 전체 · 알림 · 활동 · ● 미확인 · □ 댓글 · 5분 전 · 새 댓글이 달렸어요 · 보행로 개선 안건에 의견이 도착했습니다. · ● 미확인 · □ 투표 · 1시간 전 · 투표가 곧 종료돼요 · 참여한 투표의 종료 전 알림입니다. · □ 기관 · 어제 · 기관 채택이 취소되었습니다 · 해당 안건의 채택 기록을 확인해 주세요. · □ 신고 · 어제 · 게시물이 삭제되었습니다 · 삭제 결과 안내를 확인해 주세요. · ● 미확인 · □ 반응 · 방금 전 · 새로운 반응이 달렸어요 · 내 게시물에 새로운 반응이 추가되었습니다. · □ 관심 지역 · 오늘 · 관심 지역에 새 의제가 올라왔어요 · 월계1동의 새로운 의제를 확인해 보세요. · ● 미확인 · □ 투표 결과 · 오늘 · 투표 결과가 나왔어요 · 참여한 투표의 결과를 확인해 보세요. · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동-알림 |
| 활동 | ON_CLICK | 이동 → WF-H01-알림및활동-활동 |
| ● 미확인 · □ 댓글 · 5분 전 / 새 댓글이 달렸어요 / 보행로 개선 안건에 의견이 도착했습니다. | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| ● 미확인 · □ 투표 · 1시간 전 / 투표가 곧 종료돼요 / 참여한 투표의 종료 전 알림입니다. | ON_CLICK | 이동 → WF-F04-투표상세-진행중 |
| □ 기관 · 어제 / 기관 채택이 취소되었습니다 / 해당 안건의 채택 기록을 확인해 주세요. | ON_CLICK | 이동 → WF-K04-지역안건-채택기록 |
| □ 신고 · 어제 / 게시물이 삭제되었습니다 / 삭제 결과 안내를 확인해 주세요. | ON_CLICK | 이동 → WF-H03-삭제된게시물안내 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-H01-알림및활동-알림](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1034-6049)

노드 ID: 1034:6049. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 알림 및 활동 · 보기 · 전체 · 알림 · 활동 · ● 미확인 · □ 댓글 · 5분 전 · 새 댓글이 달렸어요 · 보행로 개선 안건에 의견이 도착했습니다. · ● 미확인 · □ 투표 · 1시간 전 · 투표가 곧 종료돼요 · 참여한 투표의 종료 전 알림입니다. · □ 기관 · 어제 · 기관 채택이 취소되었습니다 · 해당 안건의 채택 기록을 확인해 주세요. · □ 신고 · 어제 · 게시물이 삭제되었습니다 · 삭제 결과 안내를 확인해 주세요. · ● 미확인 · □ 반응 · 방금 전 · 새로운 반응이 달렸어요 · 내 게시물에 새로운 반응이 추가되었습니다. · □ 관심 지역 · 오늘 · 관심 지역에 새 의제가 올라왔어요 · 월계1동의 새로운 의제를 확인해 보세요. · ● 미확인 · □ 투표 결과 · 오늘 · 투표 결과가 나왔어요 · 참여한 투표의 결과를 확인해 보세요. · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| ● 미확인 · □ 댓글 · 5분 전 / 새 댓글이 달렸어요 / 보행로 개선 안건에 의견이 도착했습니다. | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| ● 미확인 · □ 투표 · 1시간 전 / 투표가 곧 종료돼요 / 참여한 투표의 종료 전 알림입니다. | ON_CLICK | 이동 → WF-F04-투표상세-진행중 |
| □ 기관 · 어제 / 기관 채택이 취소되었습니다 / 해당 안건의 채택 기록을 확인해 주세요. | ON_CLICK | 이동 → WF-K04-지역안건-채택기록 |
| □ 신고 · 어제 / 게시물이 삭제되었습니다 / 삭제 결과 안내를 확인해 주세요. | ON_CLICK | 이동 → WF-H03-삭제된게시물안내 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-H01-알림및활동-활동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1034-6101)

노드 ID: 1034:6101. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 알림 및 활동 · 보기 · 전체 · 알림 · 활동 · ● 미확인 · □ 댓글 · 5분 전 · 새 댓글이 달렸어요 · 보행로 개선 안건에 의견이 도착했습니다. · ● 미확인 · □ 투표 · 1시간 전 · 투표가 곧 종료돼요 · 참여한 투표의 종료 전 알림입니다. · □ 기관 · 어제 · 기관 채택이 취소되었습니다 · 해당 안건의 채택 기록을 확인해 주세요. · □ 신고 · 어제 · 게시물이 삭제되었습니다 · 삭제 결과 안내를 확인해 주세요. · ● 미확인 · □ 반응 · 방금 전 · 새로운 반응이 달렸어요 · 내 게시물에 새로운 반응이 추가되었습니다. · □ 관심 지역 · 오늘 · 관심 지역에 새 의제가 올라왔어요 · 월계1동의 새로운 의제를 확인해 보세요. · ● 미확인 · □ 투표 결과 · 오늘 · 투표 결과가 나왔어요 · 참여한 투표의 결과를 확인해 보세요. · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| ● 미확인 · □ 댓글 · 5분 전 / 새 댓글이 달렸어요 / 보행로 개선 안건에 의견이 도착했습니다. | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| ● 미확인 · □ 투표 · 1시간 전 / 투표가 곧 종료돼요 / 참여한 투표의 종료 전 알림입니다. | ON_CLICK | 이동 → WF-F04-투표상세-진행중 |
| □ 기관 · 어제 / 기관 채택이 취소되었습니다 / 해당 안건의 채택 기록을 확인해 주세요. | ON_CLICK | 이동 → WF-K04-지역안건-채택기록 |
| □ 신고 · 어제 / 게시물이 삭제되었습니다 / 삭제 결과 안내를 확인해 주세요. | ON_CLICK | 이동 → WF-H03-삭제된게시물안내 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-H02-알림및활동-빈상태](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-1998)

노드 ID: 692:1998. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 알림 및 활동 · 보기 · 전체 · 알림 · 활동 · 아직 알림이 없습니다. · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-H03-삭제된게시물안내](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2023)

노드 ID: 692:2023. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 삭제된 게시물 안내 · 게시물이 삭제되었습니다. · 삭제된 게시물의 내용을 다시 볼 수 없습니다. · 알림으로

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 알림으로 | ON_CLICK | 이동 → WF-H01-알림및활동 |

### I 화면별 연결

#### [WF-I01-마이페이지](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2048)

노드 ID: 692:2048. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 마이페이지 · 설정 · 프로필 사진 · 동네이웃 · 거주자 · 학생 | 기본 활동 지역 월계1동 · 우리 동네 이야기에 관심이 많아요. · 프로필 / 한 줄 소개 수정 ·   › · 나의 활동 · 실제 행동 횟수 · 작성 3 · 북마크 5 · 반응 12 · 댓글·답글 4 · 평가 6 · 투표 2 · 북마크 ·   › · 내가 만든 게시물 ·   › · 반응한 게시물 ·   › · 참여한 투표 ·   › · 이웃 인증 ·   › · 기관 인증 ·   › · 계정 ·   › · 활동 지역 ·   › · 관심 지역 ·   › · 알림 및 활동 내역 ·   › · 설정 ·   › · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 설정 | ON_CLICK | 이동 → WF-L01-설정 |
| 프로필 / 한 줄 소개 수정 /   › | ON_CLICK | 이동 → WF-I06-프로필수정 |
| 북마크 /   › | ON_CLICK | 이동 → WF-I02-북마크목록 |
| 내가 만든 게시물 /   › | ON_CLICK | 이동 → WF-I03-내가만든게시물 |
| 반응한 게시물 /   › | ON_CLICK | 이동 → WF-I04-반응한게시물 |
| 참여한 투표 /   › | ON_CLICK | 이동 → WF-I05-참여한투표 |
| 이웃 인증 /   › | ON_CLICK | 이동 → WF-J01-이웃인증-제출 |
| 기관 인증 /   › | ON_CLICK | 이동 → WF-J03-기관인증-정보 |
| 계정 /   › | ON_CLICK | 이동 → WF-L02-계정관리-마이페이지진입 |
| 활동 지역 /   › | ON_CLICK | 이동 → WF-I07-기본활동지역 |
| 관심 지역 /   › | ON_CLICK | 이동 → WF-I08-관심지역 |
| 알림 및 활동 내역 /   › | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 설정 /   › | ON_CLICK | 이동 → WF-L01-설정 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |

#### [WF-I02-북마크-유형-지역안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-4825)

노드 ID: 947:4825. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 북마크 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 첫 번째 사용자 첨부 사진 · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I02-북마크-유형-지역활동정보 |
| 투표 | ON_CLICK | 이동 → WF-I02-북마크-유형-투표 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 주변 보행로 개선 / 첫 번째 사용자 첨부 사진 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 월계 주민 걷기 모임 (947:4857): 이동 → WF-F03-지역활동정보상세.

#### [WF-I02-북마크-유형-지역활동정보](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-4868)

노드 ID: 947:4868. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 북마크 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 지역 활동 정보 · 월계1동 · 교통 · 월계 주민 걷기 모임 · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 | ON_CLICK | 이동 → WF-I02-북마크-유형-지역안건 |
| 투표 | ON_CLICK | 이동 → WF-I02-북마크-유형-투표 |
| 지역 활동 정보 · 월계1동 · 교통 / 월계 주민 걷기 모임 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F03-지역활동정보상세 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCardPhoto / 광운대역 주변 보행로 개선 (947:4899): 이동 → WF-F01-지역안건상세-사진있음.

#### [WF-I02-북마크-유형-투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-4911)

노드 ID: 947:4911. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 북마크 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 | ON_CLICK | 이동 → WF-I02-북마크-유형-지역안건 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I02-북마크-유형-지역활동정보 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCardPhoto / 광운대역 주변 보행로 개선 (947:4942): 이동 → WF-F01-지역안건상세-사진있음.
- PostCard / 월계 주민 걷기 모임 (947:4943): 이동 → WF-F03-지역활동정보상세.

#### [WF-I02-북마크-주제-교통](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-4954)

노드 ID: 947:4954. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 북마크 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 첫 번째 사용자 첨부 사진 · 반응 32 · 댓글 8 · 지역 활동 정보 · 월계1동 · 교통 · 월계 주민 걷기 모임 · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 | ON_CLICK | 이동 → WF-I02-북마크목록 |
| 주거 | ON_CLICK | 이동 → WF-I02-북마크-주제-주거 |
| 안전 | ON_CLICK | 이동 → WF-I02-북마크-주제-안전 |
| 복지 | ON_CLICK | 이동 → WF-I02-북마크-주제-복지 |
| 생활정보 | ON_CLICK | 이동 → WF-I02-북마크-주제-생활정보 |
| 환경 | ON_CLICK | 이동 → WF-I02-북마크-주제-환경 |
| 기타 | ON_CLICK | 이동 → WF-I02-북마크-주제-기타 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 주변 보행로 개선 / 첫 번째 사용자 첨부 사진 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| 지역 활동 정보 · 월계1동 · 교통 / 월계 주민 걷기 모임 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F03-지역활동정보상세 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-I02-북마크-주제-기타](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-5212)

노드 ID: 947:5212. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 북마크 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 | ON_CLICK | 이동 → WF-I02-북마크목록 |
| 교통 | ON_CLICK | 이동 → WF-I02-북마크-주제-교통 |
| 주거 | ON_CLICK | 이동 → WF-I02-북마크-주제-주거 |
| 안전 | ON_CLICK | 이동 → WF-I02-북마크-주제-안전 |
| 복지 | ON_CLICK | 이동 → WF-I02-북마크-주제-복지 |
| 생활정보 | ON_CLICK | 이동 → WF-I02-북마크-주제-생활정보 |
| 환경 | ON_CLICK | 이동 → WF-I02-북마크-주제-환경 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCardPhoto / 광운대역 주변 보행로 개선 (947:5243): 이동 → WF-F01-지역안건상세-사진있음.
- PostCard / 월계 주민 걷기 모임 (947:5244): 이동 → WF-F03-지역활동정보상세.

#### [WF-I02-북마크-주제-복지](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-5083)

노드 ID: 947:5083. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 북마크 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 | ON_CLICK | 이동 → WF-I02-북마크목록 |
| 교통 | ON_CLICK | 이동 → WF-I02-북마크-주제-교통 |
| 주거 | ON_CLICK | 이동 → WF-I02-북마크-주제-주거 |
| 안전 | ON_CLICK | 이동 → WF-I02-북마크-주제-안전 |
| 생활정보 | ON_CLICK | 이동 → WF-I02-북마크-주제-생활정보 |
| 환경 | ON_CLICK | 이동 → WF-I02-북마크-주제-환경 |
| 기타 | ON_CLICK | 이동 → WF-I02-북마크-주제-기타 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCardPhoto / 광운대역 주변 보행로 개선 (947:5114): 이동 → WF-F01-지역안건상세-사진있음.
- PostCard / 월계 주민 걷기 모임 (947:5115): 이동 → WF-F03-지역활동정보상세.

#### [WF-I02-북마크-주제-생활정보](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-5126)

노드 ID: 947:5126. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 북마크 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 | ON_CLICK | 이동 → WF-I02-북마크목록 |
| 교통 | ON_CLICK | 이동 → WF-I02-북마크-주제-교통 |
| 주거 | ON_CLICK | 이동 → WF-I02-북마크-주제-주거 |
| 안전 | ON_CLICK | 이동 → WF-I02-북마크-주제-안전 |
| 복지 | ON_CLICK | 이동 → WF-I02-북마크-주제-복지 |
| 환경 | ON_CLICK | 이동 → WF-I02-북마크-주제-환경 |
| 기타 | ON_CLICK | 이동 → WF-I02-북마크-주제-기타 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCardPhoto / 광운대역 주변 보행로 개선 (947:5157): 이동 → WF-F01-지역안건상세-사진있음.
- PostCard / 월계 주민 걷기 모임 (947:5158): 이동 → WF-F03-지역활동정보상세.

#### [WF-I02-북마크-주제-안전](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-5040)

노드 ID: 947:5040. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 북마크 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 | ON_CLICK | 이동 → WF-I02-북마크목록 |
| 교통 | ON_CLICK | 이동 → WF-I02-북마크-주제-교통 |
| 주거 | ON_CLICK | 이동 → WF-I02-북마크-주제-주거 |
| 복지 | ON_CLICK | 이동 → WF-I02-북마크-주제-복지 |
| 생활정보 | ON_CLICK | 이동 → WF-I02-북마크-주제-생활정보 |
| 환경 | ON_CLICK | 이동 → WF-I02-북마크-주제-환경 |
| 기타 | ON_CLICK | 이동 → WF-I02-북마크-주제-기타 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCardPhoto / 광운대역 주변 보행로 개선 (947:5071): 이동 → WF-F01-지역안건상세-사진있음.
- PostCard / 월계 주민 걷기 모임 (947:5072): 이동 → WF-F03-지역활동정보상세.

#### [WF-I02-북마크-주제-주거](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-4997)

노드 ID: 947:4997. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 북마크 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 | ON_CLICK | 이동 → WF-I02-북마크목록 |
| 교통 | ON_CLICK | 이동 → WF-I02-북마크-주제-교통 |
| 안전 | ON_CLICK | 이동 → WF-I02-북마크-주제-안전 |
| 복지 | ON_CLICK | 이동 → WF-I02-북마크-주제-복지 |
| 생활정보 | ON_CLICK | 이동 → WF-I02-북마크-주제-생활정보 |
| 환경 | ON_CLICK | 이동 → WF-I02-북마크-주제-환경 |
| 기타 | ON_CLICK | 이동 → WF-I02-북마크-주제-기타 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCardPhoto / 광운대역 주변 보행로 개선 (947:5028): 이동 → WF-F01-지역안건상세-사진있음.
- PostCard / 월계 주민 걷기 모임 (947:5029): 이동 → WF-F03-지역활동정보상세.

#### [WF-I02-북마크-주제-환경](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=947-5169)

노드 ID: 947:5169. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 북마크 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 | ON_CLICK | 이동 → WF-I02-북마크목록 |
| 교통 | ON_CLICK | 이동 → WF-I02-북마크-주제-교통 |
| 주거 | ON_CLICK | 이동 → WF-I02-북마크-주제-주거 |
| 안전 | ON_CLICK | 이동 → WF-I02-북마크-주제-안전 |
| 복지 | ON_CLICK | 이동 → WF-I02-북마크-주제-복지 |
| 생활정보 | ON_CLICK | 이동 → WF-I02-북마크-주제-생활정보 |
| 기타 | ON_CLICK | 이동 → WF-I02-북마크-주제-기타 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCardPhoto / 광운대역 주변 보행로 개선 (947:5200): 이동 → WF-F01-지역안건상세-사진있음.
- PostCard / 월계 주민 걷기 모임 (947:5201): 이동 → WF-F03-지역활동정보상세.

#### [WF-I02-북마크목록](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2073)

노드 ID: 692:2073. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 북마크 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 주제 · 전체 · 교통 · 주거 · 안전 · 복지 · 생활정보 · 환경 · 기타 · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 첫 번째 사용자 첨부 사진 · 반응 32 · 댓글 8 · 지역 활동 정보 · 월계1동 · 교통 · 월계 주민 걷기 모임 · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 | ON_CLICK | 이동 → WF-I02-북마크-유형-지역안건 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I02-북마크-유형-지역활동정보 |
| 투표 | ON_CLICK | 이동 → WF-I02-북마크-유형-투표 |
| 교통 | ON_CLICK | 이동 → WF-I02-북마크-주제-교통 |
| 주거 | ON_CLICK | 이동 → WF-I02-북마크-주제-주거 |
| 안전 | ON_CLICK | 이동 → WF-I02-북마크-주제-안전 |
| 복지 | ON_CLICK | 이동 → WF-I02-북마크-주제-복지 |
| 생활정보 | ON_CLICK | 이동 → WF-I02-북마크-주제-생활정보 |
| 환경 | ON_CLICK | 이동 → WF-I02-북마크-주제-환경 |
| 기타 | ON_CLICK | 이동 → WF-I02-북마크-주제-기타 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 주변 보행로 개선 / 첫 번째 사용자 첨부 사진 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| 지역 활동 정보 · 월계1동 · 교통 / 월계 주민 걷기 모임 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F03-지역활동정보상세 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-I03-내가만든게시물](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2098)

노드 ID: 692:2098. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 내가 만든 게시물 · 유형 · 지역 안건 · 지역 활동 정보 · 투표 · 지역 안건 · 월계1동 · 교통 · 익명 작성 · 광운대역 주변 보행로 개선 · 반응 32 · 댓글 8 · 투표 · 월계1동 · 교통 · 진행 중 · 보행 환경 우선순위 · 반응 32 · 댓글 8 · 진행 중 투표 상세 보기 ·   › · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 | ON_CLICK | 이동 → WF-I03-내가만든게시물-지역안건 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I03-내가만든게시물-지역활동정보 |
| 투표 | ON_CLICK | 이동 → WF-I03-내가만든게시물-투표 |
| 지역 안건 · 월계1동 · 교통 · 익명 작성 / 광운대역 주변 보행로 개선 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F24-내게시물상세-지역안건 |
| 투표 · 월계1동 · 교통 · 진행 중 / 보행 환경 우선순위 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F25-내게시물상세-진행중투표 |
| 진행 중 투표 상세 보기 /   › | ON_CLICK | 이동 → WF-F25-내게시물상세-진행중투표 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-I03-내가만든게시물-지역안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=953-5241)

노드 ID: 953:5241. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 내가 만든 게시물 · 유형 · 지역 안건 · 지역 활동 정보 · 투표 · 지역 안건 · 월계1동 · 교통 · 익명 작성 · 광운대역 주변 보행로 개선 · 반응 32 · 댓글 8 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I03-내가만든게시물-지역활동정보 |
| 투표 | ON_CLICK | 이동 → WF-I03-내가만든게시물-투표 |
| 지역 안건 · 월계1동 · 교통 · 익명 작성 / 광운대역 주변 보행로 개선 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F24-내게시물상세-지역안건 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 보행 환경 우선순위 (953:5259): 이동 → WF-F25-내게시물상세-진행중투표.
- Menu / 진행 중 투표 상세 보기 (953:5260): 이동 → WF-F25-내게시물상세-진행중투표.

#### [WF-I03-내가만든게시물-지역활동정보](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=953-5271)

노드 ID: 953:5271. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 내가 만든 게시물 · 유형 · 지역 안건 · 지역 활동 정보 · 투표 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 | ON_CLICK | 이동 → WF-I03-내가만든게시물-지역안건 |
| 투표 | ON_CLICK | 이동 → WF-I03-내가만든게시물-투표 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 광운대역 주변 보행로 개선 (953:5288): 이동 → WF-F24-내게시물상세-지역안건.
- PostCard / 보행 환경 우선순위 (953:5289): 이동 → WF-F25-내게시물상세-진행중투표.
- Menu / 진행 중 투표 상세 보기 (953:5290): 이동 → WF-F25-내게시물상세-진행중투표.

#### [WF-I03-내가만든게시물-투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=953-5301)

노드 ID: 953:5301. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 내가 만든 게시물 · 유형 · 지역 안건 · 지역 활동 정보 · 투표 · 투표 · 월계1동 · 교통 · 진행 중 · 보행 환경 우선순위 · 반응 32 · 댓글 8 · 진행 중 투표 상세 보기 ·   › · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 | ON_CLICK | 이동 → WF-I03-내가만든게시물-지역안건 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I03-내가만든게시물-지역활동정보 |
| 투표 · 월계1동 · 교통 · 진행 중 / 보행 환경 우선순위 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F25-내게시물상세-진행중투표 |
| 진행 중 투표 상세 보기 /   › | ON_CLICK | 이동 → WF-F25-내게시물상세-진행중투표 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 광운대역 주변 보행로 개선 (953:5318): 이동 → WF-F24-내게시물상세-지역안건.

#### [WF-I04-반응한게시물](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2123)

노드 ID: 692:2123. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 반응한 게시물 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 반응 32 · 댓글 8 · 내 현재 참여 · 공감해요 · 필요해요 · 궁금해요 · 댓글 공감 · 댓글 작성 · 투표 · 월계1동 · 교통 · 보행 환경 우선순위 · 반응 32 · 댓글 8 · 내 선택: 조명 개선 · 투표 참여 · 80명 · 남은 2일 · 게시판으로 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역안건 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역활동정보 |
| 투표 | ON_CLICK | 이동 → WF-I04-반응한게시물-투표 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 주변 보행로 개선 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| 공감해요 | ON_CLICK | 이동 → WF-I04-반응한게시물-공감해요 |
| 필요해요 | ON_CLICK | 이동 → WF-I04-반응한게시물-필요해요 |
| 댓글 공감 | ON_CLICK | 이동 → WF-I04-반응한게시물-댓글좋아요싫어요 |
| 댓글 작성 | ON_CLICK | 이동 → WF-I04-반응한게시물-댓글작성 |
| 투표 · 월계1동 · 교통 / 보행 환경 우선순위 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F04-투표상세-진행중 |
| 게시판으로 | ON_CLICK | 이동 → WF-C01-통합게시판 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-I04-반응한게시물-공감해요](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5607)

노드 ID: 959:5607. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 참여한 게시물 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 내 현재 참여 · 공감해요 · 필요해요 · 댓글 작성 · 댓글 좋아요·싫어요 · 게시판으로 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역안건 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역활동정보 |
| 투표 | ON_CLICK | 이동 → WF-I04-반응한게시물-투표 |
| 필요해요 | ON_CLICK | 이동 → WF-I04-반응한게시물-필요해요 |
| 댓글 작성 | ON_CLICK | 이동 → WF-I04-반응한게시물-댓글작성 |
| 댓글 좋아요·싫어요 | ON_CLICK | 이동 → WF-I04-반응한게시물-댓글좋아요싫어요 |
| 게시판으로 | ON_CLICK | 이동 → WF-C01-통합게시판 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 광운대역 주변 보행로 개선 (959:5626): 이동 → WF-F01-지역안건상세-사진있음.
- PostCard / 보행 환경 우선순위 (959:5634): 이동 → WF-F04-투표상세-진행중.

#### [WF-I04-반응한게시물-댓글작성](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5685)

노드 ID: 959:5685. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 참여한 게시물 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 내 현재 참여 · 공감해요 · 필요해요 · 댓글 작성 · 댓글 좋아요·싫어요 · 게시판으로 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역안건 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역활동정보 |
| 투표 | ON_CLICK | 이동 → WF-I04-반응한게시물-투표 |
| 공감해요 | ON_CLICK | 이동 → WF-I04-반응한게시물-공감해요 |
| 필요해요 | ON_CLICK | 이동 → WF-I04-반응한게시물-필요해요 |
| 댓글 좋아요·싫어요 | ON_CLICK | 이동 → WF-I04-반응한게시물-댓글좋아요싫어요 |
| 게시판으로 | ON_CLICK | 이동 → WF-C01-통합게시판 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 광운대역 주변 보행로 개선 (959:5704): 이동 → WF-F01-지역안건상세-사진있음.
- PostCard / 보행 환경 우선순위 (959:5712): 이동 → WF-F04-투표상세-진행중.

#### [WF-I04-반응한게시물-댓글좋아요싫어요](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5724)

노드 ID: 959:5724. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 참여한 게시물 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 내 현재 참여 · 공감해요 · 필요해요 · 댓글 작성 · 댓글 좋아요·싫어요 · 게시판으로 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역안건 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역활동정보 |
| 투표 | ON_CLICK | 이동 → WF-I04-반응한게시물-투표 |
| 공감해요 | ON_CLICK | 이동 → WF-I04-반응한게시물-공감해요 |
| 필요해요 | ON_CLICK | 이동 → WF-I04-반응한게시물-필요해요 |
| 댓글 작성 | ON_CLICK | 이동 → WF-I04-반응한게시물-댓글작성 |
| 게시판으로 | ON_CLICK | 이동 → WF-C01-통합게시판 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 광운대역 주변 보행로 개선 (959:5743): 이동 → WF-F01-지역안건상세-사진있음.
- PostCard / 보행 환경 우선순위 (959:5751): 이동 → WF-F04-투표상세-진행중.

#### [WF-I04-반응한게시물-지역안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5490)

노드 ID: 959:5490. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 참여한 게시물 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 반응 32 · 댓글 8 · 내 현재 참여 · 공감해요 · 필요해요 · 댓글 작성 · 댓글 좋아요·싫어요 · 게시판으로 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 | ON_CLICK | 이동 → WF-I04-반응한게시물 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역활동정보 |
| 투표 | ON_CLICK | 이동 → WF-I04-반응한게시물-투표 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 주변 보행로 개선 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F01-지역안건상세-사진있음 |
| 게시판으로 | ON_CLICK | 이동 → WF-C01-통합게시판 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 보행 환경 우선순위 (959:5517): 이동 → WF-F04-투표상세-진행중.

#### [WF-I04-반응한게시물-지역활동정보](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5529)

노드 ID: 959:5529. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 참여한 게시물 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 내 현재 참여 · 공감해요 · 필요해요 · 댓글 작성 · 댓글 좋아요·싫어요 · 게시판으로 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 | ON_CLICK | 이동 → WF-I04-반응한게시물 |
| 지역 안건 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역안건 |
| 투표 | ON_CLICK | 이동 → WF-I04-반응한게시물-투표 |
| 게시판으로 | ON_CLICK | 이동 → WF-C01-통합게시판 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 광운대역 주변 보행로 개선 (959:5548): 이동 → WF-F01-지역안건상세-사진있음.
- PostCard / 보행 환경 우선순위 (959:5556): 이동 → WF-F04-투표상세-진행중.

#### [WF-I04-반응한게시물-투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5568)

노드 ID: 959:5568. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 참여한 게시물 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 내 현재 참여 · 공감해요 · 필요해요 · 댓글 작성 · 댓글 좋아요·싫어요 · 투표 · 월계1동 · 교통 · 보행 환경 우선순위 · 반응 32 · 댓글 8 · 내 선택: 조명 개선 · 투표 참여 · 80명 · 남은 2일 · 게시판으로 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 | ON_CLICK | 이동 → WF-I04-반응한게시물 |
| 지역 안건 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역안건 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역활동정보 |
| 투표 · 월계1동 · 교통 / 보행 환경 우선순위 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F04-투표상세-진행중 |
| 게시판으로 | ON_CLICK | 이동 → WF-C01-통합게시판 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 광운대역 주변 보행로 개선 (959:5587): 이동 → WF-F01-지역안건상세-사진있음.

#### [WF-I04-반응한게시물-필요해요](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=959-5646)

노드 ID: 959:5646. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 참여한 게시물 · 유형 · 전체 · 지역 안건 · 지역 활동 정보 · 투표 · 내 현재 참여 · 공감해요 · 필요해요 · 댓글 작성 · 댓글 좋아요·싫어요 · 게시판으로 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 안건 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역안건 |
| 지역 활동 정보 | ON_CLICK | 이동 → WF-I04-반응한게시물-지역활동정보 |
| 투표 | ON_CLICK | 이동 → WF-I04-반응한게시물-투표 |
| 공감해요 | ON_CLICK | 이동 → WF-I04-반응한게시물-공감해요 |
| 댓글 작성 | ON_CLICK | 이동 → WF-I04-반응한게시물-댓글작성 |
| 댓글 좋아요·싫어요 | ON_CLICK | 이동 → WF-I04-반응한게시물-댓글좋아요싫어요 |
| 게시판으로 | ON_CLICK | 이동 → WF-C01-통합게시판 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 광운대역 주변 보행로 개선 (959:5665): 이동 → WF-F01-지역안건상세-사진있음.
- PostCard / 보행 환경 우선순위 (959:5673): 이동 → WF-F04-투표상세-진행중.

#### [WF-I05-참여한투표](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2148)

노드 ID: 692:2148. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 참여한 투표 · 진행 상태 · 전체 · 진행 중 · 종료 · 투표 · 교통 · 진행 중 · 80명 · 보행 환경 우선순위 · 반응 32 · 댓글 8 · 내 선택: 조명 개선 35% | 최다: 보행로 정비 65% · 남은 2일 · 종료 전 알림 설정됨 · 투표 · 교통 · 종료 · 종료된 투표 기록 · 반응 32 · 댓글 8 · 접근할 수 없는 투표 · 개인 참여 기록 유지 · 접근 불가 안내 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 진행 중 | ON_CLICK | 이동 → WF-I05-참여한투표-진행중 |
| 종료 | ON_CLICK | 이동 → WF-I05-참여한투표-종료 |
| 투표 · 교통 · 진행 중 · 80명 / 보행 환경 우선순위 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F04-투표상세-진행중 |
| 투표 · 교통 · 종료 / 종료된 투표 기록 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F09-투표-종료결과 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-I05-참여한투표-종료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=961-5978)

노드 ID: 961:5978. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 참여한 투표 · 진행 상태 · 전체 · 진행 중 · 종료 · 투표 · 교통 · 종료 · 종료된 투표 기록 · 반응 32 · 댓글 8 · 내 선택: 조명 개선 · 최종 결과: 보행로 정비 · 접근할 수 없는 투표 · 개인 참여 기록 유지 · 접근 불가 안내 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 | ON_CLICK | 이동 → WF-I05-참여한투표 |
| 진행 중 | ON_CLICK | 이동 → WF-I05-참여한투표-진행중 |
| 투표 · 교통 · 종료 / 종료된 투표 기록 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F09-투표-종료결과 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 보행 환경 우선순위 (961:5995): 이동 → WF-F04-투표상세-진행중.

#### [WF-I05-참여한투표-진행중](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=961-5948)

노드 ID: 961:5948. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 참여한 투표 · 진행 상태 · 전체 · 진행 중 · 종료 · 투표 · 교통 · 진행 중 · 80명 · 보행 환경 우선순위 · 반응 32 · 댓글 8 · 내 선택: 조명 개선 35% | 최다: 보행로 정비 65% · 남은 2일 · 종료 전 알림 설정됨 · 접근 불가 안내 · 메인 · 지도 · 글쓰기 · 알림 · 마이

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 | ON_CLICK | 이동 → WF-I05-참여한투표 |
| 종료 | ON_CLICK | 이동 → WF-I05-참여한투표-종료 |
| 투표 · 교통 · 진행 중 · 80명 / 보행 환경 우선순위 / 반응 32 · 댓글 8 | ON_CLICK | 이동 → WF-F04-투표상세-진행중 |
| 메인 | ON_CLICK | 이동 → WF-B01-메인 |
| 지도 | ON_CLICK | 이동 → WF-D01-전체지도 |
| 글쓰기 | ON_CLICK | 이동 → WF-E01-글쓰기-유형선택 |
| 알림 | ON_CLICK | 이동 → WF-H01-알림및활동 |
| 마이 | ON_CLICK | 이동 → WF-I01-마이페이지 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 종료된 투표 기록 (961:5968): 이동 → WF-F09-투표-종료결과.

#### [WF-I06-프로필수정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2173)

노드 ID: 692:2173. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 프로필 수정 · 프로필 사진 (선택) · 닉네임 · 동네이웃 · 최대 10자 · 중복 불가 · 한 줄 소개 · 우리 동네 이야기에 관심이 많아요. · 최대 50자 · 이웃 속성 · 복수 선택 · ☑ 거주자 · ☑ 학생 · □ 직장인 · □ 상인 · 이웃 인증 ·   › · 저장 · 취소

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 이웃 인증 /   › | ON_CLICK | 이동 → WF-J01-이웃인증-제출 |
| 저장 | ON_CLICK | 이동 → WF-I01-마이페이지 |
| 취소 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-I07-기본활동지역](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2198)

노드 ID: 692:2198. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 기본 활동 지역 · 현재 기본 활동 지역 · 월계1동 · 지역명 검색 · 월계 · 현재 위치로 찾기 · 월계1동 · 선택됨  › · 월계2동 ·   › · 월계3동 ·   › · 이 지역으로 설정하기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 현재 기본 활동 지역 · 월계1동 / 지역명 검색 / 월계 / 현재 위치로 찾기 / 월계1동 / 선택됨  › / 월계2동 /   › / 월계3동 /   › / 이 지역으로 설정하기 | ON_CLICK | 이동 → WF-I07-기본활동지역-지역선택 |
| 현재 위치로 찾기 | ON_CLICK | 이동 → WF-I07-기본활동지역-지역선택 |
| 이 지역으로 설정하기 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-I07-기본활동지역-지역선택](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1034-6155)

노드 ID: 1034:6155. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 기본 활동 지역 · 현재 기본 활동 지역 · 월계1동 · 지역명 검색 · 월계 · 현재 위치로 찾기 · 월계1동 · 선택됨  › · 월계2동 · 선택됨  › · 월계3동 ·   › · 이 지역으로 설정하기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 이 지역으로 설정하기 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-I08-관심지역](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2223)

노드 ID: 692:2223. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 관심 지역 · 추가·삭제는 즉시 반영됩니다. 0개도 가능합니다. · 지역 검색 · 관심 지역 찾기 · 월계1동 · 제거  › · 월계2동 · 추가  › · 하계1동 · 추가  › · 돌아가기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 추가·삭제는 즉시 반영됩니다. 0개도 가능합니다. / 지역 검색 / 관심 지역 찾기 / 월계1동 / 제거  › / 월계2동 / 추가  › / 하계1동 / 추가  › / 돌아가기 | ON_CLICK | 이동 → WF-I08-관심지역-추가됨 |
| 돌아가기 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-I08-관심지역-제거됨](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1034-6225)

노드 ID: 1034:6225. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 관심 지역 · 추가·삭제는 즉시 반영됩니다. 0개도 가능합니다. · 지역 검색 · 관심 지역 찾기 · 월계1동 · 추가  › · 월계2동 · 추가  › · 하계1동 · 추가  › · 돌아가기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 추가·삭제는 즉시 반영됩니다. 0개도 가능합니다. / 지역 검색 / 관심 지역 찾기 / 월계1동 / 추가  › / 월계2동 / 추가  › / 하계1동 / 추가  › / 돌아가기 | ON_CLICK | 이동 → WF-I08-관심지역-추가됨 |
| 돌아가기 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-I08-관심지역-추가됨](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=1034-6206)

노드 ID: 1034:6206. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 관심 지역 · 추가·삭제는 즉시 반영됩니다. 0개도 가능합니다. · 지역 검색 · 관심 지역 찾기 · 월계1동 · 제거  › · 월계2동 · 제거  › · 하계1동 · 추가  › · 돌아가기

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 추가·삭제는 즉시 반영됩니다. 0개도 가능합니다. / 지역 검색 / 관심 지역 찾기 / 월계1동 / 제거  › / 월계2동 / 제거  › / 하계1동 / 추가  › / 돌아가기 | ON_CLICK | 이동 → WF-I08-관심지역-제거됨 |
| 돌아가기 | ON_CLICK | 이동 → WF-I01-마이페이지 |

### J 화면별 연결

#### [WF-J01-이웃인증-제출](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2248)

노드 ID: 692:2248. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 이웃 인증 · 이 지역에 거주하는 이웃의 참여 자격을 확인합니다. · 인증할 거주 지역 · 월계1동  › · 거주 증빙 자료 첨부 · 거주 증빙 자료 · 첨부한 파일 · 제거 가능 · 제출

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 제출 | ON_CLICK | 이동 → WF-J02-이웃인증-접수 |

#### [WF-J02-이웃인증-접수](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2273)

노드 ID: 692:2273. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 이웃 인증 접수 · 인증 자료가 접수되었습니다. · 제출한 지역 · 월계1동 · 최종 제품에서는 제출 후 1주일 이내 승인 여부를 안내합니다. 접수만으로 인증이 완료되지는 않습니다. · 프로필로

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 프로필로 | ON_CLICK | 이동 → WF-I06-프로필수정 |

#### [WF-J03-기관인증-정보](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2298)

노드 ID: 692:2298. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 기관 인증 · 실제 민원 업무 담당자를 위한 인증입니다. · 기관명 * · 노원구청 · 부서명 * · 부서명 · 직책명 * · 직책명 · 이름 * · 이름 · 업무 이메일 * · 업무용 이메일 · 전화번호 * · 연락 가능한 번호 · 담당 지역 * · 담당 지역 · 재직증명서 첨부

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 재직증명서 첨부 | ON_CLICK | 이동 → WF-J04-기관인증-증빙 |

#### [WF-J04-기관인증-증빙](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2323)

노드 ID: 692:2323. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 기관 인증 자료 · 유효 증빙 · 재직증명서 · 파일 업로드 · 재직증명서.pdf · 2.4 MB · 제거 · PDF·JPG·PNG | 개수 제한 없음 / 파일당 10MB · 요청 합계 50MB · 이름과 기관명이 일치하는지, 불필요한 개인정보가 포함되어 있지 않은지 확인해 주세요. · 자료 제출

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 자료 제출 | ON_CLICK | 이동 → WF-J05-기관인증-접수 |

#### [WF-J05-기관인증-접수](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2348)

노드 ID: 692:2348. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 기관 인증 접수 · 기관 인증 자료가 접수되었습니다. · 자료 제출과 인증 완료는 다릅니다. 최종 제품에서는 운영자가 1주일 이내 실제 민원 담당 자격을 검토합니다. · 마이페이지로

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 마이페이지로 | ON_CLICK | 이동 → WF-I01-마이페이지 |

### K 화면별 연결

#### [WF-K01-담당자-월계1동](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=938-4793)

노드 ID: 938:4793. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: 월계1동  › · ‹ · 기관 담당자 · 유효 기관 인증 · 담당 지역 월계1동 · 지역 필터 · 월계1동  › · 목록 · 전체 공개 안건 · 채택한 게시물 · 반응 수 내림차순 · 결과 1개 · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 반응 32 · 의견 8

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 채택한 게시물 | ON_CLICK | 이동 → WF-K01-담당자-채택게시물 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 주변 보행로 개선 / 반응 32 · 의견 8 | ON_CLICK | 이동 → WF-K02-담당지역-채택상세 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 하계동 보행 환경 개선 (938:4813): 이동 → WF-K03-타지역-열람전용.

#### [WF-K01-담당자-전체안건](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2373)

노드 ID: 692:2373. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 기관 담당자 · 유효 기관 인증 · 담당 지역 월계1동 · 지역 필터 · 전체 지역  › · 목록 · 전체 공개 안건 · 채택한 게시물 · 반응 수 내림차순 · 결과 2개 · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 반응 32 · 의견 8 · 지역 안건 · 하계1동 · 교통 · 하계동 보행 환경 개선 · 반응 21 · 의견 5

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 필터 / 전체 지역  › | ON_CLICK | 이동 → WF-K01-담당자-월계1동 |
| 채택한 게시물 | ON_CLICK | 이동 → WF-K01-담당자-채택게시물 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 주변 보행로 개선 / 반응 32 · 의견 8 | ON_CLICK | 이동 → WF-K02-담당지역-채택상세 |
| 지역 안건 · 하계1동 · 교통 / 하계동 보행 환경 개선 / 반응 21 · 의견 5 | ON_CLICK | 이동 → WF-K03-타지역-열람전용 |

#### [WF-K01-담당자-채택게시물](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=938-4771)

노드 ID: 938:4771. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 기관 담당자 · 유효 기관 인증 · 담당 지역 월계1동 · 지역 필터 · 전체 지역  › · 목록 · 전체 공개 안건 · 채택한 게시물 · 반응 수 내림차순 · 결과 1개 · 지역 안건 · 월계1동 · 교통 · 광운대역 주변 보행로 개선 · 반응 32 · 의견 8

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 지역 필터 / 전체 지역  › | ON_CLICK | 이동 → WF-K01-담당자-월계1동 |
| 전체 공개 안건 | ON_CLICK | 이동 → WF-K01-담당자-전체안건 |
| 지역 안건 · 월계1동 · 교통 / 광운대역 주변 보행로 개선 / 반응 32 · 의견 8 | ON_CLICK | 이동 → WF-K02-담당지역-채택상세 |

현재 숨김 요소에 남아 있는 연결이며 사용자에게 보이는 UI로 취급하지 않음:

- PostCard / 하계동 보행 환경 개선 (938:4791): 이동 → WF-K03-타지역-열람전용.

#### [WF-K02-담당지역-채택상세](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2398)

노드 ID: 692:2398. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 해제.

화면에 포함된 텍스트: ‹ · 지역 안건 검토 · 지역 안건 · 교통 · 월계1동 · 동네이웃 · 10월 5일  › · 광운대역 주변 보행로 개선 · 출퇴근 시간에 사람이 많이 모이는 구간을 살펴보고 보행로를 개선하면 좋겠습니다. 우리 동네에서 필요한 부분을 함께 이야기해 주세요. · 참고 자료 · 출처 링크 · AI 요약 · 주민이 광운대역 주변 보행로 개선을 제안했습니다. 출퇴근 시간에 보행자가 몰리는 구간을 먼저 살펴보자는 내용입니다. 게시물에서 주민 의견을 함께 확인할 수 있습니다. · 주민 참여 · 공감해요 20 · 필요해요 10 · 궁금해요 2 / 댓글·답글 8 · 주민 의견 · 동네이웃 · 10월 5일 · 출퇴근 시간에 보행자가 몰려 위험합니다. 보행로 폭을 넓혀 주세요. · ↳ 답글 · 조명도 함께 개선되면 좋겠습니다. · 이 안건 채택 · 기관명과 채택 시각만 공개됩니다.

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 이 안건 채택 | ON_CLICK | 이동 → WF-K04-지역안건-채택기록 |

#### [WF-K03-타지역-열람전용](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2423)

노드 ID: 692:2423. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 안건 검토 · 지역 안건 · 교통 · 하계1동 · 하계동 보행 환경 개선 · 다른 지역의 공개 안건과 주민 의견도 열람할 수 있습니다. · 반응 21 · 의견 5 · 주민 의견 · 하계1동 주민 · 10월 4일 · 야간에는 조명이 어두워 보행이 불편합니다. 보행 환경 개선이 필요합니다. · ↳ 답글 · 유모차와 휠체어 이동도 고려해 주세요. · 채택 불가 · 담당 지역 밖 · 전체 안건 목록

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 전체 안건 목록 | ON_CLICK | 이동 → WF-K01-담당자-전체안건 |

#### [WF-K04-지역안건-채택기록](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2448)

노드 ID: 692:2448. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 지역 안건 · 채택 기록 · 광운대역 주변 보행로 개선 · 채택이 기록되었습니다. · 노원구청 · 2026.10.05 14:00 · 다른 기관 · 2026.10.05 13:00 · 본 기관 채택 취소 · 담당자 목록으로

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 본 기관 채택 취소 | ON_CLICK | 이동 → WF-K02-담당지역-채택상세 |
| 담당자 목록으로 | ON_CLICK | 이동 → WF-K01-담당자-전체안건 |

### L 화면별 연결

#### [WF-L01-설정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2473)

노드 ID: 692:2473. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 설정 · 계정 · 계정 관리 ·   › · 커뮤니티 · 내가 쓴 글 ·   › · 댓글 남긴 글 ·   › · 스크랩한 글 ·   › · 관심 키워드 ·   › · 앱 설정 · 알림 설정 ·   › · 기타 · 로그아웃 ·   › · 회원 탈퇴 ·   ›

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 계정 관리 /   › | ON_CLICK | 이동 → WF-L02-계정관리 |
| 내가 쓴 글 /   › | ON_CLICK | 이동 → WF-I03-내가만든게시물 |
| 댓글 남긴 글 /   › | ON_CLICK | 이동 → WF-I04-반응한게시물 |
| 스크랩한 글 /   › | ON_CLICK | 이동 → WF-I02-북마크목록 |
| 관심 키워드 /   › | ON_CLICK | 이동 → WF-L07-관심키워드 |
| 알림 설정 /   › | ON_CLICK | 이동 → WF-L08-알림설정 |
| 로그아웃 /   › | ON_CLICK | 이동 → WF-A01-시작 |
| 회원 탈퇴 /   › | ON_CLICK | 이동 → WF-L09-탈퇴-주의사항 |

#### [WF-L02-계정관리](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2498)

노드 ID: 692:2498. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 계정 관리 · 등록 이메일 · neighbor@example.com · 이메일 변경 ·   › · 비밀번호 변경 ·   ›

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L01-설정 |
| 이메일 변경 /   › | ON_CLICK | 이동 → WF-L03-이메일변경 |
| 비밀번호 변경 /   › | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증 |

#### [WF-L02-계정관리-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-5655)

노드 ID: 958:5655. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 계정 관리 · 등록 이메일 · neighbor@example.com · 이메일 변경 ·   › · 비밀번호 변경 ·   ›

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-I01-마이페이지 |
| 이메일 변경 /   › | ON_CLICK | 이동 → WF-L03-이메일변경-마이페이지진입 |
| 비밀번호 변경 /   › | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증-마이페이지진입 |

#### [WF-L02-계정관리-이메일변경완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4553)

노드 ID: 897:4553. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 계정 관리 · 등록 이메일 · new@example.com · 이메일 변경 ·   › · 비밀번호 변경 ·   ›

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L01-설정 |
| 이메일 변경 /   › | ON_CLICK | 이동 → WF-L03-이메일변경 |
| 비밀번호 변경 /   › | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증 |

#### [WF-L02-계정관리-이메일변경완료-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-5744)

노드 ID: 958:5744. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 계정 관리 · 등록 이메일 · new@example.com · 이메일 변경 ·   › · 비밀번호 변경 ·   ›

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-I01-마이페이지 |
| 이메일 변경 /   › | ON_CLICK | 이동 → WF-L03-이메일변경-마이페이지진입 |
| 비밀번호 변경 /   › | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증-마이페이지진입 |

#### [WF-L03-이메일변경](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2523)

노드 ID: 692:2523. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 이메일 변경 · 현재 이메일 · neighbor@example.com · 현재 비밀번호 · •••••••• · 새 이메일 · 새 이메일 주소 · 인증번호 발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L02-계정관리 |
| 인증번호 발송 | ON_CLICK | 이동 → WF-L04-이메일변경-인증 |

#### [WF-L03-이메일변경-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-5671)

노드 ID: 958:5671. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 이메일 변경 · 현재 이메일 · neighbor@example.com · 현재 비밀번호 · •••••••• · 새 이메일 · 새 이메일 주소 · 인증번호 발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L02-계정관리-마이페이지진입 |
| 인증번호 발송 | ON_CLICK | 이동 → WF-L04-이메일변경-인증-마이페이지진입 |

#### [WF-L03-이메일변경-발송실패](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4569)

노드 ID: 897:4569. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 이메일 변경 · 현재 이메일 · neighbor@example.com · 현재 비밀번호 · •••••••• · 새 이메일 · 새 이메일 주소 · 인증번호 발송 · 인증번호 발송에 실패했습니다. 다시 시도해 주세요.

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L03-이메일변경 |

#### [WF-L03-이메일변경-이미사용중인이메일](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=955-5540)

노드 ID: 955:5540. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 이메일 변경 · 현재 이메일 · neighbor@example.com · 현재 비밀번호 · •••••••• · 새 이메일 · 새 이메일 주소 · 인증번호 발송 · 이미 사용 중인 이메일입니다. 다른 이메일을 입력해 주세요.

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L03-이메일변경 |

#### [WF-L03-이메일변경-현재비밀번호불일치](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=955-5497)

노드 ID: 955:5497. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 이메일 변경 · 현재 이메일 · neighbor@example.com · 현재 비밀번호 · •••••••• · 새 이메일 · 새 이메일 주소 · 인증번호 발송 · 현재 비밀번호가 일치하지 않습니다. 다시 확인해 주세요.

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L03-이메일변경 |

#### [WF-L04-이메일변경-인증](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2548)

노드 ID: 692:2548. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 새 이메일 인증 · 인증번호를 발송했습니다. · 새 이메일 · new@example.com · 6자리 인증번호 · 123456 · 04:59 · 재발송 대기 60초 · 인증 후 이메일 변경 · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L03-이메일변경 |
| 인증 후 이메일 변경 | ON_CLICK | 이동 → WF-L02-계정관리-이메일변경완료 |
| 재발송 | ON_CLICK | 이동 → WF-L04-이메일변경-인증-재발송 |

#### [WF-L04-이메일변경-인증-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-5688)

노드 ID: 958:5688. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 새 이메일 인증 · 인증번호를 발송했습니다. · 새 이메일 · new@example.com · 6자리 인증번호 · 123456 · 04:59 · 재발송 대기 60초 · 인증 후 이메일 변경 · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L03-이메일변경-마이페이지진입 |
| 인증 후 이메일 변경 | ON_CLICK | 이동 → WF-L02-계정관리-이메일변경완료-마이페이지진입 |
| 재발송 | ON_CLICK | 이동 → WF-L04-이메일변경-인증-재발송-마이페이지진입 |

#### [WF-L04-이메일변경-인증-만료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4605)

노드 ID: 897:4605. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 새 이메일 인증 · 인증번호가 만료되었습니다. 다시 발급받아 주세요. · 새 이메일 · new@example.com · 6자리 인증번호 · 123456 · 인증번호가 만료되었습니다. 재발송해 주세요. · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L04-이메일변경-인증 |

#### [WF-L04-이메일변경-인증-불일치](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4586)

노드 ID: 897:4586. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 새 이메일 인증 · 인증번호가 일치하지 않습니다. · 새 이메일 · new@example.com · 6자리 인증번호 · 123456 · 04:59 · 재발송 대기 60초 · 인증 후 이메일 변경 · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L04-이메일변경-인증 |

#### [WF-L04-이메일변경-인증-재발송](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4624)

노드 ID: 897:4624. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 새 이메일 인증 · 인증번호가 재발송되었습니다. · 새 이메일 · new@example.com · 6자리 인증번호 · 6자리 입력 · 04:59 · 재발송 대기 60초 · 인증 후 이메일 변경 · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L03-이메일변경 |
| 인증 후 이메일 변경 | ON_CLICK | 이동 → WF-L02-계정관리-이메일변경완료 |

#### [WF-L04-이메일변경-인증-재발송-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-6234)

노드 ID: 958:6234. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 새 이메일 인증 · 인증번호가 재발송되었습니다. · 새 이메일 · new@example.com · 6자리 인증번호 · 6자리 입력 · 04:59 · 재발송 대기 60초 · 인증 후 이메일 변경 · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L03-이메일변경-마이페이지진입 |
| 인증 후 이메일 변경 | ON_CLICK | 이동 → WF-L02-계정관리-이메일변경완료-마이페이지진입 |

#### [WF-L05-비밀번호변경-발송실패](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4662)

노드 ID: 897:4662. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 비밀번호 변경 · 본인 확인 · 등록 이메일 · neighbor@example.com · 인증번호 발송 · 인증번호 발송에 실패했습니다. 다시 시도해 주세요.

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증 |

#### [WF-L05-비밀번호변경-인증](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2573)

노드 ID: 692:2573. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 비밀번호 변경 · 본인 확인 · 등록 이메일 · neighbor@example.com · 인증번호 발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L02-계정관리 |
| 인증번호 발송 | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증-발송완료 |

#### [WF-L05-비밀번호변경-인증-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-5707)

노드 ID: 958:5707. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 비밀번호 변경 · 본인 확인 · 등록 이메일 · neighbor@example.com · 인증번호 발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L02-계정관리-마이페이지진입 |
| 인증번호 발송 | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증-발송완료-마이페이지진입 |

#### [WF-L05-비밀번호변경-인증-만료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4700)

노드 ID: 897:4700. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 비밀번호 변경 · 본인 확인 · 등록 이메일 · neighbor@example.com · 인증번호 발송 · 6자리 인증번호 · 123456 · 인증번호가 만료되었습니다. 다시 발급받아 주세요. · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증-발송완료 |

#### [WF-L05-비밀번호변경-인증-발송완료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4643)

노드 ID: 897:4643. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 비밀번호 변경 · 본인 확인 · 등록 이메일 · neighbor@example.com · 인증번호 발송 · 6자리 인증번호 · 123456 · 인증번호를 발송했습니다. 04:59 · 재발송 대기 60초 · 확인 · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L02-계정관리 |
| 확인 | ON_CLICK | 이동 → WF-L06-비밀번호변경 |
| 재발송 | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증-재발송 |

#### [WF-L05-비밀번호변경-인증-발송완료-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-6282)

노드 ID: 958:6282. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 비밀번호 변경 · 본인 확인 · 등록 이메일 · neighbor@example.com · 인증번호 발송 · 6자리 인증번호 · 123456 · 인증번호를 발송했습니다. 04:59 · 재발송 대기 60초 · 확인 · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증-마이페이지진입 |
| 확인 | ON_CLICK | 이동 → WF-L06-비밀번호변경-마이페이지진입 |
| 재발송 | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증-재발송-마이페이지진입 |

#### [WF-L05-비밀번호변경-인증-불일치](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4681)

노드 ID: 897:4681. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 비밀번호 변경 · 본인 확인 · 등록 이메일 · neighbor@example.com · 인증번호 발송 · 6자리 인증번호 · 123456 · 인증번호가 일치하지 않습니다. · 확인 · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증-발송완료 |

#### [WF-L05-비밀번호변경-인증-재발송](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4719)

노드 ID: 897:4719. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 비밀번호 변경 · 본인 확인 · 등록 이메일 · neighbor@example.com · 인증번호 발송 · 6자리 인증번호 · 6자리 입력 · 인증번호가 재발송되었습니다. 04:59 · 재발송 대기 60초 · 확인 · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L02-계정관리 |
| 확인 | ON_CLICK | 이동 → WF-L06-비밀번호변경 |

#### [WF-L05-비밀번호변경-인증-재발송-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=978-6023)

노드 ID: 978:6023. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 비밀번호 변경 · 본인 확인 · 등록 이메일 · neighbor@example.com · 인증번호 발송 · 6자리 인증번호 · 6자리 입력 · 인증번호가 재발송되었습니다. 04:59 · 재발송 대기 60초 · 확인 · 재발송

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증-마이페이지진입 |
| 확인 | ON_CLICK | 이동 → WF-L06-비밀번호변경-마이페이지진입 |

#### [WF-L06-비밀번호변경](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2598)

노드 ID: 692:2598. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 새 비밀번호 설정 · 등록 이메일 인증 완료 · 새 비밀번호 · 8~64자 · 새 비밀번호 확인 · 다시 입력 · 영문과 숫자를 포함한 8자 이상으로 입력해 주세요. · 비밀번호 변경

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증 |
| 비밀번호 변경 | ON_CLICK | 이동 → WF-L01-설정 |

#### [WF-L06-비밀번호변경-마이페이지진입](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=958-5726)

노드 ID: 958:5726. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 새 비밀번호 설정 · 등록 이메일 인증 완료 · 새 비밀번호 · 8~64자 · 새 비밀번호 확인 · 다시 입력 · 영문과 숫자를 포함한 8자 이상으로 입력해 주세요. · 비밀번호 변경

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L05-비밀번호변경-인증-마이페이지진입 |
| 비밀번호 변경 | ON_CLICK | 이동 → WF-I01-마이페이지 |

#### [WF-L06-비밀번호변경-형식오류](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=955-5583)

노드 ID: 955:5583. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 새 비밀번호 설정 · 등록 이메일 인증 완료 · 새 비밀번호 · 8~64자 · 새 비밀번호 확인 · 다시 입력 · 영문과 숫자를 포함한 8자 이상으로 입력해 주세요. · 비밀번호 변경

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L06-비밀번호변경 |

#### [WF-L06-비밀번호변경-확인불일치](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=955-5626)

노드 ID: 955:5626. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 새 비밀번호 설정 · 등록 이메일 인증 완료 · 새 비밀번호 · 8~64자 · 새 비밀번호 확인 · 다시 입력 · 새 비밀번호와 확인값이 일치하지 않습니다. · 비밀번호 변경

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L06-비밀번호변경 |

#### [WF-L07-관심키워드](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2623)

노드 ID: 692:2623. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 관심 키워드 · 추천에 사용할 키워드를 최대 4개 선택해 주세요. · 선택 2/4 · ☑ 교통 · ☑ 주거 · □ 안전 · □ 복지 · □ 생활정보 · □ 환경 · □ 기타 · 저장 · 선택한 키워드는 추천에만 사용됩니다.

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 저장 | ON_CLICK | 이동 → WF-L01-설정 |

#### [WF-L07-관심키워드-4개선택](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=955-5709)

노드 ID: 955:5709. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 관심 키워드 · 최대 4개까지 선택할 수 있습니다. · 선택 4/4 · ☑ 교통 · ☑ 주거 · ☑ 안전 · ☑ 복지 · □ 생활정보 · □ 환경 · □ 기타 · 저장 · 선택한 키워드는 추천에만 사용됩니다.

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L07-관심키워드 |

#### [WF-L07-관심키워드-저장실패](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=897-4738)

노드 ID: 897:4738. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 관심 키워드 · 추천에 사용할 키워드를 최대 4개 선택해 주세요. · 선택 2/4 · ☑ 교통 · ☑ 주거 · □ 안전 · □ 복지 · □ 생활정보 · □ 환경 · □ 기타 · 다시 시도 · 저장에 실패했습니다. 기존 선택을 유지했습니다. 다시 시도해 주세요.

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L07-관심키워드 |

#### [WF-L08-알림설정](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2648)

노드 ID: 692:2648. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 알림 설정 · 푸시 알림 수신 · ON · OFF여도 서비스 내 알림과 활동 기록은 유지됩니다. · ON에서는 푸시를 1회 시도하며 실패하더라도 자동으로 다시 보내지 않습니다.

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| ON | ON_CLICK | 컴포넌트 상태 변경 → State=OFF |

#### [WF-L09-탈퇴-주의사항](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2673)

노드 ID: 692:2673. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 회원 탈퇴 · 탈퇴 전 확인해 주세요 · 게시물·댓글·답글은 유지되며 작성자는 ‘회원 탈퇴한 사용자’로 표시됩니다. 기존 익명 게시물은 익명 표시가 유지됩니다. · 북마크와 개인 설정은 삭제됩니다. 지역 활동 게시물의 문의 이메일은 제거됩니다. 반응·평가·투표 집계는 유지되며 회원 식별 연결은 제거됩니다. · 현재 비밀번호 확인 · 취소

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 현재 비밀번호 확인 | ON_CLICK | 이동 → WF-L10-탈퇴-본인확인 |
| 취소 | ON_CLICK | 이동 → WF-L01-설정 |

#### [WF-L10-탈퇴-본인확인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2698)

노드 ID: 692:2698. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 본인 확인 · 현재 비밀번호 · •••••••• · 다음 · 취소 · 비밀번호 확인 실패 시 탈퇴하지 않습니다.

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 다음 | ON_CLICK | 이동 → WF-L11-탈퇴-최종확인 |
| 취소 | ON_CLICK | 이동 → WF-L01-설정 |

#### [WF-L10-탈퇴-본인확인-비밀번호불일치](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=955-5669)

노드 ID: 955:5669. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 본인 확인 · 현재 비밀번호 · •••••••• · 다음 · 취소 · 현재 비밀번호가 일치하지 않습니다. 다시 확인해 주세요.

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이동 → WF-L10-탈퇴-본인확인 |

#### [WF-L11-탈퇴-최종확인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2723)

노드 ID: 692:2723. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: ‹ · 최종 탈퇴 확인 · 정말 회원 탈퇴를 진행하시겠습니까? · 최종 탈퇴 확인 · 취소

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| ‹ | ON_CLICK | 이전 화면으로 |
| 최종 탈퇴 확인 | ON_CLICK | 이동 → WF-L12-탈퇴완료-회원이용종료 |
| 취소 | ON_CLICK | 이동 → WF-L01-설정 |

#### [WF-L12-탈퇴완료-회원이용종료](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR/?node-id=692-2748)

노드 ID: 692:2748. 스크롤: 세로 스크롤 뷰포트 VERTICAL / 넘친 콘텐츠 숨기기 적용.

화면에 포함된 텍스트: 회원 이용 종료 · 회원 탈퇴가 완료되었습니다. · 회원 서비스 이용이 종료되었습니다. · 로그인 화면으로 이동

| 조작 대상 | 트리거 | 현재 동작과 조건 |
| --- | --- | --- |
| 로그인 화면으로 이동 | ON_CLICK | 이동 → WF-A02-로그인 |

## 부록 C 외부 주석 원문

다음은 현재 Figma에 있는 작업용 설명이다. 실제 제품 UI나 이 문서의 확정 이동 규칙과 분리해서 읽는다. UF 표기는 기존 주석에서 그대로 가져온 참고 ID이며 공식 문서와 대조해 새로 부여하지 않았다. 현재 연결과 충돌하는 주석은 17절의 확인 필요 항목을 따른다.

### 주석 · A01 694:2275

UF-A01

서비스 소개와 로그인·회원가입 진입을 제공하는 시작 화면이다.

### 주석 · A02 694:2301

UF-A02

이메일과 비밀번호로 로그인을 시도한다. 기본 성공 경로는 B01 메인이며 기존 조건부 복귀 분기는 유지한다.

### 주석 · A08 694:2437

UF-A03, UF-B04, UF-H01

프로필 사진·닉네임과 복수 이웃 속성을 설정한다. 이웃 인증 및 활동 지역 설정 진입을 제공한다.

### 주석 · A09 694:2462

UF-A03, UF-B01

활동 지역을 선택한다. 설정 버튼으로 A08-활동지역설정완료 프로필 상태에 돌아간다.

### 주석 · A04 694:2498

UF-A04, UF-A06

등록 이메일의 인증번호를 확인한다. 확인은 A05, 재발송은 A04-재발송 상태로 이어진다.

### 주석 · A05 694:2517

UF-A06

새 비밀번호와 확인값을 입력하는 화면이다. 재설정 완료 버튼으로 로그인 화면에 돌아간다.

### 주석 · E01 694:3071

UF-E01

지역 안건·지역 활동 정보·투표 작성 유형을 선택한다. 이웃 인증 필요 안내를 표시하지만 유형 선택은 현재 각 작성 화면으로 바로 연결된다.

### 주석 · E02 694:3120

UF-E01, UF-E02

지역 안건을 작성한다. 임시 저장은 해당 버튼 아래 메시지로 표시하며 페이지를 이동하지 않는다. 사진 추가는 E05, 게시하기는 F01로 연결한다.

### 주석 · E03 694:3188

UF-E01, UF-E03

지역 활동 정보를 작성한다. 임시 저장은 해당 버튼 아래 메시지로 표시한다. 사진 추가는 E05, 게시하기는 F03 상세로 연결한다.

### 주석 · E04 694:3256

UF-E01, UF-E04, UF-J02

투표를 작성한다. 임시 저장은 해당 버튼 아래 메시지로 표시한다. 사진 추가는 E05, 게시하기는 F04 진행 투표 상세로 연결한다.

### 주석 · E05 694:3286

UF-E05

카메라·갤러리 선택과 첨부 미리보기·교체·제거를 제공한다. 작성 화면으로 버튼과 뒤로는 진입 화면으로 복귀하며 관련 상태 설명은 화면 외부 작업용 주석에 있다.

### 주석 · E06 694:3320

UF-E06, UF-J02

진행 중인 본인 투표의 제목·본문·사진·링크·종료 일시·종료 전 알림을 수정한다. F25 상세 아래 게시물 수정 버튼으로 진입하고 변경 저장 후 같은 F25 상세로 돌아간다. 지역·주제·질문·선택지는 변경할 수 없으며 삭제는 E08 확인으로 이어진다.

### 주석 · F01 694:3395

UF-F01, UF-F02, UF-F03, UF-F04, UF-F05

사진 있는 안건 상세이다. 북마크·댓글 등록 결과는 해당 버튼 아래에 표시하고 화면을 유지한다. AI 요약·신고 입력 이동은 유지한다.

상태 예시 보기 → 이웃 미인증 회원

### 주석 · F02 694:3468

UF-F01, UF-D01

사진 없는 안건 상세이다. 북마크·댓글 등록 결과는 해당 버튼 아래에 표시하고 화면을 유지한다. AI 요약·신고 입력 이동은 유지한다.

### 주석 · F03 694:3555

UF-F01, UF-E03

지역 활동 상세이다. 북마크·댓글 등록 결과는 해당 버튼 아래에 표시한다. 외부 참여 URL은 현재 화면에 지정되지 않았다.

상태 예시 보기 → 종료·취소 활동

### 주석 · F04 694:3637

UF-F01, UF-G01, UF-G02

투표 미선택 안내·북마크·댓글 등록 결과는 해당 버튼 아래에 표시한다. 투표 제출 후 결과 화면 F11과 선택 변경 확인 F05는 유지한다.

상태 예시 보기 → 종료 투표

### 주석 · F07 694:3738

UF-F05

북마크 저장 성공 안내를 표시한다. 확인과 뒤로는 진입 화면으로 복귀하며 저장·해제 토글이나 실제 저장 상태 갱신은 이 화면에서 표현하지 않는다.

### 주석 · F08 694:3753

UF-F06

신고 입력을 유지하면서 제출 버튼 아래에 접수 성공 또는 실패 메시지를 표시한다. 실패 후 같은 버튼으로 다시 제출할 수 있으며 별도 결과 페이지로 이동하지 않는다.

상태 예시 보기 → 신고 실패

### 주석 · F09 694:3829

UF-G02, UF-E06

종료 투표 결과를 열람한다. 북마크·댓글 등록 결과는 해당 버튼 아래에 표시하고 화면을 유지한다.

### 주석 · G01 694:3899

UF-A05, UF-F01, UF-F03, UF-F05

공유받은 게시물의 공개 내용과 댓글을 열람하고 게스트 댓글·답글을 작성한다. 회원 기능은 로그인 안내와 함께 G02로 이동하며 로그인 전에는 해당 동작을 저장하지 않는다. 일반 탐색 메뉴는 제공하지 않는다.

### 주석 · G02 694:3916

UF-A02, UF-A05

등록 이메일·비밀번호로 로그인한다. 원 게시물 복귀 정보에 따라 일반 게시물은 G05, 투표는 G09로 돌아가며 일반 진입은 B01로 이동한다. 가입·비밀번호 복구 중 복귀 정보를 유지하고 로그인만으로 참여를 자동 실행하지 않는다.

### 작업용 주석 · 관련 상태 · 디자인 제외 694:3939

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

G02 복귀 참고: 가입 완료와 비밀번호 복구 후 로그인에서도 원 게시물 복귀 정보를 유지한다. 복귀만으로 북마크·반응·투표를 자동 저장하지 않는다. 기존 G03은 별도 화면이 아니며 이 주석은 G02의 디자인 제외 참고 정보다.

### 주석 · G04 694:3957

UF-F05

공개 게시물 링크를 공유하며 링크 복사 버튼 아래에 완료 안내를 표시한다. 수신자는 특정 게시물 상세로 직접 진입하고 열람에 인증을 강제하지 않는다. 링크 복사는 프로토타입 상태 시연이며 실제 클립보드 처리는 구현 단계에서 적용한다.

### 주석 · G05 694:4007

UF-A05, UF-F02, UF-F05

원 게시물로 돌아온 지역 미인증 회원 상태다. 북마크는 다시 눌러 저장하며 반응·이웃 인증 버튼은 인증 화면으로 이동한다. 로그인 복귀만으로 참여가 저장되거나 게스트 댓글 예외가 적용되지 않는다.

### 주석 · H01 694:4056

UF-J01, UF-J02

알림과 활동 기록을 전체·알림·활동으로 구분해 확인한다. 일반 알림은 관련 상세로, 삭제 결과는 H03 안내로 이동한다.

상태 예시 보기 → 알림 없음

### 주석 · H02 694:4091

UF-J01

푸시 OFF여도 서비스 내 기록 생성·보관. 빈 상태 예시.

### 주석 · H03 694:4106

UF-J01, UF-F06

신고한 게시물이 삭제된 결과를 안내한다. 삭제된 콘텐츠는 다시 열 수 없고 알림 목록으로 돌아간다.

### 주석 · I01 694:4190

UF-H01, UF-H05

활동 숫자는 실제 행동 횟수, 게시물 수와 다름. 취소+0, 재등록+1, 평가 직접전환/투표변경+0. 배지는 현재 유효 기관 인증에서만 파생.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I02 694:4261

UF-H02, UF-F05

북마크 토글은 상세만. 목록 유형·주제 유지 후 복귀.

### 주석 · I03 694:4310

UF-H02, UF-E06

내가 만든 게시물 카드를 선택해 본인 상세로 진입한다. 지역 안건은 F24 → E09 → F24, 진행 중 투표는 F25 → E06 → F25 순서로 수정·저장한다. 목록에서 수정 화면으로 바로 이동하지 않는다.

### 주석 · I04 694:4372

UF-H03, UF-H05

게시물별 한 카드, 현재 유효한 모든 참여 표시. 북마크만/타인의 행동 제외. 댓글과 답글은 댓글 작성으로 묶음.

### 주석 · I05 694:4421

UF-H04, UF-G02

삭제/접근 불가 기록 유지하되 본문·선택지 콘텐츠 미노출. 실제 내 선택과 최다 별도. 빈 기록 안내.

### 주석 · I06 694:4460

UF-H01

취소는 기존 프로필 유지. 저장 실패/중복 닉네임 → 입력 안내, 완료로 표시하지 않음.

### 주석 · I07 694:4487

UF-B01

기본 활동 지역을 검색하거나 현재 위치로 찾고, 선택한 지역으로 변경한다.

### 주석 · I08 694:4512

UF-B03

개수 제한 없음. 탐색/추천만, 알림·권한 부여 없음. 별도 저장 강제 없음. 0개면 활동 지역+키워드로 추천.

### 주석 · J01 694:4533

UF-B04

프로토타입은 선택·첨부·제출·접수. 인정 증빙 종류/보관/교체UI 미정. 인증 완료 최대3지역, 실제 심사/시연설정 사용자UI 없음.

### 주석 · J02 694:4549

UF-B04

실제 운영자 승인과 지역 권한 부여 제외. 이웃 인증으로 기관 배지 없음.

### 주석 · J03 694:4583

UF-I01

업무 이메일은 심사용, 6자리 인증 없음. 신청은 로그인 회원 선행. 일반 학교/기관 소속만으로 자격 부여 없음.

### 주석 · J04 694:4604

UF-I01

자료 제출이 최종 제출, 추가 최종확인 화면 없음. 첨부만으로 배지/담당자 권한 없음.

### 주석 · J05 694:4618

UF-I01, UF-I02

실제 심사/승인/반려/보완메일 제외. 최종 정책: 승인일부터1년 유효, 만료 시 배지와 전용 기능 해제; 일반회원/게시물 유지.

### 주석 · K01 694:4648

UF-I03

전체 지역 열람, 새 채택은 인증 담당 지역 지역 안건만. 채택한 게시물은 같은 화면 필터/구역, 별도 서비스화 없음.

### 주석 · K02 694:4676

UF-I04, UF-I03

유효 기관+담당지역+지역안건 조건. 채택은 관계, 게시물 상태 변경/실제 해결 완료 아님. 이웃 인증은 채택 선행 조건 아님.

### 주석 · K03 694:4695

UF-I03, UF-I04

기관 전체 열람과 새 채택 범위 분리. 활동/투표 채택 불가. 주민 반응 등은 대상지역 이웃 인증 별도.

### 주석 · K04 694:4717

UF-I04, UF-I02

채택 기록과 기관별 채택 관계를 확인한다. 본 기관 채택만 취소할 수 있고 담당자 목록으로 돌아간다.

### 주석 · L01 694:4757

UF-H08, UF-H02

설정에서 계정 관리, 개인 활동, 관심 키워드, 알림 설정, 로그아웃과 회원 탈퇴로 이동한다.

### 주석 · L02 694:4774

UF-H06, UF-H07

등록 이메일을 확인하고 이메일 변경 또는 비밀번호 변경으로 이동한다.

### 주석 · L03 694:4793

UF-H06, UF-A04

현재 비밀번호를 확인하고 새 이메일로 인증번호 발송을 요청한다.

### 주석 · L04 694:4816

UF-H06, UF-A04

새 이메일의 6자리 인증번호를 확인한 뒤 이메일 변경을 완료하거나 재발송한다.

### 주석 · L05 694:4838

UF-H07, UF-A04

등록 이메일로 인증번호를 발송하고 6자리 인증으로 본인 확인을 진행한다.

### 주석 · L06 694:4859

UF-H07

인증 완료 후 새 비밀번호와 확인값을 입력해 비밀번호를 변경한다.

### 주석 · L07 694:4892

UF-B03

추천에 사용할 관심 키워드를 최대 4개까지 선택하고 저장한다.

### 주석 · L08 694:4907

UF-H08

푸시 알림 수신을 하나의 ON/OFF 토글로 관리한다.

### 주석 · L09 694:4923

UF-J04

탈퇴 전 유지·삭제되는 정보와 처리 순서를 안내한다.

### 주석 · L10 694:4941

UF-J04

현재 비밀번호를 다시 확인한 뒤 최종 탈퇴 확인으로 이동한다.

### 주석 · L11 694:4956

UF-J04

최종 확인 후 회원 탈퇴를 처리한다.

### 주석 · L12 694:4968

UF-J04

회원 탈퇴 완료를 안내하고 로그인 화면으로 이동한다.

### 주석 · F11 695:3498

UF-G01, UF-G02

선택한 항목과 투표 결과를 표시한다. 미선택 안내는 다른 선택 제출 버튼 아래에 표시한다. 선택 변경 확인·취소 흐름은 유지한다.

### 주석 · F12 695:3544

UF-F01, UF-E03

종료된 걷기 모임의 외부 참여 비활성과 문의 정보 없음 상태를 표시한다. 취소 상태는 안내 문구로 설명되며 별도의 취소 화면·상태 전환은 없다.

### 주석 · G06 695:3575

UF-A05, UF-C04

게스트는 공유받은 특정 상세의 요약만. 원문 → 동일 공유 상세, 일반 게시판/메인/지도 경로 없음.

### 주석 · G07 695:3596

UF-F03

게스트가 부모 댓글의 대상 사용자에게 답글을 작성한다. 로그인 없이 등록 후 이전 화면으로 돌아가며 답글은 부모 아래 1단계로 표시한다. 가입 후 기존 게스트 답글을 새 계정으로 자동 이관하지 않는다.

### 주석 · G08 695:3636

UF-A05, UF-F01, UF-G02

공유 투표의 공개 집계와 게스트 댓글·답글을 이용한다. 투표 제출·북마크 선택 시 동작을 저장하지 않고 G02 로그인으로 이동하며 완료 후 G09로 돌아간다. 댓글·답글 등록 안내는 기존 화면의 버튼 아래에 표시한다.

### 주석 · G09 707:3570

UF-A05, UF-F02, UF-G01, UF-G02

로그인 후 원 공유 투표로 돌아온 지역 미인증 회원 상태다. 북마크는 다시 눌러 저장하고 투표·댓글 참여는 해당 지역 이웃 인증이 필요하며 자동 투표 제출은 없다. 뒤로 가기는 로그인 화면이 아닌 메인으로 이동한다.

### 주석 · A03 729:3508

UF-A06

등록 이메일을 입력하고 인증번호 발송을 요청하는 화면이다.

### 주석 · A06 729:3511

UF-A03, UF-A04

가입 계정정보와 약관을 입력하고 인증번호를 발송한다. 이메일 인증 전에는 프로필 설정 진행 버튼이 비활성 상태이다.

### 주석 · A07-발송 729:3514

UF-A03, UF-A04

인증번호 발송 상태이다. 확인은 A07-완료, 재발송은 A07-재발송으로 이어지며 가입 진행은 비활성 상태이다.

### 주석 · A04-재발송 740:3625

UF-A04, UF-A06

비밀번호 복구 인증번호 재발송 안내 상태이다. 확인은 A05로 이어진다. 같은 화면에서의 반복 재발송 상태 갱신은 별도 구현이 필요하다.

### 주석 · A07-재발송 740:3628

UF-A03, UF-A04

인증번호 재발송 상태이다. 확인 후 A07-완료로 이동하며, 인증 전 가입 진행은 비활성 상태이다.

### 주석 · A07-완료 740:3631

UF-A03, UF-A04

인증 완료 상태이다. 인증 입력·버튼은 비활성, 타이머는 숨김 상태이며 가입 버튼으로 A08에 진입한다.

### 주석 · A08-활동지역설정완료 746:3661

UF-A03, UF-B01, UF-H01

활동 지역 월계1동을 선택한 프로필 설정 완료 상태이다. 설정 완료하고 시작하기를 누르면 B01 메인으로 이동한다.

### 주석 · B01 748:3655

UF-B02

기본 활동 지역 월계1동의 메인이다. 검색은 C02, 지역 선택은 B02, 전체 지도는 D01로 이동한다.

### 주석 · B02 748:3658

UF-B02

현재 탐색 지역을 선택한다. 하계2동 선택은 B03으로, 닫기는 진입 화면으로 돌아간다. 월계2·3동과 하계1동은 시연 범위 밖이다.

### 주석 · B03 748:3661

UF-B02

탐색 지역을 하계2동으로 변경한 이웃 미인증 상태이다. 검색은 B12, 게시판·안건·투표 결과·지도는 하계2동 맥락을 유지한다.

### 주석 · B04 761:6555

UF-B02

하계2동이 현재 탐색 지역, 월계1동이 기본 활동 지역이다. 월계1동 선택은 B01로, 닫기는 진입 화면으로 돌아간다. 다른 동네는 시연 범위 밖이다.

### 주석 · B05 761:6558

UF-B02

하계2동 게시물을 열람하며 활동 지역 기반 추천은 월계1동 기준을 유지한다. 하계2동 글쓰기는 B09로 연결한다. 새 추천·검색·필터 동작은 이 시연에 포함하지 않는다.

### 주석 · B06 761:6561

UF-B02

하계2동 안건과 AI 요약을 열람한다. 북마크 저장 결과는 해당 버튼 아래에 표시한다. 지역 참여 제한과 신고 입력 흐름은 유지한다.

### 주석 · B07 761:6564

UF-B02

하계2동 투표의 공개 집계와 댓글을 열람한다. 투표·반응·댓글·평가는 B09 제한 안내로 연결하며 실제 참여를 실행하지 않는다.

### 주석 · B08 761:6567

UF-B02

본문을 채우는 지도에 동 경계와 기존 하계2동 대표 안건을 표시한다. 기본 활동 지역·미인증 탐색 안내와 대표 게시물 카드를 유지하며 안건 선택은 B06으로 연결한다.

### 주석 · B09 761:6570

UF-B02, UF-B04, UF-F02~F04

하계2동 이웃 인증이 없는 로그인 회원의 지역 참여 제한을 안내한다. 돌아가기는 진입 화면, 기본 활동 지역으로 돌아가기는 B01로 연결한다. 인증 진행은 이 시연에 포함하지 않는다.

### 주석 · B10 761:6573

UF-B02

하계2동 안건을 3문장으로 요약한다. 원문 보기는 B06으로, 닫기는 진입 화면으로 돌아간다.

### 주석 · B11 761:6576

UF-B02

북마크·공유·신고는 로그인 회원이 지역 이웃 인증 없이 이용할 수 있음을 안내한다. 실제 저장·공유·신고 접수는 실행하지 않으며 돌아가기로 진입 화면에 복귀한다.

### 주석 · C01 771:4072

UF-C01, UF-C03

게시판의 새 추천은 추천 내용이 바뀐 성공 상태로 연결한다. 새 후보가 없는 경우 해당 버튼 아래에 안내를 표시하며 마지막 추천을 유지한다.

### 주석 · C02 771:4075

UF-C02

검색어·지역·게시물 유형·주제 조건을 표시한다. 검색어 입력 클릭으로 보행로/백화점 예시를 전환하고 검색 시 결과 있음/없음으로 연결한다. 이는 자유 텍스트 입력을 대체하는 프로토타입 시연이다.

### 주석 · C02-검색결과있음 771:4078

UF-C02

보행로 검색과 월계1동·지역 안건·교통 조건의 결과를 보여준다. 결과 게시물을 선택해 상세로 이동한다.

### 주석 · C02-검색결과없음 771:4081

UF-C02

백화점 검색과 월계1동·지역 안건·생활정보 조건에서 검색 결과가 없음을 보여준다.

### 주석 · C01-새추천성공 771:4084

UF-C03

광운대역 야간 조명 개선 추천을 표시한다. 추천 카드 클릭은 F19, 새 후보가 없는 안내는 새 추천 버튼 아래에 표시한다.

### 작업용 주석 · 관련 상태 · 디자인 제외 778:4084

WF-E05-사진-첨부미리보기

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

관련 상태

선택 취소 → 기존 입력과 기존 사진 유지

한도/형식/권한/업로드 오류 → 실패 안내, 입력 유지

### 작업용 주석 · 관련 상태 · 디자인 제외 778:4090

WF-F01 · 댓글 버튼 아래의 정상·예외 상태

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

관련 상태

빈 입력 → 등록 차단

저장 실패 → 입력 유지·재시도

통과 → 운영자 사전 승인 없이 즉시 공개

### 작업용 주석 · 관련 상태 · 디자인 제외 778:4097

WF-F08 · 신고 제출 버튼 아래의 정상·예외 상태

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

관련 상태

실패 팝업 → 신고에 실패했습니다.

실패 시 입력 유지·재시도

실제 심사·경고 누적·제재·삭제·결과 발송은 후순위 운영

신고 접수·실패 안내는 F08의 제출 버튼 아래에 표시한다. 별도 상태 화면은 사용하지 않는다.

### 작업용 주석 · 관련 상태 · 디자인 제외 778:4104

WF-I02-북마크목록

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

관련 상태

빈 상태 → 저장한 게시물이 없습니다.

삭제된 게시물 → 관계 자동 제거, 카드 미유지

### 작업용 주석 · 관련 상태 · 디자인 제외 778:4110

WF-I03-내가만든게시물

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

관련 상태

빈 상태 → 작성한 게시물이 없습니다.

종료 투표 → 수정·삭제 불가

### 작업용 주석 · 관련 상태 · 디자인 제외 778:4116

WF-I04-참여한게시물

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

관련 상태

빈 상태 → 아직 참여한 게시물이 없습니다

### 작업용 주석 · 관련 상태 · 디자인 제외 782:4084

WF-D01-전체지도 · 화면 설명용 메타정보

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음
D01. 동별 경계와 대표 의제를 표시하는 전체 지도이다. 말풍선은 제목에 맞는 F01/F16/F17/F18 상세로 연결하며 다른 지역 참여 권한을 구분한다.

### 작업용 주석 · 관련 상태 · 디자인 제외 782:4087

WF-D03-지도-로딩실패 · 화면 설명용 메타정보

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음
D03. 지도 로딩 실패 안내이다. 다시 시도는 D01로 연결되며 A~F의 정상 지도에서 이 실패 화면으로 진입하는 분기는 없다.

### 작업용 주석 · 관련 상태 · 디자인 제외 789:4347

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

하계2동 탐색 지역을 유지한다. 검색 결과 열람은 B06으로 연결하며 지역 참여는 이웃 인증 여부에 따라 제한한다. 검색어 입력 클릭은 보행로/백화점 두 예시를 전환하는 프로토타입 시연이다.

### 작업용 주석 · 관련 상태 · 디자인 제외 789:4350

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

하계2동 탐색 지역을 유지한다. 검색 결과 열람은 B06으로 연결하며 지역 참여는 이웃 인증 여부에 따라 제한한다. 검색어 입력 클릭은 보행로/백화점 두 예시를 전환하는 프로토타입 시연이다.

### 작업용 주석 · 관련 상태 · 디자인 제외 789:4353

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

하계2동 탐색 지역을 유지한다. 검색 결과 열람은 B06으로 연결하며 지역 참여는 이웃 인증 여부에 따라 제한한다. 검색어 입력 클릭은 보행로/백화점 두 예시를 전환하는 프로토타입 시연이다.

### 작업용 주석 · 관련 상태 · 디자인 제외 789:4409

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

기본 활동 지역 외의 지도 게시물 참여 제한 안내이다. 열람·북마크·공유·신고와 지역 참여 권한을 구분한다.

### 작업용 주석 · 관련 상태 · 디자인 제외 789:4556

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

해당 지도·추천 게시물의 요약이다. 원문 보기는 제목이 일치하는 WF-F16-월계2동안건-열람전용으로 돌아간다.

### 작업용 주석 · 관련 상태 · 디자인 제외 789:4559

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

지도 말풍선 예시 월계도서관 공사 현황의 상세이다. 기본 활동 지역 외의 게시물이므로 지역 참여 제한을 유지한다.

### 작업용 주석 · 관련 상태 · 디자인 제외 789:4706

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

해당 지도·추천 게시물의 요약이다. 원문 보기는 제목이 일치하는 WF-F17-월계3동안건-열람전용으로 돌아간다.

### 작업용 주석 · 관련 상태 · 디자인 제외 789:4709

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

지도 말풍선 예시 스타필드 입점의 상세이다. 기본 활동 지역 외의 게시물이므로 지역 참여 제한을 유지한다.

### 작업용 주석 · 관련 상태 · 디자인 제외 789:4828

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

지도 말풍선 예시 과기대 독서 모임의 상세이다. 기본 활동 지역 외의 게시물이므로 지역 참여 제한을 유지한다.

### 작업용 주석 · 관련 상태 · 디자인 제외 789:4966

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

해당 지도·추천 게시물의 요약이다. 원문 보기는 제목이 일치하는 WF-F19-지역안건-새추천상세으로 돌아간다.

### 작업용 주석 · 관련 상태 · 디자인 제외 789:4969

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

새 추천 카드의 제목과 본문이 일치하는 상세이다.

### 작업용 주석 · 관련 상태 · 디자인 제외 791:4781

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

정상 동작의 완료·확인 상태를 보여주는 프로토타입 시연이다. 실제 서버 저장·삭제·투표 집계는 수행하지 않는다.

### 작업용 주석 · 관련 상태 · 디자인 제외 795:4644

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

일반 회원 기능은 프로토타입 상태 전환만 시연하며 실제 서버 저장·공유·신고 접수는 수행하지 않는다.

### 작업용 주석 · 관련 상태 · 디자인 제외 795:4647

A~F 작업용 예외 시연

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

아래 시작점은 테스트용이며 앱의 화면이나 메뉴가 아닙니다. 정상 흐름을 실패로 연결하지 않고 예외·복구 동작만 확인합니다.

지도 로딩 실패 → 다시 시도

댓글 차단 → 수정 후 등록

신고 실패 → 다시 시도

새 추천 후보 없음

검색 결과 없음

### 작업용 주석 · 관련 상태 · 디자인 제외 805:4656

F07은 G05·G09 북마크 연결에서 사용한다. A~F의 북마크 결과는 원래 버튼 아래에 표시하며 G섹션은 이번 작업에서 변경하지 않았다.

### 작업용 주석 · 관련 상태 · 디자인 제외 895:4705

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

내가 만든 지역 안건 상세다. 화면 아래 게시물 수정 → E09 → 변경 저장 → 이 상세로 복귀한다.

### 작업용 주석 · 관련 상태 · 디자인 제외 895:4708

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

내가 만든 진행 중 투표 상세다. 화면 아래 게시물 수정 → E06 → 변경 저장 → 이 상세로 복귀한다.

### 작업용 주석 · 관련 상태 · 디자인 제외 895:4711

화면 설명용 메타정보이며 실제 디자인·구현에 포함하지 않음

내 지역 안건의 기존 내용을 수정한다. 변경 저장 시 F24의 같은 게시물 상세로 돌아가며 별도 완료 화면은 사용하지 않는다.

### 주석 · D01 1134:6250

UF-D01 | 전체 이슈 지도를 탐색하는 화면이다. 지도에서 지역 이슈를 확인하고 관련 게시물 상세로 이동한다.

### 주석 · D03 1134:6252

UF-D01 | 지도 로딩 실패 상태이다. 오류 안내와 재시도 경로를 제공하며 기존 탐색 맥락을 유지한다.

### 주석 · B12 1134:6254

UF-B02, UF-C02 | 하계2동 탐색 맥락의 검색·필터 화면이다. 검색 결과에서도 선택한 탐색 지역을 유지한다.

### 주석 · B13 1134:6256

UF-B02, UF-C02 | 하계2동 검색 결과가 있는 상태이다. 결과 게시물을 선택해 같은 탐색 지역 맥락의 상세로 이동한다.

### 주석 · B14 1134:6258

UF-B02, UF-C02 | 하계2동 검색 결과가 없는 상태이다. 현재 탐색 지역과 검색 조건은 유지한다.

### 주석 · F20 1134:6260

UF-B03, UF-F02 | 탐색 지역에서 참여 권한이 없는 회원에게 지역 참여 제한을 안내한다. 열람 맥락은 유지한다.

### 주석 · F16 1134:6262

UF-D01, UF-F01 | 월계2동 지도에서 진입한 지역 안건의 열람 전용 상태이다. 현재 탐색 지역의 게시물 내용을 확인한다.

### 주석 · F21 1134:6264

UF-D01, UF-C03 | 월계2동 지도 안건의 AI 요약 상태이다. 요약과 원문 게시물의 연결을 유지한다.

### 주석 · F17 1134:6266

UF-D01, UF-F01 | 월계3동 지도에서 진입한 지역 안건의 열람 전용 상태이다. 현재 탐색 지역의 게시물 내용을 확인한다.

### 주석 · F22 1134:6268

UF-D01, UF-C03 | 월계3동 지도 안건의 AI 요약 상태이다. 요약과 원문 게시물의 연결을 유지한다.

### 주석 · F18 1134:6270

UF-B02, UF-F01 | 하계1동 지역 활동 정보의 열람 전용 상태이다. 탐색 지역 맥락에서 공개 활동 정보를 확인한다.

### 주석 · F19 1134:6272

UF-C03, UF-F01 | 게시판 AI 새 추천 카드에서 진입한 지역 안건 상세이다. 추천 원본 게시물을 열람한다.

### 주석 · F23 1134:6274

UF-C03 | 월계1동 새 추천 안건의 AI 요약 상태이다. 추천 게시물의 원문과 요약 연결을 유지한다.

### 주석 · E08 1134:6276

UF-E06 | 본인 진행 중 투표의 삭제 확인 상태이다. 삭제 확인 시 투표 삭제 상태를 반영하고 투표 작성 화면 E04로 이동한다. 취소 시 진행 중 투표 수정 화면 E06으로 돌아간다.

### 주석 · F24 1134:6278

UF-I03, UF-F01 | 내가 만든 지역 안건에서 진입한 본인 게시물 상세이다. 본인 게시물 관리 진입점을 제공한다.

### 주석 · F25 1134:6280

UF-I03, UF-G02, UF-E06 | 내가 만든 진행 중 투표의 본인 상세이다. 투표 결과와 본인 선택을 확인하고 수정은 E06으로 진입한다.

### 주석 · E09 1134:6282

UF-E06 | 본인이 작성한 지역 안건을 수정하는 화면이다. 기존 게시물 내용을 편집하고 저장 후 본인 게시물 상세로 복귀한다.

### 주석 · G02-로그인-원게시물복귀 1141:6250

확인 필요

현재 화면 상태에 대응하는 UF와 설명은 추가 확인이 필요하다.

### 주석 · E03-글쓰기-지역활동정보 1141:6253

UF-E03

지역 활동 정보 게시 완료 상태다. 게시 완료 후 작성 결과를 확인하고 해당 게시물 상세로 이어지는 현재 화면 상태를 설명한다.

### 주석 · E04-글쓰기-투표 1141:6256

UF-E04

투표 게시 완료 상태다. 게시 완료 후 작성 결과를 확인하고 진행 중 투표 상세로 이어지는 현재 화면 상태를 설명한다.

### 주석 · H02-알림및활동-빈상태 1141:6259

UF-J01, UF-J02

알림 및 활동에서 알림 탭을 선택한 상태다. 현재 화면의 알림 목록과 관련 상세 이동을 확인한다.

상태 예시 보기 → 알림 없음

### 주석 · H03-삭제된게시물안내 1141:6263

UF-J01, UF-J02

알림 및 활동에서 알림 탭을 선택한 상태다. 현재 화면의 알림 목록과 관련 상세 이동을 확인한다.

상태 예시 보기 → 알림 없음

### 주석 · L02-계정관리-이메일변경완료 1141:6267

UF-H06, UF-H07

계정 관리의 결과 또는 마이페이지 진입 상태다. 현재 화면에 표시된 계정 관리 결과를 유지한다.

### 주석 · L03-이메일변경-발송실패 1141:6270

UF-H06, UF-A04

이메일 변경 과정의 입력·오류·마이페이지 진입 상태다. 현재 화면에 표시된 검증 결과를 유지한다.

### 주석 · L04-이메일변경-인증-불일치 1141:6273

UF-H06, UF-A04

이메일 변경 인증의 불일치·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · L04-이메일변경-인증-만료 1141:6276

UF-H06, UF-A04

이메일 변경 인증의 불일치·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · L04-이메일변경-인증-재발송 1141:6279

UF-H06, UF-A04

이메일 변경 인증의 불일치·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · L05-비밀번호변경-인증-발송완료 1141:6282

UF-H07, UF-A04

비밀번호 변경 본인 인증의 발송·오류·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · L05-비밀번호변경-발송실패 1141:6285

UF-H07, UF-A04

비밀번호 변경 본인 인증의 발송·오류·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · L05-비밀번호변경-인증-불일치 1141:6288

UF-H07, UF-A04

비밀번호 변경 본인 인증의 발송·오류·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · L05-비밀번호변경-인증-만료 1141:6291

UF-H07, UF-A04

비밀번호 변경 본인 인증의 발송·오류·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · L05-비밀번호변경-인증-재발송 1141:6294

UF-H07, UF-A04

비밀번호 변경 본인 인증의 발송·오류·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · L07-관심키워드-저장실패 1141:6297

UF-B03

관심 키워드 선택 또는 저장 실패 상태다. 공통 주제에서 최대 4개 선택 정책을 기준으로 현재 상태를 표시한다.

### 주석 · K01-담당자-채택게시물 1141:6300

UF-I03

기관 담당자 안건 목록의 필터 상태다. 전체 안건, 본 기관 채택 게시물 또는 담당 지역 기준으로 목록을 확인한다.

### 주석 · K01-담당자-월계1동 1141:6303

UF-I03

기관 담당자 안건 목록의 필터 상태다. 전체 안건, 본 기관 채택 게시물 또는 담당 지역 기준으로 목록을 확인한다.

### 주석 · I02-북마크-유형-지역안건 1141:6306

UF-H02, UF-F05

북마크 목록의 유형·주제 필터 적용 상태다. 선택한 필터 조건에 맞는 북마크 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I02-북마크-유형-지역활동정보 1141:6310

UF-H02, UF-F05

북마크 목록의 유형·주제 필터 적용 상태다. 선택한 필터 조건에 맞는 북마크 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I02-북마크-유형-투표 1141:6314

UF-H02, UF-F05

북마크 목록의 유형·주제 필터 적용 상태다. 선택한 필터 조건에 맞는 북마크 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I02-북마크-주제-교통 1141:6318

UF-H02, UF-F05

북마크 목록의 유형·주제 필터 적용 상태다. 선택한 필터 조건에 맞는 북마크 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I02-북마크-주제-주거 1141:6322

UF-H02, UF-F05

북마크 목록의 유형·주제 필터 적용 상태다. 선택한 필터 조건에 맞는 북마크 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I02-북마크-주제-안전 1141:6326

UF-H02, UF-F05

북마크 목록의 유형·주제 필터 적용 상태다. 선택한 필터 조건에 맞는 북마크 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I02-북마크-주제-복지 1141:6330

UF-H02, UF-F05

북마크 목록의 유형·주제 필터 적용 상태다. 선택한 필터 조건에 맞는 북마크 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I02-북마크-주제-생활정보 1141:6334

UF-H02, UF-F05

북마크 목록의 유형·주제 필터 적용 상태다. 선택한 필터 조건에 맞는 북마크 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I02-북마크-주제-환경 1141:6338

UF-H02, UF-F05

북마크 목록의 유형·주제 필터 적용 상태다. 선택한 필터 조건에 맞는 북마크 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I02-북마크-주제-기타 1141:6342

UF-H02, UF-F05

북마크 목록의 유형·주제 필터 적용 상태다. 선택한 필터 조건에 맞는 북마크 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I03-내가만든게시물-지역안건 1141:6346

UF-H02, UF-E06

내가 만든 게시물 목록의 게시물 유형 상태다. 선택한 유형의 본인 작성 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I03-내가만든게시물-지역활동정보 1141:6350

UF-H02, UF-E06

내가 만든 게시물 목록의 게시물 유형 상태다. 선택한 유형의 본인 작성 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I03-내가만든게시물-투표 1141:6354

UF-H02, UF-E06

내가 만든 게시물 목록의 게시물 유형 상태다. 선택한 유형의 본인 작성 게시물을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · L03-이메일변경-현재비밀번호불일치 1141:6358

UF-H06, UF-A04

이메일 변경 과정의 입력·오류·마이페이지 진입 상태다. 현재 화면에 표시된 검증 결과를 유지한다.

### 주석 · L03-이메일변경-이미사용중인이메일 1141:6361

UF-H06, UF-A04

이메일 변경 과정의 입력·오류·마이페이지 진입 상태다. 현재 화면에 표시된 검증 결과를 유지한다.

### 주석 · L06-비밀번호변경-형식오류 1141:6364

UF-H07

비밀번호 변경의 형식 오류·확인 불일치 또는 마이페이지 진입 상태다. 현재 검증 결과를 표시한다.

### 주석 · L06-비밀번호변경-확인불일치 1141:6367

UF-H07

비밀번호 변경의 형식 오류·확인 불일치 또는 마이페이지 진입 상태다. 현재 검증 결과를 표시한다.

### 주석 · L10-탈퇴-본인확인-비밀번호불일치 1141:6370

UF-J04

회원 탈퇴 본인 확인에서 비밀번호가 일치하지 않는 상태다. 탈퇴는 진행하지 않고 오류를 표시한다.

### 주석 · L07-관심키워드-4개선택 1141:6373

UF-B03

관심 키워드 선택 또는 저장 실패 상태다. 공통 주제에서 최대 4개 선택 정책을 기준으로 현재 상태를 표시한다.

### 주석 · I04-반응한게시물-지역안건 1141:6376

UF-H03, UF-H05

참여한 게시물 목록의 참여 유형 상태다. 현재 유효한 참여가 있는 게시물을 게시물별 한 카드로 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · L02-계정관리-마이페이지진입 1141:6380

UF-H06, UF-H07

계정 관리의 결과 또는 마이페이지 진입 상태다. 현재 화면에 표시된 계정 관리 결과를 유지한다.

### 주석 · I04-반응한게시물-지역활동정보 1141:6383

UF-H03, UF-H05

참여한 게시물 목록의 참여 유형 상태다. 현재 유효한 참여가 있는 게시물을 게시물별 한 카드로 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · L03-이메일변경-마이페이지진입 1141:6387

UF-H06, UF-A04

이메일 변경 과정의 입력·오류·마이페이지 진입 상태다. 현재 화면에 표시된 검증 결과를 유지한다.

### 주석 · I04-반응한게시물-투표 1141:6390

UF-H03, UF-H05

참여한 게시물 목록의 참여 유형 상태다. 현재 유효한 참여가 있는 게시물을 게시물별 한 카드로 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · L04-이메일변경-인증-마이페이지진입 1141:6394

UF-H06, UF-A04

이메일 변경 인증의 불일치·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · I04-반응한게시물-공감해요 1141:6397

UF-H03, UF-H05

참여한 게시물 목록의 참여 유형 상태다. 현재 유효한 참여가 있는 게시물을 게시물별 한 카드로 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · L05-비밀번호변경-인증-마이페이지진입 1141:6401

UF-H07, UF-A04

비밀번호 변경 본인 인증의 발송·오류·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · I04-반응한게시물-필요해요 1141:6404

UF-H03, UF-H05

참여한 게시물 목록의 참여 유형 상태다. 현재 유효한 참여가 있는 게시물을 게시물별 한 카드로 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · L06-비밀번호변경-마이페이지진입 1141:6408

UF-H07

비밀번호 변경의 형식 오류·확인 불일치 또는 마이페이지 진입 상태다. 현재 검증 결과를 표시한다.

### 주석 · I04-반응한게시물-댓글작성 1141:6411

UF-H03, UF-H05

참여한 게시물 목록의 참여 유형 상태다. 현재 유효한 참여가 있는 게시물을 게시물별 한 카드로 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · L02-계정관리-이메일변경완료-마이페이지진입 1141:6415

UF-H06, UF-H07

계정 관리의 결과 또는 마이페이지 진입 상태다. 현재 화면에 표시된 계정 관리 결과를 유지한다.

### 주석 · I04-반응한게시물-댓글좋아요싫어요 1141:6418

UF-H03, UF-H05

참여한 게시물 목록의 참여 유형 상태다. 현재 유효한 참여가 있는 게시물을 게시물별 한 카드로 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · L04-이메일변경-인증-재발송-마이페이지진입 1141:6422

UF-H06, UF-A04

이메일 변경 인증의 불일치·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · L05-비밀번호변경-인증-발송완료-마이페이지진입 1141:6425

UF-H07, UF-A04

비밀번호 변경 본인 인증의 발송·오류·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · I05-참여한투표-진행중 1141:6428

UF-H04, UF-G02

참여한 투표 목록의 진행/종료 상태다. 본인의 실제 선택과 투표 상태를 기준으로 기록을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I05-참여한투표-종료 1141:6432

UF-H04, UF-G02

참여한 투표 목록의 진행/종료 상태다. 본인의 실제 선택과 투표 상태를 기준으로 기록을 표시한다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · L05-비밀번호변경-인증-재발송-마이페이지진입 1141:6436

UF-H07, UF-A04

비밀번호 변경 본인 인증의 발송·오류·만료·재발송 또는 마이페이지 진입 상태다. 현재 인증 상태를 표시한다.

### 주석 · H01-알림및활동-알림 1141:6439

UF-J01, UF-J02

알림 및 활동에서 알림 탭을 선택한 상태다. 현재 화면의 알림 목록과 관련 상세 이동을 확인한다.

상태 예시 보기 → 알림 없음

### 주석 · I07-기본활동지역-지역선택 1141:6443

UF-B01

기본 활동 지역 변경 과정의 지역 선택 상태다. 선택한 지역을 기본 활동 지역으로 설정하는 흐름을 보여준다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I08-관심지역-추가됨 1141:6447

UF-B03

관심 지역을 추가한 상태다. 관심 지역은 탐색·추천 설정이며 참여 권한이나 알림 권한을 부여하지 않는다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · I08-관심지역-제거됨 1141:6451

UF-B03

관심 지역을 제거한 상태다. 관심 지역은 탐색·추천 설정이며 참여 권한이나 알림 권한을 부여하지 않는다.

상태 예시 보기 → 유효 기관 인증 사용자

### 주석 · E02-글쓰기-지역안건-게시완료 1141:6455

UF-E02

지역 안건 게시 완료 상태다. 게시 완료 후 작성 결과를 확인하고 해당 게시물 상세로 이어지는 현재 화면 상태를 설명한다.

### 주석 · E03-글쓰기-지역활동정보-게시완료 1141:6458

UF-E03

지역 활동 정보 게시 완료 상태다. 게시 완료 후 작성 결과를 확인하고 해당 게시물 상세로 이어지는 현재 화면 상태를 설명한다.

### 주석 · E04-글쓰기-투표-게시완료 1141:6461

UF-E04

투표 게시 완료 상태다. 게시 완료 후 작성 결과를 확인하고 진행 중 투표 상세로 이어지는 현재 화면 상태를 설명한다.

## 부록 D Prototype 시작점과 주요 변수

### 시작점

| 시작점 이름 | 대상 |
| --- | --- |
| WF · 시작·회원가입 | WF-A01-시작 (689:1077) |
| WF · 회원 서비스 | WF-B01-메인 (692:1173) |
| WF · 공유 안건 게스트 | WF-G01-공유-게스트상세 (692:1848) |
| WF · 공유 투표 게스트 | WF-G08-게스트-공유투표결과 (695:3434) |
| WF · 기관 담당자 | WF-K01-담당자-전체안건 (692:2373) |
| WF · 기관 담당자 | WF-K01-담당자-채택게시물 (938:4771) |
| WF · 기관 담당자 | WF-K01-담당자-월계1동 (938:4793) |
| WF · A~F 예외 시연 · 디자인 제외 | 795:4647 (795:4647) |

A~F 예외 시연의 대상은 화면 밖 작업용 주석 그룹이며 제품 화면이 아니다.

### 흐름 관련 변수

| 변수 이름 | 초기 값 | 용도 구분 |
| --- | --- | --- |
| showTrafficBubble | {"93:0":false,"97:0":false} | 기타 참고 변수 |
| 전체동의 | {"93:0":false,"97:0":false} | 기타 참고 변수 |
| 서비스 동의 | {"93:0":false,"97:0":false} | 기타 참고 변수 |
| 개인정보 동의 | {"93:0":false,"97:0":false} | 기타 참고 변수 |
| 수신 동의 | {"93:0":false,"97:0":false} | 기타 참고 변수 |
| returnTo 있음 | {"702:1":false} | 공유 로그인 복귀 |
| returnTo 투표 | {"702:1":false} | 공유 로그인 복귀 |
| AI 요약 원문 화면 | {"702:1":"F01"} | 기타 참고 변수 |
| 검색어 예시 | {"788:0":"보행로"} | 검색 시연 |
| 신고 원문 화면 | {"788:0":"692:1598"} | 기타 참고 변수 |
| 댓글 원문 화면 | {"788:0":"692:1598"} | 기타 참고 변수 |
| 투표 선택 초안 | {"788:0":""} | 투표 시연 |
| 투표 제출 선택 | {"788:0":""} | 투표 시연 |
| 투표 선택지 1 표시 | {"788:0":"○ 보행로 정비    52표 · 65%"} | 투표 시연 |
| 투표 선택지 2 표시 | {"788:0":"○ 조명 개선    28표 · 35%"} | 투표 시연 |
| 투표 내 선택 표시 | {"788:0":"내 선택: 없음  \|  최다: 보행로 정비"} | 투표 시연 |
| 현재 투표 제목 | {"788:0":"보행로 개선, 무엇이 먼저일까요?"} | 투표 시연 |
| 본인 투표 표시 | {"788:0":true} | 투표 시연 |
| WF-E02-글쓰기-지역안건/755:5185/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-E03-글쓰기-지역활동정보/755:5173/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-E04-글쓰기-투표/755:5179/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-E06-본인투표-수정/694:3315/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-E06-본인투표-수정/694:3317/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F01-지역안건상세-사진있음/694:3350/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F01-지역안건상세-사진있음/694:3376/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F01-지역안건상세-사진있음/694:3376/메시지 내용 | {"788:0":"댓글이 등록되었습니다."} | 제자리 안내 상태 |
| WF-F02-지역안건상세-사진없음/694:3423/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F02-지역안건상세-사진없음/694:3449/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F02-지역안건상세-사진없음/694:3449/메시지 내용 | {"788:0":"댓글이 등록되었습니다."} | 제자리 안내 상태 |
| WF-F03-지역활동정보상세/694:3510/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F03-지역활동정보상세/694:3536/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F03-지역활동정보상세/694:3536/메시지 내용 | {"788:0":"댓글이 등록되었습니다."} | 제자리 안내 상태 |
| WF-F04-투표상세-진행중/694:3579/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F04-투표상세-진행중/694:3592/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F04-투표상세-진행중/694:3618/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F04-투표상세-진행중/694:3618/메시지 내용 | {"788:0":"댓글이 등록되었습니다."} | 제자리 안내 상태 |
| WF-F06-댓글-등록차단/694:3699/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| F06/수정 전 표현 오류 표시 | {"788:0":true} | 기타 참고 변수 |
| WF-F08-신고-입력/694:3750/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F08-신고-입력/694:3750/메시지 내용 | {"788:0":"신고가 접수되었습니다."} | 제자리 안내 상태 |
| WF-F09-투표-종료결과/694:3784/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F09-투표-종료결과/694:3810/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F09-투표-종료결과/694:3810/메시지 내용 | {"788:0":"댓글이 등록되었습니다."} | 제자리 안내 상태 |
| WF-F11-투표-제출후/695:3477/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-B06-지역안건상세-하계2동-열람전용/761:6037/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-B07-투표결과-하계2동-이웃미인증/761:6142/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F16-월계2동안건-열람전용/789:4437/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F17-월계3동안건-열람전용/789:4587/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F18-하계1동-독서모임-열람전용/789:4740/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F19-지역안건-새추천상세/789:4854/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F19-지역안건-새추천상세/789:4867/메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| WF-F19-지역안건-새추천상세/789:4867/메시지 내용 | {"788:0":"댓글이 등록되었습니다."} | 제자리 안내 상태 |
| C01 새 추천 / 후보 없음 메시지 표시 | {"788:0":false} | 제자리 안내 상태 |
| G01 댓글·답글 등록 안내 | {"702:1":false} | 제자리 안내 상태 |
| G08 댓글·답글 등록 안내 | {"702:1":false} | 제자리 안내 상태 |
| G04 링크 복사 안내 | {"702:1":false} | 제자리 안내 상태 |
| G07 답글 원문은 투표 | {"702:1":false} | 투표 시연 |

초기 값의 모드 키는 Figma 변수 모드 ID다. 기존에 삭제·변경된 화면 이름을 포함하는 변수도 있어 변수 존재만으로 해당 화면이 현재 존재한다고 판단하지 않는다.

### 기록과 검증 범위

현재 A~L 모바일 프레임 160개, 숨김 요소를 제외한 연결 보유 노드 965개를 기록했다. 목적지가 지정된 이동의 대상은 같은 와이어프레임 페이지의 모바일 프레임이며, L08의 CHANGE_TO 대상은 OFF 컴포넌트다. 목적지 null인 F25 분기는 확인 필요로 분리했다. 화면 밖 상태 예시 연결과 참고 주석은 제품 화면 목록의 개수에 포함하지 않는다. Prototype 전체를 실제 클릭 실행하거나 서버 기능을 테스트한 결과는 아니다.

