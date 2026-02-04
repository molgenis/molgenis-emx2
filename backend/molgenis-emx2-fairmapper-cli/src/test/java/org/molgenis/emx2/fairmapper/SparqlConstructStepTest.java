package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.step.SparqlConstructStep;

class SparqlConstructStepTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private GraphqlClient mockClient;
  private JsltTransformEngine mockTransformEngine;
  private RemotePipelineExecutor executor;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() throws IOException {
    mockClient = mock(GraphqlClient.class);
    mockTransformEngine = mock(JsltTransformEngine.class);
    executor = new RemotePipelineExecutor(mockClient, mockTransformEngine, tempDir, "testSchema");

    String sparql =
        """
        PREFIX dcat: <http://www.w3.org/ns/dcat#>
        PREFIX dct: <http://purl.org/dc/terms/>
        CONSTRUCT {
          ?s dct:title ?title .
        }
        WHERE {
          ?s a dcat:Catalog .
          ?s dct:title ?title .
        }
        """;

    Files.writeString(tempDir.resolve("filter.sparql"), sparql);
  }

  @Test
  void testSparqlConstructStep() throws IOException {
    JsonNode input =
        objectMapper.readTree(
            """
            {
              "@context": {
                "dcat": "http://www.w3.org/ns/dcat#",
                "dct": "http://purl.org/dc/terms/"
              },
              "@id": "https://example.org/catalog",
              "@type": "dcat:Catalog",
              "dct:title": "Test Catalog",
              "dct:description": "This should be filtered out"
            }
            """);

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new SparqlConstructStep("filter.sparql", null)),
            null);

    JsonNode result = executor.execute(input, mapping);

    assertNotNull(result);
  }

  @Test
  void testSparqlFileNotFound() {
    JsonNode input = objectMapper.createObjectNode();
    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new SparqlConstructStep("nonexistent.sparql", null)),
            null);

    assertThrows(IOException.class, () -> executor.execute(input, mapping));
  }

  @Test
  void testSparqlInvalidPath() {
    JsonNode input = objectMapper.createObjectNode();
    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new SparqlConstructStep("../outside/query.sparql", null)),
            null);

    assertThrows(FairMapperException.class, () -> executor.execute(input, mapping));
  }

  @Test
  void testSparqlEmptyInput() throws IOException {
    JsonNode input =
        objectMapper.readTree(
            """
            {
              "@context": {},
              "@graph": []
            }
            """);

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new SparqlConstructStep("filter.sparql", null)),
            null);

    JsonNode result = executor.execute(input, mapping);

    assertNotNull(result);
  }
}
