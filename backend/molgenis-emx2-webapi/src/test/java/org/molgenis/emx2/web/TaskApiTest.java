package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.http.Method;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TaskApiTest extends ApiTestBase {

  @BeforeAll
  static void addTestUser() {
    database.addUser("task_api_test_user");
    database.setUserPassword("task_api_test_user", "helloworld");
  }

  @AfterAll
  static void removeTestUser() {
    database.removeUser("task_api_test_user");
  }

  private static Stream<Arguments> unauthorizedPaths() {
    return Stream.of(
        Arguments.of(Method.GET, "/api/tasks", "Unable to list tasks: can only be done by admin"),
        Arguments.of(
            Method.GET, "/api/tasks/clear", "Unable to clear tasks: can only be done by admin"),
        Arguments.of(
            Method.GET,
            "/api/tasks/scheduled",
            "Unable to view scheduled tasks: can only be done by admin"),
        Arguments.of(
            Method.GET, "/api/scripts/name", "Retrieve task failed: can only be done by admin"),
        Arguments.of(
            Method.POST, "/api/scripts/name", "Submit task failed: can only be done by admin"),
        Arguments.of(Method.GET, "/api/tasks/123", "Unable to get task: can only be done by admin"),
        Arguments.of(
            Method.GET,
            "/api/tasks/123/output",
            "Retrieve task output failed: can only be done by admin"),
        Arguments.of(
            Method.DELETE, "/api/tasks/123", "Unable to delete task: can only be done by admin"),
        Arguments.of(
            Method.GET,
            "/api/tasks/123/delete",
            "Unable to delete task: can only be done by admin"),
        Arguments.of(
            Method.GET, "/my-schema/api/tasks", "Unable to list tasks: can only be done by admin"),
        Arguments.of(
            Method.GET,
            "/my-schema/api/tasks/clear",
            "Unable to clear tasks: can only be done by admin"),
        Arguments.of(
            Method.GET,
            "/my-schema/api/tasks/123",
            "Unable to get task: can only be done by admin"),
        Arguments.of(
            Method.GET,
            "/my-schema/api/scripts/script-name",
            "Retrieve task failed: can only be done by admin"),
        Arguments.of(
            Method.POST,
            "/my-schema/api/scripts/script-name",
            "Submit task failed: can only be done by admin"),
        Arguments.of(
            Method.GET,
            "/my-schema/api/tasks/123/output",
            "Retrieve task output failed: can only be done by admin"),
        Arguments.of(
            Method.DELETE,
            "/my-schema/my-app/api/tasks/123",
            "Unable to delete task: can only be done by admin"),
        Arguments.of(
            Method.GET,
            "/my-schema/my-app/api/tasks/123/delete",
            "Unable to delete task: can only be done by admin"),
        Arguments.of(
            Method.GET,
            "/my-schema/my-app/api/tasks",
            "Unable to list tasks: can only be done by admin"),
        Arguments.of(
            Method.GET,
            "/my-schema/my-app/api/tasks/clear",
            "Unable to clear tasks: can only be done by admin"),
        Arguments.of(
            Method.GET,
            "/my-schema/my-app/api/tasks/123",
            "Unable to get task: can only be done by admin"));
  }

  @ParameterizedTest
  @MethodSource("unauthorizedPaths")
  void givenSession_whenNonAdmin_thenUnauthorized(
      Method method, String path, String expectedMessage) {
    login("task_api_test_user", "helloworld");
    String message =
        given()
            .sessionId(sessionId)
            .request(method, path)
            .then()
            .statusCode(400)
            .extract()
            .asString();
    assertTrue(
        message.contains(expectedMessage),
        "Unexpected message: " + message + ", for path: " + path);
  }

  @ParameterizedTest
  @MethodSource("unauthorizedPaths")
  void givenNoSession_thenUnauthorized(Method method, String path, String expectedMessage) {
    String message = given().request(method, path).then().statusCode(400).extract().asString();
    assertTrue(
        message.contains(expectedMessage),
        "Unexpected message: " + message + ", for path: " + path);
  }
}
