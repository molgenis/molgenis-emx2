package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.SETTINGS;
import static org.molgenis.emx2.graphql.GraphqlAdminFieldFactory.mapToSettings;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.graphql.GraphqlConstants.TASK_ID;
import static org.molgenis.emx2.graphql.GraphqlSchemaFieldFactory.inputSettingsType;
import static org.molgenis.emx2.graphql.GraphqlSchemaFieldFactory.outputSettingsType;

import graphql.Scalars;
import graphql.schema.*;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.AvailableDataModels;
import org.molgenis.emx2.tasks.TaskService;

public class GraphqlDatabaseFieldFactory {

  public GraphqlDatabaseFieldFactory() {
    // no instances
  }

  // todo move to drop() mutation
  public GraphQLFieldDefinition.Builder deleteMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("deleteSchema")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(NAME).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String name = dataFetchingEnvironment.getArgument("name");
              database.dropSchema(name);
              return new GraphqlApiMutationResult(SUCCESS, "Schema %s dropped", name);
            });
  }

  // todo move to create() mutation
  public GraphQLFieldDefinition.Builder createMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("createSchema")
        .type(typeForMutationResult)
        .argument(
            GraphQLArgument.newArgument().name(GraphqlConstants.NAME).type(Scalars.GraphQLString))
        .argument(
            GraphQLArgument.newArgument().name(Constants.DESCRIPTION).type(Scalars.GraphQLString))
        .argument(
            GraphQLArgument.newArgument().name(Constants.TEMPLATE).type(Scalars.GraphQLString))
        .argument(
            GraphQLArgument.newArgument()
                .name(Constants.INCLUDE_DEMO_DATA)
                .type(Scalars.GraphQLBoolean))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String name = dataFetchingEnvironment.getArgument(NAME);
              String description = dataFetchingEnvironment.getArgument(Constants.DESCRIPTION);
              String template = dataFetchingEnvironment.getArgument(Constants.TEMPLATE);
              Boolean includeDemoData =
                  dataFetchingEnvironment.getArgument(Constants.INCLUDE_DEMO_DATA);

              database.tx(
                  db -> {
                    Schema schema = db.createSchema(name, description);
                    if (template != null) {
                      AvailableDataModels.valueOf(template)
                          .install(schema, Boolean.TRUE.equals(includeDemoData));
                    }
                  });
              return new GraphqlApiMutationResult(SUCCESS, "Schema %s created", name);
            });
  }

  public GraphQLFieldDefinition.Builder updateMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("updateSchema")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(NAME).type(Scalars.GraphQLString))
        .argument(
            GraphQLArgument.newArgument().name(Constants.DESCRIPTION).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String name = dataFetchingEnvironment.getArgument("name");
              String description = dataFetchingEnvironment.getArgument("description");
              database.updateSchema(name, description);
              return new GraphqlApiMutationResult(SUCCESS, "Schema %s updated", name);
            });
  }

  public GraphQLFieldDefinition.Builder settingsQueryField(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_settings")
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.KEYS)
                .type(GraphQLList.list(Scalars.GraphQLString)))
        .type(GraphQLList.list(outputSettingsType))
        .dataFetcher(dataFetchingEnvironment -> mapToSettings(database.getSettings()));
  }

  public GraphQLFieldDefinition.Builder schemasQuery(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("Schemas")
        .dataFetcher(
            dataFetchingEnvironment -> {
              List<Map<String, String>> result = new ArrayList<>();
              for (SchemaInfo schemaInfo : database.getSchemaInfos()) {
                HashMap<String, String> fields = new HashMap<>();
                fields.put("name", schemaInfo.tableSchema());
                if (!Objects.isNull(schemaInfo.description())) {
                  fields.put("description", schemaInfo.description());
                }
                result.add(fields);
              }
              return result;
            })
        .type(
            GraphQLList.list(
                GraphQLObjectType.newObject()
                    .name("Schema")
                    .field(
                        GraphQLFieldDefinition.newFieldDefinition()
                            .name(NAME)
                            .type(Scalars.GraphQLString)
                            .build())
                    .field(
                        GraphQLFieldDefinition.newFieldDefinition()
                            .name(Constants.DESCRIPTION)
                            .type(Scalars.GraphQLString)
                            .build())
                    .build()));
  }

  final GraphQLObjectType taskObject =
      GraphQLObjectType.newObject()
          .name("MolgenisTasks")
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(TASK_ID).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(TASK_STATUS)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(TASK_DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(TASK_SUBTASKS)
                  .type(GraphQLList.list(GraphQLTypeReference.typeRef("MolgenisTasks"))))
          .build();

  public GraphQLFieldDefinition tasksQueryField(TaskService taskService) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_tasks")
        .type(GraphQLList.list(taskObject))
        .argument(GraphQLArgument.newArgument().name(TASK_ID).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String id = dataFetchingEnvironment.getArgument(TASK_ID);
              if (id != null) {
                return List.of(taskService.getTask(id));
              }
              return taskService.listTasks();
            })
        .build();
  }

  public static final GraphQLInputObjectType inputUserType =
      GraphQLInputObjectType.newInputObject()
          .name("UsersInput")
          .field(
              GraphQLInputObjectField.newInputObjectField().name(EMAIL).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(SETTINGS)
                  .type(GraphQLList.list(inputSettingsType))
                  .build())
          .build();

  public GraphQLFieldDefinition changeMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("change")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(USERS).type(GraphQLList.list(inputUserType)))
        .argument(
            GraphQLArgument.newArgument().name(SETTINGS).type(GraphQLList.list(inputSettingsType)))
        .dataFetcher(
            dataFetchingEnvironment -> {
              StringBuilder messageBuilder = new StringBuilder();
              database.tx(
                  db -> {
                    try {
                      changeUsers(db, dataFetchingEnvironment.getArgument(USERS), messageBuilder);
                      changeSettings(
                          db, dataFetchingEnvironment.getArgument(SETTINGS), messageBuilder);
                    } catch (Exception e) {
                      throw new GraphqlException("change failed", e);
                    }
                  });
              return new GraphqlApiMutationResult(SUCCESS, messageBuilder.toString().trim());
            })
        .build();
  }

  public GraphQLFieldDefinition dropMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("drop")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(USERS).type(GraphQLList.list(inputUserType)))
        .argument(
            GraphQLArgument.newArgument().name(SETTINGS).type(GraphQLList.list(inputSettingsType)))
        .dataFetcher(
            dataFetchingEnvironment -> {
              StringBuilder messageBuilder = new StringBuilder();
              database.tx(
                  db -> {
                    try {
                      dropUsers(db, dataFetchingEnvironment.getArgument(USERS), messageBuilder);
                      dropSettings(
                          db, dataFetchingEnvironment.getArgument(SETTINGS), messageBuilder);
                    } catch (Exception e) {
                      throw new GraphqlException("change failed", e);
                    }
                  });
              return new GraphqlApiMutationResult(SUCCESS, messageBuilder.toString().trim());
            })
        .build();
  }

  private static void dropUsers(
      Database database, List<Map<String, String>> userList, StringBuilder messageBuilder) {
    if (userList != null) {
      for (Map<String, ?> userAsMap : userList) {
        database.dropUsers((String) userAsMap.get(EMAIL));
        messageBuilder.append("Dropped user '" + userAsMap.get(EMAIL) + "'. ");
      }
    }
  }

  private static void dropSettings(
      HasSettingsInterface<?> hasSettings,
      List<Map<String, String>> settingsRaw,
      StringBuilder messageBuilder) {
    if (settingsRaw != null) {
      for (Map<String, String> setting : settingsRaw) {
        hasSettings.dropSetting(setting.get(KEY));
        messageBuilder.append("Dropped setting '" + setting.get(KEY) + "'. ");
      }
    }
  }

  private static void changeUsers(
      Database database, List<Map<String, String>> userList, StringBuilder messageBuilder) {
    if (userList != null) {
      for (Map<String, ?> userAsMap : userList) {
        User user = new User(database, (String) userAsMap.get(EMAIL));
        changeSettings(user, (List<Map<String, String>>) userAsMap.get(SETTINGS), null);
        database.saveUser(user);
        messageBuilder.append("Changed user '" + userAsMap.get(EMAIL) + "'. ");
      }
    }
  }

  private static void changeSettings(
      HasSettingsInterface<?> hasSettings,
      List<Map<String, String>> settingsRaw,
      StringBuilder messageBuilder) {
    if (settingsRaw != null) {
      Map<String, String> settings = new LinkedHashMap<>();
      for (Map<String, String> setting : settingsRaw) {
        settings.put(setting.get(KEY), setting.get(VALUE));
        if (messageBuilder != null) {
          messageBuilder.append("Changed setting '" + setting.get(KEY) + "'. ");
        }
      }
      hasSettings.changeSettings(settings);
    }
  }
}
