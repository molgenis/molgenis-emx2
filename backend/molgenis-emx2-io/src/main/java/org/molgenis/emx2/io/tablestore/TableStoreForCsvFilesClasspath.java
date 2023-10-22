package org.molgenis.emx2.io.tablestore;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.RowReaderJackson;

public class TableStoreForCsvFilesClasspath implements TableAndFileStore {
  public static final String CSV_EXTENSION = ".csv";
  private final String directoryPath;
  private final Character separator;

  public TableStoreForCsvFilesClasspath(String directoryPath, Character separator) {
    if (!directoryPath.startsWith("/")) {
      directoryPath = "/" + directoryPath;
    }
    this.directoryPath = directoryPath;
    if (getClass().getResource(directoryPath) == null)
      throw new MolgenisException(
          "Import failed: Directory " + directoryPath + " doesn't exist in classpath");
    this.separator = separator;
  }

  public TableStoreForCsvFilesClasspath(String directoryPath) {
    this(directoryPath, ',');
  }

  @Override
  public void writeTable(String name, List<String> columnNames, Iterable<Row> rows) {
    throw new UnsupportedOperationException("Cannot write to classpath");
  }

  public void writeFile(String filePath, byte[] contents) {
    throw new UnsupportedOperationException("Cannot write to classpath");
  }

  @Override
  public List<Row> readTable(String name) {
    String path = directoryPath + "/" + name + CSV_EXTENSION;
    try {
      Reader reader = new InputStreamReader(getClass().getResourceAsStream(path));
      return RowReaderJackson.readList(reader, separator);
    } catch (Exception ioe) {
      throw new MolgenisException("Import '" + name + "' failed: " + ioe.getMessage(), ioe);
    }
  }

  @Override
  public void processTable(String name, RowProcessor processor) {
    processor.process(readTable(name).iterator(), this);
  }

  @Override
  public boolean containsTable(String name) {
    String path = directoryPath + "/" + name + CSV_EXTENSION;
    return getClass().getResource(path) != null;
  }

  @Override
  public Collection<String> tableNames() {
    throw new UnsupportedOperationException("Cannot list files in classpath");
  }

  @Override
  public BinaryFileWrapper getBinaryFileWrapper(String name) {
    URL url = getClass().getResource(directoryPath + "/_files");
    String path = url.getPath();
    List<File> result =
        Arrays.stream(new File(path).listFiles())
            .filter(f -> f.getName().toString().startsWith(name + "."))
            .toList();
    if (result.isEmpty()) {
      throw new MolgenisException("File not found for id " + name);
    } else if (result.size() == 1) {
      return new BinaryFileWrapper(result.get(0));
    } else {
      throw new MolgenisException(
          "File cannot be retrieved for id " + name + ": name is not unique");
    }
  }
}
