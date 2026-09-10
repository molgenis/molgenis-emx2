package org.molgenis.emx2.fairmapper.load;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.web.ApiTestBase;

class RemoteDataLoaderTest extends ApiTestBase {

  private static final String SCHEMA_NAME = RemoteDataLoaderTest.class.getSimpleName();

  private static String token;
  private static String endpoint;

  @BeforeAll
  static void setupSchema() {
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
    schema
        .getMetadata()
        .create(
            TableMetadata.table("Person").add(Column.column("name", ColumnType.STRING).setPkey()));

    token = JWTgenerator.createTemporaryToken(database);
    endpoint = "http://localhost:" + port;
  }

  @Test
  void givenSuccessfulResponse_whenLoad_thenUploadsDataToTargetSchema() {
    RemoteDataLoader loader =
        new RemoteDataLoader(endpoint, token, SCHEMA_NAME, new String[] {"Person"});

    loader.load(personTableStore());

    List<Row> rows =
        database
            .getSchema(SCHEMA_NAME)
            .getTable("Person")
            .retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);
    CompareTools.assertEquals(rows, List.of(Row.row("name", "Lewis"), Row.row("name", "Robin")));
  }

  @Test
  void givenUnsuccessfulResponse_whenLoad_thenThrows() {
    RemoteDataLoader loader =
        new RemoteDataLoader(endpoint, token, "non-existent-schema", new String[] {"Person"});

    InMemoryTableStore tableStore = personTableStore();
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> loader.load(tableStore));
    assertEquals(
        """
        Unexpected response: {
          "errors" : [
            {
              "message" : "schema cannot be null"
            }
          ]
        }""",
        exception.getMessage());
    assertNoLeftoverTempDirectories("non-existent-schema");
  }

  @Test
  void givenServerUnreachable_whenLoad_thenThrowsWrappingIOException() {
    RemoteDataLoader loader =
        new RemoteDataLoader("http://localhost:1", token, SCHEMA_NAME, new String[] {"Person"});

    InMemoryTableStore tableStore = personTableStore();
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> loader.load(tableStore));
    assertTrue(exception.getMessage().startsWith("Something went wrong when uploading zip data"));
    assertNoLeftoverTempDirectories(SCHEMA_NAME);
  }

  @Test
  void givenLoadCompletes_whenSuccessful_thenNoTempFilesAreLeftBehind() {
    RemoteDataLoader loader =
        new RemoteDataLoader(endpoint, token, SCHEMA_NAME, new String[] {"Person"});

    loader.load(personTableStore());

    assertNoLeftoverTempDirectories(SCHEMA_NAME);
  }

  private InMemoryTableStore personTableStore() {
    InMemoryTableStore tableStore = new InMemoryTableStore();
    tableStore.writeTable(
        "Person", List.of("name"), List.of(Row.row("name", "Lewis"), Row.row("name", "Robin")));
    return tableStore;
  }

  private void assertNoLeftoverTempDirectories(String schemaName) {
    Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
    try (Stream<Path> files = Files.list(tmpDir)) {
      boolean leftoverExists =
          files
              .map(path -> path.getFileName().toString())
              .anyMatch(name -> name.startsWith("remote-data-loader-" + schemaName));
      assertFalse(leftoverExists, "Expected temp directory to be cleaned up after load()");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Nested
  class UploadUrlTest {

    @Test
    void givenEndpointWithoutPath_whenUploadUrl_thenAppendsSchemaAndApiZip() {
      URL url = RemoteDataLoader.uploadUrl("http://localhost:8080", "mySchema");

      assertEquals("http://localhost:8080/mySchema/api/zip", url.toString());
    }

    @Test
    void givenEndpointWithTrailingSlash_whenUploadUrl_thenAppendsSchemaAndApiZip() {
      URL url = RemoteDataLoader.uploadUrl("http://localhost:8080/", "mySchema");

      assertEquals("http://localhost:8080/mySchema/api/zip", url.toString());
    }

    @Test
    void givenEndpointWithPathAndTrailingSlash_whenUploadUrl_thenAppendsAfterPath() {
      URL url = RemoteDataLoader.uploadUrl("http://localhost:8080/emx2/", "mySchema");

      assertEquals("http://localhost:8080/emx2/mySchema/api/zip", url.toString());
    }

    @Test
    void givenEndpointWithPathWithoutTrailingSlash_whenUploadUrl_thenReplacesLastSegment() {
      URL url = RemoteDataLoader.uploadUrl("http://localhost:8080/emx2", "mySchema");

      assertEquals("http://localhost:8080/emx2/mySchema/api/zip", url.toString());
    }

    @Test
    void givenInvalidEndpoint_whenUploadUrl_thenThrowsIllegalArgumentException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> RemoteDataLoader.uploadUrl("not a valid uri", "mySchema"));
    }

    @Test
    void givenSchemaWithSpaces_whenUploadUrl_thenThrowsIllegalArgumentException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> RemoteDataLoader.uploadUrl("http://localhost:8080", "my schema"));
    }

    @Test
    void givenEndpointWithUnknownProtocol_whenUploadUrl_thenThrowsMolgenisException() {
      MolgenisException exception =
          assertThrows(
              MolgenisException.class,
              () -> RemoteDataLoader.uploadUrl("unknown-protocol://localhost", "mySchema"));

      assertEquals(
          "Unable to stage zip file for upload: unknown protocol: unknown-protocol",
          exception.getMessage());
    }
  }
}
