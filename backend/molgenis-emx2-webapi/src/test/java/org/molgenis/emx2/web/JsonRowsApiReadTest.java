package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.FILE;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.ONTOLOGY;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REFBACK;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.TableMetadata.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.TableType;

class JsonRowsApiReadTest extends ApiTestBase {

  private static final String ANIMALS = JsonRowsApiReadTest.class.getSimpleName();
  private static final String PAGES = ANIMALS + "Pages";
  private static final String ROUND_TRIP = ANIMALS + "RoundTrip";
  private static final String ITEM_READER = "jsonrowsread_item";
  private static final String NAME = "name";
  private static final String PET = "Pet";
  private static final String BIG = "Big";
  private static final String ITEM = "Item";
  private static final String EMPTY = "Empty";
  private static final String QUERY = "query";
  private static final String LINK = "Link";
  private static final String PET_SELECTION = "{name,owner{name}}";
  private static final Pattern NEXT_LINK = Pattern.compile("^<(.+)>; rel=\"next\"$");
  private static final Pattern JAVA_CLASS_NAME =
      Pattern.compile("\\b(?:[a-z][a-z0-9]*\\.)+[A-Z]\\w+");
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String ANIMAL_STATE =
      "{Pet(orderby:{name:ASC}){name weight owner{name} tags{name} color{name} photo{id size filename extension} mg_tableclass} Dog{name breed} Bill{name visit{pet{name} day}}}";

  @BeforeAll
  static void setupSchemas() {
    database.becomeAdmin();
    database.setUserPassword(ITEM_READER, ITEM_READER);
    login("admin", "admin");
    createAnimals(ANIMALS);
    createAnimals(ROUND_TRIP);
    createPages();
  }

  private static void createAnimals(String name) {
    database.dropSchemaIfExists(name);
    Schema schema = database.createSchema(name);
    schema.create(
        table("Color").setTableType(TableType.ONTOLOGIES),
        table("Tag", column(NAME).setPkey()),
        table(
            "Owner",
            column(NAME).setPkey(),
            column("pets").setType(REFBACK).setRefTable(PET).setRefBack("owner")),
        table(
            PET,
            column(NAME).setPkey(),
            column("weight").setType(INT),
            column("owner").setType(REF).setRefTable("Owner"),
            column("tags").setType(REF_ARRAY).setRefTable("Tag"),
            column("color").setType(ONTOLOGY).setRefTable("Color"),
            column("photo").setType(FILE)),
        table("Dog", column("breed")).setInheritName(PET),
        table(
            "Visit",
            column("pet").setType(REF).setRefTable(PET).setPkey(),
            column("day").setPkey()),
        table("Bill", column(NAME).setPkey(), column("visit").setType(REF).setRefTable("Visit")));
    schema.getTable("Color").insert(new Row().setString(NAME, "red"));
    schema.getTable("Owner").insert(new Row().setString(NAME, "o1"));
    schema.getTable("Tag").insert(new Row().setString(NAME, "t1"), new Row().setString(NAME, "t2"));
    schema
        .getTable(PET)
        .insert(
            new Row()
                .setString(NAME, "p1")
                .setInt("weight", 1)
                .setString("owner", "o1")
                .setStringArray("tags", "t1", "t2")
                .setString("color", "red")
                .setBinary(
                    "photo", new BinaryFileWrapper("text/plain", "a.txt", "hello".getBytes())),
            new Row().setString(NAME, "p2").setInt("weight", 2));
    schema.getTable("Dog").insert(new Row().setString(NAME, "d1").setString("breed", "lab"));
    graphql(
        name,
        "mutation($v:[VisitInput],$b:[BillInput]){insert(Visit:$v,Bill:$b){message}}",
        Map.of(
            "v",
            List.of(Map.of("pet", Map.of(NAME, "p1"), "day", "mon")),
            "b",
            List.of(Map.of(NAME, "b1", "visit", Map.of("pet", Map.of(NAME, "p1"), "day", "mon")))));
  }

  private static void createPages() {
    database.dropSchemaIfExists(PAGES);
    Schema schema = database.createSchema(PAGES);
    schema.create(
        table(BIG, column(NAME).setPkey()),
        table(EMPTY, column(NAME).setPkey()),
        table(ITEM, column(NAME).setPkey(), column("rank").setType(INT)));
    schema
        .getTable(BIG)
        .insert(
            IntStream.range(0, 1005)
                .mapToObj(i -> new Row().setString(NAME, "r%04d".formatted(i)))
                .toList());
    schema
        .getTable(ITEM)
        .insert(
            IntStream.range(0, 8)
                .mapToObj(i -> new Row().setString(NAME, "i" + i).setInt("rank", i))
                .toList());
    schema.createRole("ItemReader");
    schema.grant("ItemReader", new TablePermission(ITEM).select(true));
    schema.addMember(ITEM_READER, "ItemReader");
  }

  @Test
  void putOfAnExistingSubclassKeyWithoutTheTableClassAnswers400AndStoresNothing() {
    String before = graphql(ROUND_TRIP, ANIMAL_STATE, null);
    Response put = put(ROUND_TRIP, PET, "[{\"name\":\"d1\",\"breed\":\"pug\"}]");
    assertEquals(400, put.statusCode(), put.asString());
    assertEquals(before, graphql(ROUND_TRIP, ANIMAL_STATE, null));
  }

  private static Response put(String schema, String path, String rows) {
    return given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(rows)
        .put("/" + schema + "/api/json/" + path);
  }

  @BeforeEach
  void asAdmin() {
    login("admin", "admin");
  }

  static Stream<Arguments> parameters() {
    return Stream.of(
        Arguments.of(
            Map.of(QUERY, PET_SELECTION, "filter", "{\"name\":{\"equals\":\"p1\"}}"),
            "(filter:{name:{equals:\"p1\"}},orderby:{name:ASC})"),
        Arguments.of(
            Map.of(QUERY, PET_SELECTION, "orderby", "{\"name\":\"DESC\"}"),
            "(orderby:{name:DESC})"),
        Arguments.of(
            Map.of(QUERY, PET_SELECTION, "search", "p2"), "(search:\"p2\",orderby:{name:ASC})"),
        Arguments.of(
            Map.of(
                QUERY, PET_SELECTION, "orderby", "{\"name\":\"ASC\"}", "limit", "2", "offset", "1"),
            "(orderby:{name:ASC},limit:2,offset:1)"),
        Arguments.of(Map.of(QUERY, "{name,tags{name}}"), "(orderby:{name:ASC})"));
  }

  @ParameterizedTest
  @MethodSource("parameters")
  void tableReadEqualsTheGraphqlResultForTheSameArguments(
      Map<String, String> parameters, String graphqlArguments) throws JsonProcessingException {
    Response rest = get(ANIMALS, PET, parameters);
    String selection = parameters.get(QUERY).replace(",", " ");
    JsonNode expected =
        MAPPER
            .readTree(graphql(ANIMALS, "{Pet%s%s}".formatted(graphqlArguments, selection), null))
            .at("/data/Pet");
    assertEquals(200, rest.statusCode(), rest.asString());
    assertFalse(expected.isMissingNode());
    assertEquals(expected, MAPPER.readTree(rest.asString()), parameters.toString());
  }

  @Test
  void dataReadWithQueryEqualsTheGraphqlResult() throws JsonProcessingException {
    String query = "{Pet(orderby:{name:ASC}){name} Pet_agg{count} Owner{name} Owner_agg{count}}";
    Response rest = get(ANIMALS, "_data", Map.of(QUERY, query));
    JsonNode expected = MAPPER.readTree(graphql(ANIMALS, query, null)).at("/data");
    assertEquals(200, rest.statusCode(), rest.asString());
    assertEquals(expected, MAPPER.readTree(rest.asString()));
    assertTrue(expected.at("/Pet_agg/count").asInt() > 0);
  }

  @Test
  void defaultShapeIsTheLiteral() throws JsonProcessingException {
    JsonNode photo =
        MAPPER.readTree(
            graphql(ANIMALS, "{Pet(filter:{name:{equals:\"p1\"}}){photo{id url}}}", null));
    String id = photo.at("/data/Pet/0/photo/id").asText();
    String url = photo.at("/data/Pet/0/photo/url").asText();
    String pets =
        """
        [{"name":"d1","breed":"lab","mg_tableclass":"%1$s.Dog"},
         {"name":"p1","weight":1,"owner":{"name":"o1"},"tags":[{"name":"t1"},{"name":"t2"}],
          "color":{"name":"red"},
          "photo":{"size":5,"id":"%2$s","filename":"a.txt","extension":"txt","url":"%3$s"},
          "mg_tableclass":"%1$s.Pet"},
         {"name":"p2","weight":2,"mg_tableclass":"%1$s.Pet"}]"""
            .formatted(ANIMALS, id, url);
    assertEquals(MAPPER.readTree(pets), body(get(ANIMALS, PET, Map.of())));
    String bills =
        """
        [{"name":"b1","visit":{"pet":{"name":"p1"},"day":"mon"}}]""";
    assertEquals(MAPPER.readTree(bills), body(get(ANIMALS, "Bill", Map.of())));
    String owners =
        """
        [{"name":"o1"}]""";
    assertEquals(MAPPER.readTree(owners), body(get(ANIMALS, "Owner", Map.of())));
    String dogs =
        """
        [{"name":"d1","breed":"lab","mg_tableclass":"%s.Dog"}]"""
            .formatted(ANIMALS);
    assertEquals(MAPPER.readTree(dogs), body(get(ANIMALS, "Dog", Map.of())));
  }

  @Test
  void getThenPutOfTheBodyStoresNoChange() throws JsonProcessingException {
    String before = graphql(ROUND_TRIP, ANIMAL_STATE, null);
    assertTrue(before.contains("\"filename\""), before);
    assertTrue(before.contains("\"breed\""), before);
    Response rows = get(ROUND_TRIP, PET, Map.of());
    Response put =
        given()
            .sessionId(sessionId)
            .contentType("application/json")
            .body(rows.asString())
            .put("/" + ROUND_TRIP + "/api/json/" + PET);
    assertEquals(200, put.statusCode(), put.asString());
    assertEquals(before, graphql(ROUND_TRIP, ANIMAL_STATE, null));
  }

  @Test
  void pageOfAThousandCrossedByTheFollowedLink() {
    Response first = get(PAGES, BIG, Map.of());
    assertEquals(1000, body(first).size());
    String next = nextLink(first);
    assertEquals("/" + PAGES + "/api/json/Big?offset=1000", next);
    Response last = follow(next);
    assertEquals(5, body(last).size());
    assertNull(last.header(LINK));
  }

  @Test
  void followingNextLinksOfASmallLimitReachesEveryRowOnceAndTheLastPageHasNoLink() {
    List<String> seen = new ArrayList<>();
    Response page = get(PAGES, ITEM, Map.of("limit", "3", "orderby", "{\"rank\":\"ASC\"}"));
    int pages = 1;
    while (pages <= 4) {
      body(page).forEach(row -> seen.add(row.get(NAME).asText()));
      if (page.header(LINK) == null) {
        break;
      }
      assertEquals(3, body(page).size());
      page = follow(nextLink(page));
      pages++;
    }
    assertEquals(3, pages);
    assertEquals(List.of("i0", "i1", "i2", "i3", "i4", "i5", "i6", "i7"), seen);
    assertNull(get(PAGES, ITEM, Map.of("limit", "8")).header(LINK));
  }

  @Test
  void dataReadWithoutQueryPagesEachTableAndLinksToItsTableRoute() {
    Response response = get(PAGES, "_data", Map.of());
    JsonNode body = body(response);
    assertEquals(1000, body.get(BIG).size());
    assertEquals(8, body.get(ITEM).size());
    List<String> links = response.headers().getValues(LINK);
    assertEquals(1, links.size(), links.toString());
    Matcher link = NEXT_LINK.matcher(links.get(0));
    assertTrue(link.matches(), links.get(0));
    assertEquals("/" + PAGES + "/api/json/Big?offset=1000", link.group(1));
    List<String> slashLinks = get(PAGES, "_data/", Map.of()).headers().getValues(LINK);
    assertEquals(links, slashLinks);
  }

  @Test
  void tableWithoutRowsReadsAsAnEmptyArray() {
    assertEquals("[]", body(get(PAGES, EMPTY, Map.of())).toString());
    assertEquals("[]", body(get(PAGES, "_data", Map.of())).get(EMPTY).toString());
  }

  @Test
  void dataReadWithoutQueryReturnsOnlyTheTablesTheCallerMayView() {
    login(ITEM_READER, ITEM_READER);
    Response response = get(PAGES, "_data", Map.of());
    assertEquals(200, response.statusCode(), response.asString());
    Set<String> keys = new HashSet<>();
    body(response).fieldNames().forEachRemaining(keys::add);
    assertEquals(Set.of(ITEM), keys);
    assertNull(response.header(LINK));
  }

  @ParameterizedTest
  @MethodSource("failingReads")
  void failedReadAnswers400WithTheFirstErrorsOwnMessage(String path, Map<String, String> params) {
    Response response = get(ANIMALS, path, params);
    assertEquals(400, response.statusCode(), response.asString());
    String message = response.jsonPath().getString("errors[0].message");
    assertFalse(message == null || message.isBlank(), response.asString());
    assertFalse(JAVA_CLASS_NAME.matcher(message).find(), message);
  }

  static Stream<Arguments> failingReads() {
    return Stream.of(
        Arguments.of(PET, Map.of("filter", "{\"nope\":{\"equals\":\"x\"}}")),
        Arguments.of(PET, Map.of(QUERY, "{nope}")),
        Arguments.of(PET, Map.of(QUERY, "{name")),
        Arguments.of(PET, Map.of("filter", "{not json")),
        Arguments.of(PET, Map.of("filter", "{\"name\":{\"equals\":\"p1\"}}}")),
        Arguments.of(PET, Map.of("offset", "99999999999")),
        Arguments.of(PET, Map.of("limit", "many")),
        Arguments.of(PET, Map.of("offset", "-1")),
        Arguments.of("_data", Map.of(QUERY, "{Nope{name}}")),
        Arguments.of("_data", Map.of(QUERY, "{Pet{nope}")));
  }

  @Test
  void offsetBeyondTheIntegerRangeNamesTheRange() {
    Response response = get(ANIMALS, PET, Map.of("offset", "99999999999"));
    assertEquals(400, response.statusCode(), response.asString());
    assertTrue(
        response.jsonPath().getString("errors[0].message").contains("2147483647"),
        response.asString());
  }

  @Test
  void readOfATableTheCallerMayNotViewAnswers400() {
    login(ITEM_READER, ITEM_READER);
    Response response = get(PAGES, BIG, Map.of());
    assertEquals(400, response.statusCode(), response.asString());
    String message = response.jsonPath().getString("errors[0].message");
    assertFalse(JAVA_CLASS_NAME.matcher(message).find(), message);
  }

  @ParameterizedTest
  @MethodSource("mutationsInQuery")
  void mutationInQueryAnswers400AndStoresNothing(String path, String query)
      throws JsonProcessingException {
    String before = graphql(ANIMALS, ANIMAL_STATE, null);
    Response response = get(ANIMALS, path, Map.of(QUERY, query));
    assertEquals(400, response.statusCode(), response.asString());
    assertFalse(
        JAVA_CLASS_NAME.matcher(response.jsonPath().getString("errors[0].message")).find(),
        response.asString());
    assertEquals(before, graphql(ANIMALS, ANIMAL_STATE, null));
  }

  static Stream<Arguments> mutationsInQuery() {
    String insert = "mutation{insert(Pet:[{name:\"injected\"}]){status}}";
    return Stream.of(
        Arguments.of("_data", insert),
        Arguments.of(PET, "{name}} " + insert + " query q{Pet{name}"));
  }

  @Test
  void unknownSchemaOrTableOnAReadGivesTheStatusOfTheCsvRoute() {
    for (String path : List.of("Nonexisting/api/%s/Pet", ANIMALS + "/api/%s/NoSuchTable")) {
      int csv = given().sessionId(sessionId).get("/" + path.formatted("csv")).statusCode();
      Response response = given().sessionId(sessionId).get("/" + path.formatted("json"));
      assertEquals(csv, response.statusCode(), response.asString());
      assertTrue(response.asString().contains("\"errors\""), response.asString());
    }
  }

  private static Response follow(String url) {
    return given().sessionId(sessionId).urlEncodingEnabled(false).get(url);
  }

  private static String nextLink(Response response) {
    Matcher link = NEXT_LINK.matcher(response.header(LINK));
    assertTrue(link.matches(), response.header(LINK));
    return link.group(1);
  }

  private static JsonNode body(Response response) {
    try {
      return MAPPER.readTree(response.asString());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(response.asString(), e);
    }
  }

  private static Response get(String schema, String path, Map<String, String> parameters) {
    return given()
        .sessionId(sessionId)
        .queryParams(parameters)
        .get("/" + schema + "/api/json/" + path);
  }

  private static String graphql(String schema, String query, Map<String, Object> variables) {
    try {
      Map<String, Object> request =
          variables == null ? Map.of(QUERY, query) : Map.of(QUERY, query, "variables", variables);
      return given()
          .sessionId(sessionId)
          .body(MAPPER.writeValueAsString(request))
          .post("/" + schema + "/api/graphql")
          .asString();
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
