package org.molgenis.emx2.fairmapper.upload;

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

class RemoteDataUploaderTest extends ApiTestBase {

  private static final String SCHEMA_NAME = RemoteDataUploaderTest.class.getSimpleName() + " test";

  private static String token;
  private static String endpoint;

  @BeforeAll
  static void setupSchema() {
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
    schema
        .getMetadata()
        .create(
            TableMetadata.table("Person").add(Column.column("name", ColumnType.STRING).setPkey()),
            TableMetadata.table("EmptyTable")
                .add(Column.column("name", ColumnType.STRING).setPkey()));

    token = JWTgenerator.createTemporaryToken(database);
    endpoint = "http://localhost:" + port;
  }

  @Test
  void givenSuccessfulResponse_whenUpload_thenUploadsDataToTargetSchema() {
    RemoteDataUploader loader = new RemoteDataUploader(endpoint, token, SCHEMA_NAME);

    loader.upload(personTableStore());

    List<Row> rows =
        database
            .getSchema(SCHEMA_NAME)
            .getTable("Person")
            .retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);
    CompareTools.assertEquals(rows, List.of(Row.row("name", "Lewis"), Row.row("name", "Robin")));

    assertNoLeftoverTempDirectories(SCHEMA_NAME);
  }

  @Test
  void givenUnsuccessfulResponse_whenUpload_thenThrows() {
    RemoteDataUploader loader = new RemoteDataUploader(endpoint, token, "non-existent-schema");

    InMemoryTableStore tableStore = personTableStore();
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> loader.upload(tableStore));
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
  void givenServerUnreachable_whenUpload_thenThrowsWrappingIOException() {
    RemoteDataUploader loader = new RemoteDataUploader("http://localhost:1", token, SCHEMA_NAME);

    InMemoryTableStore tableStore = personTableStore();
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> loader.upload(tableStore));
    assertTrue(exception.getMessage().startsWith("Something went wrong when uploading zip data"));
    assertNoLeftoverTempDirectories(SCHEMA_NAME);
  }

  @Test
  void givenTableWithNoRows_whenUpload_thenUploadsSuccessfully() {
    RemoteDataUploader loader = new RemoteDataUploader(endpoint, token, SCHEMA_NAME);

    InMemoryTableStore tableStore = new InMemoryTableStore();
    tableStore.writeTable("EmptyTable", List.of("name"), List.of());

    loader.upload(tableStore);

    List<Row> rows =
        database
            .getSchema(SCHEMA_NAME)
            .getTable("EmptyTable")
            .retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);
    assertTrue(rows.isEmpty());
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
      URL url = RemoteDataUploader.uploadUrl("http://localhost:8080", "mySchema");

      assertEquals("http://localhost:8080/mySchema/api/zip", url.toString());
    }

    @Test
    void givenEndpointWithTrailingSlash_whenUploadUrl_thenAppendsSchemaAndApiZip() {
      URL url = RemoteDataUploader.uploadUrl("http://localhost:8080/", "mySchema");

      assertEquals("http://localhost:8080/mySchema/api/zip", url.toString());
    }

    @Test
    void givenEndpointWithPathAndTrailingSlash_whenUploadUrl_thenAppendsAfterPath() {
      URL url = RemoteDataUploader.uploadUrl("http://localhost:8080/emx2/", "mySchema");

      assertEquals("http://localhost:8080/emx2/mySchema/api/zip", url.toString());
    }

    @Test
    void givenEndpointWithPathWithoutTrailingSlash_whenUploadUrl_thenReplacesLastSegment() {
      URL url = RemoteDataUploader.uploadUrl("http://localhost:8080/emx2", "mySchema");

      assertEquals("http://localhost:8080/emx2/mySchema/api/zip", url.toString());
    }

    @Test
    void givenInvalidEndpoint_whenUploadUrl_thenThrowsIllegalArgumentException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> RemoteDataUploader.uploadUrl("not a valid uri", "mySchema"));
    }

    @Test
    void givenSchemaWithSpaces_whenUploadUrl_thenEncodesSpaces() {
      URL url = RemoteDataUploader.uploadUrl("http://localhost:8080", "my schema");

      assertEquals("http://localhost:8080/my%20schema/api/zip", url.toString());
    }

    @Test
    void givenEndpointWithUnknownProtocol_whenUploadUrl_thenThrowsIllegalArgumentException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> RemoteDataUploader.uploadUrl("unknown-protocol://localhost", "mySchema"));
    }
  }
}
