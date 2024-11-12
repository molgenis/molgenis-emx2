package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.*;
import org.molgenis.emx2.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestDeleteRefback {
  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  private static void loadSchemas(Schema schema, Schema schemaOther) {
    TableMetadata patients =
        schema
            .getMetadata()
            .create(table("Patients").add(column("id").setPkey()).add(column("patient")));

    TableMetadata patientVisits =
        schemaOther
            .getMetadata()
            .create(
                table("Patients visits")
                    .add(column("visit id").setPkey())
                    .add(
                        column("patient")
                            .setType(REF)
                            .setRefTable(patients.getTableName())
                            .setRefSchemaName(schema.getName())));

    // reload
    schema
        .getMetadata()
        .getTableMetadata("Patients")
        .add(
            column("visits")
                .setType(REFBACK)
                .setRefTable(patientVisits.getTableName())
                .setRefBack("patient")
                .setRefSchemaName(schema.getName()));
  }

  @Test
  public void testRefBackDeleteRefTable() {
    Schema schemaOther = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "Other1");
    Schema schema = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "1");
    loadSchemas(schema, schemaOther);

    // should also delete the 'visits' refback in Patients
    schemaOther.getMetadata().drop("Patient visits");
    assertNull(schema.getMetadata().getTableMetadata("Patient").getColumn("visits"));
  }

  @Test
  public void testRefBackDeleteRefColumn() {
    Schema schemaOther = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "Other2");
    Schema schema = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "2");
    loadSchemas(schema, schemaOther);

    // should also delete the 'visits' refback in Patient
    schema.getMetadata().getTableMetadata("Patient visits").dropColumn("patient");
    assertNull(schema.getMetadata().getTableMetadata("Patients").getColumn("visits"));
  }

  @Test
  public void testRefBackRenameRefTable() {
    Schema schemaOther = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "Other3");
    Schema schema = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "3");
    loadSchemas(schema, schemaOther);

    // refTable rename
    schema.getMetadata().getTableMetadata("Patient visits").alterName("Patient visits2");
    assertEquals(
        "Patient visits2",
        schema.getMetadata().getTableMetadata("visits").getColumn("patient").getRefTableName());
  }

  @Test
  public void testRefBackRenameRefColumn() {
    Schema schemaOther = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "Other4");
    Schema schema = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "4");
    loadSchemas(schema, schemaOther);

    // refColumn rename
    schema
        .getMetadata()
        .getTableMetadata("Patient visits")
        .getColumn("patient")
        .setName("patient2");
    assertEquals(
        "patient2",
        schema.getMetadata().getTableMetadata("Patient visits").getColumn("visits").getRefBack());
  }
}
