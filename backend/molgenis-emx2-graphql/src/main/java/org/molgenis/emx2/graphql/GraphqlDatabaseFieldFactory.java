package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.graphql.GraphqlConstants.TASK_ID;
import static org.molgenis.emx2.graphql.GraphqlSchemaFieldFactory.outputSettingsMetadataType;

import graphql.Scalars;
import graphql.schema.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.AvailableLoadersEnum;
import org.molgenis.emx2.tasks.TaskService;

public class GraphqlDatabaseFieldFactory {

  public GraphqlDatabaseFieldFactory() {
    // no instances
  }

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
                      AvailableLoadersEnum.valueOf(template)
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
        .type(GraphQLList.list(outputSettingsMetadataType))
        .dataFetcher(dataFetchingEnvironment -> database.getSettings());
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
              database.createSetting(key, value);
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
              database.deleteSetting(key);
              return new GraphqlApiMutationResult(SUCCESS, "Database setting %s deleted", key);
            });
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
}
