package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlConstants.*;

import graphql.Scalars;
import graphql.schema.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.molgenis.emx2.Database;

public class GraphlAdminFieldFactory {
  // Output types
  private static GraphQLOutputType userType =
      GraphQLObjectType.newObject()
          .name("_AdminUserType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("username")
                  .type(Scalars.GraphQLString)
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
                    .argument(GraphQLArgument.newArgument().name(LIMIT).type(Scalars.GraphQLInt))
                    .argument(GraphQLArgument.newArgument().name(OFFSET).type(Scalars.GraphQLInt))
                    .type(GraphQLList.list(userType))
                    .build())
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name("userCount")
                    .type(Scalars.GraphQLInt)
                    .dataFetcher(
                        dataFetchingEnvironment -> {
                          return db.countUsers();
                        })
                    .build())
            .build();

    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_admin")
        .dataFetcher(
            dataFetchingEnvironment -> {
              Map<String, Object> result = new LinkedHashMap<>();
              int limit = 100;
              int offset = 0;
              // check for parameters
              for (SelectedField s :
                  dataFetchingEnvironment.getSelectionSet().getImmediateFields()) {
                if (s.getName().equals("users")) {
                  Map<String, Object> args = s.getArguments();
                  if (args.containsKey(LIMIT)) limit = (int) args.get(LIMIT);
                  if (args.containsKey(OFFSET)) offset = (int) args.get(OFFSET);
                }
              }
              result.put("users", db.getUsers(limit, offset));
              result.put("userCount", db.countUsers());
              return result;
            })
        .type(adminType)
        .build();
  }
}
