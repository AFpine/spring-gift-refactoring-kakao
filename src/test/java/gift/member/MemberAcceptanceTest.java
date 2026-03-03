
package gift.member;

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
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MemberAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    private Member existingMember;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        existingMember = memberRepository.save(new Member("existing@test.com", "password123"));
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("회원가입에 성공하면 201과 토큰을 반환한다")
    void registerSuccessfully() {
        registerMember("newuser@test.com", "password123")
            .statusCode(201)
            .body("token", notNullValue());
    }

    @Test
    @DisplayName("이미 등록된 이메일로 회원가입하면 400을 반환한다")
    void registerDuplicateEmail() {
        registerMember("existing@test.com", "password123")
            .statusCode(400);
    }

    @Test
    @DisplayName("로그인에 성공하면 200과 토큰을 반환한다")
    void loginSuccessfully() {
        loginMember("existing@test.com", "password123")
            .statusCode(200)
            .body("token", notNullValue());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인하면 400을 반환한다")
    void loginWithWrongPassword() {
        loginMember("existing@test.com", "wrongpassword")
            .statusCode(400);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 400을 반환한다")
    void loginWithNonExistentEmail() {
        loginMember("nobody@test.com", "password123")
            .statusCode(400);
    }

    private ValidatableResponse registerMember(String email, String password) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", password))
        .when()
            .post("/api/members/register")
        .then();
    }

    private ValidatableResponse loginMember(String email, String password) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", password))
        .when()
            .post("/api/members/login")
        .then();
    }
}
