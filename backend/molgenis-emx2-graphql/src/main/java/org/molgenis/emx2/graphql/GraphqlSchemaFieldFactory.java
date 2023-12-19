package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.graphql.GraphlAdminFieldFactory.mapSettingsToGraphql;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.graphql.GraphqlConstants.INHERITED;
import static org.molgenis.emx2.graphql.GraphqlConstants.KEY;
import static org.molgenis.emx2.json.JsonUtil.jsonToSchema;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.Scalars;
import graphql.schema.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlSchemaMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphqlSchemaFieldFactory {
  private static Logger logger = LoggerFactory.getLogger(SqlDatabase.class);

  public static final GraphQLInputObjectType inputSettingsMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisSettingsInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(TABLE_ID)
                  .type(Scalars.GraphQLString)
                  .description("Optional, if a setting should be applied on level of tableId"))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(KEY).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(VALUE).type(Scalars.GraphQLString))
          .build();
  public static final GraphQLInputObjectType inputLanguageValueType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisLanguageValueInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(LOCALE)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(VALUE).type(Scalars.GraphQLString))
          .build();
  public static final GraphQLType outputSettingsType =
      new GraphQLObjectType.Builder()
          .name("MolgenisSettingsType")
          .field(GraphQLFieldDefinition.newFieldDefinition().name(KEY).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.VALUE)
                  .type(Scalars.GraphQLString))
          .build();
  public static final GraphQLType outputLanguageValueType =
      new GraphQLObjectType.Builder()
          .name("MolgenisLanguageValueType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(LOCALE).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.VALUE)
                  .type(Scalars.GraphQLString))
          .build();
  static final GraphQLType changesMetadataType =
      new GraphQLObjectType.Builder()
          .name("ChangesType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(OPERATION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(STAMP).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(USERID).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(TABLENAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(OLD_ROW_DATA)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(NEW_ROW_DATA)
                  .type(Scalars.GraphQLString))
          .build();

  private static final GraphQLInputObjectType inputDropColumnType =
      new GraphQLInputObjectType.Builder()
          .name("DropColumnInput")
          .field(
              GraphQLInputObjectField.newInputObjectField().name(TABLE).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(COLUMN)
                  .type(Scalars.GraphQLString))
          .build();
  static final GraphQLInputObjectType inputDropSettingType =
      new GraphQLInputObjectType.Builder()
          .name("DropSettingsInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(TABLE_ID)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(KEY).type(Scalars.GraphQLString))
          .build();
  private static final GraphQLType outputRolesType =
      new GraphQLObjectType.Builder()
          .name("MolgenisRolesType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.NAME)
                  .type(Scalars.GraphQLString))
          .build();
  private static final GraphQLType outputMembersMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisMembersType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(EMAIL).type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(ROLE).type(Scalars.GraphQLString))
          .build();
  private static final GraphQLObjectType outputColumnMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisColumnType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(TABLE).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.LABEL)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(LABELS)
                  .type(GraphQLList.list(outputLanguageValueType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.ID)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(DESCRIPTIONS)
                  .type(GraphQLList.list(outputLanguageValueType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(COLUMN_POSITION)
                  .type(Scalars.GraphQLInt))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(COLUMN_TYPE)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(INHERITED)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(Constants.KEY)
                  .type(Scalars.GraphQLInt))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REQUIRED)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(DEFAULT_VALUE)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REF_SCHEMA_ID)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REF_SCHEMA_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REF_TABLE_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REF_TABLE_ID)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REF_LINK_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REF_LINK_ID)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REF_BACK_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REF_BACK_ID)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REF_LABEL)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REF_LABEL_DEFAULT)
                  .type(Scalars.GraphQLString))
          // TODO
          //          .field(
          //              GraphQLFieldDefinition.newFieldDefinition()
          //                  .name(CASCADE_DELETE)
          //                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(VALIDATION_EXPRESSION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(VISIBLE_EXPRESSION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(READONLY)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(COMPUTED)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SEMANTICS)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .build();
  private static final GraphQLObjectType outputTableType =
      new GraphQLObjectType.Builder()
          .name("MolgenisTableType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.LABEL)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(LABELS)
                  .type(GraphQLList.list(outputLanguageValueType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.ID)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.SCHEMA_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.SCHEMA_ID)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.INHERIT_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.INHERIT_ID)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(DESCRIPTIONS)
                  .type(GraphQLList.list(outputLanguageValueType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.COLUMNS)
                  .type(GraphQLList.list(outputColumnMetadataType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SETTINGS)
                  .type(GraphQLList.list(outputSettingsType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SEMANTICS)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(TABLE_TYPE)
                  .type(Scalars.GraphQLString))
          .build();
  private static final GraphQLObjectType outputMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisSchema")
          .field(GraphQLFieldDefinition.newFieldDefinition().name(ID).type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(LABEL).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(TABLES)
                  .type(GraphQLList.list(outputTableType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(MEMBERS)
                  .type(GraphQLList.list(outputMembersMetadataType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SETTINGS)
                  .type(GraphQLList.list(outputSettingsType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(ROLES)
                  .type(GraphQLList.list(outputRolesType)))
          .build();
  private final GraphQLInputObjectType inputMembersMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisMembersInput")
          .field(
              GraphQLInputObjectField.newInputObjectField().name(EMAIL).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(ROLE).type(Scalars.GraphQLString))
          .build();
  private GraphQLInputObjectType inputColumnMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisColumnInput")
          .field(
              GraphQLInputObjectField.newInputObjectField().name(TABLE).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(GraphqlConstants.NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(LABELS)
                  .type(GraphQLList.list(inputLanguageValueType)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(COLUMN_TYPE)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(COLUMN_POSITION)
                  .type(Scalars.GraphQLInt))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(Constants.KEY)
                  .type(Scalars.GraphQLInt))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(REQUIRED)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(DEFAULT_VALUE)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(REF_SCHEMA_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(REF_TABLE_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(REF_LINK_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(REF_BACK_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(REF_LABEL)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(OLD_NAME)
                  .type(Scalars.GraphQLString))
          // TODO
          //          .field(
          //              GraphQLInputObjectField.newInputObjectField()
          //                  .name(CASCADE_DELETE)
          //                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(DESCRIPTIONS)
                  .type(GraphQLList.list(inputLanguageValueType)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(VALIDATION_EXPRESSION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(VISIBLE_EXPRESSION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(READONLY)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(COMPUTED)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(SEMANTICS)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(DROP).type(Scalars.GraphQLBoolean))
          .build();
  private final GraphQLInputObjectType inputTableMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisTableInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(GraphqlConstants.NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(LABELS)
                  .type(GraphQLList.list(inputLanguageValueType)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(OLD_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(DROP).type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(INHERIT_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(DESCRIPTIONS)
                  .type(GraphQLList.list(inputLanguageValueType)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(SEMANTICS)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(GraphqlConstants.COLUMNS)
                  .type(GraphQLList.list(inputColumnMetadataType)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(SETTINGS)
                  .type(GraphQLList.list(inputSettingsMetadataType)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(TABLE_TYPE)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(SCHEMA_NAME)
                  .type(Scalars.GraphQLString))
          .build();

  public GraphqlSchemaFieldFactory() {
    // hide constructor
  }

  private static DataFetcher<?> queryFetcher(Schema schema) {
    return dataFetchingEnvironment -> {

      // add tables
      String json = JsonUtil.schemaToJson(schema.getMetadata(), false);
      Map<String, Object> result = new ObjectMapper().readValue(json, Map.class);

      // add members
      List<Map<String, String>> members = new ArrayList<>();
      for (Member m : schema.getMembers()) {
        members.add(Map.of("email", m.getUser(), "role", m.getRole()));
      }
      result.put(MEMBERS, members);

      // add roles
      List<Map<String, String>> roles = new ArrayList<>();
      for (String role : schema.getRoles()) {
        roles.add(Map.of(GraphqlConstants.NAME, role));
      }
      result.put(ROLES, roles);

      // add settings for the schema
      result.put(SETTINGS, mapSettingsToGraphql((schema.getMetadata().getSettings())));

      // add name
      result.put(
          ID, schema.getMetadata().getName()); // todo, think if we want to switch to identifier
      result.put(NAME, schema.getMetadata().getName());
      result.put(LABEL, schema.getMetadata().getName());
      return result;
    };
  }

  private static DataFetcher<?> dropFetcher(Schema schema) {
    return dataFetchingEnvironment -> {
      StringBuilder message = new StringBuilder();
      schema
          .getDatabase()
          .tx(
              db -> {
                Schema s = db.getSchema(schema.getName());
                dropTables(s, dataFetchingEnvironment, message);
                dropMembers(s, dataFetchingEnvironment, message);
                dropColumns(s, dataFetchingEnvironment, message);
                dropSettings(s, dataFetchingEnvironment, message);
                // this sync is a bit sad.
                ((SqlSchemaMetadata) schema.getMetadata())
                    .sync((SqlSchemaMetadata) s.getMetadata());
              });
      Map<String, String> result = new LinkedHashMap<>();
      result.put(GraphqlConstants.DETAIL, message.toString());
      return result;
    };
  }

  private static DataFetcher<?> truncateFetcher(Schema schema) {
    return dataFetchingEnvironment -> {
      StringBuilder message = new StringBuilder();
      schema
          .getDatabase()
          .tx(
              db -> {
                Schema s = db.getSchema(schema.getName());
                List<String> tables = dataFetchingEnvironment.getArgument(GraphqlConstants.TABLES);
                if (tables != null) {
                  for (String tableName : tables) {
                    Table table = s.getTable(tableName);
                    if (table == null) {
                      table = s.getTableById(tableName);
                      if (table == null) {
                        throw new GraphqlException(
                            "Truncate failed: table " + tableName + " unknown");
                      }
                    }
                    table.truncate();
                    message.append("Truncated table '" + tableName + "'\n");
                  }
                }
              });
      return new GraphqlApiMutationResult(SUCCESS, message.toString());
    };
  }

  private static void dropColumns(
      Schema schema, DataFetchingEnvironment dataFetchingEnvironment, StringBuilder message) {
    List<Map<String, ?>> columns = dataFetchingEnvironment.getArgument(GraphqlConstants.COLUMNS);
    if (columns != null) {
      for (Map<String, ?> col : columns) {
        schema
            .getMetadata()
            .getTableMetadata((String) col.get(TABLE))
            .dropColumn((String) col.get(COLUMN));
        message.append("Dropped column '" + col.get(TABLE) + "." + col.get(COLUMN) + "'\n");
      }
    }
  }

  private static void dropMembers(
      Schema schema, DataFetchingEnvironment dataFetchingEnvironment, StringBuilder message) {
    List<String> members = dataFetchingEnvironment.getArgument(GraphqlConstants.MEMBERS);
    if (members != null) {
      for (String name : members) {
        schema.removeMember(name);
        message.append("Dropped member '" + name + "'\n");
      }
    }
  }

  private static void dropSettings(
      Schema schema, DataFetchingEnvironment dataFetchingEnvironment, StringBuilder message) {
    List<Map<String, String>> settings = dataFetchingEnvironment.getArgument(SETTINGS);
    if (settings != null) {
      for (Map<String, String> setting : settings) {
        if (setting.get(TABLE_ID) != null) {
          Table table = schema.getTableById(setting.get(TABLE_ID));
          if (table == null) {
            throw new MolgenisException(
                "Cannot remove setting because table " + setting.get(TABLE + " does not exist"));
          }
          table.getMetadata().removeSetting(setting.get("key"));
          message.append("Removed table setting '" + (setting.get("key")) + "'\n");
        } else {
          schema.getMetadata().removeSetting(setting.get("key"));
          message.append("Removed schema setting '" + (setting.get("key")) + "'\n");
        }
      }
    }
  }

  private static void dropTables(
      Schema schema, DataFetchingEnvironment dataFetchingEnvironment, StringBuilder message) {
    List<String> tables = dataFetchingEnvironment.getArgument(GraphqlConstants.TABLES);
    if (tables != null) {
      for (String tableName : tables) {
        schema.dropTable(tableName);
        message.append("Dropped table '" + tableName + "'\n");
      }
    }
  }

  public GraphQLFieldDefinition.Builder schemaQuery(Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_schema")
        .type(outputMetadataType)
        .dataFetcher(GraphqlSchemaFieldFactory.queryFetcher(schema));
  }

  public GraphQLFieldDefinition.Builder changeLogQuery(Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_changes")
        .type(GraphQLList.list(changesMetadataType))
        .dataFetcher(
            dataFetchingEnvironment -> {
              int limit = dataFetchingEnvironment.getArgumentOrDefault("limit", 100);
              return schema.getChanges(limit);
            })
        .argument(GraphQLArgument.newArgument().name(LIMIT).type(Scalars.GraphQLInt));
  }

  public GraphQLFieldDefinition.Builder changeLogCountQuery(Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_changesCount")
        .type(Scalars.GraphQLInt)
        .dataFetcher(dataFetchingEnvironment -> schema.getChangesCount());
  }

  public GraphQLFieldDefinition.Builder settingsQuery(Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_settings")
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.KEYS)
                .type(GraphQLList.list(Scalars.GraphQLString)))
        .type(GraphQLList.list(outputSettingsType))
        .dataFetcher(
            dataFetchingEnvironment -> {
              final List<String> selectedKeys =
                  dataFetchingEnvironment.getArgumentOrDefault(KEYS, new ArrayList<>());
              final boolean includePages =
                  selectedKeys.stream().anyMatch(selectedKey -> selectedKey.startsWith("page."));

              return Stream.concat(
                      schema.getMetadata().getSettings().entrySet().stream()
                          .filter(
                              setting ->
                                  selectedKeys.isEmpty()
                                      || selectedKeys.contains(setting.getKey())
                                      || (includePages && setting.getKey().startsWith("page.")))
                          .map(entry -> Map.of("key", entry.getKey(), VALUE, entry.getValue())),
                      Stream.of(
                          Map.of(
                              "key",
                              IS_OIDC_ENABLED,
                              VALUE,
                              String.valueOf(schema.getDatabase().isOidcEnabled()))))
                  .toList();
            });
  }

  public GraphQLFieldDefinition changeMutation(Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("change")
        .type(typeForMutationResult)
        .dataFetcher(changeFetcher(schema))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.TABLES)
                .type(GraphQLList.list(inputTableMetadataType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.MEMBERS)
                .type(GraphQLList.list(inputMembersMetadataType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(Constants.SETTINGS)
                .type(GraphQLList.list(inputSettingsMetadataType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.COLUMNS)
                .type(GraphQLList.list(inputColumnMetadataType)))
        .build();
  }

  private DataFetcher<?> changeFetcher(Schema schema) {
    return dataFetchingEnvironment -> {
      schema
          .getDatabase()
          .tx(
              db -> {
                try {
                  Schema s = db.getSchema(schema.getName());
                  changeTables(s, dataFetchingEnvironment);
                  changeMembers(s, dataFetchingEnvironment);
                  changeColumns(s, dataFetchingEnvironment);
                  changeSettings(s, dataFetchingEnvironment);
                  // this sync is a bit sad.
                  ((SqlSchemaMetadata) schema.getMetadata())
                      .sync((SqlSchemaMetadata) s.getMetadata());
                } catch (IOException e) {
                  throw new GraphqlException("Save metadata failed", e);
                }
              });
      return new GraphqlApiMutationResult(SUCCESS, "Meta update success");
    };
  }

  private void changeColumns(Schema schema, DataFetchingEnvironment dataFetchingEnvironment)
      throws IOException {
    List<Map<String, String>> columns =
        dataFetchingEnvironment.getArgument(GraphqlConstants.COLUMNS);
    if (columns != null) {
      for (Map<String, String> c : columns) {
        String tableName = c.get(TABLE);
        TableMetadata tm = schema.getMetadata().getTableMetadata(tableName);
        if (tm == null) {
          throw new GraphqlException("Table '" + tableName + "' not found");
        }
        String json = JsonUtil.getWriter().writeValueAsString(c);
        Column column = JsonUtil.jsonToColumn(json);
        if (column.getOldName() != null) {
          tm.alterColumn(column.getOldName(), column);
        } else {
          tm.add(column);
        }
      }
    }
  }

  private void changeMembers(Schema schema, DataFetchingEnvironment dataFetchingEnvironment) {
    // members
    List<Map<String, String>> members =
        dataFetchingEnvironment.getArgument(GraphqlConstants.MEMBERS);
    if (members != null) {
      for (Map<String, String> m : members) {
        schema.addMember(m.get(EMAIL), m.get(ROLE));
      }
    }
  }

  private void changeTables(Schema schema, DataFetchingEnvironment dataFetchingEnvironment)
      throws IOException {
    Object tables = dataFetchingEnvironment.getArgument(GraphqlConstants.TABLES);
    // tables
    if (tables != null) {
      Map<String, ?> tableMap = Map.of("tables", tables);
      String json = JsonUtil.getWriter().writeValueAsString(tableMap);
      SchemaMetadata otherSchema = jsonToSchema(json);
      schema.migrate(otherSchema);
    }
  }

  private void changeSettings(Schema schema, DataFetchingEnvironment dataFetchingEnvironment) {
    List<Map<String, String>> settings = dataFetchingEnvironment.getArgument(SETTINGS);
    if (settings != null) {
      settings.forEach(
          entry -> {
            if (entry.get(TABLE_ID) != null) {
              Table table = schema.getTableById(entry.get(TABLE_ID));
              if (table == null)
                throw new MolgenisException(
                    "changeSettings failed: Table with id="
                        + entry.get(TABLE_ID)
                        + " cannot be found");
              table.getMetadata().setSetting(entry.get(KEY), entry.get(VALUE));
            } else {
              schema.getMetadata().setSetting(entry.get(KEY), entry.get(VALUE));
            }
          });
    }
  }

  private Map<String, String> convertKeyValueListToMap(List<Map<String, String>> keyValueList) {
    Map<String, String> keyValueMap = new LinkedHashMap<>();
    if (keyValueList != null) {
      keyValueList.forEach(
          entry -> {
            keyValueMap.put(entry.get(KEY), entry.get(VALUE));
          });
    }
    return keyValueMap;
  }

  public GraphQLFieldDefinition dropMutation(Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("drop")
        .type(typeForMutationResult)
        .dataFetcher(dropFetcher(schema))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.TABLES)
                .type(GraphQLList.list(Scalars.GraphQLString)))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.MEMBERS)
                .type(GraphQLList.list(Scalars.GraphQLString)))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.COLUMNS)
                .type(GraphQLList.list(inputDropColumnType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(SETTINGS)
                .type(GraphQLList.list(inputDropSettingType)))
        .build();
  }

  public GraphQLFieldDefinition truncateMutation(Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("truncate")
        .type(typeForMutationResult)
        .dataFetcher(truncateFetcher(schema))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.TABLES)
                .type(GraphQLList.list(Scalars.GraphQLString)))
        .build();
  }

  public GraphQLFieldDefinition schemaReportsField(Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_reports")
        .type(
            GraphQLObjectType.newObject()
                .name("MolgenisSqlQuery")
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(DATA)
                        .type(Scalars.GraphQLString))
                .field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(COUNT)
                        .type(Scalars.GraphQLInt)))
        .argument(GraphQLArgument.newArgument().name(ID).type(Scalars.GraphQLInt))
        .argument(
            GraphQLArgument.newArgument()
                .name(PARAMETERS)
                .type(GraphQLList.list(inputSettingsMetadataType)))
        .argument(GraphQLArgument.newArgument().name(LIMIT).type(Scalars.GraphQLInt))
        .argument(GraphQLArgument.newArgument().name(OFFSET).type(Scalars.GraphQLInt))
        .dataFetcher(
            dataFetchingEnvironment -> {
              Integer id = null;
              Map<String, Object> result = new LinkedHashMap<>();
              try {
                String reportsJson = schema.getMetadata().getSetting("reports");
                logger.info("REPORT value: " + reportsJson);
                if (reportsJson != null) {
                  id = dataFetchingEnvironment.getArgument(ID);
                  Integer offset = dataFetchingEnvironment.getArgumentOrDefault(OFFSET, 0);
                  Integer limit = dataFetchingEnvironment.getArgumentOrDefault(LIMIT, 10);
                  Map<String, String> parameters =
                      convertKeyValueListToMap(dataFetchingEnvironment.getArgument(PARAMETERS));
                  List<Map<String, Object>> reportList =
                      new ObjectMapper().readValue(reportsJson, List.class);
                  Map<String, Object> report = reportList.get(id);
                  String sql = report.get("sql") + " LIMIT " + limit + " OFFSET " + offset;
                  String countSql =
                      String.format("select count(*) from (%s) as count", report.get("sql"));
                  result.put(DATA, convertToJson(schema.retrieveSql(sql, parameters)));
                  result.put(
                      COUNT,
                      schema.retrieveSql(countSql, parameters).get(0).get("count", Integer.class));
                }
                return result;
              } catch (Exception e) {
                throw new MolgenisException("Retrieve of report '" + id + "' failed ", e);
              }
            })
        .build();
  }

  private String convertToJson(List<Row> rows) {
    try {
      List<Map<String, Object>> result = new ArrayList<>();
      for (Row row : rows) {
        result.add(row.getValueMap());
      }
      return new ObjectMapper().writeValueAsString(result);
    } catch (Exception e) {
      throw new MolgenisException("Cannot convert sql result set to json", e);
    }
  }
}
