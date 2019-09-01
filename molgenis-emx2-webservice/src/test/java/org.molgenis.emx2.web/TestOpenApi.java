package org.molgenis.emx2.web;

import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.*;
import org.junit.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.Type;
import org.molgenis.emx2.utils.MolgenisException;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Type.*;

public class TestOpenApi {

  @Test
  public void constructApi() throws MolgenisException, IOException, URISyntaxException {
    SchemaMetadata schema = new SchemaMetadata("test");

    TableMetadata table = schema.createTableIfNotExists("TypeTest");
    for (Type type : Type.values()) {
      if (MREF.equals(type) || REF.equals(type) || REF_ARRAY.equals(type)) {
        // TODO: outside of test for now
      } else {
        table.addColumn(type.toString().toLowerCase() + "Column", type);
      }
    }

    TableMetadata personTable = schema.createTableIfNotExists("Person");
    personTable.addColumn("First Name", STRING);
    personTable.addColumn("Last Name", STRING);

    OpenAPI api = OpenApiForSchemaFactory.createOpenApi(schema);
    assertEquals(6, api.getComponents().getSchemas().size()); //useless test

    StringWriter writer = new StringWriter();
    Yaml.pretty().writeValue(writer, api);
    System.out.println(writer);
  }
}
