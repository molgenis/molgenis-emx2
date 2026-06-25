package org.molgenis.emx2.web;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.GraphqlExecutor;

public class JavaScriptBindings {

  private JavaScriptBindings() {}

  private static final String SIMPLE_POST_CLIENT = "simplePostClient";

  @FunctionalInterface
  public interface SimplePostClient {
    Object execute(String query, Map<String, Object> variables, String schemaId);
  }

  public static Map<String, Function<Database, Object>> getBindingsForUser(String username) {
    Map<String, Function<Database, Object>> bindings = new HashMap<>();
    bindings.put(
        SIMPLE_POST_CLIENT,
        database ->
            (SimplePostClient)
                (query, variables, schemaId) -> {
                  Schema schema = database.getSchema(schemaId);
                  if (schema == null) {
                    throw new MolgenisException("Schema not found: " + schemaId);
                  }
                  return new GraphqlExecutor(schema)
                      .executeWithoutSession(query, variables)
                      .getData();
                });
    return bindings;
  }
}
