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
import org.molgenis.emx2.NameMapper;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.io.readers.RowReaderJackson;

public class TableStoreForCsvFilesDirectory implements TableAndFileStore {
  public static final String CSV_EXTENSION = ".csv";
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
        CsvTableWriter.write(rows, columnNames, writer, separator);
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
        try (OutputStream out = Files.newOutputStream(file)) {
          out.write(contents);
          out.flush();
        }
      } catch (Exception e) {
        throw new MolgenisException("Writing of file " + filePath + " failed: ", e);
      }
    }
  }

  @Override
  public Iterable<Row> readTable(String name) {
    return readTable(name, null);
  }

  @Override
  public List<Row> readTable(String name, NameMapper mapper) {
    Path relativePath = directoryPath.resolve(name + CSV_EXTENSION);
    try {
      Reader reader = Files.newBufferedReader(relativePath);
      return RowReaderJackson.readList(reader, mapper, separator);
    } catch (Exception ioe) {
      throw new MolgenisException("Import '" + name + "' failed: " + ioe.getMessage(), ioe);
    }
  }

  @Override
  public void processTable(String name, NameMapper mapper, RowProcessor processor) {
    processor.process(readTable(name, mapper).iterator(), this);
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
      String name = f.getName();
      if (name.endsWith(CSV_EXTENSION)) {
        result.add(name.substring(0, name.length() - CSV_EXTENSION.length()));
      }
    }
    return result;
  }

  @Override
  public BinaryFileWrapper getBinaryFileWrapper(String name) {
    Path fileDir = directoryPath.resolve("_files");
    try (Stream<Path> stream = Files.list(fileDir)) {
      List<Path> result =
          stream.filter(f -> f.getFileName().toString().startsWith(name + ".")).toList();
      if (result.isEmpty()) {
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
