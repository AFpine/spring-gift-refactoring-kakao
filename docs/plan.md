# 리팩터링 플랜

---

## 1단계: 리팩터링 준비하기 (완료)

### 요약
작동은 유지하면서 구조 변경으로 변경 난이도를 낮추는 작업을 수행했다.

| 단계 | 내용 | 상태 |
|---|---|---|
| 1 | 스타일 정리 (@author, @Autowired, 빈 줄, .toList(), 블록 주석) | 완료 |
| 2 | 불필요한 코드 제거 (OrderController.wishRepository, Collectors import) | 완료 |
| 3 | OrderController → OrderService | 완료 |
| 4 | MemberController → MemberService | 완료 |
| 5 | KakaoAuthController → KakaoAuthService | 완료 |
| 6 | OptionController → OptionService | 완료 |
| 7 | WishController → WishService | 완료 |
| 8 | ProductController + AdminProductController → ProductService | 완료 |
| 9 | AdminMemberController → MemberService 재사용 | 완료 |
| 10 | CategoryController → CategoryService | 완료 |

---

## 2단계: 리팩터링 완성하기

### 목표
작동 변경을 안전하게 수행하고, 그 결과를 증거로 보여준다.

### 요구사항 요약
1. **트랜잭션 경계 세우기** — 여러 저장 작업이 하나의 논리 작업이라면 부분 반영 방지
2. **누락된 작동 구현** — 기존 의도는 있었지만 미구현된 작동 완성 (증거 필수)
3. **도메인 책임 되찾기** — 책임/계산/판단을 올바른 위치로 이동 (최소 2개 이상 개선)

### 제약
- 작동 변경은 반드시 증거(테스트/상태 재조회)와 함께
- 구조 변경 커밋과 작동 변경 커밋 분리
- ADR은 필요 시 docs/판단근거.md에 기록

### 단계별 계획

| 단계 | 내용 | 유형 | 상태 |
|---|---|---|---|
| 1-1 | 카카오 알림을 createOrder에서 분리 | 구조 변경 | 대기 |
| 1-2 | @Transactional 추가 + 롤백 증거 테스트 | 작동 변경 | 대기 |
| 2 | wish cleanup 구현 (주문 시 위시 삭제) | 작동 변경 | 대기 |
| 3 | orElse(null)→예외 전환 + 에러 처리 (500→400) | 작동 변경 | 대기 |
| 4 | 가격 계산 로직을 Option 도메인으로 이동 | 구조 변경 | 대기 |
