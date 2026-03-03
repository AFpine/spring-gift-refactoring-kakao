package gift.category;

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
class CategoryAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        category = categoryRepository.save(new Category("테스트카테고리", "#000000", "https://example.com/test.png", "설명"));
    }

    @AfterEach
    void tearDown() {
        categoryRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("카테고리를 생성하면 201과 생성된 카테고리를 반환한다")
    void createCategory() {
        createCategoryRequest("새카테고리", "#FF0000", "https://example.com/new.png", "새 설명")
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("새카테고리"))
            .body("color", equalTo("#FF0000"))
            .body("imageUrl", equalTo("https://example.com/new.png"))
            .body("description", equalTo("새 설명"));
    }

    @Test
    @DisplayName("카테고리 목록을 조회하면 200과 전체 카테고리를 반환한다")
    void getCategories() {
        given()
        .when()
            .get("/api/categories")
        .then()
            .statusCode(200)
            .body("name", hasItem("테스트카테고리"));
    }

    @Test
    @DisplayName("카테고리를 수정하면 200과 수정된 카테고리를 반환한다")
    void updateCategory() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "수정된카테고리",
                "color", "#FFFFFF",
                "imageUrl", "https://example.com/updated.png",
                "description", "수정된 설명"
            ))
        .when()
            .put("/api/categories/" + category.getId())
        .then()
            .statusCode(200)
            .body("name", equalTo("수정된카테고리"))
            .body("color", equalTo("#FFFFFF"));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리를 수정하면 404를 반환한다")
    void updateNonExistentCategory() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", "수정",
                "color", "#FFFFFF",
                "imageUrl", "https://example.com/x.png",
                "description", ""
            ))
        .when()
            .put("/api/categories/99999")
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("카테고리를 삭제하면 204를 반환한다")
    void deleteCategory() {
        given()
        .when()
            .delete("/api/categories/" + category.getId())
        .then()
            .statusCode(204);
    }

    private ValidatableResponse createCategoryRequest(String name, String color, String imageUrl, String description) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "name", name,
                "color", color,
                "imageUrl", imageUrl,
                "description", description
            ))
        .when()
            .post("/api/categories")
        .then();
    }
}
