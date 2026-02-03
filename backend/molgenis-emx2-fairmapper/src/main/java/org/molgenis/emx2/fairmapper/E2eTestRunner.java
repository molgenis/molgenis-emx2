package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.fairmapper.model.E2eTestCase;
import org.molgenis.emx2.fairmapper.model.HttpMethod;
import org.molgenis.emx2.fairmapper.model.Mapping;

public class E2eTestRunner {
  private final Path bundlePath;
  private final Mapping mapping;
  private final GraphQL graphql;
  private final JsltTransformEngine transformEngine;
  private final Schema schema;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public E2eTestRunner(
      Path bundlePath,
      Mapping mapping,
      GraphQL graphql,
      JsltTransformEngine transformEngine,
      Schema schema) {
    this.bundlePath = bundlePath;
    this.mapping = mapping;
    this.graphql = graphql;
    this.transformEngine = transformEngine;
    this.schema = schema;
  }

  public List<E2eTestResult> runTests() {
    if (mapping.e2e() == null || mapping.e2e().tests() == null) {
      return List.of();
    }
    if (mapping.steps() == null || mapping.steps().isEmpty()) {
      throw new MolgenisException("Mapping has no steps defined for e2e tests");
    }

    List<E2eTestResult> results = new ArrayList<>();
    PipelineExecutor executor = new PipelineExecutor(graphql, transformEngine, bundlePath, schema);

    for (E2eTestCase testCase : mapping.e2e().tests()) {
      results.add(runTestCase(executor, testCase));
    }

    return results;
  }

  private E2eTestResult runTestCase(PipelineExecutor executor, E2eTestCase testCase) {
    try {
      JsonNode input = loadJson(testCase.input());
      JsonNode expectedOutput = loadJson(testCase.output());
      JsonNode actualOutput = executor.execute(input, mapping);

      if (jsonEquals(expectedOutput, actualOutput)) {
        return new E2eTestResult(testCase.method(), testCase.input(), true, null);
      } else {
        String diff =
            "Expected: "
                + expectedOutput.toPrettyString()
                + "\nActual: "
                + actualOutput.toPrettyString();
        return new E2eTestResult(testCase.method(), testCase.input(), false, diff);
      }
    } catch (Exception e) {
      return new E2eTestResult(testCase.method(), testCase.input(), false, e.getMessage());
    }
  }

  private JsonNode loadJson(String relativePath) throws IOException {
    Path fullPath = bundlePath.resolve(relativePath).normalize();
    if (!Files.exists(fullPath)) {
      throw new MolgenisException("E2e test file not found: " + relativePath);
    }
    return objectMapper.readTree(Files.readString(fullPath));
  }

  private boolean jsonEquals(JsonNode expected, JsonNode actual) {
    return expected.equals(actual);
  }

  public record E2eTestResult(HttpMethod method, String input, boolean passed, String message) {}
}
