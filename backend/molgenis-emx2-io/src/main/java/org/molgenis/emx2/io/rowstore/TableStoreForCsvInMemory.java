package org.molgenis.emx2.io.rowstore;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class TableStoreForCsvInMemory implements TableStore {
  private final Map<String, String> store;
  private Character separator;

  public TableStoreForCsvInMemory() {
    store = new LinkedHashMap<>();
    separator = ',';
  }

  @Override
  public void writeTable(String name, Iterable<Row> rows) {
    try {
      Writer writer = new StringWriter();
      Writer bufferedWriter = new BufferedWriter(writer);
      String existing = "";
      if (store.containsKey(name)) existing = store.get(name);
      CsvTableWriter.write(rows, bufferedWriter, separator);
      bufferedWriter.close();
      store.put(name, existing + writer.toString());
    } catch (IOException ioe) {
      throw new MolgenisException("import failed", ioe);
    }
  }

  @Override
  public Iterable<Row> readTable(String name) {
    if (!store.containsKey(name))
      throw new MolgenisException(
          "Import failed: Table not found. File with name " + name + " doesn't exist");
    Reader reader = new BufferedReader(new StringReader(store.get(name)));

    return CsvTableReader.read(reader, separator);
  }

  @Override
  public void processTable(String name, RowProcessor processor) {
    processor.process(readTable(name).iterator());
  }

  @Override
  public boolean containsTable(String name) {
    return store.containsKey(name);
  }

  public String viewContents(String name) {
    return store.get(name);
  }
}
