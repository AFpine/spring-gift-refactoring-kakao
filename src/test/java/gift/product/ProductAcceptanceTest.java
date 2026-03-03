package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ProductAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        category = categoryRepository.save(new Category("테스트카테고리", "#000000", "https://example.com/cat.png", ""));
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("상품을 생성하면 201과 생성된 상품을 반환한다")
    void createProduct() {
        createProductRequest("아메리카노", 4500, "https://example.com/coffee.jpg")
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("아메리카노"))
            .body("price", equalTo(4500))
            .body("imageUrl", equalTo("https://example.com/coffee.jpg"))
            .body("categoryId", equalTo(category.getId().intValue()));
    }

    @Test
    @DisplayName("상품 목록을 조회하면 200과 페이징된 결과를 반환한다")
    void getProducts() {
        createProductRequest("아메리카노", 4500, "https://example.com/americano.jpg").statusCode(201);
        createProductRequest("카페라떼", 5000, "https://example.com/latte.jpg").statusCode(201);

        given()
        .when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("totalElements", equalTo(2))
            .body("content.name", hasItem("아메리카노"))
            .body("content.name", hasItem("카페라떼"));
    }

    @Test
    @DisplayName("상품을 단건 조회하면 200과 해당 상품을 반환한다")
    void getProduct() {
        long id = createProductAndGetId("아메리카노", 4500, "https://example.com/coffee.jpg");

        given()
        .when()
            .get("/api/products/" + id)
        .then()
            .statusCode(200)
            .body("name", equalTo("아메리카노"))
            .body("price", equalTo(4500));
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 404를 반환한다")
    void getNonExistentProduct() {
        given()
        .when()
            .get("/api/products/99999")
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("상품을 수정하면 200과 수정된 상품을 반환한다")
    void updateProduct() {
        long id = createProductAndGetId("아메리카노", 4500, "https://example.com/coffee.jpg");

        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "카페라떼",
                "price", 5500,
                "imageUrl", "https://example.com/latte.jpg",
                "categoryId", category.getId()
            ))
        .when()
            .put("/api/products/" + id)
        .then()
            .statusCode(200)
            .body("name", equalTo("카페라떼"))
            .body("price", equalTo(5500));
    }

    @Test
    @DisplayName("상품을 삭제하면 204를 반환한다")
    void deleteProduct() {
        long id = createProductAndGetId("삭제용상품", 1000, "https://example.com/del.jpg");

        given()
        .when()
            .delete("/api/products/" + id)
        .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("15자 초과 이름으로 상품을 생성하면 400을 반환한다")
    void createProductWithInvalidName() {
        createProductRequest("이름이열다섯자를초과하는상품이름입니다", 1000, "https://example.com/x.jpg")
            .statusCode(400);
    }

    @Test
    @DisplayName("카카오가 포함된 이름으로 상품을 생성하면 400을 반환한다")
    void createProductWithKakaoName() {
        createProductRequest("카카오 상품", 1000, "https://example.com/x.jpg")
            .statusCode(400);
    }

    private long createProductAndGetId(String name, int price, String imageUrl) {
        return createProductRequest(name, price, imageUrl)
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    private ValidatableResponse createProductRequest(String name, int price, String imageUrl) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", name,
                "price", price,
                "imageUrl", imageUrl,
                "categoryId", category.getId()
            ))
        .when()
            .post("/api/products")
        .then();
    }
}
