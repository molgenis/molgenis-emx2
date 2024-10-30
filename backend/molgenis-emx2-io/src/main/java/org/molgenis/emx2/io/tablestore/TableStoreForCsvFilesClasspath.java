package org.molgenis.emx2.io.tablestore;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableStoreForCsvFilesClasspath implements TableAndFileStore {
  public static final String CSV_EXTENSION = ".csv";
  private static final Logger log = LoggerFactory.getLogger(TableStoreForCsvFilesClasspath.class);
  private final String directoryPath;
  private final Character separator;
  private static final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();
  private final List<String> storefiles;

  public TableStoreForCsvFilesClasspath(String directoryPath, Character separator) {
    if (!directoryPath.startsWith("/")) {
      directoryPath = "/" + directoryPath;
    }
    this.directoryPath = directoryPath;
    if (getClass().getResource(directoryPath) == null)
      throw new MolgenisException(
          "Import failed: Directory " + directoryPath + " doesn't exist in classpath");
    this.separator = separator;
    try {
      storefiles = jarSaveListFiles(directoryPath + "/_files/");
    } catch (Exception e) {
      throw new MolgenisException("File listing failed: " + e.getMessage(), e);
    }
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
  public Iterable<Row> readTable(String name) {
    String path = directoryPath + "/" + name + CSV_EXTENSION;
    try {
      Reader reader = new InputStreamReader(getClass().getResourceAsStream(path));
      return CsvTableReader.read(reader);
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
  public Collection<String> getTableNames() {
    throw new UnsupportedOperationException("Cannot list files in classpath");
  }

  @Override
  public BinaryFileWrapper getBinaryFileWrapper(String name) {
    try {
      String fileName =
          storefiles.stream().filter(f -> f.startsWith(name + ".")).findFirst().orElseThrow();
      byte[] bytes;
      try (InputStream stream =
          getClass().getResourceAsStream(directoryPath + "/_files/" + fileName)) {
        bytes = Objects.requireNonNull(stream).readAllBytes();
      }
      String mimetype = URLConnection.getFileNameMap().getContentTypeFor(fileName);
      return new BinaryFileWrapper(mimetype, fileName, bytes);

    } catch (Exception ioe) {
      throw new MolgenisException("Import '" + name + "' failed: " + ioe.getMessage(), ioe);
    }
  }

  /** List files in a directory. This method is safe to use in a jar file. */
  public List<String> jarSaveListFiles(String path) throws Exception {

    URL url = getClass().getResource(path);
    if (url == null) {
      // no files directory
      return Collections.emptyList();
    }

    URI uri = url.toURI();
    List<String> files;
    if ("jar".equals(uri.getScheme())) {
      files = safeWalkJar(path, uri);
    } else {
      return Arrays.stream(Objects.requireNonNull(new File(url.getPath()).listFiles()))
          .map(File::getName)
          .toList();
    }
    return files;
  }

  private List<String> safeWalkJar(String path, URI uri) throws Exception {
    synchronized (getLock(uri)) {
      // this'll close the FileSystem object at the end
      try (FileSystem fs = getFileSystem(uri);
          Stream<Path> walk = Files.walk(fs.getPath(path)); ) {
        return walk.map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
      }
    }
  }

  private Object getLock(URI uri) {
    String fileName = parseFileName(uri);
    locks.computeIfAbsent(fileName, s -> new Object());
    return locks.get(fileName);
  }

  private String parseFileName(URI uri) {
    String schemeSpecificPart = uri.getSchemeSpecificPart();
    return schemeSpecificPart.substring(0, schemeSpecificPart.indexOf("!"));
  }

  private FileSystem getFileSystem(URI uri) throws IOException {
    try {
      return FileSystems.getFileSystem(uri);
    } catch (FileSystemNotFoundException e) {
      return FileSystems.newFileSystem(uri, Collections.<String, String>emptyMap());
    }
  }
}
