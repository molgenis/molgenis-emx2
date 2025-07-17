package org.molgenis.emx2.datamodels;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class PatientRegistryLoader {
  public static void main(String[] args) {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema patientRegistry = database.dropCreateSchema("patientRegistry");
    DataModels.Profile.PATIENT_REGISTRY.getImportTask(patientRegistry, false).run();
  }
}
