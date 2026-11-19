package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.MG_DRAFT;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.web.Constants.ACCEPT_PARQUET;

import io.restassured.response.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.tablestore.TableStoreForParquet;

class ParquetApiTest extends ApiTestBase {

  private static final String SCHEMA_NAME = ParquetApiTest.class.getSimpleName();
  private static final String TABLE_NAME = "Animals";
  private Schema schema;

  @TempDir Path downloadDir;

  @BeforeEach
  void setupSchema() {
    schema = database.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME, column("name", STRING).setPkey(), column("legs", INT)));
    schema
        .getTable(TABLE_NAME)
        .insert(row("name", "cat", "legs", 4), row("name", "bird", "legs", 2));

    login("admin", "admin");
  }

  @Test
  void shouldDownloadTableAsParquet() throws IOException {
    Response response = get("/" + SCHEMA_NAME + "/api/parquet/" + TABLE_NAME);

    assertEquals(200, response.getStatusCode());
    assertEquals(ACCEPT_PARQUET, response.getContentType());
    assertEquals(
        "attachment; filename=" + SCHEMA_NAME + "_" + TABLE_NAME + ".parquet",
        response.getHeader("Content-Disposition"));

    CompareTools.assertEquals(
        List.of(
            row("name", "cat", "legs", 4, MG_DRAFT, null),
            row("name", "bird", "legs", 2, MG_DRAFT, null)),
        readParquet(response));
  }

  @Test
  void shouldOnlyIncludeRequestedColumns() throws IOException {
    Response response = get("/" + SCHEMA_NAME + "/api/parquet/" + TABLE_NAME + "?columns=name");

    assertEquals(200, response.getStatusCode());
    CompareTools.assertEquals(
        List.of(row("name", "cat"), row("name", "bird")), readParquet(response));
  }

  @Test
  void shouldResolveComputedValues() throws IOException {
    // the computed column is added after inserting, so no computed value is stored and the
    // download has to resolve it
    schema
        .getTable(TABLE_NAME)
        .getMetadata()
        .add(column("description").setComputed("name + ' has ' + legs + ' legs'"));

    Response response = get("/" + SCHEMA_NAME + "/api/parquet/" + TABLE_NAME);

    assertEquals(200, response.getStatusCode());
    CompareTools.assertEquals(
        List.of(
            row("name", "cat", "legs", 4, "description", "cat has 4 legs", MG_DRAFT, null),
            row("name", "bird", "legs", 2, "description", "bird has 2 legs", MG_DRAFT, null)),
        readParquet(response));
  }

  @Test
  void shouldFilterOnEquals() throws IOException {
    Response response = getWithFilter("{\"name\":{\"equals\":\"cat\"}}");

    assertEquals(200, response.getStatusCode());
    CompareTools.assertEquals(
        List.of(row("name", "cat", "legs", 4, MG_DRAFT, null)), readParquet(response));
  }

  @Test
  void shouldFilterOnRange() throws IOException {
    Response response = getWithFilter("{\"legs\":{\"between\":[1,3]}}");

    assertEquals(200, response.getStatusCode());
    CompareTools.assertEquals(
        List.of(row("name", "bird", "legs", 2, MG_DRAFT, null)), readParquet(response));
  }

  @Test
  void shouldCombineFiltersOnMultipleColumns() throws IOException {
    Response response =
        getWithFilter("{\"name\":{\"equals\":\"cat\"},\"legs\":{\"between\":[1,3]}}");

    assertEquals(200, response.getStatusCode());
    CompareTools.assertEquals(List.of(), readParquet(response));
  }

  @Test
  void shouldRejectMalformedFilter() {
    Response response = getWithFilter("{\"name\":");

    assertEquals(400, response.getStatusCode());
    assertTrue(response.getBody().asString().contains("Unexpected end-of-input"));
  }

  @Test
  void shouldRejectFilterOnUnknownColumn() {
    Response response = getWithFilter("{\"unknown\":{\"equals\":\"cat\"}}");

    assertEquals(400, response.getStatusCode());
    assertTrue(
        response.getBody().asString().contains("Column unknown unknown in table " + TABLE_NAME));
  }

  @Test
  void shouldRejectUnknownTable() {
    Response response = get("/" + SCHEMA_NAME + "/api/parquet/unknown");

    assertEquals(400, response.getStatusCode());
    assertTrue(response.getBody().asString().contains("Table unknown unknown"));
  }

  @Test
  void shouldRejectUserWithoutAccess() {
    Response response = given().when().get("/" + SCHEMA_NAME + "/api/parquet/" + TABLE_NAME);

    assertEquals(400, response.getStatusCode());
    assertTrue(response.getBody().asString().contains("Schema " + SCHEMA_NAME + " unknown"));
  }

  private static Response get(String path) {
    return given().sessionId(sessionId).when().get(path);
  }

  private static Response getWithFilter(String filter) {
    return given()
        .sessionId(sessionId)
        .queryParam("filter", filter)
        .when()
        .get("/" + SCHEMA_NAME + "/api/parquet/" + TABLE_NAME);
  }

  private List<Row> readParquet(Response response) throws IOException {
    Files.write(downloadDir.resolve(TABLE_NAME + ".parquet"), response.asByteArray());
    List<Row> rows = new ArrayList<>();
    new TableStoreForParquet(downloadDir).readTable(TABLE_NAME).forEach(rows::add);
    return rows;
  }
}
