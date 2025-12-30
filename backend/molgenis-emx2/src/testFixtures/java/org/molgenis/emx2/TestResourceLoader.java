package org.molgenis.emx2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public abstract class TestResourceLoader {
  private static final ClassLoader classLoader = TestResourceLoader.class.getClassLoader();

  public static File getFile(String filepath) {
    return new File(Objects.requireNonNull(classLoader.getResource(filepath)).getFile());
  }

  public static InputStream getFileAsStream(String filepath) {
    return classLoader.getResourceAsStream(filepath);
  }

  public static String getFileAsString(String filepath) throws IOException {
    try (InputStream inputStream = getFileAsStream(filepath)) {
      return new String(inputStream.readAllBytes());
    }
  }
}
