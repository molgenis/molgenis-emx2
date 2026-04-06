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

public class TestProfileActivation {

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
  void enableProfileCreatesTaggedColumns(@TempDir Path tempDir) throws IOException {
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
    SqlSchema schema = createSchema("TestEnableProfileCreatesTaggedColumns");
    schema.attachBundle(bundle.toBundleContext());

    assertFalse(
        columnExistsInPg(schema, "Animals", "weight"),
        "weight column should not exist before enabling");

    schema.enableProfile("subset_a");

    assertTrue(
        columnExistsInPg(schema, "Animals", "weight"),
        "weight column should exist after enabling subset_a");
    assertTrue(
        columnExistsInPg(schema, "Animals", "id"), "id (always-on) column should always exist");
    assertTrue(
        columnExistsInPg(schema, "Animals", "name"), "name (always-on) column should always exist");
  }

  @Test
  void enableProfileIsIdempotent(@TempDir Path tempDir) throws IOException {
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
    SqlSchema schema = createSchema("TestEnableProfileIsIdempotent");
    schema.attachBundle(bundle.toBundleContext());

    schema.enableProfile("subset_a");
    assertDoesNotThrow(() -> schema.enableProfile("subset_a"), "Second enable should not throw");

    assertTrue(columnExistsInPg(schema, "Animals", "weight"), "weight should still exist");
  }

  @Test
  void enableProfileCreatesTaggedTables(@TempDir Path tempDir) throws IOException {
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
    SqlSchema schema = createSchema("TestEnableProfileCreatesTaggedTables");
    schema.attachBundle(bundle.toBundleContext());

    assertFalse(
        tableExistsInPg(schema, "Sequencing"), "Sequencing should not exist before enabling");

    schema.enableProfile("subset_b");

    assertTrue(tableExistsInPg(schema, "Sequencing"), "Sequencing should exist after enabling");
    assertTrue(tableExistsInPg(schema, "Experiments"), "Experiments (always-on) should exist");
  }

  @Test
  void enableProfileRunsIncludes(@TempDir Path tempDir) throws IOException {
    Path bundleDir =
        writeBundleYaml(
            tempDir,
            "name: test_bundle\n"
                + "profiles:\n"
                + "  core:\n"
                + "    description: Core\n"
                + "    internal: true\n"
                + "  extended:\n"
                + "    description: Extended\n"
                + "    internal: true\n"
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
                + "        profiles: [core]\n"
                + "      color:\n"
                + "        type: string\n"
                + "        profiles: [extended]\n");

    Emx2Yaml.BundleResult bundle = parseBundle(bundleDir);
    SqlSchema schema = createSchema("TestEnableProfileRunsIncludes");
    schema.attachBundle(bundle.toBundleContext());

    schema.enableProfile("full");

    assertTrue(
        columnExistsInPg(schema, "Animals", "weight"),
        "weight (core profile via full template) should be created");
    assertTrue(
        columnExistsInPg(schema, "Animals", "color"),
        "color (extended profile via full template) should be created");
  }

  @Test
  void disableProfileLeavesColumnsInDb(@TempDir Path tempDir) throws IOException {
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
    SqlSchema schema = createSchema("TestDisableProfileLeavesColumns");
    schema.attachBundle(bundle.toBundleContext());

    schema.enableProfile("subset_a");
    schema.enableProfile("subset_b");
    assertTrue(columnExistsInPg(schema, "Animals", "weight"), "weight should exist after enable");

    schema.disableProfile("subset_a");

    assertTrue(
        columnExistsInPg(schema, "Animals", "weight"),
        "weight column must stay in Postgres after disable (DDL-free disable)");

    Set<String> activeAfterDisable = schema.getActiveProfiles();
    assertFalse(activeAfterDisable.contains("subset_a"), "subset_a should not be active");
    assertTrue(activeAfterDisable.contains("subset_b"), "subset_b should still be active");

    String[] weightProfiles =
        schema.getMetadata().getTableMetadata("Animals").getColumn("weight").getProfiles();
    String[] activeArray = activeAfterDisable.toArray(new String[0]);
    assertFalse(
        ProfileUtils.matchesActiveProfiles(weightProfiles, activeArray),
        "ProfileUtils should report weight as not visible when subset_a is disabled and subset_b is active");
  }

  @Test
  void disableProfileThenReEnable(@TempDir Path tempDir) throws IOException {
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
    SqlSchema schema = createSchema("TestDisableProfileThenReEnable");
    schema.attachBundle(bundle.toBundleContext());

    schema.enableProfile("subset_a");
    schema.disableProfile("subset_a");
    assertFalse(schema.getActiveProfiles().contains("subset_a"));

    schema.enableProfile("subset_a");
    assertTrue(schema.getActiveProfiles().contains("subset_a"));
    assertTrue(
        columnExistsInPg(schema, "Animals", "weight"), "weight should be present after re-enable");
  }
}
