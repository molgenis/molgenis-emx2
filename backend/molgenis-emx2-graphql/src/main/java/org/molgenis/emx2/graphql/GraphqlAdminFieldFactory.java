package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.Constants.KEY;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.FAILED;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.graphql.GraphqlSchemaFieldFactory.*;

import graphql.Scalars;
import graphql.schema.*;
import java.util.*;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.*;

public class GraphqlAdminFieldFactory {
  private GraphqlAdminFieldFactory() {
    // hide constructor
  }

  private static final String UPDATE_USER = "updateUser";

  // Output types
  private static final GraphQLOutputType userType =
      GraphQLObjectType.newObject()
          .name("_AdminUserType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(EMAIL)
                  .type(Scalars.GraphQLString)
                  .build())
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(ENABLED)
                  .type(Scalars.GraphQLBoolean)
                  .build())
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(ADMIN)
                  .type(Scalars.GraphQLBoolean)
                  .build())
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SETTINGS)
                  .type(GraphQLList.list(outputSettingsType))
                  .build())
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(ROLES)
                  .type(GraphQLList.list(userRolesType))
                  .build())
          .build();

  private static final GraphQLOutputType schemaRoleType =
      GraphQLObjectType.newObject()
          .name("_AdminSchemaRoleType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SCHEMA_ID)
                  .type(Scalars.GraphQLString)
                  .build())
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(ROLE_NAME)
                  .type(Scalars.GraphQLString)
                  .build())
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(PERMISSIONS)
                  .type(GraphQLList.list(outputPermissionType))
                  .build())
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(USERS)
                  .type(GraphQLList.list(Scalars.GraphQLString))
                  .build())
          .build();

  // retrieve user list, user count
  public static GraphQLFieldDefinition queryAdminField(Database db) {
    String userCount = "userCount";
    GraphQLOutputType adminType =
        GraphQLObjectType.newObject()
            .name("MolgenisAdmin")
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name(USERS)
                    .argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString))
                    .argument(GraphQLArgument.newArgument().name(LIMIT).type(Scalars.GraphQLInt))
                    .argument(GraphQLArgument.newArgument().name(OFFSET).type(Scalars.GraphQLInt))
                    .type(GraphQLList.list(userType))
                    .build())
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name(userCount)
                    .type(Scalars.GraphQLInt)
                    .build())
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name(SCHEMA_ROLES)
                    .argument(GraphQLArgument.newArgument().name(LIMIT).type(Scalars.GraphQLInt))
                    .argument(GraphQLArgument.newArgument().name(OFFSET).type(Scalars.GraphQLInt))
                    .argument(
                        GraphQLArgument.newArgument().name(SEARCH).type(Scalars.GraphQLString))
                    .type(GraphQLList.list(schemaRoleType))
                    .build())
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name(SCHEMA_ROLE_COUNT)
                    .argument(
                        GraphQLArgument.newArgument().name(SEARCH).type(Scalars.GraphQLString))
                    .type(Scalars.GraphQLInt)
                    .build())
            .build();

    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_admin")
        .dataFetcher(
            dataFetchingEnvironment -> {
              Map<String, Object> result = new LinkedHashMap<>();
              // check for parameters
              for (SelectedField selectedField :
                  dataFetchingEnvironment.getSelectionSet().getImmediateFields()) {
                if (selectedField.getName().equals(USERS)) {
                  result.put(USERS, getUsers(selectedField, db));
                }
                if (selectedField.getName().equals(userCount)) {
                  result.put(userCount, db.countUsers());
                }
                if (selectedField.getName().equals(SCHEMA_ROLES)) {
                  result.put(SCHEMA_ROLES, getSchemaRoles(selectedField, db));
                }
                if (selectedField.getName().equals(SCHEMA_ROLE_COUNT)) {
                  result.put(
                      SCHEMA_ROLE_COUNT,
                      (int) schemaRoles(db, searchArgument(selectedField)).count());
                }
              }
              return result;
            })
        .type(adminType)
        .build();
  }

  private static List<Map<String, Object>> getSchemaRoles(
      SelectedField selectedField, Database db) {
    Map<String, Object> args = selectedField.getArguments();
    int limit = args.containsKey(LIMIT) ? (int) args.get(LIMIT) : 100;
    int offset = args.containsKey(OFFSET) ? (int) args.get(OFFSET) : 0;
    return schemaRoles(db, searchArgument(selectedField))
        .skip(offset)
        .limit(limit)
        .map(SchemaRole::toGraphql)
        .toList();
  }

  private static String searchArgument(SelectedField selectedField) {
    return (String) selectedField.getArguments().get(SEARCH);
  }

  private static Stream<SchemaRole> schemaRoles(Database db, String search) {
    Map<String, List<String>> usersPerRole = getUsersPerRole(db.loadUserRoles());
    return db.getSchemaNames().stream()
        .flatMap(schemaName -> schemaRolesOf(db, schemaName, usersPerRole))
        .filter(schemaRole -> schemaRole.matches(search));
  }

  private static Stream<SchemaRole> schemaRolesOf(
      Database db, String schemaName, Map<String, List<String>> usersPerRole) {
    return db.getSchema(schemaName).getRoleInfos().stream()
        .filter(role -> !role.isSystemRole() && !role.permissions().isEmpty())
        .map(
            role ->
                new SchemaRole(
                    schemaName,
                    role,
                    usersPerRole.getOrDefault(schemaName + "/" + role.name(), List.of())));
  }

  private record SchemaRole(String schemaId, Role role, List<String> users) {
    private boolean matches(String search) {
      if (search == null || search.isBlank()) {
        return true;
      }
      String query = search.toLowerCase();
      return contains(schemaId, query)
          || contains(role.name(), query)
          || users.stream().anyMatch(user -> contains(user, query))
          || role.permissions().stream()
              .anyMatch(permission -> contains(permission.table(), query));
    }

    private static boolean contains(String value, String query) {
      return value != null && value.toLowerCase().contains(query);
    }

    private Map<String, Object> toGraphql() {
      Map<String, Object> schemaRole = new LinkedHashMap<>();
      schemaRole.put(SCHEMA_ID, schemaId);
      schemaRole.put(ROLE_NAME, role.name());
      schemaRole.put(PERMISSIONS, permissionsToList(role));
      schemaRole.put(USERS, users);
      return schemaRole;
    }
  }

  private static Map<String, List<String>> getUsersPerRole(List<Member> members) {
    Map<String, List<String>> usersPerRole = new LinkedHashMap<>();
    for (Member member : members) {
      usersPerRole
          .computeIfAbsent(member.getRole(), role -> new ArrayList<>())
          .add(member.getUser());
    }
    return usersPerRole;
  }

  private static Object getUsers(SelectedField selectedField, Database db) {
    Map<String, Object> args = selectedField.getArguments();
    int limit = args.containsKey(LIMIT) ? (int) args.get(LIMIT) : 100;
    int offset = args.containsKey(OFFSET) ? (int) args.get(OFFSET) : 0;
    String email = args.containsKey(EMAIL) ? (String) args.get(EMAIL) : null;
    List<Member> members = db.loadUserRoles();

    if (email != null) {
      return List.of(toGraphqlUser(db.getUser(email), members));
    } else {
      return db.getUsers(limit, offset).stream().map(user -> toGraphqlUser(user, members)).toList();
    }
  }

  public static GraphQLFieldDefinition removeUser(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("removeUser")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String email = dataFetchingEnvironment.getArgument(EMAIL);
              database.removeUser(email);
              return new GraphqlApiMutationResult(SUCCESS, "User %s removed", email);
            })
        .build();
  }

  public static GraphQLFieldDefinition setEnabledUser(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("setEnabledUser")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(EMAIL).type(Scalars.GraphQLString))
        .argument(GraphQLArgument.newArgument().name(ENABLED).type(Scalars.GraphQLBoolean))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String email = dataFetchingEnvironment.getArgument(EMAIL);
              boolean enabled = dataFetchingEnvironment.getArgument(ENABLED);
              database.setEnabledUser(email, enabled);
              return new GraphqlApiMutationResult(
                  SUCCESS, "User %s %s ", email, enabled ? "Enabled" : "Disabled");
            })
        .build();
  }

  private static Map<String, Object> toGraphqlUser(User user, List<Member> members) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put(EMAIL, user.getUsername());
    result.put(ENABLED, user.getEnabled());
    result.put(ADMIN, user.isAdmin());
    result.put(SETTINGS, mapSettingsToGraphql(user.getSettings()));

    List<Map<String, String>> roles = getRoles(user, members);
    result.put(ROLES, roles);
    return result;
  }

  private static List<Map<String, String>> getRoles(User user, List<Member> members) {
    return members.stream()
        .filter(member -> member.getUser().equals(user.getUsername()))
        .map(GraphqlAdminFieldFactory::getUserRoleMap)
        .toList();
  }

  private static Map<String, String> getUserRoleMap(Member member) {
    String role = member.getRole();
    String[] parts = role.split("/");
    Map<String, String> roleMap = new HashMap<>();
    roleMap.put(SCHEMA_ID, parts[0]);
    roleMap.put(ROLE, parts[1]);
    return roleMap;
  }

  public static Object mapSettingsToGraphql(Map<String, String> settings) {
    return settings.entrySet().stream()
        .filter(entry -> !MOLGENIS_JWT_SHARED_SECRET.equals(entry.getKey()))
        .map(entry -> Map.of(KEY, entry.getKey(), VALUE, entry.getValue()))
        .toList();
  }

  public static GraphQLFieldDefinition updateUser(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(UPDATE_USER)
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(UPDATE_USER).type(updateUserType))
        .dataFetcher(
            dataFetchingEnvironment -> executeUpdateUser(database, dataFetchingEnvironment))
        .build();
  }

  @NotNull
  private static GraphqlApiMutationResult executeUpdateUser(
      Database database, DataFetchingEnvironment dataFetchingEnvironment) {
    LinkedHashMap<String, Object> updatedUser = dataFetchingEnvironment.getArgument(UPDATE_USER);
    String userName = (String) updatedUser.get(EMAIL);
    if (userName == null) {
      return new GraphqlApiMutationResult(FAILED, "No user provided");
    }
    database.tx(
        db -> {
          String password = (String) updatedUser.get(PASSWORD);
          if (password != null) {
            db.setUserPassword(userName, password);
          }

          List<Map<String, String>> roles = (List<Map<String, String>>) updatedUser.get(ROLES);
          if (roles != null && roles.iterator().hasNext()) {
            db.updateRoles(userName, roles);
          }

          List<Map<String, String>> revokedRoles =
              (List<Map<String, String>>) updatedUser.get("revokedRoles");
          if (revokedRoles != null && revokedRoles.iterator().hasNext()) {
            db.revokeRoles(userName, revokedRoles);
          }

          Boolean enabled = (Boolean) updatedUser.get(ENABLED);
          if (enabled != null) {
            db.setEnabledUser(userName, enabled);
          }

          Boolean admin = (Boolean) updatedUser.get(ADMIN);
          if (admin != null) {
            db.setAdminUser(userName, admin);
          }
        });
    return new GraphqlApiMutationResult(SUCCESS, "User %s updated", userName);
  }

  private static final GraphQLInputObjectType inputUserRolesType =
      new GraphQLInputObjectType.Builder()
          .name("InputUserRolesType")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(SCHEMA_ID)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(ROLE).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(USER).type(Scalars.GraphQLString))
          .build();

  private static final GraphQLInputObjectType updateUserType =
      new GraphQLInputObjectType.Builder()
          .name("InputUpdateUser")
          .field(
              GraphQLInputObjectField.newInputObjectField().name(EMAIL).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(PASSWORD)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(ENABLED)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(ADMIN)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(ROLES)
                  .type(GraphQLList.list(inputUserRolesType)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name("revokedRoles")
                  .type(GraphQLList.list(inputUserRolesType)))
          .build();
}
