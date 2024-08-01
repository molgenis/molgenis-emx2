package org.molgenis.emx2.utils;

import java.util.Map;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

public class JavaScriptBindings {

  @FunctionalInterface
  public interface SimplePostClient {
    Object execute(String query, Map<String, Object> variables, String schemaId);
  }

  public static SimplePostClient simplePostClient =
      (query, variables, schemaId) -> {
        System.out.println(query);
        System.out.println(variables);
        System.out.println(schemaId);
        GraphqlApiFactory graphqlApiFactory = new GraphqlApiFactory();
        // Todo: do the data fetching;
        return null;
      };
}
