package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class TestDeleteRefback {

  private static Schema schema;

  @BeforeAll
  public static void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestRefBack.class.getSimpleName());
  }

  @Test
  public void testRefBackDelete() {

    Table patients =
        schema.create(table("Patients").add(column("id").setPkey()).add(column("patient")));

    Table patientVisits =
        schema.create(
            table("Patients visits")
                .add(column("visit id").setPkey())
                .add(column("patient").setType(REF).setRefTable(patients.getName())));

    patients
        .getMetadata()
        .add(
            column("visits")
                .setType(REFBACK)
                .setRefTable(patientVisits.getName())
                .setRefBack("patient"));

    assertNotNull(patients.getMetadata().getColumn("visits"));

    patients.getMetadata().dropColumn("visits");

    Column column = patients.getMetadata().getColumn("visits");

    assertNull(column);
  }
}
