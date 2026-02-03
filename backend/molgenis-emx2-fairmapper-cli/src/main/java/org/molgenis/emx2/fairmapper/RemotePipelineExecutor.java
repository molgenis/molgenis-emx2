package org.molgenis.emx2.fairmapper;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.rdf4j.model.Model;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.step.FrameStep;
import org.molgenis.emx2.fairmapper.model.step.MappingStep;
import org.molgenis.emx2.fairmapper.model.step.MutateStep;
import org.molgenis.emx2.fairmapper.model.step.QueryStep;
import org.molgenis.emx2.fairmapper.model.step.SparqlConstructStep;
import org.molgenis.emx2.fairmapper.model.step.SqlQueryStep;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.TransformStep;
import org.molgenis.emx2.fairmapper.rdf.FrameAnalyzer;
import org.molgenis.emx2.fairmapper.rdf.FrameDrivenFetcher;
import org.molgenis.emx2.fairmapper.rdf.JsonLdFramer;
import org.molgenis.emx2.fairmapper.rdf.JsonLdRdfConverter;
import org.molgenis.emx2.fairmapper.rdf.RdfFetcher;
import org.molgenis.emx2.fairmapper.rdf.RdfSource;
import org.molgenis.emx2.fairmapper.rdf.SparqlEngine;

public class RemotePipelineExecutor {
  private final GraphqlClient client;
  private final JsltTransformEngine transformEngine;
  private final Path bundlePath;
  private final String schema;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final MappingEngine mappingEngine = new MappingEngine();

  public RemotePipelineExecutor(
      GraphqlClient client, JsltTransformEngine transformEngine, Path bundlePath, String schema) {
    this.client = client;
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
        } else if (step instanceof MutateStep mutateStep) {
          current = executeMutate(mutateStep.path(), current);
        } else if (step instanceof SqlQueryStep sqlQueryStep) {
          throw new FairMapperException(
              "SQL queries are not supported in remote execution: " + sqlQueryStep.path());
        } else if (step instanceof FrameStep frameStep) {
          current = executeFrame(frameStep.path(), frameStep.unmapped(), current);
        } else if (step instanceof SparqlConstructStep sparqlStep) {
          current = executeSparql(sparqlStep.path(), current);
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
    return client.execute(schema, query, variables);
  }

  private JsonNode executeMutate(String mutatePath, JsonNode variables) throws IOException {
    Path resolvedPath = PathValidator.validateWithinBase(bundlePath, mutatePath);
    String mutation = Files.readString(resolvedPath);
    return client.execute(schema, mutation, variables);
  }

  private JsonNode executeFrame(String framePath, Boolean unmapped, JsonNode input)
      throws IOException {
    try {
      Path resolvedFramePath = PathValidator.validateWithinBase(bundlePath, framePath);
      ObjectNode frameDoc = (ObjectNode) objectMapper.readTree(Files.readString(resolvedFramePath));

      if (Boolean.TRUE.equals(unmapped)) {
        setExplicitRecursive(frameDoc, false);
      }

      String inputStr = objectMapper.writeValueAsString(input);
      JsonDocument inputDoc = JsonDocument.of(new StringReader(inputStr));

      String frameStr = objectMapper.writeValueAsString(frameDoc);
      JsonReader jsonReader = Json.createReader(new StringReader(frameStr));
      JsonStructure frameStructure = jsonReader.read();
      JsonDocument frameDocForApi = JsonDocument.of(frameStructure);

      JsonLdOptions options = new JsonLdOptions();
      options.setOmitGraph(true);

      JsonStructure framedJson = JsonLd.frame(inputDoc, frameDocForApi).options(options).get();

      String framedStr = framedJson.toString();
      JsonNode framedResult = objectMapper.readTree(framedStr);

      if (framedResult.isObject() && frameDoc.has("@context")) {
        ObjectNode result = objectMapper.createObjectNode();
        result.set("@context", frameDoc.get("@context"));
        framedResult.fields().forEachRemaining(e -> result.set(e.getKey(), e.getValue()));
        return result;
      }

      return framedResult;

    } catch (Exception e) {
      throw new IOException("Failed to frame JSON-LD: " + e.getMessage(), e);
    }
  }

  private void setExplicitRecursive(ObjectNode node, boolean explicit) {
    if (node.has("@explicit")) {
      node.put("@explicit", explicit);
    }
    node.fields()
        .forEachRemaining(
            entry -> {
              if (entry.getValue().isObject()) {
                setExplicitRecursive((ObjectNode) entry.getValue(), explicit);
              }
            });
  }

  private JsonNode executeSparql(String sparqlPath, JsonNode input) throws IOException {
    Path resolvedPath = PathValidator.validateWithinBase(bundlePath, sparqlPath);
    String sparql = Files.readString(resolvedPath);
    Model rdfModel = JsonLdRdfConverter.jsonLdToModel(input);
    Model result = SparqlEngine.construct(rdfModel, sparql);
    return JsonLdRdfConverter.modelToJsonLd(result);
  }

  private JsonNode executeMapping(String mappingPath, JsonNode input) throws IOException {
    Path resolvedPath = PathValidator.validateWithinBase(bundlePath, mappingPath);
    String mappingYaml = Files.readString(resolvedPath);
    return mappingEngine.transform(mappingYaml, input);
  }
}
