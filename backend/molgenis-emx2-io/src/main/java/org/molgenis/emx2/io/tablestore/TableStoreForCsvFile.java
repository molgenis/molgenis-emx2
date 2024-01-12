package org.molgenis.emx2.io.tablestore;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;

/** can only handle one table */
public class TableStoreForCsvFile implements TableStore {
  private Path csvFile;

  public TableStoreForCsvFile(Path csvFile) {
    this.csvFile = csvFile;
  }

  @Override
  public void writeTable(String name, List<String> columnNames, Iterable<Row> rows) {
    try {
      Writer writer = Files.newBufferedWriter(csvFile);
      if (rows.iterator().hasNext()) {
        CsvTableWriter.write(rows, columnNames, writer, ',');
      } else {
        // only header in case no rows provided
        writer.write(columnNames.stream().collect(Collectors.joining(",")));
      }
      writer.close();
    } catch (IOException ioe) {
      throw new MolgenisException("Export failed", ioe);
    }
  }

  @Override
  public Iterable<Row> readTable(String name) {
    try {
      return CsvTableReader.read(csvFile.toFile());
    } catch (Exception e) {
      throw new MolgenisException("Parse of CSV file failed", e);
    }
  }

  @Override
  public void processTable(String name, RowProcessor processor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsTable(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<String> tableNames() {
    return List.of(csvFile.toFile().getName());
  }
}
