package org.molgenis.emx2.web;

import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.Database;

import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.molgenis.emx2.web.Constants.ANONYMOUS;
import static org.molgenis.emx2.web.GraphqlApi.typeForMutationResult;

public class GraphqlUserFields {

  private static final String USERNAME = "username";
  private static final String PASSWORD = "password"; // NOSONAR

  private GraphqlUserFields() {
    // no instance
  }

  public static GraphQLFieldDefinition logoutField(Database database) {
    return newFieldDefinition()
        .name("_logout")
        .type(typeForMutationResult)
        .dataFetcher(
            dataFetchingEnvironment -> {
              database.setActiveUser(ANONYMOUS);
              return new GrahpqlUserApiException("User logged out");
            })
        .build();
  }

  public static GraphQLFieldDefinition registerField(Database database) {
    return newFieldDefinition()
        .name("_register")
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
  }

  public static GraphQLFieldDefinition loginField(Database database) {
    return newFieldDefinition()
        .name("_login")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(USERNAME).type(Scalars.GraphQLString))
        .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String userName = dataFetchingEnvironment.getArgument(USERNAME);
              String passWord = dataFetchingEnvironment.getArgument(PASSWORD);

              // todo, fake password for now
              if (database.hasUser(userName)) {
                database.setActiveUser(userName);
                return new GrahpqlUserApiException("Succesfull login");
              } else {
                throw new GrahpqlUserApiException("Invalid login");
              }
            })
        .build();
  }

  public static GraphQLFieldDefinition userQueryField(Database database) {
    return newFieldDefinition()
        .name("_user")
        .type(
            newObject()
                .name("MolgenisUser")
                .field(newFieldDefinition().name(USERNAME).type(Scalars.GraphQLString)))
        .dataFetcher(
            dataFetchingEnvironment ->
                database.getActiveUser() != null
                    ? Map.of(USERNAME, database.getActiveUser())
                    : Map.of(USERNAME, "none"))
        .build();
  }
}
