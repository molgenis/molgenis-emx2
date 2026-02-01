package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schibsted.spt.data.jslt.Parser;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

@Tag("slow")
public class DcatPipelineIntegrationTest {

  private static final String SCHEMA_NAME = "dcatHarvestTest";
  private static final String BASE_URL = "https://test.molgenis.org";

  private static Database database;
  private static Schema schema;
  private static GraphQL graphql;
  private static ObjectMapper mapper = new ObjectMapper();

  private static Path jsltBundlePath;
  private static Path sqlBundlePath;
  private static Path shaclPath;

  private static Model fdpShapes;
  private static Model catalogShapes;

  private static String catalogId;
  private static String datasetId;

  @BeforeAll
  static void setup() throws Exception {
    database = TestDatabaseFactory.getTestDatabase();

    database.dropSchemaIfExists(SCHEMA_NAME);
    DataModels.Profile.DATA_CATALOGUE.getImportTask(database, SCHEMA_NAME, "test", true).run();
    schema = database.getSchema(SCHEMA_NAME);

    graphql = new GraphqlApiFactory().createGraphqlForSchema(schema, new TaskServiceInMemory());

    Path projectRoot = Paths.get(System.getProperty("user.dir"), "../..").normalize();
    jsltBundlePath = projectRoot.resolve("fair-mappings/dcat-harvester");
    sqlBundlePath = projectRoot.resolve("fair-mappings/dcat-fdp-sql");
    shaclPath = projectRoot.resolve("data/_shacl/fair_data_point/v1.2");

    loadShaclShapes();
    findTestData();
  }

  private static void loadShaclShapes() throws Exception {
    try (InputStream fdpIn = Files.newInputStream(shaclPath.resolve("FAIRDataPointShape.ttl"));
        InputStream catIn = Files.newInputStream(shaclPath.resolve("CatalogShape.ttl"))) {
      fdpShapes = Rio.parse(fdpIn, "", RDFFormat.TURTLE);
      catalogShapes = Rio.parse(catIn, "", RDFFormat.TURTLE);
    }
  }

  private static void findTestData() throws Exception {
    JsonNode resourcesResult =
        executeGraphQL(
            """
        query {
          Resources(limit: 100) {
            id
            type { name }
          }
        }
        """,
            null);

    JsonNode resources = resourcesResult.get("data").get("Resources");
    assertNotNull(resources, "Should have Resources in demo data");

    for (JsonNode r : resources) {
      JsonNode typeArray = r.get("type");
      if (typeArray != null && typeArray.isArray()) {
        for (JsonNode type : typeArray) {
          String typeName = type.get("name").asText();
          if (("Catalogue".equals(typeName) || "Network".equals(typeName)) && catalogId == null) {
            catalogId = r.get("id").asText();
          }
          if (("Cohort study".equals(typeName)
                  || "Biobank".equals(typeName)
                  || "Data source".equals(typeName))
              && datasetId == null) {
            datasetId = r.get("id").asText();
          }
        }
      }
    }

    assertNotNull(catalogId, "Should find a Catalogue/Network in demo data");
  }

  @Test
  @Disabled("FDP root transforms moved to dcat-fdp-sql bundle")
  void testFdpRoot_graphqlJslt() throws Exception {
    JsonNode query =
        executeGraphQL(
            """
        query {
          _schema {
            name
            description
          }
          Resources(filter: {type: {name: {equals: "Catalogue"}}}) {
            id
          }
        }
        """,
            null);

    String jslt =
        Files.readString(jsltBundlePath.resolve("src/transforms/publish/to-fdp-root.jslt"));
    ObjectNode input = mapper.createObjectNode();
    input.put("baseUrl", BASE_URL);
    input.set("_schema", query.get("data").get("_schema"));
    input.set("Resources", query.get("data").get("Resources"));

    JsonNode output = Parser.compileString(jslt).apply(input);
    String jsonLd = mapper.writeValueAsString(output);

    Model rdf = Rio.parse(new StringReader(jsonLd), "", RDFFormat.JSONLD);
    assertShaclValid(rdf, fdpShapes, "FDP root (GraphQL+JSLT)");
  }

  @Test
  void testFdpRoot_sql() throws Exception {
    String sql = Files.readString(sqlBundlePath.resolve("src/get-fdp-root.sql"));
    Map<String, String> params = Map.of("base_url", BASE_URL, "schema", SCHEMA_NAME);

    List<Row> rows = schema.retrieveSql(sql, params);
    assertEquals(1, rows.size(), "SQL query should return one row");

    String jsonLd = rows.get(0).getString("result");
    Model rdf = Rio.parse(new StringReader(jsonLd), "", RDFFormat.JSONLD);
    assertShaclValid(rdf, fdpShapes, "FDP root (SQL)");
  }

  @Test
  @Disabled("Catalog publish transforms moved to dcat-fdp-sql bundle")
  void testCatalog_graphqlJslt() throws Exception {
    String queryStr =
        """
        query($id: String!) {
          _schema {
            name
          }
          Resources(filter: {id: {equals: $id}}) {
            id
            name
            description
          }
        }
        """;

    JsonNode query = executeGraphQL(queryStr, mapper.createObjectNode().put("id", catalogId));

    String jslt =
        Files.readString(jsltBundlePath.resolve("src/transforms/publish/to-dcat-catalog.jslt"));
    ObjectNode input = mapper.createObjectNode();
    input.put("baseUrl", BASE_URL);
    input.set("_schema", query.get("data").get("_schema"));
    input.set("resource", query.get("data").get("Resources").get(0));

    JsonNode output = Parser.compileString(jslt).apply(input);
    String jsonLd = mapper.writeValueAsString(output);

    Model rdf = Rio.parse(new StringReader(jsonLd), "", RDFFormat.JSONLD);
    assertShaclValid(rdf, catalogShapes, "Catalog (GraphQL+JSLT)");
  }

  @Test
  void testCatalog_sql() throws Exception {
    String sql = Files.readString(sqlBundlePath.resolve("src/get-catalog.sql"));
    Map<String, String> params =
        Map.of("base_url", BASE_URL, "schema", SCHEMA_NAME, "id", catalogId);

    List<Row> rows = schema.retrieveSql(sql, params);
    assertEquals(1, rows.size(), "SQL query should return one row");

    String jsonLd = rows.get(0).getString("result");
    Model rdf = Rio.parse(new StringReader(jsonLd), "", RDFFormat.JSONLD);
    assertShaclValid(rdf, catalogShapes, "Catalog (SQL)");
  }

  @Test
  @Disabled("Dataset publish transforms moved to dcat-fdp-sql bundle")
  void testDataset_graphqlJslt() throws Exception {
    String queryStr =
        """
        query($id: String!) {
          _schema {
            name
          }
          Resources(filter: {id: {equals: $id}}) {
            id
            name
            description
          }
        }
        """;

    JsonNode query = executeGraphQL(queryStr, mapper.createObjectNode().put("id", datasetId));

    String jslt =
        Files.readString(jsltBundlePath.resolve("src/transforms/publish/to-dcat-dataset.jslt"));
    ObjectNode input = mapper.createObjectNode();
    input.put("baseUrl", BASE_URL);
    input.set("_schema", query.get("data").get("_schema"));
    input.set("resource", query.get("data").get("Resources").get(0));

    JsonNode output = Parser.compileString(jslt).apply(input);
    String jsonLd = mapper.writeValueAsString(output);

    Model rdf = Rio.parse(new StringReader(jsonLd), "", RDFFormat.JSONLD);
    assertShaclValid(rdf, catalogShapes, "Dataset (GraphQL+JSLT)");
  }

  @Test
  void testDataset_sql() throws Exception {
    String sql = Files.readString(sqlBundlePath.resolve("src/get-dataset.sql"));
    Map<String, String> params =
        Map.of("base_url", BASE_URL, "schema", SCHEMA_NAME, "id", datasetId);

    List<Row> rows = schema.retrieveSql(sql, params);
    assertEquals(1, rows.size(), "SQL query should return one row");

    String jsonLd = rows.get(0).getString("result");
    Model rdf = Rio.parse(new StringReader(jsonLd), "", RDFFormat.JSONLD);
    assertShaclValid(rdf, catalogShapes, "Dataset (SQL)");
  }

  @Test
  void testTransformAndMutate_insertsResources() throws Exception {
    Path inputPath = jsltBundlePath.resolve("test/fetch/catalog.json");
    JsonNode fetchedData = mapper.readTree(Files.readString(inputPath));

    JsltTransformEngine engine = new JsltTransformEngine();
    Path transformPath = jsltBundlePath.resolve("src/to-molgenis.jslt");
    JsonNode transformed = engine.transform(transformPath, fetchedData);

    assertNotNull(transformed, "Transform should produce output");

    String mutation =
        """
        mutation($Resources: [ResourcesInput]) {
          insert(Resources: $Resources) {
            message
          }
        }
        """;
    JsonNode mutationResult = executeGraphQL(mutation, transformed);

    assertNotNull(mutationResult, "Mutation should return result");
    assertTrue(mutationResult.has("data"), "Should have data field");
    assertNotNull(mutationResult.get("data").get("insert"), "Should have insert result");

    JsonNode resourcesResult =
        executeGraphQL(
            """
        query {
          Resources(limit: 100) {
            id
            name
            type { name }
          }
        }
        """,
            null);

    JsonNode resources = resourcesResult.get("data").get("Resources");
    assertNotNull(resources, "Should have Resources table");
    assertTrue(resources.size() > 0, "Should have inserted resources");
  }

  @Test
  void testTransformStep_producesValidMolgenisJson() throws Exception {
    Path inputPath = jsltBundlePath.resolve("test/fetch/catalog.json");
    JsonNode input = mapper.readTree(Files.readString(inputPath));

    JsltTransformEngine engine = new JsltTransformEngine();
    Path transformPath = jsltBundlePath.resolve("src/to-molgenis.jslt");
    JsonNode result = engine.transform(transformPath, input);

    assertNotNull(result);
    assertTrue(
        result.has("Resources") || result.has("Organisations"),
        "Should have Resources or Organisations");
  }

  @SuppressWarnings("unchecked")
  private static JsonNode executeGraphQL(String query, JsonNode variables) throws Exception {
    ExecutionInput.Builder inputBuilder = ExecutionInput.newExecutionInput().query(query);

    if (variables != null) {
      inputBuilder.variables(mapper.convertValue(variables, Map.class));
    }

    ExecutionResult result = graphql.execute(inputBuilder.build());

    if (!result.getErrors().isEmpty()) {
      StringBuilder errorMsg = new StringBuilder("GraphQL errors:\n");
      for (graphql.GraphQLError error : result.getErrors()) {
        errorMsg.append("  - ").append(error.getMessage()).append("\n");
        if (error.getExtensions() != null) {
          errorMsg.append("    Extensions: ").append(error.getExtensions()).append("\n");
        }
      }
      fail(errorMsg.toString());
    }

    String json = convertExecutionResultToJson(result);
    return mapper.readTree(json);
  }

  private void assertShaclValid(Model data, Model shapes, String name) {
    ShaclSail shaclSail = new ShaclSail(new MemoryStore());
    SailRepository repo = new SailRepository(shaclSail);
    repo.init();

    try (SailRepositoryConnection conn = repo.getConnection()) {
      conn.begin();
      shapes.forEach(st -> conn.add(st, RDF4J.SHACL_SHAPE_GRAPH));
      conn.commit();

      conn.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk);
      data.forEach(conn::add);

      try {
        conn.commit();
      } catch (Exception e) {
        if (e.getCause() instanceof org.eclipse.rdf4j.common.exception.ValidationException ve) {
          Model report = ve.validationReportAsModel();
          fail(name + " failed SHACL validation:\n" + report);
        }
        throw e;
      }
    } finally {
      repo.shutDown();
    }
  }
}
