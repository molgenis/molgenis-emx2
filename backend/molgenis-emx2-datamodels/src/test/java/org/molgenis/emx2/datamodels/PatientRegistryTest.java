package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PATIENT_REGISTRY;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PatientRegistryTest {

  protected static Database database;
  protected static Schema patientRegistrySchema;

  public static final String SCHEMA_NAME = "patientRegistryTest";

  @BeforeAll
  public void setup() {
    if (database == null) {
      database = TestDatabaseFactory.getTestDatabase();
      //      patientRegistrySchema = database.getSchema(SCHEMA_NAME);
      patientRegistrySchema = database.dropCreateSchema(SCHEMA_NAME);
      PATIENT_REGISTRY.getImportTask(patientRegistrySchema, true).run();
    }
  }

  @Test
  void testSchema() {
    assertEquals(49, patientRegistrySchema.getTableNames().size());
  }
}
