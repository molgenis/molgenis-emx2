package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.SETTINGS;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.graphql.GraphqlSchemaFieldFactory.outputSettingsType;

import graphql.Scalars;
import graphql.schema.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.User;

public class GraphlAdminFieldFactory {
  private GraphlAdminFieldFactory() {
    // hide constructor
  }

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
                  .type(GraphQLList.list(outputSettingsType))
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
    if (email != null) {
      return List.of(toGraphqlUser(db.getUser(email)));
    } else {
      return db.getUsers(limit, offset).stream()
          .map(GraphlAdminFieldFactory::toGraphqlUser)
          .toList();
    }
  }

  private static Map<String, Object> toGraphqlUser(User user) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put(EMAIL, user.getUsername());
    result.put(SETTINGS, mapSettingsToGraphql(user.getSettings()));
    return result;
  }

  public static Object mapSettingsToGraphql(Map<String, String> settings) {
    return settings.entrySet().stream()
        .map(entry -> Map.of(KEY, entry.getKey(), VALUE, entry.getValue()))
        .toList();
  }
}
