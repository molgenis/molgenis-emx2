package org.molgenis.emx2.io.tablestore;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.BinaryFileWrapper;

class TableStoreForCsvFilesClasspathTest {

  @Test
  void getBinaryFileWrapper() {
    String directoryPath = "_demodata/applications/imagetest";
    String fileName = "e019a97346d1416f91ba56fe842f44c6";
    TableStoreForCsvFilesClasspath store = new TableStoreForCsvFilesClasspath(directoryPath);
    BinaryFileWrapper binaryFileWrapper = store.getBinaryFileWrapper(fileName);
    assertNotNull(binaryFileWrapper);
  }
}
