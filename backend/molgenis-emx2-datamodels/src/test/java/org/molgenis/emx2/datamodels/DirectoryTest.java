package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DirectoryTest extends TestLoaders {

  @Test
  public void test09DirectoryLoader() {
    assertEquals(13, directory.getTableNames().size());
  }

  @Test
  void test15DirectoryStagingLoader() {
    assertEquals(8, directoryStaging.getTableNames().size());
  }
}
