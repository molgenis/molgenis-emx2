package org.molgenis.emx2.io.tablestore;

import org.molgenis.emx2.BinaryFileWrapper;

public interface TableAndFileStore extends TableStore {
  void writeFile(String fileName, byte[] binary);

  BinaryFileWrapper getBinaryFileWrapper(String name);
}
