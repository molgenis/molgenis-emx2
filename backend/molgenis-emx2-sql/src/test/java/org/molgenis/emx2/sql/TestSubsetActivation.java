package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.ProfileUtils;
import org.molgenis.emx2.io.emx2.Emx2Yaml;

public class TestSubsetActivation {

  private static Database db;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  private SqlSchema createSchema(String schemaName) {
    return (SqlSchema) db.dropCreateSchema(schemaName);
  }

  private Emx2Yaml.BundleResult parseBundle(Path bundleDir) throws IOException {
    return Emx2Yaml.fromBundle(bundleDir);
  }

  private boolean columnExistsInPg(SqlSchema schema, String tableName, String columnName) {
    DSLContext jooq = schema.getJooq();
    return 0
        < jooq.select(count())
            .from(name("information_schema", "columns"))
            .where(
                field("table_schema")
                    .eq(schema.getName())
                    .and(field("table_name").eq(tableName))
                    .and(field("column_name").eq(columnName)))
            .fetchOne(0, Integer.class);
  }

  private boolean tableExistsInPg(SqlSchema schema, String tableName) {
    DSLContext jooq = schema.getJooq();
    return 0
        < jooq.select(count())
            .from(name("information_schema", "tables"))
            .where(
                field("table_schema").eq(schema.getName()).and(field("table_name").eq(tableName)))
            .fetchOne(0, Integer.class);
  }

  private Path writeBundleYaml(Path dir, String yaml) throws IOException {
    Path molgenisYaml = dir.resolve("molgenis.yaml");
    Files.writeString(molgenisYaml, yaml);
    return dir;
  }

  @Test
  void activateCreatesTaggedColumns(@TempDir Path tempDir) throws IOException {
    Path bundleDir =
        writeBundleYaml(
            tempDir,
            "name: test_bundle\n"
                + "subsets:\n"
                + "  subset_a:\n"
                + "    description: Subset A\n"
                + "tables:\n"
                + "  Animals:\n"
                + "    columns:\n"
                + "      id:\n"
                + "        type: int\n"
                + "        key: 1\n"
                + "      name:\n"
                + "        type: string\n"
                + "      weight:\n"
                + "        type: decimal\n"
                + "        subsets: [subset_a]\n");

    Emx2Yaml.BundleResult bundle = parseBundle(bundleDir);
    SqlSchema schema = createSchema("TestActivateCreatesTaggedColumns");
    schema.attachBundle(bundle.toBundleContext());

    assertFalse(
        columnExistsInPg(schema, "Animals", "weight"),
        "weight column should not exist before activation");

    schema.activateSubset("subset_a");

    assertTrue(
        columnExistsInPg(schema, "Animals", "weight"),
        "weight column should exist after activating subset_a");
    assertTrue(
        columnExistsInPg(schema, "Animals", "id"), "id (always-on) column should always exist");
    assertTrue(
        columnExistsInPg(schema, "Animals", "name"), "name (always-on) column should always exist");
  }

  @Test
  void activateIsIdempotent(@TempDir Path tempDir) throws IOException {
    Path bundleDir =
        writeBundleYaml(
            tempDir,
            "name: test_bundle\n"
                + "subsets:\n"
                + "  subset_a:\n"
                + "    description: Subset A\n"
                + "tables:\n"
                + "  Animals:\n"
                + "    columns:\n"
                + "      id:\n"
                + "        type: int\n"
                + "        key: 1\n"
                + "      weight:\n"
                + "        type: decimal\n"
                + "        subsets: [subset_a]\n");

    Emx2Yaml.BundleResult bundle = parseBundle(bundleDir);
    SqlSchema schema = createSchema("TestActivateIsIdempotent");
    schema.attachBundle(bundle.toBundleContext());

    schema.activateSubset("subset_a");
    assertDoesNotThrow(
        () -> schema.activateSubset("subset_a"), "Second activation should not throw");

    assertTrue(columnExistsInPg(schema, "Animals", "weight"), "weight should still exist");
  }

  @Test
  void activateCreatesTaggedTables(@TempDir Path tempDir) throws IOException {
    Path bundleDir =
        writeBundleYaml(
            tempDir,
            "name: test_bundle\n"
                + "subsets:\n"
                + "  subset_b:\n"
                + "    description: Subset B\n"
                + "tables:\n"
                + "  Experiments:\n"
                + "    columns:\n"
                + "      id:\n"
                + "        type: int\n"
                + "        key: 1\n"
                + "  Sequencing:\n"
                + "    subsets: [subset_b]\n"
                + "    columns:\n"
                + "      id:\n"
                + "        type: int\n"
                + "        key: 1\n"
                + "      read_length:\n"
                + "        type: int\n");

    Emx2Yaml.BundleResult bundle = parseBundle(bundleDir);
    SqlSchema schema = createSchema("TestActivateCreatesTaggedTables");
    schema.attachBundle(bundle.toBundleContext());

    assertFalse(
        tableExistsInPg(schema, "Sequencing"), "Sequencing should not exist before activation");

    schema.activateSubset("subset_b");

    assertTrue(tableExistsInPg(schema, "Sequencing"), "Sequencing should exist after activation");
    assertTrue(tableExistsInPg(schema, "Experiments"), "Experiments (always-on) should exist");
  }

  @Test
  void activateRunsIncludes(@TempDir Path tempDir) throws IOException {
    Path bundleDir =
        writeBundleYaml(
            tempDir,
            "name: test_bundle\n"
                + "subsets:\n"
                + "  core:\n"
                + "    description: Core\n"
                + "  extended:\n"
                + "    description: Extended\n"
                + "templates:\n"
                + "  full:\n"
                + "    description: Full template\n"
                + "    includes: [core, extended]\n"
                + "tables:\n"
                + "  Animals:\n"
                + "    columns:\n"
                + "      id:\n"
                + "        type: int\n"
                + "        key: 1\n"
                + "      weight:\n"
                + "        type: decimal\n"
                + "        subsets: [core]\n"
                + "      color:\n"
                + "        type: string\n"
                + "        subsets: [extended]\n");

    Emx2Yaml.BundleResult bundle = parseBundle(bundleDir);
    SqlSchema schema = createSchema("TestActivateRunsIncludes");
    schema.attachBundle(bundle.toBundleContext());

    schema.activateSubset("full");

    assertTrue(
        columnExistsInPg(schema, "Animals", "weight"),
        "weight (core subset via full template) should be created");
    assertTrue(
        columnExistsInPg(schema, "Animals", "color"),
        "color (extended subset via full template) should be created");
  }

  @Test
  void deactivateLeavesColumnsInDb(@TempDir Path tempDir) throws IOException {
    Path bundleDir =
        writeBundleYaml(
            tempDir,
            "name: test_bundle\n"
                + "subsets:\n"
                + "  subset_a:\n"
                + "    description: Subset A\n"
                + "  subset_b:\n"
                + "    description: Subset B\n"
                + "tables:\n"
                + "  Animals:\n"
                + "    columns:\n"
                + "      id:\n"
                + "        type: int\n"
                + "        key: 1\n"
                + "      weight:\n"
                + "        type: decimal\n"
                + "        subsets: [subset_a]\n"
                + "      color:\n"
                + "        type: string\n"
                + "        subsets: [subset_b]\n");

    Emx2Yaml.BundleResult bundle = parseBundle(bundleDir);
    SqlSchema schema = createSchema("TestDeactivateLeavesColumns");
    schema.attachBundle(bundle.toBundleContext());

    schema.activateSubset("subset_a");
    schema.activateSubset("subset_b");
    assertTrue(columnExistsInPg(schema, "Animals", "weight"), "weight should exist after activate");

    schema.deactivateSubset("subset_a");

    assertTrue(
        columnExistsInPg(schema, "Animals", "weight"),
        "weight column must stay in Postgres after deactivate (DDL-free deactivation)");

    Set<String> activeAfterDeactivate = schema.getActiveSubsets();
    assertFalse(activeAfterDeactivate.contains("subset_a"), "subset_a should not be active");
    assertTrue(activeAfterDeactivate.contains("subset_b"), "subset_b should still be active");

    String[] weightSubsets =
        schema.getMetadata().getTableMetadata("Animals").getColumn("weight").getSubsets();
    String[] activeArray = activeAfterDeactivate.toArray(new String[0]);
    assertFalse(
        ProfileUtils.matchesActiveProfiles(weightSubsets, activeArray),
        "ProfileUtils should report weight as not visible when subset_a is deactivated and subset_b is active");
  }

  @Test
  void deactivateThenReactivate(@TempDir Path tempDir) throws IOException {
    Path bundleDir =
        writeBundleYaml(
            tempDir,
            "name: test_bundle\n"
                + "subsets:\n"
                + "  subset_a:\n"
                + "    description: Subset A\n"
                + "tables:\n"
                + "  Animals:\n"
                + "    columns:\n"
                + "      id:\n"
                + "        type: int\n"
                + "        key: 1\n"
                + "      weight:\n"
                + "        type: decimal\n"
                + "        subsets: [subset_a]\n");

    Emx2Yaml.BundleResult bundle = parseBundle(bundleDir);
    SqlSchema schema = createSchema("TestDeactivateThenReactivate");
    schema.attachBundle(bundle.toBundleContext());

    schema.activateSubset("subset_a");
    schema.deactivateSubset("subset_a");
    assertFalse(schema.getActiveSubsets().contains("subset_a"));

    schema.activateSubset("subset_a");
    assertTrue(schema.getActiveSubsets().contains("subset_a"));
    assertTrue(
        columnExistsInPg(schema, "Animals", "weight"), "weight should be present after reactivate");
  }
}
