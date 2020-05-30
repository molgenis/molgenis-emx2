package org.molgenis.emx2.web.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.web.json.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.graphql.GraphqlAccountFields.EMAIL;
import static org.molgenis.emx2.web.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.web.graphql.GraphqlApiMutationResult.typeForMutationResult;

public class GraphqlSchemaFields {
  public static final String REF_TABLE_NAME = "refTable";
  public static final String REF_COLUMN_NAME = "refColumn";
  public static final String MAPPED_BY = "mappedBy";
  public static final String TABLE = "table";
  public static final String COLUMN = "column";
  public static final String DESCRIPTION = "description";
  public static final String DEFINITION = "definition";
  public static final String ROLE = "role";
  public static final String PKEY = "pkey";

  private GraphqlSchemaFields() {
    // hide constructor
  }

  public static GraphQLFieldDefinition.Builder schemaQuery(Schema schema) {
    return newFieldDefinition()
        .name("_schema")
        .type(outputMetadataType)
        .dataFetcher(GraphqlSchemaFields.queryFetcher(schema));
  }

  public static GraphQLFieldDefinition createMutation(Schema schema) {
    return newFieldDefinition()
        .name("create")
        .type(typeForMutationResult)
        .dataFetcher(createFetcher(schema))
        .argument(newArgument().name(TABLES).type(GraphQLList.list(inputTableMetadataType)))
        .argument(newArgument().name(MEMBERS).type(GraphQLList.list(inputMembersMetadataType)))
        .argument(newArgument().name(COLUMNS).type(GraphQLList.list(inputColumnMetadataType)))
        .build();
  }

  private static GraphQLInputObjectType inputColumnMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisColumnInput")
          .field(newInputObjectField().name(TABLE).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(NAME).type(Scalars.GraphQLString))
          .field(newInputObjectField().name("columnType").type(Scalars.GraphQLString))
          .field(newInputObjectField().name(PKEY).type(Scalars.GraphQLBoolean))
          .field(newInputObjectField().name("nullable").type(Scalars.GraphQLBoolean))
          .field(newInputObjectField().name(REF_TABLE_NAME).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(REF_COLUMN_NAME).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(MAPPED_BY).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(DESCRIPTION).type(Scalars.GraphQLString))
          .build();

  private static final GraphQLInputObjectType inputTableMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisTableInput")
          .field(newInputObjectField().name(NAME).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(PKEY).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(DESCRIPTION).type(Scalars.GraphQLString))
          .field(
              newInputObjectField()
                  .name("unique")
                  .type(GraphQLList.list(GraphQLList.list(Scalars.GraphQLString))))
          .field(
              newInputObjectField().name("columns").type(GraphQLList.list(inputColumnMetadataType)))
          .build();

  private static final GraphQLInputObjectType inputMembersMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisMembersInput")
          .field(newInputObjectField().name(EMAIL).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(ROLE).type(Scalars.GraphQLString))
          .build();

  private static DataFetcher<?> createFetcher(Schema schema) {
    return dataFetchingEnvironment -> {
      schema.tx(
          db -> {
            try {
              Object tables = dataFetchingEnvironment.getArgument(TABLES);
              // tables
              if (tables != null) {
                Map tableMap = Map.of("tables", tables);
                String json = JsonUtil.getWriter().writeValueAsString(tableMap);
                SchemaMetadata otherSchema = JsonUtil.jsonToSchema(json);
                schema.merge(otherSchema);
              }
              // members
              List<Map<String, String>> members = dataFetchingEnvironment.getArgument(MEMBERS);
              if (members != null) {
                for (Map<String, String> m : members) {
                  schema.addMember(m.get(EMAIL), m.get(ROLE));
                }
              }
              // columns
              List<Map<String, String>> columns = dataFetchingEnvironment.getArgument(COLUMNS);
              if (columns != null) {
                for (Map<String, String> c : columns) {
                  String tableName = c.get(TABLE);
                  TableMetadata tm = schema.getMetadata().getTableMetadata(tableName);
                  if (tm == null) {
                    throw new GraphqlException("", "Table '" + tableName + "' not found");
                  }
                  String json = JsonUtil.getWriter().writeValueAsString(c);
                  Column column = JsonUtil.jsonToColumn(json);

                  tm.addColumn(column);
                }
              }
            } catch (IOException e) {
              throw new GraphqlException("Save metadata failed", e);
            }
          });
      return new GraphqlApiMutationResult(SUCCESS, "Meta update success");
    };
  }

  public static GraphQLFieldDefinition alterMutation(Schema schema) {
    return newFieldDefinition()
        .name("alter")
        .type(typeForMutationResult)
        .dataFetcher(alterFetcher(schema))
        .argument(newArgument().name(TABLES).type(GraphQLList.list(inputAlterTableType)))
        .argument(newArgument().name(MEMBERS).type(GraphQLList.list(inputMembersMetadataType)))
        .argument(newArgument().name(COLUMNS).type(GraphQLList.list(inputAlterColumnType)))
        .build();
  }

  private static final GraphQLInputObjectType inputAlterTableType =
      new GraphQLInputObjectType.Builder()
          .name("AlterTableInput")
          .field(newInputObjectField().name(NAME).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(DEFINITION).type(inputTableMetadataType))
          .build();

  private static final GraphQLInputObjectType inputAlterColumnType =
      new GraphQLInputObjectType.Builder()
          .name("AlterColumnInput")
          .field(newInputObjectField().name(TABLE).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(NAME).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(DEFINITION).type(inputColumnMetadataType))
          .build();

  private static DataFetcher<?> alterFetcher(Schema schema) {
    return dataFetchingEnvironment -> {
      schema.tx(
          db -> {
            try {
              Object tables = dataFetchingEnvironment.getArgument(TABLES);

              // tables
              if (tables != null) {
                //                Map tableMap = Map.of("tables", tables);
                //                String json = JsonApi.getWriter().writeValueAsString(tableMap);
                //                SchemaMetadata otherSchema = jsonToSchema(json);
                //                schema.merge(otherSchema);
              }
              // members
              List<Map<String, String>> members = dataFetchingEnvironment.getArgument(MEMBERS);
              if (members != null) {
                for (Map<String, String> m : members) {
                  schema.addMember(m.get(EMAIL), m.get(ROLE));
                }
              }
              // columns {table,name,definition}
              List<Map<String, String>> columns = dataFetchingEnvironment.getArgument(COLUMNS);
              if (columns != null) {
                for (Map<String, String> c : columns) {
                  String tableName = c.get(TABLE);
                  String columnName = c.get(NAME);
                  String json = JsonUtil.getWriter().writeValueAsString(c.get(DEFINITION));
                  Column columnDefinition = JsonUtil.jsonToColumn(json);
                  TableMetadata tm = schema.getMetadata().getTableMetadata(tableName);
                  if (tm == null) {
                    throw new GraphqlException("", "Table '" + tableName + "' not found");
                  }

                  tm.alterColumn(columnName, columnDefinition);
                }
              }
            } catch (IOException e) {
              throw new GraphqlException("Save metadata failed", e);
            }
          });
      return new GraphqlApiMutationResult(SUCCESS, "Meta update success");
    };
  }

  public static GraphQLFieldDefinition dropMutation(Schema schema) {
    return newFieldDefinition()
        .name("drop")
        .type(typeForMutationResult)
        .dataFetcher(dropFetcher(schema))
        .argument(newArgument().name(TABLES).type(GraphQLList.list(Scalars.GraphQLString)))
        .argument(newArgument().name(MEMBERS).type(GraphQLList.list(Scalars.GraphQLString)))
        .argument(newArgument().name(COLUMNS).type(GraphQLList.list(inputDropColumnType)))
        .build();
  }

  private static final GraphQLInputObjectType inputDropColumnType =
      new GraphQLInputObjectType.Builder()
          .name("DropColumnInput")
          .field(newInputObjectField().name(TABLE).type(Scalars.GraphQLString))
          .field(newInputObjectField().name(COLUMN).type(Scalars.GraphQLString))
          .build();

  // medatadata
  private static final GraphQLType outputRolesMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisRolesType")
          .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .build();

  private static final GraphQLType outputMembersMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisMembersType")
          .field(newFieldDefinition().name(EMAIL).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(ROLE).type(Scalars.GraphQLString))
          .build();

  private static final GraphQLType outputSettingsMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisSettingsType")
          .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(TYPE).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(DESCRIPTION).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(VALUE).type(Scalars.GraphQLString))
          .build();

  private static final GraphQLObjectType outputColumnMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisColumnType")
          .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("columnType").type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(PKEY).type(Scalars.GraphQLBoolean))
          .field(newFieldDefinition().name("nullable").type(Scalars.GraphQLBoolean))
          .field(newFieldDefinition().name(REF_TABLE_NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(REF_COLUMN_NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(MAPPED_BY).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("validation").type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(DESCRIPTION).type(Scalars.GraphQLString))
          .build();

  private static final GraphQLObjectType outputTableMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisTableType")
          .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(PKEY).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(DESCRIPTION).type(Scalars.GraphQLString))
          .field(
              newFieldDefinition()
                  .name("unique")
                  .type(GraphQLList.list(GraphQLList.list(Scalars.GraphQLString))))
          .field(
              newFieldDefinition().name("columns").type(GraphQLList.list(outputColumnMetadataType)))
          .build();

  private static final GraphQLObjectType outputMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisMetaType")
          .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name(TABLES).type(GraphQLList.list(outputTableMetadataType)))
          .field(
              newFieldDefinition().name(MEMBERS).type(GraphQLList.list(outputMembersMetadataType)))
          .field(
              newFieldDefinition()
                  .name(SETTINGS)
                  .type(GraphQLList.list(outputSettingsMetadataType)))
          .field(newFieldDefinition().name("roles").type(GraphQLList.list(outputRolesMetadataType)))
          .build();

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
        roles.add(Map.of(NAME, role));
      }
      result.put("roles", roles);
      result.put("name", schema.getMetadata().getName());

      // add settings

      return result;
    };
  }

  private static DataFetcher<?> dropFetcher(Schema schema) {
    return dataFetchingEnvironment -> {
      StringBuffer message = new StringBuffer();
      schema.tx(
          db -> {
            List<String> tables = dataFetchingEnvironment.getArgument(TABLES);
            if (tables != null) {
              for (String tableName : tables) {
                schema.dropTable(tableName);
                message.append("Dropped table '" + tableName + "'\n");
              }
            }
            List<String> members = dataFetchingEnvironment.getArgument(MEMBERS);
            if (members != null) {
              for (String name : members) {
                schema.removeMember(name);
                message.append("Dropped member '" + name + "'\n");
              }
            }
            List<Map> columns = dataFetchingEnvironment.getArgument(COLUMNS);
            if (columns != null) {
              for (Map col : columns) {
                schema
                    .getMetadata()
                    .getTableMetadata((String) col.get(TABLE))
                    .dropColumn((String) col.get(COLUMN));
              }
            }
          });
      Map result = new LinkedHashMap<>();
      result.put(DETAIL, message.toString());
      return result;
    };
  }
}
