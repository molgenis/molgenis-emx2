package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.web.ApiTestBase;

class GraphqlClientTest extends ApiTestBase {

  private static final String SCHEMA_NAME = GraphqlClientTest.class.getSimpleName();
  private static final String QUERY =
      """
      query {
        _schemas {
          id
        }
      }
      """;
  private static final String SCHEMA_QUERY =
      """
      query {
        _schema {
          tables {
            name
          }
        }
      }
      """;
  private static String token;

  @BeforeAll
  static void setup() {
    token = JWTgenerator.createTemporaryToken(database);
    database.dropCreateSchema(SCHEMA_NAME);
  }

  @Test
  void shouldSendQueryToRealServerAndReturnDataNode() {
    GraphqlClient client = new GraphqlClient("http://localhost:" + port, token);
    JsonNode data = client.sendQuery(QUERY);

    assertTrue(data.has("_schemas"));
  }

  @Test
  void shouldThrowWhenServerUnreachable() {
    GraphqlClient client = new GraphqlClient("http://localhost:1", token);

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> client.sendQuery(QUERY));
    assertTrue(exception.getMessage().startsWith("Failed to execute graphql query"));
  }

  @Test
  void shouldSendSchemaQueryToRealServerAndReturnDataNode() {
    GraphqlClient client = new GraphqlClient("http://localhost:" + port, token);
    JsonNode data = client.sendSchemaQuery(SCHEMA_NAME, SCHEMA_QUERY);

    assertTrue(data.has("_schema"));
  }

  @Test
  void shouldThrowWhenSchemaSchemaQueryServerUnreachable() {
    GraphqlClient client = new GraphqlClient("http://localhost:1", token);

    MolgenisException exception =
        assertThrows(
            MolgenisException.class, () -> client.sendSchemaQuery(SCHEMA_NAME, SCHEMA_QUERY));
    assertTrue(exception.getMessage().startsWith("Failed to execute graphql query"));
  }

  @Nested
  class MockServerTest {

    @Test
    void givenBadRequestWithErrors_whenSendQuery_thenThrowsWithJoinedMessages() throws IOException {
      withMockServer(
          new StaticResponseHttpHandler(
              400,
              """
              {
                "errors": [
                  {"message":"boom"},
                  {"message":"bang"}
                ]
              }"""),
          client -> {
            MolgenisException exception =
                assertThrows(MolgenisException.class, () -> client.sendQuery("{}"));
            assertEquals("\"boom\", \"bang\"", exception.getMessage());
          });
    }

    @Test
    void givenBadRequestWithInvalidJson_whenSendQuery_thenThrowsWrappedException()
        throws IOException {
      withMockServer(
          new StaticResponseHttpHandler(400, "not json"),
          client -> {
            MolgenisException exception =
                assertThrows(MolgenisException.class, () -> client.sendQuery("{}"));
            assertTrue(
                exception.getMessage().contains("Unable to read error message from response"));
          });
    }

    @Test
    void givenResponseMissingDataField_whenSendQuery_thenThrows() throws IOException {
      withMockServer(
          new StaticResponseHttpHandler(200, "{\"unexpected\":\"body\"}"),
          client -> {
            MolgenisException exception =
                assertThrows(MolgenisException.class, () -> client.sendQuery("{}"));
            assertTrue(exception.getMessage().contains("Unexpected response from graphql server"));
          });
    }

    @Test
    void givenNonJsonResponseBody_whenSendQuery_thenThrowsWrappedException() throws IOException {
      withMockServer(
          new StaticResponseHttpHandler(200, "not json"),
          client -> {
            MolgenisException exception =
                assertThrows(MolgenisException.class, () -> client.sendQuery("{}"));
            assertTrue(exception.getMessage().startsWith("Failed to execute graphql query"));
          });
    }

    @Test
    void givenSendQuery_whenPosted_thenTargetsDatabaseLevelPath() throws IOException {
      withMockServer(
          new PathAssertingHttpHandler("/api/graphql", 200, "{\"data\":{\"_schemas\":[]}}"),
          client -> assertTrue(client.sendQuery("{}").has("_schemas")));
    }

    @Test
    void givenSendSchemaQuery_whenPosted_thenTargetsSchemaScopedPath() throws IOException {
      withMockServer(
          new PathAssertingHttpHandler("/MySchema/graphql", 200, "{\"data\":{\"_schema\":{}}}"),
          client -> assertTrue(client.sendSchemaQuery("MySchema", "{}").has("_schema")));
    }

    private void withMockServer(HttpHandler handler, Consumer<GraphqlClient> test)
        throws IOException {
      HttpServer mockServer = startMockServer(handler);
      try {
        GraphqlClient client =
            new GraphqlClient("http://localhost:" + mockServer.getAddress().getPort(), token);
        test.accept(client);
      } finally {
        mockServer.stop(0);
      }
    }

    private HttpServer startMockServer(HttpHandler handler) throws IOException {
      HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
      server.createContext("/", handler);
      server.start();
      return server;
    }

    private record StaticResponseHttpHandler(int status, String body) implements HttpHandler {

      @Override
      public void handle(HttpExchange exchange) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
          out.write(bytes);
        }
      }
    }

    private record PathAssertingHttpHandler(String expectedPath, int status, String body)
        implements HttpHandler {

      @Override
      public void handle(HttpExchange exchange) throws IOException {
        String actualPath = exchange.getRequestURI().getPath();
        if (!expectedPath.equals(actualPath)) {
          respond(
              exchange,
              400,
              "{\"errors\":[{\"message\":\"Expected request to "
                  + expectedPath
                  + " but was "
                  + actualPath
                  + "\"}]}");
          return;
        }
        respond(exchange, status, body);
      }

      private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
          out.write(bytes);
        }
      }
    }
  }
}
