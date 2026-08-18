package org.molgenis.emx2.fairmapper.extractors;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;

class RemoteRdfExtractorTest {

  private final RemoteRdfExtractor extractor = new RemoteRdfExtractor();
  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void shouldExtractFromEndpoint() throws IOException {
    String turtle =
        """
        @prefix ex: <http://example.org/> .
        ex:subject ex:predicate ex:object .
        """;
    server = startServer(turtle, "text/turtle", 200);

    SailRepository repository = new SailRepository(new MemoryStore());
    repository.init();
    try {
      extractor.addRdfToRepository(repository, URI.create(serverUrl()));

      try (RepositoryConnection conn = repository.getConnection()) {
        assertTrue(
            conn.hasStatement(
                Values.iri("http://example.org/subject"),
                Values.iri("http://example.org/predicate"),
                Values.iri("http://example.org/object"),
                false));
      }
    } finally {
      repository.shutDown();
    }
  }

  @Test
  void shouldThrowMolgenisExceptionOnUnreachableEndpoint() {
    SailRepository repository = new SailRepository(new MemoryStore());
    repository.init();
    try {
      URI unreachable = URI.create("http://localhost:1/does-not-exist");

      MolgenisException exception =
          assertThrows(
              MolgenisException.class, () -> extractor.addRdfToRepository(repository, unreachable));

      assertTrue(exception.getMessage().contains(unreachable.toString()));
      assertInstanceOf(IOException.class, exception.getCause());
    } finally {
      repository.shutDown();
    }
  }

  @Test
  void shouldThrowMolgenisExceptionOnServerError() throws IOException {
    server = startServer("not found", "text/plain", 404);

    SailRepository repository = new SailRepository(new MemoryStore());
    repository.init();
    try {
      URI endpoint = URI.create(serverUrl());

      MolgenisException exception =
          assertThrows(
              MolgenisException.class, () -> extractor.addRdfToRepository(repository, endpoint));

      assertTrue(exception.getMessage().contains(endpoint.toString()));
      assertInstanceOf(IOException.class, exception.getCause());
    } finally {
      repository.shutDown();
    }
  }

  private HttpServer startServer(String body, String contentType, int status) throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    httpServer.createContext(
        "/",
        exchange -> {
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", contentType);
          exchange.sendResponseHeaders(status, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    httpServer.start();
    return httpServer;
  }

  private String serverUrl() {
    return "http://localhost:" + server.getAddress().getPort() + "/";
  }
}
