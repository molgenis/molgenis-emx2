package org.molgenis.emx2.openapi;

import io.swagger.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.PathParameter;
import org.junit.Test;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.Table;
import org.molgenis.beans.SchemaBean;

public class TestOpenApi {

  @Test
  public void constructApi() throws MolgenisException {
    Schema schema = new SchemaBean("test");

    Table table = schema.createTableIfNotExists("Pet");

    OpenAPI api = new OpenAPI();

    Paths paths = new Paths();

    for (String tableName : schema.getTableNames()) {
      Table t = schema.getTable(tableName);

      PathItem pi = new PathItem();

      Operation get = new Operation();
      get.summary("retrieve");

      Operation post = new Operation().addParametersItem(new PathParameter().name("molgenisid"));

      Operation put = new Operation();

      Operation delete = new Operation();

      get.addParametersItem(new PathParameter().name("q"));
      pi.get(get);
      pi.post(post);
      pi.post(put);
      pi.post(delete);

      paths.addPathItem(table.getName(), pi);
    }

    api.setPaths(paths);

    Yaml.prettyPrint(api);
  }
}
