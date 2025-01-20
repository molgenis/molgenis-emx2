package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Profile;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.sql.model.ProfileSchema;

class TestSchemaMigrations {

  private static Database db;
  private static final String SCHEMA_NAME = "schemaMigrationTest";

  @BeforeAll
  static void setup() {
    db = new SqlDatabase(false);
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @AfterEach
  void cleanup() {
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void testSchemaMigrations() {
    Schema schema = db.createSchema(SCHEMA_NAME);
    ((SqlDatabase) db).setSchemaProfileVersion(schema, Profile.DATA_CATALOGUE, 0);
    List<ProfileSchema> migrated = new ProfileMigrations().runAppSchemaMigrations((SqlDatabase) db);

    SchemaMetadata schemaMetadata = db.getSchema(SCHEMA_NAME).getMetadata();

    assertEquals(
        ProfileMigrations.profileVersions.get(Profile.DATA_CATALOGUE),
        schemaMetadata.getProfileMigrationStep());
    assertEquals(new ProfileSchema(SCHEMA_NAME, Profile.DATA_CATALOGUE, 2), migrated.get(0));
  }
}
