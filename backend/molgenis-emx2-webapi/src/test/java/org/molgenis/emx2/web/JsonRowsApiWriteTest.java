package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.FILE;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.TableMetadata.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;

class JsonRowsApiWriteTest extends ApiTestBase {

  private static final String REST = JsonRowsApiWriteTest.class.getSimpleName() + "Rest";
  private static final String GQL = JsonRowsApiWriteTest.class.getSimpleName() + "Gql";
  private static final String OTHER = JsonRowsApiWriteTest.class.getSimpleName() + "Other";
  private static final String VIEWER = "jsonrowsapi_viewer";
  private static final String APPLICATION_JSON = "application/json";
  private static final String PET = "Pet";
  private static final String PET_OWNER = "Pet Owner";
  private static final String DOG = "Dog";
  private static final String NAME = "name";
  private static final String FILE_PET = "pf";
  private static final String PET_STATE =
      "{Pet(orderby:{name:ASC}){name weight owner{name} tags{name}} PetOwner(orderby:{name:ASC}){name} Tag(orderby:{name:ASC}){name} Dog{name breed}}";
  private static final String PHOTO_STATE =
      "{Pet(filter:{name:{equals:\"pf\"}}){name photo{id size filename extension url}}}";
  private static final Pattern JAVA_CLASS_NAME =
      Pattern.compile("\\b(?:[a-z][a-z0-9]*\\.)+[A-Z]\\w+");
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @BeforeAll
  static void setupSchemas() {
    database.becomeAdmin();
    database.setUserPassword(VIEWER, VIEWER);
    for (String name : List.of(REST, GQL, OTHER)) {
      database.dropSchemaIfExists(name);
      Schema schema = database.createSchema(name);
      schema.create(
          table("Tag", column(NAME).setPkey()),
          table(PET_OWNER, column(NAME).setPkey()),
          table(
              PET,
              column(NAME).setPkey(),
              column("weight").setType(INT),
              column("owner").setType(REF).setRefTable(PET_OWNER),
              column("tags").setType(REF_ARRAY).setRefTable("Tag"),
              column("photo").setType(FILE)),
          table(DOG, column("breed")).setInheritName(PET));
      schema
          .getTable(PET_OWNER)
          .insert(new Row().setString(NAME, "o1"), new Row().setString(NAME, "o2"));
      schema
          .getTable("Tag")
          .insert(new Row().setString(NAME, "t1"), new Row().setString(NAME, "t2"));
      schema
          .getTable(PET)
          .insert(
              new Row()
                  .setString(NAME, FILE_PET)
                  .setBinary(
                      "photo", new BinaryFileWrapper("text/plain", "a.txt", "hello".getBytes())));
      schema.addMember(VIEWER, Privileges.VIEWER.toString());
    }
    database
        .getSchema(OTHER)
        .getTable(PET)
        .insert(
            new Row()
                .setString(NAME, "pk")
                .setBinary(
                    "photo", new BinaryFileWrapper("text/plain", "a.txt", "hello".getBytes())));
  }

  @BeforeEach
  void asAdmin() {
    login("admin", "admin");
  }

  @Test
  void everyVerbStoresAsTheGraphqlMutationDoes() throws Exception {
    step(
        "POST",
        PET,
        "insert",
        "Pet",
        false,
        """
        [{"name":"v1","weight":1,"owner":{"name":"o1"},"tags":[{"name":"t1"}]},{"name":"v2","weight":2}]""");
    step(
        "POST",
        PET,
        "insert",
        "Pet",
        false,
        """
        [{"name":"v1","weight":5}]""");
    step(
        "PUT",
        PET,
        "save",
        "Pet",
        false,
        """
        [{"name":"v1","weight":6},{"name":"v3","weight":3,"tags":[{"name":"t1"},{"name":"t2"}]}]""");
    step(
        "PATCH",
        PET,
        "update",
        "Pet",
        false,
        """
        [{"name":"v2","weight":20}]""");
    step(
        "PATCH",
        PET,
        "update",
        "Pet",
        false,
        """
        [{"name":"v9","weight":9}]""");
    step(
        "POST",
        PET_OWNER,
        "insert",
        "PetOwner",
        false,
        """
        [{"name":"o3"}]""");
    step(
        "POST",
        DOG,
        "insert",
        "Dog",
        false,
        """
        [{"name":"vd","breed":"lab"}]""");
    step(
        "PUT",
        DOG,
        "save",
        "Dog",
        false,
        """
        [{"name":"vd","breed":"pug"}]""");
    step(
        "DELETE",
        PET,
        "delete",
        "Pet",
        false,
        """
        [{"name":"v1"}]""");
    step(
        "DELETE",
        PET,
        "delete",
        "Pet",
        false,
        """
        [{"name":"v9"}]""");
    step(
        "DELETE",
        PET,
        "delete",
        "Pet",
        true,
        """
        [{"name":"v9"}]""");
    step(
        "DELETE",
        DOG,
        "delete",
        "Dog",
        false,
        """
        [{"name":"vd"}]""");
  }

  private void step(
      String method, String table, String operation, String argument, boolean strict, String rows)
      throws JsonProcessingException {
    String label = method + " " + table + " " + rows + " strict=" + strict;
    Response rest = rest(method, REST, table, rows, strict);
    String mutation =
        "mutation($rows:[%sInput]){%s(%s:$rows%s){status message}}"
            .formatted(argument, operation, argument, strict ? ",strict:true" : "");
    Response graphql = graphql(GQL, mutation, Map.of("rows", MAPPER.readTree(rows)));
    assertSameAnswer(graphql, rest, operation, label);
    assertEquals(
        graphql(GQL, PET_STATE, null).asString(), graphql(REST, PET_STATE, null).asString(), label);
  }

  @Test
  void dataRouteStoresReferringTableListedBeforeTheTableItRefersTo() {
    Response response =
        rest(
            "POST",
            OTHER,
            "_data",
            """
            {"Pet":[{"name":"dp1","owner":{"name":"do1"},"tags":[{"name":"dt1"}]}],
             "PetOwner":[{"name":"do1"}],"Tag":[{"name":"dt1"}]}""",
            false);
    assertEquals(200, response.statusCode(), response.asString());
    assertEquals("SUCCESS", response.jsonPath().getString("status"));
    String state = graphql(OTHER, PET_STATE, null).asString();
    assertTrue(state.contains("dp1") && state.contains("do1") && state.contains("dt1"), state);
  }

  @Test
  void dataRouteStoresNothingWhenTheLastTableHasAnInvalidRow() {
    String before = graphql(OTHER, PET_STATE, null).asString();
    Response response =
        rest(
            "POST",
            OTHER,
            "_data",
            """
            {"PetOwner":[{"name":"dz1"}],
             "Pet":[{"name":"dzp","owner":{"name":"dz1"}},{"name":"dzq","owner":{"name":"nobody"}}]}""",
            false);
    assertCleanFailure(response);
    assertEquals(before, graphql(OTHER, PET_STATE, null).asString());
  }

  @Test
  void invalidRowInTheMiddleOfABatchStoresNothing() throws Exception {
    String rows =
        """
        [{"name":"m1"},{"name":"m2","owner":{"name":"nobody"}},{"name":"m3"}]""";
    assertFailureLikeGraphql("PUT", rows, "save");
  }

  @Test
  void columnTheTableDoesNotHaveStoresNothing() throws Exception {
    assertFailureLikeGraphql(
        "POST",
        """
        [{"name":"c1","nope":1}]""",
        "insert");
  }

  @Test
  void writeByViewerStoresNothing() throws Exception {
    login(VIEWER, VIEWER);
    assertFailureLikeGraphql(
        "POST",
        """
        [{"name":"w1"}]""",
        "insert");
  }

  private void assertFailureLikeGraphql(String method, String rows, String operation)
      throws JsonProcessingException {
    String before = graphql(OTHER, PET_STATE, null).asString();
    Response rest = rest(method, OTHER, PET, rows, false);
    Response graphql =
        graphql(
            GQL,
            "mutation($rows:[PetInput]){%s(Pet:$rows){status message}}".formatted(operation),
            Map.of("rows", MAPPER.readTree(rows)));
    assertCleanFailure(rest);
    assertSameAnswer(graphql, rest, operation, rows);
    assertEquals(before, graphql(OTHER, PET_STATE, null).asString());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          POST   | Pet    | {"name":"b1"}                                                    | must be a JSON array
          POST   | Pet    | "text"                                                           | must be a JSON array
          POST   | Pet    | null                                                             | must be a JSON array
          POST   | _data  | [{"name":"b2"}]                                                  | must be a JSON object
          POST   | _data  | {"Pet":{"name":"b3"}}                                            | must be a JSON array
          POST   | _data  | {"Pet":[{"name":"b4"}],"Tag":"t"}                                | must be a JSON array
          POST   | _data  | {"Pet":null}                                                     | must be a JSON array
          POST   | _data  | {"Pet Owner":[{"name":"dupA"}],"PetOwner":[{"name":"dupB"}]}     | PetOwner
          POST   | _data  | {}                                                               | None or invalid tables
          PUT    | _data  | {}                                                               | None or invalid tables
          PATCH  | _data  | {}                                                               | None or invalid tables
          """)
  void bodyOfTheWrongShapeStoresNothing(
      String method, String path, String body, String expectedMessage) {
    String before = graphql(OTHER, PET_STATE, null).asString();
    Response response = rest(method, OTHER, path, body, false);
    assertCleanFailure(response);
    assertTrue(
        response.jsonPath().getString("errors[0].message").contains(expectedMessage),
        response.asString());
    assertEquals(before, graphql(OTHER, PET_STATE, null).asString());
  }

  @Test
  void fileValueThatIsAStringIsRefusedAndKeepsTheStoredFile() {
    String before = graphql(OTHER, PHOTO_STATE, null).asString();
    Response response = rest("PUT", OTHER, PET, "[{\"name\":\"pf\",\"photo\":\"hello\"}]", false);
    assertCleanFailure(response);
    assertTrue(
        response.jsonPath().getString("errors[0].message").contains("cannot take file content"),
        response.asString());
    assertEquals(before, graphql(OTHER, PHOTO_STATE, null).asString());
  }

  @Test
  void putOfAFileObjectKeepsTheStoredFile() throws Exception {
    String rows = "[{\"name\":\"pk\",\"photo\":" + photoOf(OTHER, "pk") + "}]";
    assertEquals(200, rest("PUT", OTHER, PET, rows, false).statusCode());
    JsonNode photo = photoOf(OTHER, "pk");
    assertEquals(5, photo.path("size").asInt());
    assertEquals(
        "hello", given().sessionId(sessionId).when().get(photo.path("url").asText()).asString());
  }

  @Test
  void putOfNullRemovesTheStoredFileAsGraphqlDoes() throws Exception {
    String rows = "[{\"name\":\"pf\",\"photo\":null}]";
    Response rest = rest("PUT", REST, PET, rows, false);
    Response graphql =
        graphql(
            GQL,
            "mutation($rows:[PetInput]){save(Pet:$rows){status message}}",
            Map.of("rows", MAPPER.readTree(rows)));
    assertSameAnswer(graphql, rest, "save", rows);
    assertTrue(photoOf(REST, "pf").isMissingNode());
    assertTrue(photoOf(GQL, "pf").isMissingNode());
  }

  private static JsonNode photoOf(String schema, String pet) throws JsonProcessingException {
    String query =
        "{Pet(filter:{name:{equals:\"%s\"}}){photo{id size filename extension url}}}"
            .formatted(pet);
    return MAPPER.readTree(graphql(schema, query, null).asString()).at("/data/Pet/0/photo");
  }

  @Test
  void unknownSchemaOrTableGivesTheStatusOfTheCsvRoute() {
    for (String path : List.of("Nonexisting/api/%s/Pet", OTHER + "/api/%s/NoSuchTable")) {
      int csv = given().sessionId(sessionId).when().get("/" + path.formatted("csv")).statusCode();
      Response response =
          given()
              .sessionId(sessionId)
              .contentType(APPLICATION_JSON)
              .body("[]")
              .post("/" + path.formatted("json"));
      assertEquals(csv, response.statusCode(), response.asString());
      assertTrue(response.asString().contains("\"errors\""), response.asString());
    }
  }

  @Test
  void schemaPathAnswersAsTheRootDoes() {
    String root = "/" + REST + "/api/json";
    for (String method : List.of("GET", "POST", "DELETE")) {
      String body = "GET".equals(method) ? "" : "{\"tables\":[]}";
      Response atRoot = send(method, root, body);
      Response atSchema = send(method, root + "/_schema", body);
      assertEquals(atRoot.statusCode(), atSchema.statusCode(), method);
      assertEquals(atRoot.asString(), atSchema.asString(), method);
      assertEquals(200, atSchema.statusCode(), method + atSchema.asString());
    }
  }

  private static Response send(String method, String path, String body) {
    return given()
        .sessionId(sessionId)
        .contentType(APPLICATION_JSON)
        .body(body)
        .request(method, path);
  }

  private static Response rest(
      String method, String schema, String path, String body, boolean strict) {
    String url = "/" + schema + "/api/json/" + path + (strict ? "?strict=true" : "");
    return send(method, url, body);
  }

  private static Response graphql(String schema, String query, Map<String, Object> variables) {
    try {
      Map<String, Object> request =
          variables == null
              ? Map.of("query", query)
              : Map.of("query", query, "variables", variables);
      return given()
          .sessionId(sessionId)
          .body(MAPPER.writeValueAsString(request))
          .post("/" + schema + "/api/graphql");
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void assertSameAnswer(
      Response graphql, Response rest, String operation, String label)
      throws JsonProcessingException {
    assertEquals(graphql.statusCode(), rest.statusCode(), label);
    JsonNode answer = MAPPER.readTree(graphql.asString());
    JsonNode expected = graphql.statusCode() == 200 ? answer.at("/data/" + operation) : answer;
    assertEquals(expected, MAPPER.readTree(rest.asString()), label);
  }

  private static void assertCleanFailure(Response response) {
    assertEquals(400, response.statusCode(), response.asString());
    String message = response.jsonPath().getString("errors[0].message");
    assertFalse(message == null || message.isBlank(), response.asString());
    assertFalse(JAVA_CLASS_NAME.matcher(message).find(), message);
  }
}
