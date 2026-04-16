package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CoreRd3Test extends TestLoaders {
  @Test
  void coreRD3TestLoader() {
    assertEquals(11, coreRD3.getTableNames().size());
  }
}
