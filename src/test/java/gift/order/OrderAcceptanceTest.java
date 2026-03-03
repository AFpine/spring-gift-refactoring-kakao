package gift.order;

import gift.auth.JwtProvider;
import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OrderAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private Member member;
    private Option option;
    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        var category = categoryRepository.save(new Category("교환권", "#ffffff", "https://example.com/cat.png", ""));
        var product = productRepository.save(new Product("아메리카노", 5000, "https://example.com/coffee.jpg", category));
        option = optionRepository.save(new Option(product, "Tall", 100));

        member = new Member("test@test.com", "password");
        member.chargePoint(100000);
        member = memberRepository.save(member);

        token = jwtProvider.createToken(member.getEmail());
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAllInBatch();
        optionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("주문을 성공적으로 생성하면 201과 주문 정보를 반환한다")
    void createOrder() {
        createOrderRequest(token, option.getId(), 2, "생일 축하해!")
            .statusCode(201)
            .body("id", notNullValue())
            .body("optionId", equalTo(option.getId().intValue()))
            .body("quantity", equalTo(2))
            .body("message", equalTo("생일 축하해!"));
    }

    @Test
    @DisplayName("인증된 사용자의 주문 목록을 조회하면 200을 반환한다")
    void getOrders() {
        createOrderRequest(token, option.getId(), 1, "선물").statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/orders")
        .then()
            .statusCode(200)
            .body("totalElements", equalTo(1))
            .body("content[0].quantity", equalTo(1))
            .body("content[0].message", equalTo("선물"));
    }

    @Test
    @DisplayName("잘못된 토큰으로 주문하면 401을 반환한다")
    void createOrderUnauthorized() {
        createOrderRequest("invalid-token", option.getId(), 1, "선물")
            .statusCode(401);
    }

    @Test
    @DisplayName("존재하지 않는 옵션으로 주문하면 404를 반환한다")
    void createOrderWithInvalidOption() {
        createOrderRequest(token, 99999L, 1, "선물")
            .statusCode(404);
    }

    @Test
    @DisplayName("optionId가 없으면 400을 반환한다")
    void createOrderWithNullOptionId() {
        given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of("quantity", 1, "message", "선물"))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("수량이 0이면 400을 반환한다")
    void createOrderWithZeroQuantity() {
        createOrderRequest(token, option.getId(), 0, "선물")
            .statusCode(400);
    }

    private ValidatableResponse createOrderRequest(String authToken, Long optionId, int quantity, String message) {
        return given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "optionId", optionId,
                "quantity", quantity,
                "message", message
            ))
        .when()
            .post("/api/orders")
        .then();
    }
}
