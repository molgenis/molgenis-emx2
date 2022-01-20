package org.molgenis.emx2.io.tablestore;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.io.readers.RowReaderJackson;

public class TableStoreForCsvFilesDirectory implements TableStore {
  static final String CSV_EXTENSION = ".csv";
  private final Path directoryPath;
  private final Character separator;

  public TableStoreForCsvFilesDirectory(Path directoryPath, Character separator) {
    this.directoryPath = directoryPath;
    if (!directoryPath.toFile().exists())
      throw new MolgenisException("Import failed: Directory " + directoryPath + " doesn't exist");
    this.separator = separator;
  }

  public TableStoreForCsvFilesDirectory(Path directoryPath) {
    this(directoryPath, ',');
  }

  @Override
  public void writeTable(String name, List<String> columnNames, Iterable<Row> rows) {
    Path relativePath = directoryPath.resolve(name + CSV_EXTENSION);
    try {
      Writer writer = Files.newBufferedWriter(relativePath);
      if (rows.iterator().hasNext()) {
        CsvTableWriter.write(rows, writer, separator);
      } else {
        // only header in case no rows provided
        writer.write(columnNames.stream().collect(Collectors.joining("" + separator)));
      }
      writer.close();
    } catch (IOException ioe) {
      throw new MolgenisException("Export failed", ioe);
    }
  }

  public void writeFile(String filePath, byte[] contents) {
    if (contents != null && contents.length > 0) {
      try {
        Path dir = directoryPath.resolve("_files");
        if (!Files.exists(dir)) {
          Files.createDirectories(dir);
        }
        Path file = directoryPath.resolve(filePath);
        OutputStream out = Files.newOutputStream(file);
        out.write(contents);
        out.flush();
        out.close();
      } catch (Exception e) {
        new MolgenisException("Writing of file " + filePath + " failed: ", e);
      }
    }
  }

  @Override
  public List<Row> readTable(String name) {
    Path relativePath = directoryPath.resolve(name + CSV_EXTENSION);
    try {
      Reader reader = Files.newBufferedReader(relativePath);
      return RowReaderJackson.readList(reader, separator);
    } catch (IOException ioe) {
      throw new MolgenisException(
          "Import failed: Table not found. File with name '"
              + name
              + "' doesn't exist. "
              + ioe.getMessage(),
          ioe);
    }
  }

  @Override
  public void processTable(String name, RowProcessor processor) {
    processor.process(readTable(name).iterator(), this);
  }

  @Override
  public boolean containsTable(String name) {
    Path path = directoryPath.resolve(name + CSV_EXTENSION);
    return path.toFile().exists();
  }

  @Override
  public Collection<String> tableNames() {
    List<String> result = new ArrayList<>();
    for (File f : directoryPath.toFile().listFiles()) {
      result.add(f.getName()); // todo strip extension
    }
    return result;
  }

  @Override
  public BinaryFileWrapper getBinaryFileWrapper(String name) {
    Path fileDir = directoryPath.resolve("_files");
    try (Stream<Path> stream = Files.list(fileDir)) {
      List<Path> result =
          stream
              .filter(f -> f.getFileName().toString().startsWith(name + "."))
              .collect(Collectors.toList());
      if (result.size() == 0) {
        throw new MolgenisException("File not found for id " + name);
      } else if (result.size() == 1) {
        return new BinaryFileWrapper(result.get(0).toFile());
      } else {
        throw new MolgenisException(
            "File cannot be retrieved for id " + name + ": name is not unique");
      }
    } catch (IOException e) {
      throw new MolgenisException("Error retrieving file " + name, e);
    }
  }
}
