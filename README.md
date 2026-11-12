# 온메이트 (ONMATE)

> 차가 내 '싣고 내리는' 습관을 학습해서, 평소와 다르게 물건을 두고 내리려 할 때만 알려주는 차량용 앱

현대자동차그룹 차량용 앱 개발 공모전 출품작 · Pleos Connect (Android Automotive)

## 주요 기능

| 화면 | 설명 |
|---|---|
| 홈 (정차 중) | 위에서 본 차량 지도와 수납함 11곳, 이번 트립에서 연 칸, 학습 현황 |
| 출발 전 물건 입력 | 정차 중 칸(문·트렁크)을 열고 닫으면 "무엇을 두셨나요?" 원탭 입력. 최근 3번 같은 물건이면 추천 |
| 주행 중 | 조작 없이 기록만 함 (운전자 주의 분산 방지) |
| 하차 전 알림 | 도착(속도 0 + 주차 브레이크) 시, 습관과 다르게 아직 안 연 칸을 알림 |
| 하차 중 깜빡임 | 운전석 문이 열리면 물건이 있을 것 같은 칸이 차량 지도에서 깜빡임. 해당 칸을 열면 멈춤 |
| 습관 대시보드 | 상황(요일·시간대·목적지)별 칸 재개방률, 알림 대상 여부 |
| 물건 태그 | 칸마다 자주 두는 물건을 한 번만 등록, 칸별 알림 켜기/끄기 |
| 트립 기록 | 트립마다 출발 전·도착 후 연 칸과 알림 결과 |

### 습관 학습 방식
- 재개방률 = 출발 전에 열고 도착 후 다시 연 횟수 ÷ 출발 전에 연 횟수 (같은 상황끼리)
- 기록 3회 미만: 출발 전에 연 칸을 도착 후 안 열면 알림 (기본 규칙)
- 기록 3회 이상: 재개방률 70% 이상인 칸만 알림
- '두고 갈게요'를 고를 때마다 그 상황의 기준을 10%p씩 올림 (최대 95%)
- 운전석 문은 운전자가 매번 여닫으므로 물건 신호에서 제외
- 모든 데이터는 차량(기기) 안에만 저장, 외부 서버로 보내지 않음

## 실행 방법

1. Android Studio(최신 버전)에서 이 폴더를 엽니다.
2. Pleos Connect Emulator(API 34)를 설치·실행합니다.
   - SDK Manager → SDK Update Sites → `Pleos Connect System Image`
   - Mac: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
   - Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
   - Device Manager → Automotive → API 34 Pleos Connect 이미지, RAM 4GB, Cold boot
3. `app` 실행(Run).
4. 오른쪽 아래 **DEMO** 버튼으로 차량 상태를 흉내 낼 수 있습니다.
   - 예: `뒷좌석 왼쪽`(열고 닫기) → 물건 입력 → `출발` → `도착` → 하차 전 알림 → `운전석 문 열기` → 칸 깜빡임 → `뒷좌석 왼쪽 열기`
   - `자동 시나리오`는 위 과정을 자동으로 재생합니다(시연 영상 녹화용).

단위 테스트: `./gradlew test` (트립 감지·습관 학습·알림 판단 로직)

## 테스트 환경 및 필요한 설정

- Android Studio Koala 이상, JDK 17, Gradle 8.9, Android Gradle Plugin 8.5
- compileSdk 34 / minSdk 28, Kotlin 2.0, Jetpack Compose
- Pleos Connect Emulator 2.1.0 (API 34)
- 현재 버전은 **가짜 차량 데이터(FakeVehicleSource)** 로 동작합니다. 실제 Pleos Vehicle SDK 연동은 `PleosVehicleSource`에서 진행 중입니다.
- 시연용 학습 데이터(약 2주치)가 첫 실행 때 자동으로 들어갑니다. 초기화하려면 앱 데이터를 지우세요.

## APK

- 위치: `apk/팀명_온메이트_release.apk` (빌드 후 추가)
- 용량이 크면 GitHub Releases 링크로 대체

## 사용 SDK / API

| 기능 | SDK | 권한 |
|---|---|---|
| 문·트렁크 개폐 | Vehicle SDK – Door | `pleos.car.permission.CAR_DOORS` |
| 주차 브레이크 | Vehicle SDK – Brake | `pleos.car.permission.CAR_DRIVE` |
| 차량 속도 | Vehicle (속도) | `android.car.permission.CAR_SPEED` |
| 목적지 (확장) | NaviHelper SDK | – |

## 폴더 구조

```
app/src/main/java/com/onmate/app/
├─ domain/     핵심 로직 (순수 Kotlin): TripDetector, HabitLearner, AlertDecider, ItemSuggester
├─ vehicle/    차량 데이터 출처: FakeVehicleSource(시연), PleosVehicleSource(실차 연동 예정)
├─ data/       저장소(JSON 파일), 시연용 시드 데이터
└─ ui/         Jetpack Compose 화면, 차량 지도(CarMap)
```

## 향후 계획

- Pleos Vehicle SDK 실연동, NaviHelper 목적지 기반 학습
- TextToSpeech 음성 알림 (SDK 제공 시)
- 좌석 점유 정보로 사람/짐 구분, BLE 태그로 소형 물품 감지
- 카셰어링·렌터카용 반납 전 소지품 확인 (B2B)
