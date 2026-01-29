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
import org.molgenis.emx2.fairmapper.model.step.FrameStep;

class FrameStepTest {
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

    String frameContent =
        """
        {
          "@context": {
            "dcat": "http://www.w3.org/ns/dcat#",
            "dct": "http://purl.org/dc/terms/",
            "name": "dct:title",
            "description": "dct:description"
          },
          "@type": "dcat:Catalog",
          "@explicit": true,
          "name": {},
          "description": {}
        }
        """;

    Files.writeString(tempDir.resolve("frame.jsonld"), frameContent);
  }

  @Test
  void testFrameStepWithUnmappedTrue() throws IOException {
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
              "dct:description": "Extra property not in frame"
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
            List.of(new FrameStep("frame.jsonld", true, null)),
            null);

    JsonNode result = executor.execute(input, mapping);

    assertNotNull(result);
    assertTrue(result.has("name") || result.has("@graph"));
  }

  @Test
  void testFrameStepWithUnmappedFalse() throws IOException {
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
              "dct:description": "Extra property not in frame"
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
            List.of(new FrameStep("frame.jsonld", false, null)),
            null);

    JsonNode result = executor.execute(input, mapping);

    assertNotNull(result);
  }

  @Test
  void testFrameStepWithUnmappedNull() throws IOException {
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
              "dct:title": "Test Catalog"
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
            List.of(new FrameStep("frame.jsonld", null, null)),
            null);

    JsonNode result = executor.execute(input, mapping);

    assertNotNull(result);
  }

  @Test
  void testFrameStepInvalidPath() {
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
            List.of(new FrameStep("../outside/frame.jsonld", null, null)),
            null);

    assertThrows(IOException.class, () -> executor.execute(input, mapping));
  }

  @Test
  void testFrameStepFileNotFound() {
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
            List.of(new FrameStep("nonexistent.jsonld", null, null)),
            null);

    assertThrows(IOException.class, () -> executor.execute(input, mapping));
  }
}
