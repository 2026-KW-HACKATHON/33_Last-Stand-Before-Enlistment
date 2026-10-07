# #43 프로필·활동 지역·현재 위치 후보 (FE1 / A-03 · FE-R19)

기준: 2026-10-08 fetch한 origin/front/develop `671ca43`. 직전 확인 이후 FE2/기타 FE 신규 병합 없음. #51 탐색의 defaultActivityRegion/currentExploreRegion/neighborVerifiedRegions/institutionRegions 경계와 #53 PostDisplayModel/PostCard/PostDetail을 확인했으며 FE2 파일을 수정하거나 재구현하지 않았다.

## 범위와 연결

F-RBVFZX / F-QQKYLC. 기존 #40 Route /me/profile, /me/region을 구현한다. #42 SignupEditors/ProfileStepProps/RegionStepProps의 같은 편집기를 가입에도 주입한다. 공통 Header/MobileLayout/Input/Button/Notice와 Navigation/AccessGuard/SessionProvider를 소비한다. 사진 전달은 SignupProfile에 optional Client draft intent만 추가했다. Endpoint/DTO/Enum/실제 업로드 계약은 추가하지 않았다.

프로필 조회/닉네임(Unicode 최대 10자, 중복 Service 오류)/소개(최대 50자)/복수 속성/사진 선택·교체·제거/저장·취소를 구현한다. 알려진 저장 실패는 이전 저장값과 입력을 유지한다. 결과 불명·진행 중 취소는 중복 재전송을 막고 재조회로 확인한다. region 후보는 클릭으로 draft에만 선택하고 설정/가입 완료 버튼에서 반영한다. 프로필에서 연 지역 설정은 프로필 draft로 복귀하고 전체 저장은 프로필 화면에서 한다. 직접 기본 지역 화면의 저장은 같은 Service를 소비한다.

검색/현재 위치 Loading·Empty·Error·권한 거부·수동 검색 fallback·Retry. 일회성 browserRegionLocator는 명시적 클릭에서만 사용하며 변환 Adapter를 주입한다. Mock locate는 권한 상태와 후보를 시뮬레이션하고 실제 위치를 수집하지 않는다. watchPosition/지속 추적/자동 선택/이웃·기관 자격 생성 없음. FE2 탐색 상태와 Session grants를 변경하지 않는다.

root ProfileProvider가 draft/query/candidate/페이지별 scroll을 보유한다. 명시적 프로필 취소는 저장값으로 되돌리고 지역 취소는 진입 시 지역으로 되돌리며 나머지 프로필 입력은 보존한다. 가입 draft·returnTo는 기존 #42/#40 Store가 보유하며 저장/복귀가 참여 동작을 실행하지 않는다. 가입 OTP 확인 이후 signup-incomplete 접점을 소비한다. 계정 종료/전환은 #61에서 store.clear 또는 Provider remount와 안정된 Adapter 주입으로 연결해야 한다. 새로고침 영속 복원은 구현하지 않았다.

## 사진·실 API 경계

ProfileService는 Client port이며 실제 주체 식별/GET/PATCH/닉네임 중복/지역 후보 변환/사진 전송은 #7/#9/#10/#74 및 #61/#66/#68에서 합의·연결한다. photoPolicy가 없으면 새 파일 선택을 비활성화한다. image/*는 개발 Mock fixture용이며 운영 허용 형식/개수/용량/저장/삭제 규칙이 아니다. 게시물 사진 한도를 프로필에 전용하지 않았다. File은 메모리 draft에만 보유하고 object URL은 편집기에서 생성·정리한다. 제거와 교체를 한 번에 요청하지 않는다. 사진 제거 Mock은 기존 URL을 삭제한다. 실제 업로드·서버 저장·삭제·작성자 표시 재조회는 미연동이다.

AppProviders에 profileService 주입과 기본 SignupEditors를 추가했고 Mock은 제품 경로에 등록하지 않았다. 실 Adapter가 없으면 연결 대기 또는 기존 인증 Loading을 표시한다. 실 API 연동 대기. Integration 완료 아님.

## 디자인 / 검증

Figma 최종디자인 Frame 직접 확인: 1157:1703, 1157:1801, 1157:1910, 1160:5372, 1160:5452, 1164:11917. 396×852, Header58/좌우16/위20/아래32/gap12·8, Photo364×80, 칩3열·복수 선택, Input/Notice10·Button/Menu14/Pretendard 토큰을 기준으로 구현했다. OS 상태바와 폐기된 증빙 신청 메뉴는 넣지 않았다. 입력이 없는 상태는 입력 요청/선택 차단, 미지원 사진은 연결 대기다. Figma 프로필 사진은 개발 fixture public/images/profile-preview.png이고 데이터 기반 이미지 슬롯에서 소비한다.

/dev/profile-preview는 development에서만 노출한다. 정상/조회실패/Empty/저장실패/중복/권한제한/결과불명/위치거부/실패/빈후보/검색오류와 가입·공유 returnTo 조립을 선택할 수 있다. 원 화면은 개발 안내로 표시하며 #46 마이페이지나 FE2 화면을 구현하지 않는다.

브라우저 도구에 연결 가능한 브라우저가 없어 실제 396×852 렌더 비교/클릭·파일 선택 UI 인수는 미검증이다. Figma 직접 확인과 실제 화면 비교 완료를 구분한다. 자동 검증 명령/결과는 작업 보고에 기록한다. 실제 권한 프롬프트·좌표 변환·사진 전송·실저장/재조회·계정 전환·FE2 작성자 갱신은 Integration에서 검증한다.

테스트는 package의 기존 lint/typecheck/build와 tsc --target es2023 --lib esnext,dom,dom.iterable --module commonjs --moduleResolution node --jsx react-jsx --strict --skipLibCheck --esModuleInterop --types node --rootDir src --outDir <TEMP> 후 NODE_PATH=node_modules 및 node --test 방식이다. npm test/Dependency/CI 변경 없음.
