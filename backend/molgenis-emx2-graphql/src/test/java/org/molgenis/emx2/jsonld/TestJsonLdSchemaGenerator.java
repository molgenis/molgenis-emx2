package org.molgenis.emx2.jsonld;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_ID;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.datamodels.DataModels.Profile.TYPE_TEST;
import static org.molgenis.emx2.jsonld.JsonLdSchemaGenerator.generateJsonLdSchemaAsMap;
import static org.molgenis.emx2.jsonld.RestOverGraphql.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import graphql.ExecutionResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.graphql.GraphqlApi;
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
    Map result = generateJsonLdSchemaAsMap(schema, "http://localhost/pet%20store");
    System.out.println(result);

    Map<String, Object> data =
        Map.of("@id", "my:data", "Catalogues", List.of(Map.of("id", 1, "@id", "my:Catalogues/1")));
    System.out.println(convertToTurtle(mapper.convertValue(result, Map.class), data));
  }

  @Test
  void testJsonGeneratorInCombinationWithGraphql() throws IOException {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(TestJsonLdSchemaGenerator.class.getSimpleName());
    PET_STORE.getImportTask(schema, true).run();
    GraphqlApi graphQL = new GraphqlApi(schema);
    ExecutionResult result = graphQL.execute("{Pet{...AllPetFields}}");
    String schemaUrl = "http://localhost:8080";
    Map jsonLdSchema = generateJsonLdSchemaAsMap(schema.getMetadata(), schemaUrl);
    Map data = result.getData();
    data.put(MG_ID, "my:.");
    String ttl = convertToTurtle(jsonLdSchema, data);
    System.out.println(ttl);
  }

  @Test
  void testJsonGeneratorWithTypeTest() throws IOException {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(TestJsonLdSchemaGenerator.class.getSimpleName());
    TYPE_TEST.getImportTask(schema, true).run();
    schema.getTable("Types").insert(createTypeTestRow());
    GraphqlApi graphQL = new GraphqlApi(schema);
    String ttl = getTableAsTurtle(graphQL, "Types");
    System.out.println(ttl);
    ttl = getAllAsTurtle(graphQL, "http://localhost");
    System.out.println(ttl);
  }

  public Row createTypeTestRow() {
    Row row = new Row();
    // String types
    row.set("string type", "string");
    row.set("string array type", new String[] {"string1", "string2"});

    // Text types
    row.set("string text", "text");
    row.set("string array text", new String[] {"text1", "text2"});

    // JSON type
    row.set("json type", "{\"key\":\"value\"}");

    // Email types
    row.set("email type", "email");
    row.set("email array type", new String[] {"email1@example.com", "email2@example.com"});

    // Hyperlink types
    row.set("hyperlink type", "hyperlink");
    row.set("hyperlink array type", new String[] {"http://example.com/1", "http://example.com/2"});

    // Primitive types
    row.set("bool type", true);
    row.set("bool array type", new Boolean[] {true, false});
    row.set("uuid type", "550e8400-e29b-41d4-a716-446655440000");
    row.set(
        "uuid array type",
        new String[] {
          "550e8400-e29b-41d4-a716-446655440000", "550e8400-e29b-41d4-a716-446655440001"
        });
    row.set("file type", "file_example.txt");

    // Numeric types
    row.set("int type", 42);
    row.set("int array type", new Integer[] {1, 2, 3});
    row.set("long type", 123456789L);
    row.set("long array type", new Long[] {100000000L, 200000000L});
    row.set("decimal type", 3.1415);
    row.set("decimal array type", new Double[] {2.718, 1.618});

    // Date types
    row.set("date type", "2025-10-19");
    row.set("date array type", new String[] {"2025-10-19", "2025-10-20"});
    row.set("datetime type", "2025-10-19T12:34:56");
    row.set("datetime array type", new String[] {"2025-10-19T12:34:56", "2025-10-20T14:00:00"});

    // Period types
    row.set("period type", "P30D");
    row.set("period array type", new String[] {"P30D", "P1M", "P1Y2M10D"});

    return row;
  }
}
