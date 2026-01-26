package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.molgenis.emx2.fairmapper.model.Endpoint;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.Step;
import org.molgenis.emx2.fairmapper.model.step.QueryStep;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.TransformStep;

public class RemotePipelineExecutor {
  private final GraphqlClient client;
  private final JsltTransformEngine transformEngine;
  private final Path bundlePath;
  private final String schema;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public RemotePipelineExecutor(
      GraphqlClient client, JsltTransformEngine transformEngine, Path bundlePath, String schema) {
    this.client = client;
    this.transformEngine = transformEngine;
    this.bundlePath = bundlePath;
    this.schema = schema;
  }

  public JsonNode execute(JsonNode request, Endpoint endpoint) throws IOException {
    JsonNode current = request;

    for (Step step : endpoint.steps()) {
      if (step.transform() != null) {
        current = executeTransform(step.transform(), current);
      } else if (step.query() != null) {
        current = executeQuery(step.query(), current);
      }
    }

    return current;
  }

  public JsonNode execute(JsonNode request, Mapping mapping) throws IOException {
    JsonNode current = request;

    for (StepConfig step : mapping.steps()) {
      if (step instanceof TransformStep transformStep) {
        current = executeTransform(transformStep.path(), current);
      } else if (step instanceof QueryStep queryStep) {
        current = executeQuery(queryStep.path(), current);
      }
    }

    return current;
  }

  private JsonNode executeTransform(String transformPath, JsonNode input) throws IOException {
    Path resolvedPath = bundlePath.resolve(transformPath).normalize();
    return transformEngine.transform(resolvedPath, input);
  }

  private JsonNode executeQuery(String queryPath, JsonNode variables) throws IOException {
    Path resolvedPath = bundlePath.resolve(queryPath).normalize();
    String query = Files.readString(resolvedPath);
    return client.execute(schema, query, variables);
  }
}
