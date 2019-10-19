package org.molgenis.emx2.io.stores;

import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvRowReader;
import org.molgenis.emx2.io.readers.CsvRowWriter;
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

public class RowStoreForCsvInZipFile implements RowStore {
  static final String CSV_EXTENSION = ".csv";
  private Path zipFilePath;
  private Character separator;

  public RowStoreForCsvInZipFile(Path zipFilePath, Character separator) throws IOException {
    this.zipFilePath = zipFilePath;
    this.create();
    this.separator = separator;
  }

  public RowStoreForCsvInZipFile(Path zipFilePath) throws IOException {
    this(zipFilePath, ',');
  }

  private void create() throws IOException {
    Map<String, String> env = new HashMap<>();
    env.put("create", "true");
    final URI zipUri = URI.create("jar:" + zipFilePath.toUri());
    FileSystem zipfs = FileSystems.newFileSystem(zipUri, env, null);
    zipfs.close();
  }

  private FileSystem open() throws IOException {
    Map<String, String> env = new HashMap<>();
    env.put("create", "false");
    final URI zipUri = URI.create("jar:" + zipFilePath.toUri());
    return FileSystems.newFileSystem(zipUri, env, null);
  }

  @Override
  public void write(String name, List<Row> rows) {
    if (!rows.isEmpty()) {
      try (FileSystem zipfs = open()) {
        Path pathInZipfile = zipfs.getPath(File.separator + name + CSV_EXTENSION);
        Writer writer = Files.newBufferedWriter(pathInZipfile);
        CsvRowWriter.writeCsv(rows, writer, separator);
        writer.close();
      } catch (IOException ioe) {
        throw new MolgenisException("io_exception", "IO exception", ioe.getMessage(), ioe);
      }
    }
  }

  @Override
  public List<Row> read(String name) {
    try (FileSystem zipfs = open()) {
      Path pathInZipfile = zipfs.getPath(File.separator + name + CSV_EXTENSION);
      Reader reader = Files.newBufferedReader(pathInZipfile);
      return CsvRowReader.readList(reader, separator);
    } catch (IOException ioe) {
      throw new MolgenisException("io_exception", "IO exception", ioe.getMessage(), ioe);
    }
  }

  @Override
  public boolean containsTable(String name) {
    try (FileSystem zipfs = open()) {
      Path path = zipfs.getPath(File.separator + name + CSV_EXTENSION);
      return Files.exists(path);
    } catch (IOException ioe) {
      throw new MolgenisException("io_exception", "IO exception", ioe.getMessage(), ioe);
    }
  }
}
