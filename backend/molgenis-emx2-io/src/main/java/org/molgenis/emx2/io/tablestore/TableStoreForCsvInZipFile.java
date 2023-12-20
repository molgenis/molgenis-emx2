package org.molgenis.emx2.io.tablestore;

import java.io.*;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.readers.CsvTableWriter;

public class TableStoreForCsvInZipFile implements TableAndFileStore {
  static final String CSV_EXTENSION = ".csv";
  static final String TSV_EXTENSION = ".tsv";
  private final Path zipFilePath;
  private static final Character COMMA = ',';

  public TableStoreForCsvInZipFile(Path zipFilePath) {
    this.zipFilePath = zipFilePath;
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

  public void writeFile(String filePath, byte[] contents) {
    if (contents != null && contents.length > 0) {
      try (FileSystem zipfs = open()) {
        // dir exist?
        Path dir = zipfs.getPath("_files");
        if (!Files.exists(dir)) {
          Files.createDirectories(dir);
        }
        Path pathInZipfile = zipfs.getPath(filePath);
        OutputStream out = Files.newOutputStream(pathInZipfile);
        out.write(contents);
        out.flush();
        out.close();
      } catch (IOException ioe) {
        throw new MolgenisException("File export failed", ioe);
      }
    }
  }

  @Override
  public void writeTable(String name, List<String> columnNames, Iterable<Row> rows) {
    if (columnNames.isEmpty()) {
      return;
    }
    if (!Files.exists(zipFilePath)) {
      create();
    }
    try (FileSystem zipfs = open()) {
      Path pathInZipfile = zipfs.getPath(File.separator + name + CSV_EXTENSION);
      Writer writer = Files.newBufferedWriter(pathInZipfile);
      if (rows.iterator().hasNext()) {
        CsvTableWriter.write(rows, columnNames, writer, COMMA);
      } else {
        // only header in case no rows provided
        writer.write(columnNames.stream().collect(Collectors.joining("" + COMMA)));
      }
      writer.close();
    } catch (IOException ioe) {
      throw new MolgenisException("Import failed", ioe);
    }
  }

  @Override
  public void processTable(String name, RowProcessor processor) {
    try (ZipFile zf = new ZipFile(zipFilePath.toFile())) {
      ZipEntry entry = getEntry(zf, name);
      Reader reader = new BufferedReader(new InputStreamReader(zf.getInputStream(entry)));
      if (entry != null && entry.getName().endsWith(CSV_EXTENSION)) {
        processor.process(CsvTableReader.read(reader).iterator(), this);
      } else if (entry != null && entry.getName().endsWith(TSV_EXTENSION)) {
        processor.process(CsvTableReader.read(reader).iterator(), this);
      } else {
        throw new MolgenisException(
            String.format(
                "Import failed: Table '%s' has unsupported extension (should be .csv or .tsv)",
                name));
      }
    } catch (IOException e) {
      throw new MolgenisException(
          String.format("Import failed: Table '%s' not found in file. ", name), e);
    }
  }

  @Override
  public Iterable<Row> readTable(String name) {
    try (ZipFile zf = new ZipFile(zipFilePath.toFile())) {
      ZipEntry entry = getEntry(zf, name);
      Reader reader = new BufferedReader(new InputStreamReader(zf.getInputStream(entry)));
      if (entry != null && entry.getName().endsWith(CSV_EXTENSION)) {
        return StreamSupport.stream(CsvTableReader.read(reader).spliterator(), false).toList();
      } else if (entry != null && entry.getName().endsWith(TSV_EXTENSION)) {
        return StreamSupport.stream(CsvTableReader.read(reader).spliterator(), false).toList();
      } else {
        throw new MolgenisException(
            String.format(
                "Import failed: Table '%s' has unsupported extension (should be .csv or .tsv)",
                name));
      }
    } catch (Exception e) {
      throw new MolgenisException(
          String.format("Import failed: Table '%s'not found in file. ", name), e);
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

  @Override
  public Collection<String> tableNames() {
    List<String> result = new ArrayList<>();
    try (ZipFile zf = new ZipFile(zipFilePath.toFile())) {
      zf.stream()
          .forEach(
              e -> {
                String name = e.getName();
                if (name.toLowerCase().endsWith(".csv") || name.toLowerCase().endsWith(".tsv")) {
                  name = name.substring(0, name.length() - 4);
                }
                result.add(name);
              });
    } catch (IOException ioe) {
      throw new MolgenisException("Import failed: ", ioe);
    }
    return result;
  }

  @Override
  public BinaryFileWrapper getBinaryFileWrapper(String name) {
    try (ZipFile zf = new ZipFile(zipFilePath.toFile())) {
      ZipEntry entry = getEntry(zf, name);
      if (entry != null) {
        String contentType = URLConnection.guessContentTypeFromName(entry.getName());
        InputStream contents = zf.getInputStream(entry);
        return new BinaryFileWrapper(contentType, entry.getName(), contents.readAllBytes());
      } else {
        throw new MolgenisException("Import failed: file '" + name + "' not found in file.");
      }
    } catch (Exception e) {
      throw new MolgenisException("Import failed: file '" + name + "' resulted in error: ", e);
    }
  }

  // magic function to allow file in subfolder
  private ZipEntry getEntry(ZipFile zf, String name) {
    List<? extends ZipEntry> result =
        // find all files that have name as prefix
        zf.stream().filter(e -> new File(e.getName()).getName().startsWith(name + ".")).toList();
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
