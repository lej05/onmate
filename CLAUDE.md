# CLAUDE.md — 온메이트(ONMATE) 작업 가이드

Claude Code가 이 저장소에서 작업할 때 먼저 읽는 문서.

## 프로젝트
- 현대자동차그룹 차량용 앱 개발 공모전 출품작. 대학생 5인 팀.
- 플랫폼: Pleos Connect (Android Automotive 기반), 화면 1280×720 가로.
- 핵심: 차량 문·트렁크 개폐 이력으로 사용자의 '싣고 내리는' 습관을 학습하고, 평소와 다르게 물건을 두고 내리려 할 때만 알린다.
- 디자인: 피그마 'ONMATE 와이어프레임' (docs/screens/*.png 참고). 주요 색: 네이비 #002C5F, 경고 주황 #C2410C.

## 일정
- 1차 서류 마감 2026-11-15, 결과 11-23
- 2차 PT 12-03~04: **실제 동작하는 앱 시연 필수** (에뮬레이터 또는 기기)

## 구조 원칙 (지킬 것)
- `domain/` 은 순수 Kotlin. Android·SDK import 금지. 로직 변경 시 `app/src/test` 단위 테스트를 같이 고친다.
- 차량 데이터는 `VehicleSource` 인터페이스로만 받는다. UI·도메인이 Pleos SDK를 직접 부르지 않는다.
- `FakeVehicleSource` 는 시연 영상·개발용으로 계속 유지한다 (DEMO 패널).
- 데이터는 기기 안에만 저장 (외부 전송 없음). 현재 JSON 파일, 필요 시 Room으로 교체.
- 주행 중(TripPhase.DRIVING)에는 입력 UI를 띄우지 않는다.

## 상태 머신 (TripDetector)
```
PARKED --(주차 브레이크 해제 + 속도>0.5m/s)--> DRIVING
DRIVING --(속도<0.1 + 주차 브레이크 체결)--> ARRIVED   → 하차 전 알림
ARRIVED --(운전석 문 열림)--> EXITING                  → 남은 칸 깜빡임
ARRIVED/EXITING --(finishTrip() 또는 다음 출발)--> PARKED → 트립 저장
```
- PARKED 중 연 칸 = 출발 전 열림, ARRIVED 이후 연 칸 = 도착 후 열림
- 운전석(Zone.DRIVER)은 학습·알림 대상 아님 (하차 판단에만 사용)

## 학습·알림 규칙
- 상황 키: 요일 유형(평일/주말) × 시간대 × 목적지(있을 때만)
- 재개방률 = 다시 연 횟수 ÷ 출발 전에 연 횟수
- 3회 미만: 기본 규칙(안 열면 알림) / 3회 이상: 70% 이상만 알림
- '두고 갈게요'(LEFT_INTENTIONALLY) 1회당 기준 +10%p, 최대 95%
- '아무것도 안 뒀어요' 고른 칸은 그 트립에서 알림 제외
- 시연용: `MainViewModel.situationOverride` 가 '평일 아침 · 회사'로 고정되어 있음. 실차 연동 시 null.

## 남은 작업 (우선순위 순)
1. **PleosVehicleSource 구현** — `vehicle/PleosVehicleSource.kt` 주석 참고.
   - Pleos 문서의 실제 API만 사용할 것. 함수 이름을 추측하지 말고, 모르면 사람에게 문서 내용을 요청한다.
   - Door(문별 + 트렁크 지원 여부), Brake(주차 브레이크), 속도. 구독 API 없으면 polling 후 변화 시 emit.
   - `app/build.gradle.kts` 에서 `ai.pleos.playground:Vehicle` 의존성 주석 해제.
   - MainViewModel 의 `vehicle` 을 설정(또는 빌드 타입)으로 Fake/Pleos 전환.
2. Virtual Console로 문·브레이크·속도를 바꿔 전체 흐름 확인.
3. 하차 판단 보강: 문 잠금/시동 끔 신호가 있으면 `finishTrip()` 호출 (현재는 하차 후 8초 자동 마감).
4. AAOS 주행 중 UX 제한(Distraction Optimization) 확인: 주행 중 화면이 시스템에 의해 가려질 수 있음.
5. 릴리스 APK 빌드 → `apk/팀명_온메이트_release.apk`.
6. (확장) NaviHelper 목적지, TTS 음성 알림, 좌석 점유.

## 브랜드·로고 규칙
- 현대자동차 로고를 코드로 그리거나 직접 만들지 않는다. 공식 CI 파일만 `res/drawable/hyundai_logo.*` 로 넣는다.
- 좌우조합 로고 권장, 최소 크기 20mm(≈126dp), 상하좌우 최소 공간 = 심볼 높이. `ui/components/BrandLogo.kt` 가 처리.
- 폭 112dp 내비게이션 레일에는 규정상 넣지 않는다 → 시작 화면(SplashScreen)에만 표시.
- 공모전 앱의 로고 사용 가능 여부는 주최 측 확인 전까지 보류 (파일이 없으면 자동으로 숨김).

## 명령어
- 빌드: `./gradlew assembleDebug`
- 단위 테스트: `./gradlew test`
- 릴리스 APK: `./gradlew assembleRelease` → `app/build/outputs/apk/release/`

## 참고 문서
- Pleos 시작하기: https://document.pleos.ai/docs/pleos-only/getting-started
- Vehicle SDK: https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/intro
- 권한: https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/vehicle-permission
- 공모전: https://appdevchallenge.auto-hmg.io/
