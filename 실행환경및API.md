# PickPay 실행환경 및 API

기능별 시연은 [README](README.md)를 참고하세요.

카페 메뉴 주문, 공동 장바구니, 결제, 리뷰를 하나의 모바일 흐름으로 연결하는 Android + Spring Boot 기반 주문/결제 서비스입니다.

PickPay는 개인 주문과 그룹 주문을 모두 지원합니다. Android 앱은 메뉴 조회, 장바구니, 공동 주문방, 채팅, 결제, 리뷰 화면을 제공하고, Spring Boot 백엔드는 회원, 메뉴, 주문, 결제 승인, 리뷰, Firebase 연동, 알림 처리를 담당합니다.

## 주요 기능

- 회원가입, 로그인, JWT 발급/갱신/로그아웃
- Android `EncryptedSharedPreferences` 기반 토큰 저장
- 메뉴 목록/상세 조회, 개인 장바구니, 개인 주문, 최근 주문, 영수증 조회
- 그룹 주문방 생성/참여, 초대 링크 공유, 실시간 공동 장바구니
- Firebase Realtime Database 기반 그룹 채팅
- 픽업 담당자 룰렛
- Toss Payments 카드 결제 연동 및 결제 성공/실패 처리
- 결제 완료 후 개인/그룹 영수증 조회
- 리뷰 등록, 이미지 업로드, 리뷰 목록/상세 조회
- 리뷰 이미지 기반 메뉴 분석 및 AI 질의응답
- FCM 토큰 저장 및 그룹 결제 알림
- NFC 메뉴 스캔을 통한 장바구니 추가

## 프로젝트 구조

```text
Pick_Pay/
|-- payclient/                 # Android 클라이언트
|   |-- app/src/main/java/     # Kotlin 소스
|   |-- app/src/main/res/      # XML 레이아웃, drawable, navigation
|   `-- gradle/                # Gradle 버전 카탈로그
|-- paymanagement/             # Spring Boot 백엔드
|   |-- src/main/java/         # Java API, Service, Repository, Domain
|   |-- src/main/resources/    # application.properties, static images
|   `-- src/test/java/         # 테스트 코드
|-- .env.example               # 환경 변수 예시
|-- 실행환경및API.md            # 실행 환경 및 API 문서
`-- README.md                  # 기능별 소개 및 시연
```

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Android | Kotlin, XML Layout, ViewBinding, Jetpack Navigation, Retrofit, OkHttp, Gson |
| Android 저장소/보안 | AndroidX Security Crypto, SharedPreferences |
| Android 외부 연동 | Firebase Realtime Database, Firebase Messaging, Toss Payments Android SDK, NFC, Glide |
| Android AI | Google AI Edge LiteRT-LM |
| Backend | Java 21, Spring Boot 4.0.6, Spring WebMVC, Spring Security, Spring Data JPA |
| Backend 외부 연동 | MySQL, Firebase Admin SDK, Toss Payments REST API, Springdoc OpenAPI, JJWT |
| Build | Gradle, Maven Wrapper |

## 사전 준비

- JDK 21
- MySQL
- Android Studio 및 Android SDK
- Android 기기 또는 에뮬레이터
- Firebase 프로젝트
- Toss Payments 테스트/운영 키

NFC 기능을 테스트하려면 NFC를 지원하는 실제 Android 기기가 필요합니다. `AndroidManifest.xml`에서 NFC 기능이 필수로 선언되어 있습니다.

## 환경 변수

루트 경로의 `.env.example`을 복사해 `.env`를 만듭니다.

```powershell
Copy-Item .env.example .env
```

예시:

```properties
BASE_URL=http://192.168.0.10:8080/

DB_NAME=pickpay_user
DB_PASS=your_password

TOSS_CLIENT_KEY=your_toss_client_key
PG_SECRET_KEY=your_toss_secret_key
TOSS_CONFIRM_URL=https://api.tosspayments.com/v1/payments/confirm
TOSS_CANCEL_URL=https://api.tosspayments.com/v1/payments/{paymentKey}/cancel
PAYMENT_EXPIRATION_MINUTES=30
```

`DB_NAME`은 현재 `application.properties`에서 MySQL 사용자명으로 사용됩니다. 데이터베이스 이름은 JDBC URL에 `pickpay`로 고정되어 있습니다.

Android 앱에서 실제 기기로 백엔드에 접속하려면 `BASE_URL`에 PC의 IPv4 주소를 넣어야 합니다.

- Android 에뮬레이터: `http://10.0.2.2:8080/`
- 실제 기기: `http://<PC IPv4 주소>:8080/`

## 백엔드 실행

MySQL에 데이터베이스를 생성합니다.

```sql
CREATE DATABASE pickpay CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Firebase Admin SDK 키 파일을 아래 경로에 배치합니다.

```text
paymanagement/src/main/resources/firebase-service-key.json
```

서버를 실행합니다.

```powershell
cd paymanagement
.\mvnw.cmd spring-boot:run
```

서버 기본 포트는 `8080`입니다. Swagger UI는 서버 실행 후 아래 주소에서 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

테스트 실행:

```powershell
cd paymanagement
.\mvnw.cmd test
```

## Android 실행

Firebase Android 설정 파일을 아래 경로에 배치합니다.

```text
payclient/app/google-services.json
```

디버그 빌드:

```powershell
cd payclient
.\gradlew.bat assembleDebug
```

테스트 실행:

```powershell
cd payclient
.\gradlew.bat test
```

Android Studio에서 `payclient` 폴더를 열어 실행할 수도 있습니다.

## AI 모델 파일

리뷰 상세 화면의 AI 분석 기능은 앱 내부 저장소에서 아래 파일명을 찾습니다.

```text
gemma-4-E2B-it.litertlm
```

파일이 없으면 앱은 실행되지만 AI 분석 기능은 모델 파일 없음 상태로 동작합니다. 테스트 시에는 해당 모델 파일을 앱의 내부 files 디렉터리에 배치해야 합니다.

```text
/data/data/com.ssafy.payclient/files/gemma-4-E2B-it.litertlm
```

## 주요 API

대부분의 API는 로그인 후 발급받은 Bearer JWT가 필요합니다.

| 구분 | 메서드 및 경로 | 설명 |
| --- | --- | --- |
| Auth/User | `POST /login` | 로그인 |
| Auth/User | `POST /logout` | 로그아웃 |
| Auth/User | `POST /user/exist` | 사용자 ID 중복 확인 |
| Auth/User | `POST /user` | 회원가입 |
| Auth/User | `GET /user` | 내 정보 조회 |
| Auth/User | `PUT /user` | 내 정보 수정 |
| Auth/User | `DELETE /user` | 회원 탈퇴 |
| Auth/User | `PATCH /me/fcm-token` | FCM 토큰 저장 |
| JWT | `POST /jwt/exchange` | 쿠키 기반 토큰 교환 |
| JWT | `POST /jwt/refresh` | 토큰 재발급 |
| Menu | `GET /api/menus` | 메뉴 목록 조회 |
| Menu | `GET /api/menus/{menuId}` | 메뉴 상세 조회 |
| Menu | `GET /api/menus/name/{name}` | 메뉴명으로 조회 |
| Menu | `PATCH /api/menus/{menuId}` | 재고 차감 |
| Order | `POST /api/orders` | 개인 주문 생성 |
| Order | `GET /api/orders/recent` | 최근 주문 조회 |
| Order | `GET /api/orders/{orderNo}/receipt` | 개인 주문 영수증 조회 |
| Group | `POST /api/groups` | 그룹 주문방 생성 |
| Group | `POST /api/groups/join` | 그룹 주문방 참여 |
| Group | `POST /api/groups/{groupId}/close` | 그룹 주문 마감 |
| Group | `GET /api/groups/{groupId}/receipt` | 그룹 영수증 조회 |
| Group | `POST /api/groups/{groupId}/pickup-roulette` | 픽업 담당자 룰렛 실행 |
| Payment | `POST /api/payments/complete` | Toss 결제 승인 완료 처리 |
| Payment | `POST /api/payments/fail` | 결제 실패 처리 |
| Review | `POST /api/reviews` | 리뷰 등록 |
| Review | `GET /api/reviews` | 리뷰 목록 조회 |
| Review | `GET /api/reviews/{reviewId}` | 리뷰 상세 조회 |

## Firebase 사용 위치

- 그룹 주문방 상태: `group_orders/{groupId}/status`
- 실시간 공동 장바구니: `group_orders/{groupId}/items`
- 그룹 채팅: `group_orders/{groupId}/chat/messages`
- 픽업 룰렛: `group_orders/{groupId}/pickupRoulette`

백엔드는 그룹 주문 마감 시 Firebase 장바구니 데이터를 읽어 실제 주문 데이터로 저장하고, 결제 상태 변경 시 Firebase 그룹 상태를 동기화합니다.

## 로컬 실행 팁

- Android 실제 기기와 백엔드는 같은 네트워크에 있어야 합니다.
- Windows 방화벽에서 `8080` 포트 접근이 막히면 앱에서 API 호출이 실패할 수 있습니다.
- `.env`, `google-services.json`, `firebase-service-key.json`은 민감정보 파일이므로 커밋하지 않습니다.
- 서버 최초 실행 시 테이블이 비어 있으면 샘플 사용자, 메뉴, 리뷰 데이터가 자동 생성됩니다.
- 메뉴 이미지는 백엔드의 `src/main/resources/static/images` 하위 파일을 `/images/**` 경로로 제공합니다.
