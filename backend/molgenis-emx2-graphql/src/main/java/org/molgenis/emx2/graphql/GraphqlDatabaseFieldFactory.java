package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.*;
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
import java.util.stream.Collectors;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.AvailableDataModels;
import org.molgenis.emx2.tasks.TaskService;

public class GraphqlDatabaseFieldFactory {

  public GraphqlDatabaseFieldFactory() {
    // no instances
  }

  static final GraphQLInputObjectType inputSchemaType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisSchemaInput")
          .field(
              GraphQLInputObjectField.newInputObjectField().name(NAME).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(Constants.IS_CHANGELOG_ENABLED)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(Constants.TEMPLATE)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(Constants.INCLUDE_DEMO_DATA)
                  .type(Scalars.GraphQLBoolean))
          .build();

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
        .name("create")
        .type(typeForMutationResult)
        .argument(
            GraphQLArgument.newArgument().name(SCHEMAS).type(GraphQLList.list(inputSchemaType)))
        // todo also include 'settings' into this 'create' mutation
        .dataFetcher(
            dataFetchingEnvironment -> {
              List<SchemaMetadata> schemaList = dataFetchingEnvironment.getArgument(SCHEMAS);
              database.tx(
                  db -> {
                    for (SchemaMetadata schemaMetadata : schemaList) {
                      Schema schema = db.create(schemaMetadata);
                      if (schemaMetadata.getTemplate() != null) {
                        AvailableDataModels.valueOf(schemaMetadata.getTemplate())
                            .install(
                                schema, Boolean.TRUE.equals(schemaMetadata.isIncludeDemoData()));
                      }
                    }
                  });
              return new GraphqlApiMutationResult(
                  SUCCESS,
                  "Schema(s) %s created",
                  schemaList.stream()
                      .map(schema -> schema.getName())
                      .collect(Collectors.joining(",")));
            });
  }

  private SchemaMetadata getSchema(Map<String, Object> schemaInfo) {
    Row row = new Row(schemaInfo);
    return new SchemaMetadata()
        .setName(row.getString(NAME))
        .setDescription(row.getString(DESCRIPTION))
        .setIsChangeLogEnabled(row.getBoolean(IS_CHANGELOG_ENABLED));
  }

  public GraphQLFieldDefinition.Builder updateMutation(Database database) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("update")
        .type(typeForMutationResult)
        .argument(GraphQLArgument.newArgument().name(SCHEMAS).type(inputSchemaType))
        .dataFetcher(
            dataFetchingEnvironment -> {
              // todo
              String name = dataFetchingEnvironment.getArgument(NAME);
              String description = dataFetchingEnvironment.getArgument(Constants.DESCRIPTION);
              boolean isChangeLogEnabled =
                  dataFetchingEnvironment.getArgument(Constants.IS_CHANGELOG_ENABLED);
              database.updateSchema(name, description, isChangeLogEnabled);
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
                fields.put("isChangelogEnabled", String.valueOf(schemaInfo.isChangelogEnabled()));
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
                    .field(
                        GraphQLFieldDefinition.newFieldDefinition()
                            .name(Constants.IS_CHANGELOG_ENABLED)
                            .type(Scalars.GraphQLBoolean)
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
