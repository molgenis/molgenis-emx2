package org.molgenis.emx2.io;

import org.molgenis.MolgenisException;
import org.molgenis.Row;
import org.molgenis.emx2.io.csv.CsvRowReader;
import org.molgenis.emx2.io.csv.CsvRowWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RowStoreForCsvFilesDirectory implements RowStore {
  private final Path directoryPath;

  public RowStoreForCsvFilesDirectory(Path directoryPath) throws MolgenisException {
    this.directoryPath = directoryPath;
    if (directoryPath.toFile().exists()) {
      throw new MolgenisException("Directory '" + directoryPath + "' already exists");
    }
  }

  @Override
  public void write(String name, List<Row> rows) throws IOException {
    Path relativePath = directoryPath.resolve(name + CSV_EXTENSION);
    Files.createDirectories(directoryPath);
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
}
