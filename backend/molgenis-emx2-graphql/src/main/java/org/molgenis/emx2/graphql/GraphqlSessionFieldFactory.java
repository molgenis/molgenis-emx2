package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.FAILED;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.JWTgenerator;
import org.molgenis.emx2.Schema;

public class GraphqlSessionFieldFactory {

  private static final String ROLES = "roles";
  private static final String SCHEMAS = "schemas";

  public GraphqlSessionFieldFactory() {
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
                return new GraphqlApiMutationResult(FAILED, "Password cannot be not null");
              }
              if (passWord.length() < 8) {
                return new GraphqlApiMutationResult(FAILED, "Password too short");
              }
              if (database.hasUser(userName)) {
                return new GraphqlApiMutationResult(FAILED, "Username already exists");
              }
              database.tx(
                  db -> {
                    // uplift permissions
                    String activeUser = db.getActiveUser();
                    try {
                      db.becomeAdmin();
                      db.addUser(userName);
                      db.setUserPassword(userName, passWord);
                    } finally {
                      // always lift down again
                      db.setActiveUser(activeUser);
                    }
                  });
              return new GraphqlApiMutationResult(
                  GraphqlApiMutationResult.Status.SUCCESS, "User '%s' added", userName);
            })
        .build();
  }

  public GraphQLFieldDefinition signinField(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("signin")
        .type(GraphqlApiSigninResult.typeForSignResult)
        .argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString))
        .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String userName = dataFetchingEnvironment.getArgument(EMAIL);
              String passWord = dataFetchingEnvironment.getArgument(PASSWORD);

              if (database.hasUser(userName) && database.checkUserPassword(userName, passWord)) {
                database.setActiveUser(userName);
                GraphqlApiSigninResult result =
                    new GraphqlApiSigninResult(
                        GraphqlApiMutationResult.Status.SUCCESS,
                        JWTgenerator.createTokenForUser(userName, 60),
                        "Signed in as '%s'",
                        userName);
                return result;
              } else {
                return new GraphqlApiMutationResult(
                    FAILED, "Sign in as '%s' failed: user or password unknown", userName);
              }
            })
        .build();
  }

  public GraphQLFieldDefinition userQueryField(Database database, Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_session")
        .type(
            GraphQLObjectType.newObject()
                .name("MolgenisSession")
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(EMAIL)
                        .type(Scalars.GraphQLString))
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(ROLES)
                        .type(GraphQLList.list(Scalars.GraphQLString)))
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(SCHEMAS)
                        .type(GraphQLList.list(Scalars.GraphQLString))))
        .dataFetcher(
            dataFetchingEnvironment -> {
              Map<String, Object> result = new LinkedHashMap<>();
              result.put(
                  EMAIL, database.getActiveUser() != null ? database.getActiveUser() : "anonymous");
              if (schema != null) {
                result.put(ROLES, schema.getInheritedRolesForActiveUser());
              }
              result.put(SCHEMAS, database.getSchemaNames());
              return result;
            })
        .build();
  }

  public GraphQLFieldDefinition changePasswordField(Database database) {
    GraphQLFieldDefinition.Builder builder =
        GraphQLFieldDefinition.newFieldDefinition()
            .name("changePassword")
            .type(typeForMutationResult);
    if (database.isAdmin()) {
      builder.argument(GraphQLArgument.newArgument().name(USERNAME).type(Scalars.GraphQLString));
    }
    return builder
        .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String password = dataFetchingEnvironment.getArgument(PASSWORD);
              String username = dataFetchingEnvironment.getArgument(USERNAME);
              if (username == null) {
                username = database.getActiveUser();
              }
              if (password != null) {
                database.setUserPassword(username, password);
                return new GraphqlApiMutationResult(SUCCESS, "Password changed");
              } else {
                return new GraphqlApiMutationResult(FAILED, "Password not changed: empty");
              }
            })
        .build();
  }
}
