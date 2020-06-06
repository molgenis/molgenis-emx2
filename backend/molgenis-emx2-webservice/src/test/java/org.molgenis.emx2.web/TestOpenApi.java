package org.molgenis.emx2.web;

import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.*;
import org.junit.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.ColumnType;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

public class TestOpenApi {

  @Test
  public void constructApi() throws IOException, URISyntaxException {
    SchemaMetadata schema = new SchemaMetadata("test");

    TableMetadata table = schema.create(table("TypeTest"));
    for (ColumnType columnType : ColumnType.values()) {
      if (
      // MREF.equals(columnType) ||
      REF.equals(columnType) || REF_ARRAY.equals(columnType) || REFBACK.equals(columnType)) {
        // TODO: outside of test for now
      } else {
        table.add(column(columnType.toString().toLowerCase() + "Column").type(columnType));
      }
    }

    TableMetadata personTable =
        schema.create(table("Person").add(column("First Name")).add(column("Last Name")));

    OpenAPI api = OpenApiYamlGenerator.createOpenApi(schema);
    assertEquals(1, api.getComponents().getSchemas().size()); // useless test

    StringWriter writer = new StringWriter();
    Yaml.pretty().writeValue(writer, api);
    System.out.println(writer);
  }
}
