package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.SETTINGS;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.graphql.GraphqlSchemaFieldFactory.outputSettingsMetadataType;

import graphql.Scalars;
import graphql.schema.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.User;

public class GraphlAdminFieldFactory {
  // Output types
  private static GraphQLOutputType userType =
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
                  .type(GraphQLList.list(outputSettingsMetadataType))
                  .build())
          .build();

  // retrieve user list, user count
  public static GraphQLFieldDefinition queryAdminField(Database db) {
    GraphQLOutputType adminType =
        GraphQLObjectType.newObject()
            .name("_AdminType")
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name("users")
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
              int limit = 100;
              int offset = 0;
              String email = null;
              // check for parameters
              for (SelectedField s :
                  dataFetchingEnvironment.getSelectionSet().getImmediateFields()) {
                if (s.getName().equals("users")) {
                  Map<String, Object> args = s.getArguments();
                  if (args.containsKey(LIMIT)) limit = (int) args.get(LIMIT);
                  if (args.containsKey(OFFSET)) offset = (int) args.get(OFFSET);
                  if (args.containsKey(EMAIL)) email = (String) args.get(EMAIL);
                }
                if (email != null) {
                  result.put("users", List.of(toGraphqlUser(db.getUser(email))));
                } else {
                  result.put(
                      "users",
                      db.getUsers(limit, offset).stream()
                          .map(user -> toGraphqlUser(user))
                          .collect(Collectors.toList()));
                }
                if (s.getName().equals("users")) {
                  result.put("userCount", db.countUsers());
                }
              }
              return result;
            })
        .type(adminType)
        .build();
  }

  private static Map<String, Object> toGraphqlUser(User user) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put(EMAIL, user.getUsername());
    result.put(SETTINGS, mapToSettings(user.getSettings()));
    return result;
  }

  public static Object mapToSettings(Map<String, String> settings) {
    return settings.entrySet().stream()
        .map(entry -> Map.of(KEY, entry.getKey(), VALUE, entry.getValue()))
        .collect(Collectors.toList());
  }
}
