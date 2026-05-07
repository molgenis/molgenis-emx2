package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.graphql.GraphqlAdminFieldFactory.mapSettingsToGraphql;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.Status.SUCCESS;
import static org.molgenis.emx2.graphql.GraphqlApiMutationResult.typeForMutationResult;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;
import static org.molgenis.emx2.graphql.GraphqlConstants.INHERITED;
import static org.molgenis.emx2.graphql.GraphqlConstants.KEY;
import static org.molgenis.emx2.json.JsonUtil.jsonToSchema;
import static org.molgenis.emx2.settings.ReportUtils.getReportAsJson;
import static org.molgenis.emx2.settings.ReportUtils.getReportCount;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.Scalars;
import graphql.schema.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlRoleManager;
import org.molgenis.emx2.sql.SqlSchemaMetadata;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskService;

public class GraphqlSchemaFieldFactory {

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
  private static final GraphQLType outputPermissionType =
      new GraphQLObjectType.Builder()
          .name("MolgenisPermissionType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(TABLE).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.SELECT)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.INSERT)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.UPDATE)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.DELETE)
                  .type(Scalars.GraphQLBoolean))
          .build();

  static final GraphQLType outputRolesType =
      new GraphQLObjectType.Builder()
          .name("MolgenisRolesType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.SYSTEM)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.PERMISSIONS)
                  .type(GraphQLList.list(outputPermissionType)))
          .build();

  static final GraphQLType userRolesType =
      new GraphQLObjectType.Builder()
          .name("MolgenisUserRolesType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SCHEMA_ID)
                  .type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(ROLE).type(Scalars.GraphQLString))
          .build();

  private static final GraphQLType outputMembersMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisMembersType")
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(EMAIL).type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(ROLE).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(GROUP).type(Scalars.GraphQLString))
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
                  .name(GraphqlConstants.SECTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(GraphqlConstants.HEADING)
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
                  .name(GraphqlConstants.COLUMN_FORM_LABEL)
                  .type(Scalars.GraphQLString))
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
                  .type(Scalars.GraphQLString))
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
                  .name(SCHEMA_ID)
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

  private final GraphQLInputObjectType inputMembersMetadataType =
      new GraphQLInputObjectType.Builder()
          .name("MolgenisMembersInput")
          .field(
              GraphQLInputObjectField.newInputObjectField().name(EMAIL).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(USER).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(ROLE).type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(GROUP).type(Scalars.GraphQLString))
          .build();
  private final GraphQLInputObjectType inputColumnMetadataType =
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
                  .name(COLUMN_FORM_LABEL)
                  .type(Scalars.GraphQLString))
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
                  .type(Scalars.GraphQLString))
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
          .build();

  public GraphqlSchemaFieldFactory() {
    // hide constructor
  }

  static Map<String, Object> roleToMap(Role role) {
    Map<String, Object> roleMap = new LinkedHashMap<>();
    roleMap.put(GraphqlConstants.NAME, role.name());
    roleMap.put(GraphqlConstants.SYSTEM, role.isSystemRole());
    roleMap.put(
        GraphqlConstants.PERMISSIONS,
        role.permissions().stream()
            .map(
                p -> {
                  Map<String, Object> permMap = new LinkedHashMap<>();
                  permMap.put(TABLE, p.table());
                  permMap.put(GraphqlConstants.SELECT, p.select());
                  permMap.put(GraphqlConstants.INSERT, p.insert());
                  permMap.put(GraphqlConstants.UPDATE, p.update());
                  permMap.put(GraphqlConstants.DELETE, p.delete());
                  return permMap;
                })
            .toList());
    return roleMap;
  }

  private static DataFetcher<?> queryFetcher(Schema schema) {
    return dataFetchingEnvironment -> {

      // add tables
      String json = JsonUtil.schemaToJson(schema.getMetadata(), false);
      Map<String, Object> result = new ObjectMapper().readValue(json, Map.class);

      SqlRoleManager roleManager = ((SqlDatabase) schema.getDatabase()).getRoleManager();
      String schemaName = schema.getName();

      List<Map<String, Object>> members = new ArrayList<>();
      for (Member m : schema.getMembers()) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(EMAIL, m.getUser());
        row.put(ROLE, m.getRole());
        row.put(GROUP, null);
        members.add(row);
      }
      for (Map<String, Object> customRow : roleManager.listCustomMemberships(schemaName)) {
        members.add(customRow);
      }
      result.put(MEMBERS, members);

      List<Map<String, Object>> allRoles = new ArrayList<>();
      for (Role role : schema.getRoleInfos()) {
        PermissionSet ps;
        if (role.isSystemRole()) {
          PermissionSet.TablePermissions wildcard =
              new PermissionSet.TablePermissions()
                  .setSelect(SelectScope.ALL)
                  .setInsert(UpdateScope.ALL)
                  .setUpdate(UpdateScope.ALL)
                  .setDelete(UpdateScope.ALL);
          ps = new PermissionSet().setDescription(role.name()).putTable("*", wildcard);
        } else {
          ps = roleManager.getPermissions(schema, role.name());
        }
        allRoles.add(
            GraphqlPermissionFieldFactory.permissionSetToMap(
                role.name(), schemaName, role.isSystemRole(), ps));
      }

      result.put(ROLES, allRoles);

      result.put(GROUPS, roleManager.listGroups(schema));

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
                dropRoles(s, dataFetchingEnvironment, message);
                dropGroups(s, dataFetchingEnvironment, message);
                dropColumns(s, dataFetchingEnvironment, message);
                dropSettings(s, dataFetchingEnvironment, message);
                // this sync is a bit sad.
                ((SqlSchemaMetadata) schema.getMetadata())
                    .sync((SqlSchemaMetadata) s.getMetadata());
                db.getListener().onSchemaChange();
              });
      return new GraphqlApiMutationResult(SUCCESS, message.toString());
    };
  }

  private static DataFetcher<?> truncateFetcher(Schema schema, TaskService taskService) {
    return dataFetchingEnvironment -> {
      List<String> tables = dataFetchingEnvironment.getArgument(GraphqlConstants.TABLES);
      boolean async = dataFetchingEnvironment.getArgumentOrDefault(GraphqlConstants.ASYNC, false);
      GraphqlApiMutationResult result =
          new GraphqlApiMutationResult(SUCCESS, "Truncated tables: " + String.join(", ", tables));

      if (async) {
        Task task =
            new Task() {
              @Override
              public void run() {
                this.start();
                this.setDescription("Truncating table: " + String.join(", ", tables));
                try {
                  truncateTables(schema, tables);
                } catch (MolgenisException e) {
                  this.completeWithError(e.getMessage());
                  throw (e);
                }
                this.setDescription("Completed truncating table");
                this.complete();
              }
            };
        task.setDescription("Truncating table");
        String id = taskService.submit(task);
        result.setTaskId(id);
      } else {
        truncateTables(schema, tables);
      }
      return result;
    };
  }

  private static void truncateTables(Schema schema, List<String> tables) {
    schema
        .getDatabase()
        .tx(
            db -> {
              Schema s = db.getSchema(schema.getName());
              if (tables != null) {
                for (String tableName : tables) {
                  Table table = s.getTable(tableName);
                  if (table == null) {
                    throw new GraphqlException("Truncate failed: table " + tableName + " unknown");
                  } else {
                    table.truncate();
                  }
                }
              }
            });
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

  private static void changeRoles(Schema schema, DataFetchingEnvironment dataFetchingEnvironment) {
    List<Map<String, Object>> roles = dataFetchingEnvironment.getArgument(GraphqlConstants.ROLES);
    if (roles == null) return;
    SqlRoleManager roleManager = ((SqlDatabase) schema.getDatabase()).getRoleManager();
    for (Map<String, Object> roleMap : roles) {
      Object nameVal = roleMap.get(GraphqlConstants.NAME);
      if (!(nameVal instanceof String roleName)) continue;
      if (roleManager.isSystemRole(roleName)) {
        throw new MolgenisException("System roles are immutable: cannot modify '" + roleName + "'");
      }
    }
    GraphqlPermissionFieldFactory.requireManagerOrOwner(schema.getDatabase(), schema);
    for (Map<String, Object> roleMap : roles) {
      Object nameVal = roleMap.get(GraphqlConstants.NAME);
      if (!(nameVal instanceof String roleName)) continue;
      Object descVal = roleMap.get(GraphqlConstants.DESCRIPTION);
      String description = descVal instanceof String str ? str : "";
      if (!roleManager.roleExists(schema.getName(), roleName)) {
        roleManager.createRole(schema, roleName, description);
      }
      PermissionSet ps = GraphqlPermissionFieldFactory.toPermissionSet(roleMap);
      ps.setSchema(schema.getName());
      roleManager.setPermissions(schema, roleName, ps);
    }
  }

  private static void changeGroups(Schema schema, DataFetchingEnvironment dataFetchingEnvironment) {
    List<Map<String, Object>> groups = dataFetchingEnvironment.getArgument(GraphqlConstants.GROUPS);
    if (groups == null) return;
    GraphqlPermissionFieldFactory.requireManagerOrOwner(schema.getDatabase(), schema);
    SqlRoleManager roleManager = ((SqlDatabase) schema.getDatabase()).getRoleManager();
    for (Map<String, Object> groupMap : groups) {
      Object nameVal = groupMap.get(GraphqlConstants.NAME);
      if (!(nameVal instanceof String groupName)) continue;
      boolean exists =
          roleManager.listGroups(schema).stream()
              .anyMatch(existing -> groupName.equals(existing.get("name")));
      if (!exists) {
        roleManager.createGroup(schema, groupName);
      }
      Object usersVal = groupMap.get(GraphqlConstants.USERS);
      if (usersVal instanceof List<?> userList) {
        List<String> desired =
            userList.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        replaceGroupMembers(roleManager, schema, groupName, desired);
      }
    }
  }

  private static void replaceGroupMembers(
      SqlRoleManager roleManager, Schema schema, String groupName, List<String> desired) {
    List<String> current = currentGroupMembers(roleManager, schema, groupName);
    for (String user : desired) {
      if (!current.contains(user)) {
        roleManager.addGroupMember(schema, groupName, user);
      }
    }
    for (String user : current) {
      if (!desired.contains(user)) {
        roleManager.removeGroupMember(schema, groupName, user);
      }
    }
  }

  private static List<String> currentGroupMembers(
      SqlRoleManager roleManager, Schema schema, String groupName) {
    return roleManager.listGroups(schema).stream()
        .filter(entry -> groupName.equals(entry.get("name")))
        .findFirst()
        .map(
            entry -> {
              Object users = entry.get("users");
              if (users instanceof List<?> list) {
                return list.stream()
                    .filter(Map.class::isInstance)
                    .map(u -> ((Map<?, ?>) u).get("name"))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
              }
              return List.<String>of();
            })
        .orElse(List.of());
  }

  private static void dropRoles(
      Schema schema, DataFetchingEnvironment dataFetchingEnvironment, StringBuilder message) {
    List<String> roles = dataFetchingEnvironment.getArgument(GraphqlConstants.ROLES);
    if (roles == null) return;
    GraphqlPermissionFieldFactory.requireManagerOrOwner(schema.getDatabase(), schema);
    SqlRoleManager roleManager = ((SqlDatabase) schema.getDatabase()).getRoleManager();
    for (String roleName : roles) {
      roleManager.deleteRole(schema, roleName);
      message.append("Dropped role '").append(roleName).append("'\n");
    }
  }

  private static void dropGroups(
      Schema schema, DataFetchingEnvironment dataFetchingEnvironment, StringBuilder message) {
    List<String> groups = dataFetchingEnvironment.getArgument(GraphqlConstants.GROUPS);
    if (groups == null) return;
    GraphqlPermissionFieldFactory.requireManagerOrOwner(schema.getDatabase(), schema);
    SqlRoleManager roleManager = ((SqlDatabase) schema.getDatabase()).getRoleManager();
    for (String groupName : groups) {
      roleManager.deleteGroup(schema, groupName);
      message.append("Dropped group '").append(groupName).append("'\n");
    }
  }

  private static void dropMembers(
      Schema schema, DataFetchingEnvironment dataFetchingEnvironment, StringBuilder message) {
    List<Map<String, String>> members =
        dataFetchingEnvironment.getArgument(GraphqlConstants.MEMBERS);
    if (members == null) return;
    SqlRoleManager roleManager = ((SqlDatabase) schema.getDatabase()).getRoleManager();
    for (Map<String, String> m : members) {
      String userField = m.get(USER);
      String resolvedUser = (userField != null && !userField.isEmpty()) ? userField : m.get(EMAIL);
      String role = m.get(ROLE);
      String group = m.get(GROUP);
      if (roleManager.isSystemRole(role)) {
        schema.removeMember(resolvedUser);
        message.append("Dropped member '").append(resolvedUser).append("'\n");
      } else {
        rejectCustomRoleEscalation(schema, role);
        if (group == null || group.isEmpty()) {
          roleManager.revokeRoleFromUser(schema, role, resolvedUser);
          message.append("Dropped schema-wide member '").append(resolvedUser).append("'\n");
        } else {
          roleManager.removeGroupMembership(schema.getName(), group, resolvedUser, role);
          message
              .append("Dropped member '")
              .append(resolvedUser)
              .append("' from group '")
              .append(group)
              .append("'\n");
        }
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
    GraphQLObjectType.Builder builder =
        new GraphQLObjectType.Builder()
            .name("MolgenisSchema")
            .field(GraphQLFieldDefinition.newFieldDefinition().name(ID).type(Scalars.GraphQLString))
            .field(
                GraphQLFieldDefinition.newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
            .field(
                GraphQLFieldDefinition.newFieldDefinition().name(LABEL).type(Scalars.GraphQLString))
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name(TABLES)
                    .type(GraphQLList.list(outputTableType)))
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name(SETTINGS)
                    .type(GraphQLList.list(outputSettingsType)))
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name(ROLES)
                    .type(GraphQLList.list(GraphqlPermissionFieldFactory.roleOutputType)))
            .field(
                GraphQLFieldDefinition.newFieldDefinition()
                    .name(GROUPS)
                    .type(GraphQLList.list(GraphqlPermissionFieldFactory.groupOutputType)));

    List<String> roles = schema.getInheritedRolesForActiveUser();
    if (roles.contains(Privileges.MANAGER.toString())
        || roles.contains(Privileges.OWNER.toString())) {
      builder.field(
          GraphQLFieldDefinition.newFieldDefinition()
              .name(MEMBERS)
              .type(GraphQLList.list(outputMembersMetadataType)));
    }

    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_schema")
        .type(builder)
        .dataFetcher(GraphqlSchemaFieldFactory.queryFetcher(schema));
  }

  public GraphQLFieldDefinition.Builder changeLogQuery(Schema schema) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("_changes")
        .type(GraphQLList.list(changesMetadataType))
        .dataFetcher(
            dataFetchingEnvironment -> {
              int limit = dataFetchingEnvironment.getArgumentOrDefault("limit", 100);
              int offset = dataFetchingEnvironment.getArgumentOrDefault("offset", 0);
              return schema.getChanges(limit, offset);
            })
        .argument(GraphQLArgument.newArgument().name(LIMIT).type(Scalars.GraphQLInt))
        .argument(GraphQLArgument.newArgument().name(OFFSET).type(Scalars.GraphQLInt));
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
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.ROLES)
                .type(GraphQLList.list(GraphqlPermissionFieldFactory.inputRoleType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.GROUPS)
                .type(GraphQLList.list(GraphqlPermissionFieldFactory.groupInputType)))
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
                  if (s == null) {
                    throw new MolgenisException(
                        "Not authorized: schema '" + schema.getName() + "' is not accessible");
                  }
                  changeTables(s, dataFetchingEnvironment);
                  changeRoles(s, dataFetchingEnvironment);
                  changeGroups(s, dataFetchingEnvironment);
                  changeMembers(s, dataFetchingEnvironment);
                  changeColumns(s, dataFetchingEnvironment);
                  changeSettings(s, dataFetchingEnvironment);
                  ((SqlSchemaMetadata) schema.getMetadata())
                      .sync((SqlSchemaMetadata) s.getMetadata());
                  db.getListener().onSchemaChange();
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
    List<Map<String, String>> members =
        dataFetchingEnvironment.getArgument(GraphqlConstants.MEMBERS);
    if (members == null) return;
    SqlRoleManager roleManager = ((SqlDatabase) schema.getDatabase()).getRoleManager();
    for (Map<String, String> m : members) {
      String userField = m.get(USER);
      String resolvedUser = (userField != null && !userField.isEmpty()) ? userField : m.get(EMAIL);
      String role = m.get(ROLE);
      String group = m.get(GROUP);
      if (roleManager.isSystemRole(role)) {
        if (group != null && !group.isEmpty()) {
          throw new MolgenisException("System role '" + role + "' cannot be assigned to a group");
        }
        schema.addMember(resolvedUser, role);
      } else {
        rejectCustomRoleEscalation(schema, role);
        if (group == null || group.isEmpty()) {
          roleManager.grantRoleToUser(schema, role, resolvedUser);
        } else {
          roleManager.addGroupMembership(schema.getName(), group, resolvedUser, role);
        }
      }
    }
  }

  private static void rejectCustomRoleEscalation(Schema schema, String roleName) {
    if (schema.getDatabase().isAdmin()) return;
    if (schema.hasActiveUserRole(Privileges.OWNER)) return;
    if (schema.hasActiveUserRole(Privileges.MANAGER)) return;
    throw new MolgenisException(
        "Privilege escalation denied: only admin, Owner or Manager can grant custom role '"
            + roleName
            + "' in schema '"
            + schema.getName()
            + "'");
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
                .type(GraphQLList.list(inputMembersMetadataType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.COLUMNS)
                .type(GraphQLList.list(inputDropColumnType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(SETTINGS)
                .type(GraphQLList.list(inputDropSettingType)))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.ROLES)
                .type(GraphQLList.list(Scalars.GraphQLString)))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.GROUPS)
                .type(GraphQLList.list(Scalars.GraphQLString)))
        .build();
  }

  public GraphQLFieldDefinition.Builder truncateMutation(Schema schema, TaskService taskService) {
    return GraphQLFieldDefinition.newFieldDefinition()
        .name("truncate")
        .dataFetcher(truncateFetcher(schema, taskService))
        .type(typeForMutationResult)
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.TABLES)
                .type(GraphQLList.list(Scalars.GraphQLString)))
        .argument(
            GraphQLArgument.newArgument()
                .name(GraphqlConstants.ASYNC)
                .type(Scalars.GraphQLBoolean));
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
        .argument(GraphQLArgument.newArgument().name(ID).type(Scalars.GraphQLString))
        .argument(
            GraphQLArgument.newArgument()
                .name(PARAMETERS)
                .type(GraphQLList.list(inputSettingsMetadataType)))
        .argument(GraphQLArgument.newArgument().name(LIMIT).type(Scalars.GraphQLInt))
        .argument(GraphQLArgument.newArgument().name(OFFSET).type(Scalars.GraphQLInt))
        .dataFetcher(
            dataFetchingEnvironment -> {
              Map<String, Object> result = new LinkedHashMap<>();
              final String id = dataFetchingEnvironment.getArgument(ID);
              Integer offset = dataFetchingEnvironment.getArgumentOrDefault(OFFSET, 0);
              Integer limit = dataFetchingEnvironment.getArgumentOrDefault(LIMIT, 10);
              Map<String, String> parameters =
                  convertKeyValueListToMap(dataFetchingEnvironment.getArgument(PARAMETERS));
              result.put(DATA, getReportAsJson(id, schema, parameters, limit, offset));
              result.put(COUNT, getReportCount(id, schema, parameters));
              return result;
            })
        .build();
  }
}
