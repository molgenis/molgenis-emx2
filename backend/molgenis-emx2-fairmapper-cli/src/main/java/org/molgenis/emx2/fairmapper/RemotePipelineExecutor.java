package org.molgenis.emx2.fairmapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.rdf4j.model.Model;
import org.molgenis.emx2.fairmapper.model.Endpoint;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.Step;
import org.molgenis.emx2.fairmapper.model.step.MutateStep;
import org.molgenis.emx2.fairmapper.model.step.QueryStep;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.TransformStep;
import org.molgenis.emx2.fairmapper.rdf.FrameAnalyzer;
import org.molgenis.emx2.fairmapper.rdf.FrameDrivenFetcher;
import org.molgenis.emx2.fairmapper.rdf.JsonLdFramer;
import org.molgenis.emx2.fairmapper.rdf.RdfFetcher;
import org.molgenis.emx2.fairmapper.rdf.RdfSource;

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

    if (mapping.fetch() != null) {
      String fetchUrl = resolvePlaceholders(mapping.fetch(), request);
      current = executeFetch(fetchUrl, mapping.frame());
    }

    if (mapping.steps() != null) {
      for (StepConfig step : mapping.steps()) {
        if (step instanceof TransformStep transformStep) {
          current = executeTransform(transformStep.path(), current);
        } else if (step instanceof QueryStep queryStep) {
          current = executeQuery(queryStep.path(), current);
        } else if (step instanceof MutateStep mutateStep) {
          current = executeMutate(mutateStep.path(), current);
        }
      }
    }

    return current;
  }

  private JsonNode executeFetch(String url, String framePath) throws IOException {
    RdfSource source = new RdfFetcher(url);
    Path resolvedFramePath = PathValidator.validateWithinBase(bundlePath, framePath);
    JsonNode frame = objectMapper.readTree(Files.readString(resolvedFramePath));

    FrameAnalyzer analyzer = new FrameAnalyzer();
    FrameDrivenFetcher fetcher = new FrameDrivenFetcher(source, analyzer);
    Model model = fetcher.fetch(url, frame, 5, 50);

    JsonLdFramer framer = new JsonLdFramer();
    return framer.frame(model, frame);
  }

  private String resolvePlaceholders(String template, JsonNode request) {
    if (template.contains("${SOURCE_URL}") && request.has("SOURCE_URL")) {
      return template.replace("${SOURCE_URL}", request.get("SOURCE_URL").asText());
    }
    return template;
  }

  private JsonNode executeTransform(String transformPath, JsonNode input) throws IOException {
    Path resolvedPath = PathValidator.validateWithinBase(bundlePath, transformPath);
    return transformEngine.transform(resolvedPath, input);
  }

  private JsonNode executeQuery(String queryPath, JsonNode variables) throws IOException {
    Path resolvedPath = PathValidator.validateWithinBase(bundlePath, queryPath);
    String query = Files.readString(resolvedPath);
    return client.execute(schema, query, variables);
  }

  private JsonNode executeMutate(String mutatePath, JsonNode variables) throws IOException {
    Path resolvedPath = PathValidator.validateWithinBase(bundlePath, mutatePath);
    String mutation = Files.readString(resolvedPath);
    return client.execute(schema, mutation, variables);
  }
}
