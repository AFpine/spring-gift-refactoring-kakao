package gift.wish;

import gift.auth.JwtProvider;
import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
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
class WishAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private Member member;
    private Member otherMember;
    private Product product;
    private String token;
    private String otherToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;

        var category = categoryRepository.save(new Category("테스트카테고리", "#000000", "https://example.com/cat.png", ""));
        product = productRepository.save(new Product("테스트상품", 1000, "https://example.com/product.png", category));

        member = memberRepository.save(new Member("wish@test.com", "password"));
        otherMember = memberRepository.save(new Member("other@test.com", "password"));

        token = jwtProvider.createToken(member.getEmail());
        otherToken = jwtProvider.createToken(otherMember.getEmail());
    }

    @AfterEach
    void tearDown() {
        wishRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("위시를 추가하면 201과 위시 정보를 반환한다")
    void addWish() {
        addWishRequest(token, product.getId())
            .statusCode(201)
            .body("id", notNullValue())
            .body("productId", equalTo(product.getId().intValue()))
            .body("name", equalTo("테스트상품"))
            .body("price", equalTo(1000));
    }

    @Test
    @DisplayName("이미 추가된 상품을 다시 위시하면 200과 기존 위시를 반환한다")
    void addDuplicateWish() {
        addWishRequest(token, product.getId()).statusCode(201);

        addWishRequest(token, product.getId())
            .statusCode(200)
            .body("productId", equalTo(product.getId().intValue()));
    }

    @Test
    @DisplayName("위시 목록을 조회하면 200과 페이징된 결과를 반환한다")
    void getWishes() {
        addWishRequest(token, product.getId()).statusCode(201);

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/wishes")
        .then()
            .statusCode(200)
            .body("totalElements", equalTo(1))
            .body("content[0].productId", equalTo(product.getId().intValue()))
            .body("content[0].name", equalTo("테스트상품"));
    }

    @Test
    @DisplayName("위시를 삭제하면 204를 반환한다")
    void removeWish() {
        long wishId = addWishRequest(token, product.getId())
            .statusCode(201)
            .extract().jsonPath().getLong("id");

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/api/wishes/" + wishId)
        .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("인증 없이 위시를 추가하면 401을 반환한다")
    void addWishUnauthorized() {
        addWishRequest("invalid-token", product.getId())
            .statusCode(401);
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 위시를 추가하면 404를 반환한다")
    void addWishWithNonExistentProduct() {
        addWishRequest(token, 99999L)
            .statusCode(404);
    }

    @Test
    @DisplayName("타인의 위시를 삭제하면 403을 반환한다")
    void removeOtherMembersWish() {
        long wishId = addWishRequest(token, product.getId())
            .statusCode(201)
            .extract().jsonPath().getLong("id");

        given()
            .header("Authorization", "Bearer " + otherToken)
        .when()
            .delete("/api/wishes/" + wishId)
        .then()
            .statusCode(403);
    }

    private ValidatableResponse addWishRequest(String authToken, Long productId) {
        return given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body(Map.of("productId", productId))
        .when()
            .post("/api/wishes")
        .then();
    }
}
