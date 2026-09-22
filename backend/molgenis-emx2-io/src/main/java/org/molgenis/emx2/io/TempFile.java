package org.molgenis.emx2.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Currently this class only exist to reduce duplicate code (based on CsvApi implementation).
 * Before harmonization of the different implementations for creating tmp files, this class should
 * be rewritten according to the required specs.
 */
public class TempFile implements AutoCloseable {
  private static final String TMP_DIR_PREFIX = "tempfiles-delete-on-finish";
  private static final Logger logger = LoggerFactory.getLogger(TempFile.class);

  private final Path tmpDir;
  private final Path tmpFile;

  public Path get() {
    return tmpFile;
  }

  public TempFile(String filename) throws IOException {
    tmpDir = Files.createTempDirectory(TMP_DIR_PREFIX);
    tmpFile = tmpDir.resolve(filename);
  }

  public void streamTo(OutputStream out) throws IOException {
    try (InputStream in = Files.newInputStream(get())) {
      in.transferTo(out);
    }
  }

  @Override
  public void close() {
    try {
      Files.deleteIfExists(tmpFile);
    } catch (IOException e) {
      logger.error("An error occurred while trying to delete a temporary file: {}", e.getMessage());
    } finally {
      try {
        Files.deleteIfExists(tmpDir);
      } catch (IOException e) {
        logger.error(
            "An error occurred while trying to delete the directory belonging to a temporary file: {}",
            e.getMessage());
      }
    }
  }
}
