package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx2.Emx2Yaml;

public class TestBundleLock {

  private static final String BUNDLE_YAML =
      "name: lock_test_bundle\n"
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
          + "        subsets: [subset_a]\n";

  private static Database db;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  private SqlSchema createBundleSchema(String schemaName, Path tempDir) throws IOException {
    Path molgenisYaml = tempDir.resolve("molgenis.yaml");
    Files.writeString(molgenisYaml, BUNDLE_YAML);
    Emx2Yaml.BundleResult bundle = Emx2Yaml.fromBundle(tempDir);
    SqlSchema schema = (SqlSchema) db.dropCreateSchema(schemaName);
    schema.migrate(bundle.getSchema());
    schema.attachBundle(bundle.toBundleContext());
    return schema;
  }

  @Test
  void migrateAllowedWhenLockOff(@TempDir Path tempDir) throws IOException {
    SqlSchema schema = createBundleSchema("TestBundleLockOff", tempDir);

    assertFalse(schema.isBundleLocked(), "lock should be off by default");
    SchemaMetadata extra = new SchemaMetadata();
    extra.create(new TableMetadata("ExtraTable").add(column("id").setPkey()));
    assertDoesNotThrow(() -> schema.migrate(extra));
  }

  @Test
  void migrateBlockedWhenLockOn(@TempDir Path tempDir) throws IOException {
    SqlSchema schema = createBundleSchema("TestBundleLockOn", tempDir);

    schema.getMetadata().setSetting(Constants.BUNDLE_LOCK_FEATURE_FLAG, "true");

    assertTrue(schema.isBundleLocked(), "lock should be on after setting flag");

    SchemaMetadata blocked = new SchemaMetadata();
    blocked.create(new TableMetadata("BlockedTable").add(column("id").setPkey()));
    MolgenisException ex = assertThrows(MolgenisException.class, () -> schema.migrate(blocked));
    assertTrue(
        ex.getMessage().contains("bundle lock is enabled"),
        "error should mention bundle lock, got: " + ex.getMessage());
  }

  @Test
  void activateSubsetStillWorksWhenLockOn(@TempDir Path tempDir) throws IOException {
    SqlSchema schema = createBundleSchema("TestBundleLockActivate", tempDir);

    schema.getMetadata().setSetting(Constants.BUNDLE_LOCK_FEATURE_FLAG, "true");
    assertTrue(schema.isBundleLocked());

    assertDoesNotThrow(
        () -> schema.activateSubset("subset_a"),
        "activateSubset should not be blocked by bundle lock");
  }

  @Test
  void nonBundleSchemaIsNotLocked() {
    Schema schema = db.dropCreateSchema("TestBundleLockNonBundle");

    assertFalse(schema.isBundleBacked(), "non-bundle schema should not be bundle-backed");
    assertFalse(schema.isBundleLocked(), "non-bundle schema should not be locked");
  }
}
