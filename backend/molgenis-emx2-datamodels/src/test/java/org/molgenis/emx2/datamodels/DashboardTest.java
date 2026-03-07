package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DashboardTest extends TestLoaders {

  @Test
  public void dashboardTestLoader() {
    assertEquals(7, dashboard.getTableNames().size());
  }
}
