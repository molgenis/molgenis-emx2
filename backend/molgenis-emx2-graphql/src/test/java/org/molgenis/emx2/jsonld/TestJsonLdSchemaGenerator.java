package org.molgenis.emx2.jsonld;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_ID;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.datamodels.DataModels.Profile.TYPE_TEST;
import static org.molgenis.emx2.jsonld.RestOverGraphql.*;
import static org.molgenis.emx2.rdf.jsonld.JsonLdSchemaGenerator.generateJsonLdSchemaAsMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import graphql.ExecutionResult;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.graphql.GraphqlExecutor;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestJsonLdSchemaGenerator {
  private static final ObjectMapper mapper =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  @Test
  void testJsonLdSchemaGenerator() throws Exception {
    SchemaMetadata schema = new SchemaMetadata("testSchema");
    TableMetadata catalogues =
        table(
            "Catalogues",
            column("id").setPkey(),
            column("dataset").setType(ColumnType.REF_ARRAY).setRefTable("Datasets"));
    catalogues.setSemantics("dcat:Catalog");
    TableMetadata datasets = table("Datasets", column("id").setPkey());
    datasets.setSemantics("dcat:Dataset");
    schema.create(catalogues, datasets);
    Map result = generateJsonLdSchemaAsMap(schema, "http://localhost/pet%20store");
    System.out.println(result);

    Map context = (Map) result.get("@context");
    Map cataloguesNode = (Map) context.get("Catalogues");
    assertEquals("dcat:Catalog", cataloguesNode.get("@type"));
    assertEquals("TestSchema:Catalogues", cataloguesNode.get("@id"));
    Map datasetsNode = (Map) context.get("Datasets");
    assertEquals("dcat:Dataset", datasetsNode.get("@type"));
    assertEquals("TestSchema:Datasets", datasetsNode.get("@id"));

    Map datasetColumn = (Map) context.get("dataset");
    assertEquals("@id", datasetColumn.get("@type"));

    Map<String, Object> data = new LinkedHashMap<>();
    Map<String, Object> catalogue = new LinkedHashMap<>();
    catalogue.put("id", 1);
    catalogue.put(MG_ID, "TestSchema:Catalogues/1");
    data.put("Catalogues", List.of(catalogue));
    String ttl = convertToTurtle(mapper.convertValue(result, Map.class), data);
    System.out.println(ttl);

    assertTrue(ttl.contains("TestSchema:Catalogues/1") || ttl.contains("#Catalogues/1"));
    assertTrue(ttl.contains("dcat:"));
  }

  @Test
  void testJsonGeneratorInCombinationWithGraphql() throws IOException {
    Database database = TestDatabaseFactory.getTestDatabase();
    String schemaName = "TestJsonLdPetStore";
    database.dropSchemaIfExists(schemaName);
    PET_STORE.getImportTask(database, schemaName, "Pet Store", true).run();
    Schema schema = database.getSchema(schemaName);
    GraphqlExecutor graphQL = new GraphqlExecutor(schema);
    ExecutionResult result =
        graphQL.execute(
            "{Pet{...AllPetFields}}", Map.of(), new GraphqlExecutor.DummySessionHandler());
    String schemaUrl = "http://localhost:8080";
    Map jsonLdSchema = generateJsonLdSchemaAsMap(schema.getMetadata(), schemaUrl);
    Map data = result.getData();
    String ttl = convertToTurtle(jsonLdSchema, data);
    System.out.println(ttl);
  }

  @Test
  void testJsonGeneratorWithTypeTest() throws IOException {
    Database database = TestDatabaseFactory.getTestDatabase();
    String schemaName = "TestJsonLdTypeTest";
    database.dropSchemaIfExists(schemaName);
    TYPE_TEST.getImportTask(database, schemaName, "Type Test", true).run();
    Schema schema = database.getSchema(schemaName);
    schema.getTable("Types").insert(createTypeTestRow());
    GraphqlExecutor graphQL = new GraphqlExecutor(schema);
    String ttl = getTableAsTurtle(graphQL, "Types");
    System.out.println(ttl);
    ttl = getAllAsTurtle(graphQL, "http://localhost");
    System.out.println(ttl);
  }

  @Test
  void testStripJsonLdKeywords() {
    Map<String, Object> nested = new LinkedHashMap<>();
    nested.put("@id", "http://example.com/1");
    nested.put("@type", "Person");
    nested.put("name", "John");
    nested.put("age", 30);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("@context", "http://schema.org");
    data.put("@id", "http://example.com/root");
    data.put("title", "Test");
    data.put("nested", nested);
    data.put(
        "items",
        List.of(Map.of("@id", "item1", "value", "a"), Map.of("@id", "item2", "value", "b")));

    Map<String, Object> result = stripJsonLdKeywords(data);

    assertEquals("Test", result.get("title"));
    assertTrue(!result.containsKey("@context"));
    assertTrue(!result.containsKey("@id"));

    Map<String, Object> nestedResult = (Map<String, Object>) result.get("nested");
    assertEquals("John", nestedResult.get("name"));
    assertEquals(30, nestedResult.get("age"));
    assertTrue(!nestedResult.containsKey("@id"));
    assertTrue(!nestedResult.containsKey("@type"));

    List<Map<String, Object>> itemsResult = (List<Map<String, Object>>) result.get("items");
    assertEquals(2, itemsResult.size());
    assertEquals("a", itemsResult.get(0).get("value"));
    assertEquals("b", itemsResult.get(1).get("value"));
    assertTrue(!itemsResult.get(0).containsKey("@id"));
    assertTrue(!itemsResult.get(1).containsKey("@id"));
  }

  @Test
  void testImportJsonLd() throws IOException {
    Map<String, Object> testData = new LinkedHashMap<>();
    testData.put("@id", "http://example.com/test1");
    testData.put("@type", "TestType");
    testData.put("name", "Test Name");
    testData.put("value", 123);
    testData.put("nested", Map.of("@id", "http://example.com/nested", "nestedValue", "abc"));

    Map<String, Object> jsonLdData = new LinkedHashMap<>();
    jsonLdData.put("@context", "http://schema.org");
    jsonLdData.put("data", testData);

    Map<String, Object> stripped = stripJsonLdKeywords(testData);

    assertEquals("Test Name", stripped.get("name"));
    assertEquals(123, stripped.get("value"));
    assertTrue(!stripped.containsKey("@id"));
    assertTrue(!stripped.containsKey("@type"));

    Map<String, Object> nestedStripped = (Map<String, Object>) stripped.get("nested");
    assertEquals("abc", nestedStripped.get("nestedValue"));
    assertTrue(!nestedStripped.containsKey("@id"));
  }

  public Row createTypeTestRow() {
    Row row = new Row();
    row.set("string type", "string");
    row.set("string array type", new String[] {"string1", "string2"});

    row.set("string text", "text");
    row.set("string array text", new String[] {"text1", "text2"});

    row.set("json type", "{\"key\":\"value\"}");

    row.set("email type", "email");
    row.set("email array type", new String[] {"email1@example.com", "email2@example.com"});

    row.set("hyperlink type", "hyperlink");
    row.set("hyperlink array type", new String[] {"http://example.com/1", "http://example.com/2"});

    row.set("bool type", true);
    row.set("bool array type", new Boolean[] {true, false});
    row.set("uuid type", "550e8400-e29b-41d4-a716-446655440000");
    row.set(
        "uuid array type",
        new String[] {
          "550e8400-e29b-41d4-a716-446655440000", "550e8400-e29b-41d4-a716-446655440001"
        });
    row.set("file type", "file_example.txt");

    row.set("int type", 42);
    row.set("int array type", new Integer[] {1, 2, 3});
    row.set("long type", 123456789L);
    row.set("long array type", new Long[] {100000000L, 200000000L});
    row.set("decimal type", 3.1415);
    row.set("decimal array type", new Double[] {2.718, 1.618});

    row.set("date type", "2025-10-19");
    row.set("date array type", new String[] {"2025-10-19", "2025-10-20"});
    row.set("datetime type", "2025-10-19T12:34:56");
    row.set("datetime array type", new String[] {"2025-10-19T12:34:56", "2025-10-20T14:00:00"});

    row.set("period type", "P30D");
    row.set("period array type", new String[] {"P30D", "P1M", "P1Y2M10D"});

    return row;
  }
}
