package org.molgenis.emx2.web;

import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.*;
import org.junit.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.utils.MolgenisException;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.ColumnType.*;

public class TestOpenApi {

  @Test
  public void constructApi() throws MolgenisException, IOException, URISyntaxException {
    SchemaMetadata schema = new SchemaMetadata("test");

    TableMetadata table = schema.createTableIfNotExists("TypeTest");
    for (ColumnType columnType : ColumnType.values()) {
      if (MREF.equals(columnType) || REF.equals(columnType) || REF_ARRAY.equals(columnType)) {
        // TODO: outside of test for now
      } else {
        table.addColumn(columnType.toString().toLowerCase() + "Column", columnType);
      }
    }

    TableMetadata personTable = schema.createTableIfNotExists("Person");
    personTable.addColumn("First Name", STRING);
    personTable.addColumn("Last Name", STRING);

    OpenAPI api = OpenApiForSchemaFactory.createOpenApi(schema);
    assertEquals(7, api.getComponents().getSchemas().size()); // useless test

    StringWriter writer = new StringWriter();
    Yaml.pretty().writeValue(writer, api);
    System.out.println(writer);
  }
}
