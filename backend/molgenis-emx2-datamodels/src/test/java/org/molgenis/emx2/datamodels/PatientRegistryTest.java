package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PatientRegistryTest extends TestLoaders {

  @Test
  public void patientRegistryDemoTestLoader() {
    assertEquals(86, patientRegistryDemo.getTableNames().size());
  }

  @Test
  void testPatientRegistryLoader() {
    assertEquals(48, patientRegistry.getTableNames().size());
  }
}
