package org.molgenis.emx2.fairmapper.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.fairmapper.FairMapperException;

class LocalRdfSourceTest {

  @TempDir Path tempDir;
  Path rdfDir;
  LocalRdfSource rdfSource;

  @BeforeEach
  void setUp() throws IOException {
    rdfDir = tempDir.resolve("rdf");
    Files.createDirectories(rdfDir);

    String validTurtle =
        """
        @prefix ex: <http://example.org/> .
        ex:subject ex:predicate ex:object .
        """;
    Files.writeString(rdfDir.resolve("valid.ttl"), validTurtle);

    rdfSource = new LocalRdfSource(rdfDir);
  }

  @Test
  void testValidRelativePathWorks() throws IOException {
    Model model = rdfSource.fetch("valid.ttl");
    assertNotNull(model);
    assertFalse(model.isEmpty());
  }

  @Test
  void testPathTraversalBlocked() {
    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> rdfSource.fetch("../../../etc/passwd"));

    assertTrue(ex.getMessage().contains("escapes base directory"));
  }

  @Test
  void testAbsolutePathBlocked() {
    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> rdfSource.fetch("/etc/passwd"));

    assertTrue(ex.getMessage().contains("escapes base directory"));
  }

  @Test
  void testNormalizedTraversalBlocked() {
    FairMapperException ex =
        assertThrows(
            FairMapperException.class, () -> rdfSource.fetch("subdir/../../../etc/passwd"));

    assertTrue(ex.getMessage().contains("escapes base directory"));
  }

  @Test
  void testRelativePathWithDotsAllowedIfStaysInside() throws IOException {
    Files.createDirectories(rdfDir.resolve("subdir"));
    Files.copy(rdfDir.resolve("valid.ttl"), rdfDir.resolve("subdir/data.ttl"));

    Model model = rdfSource.fetch("subdir/../valid.ttl");
    assertNotNull(model);
    assertFalse(model.isEmpty());
  }
}
