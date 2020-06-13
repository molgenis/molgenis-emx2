package org.molgenis.emx2.graphql;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphqlVersionField {

  public static final String IMPLEMENTATION_VERSION = "ImplementationVersion";
  public static final String SPECIFICATION_VERSION = "SpecificationVersion";

  public static GraphQLFieldDefinition.Builder queryVersionField() {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_manifest")
        .dataFetcher(
            dataFetchingEnvironment -> {
              Map<String, String> result = new HashMap<>();
              result.put(IMPLEMENTATION_VERSION, getImplementationVersion());
              result.put(SPECIFICATION_VERSION, getSpecificationVersion());
              return result;
            })
        .type(
            GraphQLObjectType.newObject()
                .name("Manifest")
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(IMPLEMENTATION_VERSION)
                        .type(Scalars.GraphQLString)
                        .build())
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(SPECIFICATION_VERSION)
                        .type(Scalars.GraphQLString)
                        .build())
                .build());
  }

  private static String getSpecificationVersion() {
    String result = GraphqlVersionField.class.getPackage().getSpecificationVersion();
    if (result == null) return "DEVELOPMENT";
    return result;
  }

  private static String getImplementationVersion() {
    String result = GraphqlVersionField.class.getPackage().getImplementationVersion();
    if (result == null) return "DEVELOPMENT";
    return result;
  }
}
