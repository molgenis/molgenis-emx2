package org.molgenis.emx2.web.graphql;

import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.Database;

import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.molgenis.emx2.web.Constants.ANONYMOUS;
import static org.molgenis.emx2.web.graphql.GraphqlApiMutationResult.Status.FAILED;
import static org.molgenis.emx2.web.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.web.graphql.GraphqlApiMutationResult.typeForMutationResult;

public class GraphqlAccountFields {

  public static final String EMAIL = "email";
  private static final String PASSWORD = "password"; // NOSONAR

  private GraphqlAccountFields() {
    // no instance
  }

  public static GraphQLFieldDefinition signoutField(Database database) {
    return newFieldDefinition()
        .name("signout")
        .type(typeForMutationResult)
        .dataFetcher(
            dataFetchingEnvironment -> {
              String user = database.getActiveUser();
              database.setActiveUser(ANONYMOUS);
              return new GraphqlApiMutationResult(SUCCESS, "User '%s' has signed out", user);
            })
        .build();
  }

  public static GraphQLFieldDefinition signupField(Database database) {
    return newFieldDefinition()
        .name("signup")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString))
        .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String userName = dataFetchingEnvironment.getArgument(EMAIL);
              String passWord = dataFetchingEnvironment.getArgument(PASSWORD);
              if (passWord == null) {
                return new GraphqlApiMutationResult(FAILED, "Password cannot be not null");
              }
              if (passWord.length() < 8) {
                return new GraphqlApiMutationResult(FAILED, "Password too short");
              }
              database.tx(
                  db -> {
                    db.addUser(userName);
                    db.setUserPassword(userName, passWord);
                  });
              return new GraphqlApiMutationResult(SUCCESS, "User '%s' added", userName);
            })
        .build();
  }

  public static GraphQLFieldDefinition signinField(Database database) {
    return newFieldDefinition()
        .name("signin")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString))
        .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String userName = dataFetchingEnvironment.getArgument(EMAIL);
              String passWord = dataFetchingEnvironment.getArgument(PASSWORD);

              // todo, fake password for now
              if (database.hasUser(userName) && database.checkUserPassword(userName, passWord)) {
                database.setActiveUser(userName);
                return new GraphqlApiMutationResult(SUCCESS, "Signed in as '%s'", userName);
              } else {
                return new GraphqlApiMutationResult(
                    FAILED, "Sign in as '%s' failed: user or password unknown", userName);
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
                .field(newFieldDefinition().name(EMAIL).type(Scalars.GraphQLString)))
        .dataFetcher(
            dataFetchingEnvironment ->
                database.getActiveUser() != null
                    ? Map.of(EMAIL, database.getActiveUser())
                    : Map.of(EMAIL, "none"))
        .build();
  }
}
