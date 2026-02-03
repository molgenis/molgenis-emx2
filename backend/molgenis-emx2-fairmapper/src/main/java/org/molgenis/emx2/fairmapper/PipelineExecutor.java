package org.molgenis.emx2.fairmapper;

import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.Model;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.step.MappingStep;
import org.molgenis.emx2.fairmapper.model.step.QueryStep;
import org.molgenis.emx2.fairmapper.model.step.SqlQueryStep;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.TransformStep;
import org.molgenis.emx2.fairmapper.rdf.FrameAnalyzer;
import org.molgenis.emx2.fairmapper.rdf.FrameDrivenFetcher;
import org.molgenis.emx2.fairmapper.rdf.JsonLdFramer;
import org.molgenis.emx2.fairmapper.rdf.RdfFetcher;
import org.molgenis.emx2.fairmapper.rdf.RdfSource;

public class PipelineExecutor {
  private final GraphQL graphql;
  private final JsltTransformEngine transformEngine;
  private final Path bundlePath;
  private final Schema schema;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final MappingEngine mappingEngine = new MappingEngine();

  public PipelineExecutor(
      GraphQL graphql, JsltTransformEngine transformEngine, Path bundlePath, Schema schema) {
    this.graphql = graphql;
    this.transformEngine = transformEngine;
    this.bundlePath = bundlePath;
    this.schema = schema;
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
        } else if (step instanceof SqlQueryStep sqlQueryStep) {
          current = executeSqlQuery(sqlQueryStep.path(), current);
        } else if (step instanceof MappingStep mappingStep) {
          current = executeMapping(mappingStep.path(), current);
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

    ExecutionInput.Builder inputBuilder = ExecutionInput.newExecutionInput().query(query);

    // Add variables if present
    if (variables != null && variables.isObject() && variables.size() > 0) {
      inputBuilder.variables(objectMapper.convertValue(variables, java.util.Map.class));
    }

    ExecutionResult result = graphql.execute(inputBuilder.build());

    if (!result.getErrors().isEmpty()) {
      throw new IOException("GraphQL errors: " + result.getErrors());
    }

    String json = convertExecutionResultToJson(result);
    return objectMapper.readTree(json);
  }

  private JsonNode executeSqlQuery(String queryPath, JsonNode variables) throws IOException {
    Path resolvedPath = PathValidator.validateWithinBase(bundlePath, queryPath);
    String sql = Files.readString(resolvedPath);

    Map<String, String> params = new HashMap<>();
    if (variables != null && variables.isObject()) {
      variables
          .fields()
          .forEachRemaining(
              entry -> {
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                  params.put(entry.getKey(), value.asText());
                } else if (value.isNumber() || value.isBoolean()) {
                  params.put(entry.getKey(), value.asText());
                }
              });
    }

    List<Row> rows = schema.retrieveSql(sql, params);
    if (rows.isEmpty()) {
      return objectMapper.createObjectNode();
    }

    String result = rows.get(0).getString("result");
    return objectMapper.readTree(result);
  }

  private JsonNode executeMapping(String mappingPath, JsonNode input) throws IOException {
    Path resolvedPath = PathValidator.validateWithinBase(bundlePath, mappingPath);
    String mappingYaml = Files.readString(resolvedPath);
    return mappingEngine.transform(mappingYaml, input);
  }
}
