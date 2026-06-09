package org.molgenis.emx2.rdf.generators.query;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.TableMetadata;

class FileBasedQueryGeneratorTest {

  @TempDir Path tempDir;

  @Test
  void whenTableHasFile_thenReturnFileContent() throws IOException {
    Path queryFile = tempDir.resolve("query.rq");
    String expectedQuery = "SELECT ?s ?p ?o WHERE { ?s ?p ?o . }";
    Files.writeString(queryFile, expectedQuery);

    QueryGenerator generator = new FileBasedQueryGenerator(Map.of("MyTable", queryFile));

    assertEquals(expectedQuery, generator.generate(new TableMetadata("MyTable")));
  }

  @Test
  void whenTableNameNotInMap_thenThrowMolgenisException() {
    QueryGenerator generator = new FileBasedQueryGenerator(Map.of());

    assertThrows(
        MolgenisException.class, () -> generator.generate(new TableMetadata("UnknownTable")));
  }

  @Test
  void whenFileDoesNotExist_thenThrowMolgenisException() {
    Path nonExistent = tempDir.resolve("does-not-exist.rq");
    QueryGenerator generator = new FileBasedQueryGenerator(Map.of("MyTable", nonExistent));

    assertThrows(MolgenisException.class, () -> generator.generate(new TableMetadata("MyTable")));
  }

  @Test
  void whenMultipleTablesConfigured_thenReturnsFileForRequestedTable() throws IOException {
    Path fileA = tempDir.resolve("a.rq");
    Path fileB = tempDir.resolve("b.rq");
    Files.writeString(fileA, "SELECT ?a WHERE { ?a a <https://example.org/A> . }");
    Files.writeString(fileB, "SELECT ?b WHERE { ?b a <https://example.org/B> . }");

    QueryGenerator generator =
        new FileBasedQueryGenerator(Map.of("TableA", fileA, "TableB", fileB));

    assertEquals(Files.readString(fileA), generator.generate(new TableMetadata("TableA")));
    assertEquals(Files.readString(fileB), generator.generate(new TableMetadata("TableB")));
  }
}
