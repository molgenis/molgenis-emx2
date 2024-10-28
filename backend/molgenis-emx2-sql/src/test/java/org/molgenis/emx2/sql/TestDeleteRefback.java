package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestDeleteRefback {

  private static Schema schema;

  @BeforeAll
  public static void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestRefBack.class.getSimpleName());
  }

  @Test
  public void testRefBackDelete() {
    TableMetadata patients = table("Patients").add(column("id").setPkey()).add(column("patient"));

    TableMetadata patientVisits =
        table("Patients visits")
            .add(column("visit id").setPkey())
            .add(column("patient").setType(REF).setRefTable(patients.getTableName()));

    patients.add(
        column("visits")
            .setType(REFBACK)
            .setRefTable(patientVisits.getTableName())
            .setRefBack("patient"));

    SchemaMetadata schemaMetadata = new SchemaMetadata();
    schemaMetadata.create(patients, patientVisits);
    schema.migrate(schemaMetadata);

    schemaMetadata.getTableMetadata("Patients visits").drop();
    // Should throw exception as Patient has a refback column (visits)
    assertThrows(SqlMolgenisException.class, () -> schema.migrate(schemaMetadata));

    // Drop the refback and try again
    schemaMetadata.getTableMetadata("Patients").dropColumn("visits");
    schema.migrate(schemaMetadata);
  }
}
