package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.DESCRIPTION;
import static org.molgenis.emx2.Constants.SETTINGS;
import static org.molgenis.emx2.graphql.GraphlAdminFieldFactory.mapSettingsToGraphql;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.graphql.GraphqlConstants.TASK_ID;
import static org.molgenis.emx2.graphql.GraphqlSchemaFieldFactory.*;

import graphql.Scalars;
import graphql.schema.*;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.AvailableDataModels;
import org.molgenis.emx2.tasks.TaskService;

public class GraphqlDatabaseFieldFactory {

  public static final GraphQLType outputSchemasType =
      new GraphQLObjectType.Builder()
          .name("SchemaInfo")
          .field(GraphQLFieldDefinition.newFieldDefinition().name(ID).type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(LABEL).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .build();

  public GraphqlDatabaseFieldFactory() {
    // no instances
  }

  public GraphQLFieldDefinition.Builder deleteMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("deleteSchema")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(ID).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String id = dataFetchingEnvironment.getArgument(ID);
              database.dropSchema(id); // id and name are still equal, might change in future
              return new GraphqlApiMutationResult(SUCCESS, "Schema %s dropped", id);
            });
  }

  public GraphQLFieldDefinition.Builder createMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("createSchema")
        .type(typeForMutationResult)
        .argument(
            GraphQLArgument.newArgument().name(GraphqlConstants.NAME).type(Scalars.GraphQLString))
        .argument(GraphQLArgument.newArgument().name(DESCRIPTION).type(Scalars.GraphQLString))
        .argument(
            GraphQLArgument.newArgument().name(Constants.TEMPLATE).type(Scalars.GraphQLString))
        .argument(
            GraphQLArgument.newArgument()
                .name(Constants.INCLUDE_DEMO_DATA)
                .type(Scalars.GraphQLBoolean))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String name = dataFetchingEnvironment.getArgument(NAME);
              String description = dataFetchingEnvironment.getArgument(DESCRIPTION);
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
        .argument(GraphQLArgument.newArgument().name(DESCRIPTION).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String name = dataFetchingEnvironment.getArgument(NAME);
              String description = dataFetchingEnvironment.getArgument(Constants.DESCRIPTION);
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
        .dataFetcher(dataFetchingEnvironment -> mapSettingsToGraphql(database.getSettings()));
  }

  public GraphQLFieldDefinition.Builder createSettingsMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(("createSetting"))
        .type(typeForMutationResult)
        .argument(
            GraphQLArgument.newArgument().name(Constants.SETTINGS_NAME).type(Scalars.GraphQLString))
        .argument(
            GraphQLArgument.newArgument()
                .name(Constants.SETTINGS_VALUE)
                .type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String key = dataFetchingEnvironment.getArgument(Constants.SETTINGS_NAME);
              String value = dataFetchingEnvironment.getArgument(Constants.SETTINGS_VALUE);
              database.setSetting(key, value);
              return new GraphqlApiMutationResult(SUCCESS, "Database setting %s created", key);
            });
  }

  public GraphQLFieldDefinition.Builder deleteSettingsMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name(("deleteSetting"))
        .type(typeForMutationResult)
        .argument(
            GraphQLArgument.newArgument().name(Constants.SETTINGS_NAME).type(Scalars.GraphQLString))
        .dataFetcher(
            dataFetchingEnvironment -> {
              String key = dataFetchingEnvironment.getArgument(Constants.SETTINGS_NAME);
              database.removeSetting(key);
              return new GraphqlApiMutationResult(SUCCESS, "Database setting %s deleted", key);
            });
  }

  public GraphQLFieldDefinition.Builder schemasQuery(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_schemas")
        .dataFetcher(
            dataFetchingEnvironment -> {
              List<Map<String, String>> result = new ArrayList<>();
              for (SchemaInfo schemaInfo : database.getSchemaInfos()) {
                HashMap<String, String> fields = new HashMap<>();
                fields.put("id", schemaInfo.tableSchema()); // todo if we want identifier here
                fields.put(
                    "label", schemaInfo.tableSchema()); // todo if we want something else than name
                fields.put("name", schemaInfo.tableSchema());
                if (!Objects.isNull(schemaInfo.description())) {
                  fields.put("description", schemaInfo.description());
                }
                result.add(fields);
              }
              return result;
            })
        .type(GraphQLList.list(outputSchemasType));
  }

  final GraphQLObjectType outputTaskType =
      GraphQLObjectType.newObject()
          .name("MolgenisTask")
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
                  .type(GraphQLList.list(GraphQLTypeReference.typeRef("MolgenisTask"))))
          .build();

  public GraphQLFieldDefinition tasksQueryField(TaskService taskService) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_tasks")
        .type(GraphQLList.list(outputTaskType))
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
                  .type(GraphQLList.list(inputSettingsMetadataType))
                  .build())
          .build();

  public GraphQLFieldDefinition changeMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("change")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(USERS).type(GraphQLList.list(inputUserType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(SETTINGS)
                .type(GraphQLList.list(inputSettingsMetadataType)))
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
            GraphQLArgument.newArgument()
                .name(SETTINGS)
                .type(GraphQLList.list(inputDropSettingType)))
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
        database.removeUser((String) userAsMap.get(EMAIL));
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
        hasSettings.removeSetting(setting.get(KEY));
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
