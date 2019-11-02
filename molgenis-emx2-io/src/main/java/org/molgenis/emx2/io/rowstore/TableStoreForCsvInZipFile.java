package org.molgenis.emx2.io.rowstore;

import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.ErrorCodes;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.utils.MolgenisException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableStoreForCsvInZipFile implements TableStore {
  static final String CSV_EXTENSION = ".csv";
  private Path zipFilePath;
  private Character separator;

  public TableStoreForCsvInZipFile(Path zipFilePath, Character separator) {
    this.zipFilePath = zipFilePath;
    this.create();
    this.separator = separator;
  }

  public TableStoreForCsvInZipFile(Path zipFilePath) {
    this(zipFilePath, ',');
  }

  private void create() {
    Map<String, String> env = new HashMap<>();
    env.put("create", "true");
    final URI zipUri = URI.create("jar:" + zipFilePath.toUri());
    try {
      FileSystem zipfs = FileSystems.newFileSystem(zipUri, env, null);
      zipfs.close();
    } catch (IOException ioe) {
      throw new MolgenisException(ioe);
    }
  }

  private FileSystem open() throws IOException {
    Map<String, String> env = new HashMap<>();
    env.put("create", "false");
    final URI zipUri = URI.create("jar:" + zipFilePath.toUri());
    return FileSystems.newFileSystem(zipUri, env, null);
  }

  @Override
  public void writeTable(String name, List<Row> rows) {
    if (!rows.isEmpty()) {
      try (FileSystem zipfs = open()) {
        Path pathInZipfile = zipfs.getPath(File.separator + name + CSV_EXTENSION);
        Writer writer = Files.newBufferedWriter(pathInZipfile);
        CsvTableWriter.rowsToCsv(rows, writer, separator);
        writer.close();
      } catch (IOException ioe) {
        throw new MolgenisException(
            ErrorCodes.IO_EXCEPTION, ErrorCodes.IO_EXCEPTION_MESSAGE, ioe.getMessage(), ioe);
      }
    }
  }

  @Override
  public List<Row> readTable(String name) {
    try (FileSystem zipfs = open()) {
      Path pathInZipfile = zipfs.getPath(File.separator + name + CSV_EXTENSION);
      Reader reader = Files.newBufferedReader(pathInZipfile);
      return CsvTableReader.readList(reader, separator);
    } catch (IOException ioe) {
      throw new MolgenisException(
          ErrorCodes.NOT_FOUND,
          ErrorCodes.NOT_FOUND_MESSAGE,
          "CsvStringStore with name '" + name + "' doesn't exist. " + ioe.getMessage(),
          ioe);
    }
  }

  @Override
  public boolean containsTable(String name) {
    try (FileSystem zipfs = open()) {
      Path path = zipfs.getPath(File.separator + name + CSV_EXTENSION);
      return Files.exists(path);
    } catch (IOException ioe) {
      throw new MolgenisException(
          ErrorCodes.IO_EXCEPTION, ErrorCodes.IO_EXCEPTION_MESSAGE, ioe.getMessage(), ioe);
    }
  }
}
