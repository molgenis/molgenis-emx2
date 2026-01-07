package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PagesTest extends TestLoaders {
  @Test
  public void pagesDemoTestLoader() {
    assertEquals(22, pagesSchema.getTableNames().size());
  }
}
