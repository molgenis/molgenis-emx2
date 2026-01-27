package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.fairmapper.BundleLoader;
import org.molgenis.emx2.fairmapper.ContentNegotiator;
import org.molgenis.emx2.fairmapper.JsltTransformEngine;
import org.molgenis.emx2.fairmapper.PipelineExecutor;
import org.molgenis.emx2.fairmapper.model.Mapping;
import org.molgenis.emx2.fairmapper.model.MappingBundle;
import org.molgenis.emx2.fairmapper.rdf.JsonLdToRdf;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FairMapperApi {
  private static final Logger logger = LoggerFactory.getLogger(FairMapperApi.class);
  private static final String FAIR_MAPPINGS_DIR = "fair-mappings";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private FairMapperApi() {}

  public static void create(Javalin app) {
    List<BundleRegistration> bundles = loadBundles();
    registerRoutes(app, bundles);
    logger.info("Registered {} FAIRmapper bundle(s)", bundles.size());
  }

  private static List<BundleRegistration> loadBundles() {
    List<BundleRegistration> bundles = new ArrayList<>();
    Path fairMappingsPath = Paths.get(FAIR_MAPPINGS_DIR);

    if (!Files.exists(fairMappingsPath) || !Files.isDirectory(fairMappingsPath)) {
      logger.warn("FAIRmapper directory not found: {}", fairMappingsPath.toAbsolutePath());
      return bundles;
    }

    BundleLoader loader = new BundleLoader();

    try {
      Files.list(fairMappingsPath)
          .filter(Files::isDirectory)
          .forEach(
              bundleDir -> {
                Path configPath = bundleDir.resolve("fairmapper.yaml");
                if (Files.exists(configPath)) {
                  try {
                    MappingBundle bundle = loader.load(configPath);
                    bundles.add(new BundleRegistration(bundle, bundleDir));
                    logger.info("Loaded bundle: {}", bundle.name());
                  } catch (Exception e) {
                    logger.error("Failed to load bundle from {}", configPath, e);
                  }
                }
              });
    } catch (IOException e) {
      logger.error("Failed to list FAIRmapper bundles directory", e);
    }

    return bundles;
  }

  private static void registerRoutes(Javalin app, List<BundleRegistration> bundles) {
    for (BundleRegistration reg : bundles) {
      List<Mapping> mappings = reg.bundle.getMappings();
      if (mappings != null) {
        for (Mapping mapping : mappings) {
          for (String method : mapping.methods()) {
            registerEndpoint(app, method, mapping, reg.bundlePath);
          }
        }
      }
    }
  }

  private static void registerEndpoint(
      Javalin app, String method, Mapping mapping, Path bundlePath) {
    String path = mapping.endpoint();

    switch (method.toUpperCase()) {
      case "GET":
        app.get(path, ctx -> handleRequest(ctx, mapping, bundlePath));
        logger.info("Registered GET {}", path);
        break;
      case "POST":
        app.post(path, ctx -> handleRequest(ctx, mapping, bundlePath));
        logger.info("Registered POST {}", path);
        break;
      default:
        logger.warn("Unsupported HTTP method: {}", method);
    }
  }

  static void handleRequest(Context ctx, Mapping mapping, Path bundlePath) {
    try {
      Schema schema = getSchema(ctx);
      if (schema == null) {
        throw new MolgenisException("Schema not found or not accessible");
      }

      GraphqlApiFactory factory = new GraphqlApiFactory();
      GraphQL graphQL = factory.createGraphqlForSchema(schema);

      JsltTransformEngine transformEngine = new JsltTransformEngine();
      PipelineExecutor executor = new PipelineExecutor(graphQL, transformEngine, bundlePath);

      JsonNode requestBody = parseRequestBody(ctx);
      JsonNode result = executor.execute(requestBody, mapping);

      String acceptHeader = ctx.header("Accept");
      String outputFormat = ContentNegotiator.resolveOutputFormat(acceptHeader, mapping.output());

      ctx.contentType(ContentNegotiator.getMimeType(outputFormat));

      if (ContentNegotiator.isRdfFormat(outputFormat)) {
        JsonLdToRdf converter = new JsonLdToRdf();
        String rdfOutput = converter.convert(objectMapper.writeValueAsString(result), outputFormat);
        ctx.result(rdfOutput);
      } else {
        ctx.result(objectMapper.writeValueAsString(result));
      }

    } catch (Exception e) {
      logger.error("FAIRmapper request failed", e);
      ctx.status(500);
      ctx.json(new ErrorResponse(e.getMessage()));
    }
  }

  private static JsonNode parseRequestBody(Context ctx) throws IOException {
    String body = ctx.body();
    if (body == null || body.isEmpty()) {
      return objectMapper.createObjectNode();
    }
    return objectMapper.readTree(body);
  }

  private static record BundleRegistration(MappingBundle bundle, Path bundlePath) {}

  private static record ErrorResponse(String error) {}
}
