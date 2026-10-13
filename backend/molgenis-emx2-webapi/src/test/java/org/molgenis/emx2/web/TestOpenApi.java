package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

@Tag("slow")
public class TestOpenApi {

  @Test
  public void constructApi() throws IOException {
    SchemaMetadata schema = new SchemaMetadata("test");

    TableMetadata table = schema.create(TableMetadata.table("TypeTest"));
    for (ColumnType columnType : ColumnType.values()) {
      if (columnType.isReference()) {
        // TODO: outside of test for now
      } else {
        table.add(
            Column.column(columnType.toString().toLowerCase() + "Column").setType(columnType));
      }
    }

    TableMetadata personTable =
        schema.create(
            TableMetadata.table("Person")
                .add(Column.column("First_Name"))
                .add(Column.column("Last_Name")));

    OpenAPI api = OpenApiYamlGenerator.createOpenApi(schema);
    assertEquals(3, api.getComponents().getSchemas().size()); // useless test

    assertEquals(3, api.getComponents().getSchemas().size()); // useless test

    StringWriter writer = new StringWriter();
    Yaml.pretty().writeValue(writer, api);
    System.out.println(writer);
  }

  private static final String JSON_PREFIX = "/test/api/json";
  private static final String REF_PREFIX = "#/components/schemas/";

  private static OpenAPI jsonApi() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata pet =
        schema.create(
            TableMetadata.table("Pet")
                .add(Column.column("name").setPkey())
                .add(Column.column("weight").setType(ColumnType.DECIMAL))
                .add(Column.column("tags").setType(ColumnType.STRING_ARRAY))
                .add(Column.column("mg_tableclass"))
                .add(Column.column("mg_insertedOn")));
    schema.create(
        TableMetadata.table("Pet Owner")
            .add(Column.column("owner name").setPkey())
            .add(Column.column("age").setType(ColumnType.INT))
            .add(Column.column("pet").setType(ColumnType.REF).setRefTable("Pet")));
    pet.add(
        Column.column("owners")
            .setType(ColumnType.REFBACK)
            .setRefTable("Pet Owner")
            .setRefBack("pet"));
    return OpenApiYamlGenerator.createOpenApi(schema);
  }

  private static Schema<?> rowSchema(OpenAPI api, String ref) {
    assertTrue(ref.startsWith(REF_PREFIX), ref);
    return api.getComponents().getSchemas().get(ref.substring(REF_PREFIX.length()));
  }

  private static String requestRef(Operation operation) {
    Schema<?> body = operation.getRequestBody().getContent().get("application/json").getSchema();
    return ((ArraySchema) body).getItems().get$ref();
  }

  private static void assertTablePath(
      OpenAPI api, String table, List<String> columns, List<String> otherColumns) {
    PathItem path = api.getPaths().get(JSON_PREFIX + "/" + table);
    assertNotNull(path, table);
    List<Operation> writes =
        List.of(path.getPost(), path.getPut(), path.getPatch(), path.getDelete());
    assertNotNull(path.getGet());
    String ref = null;
    for (Operation write : writes) {
      assertNotNull(write, table);
      String writeRef = requestRef(write);
      assertTrue(ref == null || ref.equals(writeRef));
      ref = writeRef;
    }
    Schema<?> getItems =
        ((ArraySchema)
                path.getGet()
                    .getResponses()
                    .get("200")
                    .getContent()
                    .get("application/json")
                    .getSchema())
            .getItems();
    assertEquals(ref, getItems.get$ref());
    Schema<?> row = rowSchema(api, ref);
    assertTrue(row.getProperties().keySet().containsAll(columns), row.getProperties().toString());
    for (String other : otherColumns) {
      assertFalse(row.getProperties().containsKey(other), other);
    }
  }

  @Test
  public void jsonTablePathsCarryTheirRowSchema() {
    OpenAPI api = jsonApi();
    assertTablePath(
        api,
        "Pet",
        List.of("name", "weight", "tags", "mg_tableclass"),
        List.of("ownerName", "age"));
    assertTablePath(
        api, "Pet Owner", List.of("ownerName", "age"), List.of("name", "weight", "tags"));
    Schema<?> pet = rowSchema(api, requestRef(api.getPaths().get(JSON_PREFIX + "/Pet").getPost()));
    assertFalse(pet.getProperties().containsKey("owners"));
    assertFalse(pet.getProperties().containsKey("mg_insertedOn"));
    assertEquals("array", pet.getProperties().get("tags").getType());
    assertEquals("string", ((ArraySchema) pet.getProperties().get("tags")).getItems().getType());
  }

  @Test
  public void jsonTablePathsListQueryParametersAndStrict() {
    PathItem path = jsonApi().getPaths().get(JSON_PREFIX + "/Pet");
    assertEquals(
        List.of("filter", "orderby", "search", "limit", "offset", "query"),
        path.getGet().getParameters().stream().map(Parameter::getName).toList());
    assertEquals(
        List.of("strict"),
        path.getDelete().getParameters().stream().map(Parameter::getName).toList());
  }

  @Test
  public void jsonDataAndSchemaPathsHaveTheirVerbs() {
    OpenAPI api = jsonApi();
    PathItem data = api.getPaths().get(JSON_PREFIX + "/_data");
    assertNotNull(data);
    assertNotNull(data.getGet());
    assertNotNull(data.getPost());
    assertNotNull(data.getPut());
    assertNotNull(data.getPatch());
    assertNotNull(data.getDelete());
    PathItem schema = api.getPaths().get(JSON_PREFIX + "/_schema");
    assertNotNull(schema);
    assertNotNull(schema.getGet());
    assertNotNull(schema.getPost());
    assertNotNull(schema.getDelete());
    assertNull(schema.getPut());
    assertNull(schema.getPatch());
  }
}
