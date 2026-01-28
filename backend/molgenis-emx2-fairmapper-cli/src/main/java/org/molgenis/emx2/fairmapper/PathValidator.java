package org.molgenis.emx2.fairmapper;

import java.io.IOException;
import java.nio.file.Path;

public final class PathValidator {
  private PathValidator() {}

  public static Path validateWithinBase(Path baseDir, String relativePath) {
    try {
      Path resolved = baseDir.resolve(relativePath).normalize();
      String basePath = baseDir.toFile().getCanonicalPath();
      String resolvedPath = resolved.toFile().getCanonicalPath();

      if (!resolvedPath.startsWith(basePath)) {
        throw new FairMapperException("Path escapes base directory: " + relativePath);
      }
      return resolved;
    } catch (IOException e) {
      throw new FairMapperException("Failed to resolve path: " + relativePath, e);
    }
  }
}
