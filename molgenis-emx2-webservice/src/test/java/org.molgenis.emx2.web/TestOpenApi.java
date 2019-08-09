package org.molgenis.emx2.web;

import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.*;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.beans.SchemaMetadata;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;

import static org.molgenis.Type.*;

public class TestOpenApi {

  @Test
  public void constructApi() throws MolgenisException, IOException, URISyntaxException {
    org.molgenis.Schema schema = new SchemaMetadata("test");

    Table table = schema.createTableIfNotExists("TypeTest");
    for (Type type : Type.values()) {
      if (MREF.equals(type) || REF.equals(type) || REF_ARRAY.equals(type)) {
        // TODO: outside of test for now
      } else {
        table.addColumn(type.toString().toLowerCase() + "Column", type);
      }
    }

    Table personTable = schema.createTableIfNotExists("Person");
    personTable.addColumn("First Name", STRING);
    personTable.addColumn("Last Name", STRING);

    OpenAPI api = OpenApiFactory.create(schema);

    StringWriter writer = new StringWriter();
    Yaml.pretty().writeValue(writer, api);
    System.out.println(writer);
  }
}
