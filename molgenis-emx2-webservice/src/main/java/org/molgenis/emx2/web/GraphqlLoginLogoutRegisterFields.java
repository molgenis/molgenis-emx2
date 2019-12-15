package org.molgenis.emx2.web;

import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.Database;

import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.molgenis.emx2.web.Constants.ANONYMOUS;
import static org.molgenis.emx2.web.GraphqlApiMutationResult.Status.FAILED;
import static org.molgenis.emx2.web.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.web.GraphqlApiMutationResult.typeForMutationResult;

public class GraphqlLoginLogoutRegisterFields {

  private static final String USERNAME = "username";
  private static final String PASSWORD = "password"; // NOSONAR

  private GraphqlLoginLogoutRegisterFields() {
    // no instance
  }

  public static GraphQLFieldDefinition logoutField(Database database) {
    return newFieldDefinition()
        .name("logout")
        .type(typeForMutationResult)
        .dataFetcher(
            dataFetchingEnvironment -> {
              String user = database.getActiveUser();
              database.setActiveUser(ANONYMOUS);
              return new GraphqlApiMutationResult(SUCCESS, "User '%s' logged out", user);
            })
        .build();
  }

  public static GraphQLFieldDefinition registerField(Database database) {
    return newFieldDefinition()
        .name("register")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(USERNAME).type(Scalars.GraphQLString))
        .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
        .argument(
            GraphQLArgument.newArgument().name(PASSWORD + "_REPEAT").type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String userName = dataFetchingEnvironment.getArgument(USERNAME);
              database.addUser(userName);
              return new GraphqlApiMutationResult(SUCCESS, "User '%s' added", userName);
            })
        .build();
  }

  public static GraphQLFieldDefinition loginField(Database database) {
    return newFieldDefinition()
        .name("login")
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
                return new GraphqlApiMutationResult(SUCCESS, "Logged in as '%s'", userName);
              } else {
                return new GraphqlApiMutationResult(FAILED, "Login as '%s' failed", userName);
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
