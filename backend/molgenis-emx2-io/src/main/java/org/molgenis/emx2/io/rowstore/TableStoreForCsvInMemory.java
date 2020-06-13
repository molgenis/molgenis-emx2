package org.molgenis.emx2.io.rowstore;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.molgenis.emx2.io.emx2.Emx2.IMPORT_FAILED;

public class TableStoreForCsvInMemory implements TableStore {
  private final Map<String, String> store;
  private Character separator;

  public TableStoreForCsvInMemory() {
    store = new LinkedHashMap<>();
    separator = ',';
  }

  @Override
  public void writeTable(String name, List<Row> rows) {
    try {
      Writer writer = new StringWriter();
      Writer bufferedWriter = new BufferedWriter(writer);
      String existing = "";
      if (store.containsKey(name)) existing = store.get(name);
      CsvTableWriter.rowsToCsv(rows, bufferedWriter, separator);
      bufferedWriter.close();
      store.put(name, existing + writer.toString());
    } catch (IOException ioe) {
      throw new MolgenisException(IMPORT_FAILED, ioe.getMessage(), ioe);
    }
  }

  @Override
  public List<Row> readTable(String name) {
    if (!store.containsKey(name))
      throw new MolgenisException(
          IMPORT_FAILED, "Table not found. File with name " + name + " doesn't exist");
    Reader reader = new BufferedReader(new StringReader(store.get(name)));

    return CsvTableReader.readList(reader, separator);
  }

  @Override
  public boolean containsTable(String name) {
    return store.containsKey(name);
  }

  public String viewContents(String name) {
    return store.get(name);
  }
}
