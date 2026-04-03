package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.io.StringWriter;
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
    assertEquals(1, api.getComponents().getSchemas().size()); // useless test

    assertEquals(1, api.getComponents().getSchemas().size()); // useless test

    StringWriter writer = new StringWriter();
    Yaml.pretty().writeValue(writer, api);
    System.out.println(writer);
  }
}
