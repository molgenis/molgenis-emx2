package org.molgenis.emx2.jsonld;

import static graphql.Assert.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_ID;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.jsonld.JsonLdSchemaGenerator.generateJsonLdSchema;
import static org.molgenis.emx2.jsonld.JsonLdSchemaGenerator.generateJsonLdSchemaAsMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import graphql.ExecutionResult;
import graphql.GraphQL;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestJsonLdSchemaGenerator {
  private static final ObjectMapper mapper =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  @Test
  void testJsonLdSchemaGenerator() throws Exception {
    SchemaMetadata schema = new SchemaMetadata();
    TableMetadata catalogues =
        table(
            "Catalogues",
            column("id").setPkey(),
            column("dataset").setType(ColumnType.REF_ARRAY).setRefTable("Datasets"));
    catalogues.setSemantics("dcat:Catalog");
    TableMetadata datasets = table("Datasets", column("id").setPkey());
    catalogues.setSemantics("dcat:Dataset");
    schema.create(catalogues, datasets);
    String result = generateJsonLdSchema(schema, "http://localhost/pet%20store");
    System.out.println(result);

    Map<String, ?> data =
        Map.of("@id", "my:data", "Catalogues", List.of(Map.of("id", 1, "@id", "my:Catalogues/1")));
    assertTrue(validateJsonLdSchema(result, data));
    System.out.println(convertToTurtle(result, data));
  }

  @Test
  void testJsonGeneratorInCombinationWithGraphql() throws IOException {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(TestJsonLdSchemaGenerator.class.getSimpleName());
    PET_STORE.getImportTask(schema, true).run();
    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(schema);
    ExecutionResult result = graphQL.execute("{Pet{mg_id,name}}");
    String schemaUrl = "http://localhost:8080";
    Map jsonLdSchema = generateJsonLdSchemaAsMap(schema.getMetadata(), schemaUrl);
    Map data = result.getData();
    data.put(MG_ID, "my:.");
    String ttl = convertToTurtle(jsonLdSchema, data);
    System.out.println(ttl);
  }

  public static String convertToTurtle(String jsonLdSchema, Map<String, ?> graphqlLikeData) {
    try {
      return convertToTurtle(mapper.readValue(jsonLdSchema, Map.class), graphqlLikeData);
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage());
    }
  }

  public static String convertToTurtle(Map<String, ?> jsonLdSchema, Map<String, ?> graphqlLikeData)
      throws IOException {

    Map wrapper = new LinkedHashMap<>();
    wrapper.putAll(jsonLdSchema);
    wrapper.put("data", graphqlLikeData);

    System.out.println(mapper.writeValueAsString(wrapper));

    try (StringReader reader = new StringReader(mapper.writeValueAsString(wrapper))) {
      Model model = Rio.parse(reader, "", RDFFormat.JSONLD);
      StringWriter writer = new StringWriter();
      Rio.write(model, writer, RDFFormat.TURTLE);
      return writer.toString();
    }
  }

  public static boolean validateJsonLdSchema(String jsonLdSchema, Map<String, ?> graphqlLikeData)
      throws Exception {
    Map<String, Object> wrapper = mapper.readValue(jsonLdSchema, Map.class);
    wrapper.put("data", graphqlLikeData);
    try {
      String json = mapper.writeValueAsString(wrapper);
      try (StringReader reader = new StringReader(json)) {
        Model model = Rio.parse(reader, "", RDFFormat.JSONLD);
        System.out.println("RDF4J parsed " + model.size() + " triples from context.");
      }
    } catch (RDFParseException e) {
      System.err.println(
          "RDFParseException: "
              + e.getMessage()
              + " (line "
              + e.getLineNumber()
              + ", col "
              + e.getColumnNumber()
              + ")");
      return false;
    } catch (Exception e) {
      System.err.println("Other error parsing JSON-LD: " + e.getMessage());
      return false;
    }

    return true;
  }
}
