package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.json.JsonUtil.jsonToSchema;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.Scalars;
import graphql.schema.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;

public class GraphqlSchemaFieldFactory {

  static final GraphQLInputObjectType inputAlterSettingType =
      new GraphQLInputObjectType.Builder()
          .name("AlterSettingInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(GraphqlConstants.KEY)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(VALUE).type(Scalars.GraphQLString))
          .build();
  static final GraphQLType outputSettingsMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisSettingsType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.KEY)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.VALUE)
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
  // medatadata
  private static final GraphQLType outputRolesMetadataType =
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
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(COLUMN_TYPE)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(Constants.KEY)
                  .type(Scalars.GraphQLInt))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(NULLABLE)
                  .type(Scalars.GraphQLBoolean))
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
                  .name(REF_FROM)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REF_TO)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(REF_JS_TEMPLATE)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(CASCADE_DELETE)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(MAPPED_BY)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.VALIDATION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(RDF_TEMPLATE)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(INHERITED)
                  .type(Scalars.GraphQLBoolean))
          .build();
  private static final GraphQLObjectType outputTableMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisTableType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.EXTERNAL_SCHEMA)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.INHERIT)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.COLUMNS)
                  .type(GraphQLList.list(outputColumnMetadataType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SETTINGS)
                  .type(GraphQLList.list(outputSettingsMetadataType)))
          .build();
  private static final GraphQLObjectType outputMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisMetaType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.TABLES)
                  .type(GraphQLList.list(outputTableMetadataType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.MEMBERS)
                  .type(GraphQLList.list(outputMembersMetadataType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name("roles")
                  .type(GraphQLList.list(outputRolesMetadataType)))
          .build();
  private final GraphQLInputObjectType inputMembersMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisMembersInput")
          .field(
              GraphQLInputObjectField.newInputObjectField().name(EMAIL).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(ROLE).type(Scalars.GraphQLString))
          .build();
  private final GraphQLInputObjectType inputAlterTableType =
      new GraphQLInputObjectType.Builder()
          .name("AlterTableInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(GraphqlConstants.NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(DEFINITION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(SETTINGS)
                  .type(GraphQLList.list(inputAlterSettingType)))
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
                  .name(COLUMN_TYPE)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(Constants.KEY)
                  .type(Scalars.GraphQLInt))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(NULLABLE)
                  .type(Scalars.GraphQLBoolean))
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
                  .name(REF_FROM)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(REF_TO)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(CASCADE_DELETE)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(MAPPED_BY)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(RDF_TEMPLATE)
                  .type(Scalars.GraphQLString))
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
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(GraphqlConstants.COLUMNS)
                  .type(GraphQLList.list(inputColumnMetadataType)))
          .build();
  private final GraphQLInputObjectType inputAlterColumnType =
      new GraphQLInputObjectType.Builder()
          .name("AlterColumnInput")
          .field(
              GraphQLInputObjectField.newInputObjectField().name(TABLE).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(GraphqlConstants.NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(DEFINITION)
                  .type(inputColumnMetadataType))
          .build();

  public GraphqlSchemaFieldFactory() {
    // hide constructor
  }

  private static DataFetcher<?> queryFetcher(Schema schema) {
    return dataFetchingEnvironment -> {

      // add tables
      String json = JsonUtil.schemaToJson(schema.getMetadata());
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
      result.put("roles", roles);

      result.put("name", schema.getMetadata().getName());
      return result;
    };
  }

  private static DataFetcher<?> dropFetcher(Schema schema) {
    return dataFetchingEnvironment -> {
      StringBuilder message = new StringBuilder();
      schema.tx(
          db -> {
            dropTables(schema, dataFetchingEnvironment, message);
            dropMembers(schema, dataFetchingEnvironment, message);
            dropColumns(schema, dataFetchingEnvironment, message);
          });
      Map result = new LinkedHashMap<>();
      result.put(GraphqlConstants.DETAIL, message.toString());
      return result;
    };
  }

  private static void dropColumns(
      Schema schema, DataFetchingEnvironment dataFetchingEnvironment, StringBuilder message) {
    List<Map> columns = dataFetchingEnvironment.getArgument(GraphqlConstants.COLUMNS);
    if (columns != null) {
      for (Map col : columns) {
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

  public GraphQLFieldDefinition.Builder settingsQuery(Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_settings")
        .type(GraphQLList.list(outputSettingsMetadataType))
        .dataFetcher(
            dataFetchingEnvironment ->
                // add settings
                schema.getMetadata().getSettings().entrySet().stream()
                    .map(entry -> Map.of("key", entry.getKey(), VALUE, entry.getValue()))
                    .collect(Collectors.toList()));
  }

  public GraphQLFieldDefinition createMutation(Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("create")
        .type(typeForMutationResult)
        .dataFetcher(createFetcher(schema))
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
                .name(GraphqlConstants.COLUMNS)
                .type(GraphQLList.list(inputColumnMetadataType)))
        .build();
  }

  private DataFetcher<?> createFetcher(Schema schema) {
    return dataFetchingEnvironment -> {
      schema.tx(
          db -> {
            try {
              createTables(schema, dataFetchingEnvironment);
              createMembers(schema, dataFetchingEnvironment);
              createColumns(schema, dataFetchingEnvironment);
            } catch (IOException e) {
              throw new GraphqlException("Save metadata failed", e);
            }
          });
      return new GraphqlApiMutationResult(SUCCESS, "Meta update success");
    };
  }

  private void createColumns(Schema schema, DataFetchingEnvironment dataFetchingEnvironment)
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

        tm.add(column);
      }
    }
  }

  private void createMembers(Schema schema, DataFetchingEnvironment dataFetchingEnvironment) {
    // members
    List<Map<String, String>> members =
        dataFetchingEnvironment.getArgument(GraphqlConstants.MEMBERS);
    if (members != null) {
      for (Map<String, String> m : members) {
        schema.addMember(m.get(EMAIL), m.get(ROLE));
      }
    }
  }

  private void createTables(Schema schema, DataFetchingEnvironment dataFetchingEnvironment)
      throws IOException {
    Object tables = dataFetchingEnvironment.getArgument(GraphqlConstants.TABLES);
    // tables
    if (tables != null) {
      Map tableMap = Map.of("tables", tables);
      String json = JsonUtil.getWriter().writeValueAsString(tableMap);
      SchemaMetadata otherSchema = jsonToSchema(json);
      schema.merge(otherSchema);
    }
  }

  public GraphQLFieldDefinition alterMutation(Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("alter")
        .type(typeForMutationResult)
        .dataFetcher(alterFetcher(schema))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.TABLES)
                .type(GraphQLList.list(inputAlterTableType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.MEMBERS)
                .type(GraphQLList.list(inputMembersMetadataType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.COLUMNS)
                .type(GraphQLList.list(inputAlterColumnType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(SETTINGS)
                .type(GraphQLList.list(inputAlterSettingType)))
        .build();
  }

  private DataFetcher<?> alterFetcher(Schema schema) {
    return dataFetchingEnvironment -> {
      schema.tx(
          db -> {
            try {
              createTables(schema, dataFetchingEnvironment);
              createMembers(schema, dataFetchingEnvironment);
              alterColumns(schema, dataFetchingEnvironment);
              createSettings(schema, dataFetchingEnvironment);
            } catch (IOException e) {
              throw new GraphqlException("Save metadata failed", e);
            }
          });
      return new GraphqlApiMutationResult(SUCCESS, "Meta update success");
    };
  }

  private void createSettings(Schema schema, DataFetchingEnvironment dataFetchingEnvironment) {
    List<Map<String, String>> settings = dataFetchingEnvironment.getArgument(SETTINGS);
    if (settings != null) {
      // get the old settings
      Map<String, String> settingsMap = schema.getMetadata().getSettings();
      // convert from  {key:xx,value:yy} to {xx:yy}, merge with old settings
      settings.forEach(
          entry -> {
            if (entry.get(VALUE) == null || entry.get(VALUE).trim().equals("")) {
              // remove the key
              settingsMap.remove(entry.get("key"));
            } else {
              settingsMap.put(entry.get("key"), entry.get(VALUE));
            }
          });
      schema.getMetadata().setSettings(settingsMap);
    }
  }

  private void alterColumns(Schema schema, DataFetchingEnvironment dataFetchingEnvironment)
      throws IOException {
    List<Map<String, String>> columns =
        dataFetchingEnvironment.getArgument(GraphqlConstants.COLUMNS);
    if (columns != null) {
      for (Map<String, String> c : columns) {
        String tableName = c.get(TABLE);
        String columnName = c.get(GraphqlConstants.NAME);
        String json = JsonUtil.getWriter().writeValueAsString(c.get(DEFINITION));
        Column columnDefinition = JsonUtil.jsonToColumn(json);
        TableMetadata tm = schema.getMetadata().getTableMetadata(tableName);
        if (tm == null) {
          throw new GraphqlException("Table '" + tableName + "' not found");
        }

        tm.alterColumn(columnName, columnDefinition);
      }
    }
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
        .build();
  }
}
