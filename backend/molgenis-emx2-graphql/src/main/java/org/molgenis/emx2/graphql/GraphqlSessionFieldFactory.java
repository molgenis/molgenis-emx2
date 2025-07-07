package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.SETTINGS;
import static org.molgenis.emx2.graphql.GraphqlAdminFieldFactory.mapSettingsToGraphql;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.FAILED;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.graphql.GraphqlSchemaFieldFactory.outputSettingsType;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.JWTgenerator;

public class GraphqlSessionFieldFactory {

  public GraphqlSessionFieldFactory() {
    // no instance
  }

  public GraphQLFieldDefinition signoutField(UserSession session) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("signout")
        .type(GraphqlApiMutationResult.typeForMutationResult)
        .dataFetcher(
            dataFetchingEnvironment -> {
              String user = session.getSessionUser();
              session.signOut();
              return new GraphqlApiMutationResult(
                  GraphqlApiMutationResult.Status.SUCCESS, "User '%s' has signed out", user);
            })
        .build();
  }

  public GraphQLFieldDefinition signupField(UserSession session) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("signup")
        .type(GraphqlApiMutationResult.typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString))
        .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String userName = dataFetchingEnvironment.getArgument(EMAIL);
              String passWord = dataFetchingEnvironment.getArgument(PASSWORD);
              try {
                session.signUp(userName, passWord);
              } catch (MolgenisException e) {
                return new GraphqlApiMutationResult(FAILED, e.getMessage());
              }
              return new GraphqlApiMutationResult(
                  GraphqlApiMutationResult.Status.SUCCESS, "User '%s' added", userName);
            })
        .build();
  }

  public GraphQLFieldDefinition signinField(UserSession session) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("signin")
        .type(GraphqlApiMutationResultWithToken.typeForSignResult)
        .argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString))
        .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String userName = dataFetchingEnvironment.getArgument(EMAIL);
              String passWord = dataFetchingEnvironment.getArgument(PASSWORD);
              try {
                session.signIn(userName, passWord);
                GraphqlApiMutationResultWithToken result =
                    new GraphqlApiMutationResultWithToken(
                        GraphqlApiMutationResult.Status.SUCCESS,
                        JWTgenerator.createTemporaryToken(session.getDatabase(), userName),
                        "Signed in as '%s'",
                        userName);
                return result;
              } catch (MolgenisException e) {
                return new GraphqlApiMutationResult(FAILED, e.getMessage());
              }
            })
        .build();
  }

  public GraphQLFieldDefinition sessionQueryField(Database database, Schema schema) {
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
                        .name(ADMIN)
                        .type(Scalars.GraphQLBoolean))
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(ROLES)
                        .type(GraphQLList.list(Scalars.GraphQLString)))
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(SCHEMAS)
                        .type(GraphQLList.list(Scalars.GraphQLString)))
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(SETTINGS)
                        .type(GraphQLList.list(outputSettingsType)))
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(TOKEN)
                        .type(Scalars.GraphQLString)))
        .dataFetcher(
            dataFetchingEnvironment -> {
              Map<String, Object> result = new LinkedHashMap<>();
              result.put(
                  EMAIL, database.getActiveUser() != null ? database.getActiveUser() : "anonymous");
              result.put(ADMIN, database.isAdmin());
              if (schema != null) {
                result.put(ROLES, schema.getInheritedRolesForActiveUser());
              }
              result.put(SCHEMAS, database.getSchemaNames());
              User user = database.getUser(database.getActiveUser());
              result.put(
                  SETTINGS, user != null ? mapSettingsToGraphql(user.getSettings()) : Map.of());
              result.put(
                  TOKEN, JWTgenerator.createTemporaryToken(database, database.getActiveUser()));
              return result;
            })
        .build();
  }

  public GraphQLFieldDefinition createTokenField(Database database) {
    GraphQLFieldDefinition.Builder builder =
        GraphQLFieldDefinition.newFieldDefinition()
            .name("createToken")
            .type(GraphqlApiMutationResultWithToken.typeForSignResult);
    builder.argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString));
    return builder
        .argument(GraphQLArgument.newArgument().name(TOKEN_NAME).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String tokenId = dataFetchingEnvironment.getArgument(TOKEN_NAME);
              String userName = dataFetchingEnvironment.getArgument(EMAIL);
              if (!database.isAdmin() && !userName.equals(database.getActiveUser())) {
                throw new MolgenisException(
                    "Create token failed: Only admins can create tokens for other users");
              }
              return new GraphqlApiMutationResultWithToken(
                  GraphqlApiMutationResult.Status.SUCCESS,
                  JWTgenerator.createNamedTokenForUser(database, userName, tokenId),
                  "Token '%s' created for user '%s'",
                  tokenId,
                  userName);
            })
        .build();
  }

  public GraphQLFieldDefinition changePasswordField(Database database) {
    GraphQLFieldDefinition.Builder builder =
        GraphQLFieldDefinition.newFieldDefinition()
            .name("changePassword")
            .type(typeForMutationResult);
    if (database.isAdmin()) {
      builder.argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString));
    }
    return builder
        .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String password = dataFetchingEnvironment.getArgument(PASSWORD);
              String username = dataFetchingEnvironment.getArgument(EMAIL);
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
