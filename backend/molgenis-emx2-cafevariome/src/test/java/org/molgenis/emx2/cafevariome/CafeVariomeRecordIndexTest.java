package org.molgenis.emx2.cafevariome;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.datamodels.TestLoaders;

public class CafeVariomeRecordIndexTest extends TestLoaders {

  @Test
  public void testRecordIndex() {
    assertEquals(86, patientRegistryDemo.getTableNames().size());
  }
}
