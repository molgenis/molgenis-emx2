package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Schema;

public class TestDropSchemaWithLingeringTriggers {
  private static SqlDatabase db;
  private static final String schemaName =
      TestDropSchemaWithLingeringTriggers.class.getSimpleName();
  private static final String otherSchemaName = schemaName + "Referring";

  @BeforeAll
  public static void setUp() {
    db = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  }

  @AfterEach
  public void tearDown() {
    db.dropSchemaIfExists(otherSchemaName);
    db.dropSchemaIfExists(schemaName);
  }

  private Schema createPatientRegistry(String name) {
    Schema schema = db.dropCreateSchema(name);
    schema.create(table("Hospital", column("name").setPkey(), column("city")));
    schema.create(
        table(
            "Doctor",
            column("id").setPkey(),
            column("name"),
            column("hospital").setType(REF).setRefTable("Hospital")));
    schema.create(
        table(
            "Patient",
            column("id").setPkey(),
            column("name"),
            column("doctors").setType(REF).setRefTable("Doctor")));
    return schema;
  }

  private void createTriggerFunction(String functionName) {
    db.getJooq()
        .execute(
            "CREATE FUNCTION {0}() RETURNS trigger AS $BODY$ BEGIN RETURN NEW; END; $BODY$ LANGUAGE plpgsql",
            name(schemaName, functionName));
  }

  private void createConstraintTrigger(
      String triggerName, String tableName, String columnName, String functionName) {
    db.getJooq()
        .execute(
            "CREATE CONSTRAINT TRIGGER {0} AFTER INSERT OR UPDATE OF {1} ON {2} "
                + "DEFERRABLE INITIALLY IMMEDIATE FOR EACH ROW EXECUTE PROCEDURE {3}()",
            name(triggerName),
            name(columnName),
            name(schemaName, tableName),
            name(schemaName, functionName));
  }

  private void createRefArrayTriggersOfPreviousVersion() {
    String referenceExistsCheck = "C_" + schemaName + "_Patient_doctors";
    String referredCheck = "DEL_" + schemaName + "_Patient_doctors";

    createTriggerFunction(referenceExistsCheck);
    createTriggerFunction(referredCheck);
    createConstraintTrigger(referenceExistsCheck, "Patient", "doctors", referenceExistsCheck);
    createConstraintTrigger(referredCheck, "Doctor", "id", referredCheck);
  }

  @Test
  public void dropSchemaSucceedsWithRefArrayTriggersLeftBehindByPreviousVersion() {
    createPatientRegistry(schemaName);
    createRefArrayTriggersOfPreviousVersion();

    db.dropSchema(schemaName);

    assertNull(db.getSchema(schemaName));
  }

  @Test
  public void dropColumnSucceedsWithRefArrayTriggersLeftBehindByPreviousVersion() {
    Schema schema = createPatientRegistry(schemaName);
    createRefArrayTriggersOfPreviousVersion();

    schema.getMetadata().getTableMetadata("Patient").dropColumn("doctors");

    assertNull(schema.getMetadata().getTableMetadata("Patient").getColumn("doctors"));
  }

  @Test
  public void dropSchemaStillBlockedByReferenceFromOtherSchema() {
    createPatientRegistry(schemaName);
    Schema otherSchema = db.createSchema(otherSchemaName);
    otherSchema.create(
        table(
            "Visit",
            column("id").setPkey(),
            column("doctors")
                .setType(REF_ARRAY)
                .setRefSchemaName(schemaName)
                .setRefTable("Doctor")));

    assertThrows(Exception.class, () -> db.dropSchema(schemaName));

    assertNotNull(db.getSchema(schemaName));
  }
}
