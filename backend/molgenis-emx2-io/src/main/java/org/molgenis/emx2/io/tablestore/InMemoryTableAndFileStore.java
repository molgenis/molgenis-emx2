package org.molgenis.emx2.io.tablestore;

import java.util.HashMap;
import java.util.Map;
import org.molgenis.emx2.BinaryFileWrapper;

public class InMemoryTableAndFileStore extends TableStoreForCsvInMemory
    implements TableAndFileStore {

  Map<String, byte[]> data = new HashMap<>();

  @Override
  public void writeFile(String fileName, byte[] binary) {
    data.put(fileName, binary);
  }

  @Override
  public BinaryFileWrapper getBinaryFileWrapper(String name) {
    return new BinaryFileWrapper("bytes", name, data.get(name));
  }
}
