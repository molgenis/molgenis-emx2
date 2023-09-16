package org.molgenis.emx2.io.tablestore;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.NameMapper;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;

public class TableStoreForCsvInMemory implements TableStore {
  private final Map<String, String> store;
  private Character separator;

  public TableStoreForCsvInMemory() {
    this(',');
  }

  public TableStoreForCsvInMemory(Character seperator) {
    Objects.requireNonNull(seperator);
    store = new LinkedHashMap<>();
    separator = seperator;
  }

  @Override
  public void writeTable(String name, List<String> columnNames, Iterable<Row> rows) {
    try {
      Writer writer = new StringWriter();
      Writer bufferedWriter = new BufferedWriter(writer);
      String existing = "";
      if (store.containsKey(name)) existing = store.get(name);
      if (rows.iterator().hasNext()) {
        CsvTableWriter.write(rows, columnNames, bufferedWriter, separator);
      } else {
        // only header in case no rows provided
        writer.write(columnNames.stream().collect(Collectors.joining("" + separator)));
      }
      bufferedWriter.close();
      store.put(name, existing + writer.toString());
    } catch (IOException ioe) {
      throw new MolgenisException("export failed", ioe);
    }
  }

  @Override
  public Iterable<Row> readTable(String name) {
    return this.readTable(name, null);
  }

  @Override
  public Iterable<Row> readTable(String name, NameMapper mapper) {
    if (!store.containsKey(name))
      throw new MolgenisException(
          "Import failed: Table not found. File with name " + name + " doesn't exist");
    Reader reader = new BufferedReader(new StringReader(store.get(name)));

    return CsvTableReader.read(reader, mapper);
  }

  @Override
  public void processTable(String name, NameMapper mapper, RowProcessor processor) {
    processor.process(readTable(name, mapper).iterator(), this);
  }

  @Override
  public boolean containsTable(String name) {
    return store.containsKey(name);
  }

  @Override
  public Collection<String> tableNames() {
    return this.store.keySet();
  }

  public String getCsvString(String tableName) {
    return this.store.get(tableName);
  }
}
