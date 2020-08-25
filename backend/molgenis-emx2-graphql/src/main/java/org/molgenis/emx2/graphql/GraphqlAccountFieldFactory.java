package org.molgenis.emx2.graphql;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.molgenis.emx2.Database;

import java.util.Map;

public class GraphqlAccountFieldFactory {

  public static final String EMAIL = "email";
  private static final String PASSWORD = "password"; // NOSONAR

  public GraphqlAccountFieldFactory() {
    // no instance
  }

  public GraphQLFieldDefinition signoutField(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("signout")
        .type(GraphqlApiMutationResult.typeForMutationResult)
        .dataFetcher(
            dataFetchingEnvironment -> {
              String user = database.getActiveUser();
              database.setActiveUser(GraphqlConstants.ANONYMOUS);
              return new GraphqlApiMutationResult(
                  GraphqlApiMutationResult.Status.SUCCESS, "User '%s' has signed out", user);
            })
        .build();
  }

  public GraphQLFieldDefinition signupField(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("signup")
        .type(GraphqlApiMutationResult.typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString))
        .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String userName = dataFetchingEnvironment.getArgument(EMAIL);
              String passWord = dataFetchingEnvironment.getArgument(PASSWORD);
              if (passWord == null) {
                return new GraphqlApiMutationResult(
                    GraphqlApiMutationResult.Status.FAILED, "Password cannot be not null");
              }
              if (passWord.length() < 8) {
                return new GraphqlApiMutationResult(
                    GraphqlApiMutationResult.Status.FAILED, "Password too short");
              }
              database.tx(
                  db -> {
                    db.addUser(userName);
                    db.setUserPassword(userName, passWord);
                  });
              return new GraphqlApiMutationResult(
                  GraphqlApiMutationResult.Status.SUCCESS, "User '%s' added", userName);
            })
        .build();
  }

  public GraphQLFieldDefinition signinField(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("signin")
        .type(GraphqlApiMutationResult.typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString))
        .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String userName = dataFetchingEnvironment.getArgument(EMAIL);
              String passWord = dataFetchingEnvironment.getArgument(PASSWORD);

              // todo, fake password for now
              if (database.hasUser(userName) && database.checkUserPassword(userName, passWord)) {
                database.setActiveUser(userName);
                return new GraphqlApiMutationResult(
                    GraphqlApiMutationResult.Status.SUCCESS, "Signed in as '%s'", userName);
              } else {
                return new GraphqlApiMutationResult(
                    GraphqlApiMutationResult.Status.FAILED,
                    "Sign in as '%s' failed: user or password unknown",
                    userName);
              }
            })
        .build();
  }

  public GraphQLFieldDefinition userQueryField(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_user")
        .type(
            GraphQLObjectType.newObject()
                .name("MolgenisUser")
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(EMAIL)
                        .type(Scalars.GraphQLString)))
        .dataFetcher(
            dataFetchingEnvironment ->
                database.getActiveUser() != null
                    ? Map.of(EMAIL, database.getActiveUser())
                    : Map.of(EMAIL, "none"))
        .build();
  }
}
