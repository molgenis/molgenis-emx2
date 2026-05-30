package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;

public class TestRlsEnabledMetadataRoundtrip {

  private static final String SCHEMA_NAME = TestRlsEnabledMetadataRoundtrip.class.getSimpleName();
  private static Database db;
  private static DSLContext jooq;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    jooq = ((SqlDatabase) db).getJooq();
    db.dropCreateSchema(SCHEMA_NAME);
  }

  @Test
  void defaultsFalse_onNewTable() {
    Schema schema = db.getSchema(SCHEMA_NAME);
    schema.create(table("DefaultsTable").add(column("id").setPkey()));

    assertFalse(
        Boolean.TRUE.equals(schema.getTable("DefaultsTable").getMetadata().getRlsEnabled()),
        "rlsEnabled must default to false on a freshly created table");

    db.clearCache();
    Schema reloaded = db.getSchema(SCHEMA_NAME);
    assertFalse(
        Boolean.TRUE.equals(reloaded.getTable("DefaultsTable").getMetadata().getRlsEnabled()),
        "rlsEnabled must be false after cache reload");
  }

  @Test
  void roundTripsTrue() {
    Schema schema = db.getSchema(SCHEMA_NAME);
    schema.create(table("RlsTable").add(column("id").setPkey()));

    TableMetadata meta = schema.getTable("RlsTable").getMetadata();
    meta.setRlsEnabled(true);
    MetadataUtils.saveTableMetadata(jooq, meta);

    db.clearCache();
    Schema reloaded = db.getSchema(SCHEMA_NAME);
    assertTrue(
        Boolean.TRUE.equals(reloaded.getTable("RlsTable").getMetadata().getRlsEnabled()),
        "rlsEnabled must be true after save + cache reload");
  }
}
