package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.enumMutationResultStatus;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

public class GraphqlApiMutationResultWithToken {
  private String message;
  private GraphqlApiMutationResult.Status status;
  private String token;

  public GraphqlApiMutationResultWithToken(
      GraphqlApiMutationResult.Status status,
      String token,
      String message,
      Object... formatValues) {
    this.status = status;
    this.message = String.format(message, formatValues);
    this.token = token;
  }

  public static final GraphQLObjectType typeForSignResult =
      GraphQLObjectType.newObject()
          .name("SigninResult")
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
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("token")
                  .type(Scalars.GraphQLString)
                  .build())
          .build();
}
