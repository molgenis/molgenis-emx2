package org.molgenis.emx2.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceLoader {
  private static final Logger logger = LoggerFactory.getLogger(ResourceLoader.class);
  private static final URI jarUri;

  static {
    try {
      jarUri = ResourceLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    } catch (URISyntaxException e) {
      throw new MolgenisException("An error occurred while defining JAR location");
    }
  }

  public static File[] loadFilesFromDir(String dirPath) throws IOException {
    // no try-with-resources: UnixFileSystem.close() -> UnsupportedOperationException()
    FileSystem fs = Paths.get(jarUri).getFileSystem();

    try (Stream<Path> stream = Files.walk(fs.getPath(dirPath))) {
      File[] files = stream.map(Path::toFile).filter(File::isFile).toArray(File[]::new);
      logger.info(
          "Retrieved files: "
              + Arrays.stream(files).map(File::getPath).collect(Collectors.joining(", ")));
      return files;
    }
  }
}
