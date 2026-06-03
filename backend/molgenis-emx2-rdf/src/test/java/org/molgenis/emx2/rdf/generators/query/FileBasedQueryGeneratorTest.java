package org.molgenis.emx2.rdf.generators.query;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.MolgenisException;

class FileBasedQueryGeneratorTest {

  @TempDir Path tempDir;

  @Test
  void whenFileExists_thenReturnFileContent() throws IOException {
    Path queryFile = tempDir.resolve("query.rq");
    String expectedQuery = "SELECT ?s ?p ?o WHERE { ?s ?p ?o . }";
    Files.writeString(queryFile, expectedQuery);

    String result = new FileBasedQueryGenerator(queryFile).generate(null);

    assertEquals(expectedQuery, result);
  }

  @Test
  void whenFileExists_thenIgnoresTableMetadata() throws IOException {
    Path queryFile = tempDir.resolve("query.rq");
    String expectedQuery = "SELECT ?s WHERE { ?s a <https://example.org/Type> . }";
    Files.writeString(queryFile, expectedQuery);

    FileBasedQueryGenerator generator = new FileBasedQueryGenerator(queryFile);
    String resultWithNull = generator.generate(null);
    String resultWithDifferentMetadata = generator.generate(null);

    assertEquals(resultWithNull, resultWithDifferentMetadata);
  }

  @Test
  void whenFileDoesNotExist_thenThrowMolgenisException() {
    Path nonExistent = tempDir.resolve("does-not-exist.rq");
    QueryGenerator generator = new FileBasedQueryGenerator(nonExistent);
    assertThrows(MolgenisException.class, () -> generator.generate(null));
  }
}
