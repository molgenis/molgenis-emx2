package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.SETTINGS;
import static org.molgenis.emx2.graphql.GraphqlAdminFieldFactory.mapSettingsToGraphql;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.FAILED;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.graphql.GraphqlSchemaFieldFactory.outputSettingsType;
import static org.molgenis.emx2.utils.TypeUtils.convertToPascalCase;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.ReferenceScope;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.sql.SqlDatabase;

public class GraphqlSessionFieldFactory {

  static final GraphQLObjectType outputTablePermissionsType =
      GraphQLObjectType.newObject()
          .name("MolgenisTablePermission")
          .field(GraphQLFieldDefinition.newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(ID).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(CAN_VIEW)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(CAN_AGGREGATE)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(CAN_INSERT)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(CAN_UPDATE)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(CAN_DELETE)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(CAN_REFERENCE)
                  .type(Scalars.GraphQLBoolean))
          .build();

  public GraphqlSessionFieldFactory() {
    // no instance
  }

  public GraphQLFieldDefinition signoutField(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("signout")
        .type(GraphqlApiMutationResult.typeForMutationResult)
        .dataFetcher(
            dataFetchingEnvironment -> {
              GraphqlSessionHandlerInterface sessionHandler =
                  dataFetchingEnvironment
                      .getGraphQlContext()
                      .get(GraphqlSessionHandlerInterface.class);
              sessionHandler.destroySession();
              return new GraphqlApiMutationResult(
                  GraphqlApiMutationResult.Status.SUCCESS,
                  "User '%s' has signed out",
                  database.getActiveUser());
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
                return new GraphqlApiMutationResult(FAILED, "Email already exists");
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
        .type(GraphqlApiMutationResultWithToken.typeForSignResult)
        .argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString))
        .argument(GraphQLArgument.newArgument().name(PASSWORD).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String userName = dataFetchingEnvironment.getArgument(EMAIL);
              String passWord = dataFetchingEnvironment.getArgument(PASSWORD);
              if (database.hasUser(userName) && database.checkUserPassword(userName, passWord)) {
                if (database.getUser(userName).getEnabled()) {
                  GraphqlSessionHandlerInterface sessionHandler =
                      dataFetchingEnvironment
                          .getGraphQlContext()
                          .get(GraphqlSessionHandlerInterface.class);
                  sessionHandler.createSession(userName);
                  // token can only be created as that user
                  // to make sure we don't change database user we create new instance
                  Database temp = new SqlDatabase(false);
                  temp.setActiveUser(userName);
                  return new GraphqlApiMutationResultWithToken(
                      GraphqlApiMutationResult.Status.SUCCESS,
                      JWTgenerator.createTemporaryToken(temp, userName),
                      "Signed in as '%s'",
                      userName);
                } else {
                  return new GraphqlApiMutationResult(
                      FAILED, "User '%s' disabled: check with your administrator", userName);
                }
              } else {
                return new GraphqlApiMutationResult(
                    FAILED, "Sign in as '%s' failed: user or password unknown", userName);
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
                        .name(TABLE_PERMISSIONS)
                        .type(GraphQLList.list(outputTablePermissionsType)))
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
                result.put(TABLE_PERMISSIONS, buildTablePermissions(schema));
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

  private static List<Map<String, Object>> buildTablePermissions(Schema schema) {
    List<Map<String, Object>> result =
        new ArrayList<>(
            schema.getPermissionsForActiveUser().stream()
                .map(
                    p -> {
                      SelectScope select = p.select();
                      Map<String, Object> entry = new LinkedHashMap<>();
                      entry.put(ID, convertToPascalCase(p.table()));
                      entry.put(NAME, p.table());
                      entry.put(CAN_VIEW, select != null && select.allowsRowAccess());
                      entry.put(CAN_AGGREGATE, select != null && select.allowsAggregate());
                      entry.put(CAN_INSERT, p.insert() != null && p.insert() != UpdateScope.NONE);
                      entry.put(CAN_UPDATE, p.update() != null && p.update() != UpdateScope.NONE);
                      entry.put(CAN_DELETE, p.delete() != null && p.delete() != UpdateScope.NONE);
                      entry.put(
                          CAN_REFERENCE,
                          (select != null && select.allowsRowAccess())
                              || (p.reference() != null && p.reference() != ReferenceScope.NONE));
                      return entry;
                    })
                .toList());

    Set<String> alreadyListed =
        result.stream().map(entry -> (String) entry.get(NAME)).collect(Collectors.toSet());

    for (TableMetadata tm : schema.getMetadata().getTables()) {
      if (tm.getTableType() == TableType.ONTOLOGIES && !alreadyListed.contains(tm.getTableName())) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put(ID, convertToPascalCase(tm.getTableName()));
        entry.put(NAME, tm.getTableName());
        entry.put(CAN_VIEW, true);
        entry.put(CAN_AGGREGATE, false);
        entry.put(CAN_INSERT, false);
        entry.put(CAN_UPDATE, false);
        entry.put(CAN_DELETE, false);
        entry.put(CAN_REFERENCE, true);
        result.add(entry);
      }
    }
    return result;
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
