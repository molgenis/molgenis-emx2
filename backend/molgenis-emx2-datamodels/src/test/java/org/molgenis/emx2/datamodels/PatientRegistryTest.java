package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PatientRegistryTest extends TestLoaders {

  @Test
  public void patientRegistryDemoTestLoader() {
    assertEquals(88, patientRegistryDemo.getTableNames().size());
  }

  @Test
  void testPatientRegistryLoader() {
    // 62 = 61 + the Biobanks subtype table, which PatientRegistry inherits via its
    // DataCatalogueFlat
    // profile tag (Biobanks is DataCatalogueFlat-only).
    assertEquals(62, patientRegistry.getTableNames().size());
  }
}
