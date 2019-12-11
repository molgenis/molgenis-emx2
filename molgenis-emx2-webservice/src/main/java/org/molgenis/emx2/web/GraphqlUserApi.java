package org.molgenis.emx2.web;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.Database;
import spark.Request;

import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.molgenis.emx2.web.GraphqlTypes.*;

public class GraphqlUserApi {

  private static final String USERNAME = "username";
  private static final String PASSWORD = "password"; // NOSONAR

  private Database database;
  private String username;

  public GraphqlUserApi(Database database) {
    this.database = database;
  }

  public ExecutionResult execute(String query, Request request) {
    ExecutionResult result = graphql.execute(query);
    if (username != null) {
      // set the session
      request.session(true).attribute("username", username);
    }
    return result;
  }

  private final GraphQLFieldDefinition logoutMutationField =
      newFieldDefinition()
          .name("logout")
          .type(typeForMutationResult)
          .dataFetcher(
              dataFetchingEnvironment -> {
                database.clearActiveUser();
                return new GrahpqlUserApiException("User logged out");
              })
          .build();

  private final GraphQLFieldDefinition registerMutationField =
      newFieldDefinition()
          .name("register")
          .type(typeForMutationResult)
          .argument(GraphQLArgument.newArgument().name(USERNAME).type(Scalars.GraphQLString))
          .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
          .argument(
              GraphQLArgument.newArgument().name(PASSWORD + "_REPEAT").type(Scalars.GraphQLString))
          .dataFetcher(
              dataFetchingEnvironment -> {
                database.addUser(USERNAME);
                return new GrahpqlUserApiException("User added");
              })
          .build();

  private final GraphQLFieldDefinition loginMutationField =
      newFieldDefinition()
          .name("login")
          .type(typeForMutationResult)
          .argument(GraphQLArgument.newArgument().name(USERNAME).type(Scalars.GraphQLString))
          .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
          .dataFetcher(
              dataFetchingEnvironment -> {
                String userName = dataFetchingEnvironment.getArgument(USERNAME);
                String passWord = dataFetchingEnvironment.getArgument(PASSWORD);

                // todo, fake it for now
                if (database.hasUser(userName)) {
                  this.username = userName;
                  return new GrahpqlUserApiException("Succesfull login");
                } else {
                  throw new GrahpqlUserApiException("Invalid login");
                }
              })
          .build();

  private final GraphQLFieldDefinition userQueryField =
      newFieldDefinition()
          .name("User")
          .type(
              newObject()
                  .name("User")
                  .field(newFieldDefinition().name(USERNAME).type(Scalars.GraphQLString)))
          .dataFetcher(
              dataFetchingEnvironment -> {
                return database.getActiveUser() != null
                    ? Map.of(USERNAME, database.getActiveUser())
                    : Map.of(USERNAME, "none");
              })
          .build();

  private final GraphQL graphql =
      GraphQL.newGraphQL(
              GraphQLSchema.newSchema()
                  .query(newObject().name("Query").field(userQueryField))
                  .mutation(
                      newObject()
                          .name("Mutation")
                          .field(logoutMutationField)
                          .field(registerMutationField)
                          .field(loginMutationField))
                  .build())
          .build();
}
