package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.MG_CHANGE_GROUP;
import static org.molgenis.emx2.Constants.MG_CHANGE_OWNER;
import static org.molgenis.emx2.Constants.ROLE;
import static org.molgenis.emx2.Constants.TABLE;
import static org.molgenis.emx2.graphql.GraphqlConstants.*;

import graphql.Scalars;
import graphql.schema.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;

public class GraphqlPermissionFieldFactory {

  static final GraphQLEnumType selectScopeEnumType;
  static final GraphQLEnumType updateScopeEnumType;

  static {
    GraphQLEnumType.Builder selectBuilder = GraphQLEnumType.newEnum().name("MolgenisSelectScope");
    for (SelectScope scope : SelectScope.values()) {
      selectBuilder.value(scope.name(), scope);
    }
    selectScopeEnumType = selectBuilder.build();

    GraphQLEnumType.Builder updateBuilder = GraphQLEnumType.newEnum().name("MolgenisUpdateScope");
    for (UpdateScope scope : UpdateScope.values()) {
      updateBuilder.value(scope.name(), scope);
    }
    updateScopeEnumType = updateBuilder.build();
  }

  static final GraphQLInputObjectType tablePermissionInputType =
      GraphQLInputObjectType.newInputObject()
          .name("MolgenisTablePermissionInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(TABLE)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(SELECT).type(selectScopeEnumType))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(INSERT).type(updateScopeEnumType))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(UPDATE).type(updateScopeEnumType))
          .field(
              GraphQLInputObjectField.newInputObjectField().name(DELETE).type(updateScopeEnumType))
          .build();

  static final GraphQLInputObjectType inputRoleType =
      GraphQLInputObjectType.newInputObject()
          .name("MolgenisRoleInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(NAME)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(SCHEMA_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(PERMISSIONS)
                  .type(GraphQLList.list(tablePermissionInputType)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(MG_CHANGE_OWNER)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(MG_CHANGE_GROUP)
                  .type(Scalars.GraphQLBoolean))
          .build();

  static final GraphQLObjectType tablePermissionOutputType =
      GraphQLObjectType.newObject()
          .name("MolgenisTablePermissionOutput")
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(TABLE).type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(SELECT).type(selectScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(INSERT).type(updateScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(UPDATE).type(updateScopeEnumType))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(DELETE).type(updateScopeEnumType))
          .build();

  static final GraphQLObjectType roleOutputType =
      GraphQLObjectType.newObject()
          .name("MolgenisRoleOutput")
          .field(GraphQLFieldDefinition.newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(DESCRIPTION)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(SCHEMA_NAME)
                  .type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition().name(SYSTEM).type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(PERMISSIONS)
                  .type(GraphQLList.list(tablePermissionOutputType)))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(MG_CHANGE_OWNER)
                  .type(Scalars.GraphQLBoolean))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(MG_CHANGE_GROUP)
                  .type(Scalars.GraphQLBoolean))
          .build();

  static final GraphQLObjectType groupUserOutputType =
      GraphQLObjectType.newObject()
          .name("MolgenisGroupUserOutput")
          .field(GraphQLFieldDefinition.newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(GraphQLFieldDefinition.newFieldDefinition().name(ROLE).type(Scalars.GraphQLString))
          .build();

  static final GraphQLObjectType groupOutputType =
      GraphQLObjectType.newObject()
          .name("MolgenisGroupOutput")
          .field(GraphQLFieldDefinition.newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(
              GraphQLFieldDefinition.newFieldDefinition()
                  .name(USERS)
                  .type(GraphQLList.list(groupUserOutputType)))
          .build();

  static final GraphQLInputObjectType groupInputType =
      GraphQLInputObjectType.newInputObject()
          .name("MolgenisGroupInput")
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(NAME)
                  .type(GraphQLNonNull.nonNull(Scalars.GraphQLString)))
          .field(
              GraphQLInputObjectField.newInputObjectField()
                  .name(USERS)
                  .type(GraphQLList.list(Scalars.GraphQLString)))
          .build();

  static SelectScope toSelectScope(Object graphqlValue) {
    if (graphqlValue == null) {
      return SelectScope.NONE;
    }
    if (graphqlValue instanceof SelectScope scope) {
      return scope;
    }
    try {
      return SelectScope.fromString(graphqlValue.toString());
    } catch (MolgenisException ignored) {
      return SelectScope.NONE;
    }
  }

  static UpdateScope toUpdateScope(Object graphqlValue) {
    if (graphqlValue == null) {
      return UpdateScope.NONE;
    }
    if (graphqlValue instanceof UpdateScope scope) {
      return scope;
    }
    try {
      return UpdateScope.fromString(graphqlValue.toString());
    } catch (MolgenisException ignored) {
      return UpdateScope.NONE;
    }
  }

  static PermissionSet toPermissionSet(Map<String, Object> input) {
    PermissionSet ps = new PermissionSet();
    if (input == null) {
      return ps;
    }

    boolean changeOwner = Boolean.TRUE.equals(input.get(MG_CHANGE_OWNER));
    boolean changeGroup = Boolean.TRUE.equals(input.get(MG_CHANGE_GROUP));
    ps.setChangeOwner(changeOwner);
    ps.setChangeGroup(changeGroup);
    Object descriptionValue = input.get(DESCRIPTION);
    ps.setDescription(descriptionValue instanceof String str ? str : "");
    Object schemaValue = input.get(SCHEMA_NAME);
    if (schemaValue instanceof String str) {
      ps.setSchema(str);
    }

    Object tablesValue = input.get(PERMISSIONS);
    if (!(tablesValue instanceof List<?> tableList)) {
      return ps;
    }

    for (Object entry : tableList) {
      if (!(entry instanceof Map<?, ?> rawMap)) {
        continue;
      }
      String tableName = extractString(rawMap, TABLE);
      if (tableName == null) {
        continue;
      }

      SelectScope selectScope = toSelectScope(rawMap.get(SELECT));
      UpdateScope insertScope = toUpdateScope(rawMap.get(INSERT));
      UpdateScope updateScope = toUpdateScope(rawMap.get(UPDATE));
      UpdateScope deleteScope = toUpdateScope(rawMap.get(DELETE));

      PermissionSet.TablePermissions perms =
          new PermissionSet.TablePermissions()
              .setSelect(selectScope)
              .setInsert(insertScope)
              .setUpdate(updateScope)
              .setDelete(deleteScope);

      ps.putTable(tableName, perms);
    }

    return ps;
  }

  private static String extractString(Map<?, ?> map, String key) {
    Object value = map.get(key);
    return value instanceof String str ? str : null;
  }

  static Map<String, Object> permissionSetToMap(
      String roleName, String schemaName, boolean isSystemRole, PermissionSet ps) {
    Map<String, Object> roleMap = new LinkedHashMap<>();
    roleMap.put(NAME, roleName);
    roleMap.put(SCHEMA_NAME, schemaName);
    roleMap.put(DESCRIPTION, ps.getDescription());
    roleMap.put(SYSTEM, isSystemRole);
    roleMap.put(MG_CHANGE_OWNER, ps.isChangeOwner());
    roleMap.put(MG_CHANGE_GROUP, ps.isChangeGroup());
    List<Map<String, Object>> tableList =
        ps.getTables().entrySet().stream()
            .map(
                entry -> {
                  Map<String, Object> tableMap = new LinkedHashMap<>();
                  tableMap.put(TABLE, entry.getKey());
                  tableMap.put(SELECT, entry.getValue().getSelect());
                  tableMap.put(INSERT, entry.getValue().getInsert());
                  tableMap.put(UPDATE, entry.getValue().getUpdate());
                  tableMap.put(DELETE, entry.getValue().getDelete());
                  return tableMap;
                })
            .toList();
    roleMap.put(PERMISSIONS, tableList);
    return roleMap;
  }

  public static void requireManagerOrOwner(Database db, Schema schema) {
    if (db.isAdmin()) return;
    if (schema == null
        || (!schema.hasActiveUserRole(Privileges.MANAGER)
            && !schema.hasActiveUserRole(Privileges.OWNER))) {
      String schemaName = schema != null ? schema.getName() : "unknown";
      throw new MolgenisException(
          "Only Manager or Owner can grant custom roles on schema " + schemaName);
    }
  }

  private GraphqlPermissionFieldFactory() {}
}
