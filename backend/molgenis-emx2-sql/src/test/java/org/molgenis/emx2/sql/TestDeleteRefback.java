package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Order;
import org.molgenis.emx2.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestDeleteRefback {

  private static Schema schema;
  private static Schema schemaOther;
  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    schemaOther = database.dropCreateSchema(TestRefBack.class.getSimpleName() + "Other");
    schema = database.dropCreateSchema(TestRefBack.class.getSimpleName());
  }

  private static SchemaMetadata getSchemaMetadata() {
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
    return schemaMetadata;
  }

  @Test
  @Order(1)
  public void testRefBackDelete() {
    SchemaMetadata schemaMetadata = getSchemaMetadata();
    schema.migrate(schemaMetadata);

    schemaMetadata.getTableMetadata("Patients visits").drop();
    // Should throw exception as Patient has a refback column (visits)
    assertThrows(SqlMolgenisException.class, () -> schema.migrate(schemaMetadata));

    // Drop the refback and try again
    schemaMetadata.getTableMetadata("Patients").dropColumn("visits");
    schema.migrate(schemaMetadata);

    // Refresh the schema
    SchemaMetadata schemaMetadataNew = getSchemaMetadata();
    schema.migrate(schemaMetadataNew);
    // Drop both tables should work
    schemaMetadataNew.getTableMetadata("Patients").drop();
    schemaMetadataNew.getTableMetadata("Patients visits").drop();
    schema.migrate(schemaMetadataNew);
  }

  @Test
  @Order(2)
  public void testRefbackDeleteOtherSchema() {
    SchemaMetadata schemaMetadata = getSchemaMetadata();
    schema.migrate(schemaMetadata);

    // Create patient visits table on another schema
    TableMetadata patientVisits = schemaMetadata.getTableMetadata("Patients visits");
    Column patient = patientVisits.getColumn("patient");
    patient.setRefSchemaName(TestRefBack.class.getSimpleName());
    patientVisits.alterColumn(patient);
    SchemaMetadata schemaMetadataOther = new SchemaMetadata();
    schemaMetadataOther.create(patientVisits);
    schemaOther.migrate(schemaMetadataOther);

    // Set refback to schemaOther
    Column visitsRefBack = schema.getTable("Patients").getMetadata().getColumn("visits");
    visitsRefBack.setRefSchemaName(TestRefBack.class.getSimpleName() + "Other");
    schemaMetadata.getTableMetadata("Patients").alterColumn(visitsRefBack);
    schema.migrate(schemaMetadata);

    // Delete schemaOther.visits should now fail
    schemaMetadataOther.getTableMetadata("Patients visits").drop();
    assertThrows(SqlMolgenisException.class, () -> schemaOther.migrate(schemaMetadataOther));
  }

  @AfterAll
  public static void after() {
    database.dropSchemaIfExists(TestRefBack.class.getSimpleName() + "Other");
    database.dropSchemaIfExists(TestRefBack.class.getSimpleName());
  }
}
