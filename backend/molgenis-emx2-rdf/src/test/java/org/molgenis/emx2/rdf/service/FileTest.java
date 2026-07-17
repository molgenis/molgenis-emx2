package org.molgenis.emx2.rdf.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.TestResourceLoader.getFile;

import java.io.IOException;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;

class FileTest extends RdfServiceTest {
  private static final String SCHEMA_NAME = FileTest.class.getSimpleName();

  static Schema fileTest;

  @BeforeAll
  static void beforeAll() {
    fileTest = database.dropCreateSchema(SCHEMA_NAME);
    fileTest.create(
        table(
            "myFiles",
            column("id").setType(ColumnType.STRING).setPkey(),
            column("file").setType(ColumnType.FILE)));

    fileTest.getTable("myFiles").insert(row("id", "1", "file", getFile("testfiles/molgenis.png")));
  }

  @AfterAll
  static void afterAll() {
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void testFileMetadataTriples() throws IOException {
    InMemoryRDFHandler handler = parseSchemaRdf(fileTest);

    Set<Value> files =
        handler
            .resources
            .get(Values.iri(getApi(fileTest) + "MyFiles/id=1"))
            .get(Values.iri(getApi(fileTest) + "MyFiles/column/file"));

    IRI fileIRI = (IRI) files.stream().findFirst().get();

    Set<Value> fileNames = handler.resources.get(fileIRI).get(DCTERMS.TITLE);
    Set<Value> fileFormats = handler.resources.get(fileIRI).get(DCTERMS.FORMAT);

    assertAll(
        () -> assertEquals(1, files.size()),
        () -> assertEquals(1, fileNames.size()),
        () -> assertEquals(Values.literal("molgenis.png"), fileNames.stream().findFirst().get()),
        () -> assertEquals(1, fileFormats.size()),
        () ->
            assertEquals(
                Values.iri("http://www.iana.org/assignments/media-types/image/png"),
                fileFormats.stream().findFirst().get()));
  }
}
