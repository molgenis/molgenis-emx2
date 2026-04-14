package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CoreRd3Test extends TestLoaders {
  @Test
  void testCoreRD3Loader() {
    assertEquals(11, coreRd3.getTableNames().size());
  }
}
