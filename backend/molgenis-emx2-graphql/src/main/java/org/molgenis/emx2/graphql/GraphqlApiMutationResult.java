package org.molgenis.emx2.graphql;

import graphql.Scalars;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.FAILED;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;

public class GraphqlApiMutationResult {
  public enum Status {
    SUCCESS,
    FAILED,
    UNKNOWN
  }

  private String message;
  private Status status;
  private Map<String, String> details = new LinkedHashMap<>();
  private String code;

  public GraphqlApiMutationResult(Status status, String message, Object... formatValues) {
    this.status = status;
    this.message = String.format(message, formatValues);
  }

  public static final GraphQLEnumType enumMutationResultStatus =
      GraphQLEnumType.newEnum()
          .name("MolgenisMutationResultStatus")
          .value("SUCCESS", SUCCESS)
          .value("FAILED", FAILED)
          .build();

  public static final GraphQLObjectType typeForMutationResult =
      GraphQLObjectType.newObject()
          .name("MolgenisResult")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("status")
                  .type(enumMutationResultStatus)
                  .build())
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("message")
                  .type(Scalars.GraphQLString)
                  .build())
          .build();
}
