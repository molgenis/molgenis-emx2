package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.*;
import org.molgenis.emx2.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestRefBackChangeCascade {
  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = new SqlDatabase(SqlDatabase.ADMIN_USER);
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

    // get fresh schema metadata and check
    schema
        .getMetadata()
        .getTableMetadata("Patients")
        .add(
            column("visits")
                .setType(REFBACK)
                .setRefSchemaName(schemaOther.getName())
                .setRefTable(patientVisits.getTableName())
                .setRefBack("patient"));
  }

  @Test
  public void testRefBackDeleteRefTable() {
    Schema schemaOther = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "Other1");
    Schema schema = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "1");
    loadSchemas(schema, schemaOther);

    // should also delete the 'visits' refback in Patients
    schemaOther.getMetadata().drop("Patients visits");

    // get fresh schema metadata and check
    assertNull(
        database
            .getSchema(schema.getName())
            .getMetadata()
            .getTableMetadata("Patients")
            .getColumn("visits"));
  }

  @Test
  public void testRefBackDeleteRefColumn() {
    Schema schemaOther = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "Other2");
    Schema schema = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "2");
    loadSchemas(schema, schemaOther);

    // should also delete the 'visits' refback in Patient
    schemaOther.getMetadata().getTableMetadata("Patients visits").dropColumn("patient");

    // get fresh schema metadata and check
    assertNull(
        database
            .getSchema(schema.getName())
            .getMetadata()
            .getTableMetadata("Patients")
            .getColumn("visits"));
  }

  @Test
  public void testRefBackRenameRefTable() {
    Schema schemaOther = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "Other3");
    Schema schema = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "3");
    loadSchemas(schema, schemaOther);

    // refTable rename
    schemaOther.getMetadata().getTableMetadata("Patients visits").alterName("Patients visits2");
    assertEquals(
        "Patients visits2",
        database
            .getSchema(schema.getName())
            .getMetadata()
            .getTableMetadata("Patients")
            .getColumn("visits")
            .getRefTableName());
  }

  @Test
  public void testRefBackRenameRefColumn() {
    Schema schemaOther = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "Other4");
    Schema schema = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "4");
    loadSchemas(schema, schemaOther);

    // refColumn rename
    Column refColumn =
        schemaOther
            .getMetadata()
            .getTableMetadata("Patients visits")
            .getColumn("patient")
            .setName("patient2");
    schemaOther.getMetadata().getTableMetadata("Patients visits").alterColumn("patient", refColumn);

    // get fresh schema metadata and check
    assertEquals(
        "patient2",
        database
            .getSchema(schema.getName())
            .getMetadata()
            .getTableMetadata("Patients")
            .getColumn("visits")
            .getRefBack());
  }

  @Test
  public void testRefBackChangeRefToNonRef() {
    Schema schemaOther = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "Other5");
    Schema schema = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "5");
    loadSchemas(schema, schemaOther);

    // refColumn deref
    Column refColumn =
        schemaOther
            .getMetadata()
            .getTableMetadata("Patients visits")
            .getColumn("patient")
            .setRefTable(null)
            .setType(STRING);
    schemaOther.getMetadata().getTableMetadata("Patients visits").alterColumn("patient", refColumn);

    // get fresh schema metadata and check
    assertNull(
        database
            .getSchema(schema.getName())
            .getMetadata()
            .getTableMetadata("Patients")
            .getColumn("visits"));
  }
}
