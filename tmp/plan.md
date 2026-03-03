# 리팩터링 플랜

## 단계 1: 스타일 정리

스타일 불일치를 하나의 기준으로 통일한다. 작동은 안 바꿈.

### @author/@since Javadoc 제거
auth, member 패키지 일부 파일에만 있고 나머지엔 없으니까 없는 쪽으로 통일.
대상: AuthenticationResolver, JwtProvider, TokenResponse, AdminMemberController, Member, MemberController, MemberRequest, MemberRepository

### @Autowired 제거
생성자 1개면 Spring이 알아서 주입해주니까 필요 없음. 대부분의 클래스가 이미 안 쓰고 있음.
대상: AuthenticationResolver, JwtProvider, AdminMemberController, MemberController

### Entity 필드 빈 줄 통일
Member.java만 필드 사이에 빈 줄이 있고 나머지 엔티티는 없음. 빈 줄 없는 쪽으로 통일.

### Collectors.toList() -> .toList()
OptionController만 `Collectors.toList()` 쓰고 있고 CategoryController는 `.toList()`. Java 21 프로젝트니까 `.toList()`로 통일.

### 블록 주석 제거
KakaoAuthController, OptionController에만 클래스 설명 블록 주석이 있음. 나머지엔 없으니까 제거.

---

## 단계 2: 불필요한 코드 제거

### OrderController.wishRepository 제거
주입만 되고 실제로 쓰이지 않음. `// 6. cleanup wish` 주석이 있어서 의도는 있었지만 미구현 상태.
필드 선언, 생성자 파라미터, 할당 전부 제거. import도 같이 제거.

### Collectors import 제거
단계 1에서 `.toList()`로 바꿨으니 OptionController의 `Collectors` import도 제거.

---

## 단계 3: OrderController -> OrderService

주문 처리의 전체 흐름이 Controller에 들어있어서 Service로 뺀다.

### OrderService 생성
- 의존성: OrderRepository, OptionRepository, MemberRepository, KakaoMessageClient
- `createOrder(member, request)`: 옵션 조회 -> 재고 차감 -> 포인트 차감 -> 주문 저장 -> 카카오 알림
  - 옵션 없으면 `NoSuchElementException` 던짐 (기존 `orElse(null)` 패턴에서 변경)
  - 카카오 알림은 best-effort로, 실패해도 주문은 성공 처리
- `getOrders(memberId, pageable)`: 페이징 조회 후 OrderResponse 변환

### OrderController 수정
- 의존성을 OrderService + AuthenticationResolver만 남김
- 인증 확인 -> Service 호출 -> HTTP 응답 변환만 담당
- `NoSuchElementException` catch해서 404 반환

### 테스트 수정
- 기존 OrderControllerTest에서 모킹 대상을 Repository/Client에서 OrderService로 변경

---

## 단계 4: MemberController -> MemberService

회원가입/로그인 로직이 Controller에 있어서 Service로 뺀다.

### MemberService 생성
- 의존성: MemberRepository, JwtProvider
- `register(request)`: 이메일 중복 검사 -> 회원 생성 -> JWT 발급 -> TokenResponse 반환
  - 중복이면 `IllegalArgumentException`
- `login(request)`: 이메일로 회원 조회 -> 비밀번호 비교 -> JWT 발급
  - 실패하면 `IllegalArgumentException`

### MemberController 수정
- MemberService만 주입받고 register/login 위임
- `@ExceptionHandler`로 `IllegalArgumentException` -> 400 처리

---

## 단계 5: KakaoAuthController -> KakaoAuthService

OAuth 관련 로직이 전부 Controller에 있어서 Service로 뺀다.

### KakaoAuthService 생성
- 의존성: KakaoLoginProperties, KakaoLoginClient, MemberRepository, JwtProvider
- `buildKakaoAuthUrl()`: 카카오 인가 URL 조립 (client_id, redirect_uri, scope)
- `handleCallback(code)`: 인가 코드 -> 토큰 교환 -> 사용자 정보 조회 -> 회원 등록/갱신 -> JWT 발급
  - 기존 회원이면 카카오 토큰만 갱신, 신규면 이메일로 회원 생성

### KakaoAuthController 수정
- KakaoAuthService만 주입
- login: URL 만들어서 302 리다이렉트
- callback: 코드 받아서 Service 호출, TokenResponse 반환

---

## 단계 6: OptionController -> OptionService

옵션 관련 도메인 규칙이 Controller에 있어서 Service로 뺀다.

### OptionService 생성
- 의존성: OptionRepository, ProductRepository
- `getOptions(productId)`: 상품 존재 확인 후 옵션 목록 조회
- `createOption(productId, request)`: 옵션명 검증(OptionNameValidator) -> 상품 존재 확인 -> 중복 옵션명 검사 -> 저장
  - 검증 실패시 `IllegalArgumentException`
- `deleteOption(productId, optionId)`: 상품 존재 확인 -> 최소 1개 옵션 제약 확인 -> 삭제
  - 옵션 1개뿐이면 `IllegalArgumentException`

### OptionController 수정
- OptionService만 주입
- `NoSuchElementException` -> 404, `IllegalArgumentException` -> 400으로 매핑

---

## 단계 7: WishController -> WishService

위시 관련 도메인 규칙이 Controller에 있어서 Service로 뺀다.

### WishService 생성
- 의존성: WishRepository, ProductRepository
- `getWishes(memberId, pageable)`: 페이징 조회
- `addWish(memberId, request)`: 상품 존재 확인 -> 중복 위시 검사 -> 저장
  - 이미 있으면 기존 위시 반환, 새로 만들면 신규 표시
  - `AddWishResult(wish, created)` 레코드로 구분 -> Controller에서 200/201 분기
- `removeWish(memberId, wishId)`: 위시 존재 확인 -> 소유자 검증 -> 삭제
  - 본인 것 아니면 `IllegalArgumentException` -> Controller에서 403

### WishController 수정
- WishService + AuthenticationResolver만 주입
- 인증 확인 후 Service 위임

---

## 단계 8: ProductController + AdminProductController -> ProductService

상품명 검증이랑 카테고리 검증이 두 컨트롤러에서 중복되고 있어서 Service로 뽑아서 공유한다.

### ProductService 생성
- 의존성: ProductRepository, CategoryRepository
- `getProducts(pageable)`: 페이징 조회
- `getProduct(id)`: 단건 조회, 없으면 `NoSuchElementException`
- `createProduct(name, price, imageUrl, categoryId, allowKakao)`: 상품명 검증 -> 카테고리 조회 -> 저장
- `updateProduct(id, name, price, imageUrl, categoryId, allowKakao)`: 상품명 검증 -> 상품 조회 -> 카테고리 조회 -> 수정
- `deleteProduct(id)`: 삭제
- `getAllProducts()`: Admin 목록용 전체 조회
- `validateProductName(name, allowKakao)`: Admin 폼에서 사전 검증용 (에러 리스트 반환)

핵심: API 컨트롤러는 `allowKakao=false`, Admin 컨트롤러는 `allowKakao=true`로 호출해서 기존 동작 유지.

### ProductController 수정
- ProductService만 주입
- CRUD를 `allowKakao=false`로 위임

### AdminProductController 수정
- ProductService + CategoryRepository 주입 (폼에 카테고리 목록 넣어줘야 하니까)
- CRUD를 `allowKakao=true`로 위임
- 폼 렌더링 로직(populateNewForm, populateEditForm)은 프레젠테이션이니까 Controller에 남김

---

## 단계 9: AdminMemberController -> MemberService 재사용

단계 4에서 만든 MemberService를 Admin에서도 쓴다. 이메일 중복 검사 같은 게 똑같은 로직이었으니까.

### MemberService에 Admin용 메서드 추가
- `getAllMembers()`, `getMember(id)`, `existsByEmail(email)`
- `createMember(email, password)`, `updateMember(id, email, password)`
- `chargePoint(id, amount)`, `deleteMember(id)`

### AdminMemberController 수정
- MemberRepository 직접 주입 -> MemberService 주입으로 변경
- 기존 로직을 전부 Service 메서드로 위임

---

## 단계 10: CategoryController -> CategoryService

비즈니스 로직은 거의 없지만, 프로젝트 전체에서 Controller-Service 패턴을 일관되게 쓰기 위해 추출.

### CategoryService 생성
- 의존성: CategoryRepository
- `getCategories()`: 전체 조회 -> CategoryResponse 변환
- `createCategory(request)`: 저장
- `updateCategory(id, request)`: 조회 -> 수정, 없으면 `NoSuchElementException`
- `deleteCategory(id)`: 삭제

### CategoryController 수정
- CategoryRepository 직접 주입 -> CategoryService 주입으로 변경
- 전부 Service로 위임

---

## 모든 단계 공통

### 제약
- 외부에서 보이는 작동은 절대 안 바꿈
- Controller는 인증 + HTTP 매핑만, Service는 비즈니스 로직만

### 예외 처리 패턴
- Service에서 `NoSuchElementException` -> Controller에서 404
- Service에서 `IllegalArgumentException` -> Controller에서 400 (또는 `@ExceptionHandler`)
- 인증 실패 -> Controller에서 직접 401

### 검증
매 단계마다:
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test
```
