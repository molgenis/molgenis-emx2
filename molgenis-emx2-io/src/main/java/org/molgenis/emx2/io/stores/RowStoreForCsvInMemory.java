package org.molgenis.emx2.io.stores;

import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.ErrorCodes;
import org.molgenis.emx2.io.readers.CsvRowReader;
import org.molgenis.emx2.io.readers.CsvRowWriter;
import org.molgenis.emx2.utils.MolgenisException;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RowStoreForCsvInMemory implements RowStore {
  private final Map<String, String> store;
  private Character separator;

  public RowStoreForCsvInMemory() {
    store = new LinkedHashMap<>();
    separator = ',';
  }

  @Override
  public void write(String name, List<Row> rows) {
    try {
      Writer writer = new StringWriter();
      Writer bufferedWriter = new BufferedWriter(writer);
      String existing = "";
      if (store.containsKey(name)) existing = store.get(name);
      CsvRowWriter.writeCsv(rows, bufferedWriter, separator);
      bufferedWriter.close();
      store.put(name, existing + writer.toString());
    } catch (IOException ioe) {
      throw new MolgenisException(
          ErrorCodes.IO_EXCEPTION, ErrorCodes.IO_EXCEPTION_MESSAGE, ioe.getMessage(), ioe);
    }
  }

  @Override
  public List<Row> read(String name) {
    if (!store.containsKey(name))
      throw new MolgenisException(
          "not_found", "Not found", "CsvStringStore with name " + name + " doesn't exist");
    Reader reader = new BufferedReader(new StringReader(store.get(name)));

    return CsvRowReader.readList(reader, separator);
  }

  @Override
  public boolean containsTable(String name) {
    return store.containsKey(name);
  }

  public String viewContents(String name) {
    return store.get(name);
  }
}
