package org.molgenis.emx2.sql;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.appmigrations.Profile;

public class TestSchemaMigrations {

  private static Database db;
  private static final String SCHEMA_NAME = "appMigrationTest";

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
    ((SqlDatabase) db).setSchemaProfileVersion(schema, Profile.CATALOGUE, 0);
    new ProfileMigrations().runAppSchemaMigrations((SqlDatabase) db);
  }
}
