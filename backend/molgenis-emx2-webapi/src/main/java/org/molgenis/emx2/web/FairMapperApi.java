package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.sanitize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.GraphQL;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
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
import org.molgenis.emx2.rdf.shacl.ShaclSelector;
import org.molgenis.emx2.rdf.shacl.ShaclSet;
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
          if (mapping.name() == null) {
            continue;
          }
          List<String> methods = mapping.methods() != null ? mapping.methods() : List.of("GET");
          for (String method : methods) {
            registerRoute(app, method, reg.bundle.name(), mapping, reg.bundlePath);
          }
        }
      }
    }
  }

  private static void registerRoute(
      Javalin app, String method, String bundleName, Mapping mapping, Path bundlePath) {
    String path = "/{schema}/api/fair/" + bundleName + "/" + mapping.route();

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
      String schemaName = ctx.pathParam("schema");
      if (schemaName == null || schemaName.isEmpty()) {
        throw new MolgenisException("Path parameter 'schema' is required");
      }

      Schema schema =
          MolgenisWebservice.applicationCache.getSchemaForUser(sanitize(schemaName), ctx);
      if (schema == null) {
        throw new MolgenisException("Schema not found or not accessible: " + schemaName);
      }

      GraphqlApiFactory factory = new GraphqlApiFactory();
      GraphQL graphQL = factory.createGraphqlForSchema(schema);

      JsltTransformEngine transformEngine = new JsltTransformEngine();
      PipelineExecutor executor =
          new PipelineExecutor(graphQL, transformEngine, bundlePath, schema);

      JsonNode requestBody = parseRequestBody(ctx);
      JsonNode result = executor.execute(requestBody, mapping);

      if (ctx.queryParam("validate") != null) {
        handleValidation(ctx, result);
      } else {
        handleNormalOutput(ctx, result, mapping);
      }

    } catch (Exception e) {
      logger.error("FAIRmapper request failed", e);
      ctx.status(500);
      ctx.json(new ErrorResponse(e.getMessage()));
    }
  }

  private static void handleNormalOutput(Context ctx, JsonNode result, Mapping mapping)
      throws Exception {
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
  }

  private static void handleValidation(Context ctx, JsonNode result) throws Exception {
    String shaclSetId = sanitize(ctx.queryParam("validate"));
    ShaclSet shaclSet = ShaclSelector.get(shaclSetId);
    if (shaclSet == null) {
      ctx.status(404);
      throw new MolgenisException("Validation set could not be found: " + shaclSetId);
    }

    String jsonLd = objectMapper.writeValueAsString(result);
    InputStream inputStream = new ByteArrayInputStream(jsonLd.getBytes(StandardCharsets.UTF_8));
    Model dataModel = Rio.parse(inputStream, "", RDFFormat.JSONLD);

    Model validationReport = performShaclValidation(dataModel, shaclSet);

    RDFFormat format = RDFApi.selectFormat(ctx);
    ctx.contentType(format.getDefaultMIMEType());

    StringWriter writer = new StringWriter();
    Rio.write(validationReport, writer, format);
    ctx.result(writer.toString());
  }

  private static Model performShaclValidation(Model dataModel, ShaclSet shaclSet)
      throws IOException {
    ShaclSail shaclSail = new ShaclSail(new MemoryStore());
    SailRepository repository = new SailRepository(shaclSail);
    repository.init();

    try (SailRepositoryConnection connection = repository.getConnection()) {
      connection.begin();
      for (int i = 0; i < shaclSet.files().length; i++) {
        try (InputStream shapesStream = shaclSet.getInputStream(i)) {
          connection.add(shapesStream, null, RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
        }
      }
      connection.commit();

      connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk);
      dataModel.forEach(connection::add);

      try {
        connection.commit();
        Model report =
            Rio.parse(
                new ByteArrayInputStream(
                    """
                @prefix sh: <http://www.w3.org/ns/shacl#> .

                [] a sh:ValidationReport;
                  sh:conforms true.
                """
                        .getBytes(StandardCharsets.UTF_8)),
                "",
                RDFFormat.TURTLE);
        return report;
      } catch (Exception e) {
        if (e.getCause() instanceof ValidationException ve) {
          return ve.validationReportAsModel();
        }
        throw e;
      }
    } finally {
      repository.shutDown();
    }
  }

  private static JsonNode parseRequestBody(Context ctx) throws IOException {
    ObjectNode request = objectMapper.createObjectNode();

    String body = ctx.body();
    if (body != null && !body.isEmpty()) {
      JsonNode bodyNode = objectMapper.readTree(body);
      if (bodyNode.isObject()) {
        request.setAll((ObjectNode) bodyNode);
      }
    }

    for (Map.Entry<String, String> param : ctx.pathParamMap().entrySet()) {
      request.put(param.getKey(), param.getValue());
    }

    String baseUrl = ctx.scheme() + "://" + ctx.host();
    request.put("base_url", baseUrl);

    return request;
  }

  private static record BundleRegistration(MappingBundle bundle, Path bundlePath) {}

  private static record ErrorResponse(String error) {}
}
