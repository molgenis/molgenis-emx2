package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.enumMutationResultStatus;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;

public class GraphqlApiMutationResultWithToken {
  private final String message;
  private final GraphqlApiMutationResult.Status status;
  private final String token;
  private final String user;

  public GraphqlApiMutationResultWithToken(
      GraphqlApiMutationResult.Status status, String token, String message, String userName) {
    this.status = status;
    this.message = message;
    this.token = token;
    this.user = userName;
  }

  public static final GraphQLObjectType typeForSignResult =
      GraphQLObjectType.newObject()
          .name("SigninResult")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.STATUS)
                  .type(enumMutationResultStatus)
                  .build())
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.MESSAGE)
                  .type(Scalars.GraphQLString)
                  .build())
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.TOKEN)
                  .type(Scalars.GraphQLString)
                  .build())
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.USER)
                  .type(Scalars.GraphQLString)
                  .build())
          .build();
}
