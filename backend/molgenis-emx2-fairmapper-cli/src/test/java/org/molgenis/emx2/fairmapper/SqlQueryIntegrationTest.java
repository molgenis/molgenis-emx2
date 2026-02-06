package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class SqlQueryIntegrationTest {

  private static Database database;
  private static final ObjectMapper mapper = new ObjectMapper();

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void testDcatFdpSqlQuery() throws IOException {
    String schemaName = "test_dcat_fdp";
    database.dropSchemaIfExists(schemaName);
    Schema schema = database.createSchema(schemaName);

    schema.create(
        table("Resources")
            .add(column("id").setPkey())
            .add(column("name"))
            .add(column("description"))
            .add(column("website"))
            .add(column("type").setType(STRING_ARRAY))
            .add(column("data resources").setType(STRING_ARRAY)));

    schema
        .getTable("Resources")
        .insert(
            row(
                "id",
                "lifelines",
                "name",
                "LifeLines",
                "description",
                "A cohort study catalog",
                "website",
                "https://lifelines.nl",
                "type",
                new String[] {"Catalogue"},
                "data resources",
                new String[] {"dataset1"}));
    schema
        .getTable("Resources")
        .insert(
            row(
                "id",
                "dataset1",
                "name",
                "Dataset One",
                "description",
                "First dataset",
                "type",
                new String[] {"Cohort study"}));

    java.nio.file.Path projectRoot = Paths.get("").toAbsolutePath();
    while (!Files.exists(projectRoot.resolve("fair-mappings")) && projectRoot.getParent() != null) {
      projectRoot = projectRoot.getParent();
    }
    String sql =
        Files.readString(projectRoot.resolve("fair-mappings/dcat-fdp-sql/src/get-catalog.sql"));

    Map<String, String> params =
        Map.of("base_url", "https://test.molgenis.org", "schema", schemaName, "id", "lifelines");

    List<Row> rows = schema.retrieveSql(sql, params);

    assertNotNull(rows);
    assertEquals(1, rows.size(), "Expected exactly one row");

    Row resultRow = rows.get(0);
    assertNotNull(resultRow);

    String jsonStr = resultRow.get("result", String.class);
    assertNotNull(jsonStr, "Result column should not be null");
    JsonNode jsonResult = mapper.readTree(jsonStr);

    assertTrue(jsonResult.has("@context"), "JSON-LD should have @context");
    JsonNode context = jsonResult.get("@context");
    assertTrue(context.has("dcat"), "@context should include dcat");
    assertTrue(context.has("dct"), "@context should include dct");

    assertTrue(jsonResult.has("@type"), "JSON-LD should have @type");
    assertEquals("dcat:Catalog", jsonResult.get("@type").asText());

    assertTrue(jsonResult.has("dct:title"), "JSON-LD should have dct:title");
    assertEquals("LifeLines", jsonResult.get("dct:title").asText());

    assertTrue(jsonResult.has("dcat:dataset"), "JSON-LD should have dcat:dataset");
    JsonNode datasets = jsonResult.get("dcat:dataset");
    assertTrue(datasets.isArray(), "dcat:dataset should be an array");
    assertEquals(1, datasets.size(), "Should have one dataset");

    database.dropSchemaIfExists(schemaName);
  }
}
