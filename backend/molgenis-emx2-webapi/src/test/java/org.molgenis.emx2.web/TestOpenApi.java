package org.molgenis.emx2.web;

import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.*;
import org.junit.Assert;
import org.junit.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.ColumnType;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;

public class TestOpenApi {

  @Test
  public void constructApi() throws IOException, URISyntaxException {
    SchemaMetadata schema = new SchemaMetadata("test");

    TableMetadata table = schema.create(TableMetadata.table("TypeTest"));
    for (ColumnType columnType : ColumnType.values()) {
      if (
      // MREF.equals(columnType) ||
      ColumnType.REF.equals(columnType)
          || ColumnType.REF_ARRAY.equals(columnType)
          || ColumnType.REFBACK.equals(columnType)) {
        // TODO: outside of test for now
      } else {
        table.add(Column.column(columnType.toString().toLowerCase() + "Column").type(columnType));
      }
    }

    TableMetadata personTable =
        schema.create(
            TableMetadata.table("Person")
                .add(Column.column("First Name"))
                .add(Column.column("Last Name")));

    OpenAPI api = OpenApiYamlGenerator.createOpenApi(schema);
    Assert.assertEquals(1, api.getComponents().getSchemas().size()); // useless test

    OpenAPI api2 = CsvApi.getOpenAPI(schema);
    Assert.assertEquals(1, api.getComponents().getSchemas().size()); // useless test

    StringWriter writer = new StringWriter();
    Yaml.pretty().writeValue(writer, api);
    System.out.println(writer);
  }
}
