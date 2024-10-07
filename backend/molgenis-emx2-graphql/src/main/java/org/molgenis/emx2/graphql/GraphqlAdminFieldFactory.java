package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.Constants.KEY;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.graphql.GraphqlSchemaFieldFactory.*;

import graphql.Scalars;
import graphql.schema.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;

public class GraphqlAdminFieldFactory {
  private GraphqlAdminFieldFactory() {
    // hide constructor
  }

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
                  .name(SETTINGS)
                  .type(GraphQLList.list(outputSettingsType))
                  .build())
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(ROLES)
                  .type(GraphQLList.list(outputUserRolesType))
                  .build())
          .build();

  // retrieve user list, user count
  public static GraphQLFieldDefinition queryAdminField(Database db) {
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
                    .name("userCount")
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
                if (selectedField.getName().equals("userCount")) {
                  result.put("userCount", db.countUsers());
                }
              }
              return result;
            })
        .type(adminType)
        .build();
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

  private static Map<String, Object> toGraphqlUser(User user, List<Member> members) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put(EMAIL, user.getUsername());
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
}
