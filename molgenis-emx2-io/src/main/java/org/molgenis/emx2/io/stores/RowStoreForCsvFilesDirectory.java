package org.molgenis.emx2.io.stores;

import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvRowReader;
import org.molgenis.emx2.io.readers.CsvRowWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RowStoreForCsvFilesDirectory implements RowStore {
  static final String CSV_EXTENSION = ".readers";

  private final Path directoryPath;

  public RowStoreForCsvFilesDirectory(Path directoryPath) throws IOException {
    this.directoryPath = directoryPath;
    if (!directoryPath.toFile().exists())
      throw new IOException("Directory " + directoryPath + " doesn't exist");
  }

  @Override
  public void write(String name, List<Row> rows) throws IOException {
    if (rows.isEmpty()) return;
    Path relativePath = directoryPath.resolve(name + CSV_EXTENSION);
    Writer writer = Files.newBufferedWriter(relativePath);
    CsvRowWriter.writeCsv(rows, writer);
    writer.close();
  }

  @Override
  public List<Row> read(String name) throws IOException {
    Path relativePath = directoryPath.resolve(name + CSV_EXTENSION);
    Reader reader = Files.newBufferedReader(relativePath);
    return CsvRowReader.readList(reader);
  }

  @Override
  public boolean containsTable(String name) throws IOException {
    Path path = directoryPath.resolve(name + CSV_EXTENSION);
    return path.toFile().exists();
  }
}
