package org.molgenis.emx2.fairmapper.engine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.molgenis.emx2.fairmapper.FairMapperException;
import org.molgenis.emx2.fairmapper.GraphqlClient;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.step.MutateStep;
import org.molgenis.emx2.fairmapper.model.step.QueryStep;
import org.molgenis.emx2.fairmapper.model.step.TransformStep;

class RemotePipelineExecutorTest {
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

    Files.writeString(tempDir.resolve("query.gql"), "query { Users { id name } }");
    Files.writeString(tempDir.resolve("mutate.gql"), "mutation { insert { Users(data: $data) } }");
    Files.writeString(tempDir.resolve("transform.jslt"), ".data.Users");
  }

  @Test
  void testMappingWithNullStepsReturnsInputUnchanged() throws IOException {
    JsonNode input = objectMapper.createObjectNode().put("test", "value");
    Mapping mapping = new Mapping("test", null, null, null, null, null, null, null, null);

    JsonNode result = executor.execute(input, mapping);

    assertEquals(input, result);
    verifyNoInteractions(mockClient);
    verifyNoInteractions(mockTransformEngine);
  }

  @Test
  void testMappingWithEmptyStepsListReturnsInputUnchanged() throws IOException {
    JsonNode input = objectMapper.createObjectNode().put("test", "value");
    Mapping mapping =
        new Mapping("test", null, null, null, null, null, null, Collections.emptyList(), null);

    JsonNode result = executor.execute(input, mapping);

    assertEquals(input, result);
    verifyNoInteractions(mockClient);
    verifyNoInteractions(mockTransformEngine);
  }

  @Test
  void testQueryStepSuccess() throws IOException {
    JsonNode variables = objectMapper.createObjectNode().put("limit", 10);
    JsonNode expectedResult =
        objectMapper.readTree("{\"data\":{\"Users\":[{\"id\":1,\"name\":\"Alice\"}]}}");

    when(mockClient.execute(eq("testSchema"), anyString(), eq(variables)))
        .thenReturn(expectedResult);

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new QueryStep("query.gql", null)),
            null);

    JsonNode result = executor.execute(variables, mapping);

    assertEquals(expectedResult, result);
    verify(mockClient).execute(eq("testSchema"), anyString(), eq(variables));
  }

  @Test
  void testQueryStepPropagatesIOException() throws IOException {
    JsonNode variables = objectMapper.createObjectNode();
    IOException expectedException = new IOException("Network error");

    when(mockClient.execute(eq("testSchema"), anyString(), eq(variables)))
        .thenThrow(expectedException);

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new QueryStep("query.gql", null)),
            null);

    IOException thrown =
        assertThrows(IOException.class, () -> executor.execute(variables, mapping));

    assertEquals(expectedException, thrown);
  }

  @Test
  void testMutateStepSuccess() throws IOException {
    JsonNode variables = objectMapper.createObjectNode().put("data", "new record");
    JsonNode expectedResult = objectMapper.readTree("{\"data\":{\"insert\":{\"Users\":1}}}");

    when(mockClient.execute(eq("testSchema"), anyString(), eq(variables)))
        .thenReturn(expectedResult);

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("POST"),
            null,
            null,
            null,
            List.of(new MutateStep("mutate.gql")),
            null);

    JsonNode result = executor.execute(variables, mapping);

    assertEquals(expectedResult, result);
    verify(mockClient).execute(eq("testSchema"), anyString(), eq(variables));
  }

  @Test
  void testTransformStepSuccess() throws IOException {
    JsonNode input = objectMapper.readTree("{\"data\":{\"Users\":[{\"id\":1}]}}");
    JsonNode expectedOutput = objectMapper.readTree("[{\"id\":1}]");

    when(mockTransformEngine.transform(eq(tempDir.resolve("transform.jslt")), eq(input)))
        .thenReturn(expectedOutput);

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("GET"),
            null,
            null,
            null,
            List.of(new TransformStep("transform.jslt", null)),
            null);

    JsonNode result = executor.execute(input, mapping);

    assertEquals(expectedOutput, result);
    verify(mockTransformEngine).transform(eq(tempDir.resolve("transform.jslt")), eq(input));
  }

  @Test
  void testMultiStepPipeline() throws IOException {
    JsonNode input = objectMapper.createObjectNode().put("limit", 5);
    JsonNode queryResult =
        objectMapper.readTree("{\"data\":{\"Users\":[{\"id\":1,\"name\":\"Bob\"}]}}");
    JsonNode transformResult = objectMapper.readTree("[{\"id\":1,\"name\":\"Bob\"}]");
    JsonNode mutateResult = objectMapper.readTree("{\"data\":{\"insert\":{\"Users\":1}}}");

    when(mockClient.execute(eq("testSchema"), anyString(), eq(input))).thenReturn(queryResult);
    when(mockTransformEngine.transform(eq(tempDir.resolve("transform.jslt")), eq(queryResult)))
        .thenReturn(transformResult);
    when(mockClient.execute(eq("testSchema"), anyString(), eq(transformResult)))
        .thenReturn(mutateResult);

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            null,
            List.of("POST"),
            null,
            null,
            null,
            List.of(
                new QueryStep("query.gql", null),
                new TransformStep("transform.jslt", null),
                new MutateStep("mutate.gql")),
            null);

    JsonNode result = executor.execute(input, mapping);

    assertEquals(mutateResult, result);

    InOrder inOrder = inOrder(mockClient, mockTransformEngine);
    inOrder.verify(mockClient).execute(eq("testSchema"), anyString(), eq(input));
    inOrder
        .verify(mockTransformEngine)
        .transform(eq(tempDir.resolve("transform.jslt")), eq(queryResult));
    inOrder.verify(mockClient).execute(eq("testSchema"), anyString(), eq(transformResult));
  }

  @Test
  void testTransformStepInvalidPath() {
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
            List.of(new TransformStep("../outside/transform.jslt", null)),
            null);

    assertThrows(FairMapperException.class, () -> executor.execute(input, mapping));
  }

  @Test
  void testPlaceholderResolution() throws IOException {
    JsonNode input = objectMapper.createObjectNode().put("SOURCE_URL", "https://example.org/data");

    Mapping mapping =
        new Mapping(
            "test",
            "/api/test",
            "${SOURCE_URL}",
            List.of("GET"),
            null,
            null,
            "frame.jsonld",
            null,
            null);

    Files.writeString(tempDir.resolve("frame.jsonld"), "{\"@context\": {}, \"@type\": \"Person\"}");

    assertThrows(IOException.class, () -> executor.execute(input, mapping));
  }
}
