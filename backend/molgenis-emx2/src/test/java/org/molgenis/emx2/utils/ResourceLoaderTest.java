package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ResourceLoaderTest {
  @Test
  void testLoadFilesFromExistingDir() throws IOException {
    // jarUri in testing is not a jar with resources but the path to this module:
    // <path-to>/molgenis-emx2/backend/molgenis-emx2

    List<String> expected = List.of(("build/resources/main/log4j2.xml"));
    List<String> actual =
        Arrays.stream(ResourceLoader.loadFilesFromDir("build/resources/main"))
            .map(File::getPath)
            .collect(Collectors.toList());

    assertEquals(expected, actual);
  }

  @Test
  void testLoadFilesFromNonExistingDir() throws IOException {
    assertThrows(
        NoSuchFileException.class,
        () -> ResourceLoader.loadFilesFromDir("build/resources/nonExistingDir"));
  }
}
