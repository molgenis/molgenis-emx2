package org.molgenis.emx2.io.rowstore;

import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;

import java.io.*;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class TableStoreForCsvInZipFile implements TableStore {
  static final String CSV_EXTENSION = ".csv";
  private Path zipFilePath;
  private Character separator;

  public TableStoreForCsvInZipFile(Path zipFilePath, Character separator) {
    this.zipFilePath = zipFilePath;
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
      throw new MolgenisException("Import failed", ioe);
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
      if (!Files.exists(zipFilePath)) {
        create();
      }
      try (FileSystem zipfs = open()) {
        Path pathInZipfile = zipfs.getPath(File.separator + name + CSV_EXTENSION);
        Writer writer = Files.newBufferedWriter(pathInZipfile);
        CsvTableWriter.rowsToCsv(rows, writer, separator);
        writer.close();
      } catch (IOException ioe) {
        throw new MolgenisException("Import failed", ioe.getMessage(), ioe);
      }
    }
  }

  @Override
  public List<Row> readTable(String name) {
    try (ZipFile zf = new ZipFile(zipFilePath.toFile())) {
      ZipEntry entry = getEntry(zf, name);
      Reader reader = new BufferedReader(new InputStreamReader(zf.getInputStream(entry)));
      return CsvTableReader.readList(reader, separator);
    } catch (Exception e) {
      throw new MolgenisException("Import failed: Table '" + name + "' not found in file. ", e);
    }
  }

  @Override
  public boolean containsTable(String name) {
    try (ZipFile zf = new ZipFile(zipFilePath.toFile())) {
      ZipEntry entry = getEntry(zf, name);
      return entry != null;
    } catch (IOException ioe) {
      throw new MolgenisException("Import failed: ", ioe);
    }
  }

  // magic functiont to allow file in subfolder
  private ZipEntry getEntry(ZipFile zf, String name) {
    List<ZipEntry> result =
        zf.stream()
            .filter(e -> e.getName().endsWith(File.separator + name + CSV_EXTENSION))
            .collect(Collectors.toList());
    if (result.size() > 1) {
      throw new MolgenisException(
          "Import failed, contains multiple files of name " + name + " in different subfolders");
    }
    if (result.size() == 1) {
      return result.get(0);
    }
    return null;
  }
}
