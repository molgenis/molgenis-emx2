package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PatientRegistryTest extends TestLoaders {

  @Test
  public void patientRegistryDemoTestLoader() {
    assertEquals(86, patientRegistryDemo.getTableNames().size());
  }

  @Test
  void testPatientRegistryLoader() {
    assertEquals(49, patientRegistry.getTableNames().size());
  }
}
