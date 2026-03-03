package gift.option;

import gift.category.Category;
import gift.category.CategoryRepository;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OptionAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;
    private Product product;
    private Option defaultOption;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        category = categoryRepository.save(new Category("테스트카테고리", "#000000", "https://example.com/cat.png", ""));
        product = productRepository.save(new Product("테스트상품", 1000, "https://example.com/product.png", category));
        defaultOption = optionRepository.save(new Option(product, "기본옵션", 100));
    }

    @AfterEach
    void tearDown() {
        optionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("상품의 옵션 목록을 조회하면 200과 옵션 목록을 반환한다")
    void getOptions() {
        given()
        .when()
            .get("/api/products/" + product.getId() + "/options")
        .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].name", equalTo("기본옵션"))
            .body("[0].quantity", equalTo(100));
    }

    @Test
    @DisplayName("옵션을 생성하면 201과 생성된 옵션을 반환한다")
    void createOption() {
        createOptionRequest("추가옵션", 50)
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("추가옵션"))
            .body("quantity", equalTo(50));
    }

    @Test
    @DisplayName("중복된 옵션명으로 생성하면 400을 반환한다")
    void createDuplicateOption() {
        createOptionRequest("기본옵션", 50)
            .statusCode(400);
    }

    @Test
    @DisplayName("옵션을 삭제하면 204를 반환한다")
    void deleteOption() {
        var extraOption = optionRepository.save(new Option(product, "삭제용옵션", 10));

        given()
        .when()
            .delete("/api/products/" + product.getId() + "/options/" + extraOption.getId())
        .then()
            .statusCode(204);
    }

    @Test
    @DisplayName("마지막 남은 옵션을 삭제하면 400을 반환한다")
    void deleteLastOption() {
        given()
        .when()
            .delete("/api/products/" + product.getId() + "/options/" + defaultOption.getId())
        .then()
            .statusCode(400);
    }

    private ValidatableResponse createOptionRequest(String name, int quantity) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "quantity", quantity))
        .when()
            .post("/api/products/" + product.getId() + "/options")
        .then();
    }
}
