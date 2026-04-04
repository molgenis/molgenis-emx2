package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestProfileFiltering {

  private static Schema schema;
  private static GraphqlExecutor graphql;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestProfileFiltering.class.getSimpleName());

    schema.create(
        table("Experiments")
            .add(column("experiment_id").setPkey())
            .add(column("date").setType(ColumnType.DATE))
            .add(
                column("experiment_type")
                    .setType(ColumnType.EXTENSION)
                    .setRequired(true)
                    .setProfiles("-core"))
            .add(column("sample").setType(ColumnType.STRING))
            .add(column("wgs_only_field").setType(ColumnType.STRING).setProfiles("wgs"))
            .add(column("always_visible").setType(ColumnType.STRING)));

    schema.create(
        table("WGS")
            .setInheritNames("Experiments")
            .setProfiles("wgs")
            .add(column("coverage").setType(ColumnType.INT))
            .add(column("platform").setType(ColumnType.STRING)));

    schema.create(
        table("RNAseq")
            .setInheritNames("Experiments")
            .setProfiles("rna")
            .add(column("library_strategy").setType(ColumnType.STRING))
            .add(column("read_length").setType(ColumnType.INT)));

    schema.create(
        table("Imaging")
            .setInheritNames("Experiments")
            .setProfiles("imaging")
            .add(column("modality").setType(ColumnType.STRING)));

    schema.getMetadata().setActiveProfiles("wgs", "rna");

    graphql = new GraphqlExecutor(schema, new TaskServiceInMemory());
  }

  private JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphql.executeWithoutSession(query));
    JsonNode node = MAPPER.readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return node.get("data");
  }

  private JsonNode findByName(JsonNode arrayNode, String name) {
    if (arrayNode == null || !arrayNode.isArray()) return null;
    for (JsonNode node : arrayNode) {
      JsonNode nameNode = node.get("name");
      if (nameNode != null && name.equals(nameNode.asText())) return node;
    }
    return null;
  }

  @Test
  void testSchemaWithoutFilter_returnsAllTables() throws IOException {
    JsonNode result = execute("{_schema{tables{name,profiles}}}");
    JsonNode tables = result.at("/_schema/tables");

    assertTrue(tables.size() >= 4, "Expected at least 4 tables, got " + tables.size());
    assertNotNull(findByName(tables, "Experiments"), "Experiments should be present");
    assertNotNull(findByName(tables, "WGS"), "WGS should be present");
    assertNotNull(findByName(tables, "RNAseq"), "RNAseq should be present");
    assertNotNull(findByName(tables, "Imaging"), "Imaging should be present");
  }

  @Test
  void testSchemaWithApplyProfileFilter_filtersToActiveProfiles() throws IOException {
    JsonNode result = execute("{_schema(applyProfileFilter:true){tables{name,profiles}}}");
    JsonNode tables = result.at("/_schema/tables");

    assertNotNull(findByName(tables, "WGS"), "WGS should be visible with wgs profile active");
    assertNotNull(findByName(tables, "RNAseq"), "RNAseq should be visible with rna profile active");
    assertNull(
        findByName(tables, "Imaging"), "Imaging should be hidden without imaging profile active");
    assertNotNull(
        findByName(tables, "Experiments"), "Experiments should always be visible (no profiles)");
  }

  @Test
  void testSchemaWithExplicitProfiles_overridesSchemaProfiles() throws IOException {
    JsonNode result = execute("{_schema(profiles:[\"imaging\"]){tables{name}}}");
    JsonNode tables = result.at("/_schema/tables");

    assertNotNull(findByName(tables, "Imaging"), "Imaging should be visible with imaging profile");
    assertNull(findByName(tables, "WGS"), "WGS should be hidden without wgs profile");
    assertNull(findByName(tables, "RNAseq"), "RNAseq should be hidden without rna profile");
    assertNotNull(
        findByName(tables, "Experiments"), "Experiments should always be visible (no profiles)");
  }

  @Test
  void testColumnProfileFiltering_negativeProfile_hiddenWhenProfileActive() throws IOException {
    JsonNode result =
        execute("{_schema(profiles:[\"core\"]){tables{name,columns{name,profiles}}}}");
    JsonNode expTable = findByName(result.at("/_schema/tables"), "Experiments");
    assertNotNull(expTable, "Experiments table should be present");

    JsonNode columns = expTable.get("columns");
    assertNull(
        findByName(columns, "experiment_type"),
        "experiment_type should be hidden when core is active (has -core profile)");
  }

  @Test
  void testColumnProfileFiltering_negativeProfile_visibleWhenProfileNotActive() throws IOException {
    JsonNode result =
        execute("{_schema(applyProfileFilter:true){tables{name,columns{name,profiles}}}}");
    JsonNode expTable = findByName(result.at("/_schema/tables"), "Experiments");
    assertNotNull(expTable, "Experiments table should be present");

    JsonNode columns = expTable.get("columns");
    assertNotNull(
        findByName(columns, "experiment_type"),
        "experiment_type should be visible when core is not active (active: wgs, rna)");
  }

  @Test
  void testColumnPositiveProfile_visibleWhenProfileActive() throws IOException {
    JsonNode result = execute("{_schema(profiles:[\"wgs\"]){tables{name,columns{name,profiles}}}}");
    JsonNode expTable = findByName(result.at("/_schema/tables"), "Experiments");
    assertNotNull(expTable);

    JsonNode columns = expTable.get("columns");
    assertNotNull(
        findByName(columns, "wgs_only_field"),
        "wgs_only_field should be visible when wgs profile is active");
    assertNotNull(
        findByName(columns, "always_visible"),
        "always_visible (no profiles) should always be visible");
  }

  @Test
  void testColumnPositiveProfile_hiddenWhenProfileNotActive() throws IOException {
    JsonNode result =
        execute("{_schema(profiles:[\"imaging\"]){tables{name,columns{name,profiles}}}}");
    JsonNode expTable = findByName(result.at("/_schema/tables"), "Experiments");
    assertNotNull(expTable);

    JsonNode columns = expTable.get("columns");
    assertNull(
        findByName(columns, "wgs_only_field"),
        "wgs_only_field should be hidden when wgs profile is not active");
    assertNotNull(
        findByName(columns, "always_visible"),
        "always_visible (no profiles) should always be visible");
  }

  @Test
  void testActiveProfilesInSchemaOutput() throws IOException {
    JsonNode result = execute("{_schema{activeProfiles}}");
    JsonNode activeProfiles = result.at("/_schema/activeProfiles");
    assertNotNull(activeProfiles);
    assertTrue(activeProfiles.isArray());
    assertEquals(2, activeProfiles.size());
    assertEquals("wgs", activeProfiles.get(0).asText());
    assertEquals("rna", activeProfiles.get(1).asText());
  }

  @Test
  void testTableProfilesViaChangeMutation() throws IOException {
    execute(
        "mutation{change(tables:[{name:\"NewProfiled\",profiles:[\"test\"],columns:[{name:\"id\",columnType:\"STRING\",key:1}]}]){message}}");
    JsonNode result = execute("{_schema{tables{name,profiles}}}");
    JsonNode tables = result.at("/_schema/tables");
    JsonNode newTable = findByName(tables, "NewProfiled");
    assertNotNull(newTable, "NewProfiled should exist");
    JsonNode profiles = newTable.get("profiles");
    assertNotNull(profiles, "NewProfiled should have profiles");
    assertEquals(1, profiles.size());
    assertEquals("test", profiles.get(0).asText());

    execute("mutation{change(tables:[{name:\"NewProfiled\",drop:true}]){message}}");
  }

  @Test
  void testChangeActiveProfilesViaMutation() throws IOException {
    try {
      execute("mutation{change(activeProfiles:[\"imaging\"]){message}}");
      JsonNode result = execute("{_schema{activeProfiles}}");
      JsonNode activeProfiles = result.at("/_schema/activeProfiles");
      assertEquals(1, activeProfiles.size());
      assertEquals("imaging", activeProfiles.get(0).asText());
    } finally {
      execute("mutation{change(activeProfiles:[\"wgs\",\"rna\"]){message}}");
    }
  }

  @Test
  void testProfilesFieldReturnedInTableMetadata() throws IOException {
    JsonNode result = execute("{_schema{tables{name,profiles}}}");

    JsonNode wgs = findByName(result.at("/_schema/tables"), "WGS");
    assertNotNull(wgs, "WGS should be present");
    JsonNode wgsProfiles = wgs.get("profiles");
    assertNotNull(wgsProfiles, "WGS should have profiles field");
    assertTrue(wgsProfiles.isArray(), "profiles should be an array");
    assertEquals(1, wgsProfiles.size(), "WGS should have exactly 1 profile");
    assertEquals("wgs", wgsProfiles.get(0).asText(), "WGS profile should be 'wgs'");
  }
}
