package org.molgenis.emx2.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import org.junit.Test;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.Table;
import org.molgenis.beans.SchemaBean;

public class TestOpenApi {

    @Test void constructApi() throws MolgenisException {
        Schema schema = new SchemaBean("test");


Table table = schema.createTableIfNotExists("Pet");


        OpenAPI api = new OpenAPI();

        Paths paths = new Paths();

        for(String tableName: schema.getTableNames()) {
            Table t = schema.getTable(tableName);
            paths.addPathItem("test");
        }

        System.out.println(api.toString());


    }
}
