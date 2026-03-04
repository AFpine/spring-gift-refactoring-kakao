# spring-gift-refactoring

## 1단계: 리팩터링 준비하기

### 기능 목록

#### 스타일 정리
- [x] `@author/@since` Javadoc 제거 — 일부 파일에만 있어서 통일
- [x] 불필요한 `@Autowired` 제거 — 생성자 1개면 필요 없음
- [x] 클래스 설명 블록 주석 제거 — 대부분 파일에 없으므로 통일
- [x] `Member.java` 필드 사이 빈 줄 제거 — 다른 엔티티와 통일
- [x] `Collectors.toList()` -> `.toList()` 변경

#### 불필요한 코드 제거
- [x] `OrderController.wishRepository` 제거 — 주입만 되고 쓰이지 않음
- [x] `OptionController`의 `Collectors` import 제거 — 위 작업에서 같이 처리

#### 서비스 계층 추출
- [x] `OrderController` -> `OrderService`
- [x] `MemberController` -> `MemberService`
- [x] `KakaoAuthController` -> `KakaoAuthService`
- [x] `OptionController` -> `OptionService`
- [x] `WishController` -> `WishService`
- [x] `ProductController` -> `ProductService`
- [x] `AdminProductController` -> `ProductService` 재사용
- [x] `AdminMemberController` -> `MemberService` 재사용
- [x] `CategoryController` -> `CategoryService`

### 구현 전략
1. 스타일 -> 불필요 코드 -> 서비스 추출 순서로 진행
2. 스타일/불필요 코드를 먼저 정리해야 서비스 추출할 때 diff가 깨끗함
3. 서비스 추출은 비즈니스 로직이 복잡한 컨트롤러부터 처리
4. 한 번에 하나의 컨트롤러만 바꾸고, 매번 전체 테스트 돌려서 확인
5. 구조만 바꾸고 작동은 절대 안 바꿈

### AI 활용 기록
- Claude Code로 프로젝트 전체 분석 (스타일 불일치, 미사용 코드, 비즈니스 로직 위치 탐색)
- 계획 문서 초안 작성에 활용
- 서비스 추출 코드 생성 후 직접 검토/수정
- 매 단계마다 전체 테스트 통과 확인

---

## 2단계: 리팩터링 완성하기

### 기능 목록

#### 트랜잭션 경계 세우기
- [x] 카카오 알림을 createOrder에서 분리 (구조 변경)
- [x] OrderService.createOrder()에 @Transactional 추가 + 롤백 증거

#### 누락된 작동 구현
- [ ] 주문 완료 시 wish cleanup 구현

#### 도메인 책임 되찾기
- [ ] orElse(null) → 예외 전환 + 에러 처리 개선 (500→400)
- [ ] 가격 계산 로직을 Option 도메인으로 이동

### 구현 전략
1. 트랜잭션 경계 → 누락 작동 → 도메인 책임 순서로 진행
2. 트랜잭션 경계를 먼저 세워야 이후 wish cleanup이 트랜잭션 안에 포함됨
3. 구조 변경 커밋과 작동 변경 커밋을 분리
4. 매 단계마다 전체 테스트 통과 확인
5. 작동 변경은 반드시 테스트로 증명

### AI 활용 기록
> 작업 진행하며 기록
